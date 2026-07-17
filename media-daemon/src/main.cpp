// media_daemon — the Horizons media (STT/TTS) daemon.
//
// Serves the wire contract DaemonSttClient/DaemonTtsClient already speak:
//   GET  /health          200 when every configured model is loaded, else 503
//   POST /stt   audio/wav → 200 {"text":"…"}
//   POST /tts   {"text":"…","voice":"0","speed":1.0} → 200 audio/wav bytes
//
// Runtime: sherpa-onnx C API — Moonshine (offline ASR) + Kokoro (offline TTS).
// Runs as a detached process on 127.0.0.1:8091 (launch via DaemonLauncher,
// guard via CliffordService — same pattern as ort_engine/geniex).
//
// Serve-first (wiki/BOOT-SEQUENCE.md invariant I2): the socket binds and
// answers immediately; models load on a background thread; /health returns
// 503 while loading or if a model dir is missing/broken. The process NEVER
// exits over a bad model — alive ≠ ready, and the watchdog must never see
// a load failure as a dead process.
//
// HTTP: reads the full header block, honors Content-Length, and loops recv()
// until the body is complete — deliberately NOT repeating ort_engine's
// single-recv() 8KB truncation bug (WAV uploads are hundreds of KB).
//
// Usage:
//   media_daemon --port 8091 --stt-dir /path/moonshine --tts-dir /path/kokoro
//                [--threads 2]

#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <unistd.h>

#include <atomic>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#include "sherpa-onnx/c-api/c-api.h"

namespace {

std::atomic<bool> g_stt_ready{false};
std::atomic<bool> g_tts_ready{false};
std::atomic<bool> g_loading{true};

const SherpaOnnxOfflineRecognizer *g_recognizer = nullptr;
const SherpaOnnxOfflineTts *g_tts = nullptr;
std::mutex g_stt_mutex;  // sherpa streams are cheap; recognizer calls serialized
std::mutex g_tts_mutex;

std::string g_stt_dir;
std::string g_tts_dir;
int g_threads = 2;

bool FileExists(const std::string &p) {
  struct stat st{};
  return ::stat(p.c_str(), &st) == 0;
}

// First existing candidate under dir, or "".
std::string Pick(const std::string &dir, const std::vector<std::string> &names) {
  for (const auto &n : names) {
    std::string p = dir + "/" + n;
    if (FileExists(p)) return p;
  }
  return "";
}

// ── Model loading (background thread) ──────────────────────────────────────

void LoadStt() {
  if (g_stt_dir.empty()) return;
  std::string preprocessor = Pick(g_stt_dir, {"preprocess.onnx"});
  std::string encoder = Pick(g_stt_dir, {"encode.int8.onnx", "encode.onnx"});
  std::string uncached = Pick(g_stt_dir, {"uncached_decode.int8.onnx", "uncached_decode.onnx"});
  std::string cached = Pick(g_stt_dir, {"cached_decode.int8.onnx", "cached_decode.onnx"});
  std::string tokens = Pick(g_stt_dir, {"tokens.txt"});
  if (preprocessor.empty() || encoder.empty() || uncached.empty() ||
      cached.empty() || tokens.empty()) {
    fprintf(stderr, "[media_daemon] STT: moonshine files missing in %s — serving 503\n",
            g_stt_dir.c_str());
    return;
  }

  SherpaOnnxOfflineModelConfig model_config;
  memset(&model_config, 0, sizeof(model_config));
  model_config.debug = 0;
  model_config.num_threads = g_threads;
  model_config.provider = "cpu";
  model_config.tokens = tokens.c_str();
  model_config.moonshine.preprocessor = preprocessor.c_str();
  model_config.moonshine.encoder = encoder.c_str();
  model_config.moonshine.uncached_decoder = uncached.c_str();
  model_config.moonshine.cached_decoder = cached.c_str();

  SherpaOnnxOfflineRecognizerConfig config;
  memset(&config, 0, sizeof(config));
  config.decoding_method = "greedy_search";
  config.model_config = model_config;

  g_recognizer = SherpaOnnxCreateOfflineRecognizer(&config);
  if (g_recognizer) {
    g_stt_ready = true;
    fprintf(stderr, "[media_daemon] STT ready (moonshine, %s)\n", g_stt_dir.c_str());
  } else {
    fprintf(stderr, "[media_daemon] STT: recognizer create FAILED — serving 503\n");
  }
}

void LoadTts() {
  if (g_tts_dir.empty()) return;
  std::string model = Pick(g_tts_dir, {"model.onnx"});
  std::string voices = Pick(g_tts_dir, {"voices.bin"});
  std::string tokens = Pick(g_tts_dir, {"tokens.txt"});
  std::string data_dir = g_tts_dir + "/espeak-ng-data";
  if (model.empty() || voices.empty() || tokens.empty() || !FileExists(data_dir)) {
    fprintf(stderr, "[media_daemon] TTS: kokoro files missing in %s — serving 503\n",
            g_tts_dir.c_str());
    return;
  }

  SherpaOnnxOfflineTtsConfig config;
  memset(&config, 0, sizeof(config));
  config.model.kokoro.model = model.c_str();
  config.model.kokoro.voices = voices.c_str();
  config.model.kokoro.tokens = tokens.c_str();
  config.model.kokoro.data_dir = data_dir.c_str();
  config.model.num_threads = g_threads;
  config.model.debug = 0;

  g_tts = SherpaOnnxCreateOfflineTts(&config);
  if (g_tts) {
    g_tts_ready = true;
    fprintf(stderr, "[media_daemon] TTS ready (kokoro, %s)\n", g_tts_dir.c_str());
  } else {
    fprintf(stderr, "[media_daemon] TTS: create FAILED — serving 503\n");
  }
}

void LoaderThread() {
  LoadStt();
  LoadTts();
  g_loading = false;
}

// ── Tiny JSON helpers (only what /tts needs — no dependency) ───────────────

// Extract a JSON string value: "key":"…" with \" \\ \n \t \r \uXXXX(skipped→?) unescaping.
std::string JsonString(const std::string &body, const std::string &key) {
  std::string pat = "\"" + key + "\"";
  size_t k = body.find(pat);
  if (k == std::string::npos) return "";
  size_t colon = body.find(':', k + pat.size());
  if (colon == std::string::npos) return "";
  size_t q = body.find('"', colon + 1);
  if (q == std::string::npos) return "";
  std::string out;
  for (size_t i = q + 1; i < body.size(); ++i) {
    char c = body[i];
    if (c == '\\' && i + 1 < body.size()) {
      char e = body[++i];
      switch (e) {
        case 'n': out += '\n'; break;
        case 't': out += '\t'; break;
        case 'r': out += '\r'; break;
        case 'u': out += '?'; i += 4 <= body.size() - i ? 4 : 0; break;
        default: out += e; break;  // covers \" \\ \/
      }
    } else if (c == '"') {
      return out;
    } else {
      out += c;
    }
  }
  return out;
}

double JsonNumber(const std::string &body, const std::string &key, double dflt) {
  std::string pat = "\"" + key + "\"";
  size_t k = body.find(pat);
  if (k == std::string::npos) return dflt;
  size_t colon = body.find(':', k + pat.size());
  if (colon == std::string::npos) return dflt;
  size_t i = colon + 1;
  while (i < body.size() && (body[i] == ' ' || body[i] == '"')) ++i;
  return atof(body.c_str() + i);
}

std::string JsonEscape(const std::string &s) {
  std::string out;
  for (char c : s) {
    switch (c) {
      case '"': out += "\\\""; break;
      case '\\': out += "\\\\"; break;
      case '\n': out += "\\n"; break;
      case '\r': out += "\\r"; break;
      case '\t': out += "\\t"; break;
      default:
        if (static_cast<unsigned char>(c) < 0x20) { out += ' '; }
        else out += c;
    }
  }
  return out;
}

// ── HTTP plumbing ──────────────────────────────────────────────────────────

bool SendAll(int fd, const char *data, size_t n) {
  size_t sent = 0;
  while (sent < n) {
    ssize_t w = ::send(fd, data + sent, n - sent, 0);
    if (w <= 0) return false;
    sent += static_cast<size_t>(w);
  }
  return true;
}

void Respond(int fd, int code, const char *status, const std::string &content_type,
             const std::string &body) {
  char header[256];
  int hn = snprintf(header, sizeof(header),
                    "HTTP/1.1 %d %s\r\n"
                    "Content-Type: %s\r\n"
                    "Content-Length: %zu\r\n"
                    "Connection: close\r\n\r\n",
                    code, status, content_type.c_str(), body.size());
  SendAll(fd, header, static_cast<size_t>(hn));
  SendAll(fd, body.data(), body.size());
}

void RespondWav(int fd, const std::vector<char> &wav) {
  char header[256];
  int hn = snprintf(header, sizeof(header),
                    "HTTP/1.1 200 OK\r\n"
                    "Content-Type: audio/wav\r\n"
                    "Content-Length: %zu\r\n"
                    "Connection: close\r\n\r\n",
                    wav.size());
  SendAll(fd, header, static_cast<size_t>(hn));
  SendAll(fd, wav.data(), wav.size());
}

// Read the full request: headers to \r\n\r\n, then exactly Content-Length body
// bytes. Loops recv() — large WAV/text bodies arrive in many segments.
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

  // Request line
  size_t sp1 = buf.find(' ');
  size_t sp2 = (sp1 == std::string::npos) ? std::string::npos : buf.find(' ', sp1 + 1);
  if (sp1 == std::string::npos || sp2 == std::string::npos) return false;
  *method = buf.substr(0, sp1);
  *path = buf.substr(sp1 + 1, sp2 - sp1 - 1);

  // Content-Length (case-insensitive scan of the header block)
  size_t content_length = 0;
  {
    std::string headers = buf.substr(0, header_end);
    std::string lower;
    lower.resize(headers.size());
    for (size_t i = 0; i < headers.size(); ++i)
      lower[i] = static_cast<char>(tolower(static_cast<unsigned char>(headers[i])));
    size_t cl = lower.find("content-length:");
    if (cl != std::string::npos)
      content_length = static_cast<size_t>(atoll(headers.c_str() + cl + 15));
  }
  if (content_length > 64u * 1024 * 1024) return false;  // sanity cap: 64 MB

  *body = buf.substr(header_end + 4);
  while (body->size() < content_length) {
    ssize_t n = ::recv(fd, chunk, sizeof(chunk), 0);
    if (n <= 0) return false;
    body->append(chunk, static_cast<size_t>(n));
  }
  body->resize(content_length);
  return true;
}

// ── Handlers ───────────────────────────────────────────────────────────────

void HandleHealth(int fd) {
  bool stt_configured = !g_stt_dir.empty();
  bool tts_configured = !g_tts_dir.empty();
  bool ready = (!stt_configured || g_stt_ready) &&
               (!tts_configured || g_tts_ready) &&
               (stt_configured || tts_configured) && !g_loading;
  char body[256];
  snprintf(body, sizeof(body),
           "{\"stt\":%s,\"tts\":%s,\"loading\":%s}",
           g_stt_ready ? "true" : "false",
           g_tts_ready ? "true" : "false",
           g_loading ? "true" : "false");
  if (ready) Respond(fd, 200, "OK", "application/json", body);
  else Respond(fd, 503, "Service Unavailable", "application/json", body);
}

void HandleStt(int fd, const std::string &body) {
  if (!g_stt_ready) {
    Respond(fd, 503, "Service Unavailable", "application/json",
            "{\"error\":\"stt model not loaded\"}");
    return;
  }
  const SherpaOnnxWave *wave =
      SherpaOnnxReadWaveFromBinaryData(body.data(), static_cast<int32_t>(body.size()));
  if (!wave) {
    Respond(fd, 400, "Bad Request", "application/json",
            "{\"error\":\"body is not a valid mono 16-bit WAV\"}");
    return;
  }
  std::string text;
  {
    std::lock_guard<std::mutex> lock(g_stt_mutex);
    const SherpaOnnxOfflineStream *stream = SherpaOnnxCreateOfflineStream(g_recognizer);
    SherpaOnnxAcceptWaveformOffline(stream, wave->sample_rate, wave->samples,
                                    wave->num_samples);
    SherpaOnnxDecodeOfflineStream(g_recognizer, stream);
    const SherpaOnnxOfflineRecognizerResult *result =
        SherpaOnnxGetOfflineStreamResult(stream);
    if (result && result->text) text = result->text;
    SherpaOnnxDestroyOfflineRecognizerResult(result);
    SherpaOnnxDestroyOfflineStream(stream);
  }
  SherpaOnnxFreeWave(wave);
  Respond(fd, 200, "OK", "application/json",
          "{\"text\":\"" + JsonEscape(text) + "\"}");
}

void HandleTts(int fd, const std::string &body) {
  if (!g_tts_ready) {
    Respond(fd, 503, "Service Unavailable", "application/json",
            "{\"error\":\"tts model not loaded\"}");
    return;
  }
  std::string text = JsonString(body, "text");
  if (text.empty()) {
    Respond(fd, 400, "Bad Request", "application/json",
            "{\"error\":\"missing text\"}");
    return;
  }
  // "voice" arrives as a Kokoro speaker id — integer, possibly quoted.
  int sid = static_cast<int>(JsonNumber(body, "voice", 0));
  float speed = static_cast<float>(JsonNumber(body, "speed", 1.0));
  if (speed <= 0.0f || speed > 4.0f) speed = 1.0f;

  const SherpaOnnxGeneratedAudio *audio = nullptr;
  {
    std::lock_guard<std::mutex> lock(g_tts_mutex);
    SherpaOnnxGenerationConfig cfg;
    memset(&cfg, 0, sizeof(cfg));
    cfg.silence_scale = 0.2f;
    cfg.sid = sid;
    cfg.speed = speed;
    audio = SherpaOnnxOfflineTtsGenerateWithConfig(g_tts, text.c_str(), &cfg,
                                                   nullptr, nullptr);
  }
  if (!audio || audio->n <= 0) {
    if (audio) SherpaOnnxDestroyOfflineTtsGeneratedAudio(audio);
    Respond(fd, 500, "Internal Server Error", "application/json",
            "{\"error\":\"generation failed\"}");
    return;
  }
  int64_t wav_size = SherpaOnnxWaveFileSize(audio->n);
  std::vector<char> wav(static_cast<size_t>(wav_size));
  SherpaOnnxWriteWaveToBuffer(audio->samples, audio->n, audio->sample_rate, wav.data());
  SherpaOnnxDestroyOfflineTtsGeneratedAudio(audio);
  RespondWav(fd, wav);
}

void HandleConnection(int fd) {
  std::string method, path, body;
  if (ReadRequest(fd, &method, &path, &body)) {
    if (method == "GET" && path == "/health") HandleHealth(fd);
    else if (method == "POST" && path == "/stt") HandleStt(fd, body);
    else if (method == "POST" && path == "/tts") HandleTts(fd, body);
    else Respond(fd, 404, "Not Found", "application/json", "{\"error\":\"not found\"}");
  }
  ::close(fd);
}

}  // namespace

int main(int argc, char *argv[]) {
  int port = 8091;
  for (int i = 1; i + 1 < argc; ++i) {
    if (!strcmp(argv[i], "--port")) port = atoi(argv[++i]);
    else if (!strcmp(argv[i], "--stt-dir")) g_stt_dir = argv[++i];
    else if (!strcmp(argv[i], "--tts-dir")) g_tts_dir = argv[++i];
    else if (!strcmp(argv[i], "--threads")) g_threads = atoi(argv[++i]);
  }
  if (g_stt_dir.empty() && g_tts_dir.empty()) {
    fprintf(stderr,
            "[media_daemon] WARNING: neither --stt-dir nor --tts-dir given — "
            "serving 503 forever (serve-first: not exiting).\n");
  }

  // Serve-first: socket up before any model I/O.
  int server_fd = ::socket(AF_INET, SOCK_STREAM, 0);
  if (server_fd < 0) { perror("socket"); return 1; }
  int opt = 1;
  setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
  sockaddr_in addr{};
  addr.sin_family = AF_INET;
  addr.sin_addr.s_addr = inet_addr("127.0.0.1");  // loopback only, never 0.0.0.0
  addr.sin_port = htons(static_cast<uint16_t>(port));
  if (::bind(server_fd, reinterpret_cast<sockaddr *>(&addr), sizeof(addr)) < 0) {
    perror("bind");
    return 1;
  }
  if (::listen(server_fd, 8) < 0) { perror("listen"); return 1; }
  fprintf(stderr, "[media_daemon] serving http://127.0.0.1:%d (stt=%s tts=%s)\n",
          port, g_stt_dir.empty() ? "-" : g_stt_dir.c_str(),
          g_tts_dir.empty() ? "-" : g_tts_dir.c_str());

  std::thread(LoaderThread).detach();

  while (true) {
    int client = ::accept(server_fd, nullptr, nullptr);
    if (client < 0) continue;
    HandleConnection(client);  // requests served serially; models are the bottleneck
  }
}
