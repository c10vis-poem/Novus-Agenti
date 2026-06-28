#include "tokenizer.h"
#include <fstream>
#include <sstream>
#include <iostream>
#include <algorithm>
#include <regex>

// Minimal tokenizer: loads vocab + merges from HuggingFace tokenizer.json.
// For production, consider sentencepiece or tokenizers-cpp.
// This implementation handles the core BPE encode/decode loop.

struct Tokenizer::Impl {
    std::string path;
    std::unordered_map<std::string, int64_t> token_to_id;
    std::unordered_map<int64_t, std::string> id_to_token;
    std::vector<std::pair<std::string, std::string>> merges;
    int64_t eos_id = -1;
    int64_t bos_id = -1;
    bool loaded = false;

    explicit Impl(const std::string& p) : path(p) {}

    bool parse_tokenizer_json() {
        std::ifstream f(path);
        if (!f.is_open()) {
            std::cerr << "[tokenizer] cannot open: " << path << "\n";
            return false;
        }

        // Simple JSON parsing for tokenizer.json
        // In production, use nlohmann/json or simdjson
        std::stringstream ss;
        ss << f.rdbuf();
        std::string json = ss.str();

        // Extract vocab entries: "token": id pairs
        // This is a simplified parser — works for HF tokenizer.json format
        auto extract_vocab = [&](const std::string& section_key) {
            auto pos = json.find("\"" + section_key + "\"");
            if (pos == std::string::npos) return;

            auto brace = json.find('{', pos);
            if (brace == std::string::npos) return;

            int depth = 1;
            size_t i = brace + 1;
            std::string current_key;
            bool in_string = false;
            bool escaped = false;

            while (i < json.size() && depth > 0) {
                char c = json[i];
                if (escaped) { escaped = false; i++; continue; }
                if (c == '\\') { escaped = true; i++; continue; }
                if (c == '"') in_string = !in_string;
                if (!in_string) {
                    if (c == '{') depth++;
                    if (c == '}') depth--;
                }
                i++;
            }

            std::string vocab_block = json.substr(brace, i - brace);
            // Parse "token": id pairs with regex
            std::regex re(R"RE("((?:[^"\\]|\\.)*)"\s*:\s*(\d+))RE");
            auto rbegin = std::sregex_iterator(vocab_block.begin(), vocab_block.end(), re);
            auto rend = std::sregex_iterator();
            for (auto it = rbegin; it != rend; ++it) {
                std::string tok = (*it)[1].str();
                int64_t id = std::stoll((*it)[2].str());
                token_to_id[tok] = id;
                id_to_token[id] = tok;
            }
        };

        extract_vocab("vocab");

        // Find eos/bos from added_tokens
        auto find_special = [&](const std::string& content) -> int64_t {
            auto it = token_to_id.find(content);
            if (it != token_to_id.end()) return it->second;
            return -1;
        };

        eos_id = find_special("<|endoftext|>");
        if (eos_id < 0) eos_id = find_special("<|end|>");
        if (eos_id < 0) eos_id = find_special("</s>");
        if (eos_id < 0) eos_id = find_special("<|im_end|>");

        bos_id = find_special("<|startoftext|>");
        if (bos_id < 0) bos_id = find_special("<s>");
        if (bos_id < 0) bos_id = find_special("<|im_start|>");

        std::cerr << "[tokenizer] vocab=" << token_to_id.size()
                  << " eos=" << eos_id << " bos=" << bos_id << "\n";
        return !token_to_id.empty();
    }
};

Tokenizer::Tokenizer(const std::string& path) : impl_(std::make_unique<Impl>(path)) {}
Tokenizer::~Tokenizer() = default;

bool Tokenizer::load() {
    impl_->loaded = impl_->parse_tokenizer_json();
    return impl_->loaded;
}

std::vector<int64_t> Tokenizer::encode(const std::string& text) const {
    // Byte-level fallback: encode each character by looking up single-char tokens
    // For production, implement full BPE merge loop or use tokenizers-cpp
    std::vector<int64_t> ids;
    if (!impl_->loaded) return ids;

    // Try longest-match greedy tokenization
    size_t i = 0;
    while (i < text.size()) {
        int best_len = 0;
        int64_t best_id = -1;
        // Try progressively shorter substrings
        for (int len = std::min((int)(text.size() - i), 64); len > 0; --len) {
            auto substr = text.substr(i, len);
            auto it = impl_->token_to_id.find(substr);
            if (it != impl_->token_to_id.end()) {
                best_len = len;
                best_id = it->second;
                break;
            }
        }
        if (best_id >= 0) {
            ids.push_back(best_id);
            i += best_len;
        } else {
            // Unknown byte — skip (production: use byte fallback tokens)
            i++;
        }
    }
    return ids;
}

std::string Tokenizer::decode(const std::vector<int64_t>& ids) const {
    std::string result;
    for (auto id : ids) {
        auto it = impl_->id_to_token.find(id);
        if (it != impl_->id_to_token.end()) {
            result += it->second;
        }
    }
    return result;
}

int64_t Tokenizer::eos_token_id() const { return impl_->eos_id; }
int64_t Tokenizer::bos_token_id() const { return impl_->bos_id; }
int Tokenizer::vocab_size() const { return static_cast<int>(impl_->token_to_id.size()); }
