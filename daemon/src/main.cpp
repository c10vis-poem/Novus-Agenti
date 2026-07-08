#include "engine.h"
#include "http_server.h"
#include <iostream>
#include <string>
#include <cstdlib>
#include <csignal>
#include <thread>

// ort_engine — on-device LLM inference daemon for Hexagon HTP v75
//
// Loads a qnn_context_binary via ONNX Runtime + QNN Execution Provider,
// serves HTTP at 127.0.0.1:8080 matching the NpuClient.kt wire protocol.
//
// Usage:
//   ort_engine --model /path/to/qwen3_5_9b_unified.bin \
//              --tokenizer /path/to/tokenizer.json \
//              --port 8080 --max-seq-len 2048
//
// Deployed via DaemonLauncher.kt (sh -T- detach, reparented to init).

static HttpServer* g_server = nullptr;

static void signal_handler(int sig) {
    std::cerr << "[ort_engine] caught signal " << sig << ", shutting down\n";
    if (g_server) g_server->stop();
}

static std::string get_arg(int argc, char** argv, const std::string& flag, const std::string& def) {
    for (int i = 1; i < argc - 1; ++i) {
        if (flag == argv[i]) return argv[i + 1];
    }
    // Check environment variable fallback
    std::string env_key = flag.substr(2); // strip --
    for (auto& c : env_key) { if (c == '-') c = '_'; c = toupper(c); }
    const char* env = std::getenv(env_key.c_str());
    if (env) return env;
    return def;
}

int main(int argc, char** argv) {
    std::cerr << "[ort_engine] Novus Agenti · Hexagon HTP v75 inference daemon\n";

    std::string model_path   = get_arg(argc, argv, "--model",       "/storage/emulated/0/Download/qwen3_5_9b_unified.bin");
    std::string tok_path     = get_arg(argc, argv, "--tokenizer",   "/storage/emulated/0/Download/tokenizer.json");
    int port                 = std::stoi(get_arg(argc, argv, "--port", "8080"));
    int max_seq              = std::stoi(get_arg(argc, argv, "--max-seq-len", "2048"));

    std::cerr << "  model:     " << model_path << "\n"
              << "  tokenizer: " << tok_path << "\n"
              << "  port:      " << port << "\n"
              << "  max_seq:   " << max_seq << "\n";

    EngineConfig cfg;
    cfg.model_path = model_path;
    cfg.tokenizer_path = tok_path;
    cfg.max_seq_len = max_seq;

    Engine engine(cfg);

    // ── Crash-loop fix ────────────────────────────────────────────────────────
    // Load the model on a BACKGROUND thread and start the HTTP server IMMEDIATELY,
    // regardless of whether the load succeeds.
    //
    // Previously main() did `if (!engine.load()) return 1;` — a missing, partial,
    // or invalid model (or a missing libonnxruntime.so / QNN init failure) made the
    // daemon exit BEFORE it ever bound port 8080. The CliffordService watchdog then
    // pinged /health, got connection-refused, assumed "dead", and relaunched it —
    // whereupon the daemon suicided again on load. An unbreakable crash loop that
    // could never cold-start, because there was nothing to serve in the first place.
    //
    // Now the daemon ALWAYS binds :port and serves. Until the model is ready,
    // Engine::is_ready() is false, so /health returns 503 and /generate returns
    // "[engine not ready]" (Engine::generate already guards this internally). A live
    // process serving 503 tells the watchdog "alive but not ready" instead of "dead",
    // so it stops relaunching. No model → no endpoint → but NO thrash, and the moment
    // a valid model loads, /health flips to 200 and the watchdog activates NpuClient.
    std::thread loader([&engine]() {
        if (engine.load()) {
            std::cerr << "[ort_engine] model loaded — /health now 200\n";
        } else {
            std::cerr << "[ort_engine] model load FAILED — daemon STAYS UP serving /health 503.\n"
                         "             This is intentional: no crash loop. Import a valid\n"
                         "             qnn_context_binary, then restart the daemon to retry.\n";
        }
    });
    loader.detach();

    // Parse generate request JSON (minimal — matches NpuClient.kt contract)
    auto parse_request = [](const std::string& body) -> GenerateRequest {
        GenerateRequest req;
        // Minimal JSON parse — find key fields
        auto extract_string = [&](const std::string& key) -> std::string {
            auto pos = body.find("\"" + key + "\"");
            if (pos == std::string::npos) return "";
            auto colon = body.find(':', pos);
            auto quote1 = body.find('"', colon + 1);
            auto quote2 = body.find('"', quote1 + 1);
            if (quote1 == std::string::npos || quote2 == std::string::npos) return "";
            return body.substr(quote1 + 1, quote2 - quote1 - 1);
        };
        auto extract_float = [&](const std::string& key, float def) -> float {
            auto pos = body.find("\"" + key + "\"");
            if (pos == std::string::npos) return def;
            auto colon = body.find(':', pos);
            try { return std::stof(body.substr(colon + 1)); }
            catch (...) { return def; }
        };
        auto extract_int = [&](const std::string& key, int def) -> int {
            auto pos = body.find("\"" + key + "\"");
            if (pos == std::string::npos) return def;
            auto colon = body.find(':', pos);
            try { return std::stoi(body.substr(colon + 1)); }
            catch (...) { return def; }
        };

        req.prompt = extract_string("prompt");
        req.temperature = extract_float("temperature", 0.7f);
        req.max_tokens = extract_int("max_tokens", 2048);
        return req;
    };

    HttpServer server(port,
        [&engine, &parse_request](const std::string& body, StreamCallback cb) {
            auto req = parse_request(body);
            engine.generate(req, [&cb](const std::string& token, int idx, bool done) {
                return cb(token, idx, done);
            });
        },
        [&engine]() { return engine.is_ready(); }
    );

    g_server = &server;
    std::signal(SIGTERM, signal_handler);
    std::signal(SIGINT, signal_handler);

    server.run();
    std::cerr << "[ort_engine] shutdown complete\n";
    return 0;
}
