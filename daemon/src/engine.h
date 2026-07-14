#pragma once
#include <string>
#include <vector>
#include <functional>
#include <memory>

struct EngineConfig {
    std::string model_path;          // qnn_context_binary (.bin)
    std::string tokenizer_path;      // tokenizer.json (HF format)
    int max_seq_len = 2048;
    float default_temperature = 0.7f;
    int default_max_tokens = 2048;
};

struct GenerateRequest {
    std::string prompt;
    float temperature = 0.7f;
    int max_tokens = 2048;
    bool stream = true;
    // Base64 JPEG, present when NpuClient.kt's streamImage() is used. Vision lives in
    // THIS daemon alongside the LLM (session-16 decision) — no separate vision process.
    // Not yet wired to a VLM decode path; see Engine::generate's guard.
    std::string image_b64;
};

// Callback: (token_text, token_index, is_done) → return false to abort
using TokenCallback = std::function<bool(const std::string&, int, bool)>;

class Engine {
public:
    explicit Engine(const EngineConfig& config);
    ~Engine();

    bool load();
    bool is_ready() const;
    void generate(const GenerateRequest& req, TokenCallback cb);

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;
};
