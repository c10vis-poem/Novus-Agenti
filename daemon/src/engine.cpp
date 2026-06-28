#include "engine.h"
#include "tokenizer.h"
#include "sampler.h"
#include <onnxruntime_cxx_api.h>
#include <cstring>
#include <iostream>
#include <chrono>

struct Engine::Impl {
    EngineConfig config;
    Ort::Env env{ORT_LOGGING_LEVEL_WARNING, "ort_engine"};
    Ort::SessionOptions session_opts;
    std::unique_ptr<Ort::Session> session;
    Tokenizer tokenizer;
    bool ready = false;

    // Pre-allocated buffers for static-shape inference
    std::vector<int64_t> input_ids_buf;
    std::vector<int64_t> attention_mask_buf;
    std::vector<int64_t> position_ids_buf;

    explicit Impl(const EngineConfig& cfg) : config(cfg), tokenizer(cfg.tokenizer_path) {
        input_ids_buf.resize(config.max_seq_len, 0);
        attention_mask_buf.resize(config.max_seq_len, 0);
        position_ids_buf.resize(4 * config.max_seq_len, 0);
    }
};

Engine::Engine(const EngineConfig& config) : impl_(std::make_unique<Impl>(config)) {}
Engine::~Engine() = default;

bool Engine::load() {
    auto& d = *impl_;
    try {
        d.session_opts.SetIntraOpNumThreads(1);
        d.session_opts.SetGraphOptimizationLevel(GraphOptimizationLevel::ORT_ENABLE_ALL);

        // QNN EP: register QNN execution provider for Hexagon HTP
        // ORT loads the qnn_context_binary directly — no ONNX needed at runtime
        std::vector<const char*> qnn_keys = {
            "backend_path",
            "qnn_context_cache_enable",
            "qnn_context_cache_path",
        };
        std::vector<const char*> qnn_vals = {
            "libQnnHtp.so",
            "1",
            d.config.model_path.c_str(),
        };
        auto status = OrtSessionOptionsAppendExecutionProvider(
            d.session_opts, "QNN",
            qnn_keys.data(), qnn_vals.data(), qnn_keys.size());
        if (status) {
            std::cerr << "[engine] QNN EP registration failed\n";
            return false;
        }

        d.session = std::make_unique<Ort::Session>(
            d.env, d.config.model_path.c_str(), d.session_opts);

        if (!d.tokenizer.load()) {
            std::cerr << "[engine] tokenizer load failed\n";
            return false;
        }

        // Warm-up pass: prime Hexagon SRAM caches (~30% faster first inference)
        std::cerr << "[engine] warm-up pass ...\n";
        GenerateRequest warmup;
        warmup.prompt = "Hello";
        warmup.max_tokens = 1;
        generate(warmup, [](const std::string&, int, bool) { return true; });
        std::cerr << "[engine] ready\n";
        d.ready = true;
        return true;
    } catch (const Ort::Exception& e) {
        std::cerr << "[engine] load error: " << e.what() << "\n";
        return false;
    }
}

bool Engine::is_ready() const { return impl_->ready; }

void Engine::generate(const GenerateRequest& req, TokenCallback cb) {
    auto& d = *impl_;
    if (!d.ready) {
        cb("[engine not ready]", 0, true);
        return;
    }

    auto token_ids = d.tokenizer.encode(req.prompt);
    if (token_ids.empty()) {
        cb("[empty prompt]", 0, true);
        return;
    }

    int cur_len = static_cast<int>(token_ids.size());
    if (cur_len >= d.config.max_seq_len) {
        cb("[prompt exceeds max_seq_len]", 0, true);
        return;
    }

    Sampler sampler(req.temperature);
    auto eos_id = d.tokenizer.eos_token_id();

    for (int step = 0; step < req.max_tokens && cur_len < d.config.max_seq_len; ++step) {
        // Fill input buffers
        std::fill(d.input_ids_buf.begin(), d.input_ids_buf.end(), 0);
        std::fill(d.attention_mask_buf.begin(), d.attention_mask_buf.end(), 0);
        for (int i = 0; i < cur_len; ++i) {
            d.input_ids_buf[i] = token_ids[i];
            d.attention_mask_buf[i] = 1;
        }
        // Position IDs: [4, 1, max_seq_len] — text, temporal, height, width all identical
        for (int ch = 0; ch < 4; ++ch) {
            for (int i = 0; i < d.config.max_seq_len; ++i) {
                d.position_ids_buf[ch * d.config.max_seq_len + i] = i;
            }
        }

        // Run inference
        auto memory_info = Ort::MemoryInfo::CreateCpu(
            OrtAllocatorType::OrtArenaAllocator, OrtMemType::OrtMemTypeDefault);

        std::array<int64_t, 2> ids_shape = {1, d.config.max_seq_len};
        std::array<int64_t, 3> pos_shape = {4, 1, d.config.max_seq_len};

        auto ids_tensor = Ort::Value::CreateTensor<int64_t>(
            memory_info, d.input_ids_buf.data(), d.input_ids_buf.size(),
            ids_shape.data(), ids_shape.size());
        auto mask_tensor = Ort::Value::CreateTensor<int64_t>(
            memory_info, d.attention_mask_buf.data(), d.attention_mask_buf.size(),
            ids_shape.data(), ids_shape.size());
        auto pos_tensor = Ort::Value::CreateTensor<int64_t>(
            memory_info, d.position_ids_buf.data(), d.position_ids_buf.size(),
            pos_shape.data(), pos_shape.size());

        const char* input_names[] = {"input_ids", "attention_mask", "position_ids"};
        const char* output_names[] = {"logits"};
        std::array<Ort::Value, 3> inputs = {
            std::move(ids_tensor), std::move(mask_tensor), std::move(pos_tensor)};

        auto outputs = d.session->Run(
            Ort::RunOptions{nullptr},
            input_names, inputs.data(), inputs.size(),
            output_names, 1);

        // Extract logits for last valid position
        auto& logits_tensor = outputs[0];
        auto logits_shape = logits_tensor.GetTensorTypeAndShapeInfo().GetShape();
        int vocab_size = static_cast<int>(logits_shape.back());
        const float* logits_data = logits_tensor.GetTensorData<float>();
        const float* last_logits = logits_data + (cur_len - 1) * vocab_size;

        int next_id = sampler.sample(last_logits, vocab_size);

        if (next_id == eos_id) {
            cb("", step, true);
            return;
        }

        token_ids.push_back(next_id);
        cur_len++;

        auto token_text = d.tokenizer.decode({next_id});
        if (!cb(token_text, step, false)) return; // caller aborted
    }

    cb("", static_cast<int>(token_ids.size()), true);
}
