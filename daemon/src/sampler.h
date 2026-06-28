#pragma once
#include <random>
#include <vector>
#include <algorithm>
#include <cmath>

// CPU-side sampling — runs after logits return from DSP. ~1ms overhead.
class Sampler {
public:
    explicit Sampler(float temperature = 0.7f, int top_k = 50, float top_p = 0.9f)
        : temperature_(temperature), top_k_(top_k), top_p_(top_p),
          rng_(std::random_device{}()) {}

    int sample(const float* logits, int vocab_size) {
        if (temperature_ <= 0.0f) return argmax(logits, vocab_size);

        // Temperature scaling
        scores_.resize(vocab_size);
        for (int i = 0; i < vocab_size; ++i)
            scores_[i] = logits[i] / temperature_;

        // Top-k filter
        indices_.resize(vocab_size);
        for (int i = 0; i < vocab_size; ++i) indices_[i] = i;

        int k = std::min(top_k_, vocab_size);
        std::partial_sort(indices_.begin(), indices_.begin() + k, indices_.end(),
            [this](int a, int b) { return scores_[a] > scores_[b]; });
        indices_.resize(k);

        // Softmax over top-k
        float max_score = scores_[indices_[0]];
        float sum = 0.0f;
        probs_.resize(k);
        for (int i = 0; i < k; ++i) {
            probs_[i] = std::exp(scores_[indices_[i]] - max_score);
            sum += probs_[i];
        }
        for (int i = 0; i < k; ++i) probs_[i] /= sum;

        // Top-p (nucleus) filter
        float cumsum = 0.0f;
        int cutoff = k;
        for (int i = 0; i < k; ++i) {
            cumsum += probs_[i];
            if (cumsum >= top_p_) { cutoff = i + 1; break; }
        }
        probs_.resize(cutoff);
        indices_.resize(cutoff);

        // Renormalize
        sum = 0.0f;
        for (float p : probs_) sum += p;
        for (float& p : probs_) p /= sum;

        // Sample
        std::discrete_distribution<int> dist(probs_.begin(), probs_.end());
        return indices_[dist(rng_)];
    }

private:
    float temperature_;
    int top_k_;
    float top_p_;
    std::mt19937 rng_;
    std::vector<float> scores_;
    std::vector<int> indices_;
    std::vector<float> probs_;

    static int argmax(const float* data, int n) {
        return static_cast<int>(
            std::max_element(data, data + n) - data);
    }
};
