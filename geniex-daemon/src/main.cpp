// geniex_daemon — the Horizons model+vision daemon (GenieX runtime).
//
// Serves the OpenAI-compatible wire GenieXClient already speaks:
//   GET  /v1/models            200 {"data":[{"id":"…"}]} once the model is
//                              loaded; 503 {"data":[]} while loading/failed
//   POST /v1/chat/completions  {"model","messages":[{role,content}],
//                               "stream":true,"max_tokens","temperature"}
//                              → SSE chunks {"choices":[{"delta":{"content"}}]}
//                                terminated by data: [DONE]
//                              → or one JSON completion when "stream" false
//
// Runtime: libgeniex (qualcomm/GenieX SDK, C API in geniex.h) with its
// dlopen'd backend plugins — "llama_cpp" (GGML: Q4_0 GGUF, the primary
// path) or "qairt" (HTP bundles; perf comes from the bundle's own
// htp_backend_ext_config.json — see wiki/GENIEX-DAEMON-PLAN.md).
//
// Serve-first (wiki/BOOT-SEQUENCE.md I2): socket binds immediately, the
// model loads on a background thread, /v1/models answers 503-shaped output
// until ready, and the process never exits over a bad model. HTTP reads
// honor Content-Length (no single-recv truncation).
//
// Usage:
//   GENIEX_PLUGIN_PATH=<plugins dir> geniex_daemon \
//     --port 18181 --model <path.gguf | qairt-bundle-dir> \
//     [--plugin llama_cpp|qairt] [--device npu|gpu|cpu|hybrid]
//     [--model-name <id shown in /v1/models>] [--threads N] [--nctx N]

#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <unistd.h>

#include <atomic>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <map>
#include <memory>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#include "geniex.h"

// ───────────────────────── minimal JSON (parse + escape) ────────────────────
// Small recursive-descent parser: objects, arrays, strings, numbers, bools,
// null. Enough for the chat-completions request body; no external deps.
namespace mini_json {

struct Value {
  enum Kind { Null, Bool, Num, Str, Arr, Obj } kind = Null;
  bool b = false;
  double num = 0;
  std::string str;
  std::vector<Value> arr;
  std::map<std::string, Value> obj;

  const Value *get(const std::string &k) const {
    if (kind != Obj) return nullptr;
    auto it = obj.find(k);
    return it == obj.end() ? nullptr : &it->second;
  }
};

struct Parser {
  const char *p, *end;
  bool ok = true;

  explicit Parser(const std::string &s) : p(s.data()), end(s.data() + s.size()) {}

  void ws() { while (p < end && (*p == ' ' || *p == '\t' || *p == '\n' || *p == '\r')) ++p; }
  bool lit(const char *s) {
    size_t n = strlen(s);
    if (static_cast<size_t>(end - p) >= n && !strncmp(p, s, n)) { p += n; return true; }
    return false;
  }

  Value parse() { ws(); return value(); }

  Value value() {
    ws();
    if (p >= end) { ok = false; return {}; }
    switch (*p) {
      case '{': return object();
      case '[': return array();
      case '"': { Value v; v.kind = Value::Str; v.str = string(); return v; }
      case 't': { Value v; v.kind = Value::Bool; v.b = true; ok &= lit("true"); return v; }
      case 'f': { Value v; v.kind = Value::Bool; v.b = false; ok &= lit("false"); return v; }
      case 'n': { ok &= lit("null"); return {}; }
      default: return number();
    }
  }

  Value object() {
    Value v; v.kind = Value::Obj; ++p;  // '{'
    ws();
    if (p < end && *p == '}') { ++p; return v; }
    while (ok && p < end) {
      ws();
      if (*p != '"') { ok = false; break; }
      std::string key = string();
      ws();
      if (p >= end || *p != ':') { ok = false; break; }
      ++p;
      v.obj[key] = value();
      ws();
      if (p < end && *p == ',') { ++p; continue; }
      if (p < end && *p == '}') { ++p; break; }
      ok = false; break;
    }
    return v;
  }

  Value array() {
    Value v; v.kind = Value::Arr; ++p;  // '['
    ws();
    if (p < end && *p == ']') { ++p; return v; }
    while (ok && p < end) {
      v.arr.push_back(value());
      ws();
      if (p < end && *p == ',') { ++p; continue; }
      if (p < end && *p == ']') { ++p; break; }
      ok = false; break;
    }
    return v;
  }

  std::string string() {
    std::string out; ++p;  // '"'
    while (p < end && *p != '"') {
      char c = *p++;
      if (c == '\\' && p < end) {
        char e = *p++;
        switch (e) {
          case 'n': out += '\n'; break;
          case 't': out += '\t'; break;
          case 'r': out += '\r'; break;
          case 'b': out += '\b'; break;
          case 'f': out += '\f'; break;
          case 'u': {
            // Decode \uXXXX (BMP only; surrogate pairs collapsed to '?')
            if (end - p >= 4) {
              char hex[5] = {p[0], p[1], p[2], p[3], 0};
              unsigned cp = static_cast<unsigned>(strtoul(hex, nullptr, 16));
              p += 4;
              if (cp < 0x80) out += static_cast<char>(cp);
              else if (cp < 0x800) {
                out += static_cast<char>(0xC0 | (cp >> 6));
                out += static_cast<char>(0x80 | (cp & 0x3F));
              } else if (cp >= 0xD800 && cp <= 0xDFFF) {
                out += '?';  // surrogate half — not decoding pairs here
                if (cp <= 0xDBFF && end - p >= 6 && p[0] == '\\' && p[1] == 'u') p += 6;
              } else {
                out += static_cast<char>(0xE0 | (cp >> 12));
                out += static_cast<char>(0x80 | ((cp >> 6) & 0x3F));
                out += static_cast<char>(0x80 | (cp & 0x3F));
              }
            } else { ok = false; }
            break;
          }
          default: out += e; break;  // '"' '\\' '/'
        }
      } else {
        out += c;
      }
    }
    if (p < end) ++p;  // closing '"'
    else ok = false;
    return out;
  }

  Value number() {
    Value v; v.kind = Value::Num;
    char *np = nullptr;
    v.num = strtod(p, &np);
    if (np == p) { ok = false; return {}; }
    p = np;
    return v;
  }
};

std::string escape(const std::string &s) {
  std::string out;
  out.reserve(s.size() + 8);
  for (char c : s) {
    switch (c) {
      case '"': out += "\\\""; break;
      case '\\': out += "\\\\"; break;
      case '\n': out += "\\n"; break;
      case '\r': out += "\\r"; break;
      case '\t': out += "\\t"; break;
      default:
        if (static_cast<unsigned char>(c) < 0x20) out += ' ';
        else out += c;
    }
  }
  return out;
}

}  // namespace mini_json

// ───────────────────────── daemon state ─────────────────────────────────────
namespace {

std::atomic<bool> g_model_ready{false};
std::atomic<bool> g_loading{true};
geniex_LLM *g_llm = nullptr;
std::mutex g_llm_mutex;  // one generation at a time — the NPU serializes anyway
std::string g_model_name = "geniex-local";
std::string g_load_error;

std::string g_model_path;
std::string g_plugin = "llama_cpp";
std::string g_device;   // empty = plugin default
int g_threads = 4;
int g_nctx = 0;          // 0 = model default; ONLY passed to llama_cpp (qairt rejects it)

void LoaderThread() {
  geniex_LlmCreateInput in;
  memset(&in, 0, sizeof(in));
  in.model_name = g_model_name.c_str();
  in.model_path = g_model_path.c_str();
  in.plugin_id = g_plugin.c_str();
  in.device_id = g_device.empty() ? nullptr : g_device.c_str();
  in.config.n_threads = g_threads;
  // qairt plugin hard-rejects n_ctx/n_gpu_layers (llama_cpp-only) — leave 0 there.
  if (g_plugin == "llama_cpp" && g_nctx > 0) in.config.n_ctx = g_nctx;

  geniex_LLM *handle = nullptr;
  int32_t rc = geniex_llm_create(&in, &handle);
  if (rc == GENIEX_SUCCESS && handle) {
    g_llm = handle;
    g_model_ready = true;
    fprintf(stderr, "[geniex_daemon] model ready: %s (%s/%s)\n",
            g_model_path.c_str(), g_plugin.c_str(),
            g_device.empty() ? "default" : g_device.c_str());
  } else {
    const char *msg = geniex_get_error_message(static_cast<geniex_ErrorCode>(rc));
    g_load_error = msg ? msg : "unknown error";
    fprintf(stderr, "[geniex_daemon] model load FAILED (%d: %s) — serving 503, not exiting\n",
            rc, g_load_error.c_str());
  }
  g_loading = false;
}

// ───────────────────────── HTTP plumbing (same shape as media_daemon) ──────
bool SendAll(int fd, const char *data, size_t n) {
  size_t sent = 0;
  while (sent < n) {
    ssize_t w = ::send(fd, data + sent, n - sent, 0);
    if (w <= 0) return false;
    sent += static_cast<size_t>(w);
  }
  return true;
}

void Respond(int fd, int code, const char *status, const std::string &body) {
  char header[256];
  int hn = snprintf(header, sizeof(header),
                    "HTTP/1.1 %d %s\r\n"
                    "Content-Type: application/json\r\n"
                    "Content-Length: %zu\r\n"
                    "Connection: close\r\n\r\n",
                    code, status, body.size());
  SendAll(fd, header, static_cast<size_t>(hn));
  SendAll(fd, body.data(), body.size());
}

bool ReadRequest(int fd, std::string *method, std::string *path, std::string *body) {
  std::string buf;
  buf.reserve(16 * 1024);
  char chunk[16 * 1024];
  size_t header_end = std::string::npos;

  while (header_end == std::string::npos) {
    ssize_t n = ::recv(fd, chunk, sizeof(chunk), 0);
    if (n <= 0) return false;
    buf.append(chunk, static_cast<size_t>(n));
    header_end = buf.find("\r\n\r\n");
    if (buf.size() > 64 * 1024 && header_end == std::string::npos) return false;
  }

  size_t sp1 = buf.find(' ');
  size_t sp2 = (sp1 == std::string::npos) ? std::string::npos : buf.find(' ', sp1 + 1);
  if (sp1 == std::string::npos || sp2 == std::string::npos) return false;
  *method = buf.substr(0, sp1);
  *path = buf.substr(sp1 + 1, sp2 - sp1 - 1);

  size_t content_length = 0;
  {
    std::string headers = buf.substr(0, header_end);
    std::string lower(headers.size(), 0);
    for (size_t i = 0; i < headers.size(); ++i)
      lower[i] = static_cast<char>(tolower(static_cast<unsigned char>(headers[i])));
    size_t cl = lower.find("content-length:");
    if (cl != std::string::npos)
      content_length = static_cast<size_t>(atoll(headers.c_str() + cl + 15));
  }
  if (content_length > 32u * 1024 * 1024) return false;

  *body = buf.substr(header_end + 4);
  while (body->size() < content_length) {
    ssize_t n = ::recv(fd, chunk, sizeof(chunk), 0);
    if (n <= 0) return false;
    body->append(chunk, static_cast<size_t>(n));
  }
  body->resize(content_length);
  return true;
}

// ───────────────────────── handlers ────────────────────────────────────────
void HandleModels(int fd) {
  if (g_model_ready) {
    Respond(fd, 200, "OK",
            "{\"object\":\"list\",\"data\":[{\"id\":\"" + mini_json::escape(g_model_name) +
            "\",\"object\":\"model\",\"owned_by\":\"geniex_daemon\"}]}");
  } else {
    std::string why = g_loading ? "loading" : ("load failed: " + g_load_error);
    Respond(fd, 503, "Service Unavailable",
            "{\"object\":\"list\",\"data\":[],\"status\":\"" + mini_json::escape(why) + "\"}");
  }
}

struct StreamCtx {
  int fd;
  bool client_alive = true;
};

// Token callback: emit one OpenAI chat.completion.chunk per token, straight
// to the socket. Returning false cancels generation (client went away).
bool OnToken(const char *token, void *user_data) {
  auto *ctx = static_cast<StreamCtx *>(user_data);
  if (!ctx->client_alive || !token) return ctx->client_alive;
  std::string frame =
      "data: {\"object\":\"chat.completion.chunk\",\"choices\":[{\"index\":0,"
      "\"delta\":{\"content\":\"" + mini_json::escape(token) +
      "\"},\"finish_reason\":null}]}\n\n";
  if (!SendAll(ctx->fd, frame.data(), frame.size())) ctx->client_alive = false;
  return ctx->client_alive;
}

void HandleChatCompletions(int fd, const std::string &body) {
  if (!g_model_ready) {
    std::string why = g_loading ? "model still loading" : ("model load failed: " + g_load_error);
    Respond(fd, 503, "Service Unavailable",
            "{\"error\":{\"message\":\"" + mini_json::escape(why) + "\"}}");
    return;
  }

  mini_json::Parser parser(body);
  mini_json::Value req = parser.parse();
  if (!parser.ok || req.kind != mini_json::Value::Obj) {
    Respond(fd, 400, "Bad Request", "{\"error\":{\"message\":\"invalid JSON body\"}}");
    return;
  }
  const mini_json::Value *messages = req.get("messages");
  if (!messages || messages->kind != mini_json::Value::Arr || messages->arr.empty()) {
    Respond(fd, 400, "Bad Request", "{\"error\":{\"message\":\"missing messages[]\"}}");
    return;
  }

  // Flatten messages. Content may be a plain string, or an OpenAI content-part
  // array — text parts are concatenated; image parts are NOT handled yet
  // (VLM path is a follow-up: geniex_vlm_* + image_paths).
  std::vector<std::pair<std::string, std::string>> msgs;  // role, content
  for (const auto &m : messages->arr) {
    const mini_json::Value *role = m.get("role");
    const mini_json::Value *content = m.get("content");
    if (!role || role->kind != mini_json::Value::Str || !content) continue;
    std::string text;
    if (content->kind == mini_json::Value::Str) {
      text = content->str;
    } else if (content->kind == mini_json::Value::Arr) {
      for (const auto &part : content->arr) {
        const mini_json::Value *type = part.get("type");
        const mini_json::Value *t = part.get("text");
        if (type && type->kind == mini_json::Value::Str && type->str == "text" &&
            t && t->kind == mini_json::Value::Str)
          text += t->str;
      }
    }
    msgs.emplace_back(role->str, text);
  }
  if (msgs.empty()) {
    Respond(fd, 400, "Bad Request", "{\"error\":{\"message\":\"no usable messages\"}}");
    return;
  }

  bool stream = false;
  if (const auto *s = req.get("stream")) stream = (s->kind == mini_json::Value::Bool && s->b);
  int max_tokens = 2048;
  if (const auto *mt = req.get("max_tokens"))
    if (mt->kind == mini_json::Value::Num && mt->num > 0) max_tokens = static_cast<int>(mt->num);
  float temperature = -1.0f;
  if (const auto *t = req.get("temperature"))
    if (t->kind == mini_json::Value::Num) temperature = static_cast<float>(t->num);

  std::lock_guard<std::mutex> lock(g_llm_mutex);

  // Chat template → prompt string.
  std::vector<geniex_LlmChatMessage> cmsgs(msgs.size());
  for (size_t i = 0; i < msgs.size(); ++i) {
    cmsgs[i].role = msgs[i].first.c_str();
    cmsgs[i].content = msgs[i].second.c_str();
  }
  geniex_LlmApplyChatTemplateInput tin;
  memset(&tin, 0, sizeof(tin));
  tin.messages = cmsgs.data();
  tin.message_count = static_cast<int32_t>(cmsgs.size());
  tin.add_generation_prompt = true;
  geniex_LlmApplyChatTemplateOutput tout;
  memset(&tout, 0, sizeof(tout));
  std::string prompt;
  if (geniex_llm_apply_chat_template(g_llm, &tin, &tout) == GENIEX_SUCCESS && tout.formatted_text) {
    prompt = tout.formatted_text;
    geniex_free(tout.formatted_text);
  } else {
    // Template failure — degrade to the last user message rather than erroring.
    prompt = msgs.back().second;
  }

  geniex_SamplerConfig sampler;
  memset(&sampler, 0, sizeof(sampler));
  bool use_sampler = false;
  if (temperature >= 0.0f) {
    sampler.temperature = temperature;
    sampler.top_p = 0.95f;
    sampler.top_k = 40;
    sampler.seed = -1;
    use_sampler = true;
  }

  geniex_GenerationConfig gen;
  memset(&gen, 0, sizeof(gen));
  gen.max_tokens = max_tokens;
  if (use_sampler) gen.sampler_config = &sampler;

  geniex_LlmGenerateInput gin;
  memset(&gin, 0, sizeof(gin));
  gin.prompt_utf8 = prompt.c_str();
  gin.config = &gen;

  if (stream) {
    const char *hdr =
        "HTTP/1.1 200 OK\r\n"
        "Content-Type: text/event-stream\r\n"
        "Cache-Control: no-cache\r\n"
        "Connection: close\r\n\r\n";
    if (!SendAll(fd, hdr, strlen(hdr))) return;
    StreamCtx ctx{fd};
    gin.on_token = OnToken;
    gin.user_data = &ctx;

    geniex_LlmGenerateOutput gout;
    memset(&gout, 0, sizeof(gout));
    int32_t rc = geniex_llm_generate(g_llm, &gin, &gout);
    if (gout.full_text) geniex_free(gout.full_text);

    if (ctx.client_alive) {
      const char *fin =
          "data: {\"object\":\"chat.completion.chunk\",\"choices\":[{\"index\":0,"
          "\"delta\":{},\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n";
      SendAll(fd, fin, strlen(fin));
    }
    if (rc != GENIEX_SUCCESS)
      fprintf(stderr, "[geniex_daemon] generate rc=%d (%s)\n", rc,
              geniex_get_error_message(static_cast<geniex_ErrorCode>(rc)));
  } else {
    geniex_LlmGenerateOutput gout;
    memset(&gout, 0, sizeof(gout));
    int32_t rc = geniex_llm_generate(g_llm, &gin, &gout);
    if (rc != GENIEX_SUCCESS || !gout.full_text) {
      const char *msg = geniex_get_error_message(static_cast<geniex_ErrorCode>(rc));
      Respond(fd, 500, "Internal Server Error",
              "{\"error\":{\"message\":\"" + mini_json::escape(msg ? msg : "generation failed") + "\"}}");
      if (gout.full_text) geniex_free(gout.full_text);
      return;
    }
    std::string text = gout.full_text;
    geniex_free(gout.full_text);
    Respond(fd, 200, "OK",
            "{\"object\":\"chat.completion\",\"model\":\"" + mini_json::escape(g_model_name) +
            "\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"" +
            mini_json::escape(text) + "\"},\"finish_reason\":\"stop\"}]}");
  }
}

void HandleConnection(int fd) {
  std::string method, path, body;
  if (ReadRequest(fd, &method, &path, &body)) {
    if (method == "GET" && (path == "/v1/models" || path == "/v1/models/")) HandleModels(fd);
    else if (method == "POST" && path == "/v1/chat/completions") HandleChatCompletions(fd, body);
    else if (method == "GET" && path == "/") Respond(fd, 200, "OK", "{\"service\":\"geniex_daemon\"}");
    else Respond(fd, 404, "Not Found", "{\"error\":{\"message\":\"not found\"}}");
  }
  ::close(fd);
}

}  // namespace

int main(int argc, char *argv[]) {
  int port = 18181;
  const char *plugin_dir = nullptr;
  for (int i = 1; i + 1 < argc; ++i) {
    if (!strcmp(argv[i], "--port")) port = atoi(argv[++i]);
    else if (!strcmp(argv[i], "--model")) g_model_path = argv[++i];
    else if (!strcmp(argv[i], "--plugin")) g_plugin = argv[++i];
    else if (!strcmp(argv[i], "--device")) g_device = argv[++i];
    else if (!strcmp(argv[i], "--model-name")) g_model_name = argv[++i];
    else if (!strcmp(argv[i], "--threads")) g_threads = atoi(argv[++i]);
    else if (!strcmp(argv[i], "--nctx")) g_nctx = atoi(argv[++i]);
    else if (!strcmp(argv[i], "--plugin-dir")) plugin_dir = argv[++i];
  }
  if (plugin_dir) setenv("GENIEX_PLUGIN_PATH", plugin_dir, 1);
  if (g_model_path.empty()) {
    fprintf(stderr,
            "[geniex_daemon] WARNING: no --model given — serving 503 forever "
            "(serve-first: not exiting).\n");
    g_loading = false;
    g_load_error = "no --model argument";
  }

  // Serve-first: socket before any model I/O.
  int server_fd = ::socket(AF_INET, SOCK_STREAM, 0);
  if (server_fd < 0) { perror("socket"); return 1; }
  int opt = 1;
  setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
  sockaddr_in addr{};
  addr.sin_family = AF_INET;
  addr.sin_addr.s_addr = inet_addr("127.0.0.1");  // loopback only
  addr.sin_port = htons(static_cast<uint16_t>(port));
  if (::bind(server_fd, reinterpret_cast<sockaddr *>(&addr), sizeof(addr)) < 0) {
    perror("bind");
    return 1;
  }
  if (::listen(server_fd, 8) < 0) { perror("listen"); return 1; }
  fprintf(stderr, "[geniex_daemon] serving http://127.0.0.1:%d/v1 (model=%s plugin=%s)\n",
          port, g_model_path.empty() ? "-" : g_model_path.c_str(), g_plugin.c_str());

  if (geniex_init() != GENIEX_SUCCESS) {
    // Keep serving 503 rather than dying — the watchdog treats a dead
    // process as relaunchable; a broken install shouldn't crash-loop.
    fprintf(stderr, "[geniex_daemon] geniex_init failed — serving 503\n");
    g_loading = false;
    g_load_error = "geniex_init failed";
  } else if (!g_model_path.empty()) {
    std::thread(LoaderThread).detach();
  }

  while (true) {
    int client = ::accept(server_fd, nullptr, nullptr);
    if (client < 0) continue;
    HandleConnection(client);
  }
}
