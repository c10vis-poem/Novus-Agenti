#pragma once
#include <string>
#include <vector>
#include <unordered_map>
#include <memory>

// Minimal BPE tokenizer that reads HuggingFace tokenizer.json
class Tokenizer {
public:
    explicit Tokenizer(const std::string& path);
    ~Tokenizer();

    bool load();
    std::vector<int64_t> encode(const std::string& text) const;
    std::string decode(const std::vector<int64_t>& ids) const;
    int64_t eos_token_id() const;
    int64_t bos_token_id() const;
    int vocab_size() const;

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;
};
