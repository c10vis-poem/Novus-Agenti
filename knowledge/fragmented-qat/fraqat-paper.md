Source: Drive file ID `10uJWwhRSMI_nrgWKJTi9coOc-rIV7NRj` ("Copy of 2510.14823v1.pdf") — research paper (arXiv:2510.14823v1 [cs.CV], 16 Oct 2025).

# FraQAT: Quantization Aware Training with Fractional Bits

**Authors**: Luca Morreale, Alberto Gil C. P. Ramos, Malcolm Chadwick, Mehdi Noroozi, Ruchika Chavhan, Abhinav Mehrotra, Sourav Bhattacharya — Samsung AI Center, Cambridge, UK.

> Note: all benchmark numbers below (FID, CLIP-FID, ImageReward, latency figures, GPU-hour counts, etc.) are the paper's own reported results and have not been independently verified.

## Abstract

State-of-the-art (SOTA) generative models have demonstrated impressive capabilities in image synthesis or text generation, often with a large capacity model. However, these large models cannot be deployed on smartphones due to the limited availability of on-board memory and computations. Quantization methods lower the precision of the model parameters, allowing for efficient computations, e.g., in INT8. Although aggressive quantization addresses efficiency and memory constraints, preserving the quality of the model remains a challenge.

To retain quality in previous aggressive quantization, the authors propose a new fractional bits quantization (FraQAT) approach. The novelty is a simple yet effective idea: progressively reduce the model's precision from 32 to 4 bits per parameter, and exploit the fractional bits during optimization to maintain high generation quality. FraQAT yields improved quality on a variety of diffusion models, including SD3.5-Medium, Sana, PixArt-Σ, and FLUX.1-schnell, while achieving 4–7% lower FID than standard QAT. Finally, the authors deploy and run Sana on a Samsung S25U, which runs on the Qualcomm SM8750-AB Snapdragon 8 Elite Hexagon Tensor Processor (HTP).

**Figure 1 callout**: FraQAT is a Quantization Aware Training (QAT) technique that grants generative models high fidelity at a fraction of the training time required. Large text-to-image (T2I) models quantized with FraQAT (W4A8) achieve 16% lower FID score than the state of the art (shown across Sana 600M, SD3.5-M, and Flux-schnell).

## 1. Introduction

Over the past few years, generative models have made impressive progress in synthesizing high-quality images [1, 2, 3] and texts [4, 5]. Such a breakthrough is partly achieved by enlarging the model's size, e.g., Diffusion Transformer (DiT) models with over 10 billion (10B) parameters are increasingly common. However, larger models require significantly more resources, hence higher inference-time or latency, even for inference. This increase is particularly problematic for deploying these models on resource-limited devices, e.g., smartphones, thus limiting their wide-scale usage.

A well-established approach to mitigate these resource constraints is quantization: by shifting parameters from 32 bits to a lower precision, e.g., 4 bits, the model's weight-allocation footprint in its computational graph is significantly reduced. While past quantization research aimed mostly at decreasing model size, low-precision hardware support, such as NPUs on smartphones, drives researchers to further decrease inference latency. For example, latency gains from reduced data movement are boosted by native support for low-precision operations, e.g., using 4-bit weights and 8-bit activations (W4A8). Although initially few devices offered support for these operations, modern hardware manufacturers readily offer low-precision operations across devices: W4A8 in Qualcomm Snapdragon HTP [6], INT8/BF16/FP16 in Intel CPUs [7], Block FP16 in AMD CPUs [8], and FP8/FP4 in NVIDIA GPU H100/H200 [9], to name a few.

The advantages of deploying cloud-quality generative models on-device are multi-fold: it preserves users' privacy while offering a low-latency experience. For service providers, it reduces operating costs by pushing compute from expensive servers to users' personal devices as well as avoiding violating country-specific privacy regulations. This work targets mobile deployment, and restricts itself to W4A8 given its ubiquitous availability across devices.

Quantization approaches fall under two main categories: Post Training Quantization (PTQ) and Quantization-Aware-Training (QAT). PTQ creates a low-precision model from a high-precision pre-trained model using a small calibration dataset. Recent progress in PTQ research has resulted in W4A32 and W8A32 high-quality quantized models from pre-trained SANA [10], SANA 1.5 [2], and SANA-Sprint [11]. Mixed-precision W4A32 and W16A32 approaches like SVDQuant [12] have also yielded high-quality quantized models from pre-trained FLUX.1-schnell. In essence, PTQ is ideal for cases where access to a large training dataset or compute cluster is limited.

Despite its success, PTQ requires careful data selection [13]. For example, a poorly selected calibration dataset may manifest in poor prompt adherence or exhibit color shifts during deployment. Instead, Quantization-Aware-Training (QAT) optimizes weights in lower precision to boost the overall model's performance [14, 15, 16]. In general, QAT approaches yield better results, at lower precision, when a large training dataset or compute cluster is available. Nonetheless, quantized models suffer from a quality loss compared to the original FP32 model.

The authors propose fractional bits quantization (FraQAT) to bridge the quality gap between the original and the quantized model. Inspired by Curriculum Learning [17], their training process progressively increases the quantization complexity, i.e., gradually lowers parameter precision, while replicating the original model's output. FraQAT reduces outliers, stabilizes training and yields improved prompt adherence and image generation quality (Section 2.2). They apply FraQAT to the linear layers of SOTA generative models as they contain the majority of the parameters, and empirically demonstrate the advantages of the proposed techniques on diffusion models (Sections 4.1, 4.2). In terms of image quality, FraQAT achieves 16% lower FID than SOTA QAT. To address computational costs, they perform an outlier analysis (Section 4.3), and selectively train a subset of the model's layers. Finally, they quantize and deploy a model on a Samsung S25U, running on Qualcomm SM8750-AB Snapdragon 8 Elite Hexagon Tensor Processor (HTP) (Section 4.4).

## 2. Method

### 2.1 Quantization Preliminaries

The goal of quantization is to approximate — in dynamic or static finite precision — internal model operations, such as operations within linear layers `x × W` where `x ∈ R^(B×m)` and `W ∈ R^(m×n)`. Depending on hardware support, the quantization operation `W_b` on a matrix `W` to `b` bits can be expressed with narrower range as:

```
Q(W)_b := round( (2^(b-1) - 1) / max_{i,j}|[W]_{i,j}| · W )  ∈ {-2^(b-1), ..., 2^(b-1) - 1}
S(W)_b := max_{i,j}|[W]_{i,j}| / (2^(b-1) - 1)  ∈ R+
W_b := S(W)_b · Q(W)_b                                                    (1)
```

or with wider range as:

```
Q(W)_b := round( 2^b · (W - w_min) / (w_max - w_min) )  ∈ {0, ..., 2^b - 1}
S(W)_b := (w_max - w_min) / 2^b  ∈ R+
W_b := S(W)_b · Q(W)_b + w_min                                            (2)
```

where `w_min := min_{i,j}[W]_{i,j}` and `w_max := max_{i,j}[W]_{i,j}`. Most simply for (1), matrix multiplications can be rewritten as: `x_bx · W_bW = (S(x)_bx · S(W)_bW)(Q(x)_bx · Q(W)_bW)` where `bx` and `bW` may differ. Therefore, matrix multiplication `x_bx W_bW` can be reduced to the multiplication of two floats `S(x)_bx S(W)_bW` and matrix multiplication of two integer matrices `Q(x)_bx Q(W)_bW`.

Dynamic quantization refers to the case where `w_min`/`w_max` are computed at runtime, per sample, based on the input. In static quantization, `w_min`/`w_max` are pre-computed and shared across all samples. Dynamic quantization, especially when applied to activations, allows robust handling of outliers since each sample range is computed to maximize representability. Static quantization is more restrictive and generates more outliers, making the quantization problem strictly harder. Edge devices, such as smartphones, only support static quantization, while GPUs support both.

Activations are often quantized through a look-up table mapping from a `2^b`-sized partition of the input range into a fixed number of quantized output values, e.g., the previous layer output `x`. In general, weights and activations may be quantized to different precisions, upcasted to the same precision before computation and downcasted after computation. The paper makes the number of bits in weights and activations explicit with subscripts, e.g., `x32` refers to a 32-bit approximation of `x`.

Due to restricted address spaces in most mobile accelerators, it is critical to decrease weights precision aggressively, especially in large vision or language models (e.g., 12B-parameter models), otherwise these models cannot even be placed on the target devices. However, naively lowering the weight's precision from FP32 to INT4 causes severe degradation in generated results — exacerbated by lowering activation precision, as required by integer-only accelerators (most often to INT8 for reduced generation latency). At a high level, the quality-degradation phenomenon is attributed to outliers in both activations and weights due to training. The overall challenge of quantization is to approximate the original network's behavior while lowering the precision:

```
x·W ≈ x8·W4.                                                              (3)
```

### 2.2 Fractional Quantization-Aware-Training

Intuitively, QAT approaches — including the proposed FraQAT — handle outliers, both in weights and activations, by shifting parameters to quantization centroids within or towards adjacent bins, hence re-distributing weights in a more compact space. Consequently, the further apart bins are, the harder the optimization problem. The authors further speculate that it is slower to optimize for lower precisions (INT4 vs FP32) as the gap between two adjacent representable numbers is much larger. This can be observed in Figure 2, where the loss is higher for lower precisions — outliers appear gradually as precision decreases.

**Figure 2 (Bit vs Loss)**: as precision (number of bits) is reduced, the average knowledge-distillation loss increases — the gap between student and teacher widens. From a quantization perspective, this implies outliers incrementally affect the student model. (Curves shown for SD3.5-M, Sana 600M, PixArt-Σ, Flux-schnell.)

**Figure 3 (Classic QAT vs FraQAT)**: Classic QAT first computes the loss at the lower precision, then propagates it back to the original precision and optimizes the weights — resulting in coarse and noisy gradients. Fractional Quantization Aware Training relies on intermediate precisions (from INT8 to INT4 as training progresses) to incrementally adjust to weight distributions; parameters smoothly shift between bins thanks to the finer gradients available at intermediate precision.

To address this issue, the authors take inspiration from Curriculum Learning [17] literature: they progressively increase the complexity of the task during optimization by gradually lowering weights' precision while approximating the full-precision model's output. This is achieved by two key designs: first, FraQAT leverages weights from pre-trained models. Second, FraQAT continuously steps between discrete quantization ranges to exploit the fact that Eq. (1)–(2) are purely a software construct — hence it is possible to span any continuous (not just discrete) precision `b ∈ [32, 1] ∈ R`.

Coupled together, these concepts establish FraQAT as a faster and higher-quality QAT scheme: Fractional Quantization Aware Training. Given a model, FraQAT progressively lowers the precision — first coarsely between FP32 and INT8, then finely from INT8 to INT4 — stepping through intermediate fractional bits during training (Figure 3, Algorithm 1). As training progresses, outliers gradually appear (Figure 2) and are addressed. By optimizing at fractional-bit precision in a curriculum fashion, FraQAT allows weights to move to stable configurations, yielding higher-quality samples and reducing training time. Throughout the entire training process, FraQAT keeps all activation quantization constant (INT8).

As training progresses, this progressive lowering of precision smoothly moves the weights' distribution, facilitating quantization (cf. Figure 3). It is even possible to set `b = 5.5`. Although half-bit precisions have no direct meaning, in practice they bridge the gap in the range of representable numbers between two precisions: `INT6 ∈ [-32, 31]`, `INT5.5 ∈ [-22, 21]`, `INT5 ∈ [-16, 15]`. In other words, half-bit precisions reduce the distance between adjacent bins, speeding up convergence without ad-hoc hyper-parameters such as learning rate.

The proposed FraQAT approach is generally applicable to any model and quantization level. Given the widespread usage of DiT and MM-DiT blocks in SOTA T2I models, the presentation focuses on DiT models. Since model-size limits must first be met for any on-device placement, FraQAT quantizes linear layers as they contain the bulk of the parameters of DiT models (99.9%). FraQAT targets the most aggressive W4A8 quantization, as it allows for a wider range of models to fit edge accelerators with the lowest generation latency — though the technique itself is applicable to any precision.

#### Algorithm 1 — Fractional Quantization Aware Training

```
Input:  Pre-trained model M_{W32A32}, dataset D, loss function L,
        quantization schedule B (e.g., {8, 5, 4.5, 4}), optimizer O
Output: Quantized model M_{W4A8}

1:  M_{WbA8} ← M_{W32A32}
2:  for b in B do
3:      M_{WbA8} ← QUANTIZE_LINEAR_LAYER(M_{WbA8}, W_bA8)
4:      for batch in D do
5:          O_{WbA8}  ← FORWARD(M_{WbA8}, batch)
6:          O_{W32A32} ← FORWARD(M_{W32A32}, batch)
7:          l ← L( stop_grad(O_{W32A32}), O_{WbA8} )
8:          OPTIMIZE(O, l, M_{WbA8})
9:      end for
10: end for
11: return M_{WbA8}
```

## 3. Related Works

Large diffusion models are the de-facto framework for image generation [18, 19, 20, 21]. Large Language Models (LLMs) show human-like abilities with text [4, 22, 23]. However, quality and diversity come at a cost: these models have a huge number of parameters and cannot be hosted on an on-device NPU without some form of quantization.

**Tackling computational complexity.** Diffusion models' computational complexity has two major sources: the number of denoising passes and the conditioning mechanism. The former can be addressed by distilling the model to few or a single pass [24, 25]; the latter by modeling the latent-noise space [26] to decrease the number of function evaluations. Despite the success of these approaches, a major bottleneck remains: the memory required for inference. Quantization aims to preserve the original model's quality when moving to lower precision — thus saving memory and enabling deployment.

**Quantization-Aware-Training.** QAT methods optimize model weights at lower precision [15, 27], aiming to recover the original performance. Early approaches [15, 27] study QAT on ResNet for classification: starting from low-bit precision (`b = 2` or `4`), weight quantization is progressively reduced [27] or selected at random [15]. Although [15, 27] closely relate to FraQAT, they (i) focus on classification networks, (ii) ignore the gap with full-precision models and the hierarchical nature of different precisions by starting from a low-bit quantization, and (iii) aim to get models at different precisions. In parallel, Fracbits [28] introduces bit-width optimization relying on a non-standard quantization formula for fractional bits — bit-widths are regularized to achieve the desired precision, followed by a binary search and fine-tuning process to finalize weights. However, this focuses on average bit length across layers, obtaining lower bits in some layers at the cost of higher bits in others, which may not map to readily available hardware; Fracbits also focuses on classification rather than generative tasks.

More recently, MatryoshkaQAT [14] exploits the nested structure of a number's byte representation to encode LLM weights at different precisions (8, 4, and 2); joint training at the three precisions results in a multi-precision model. Parallel to this work, Liu et al. [29] extend [30] and discover that models quantized to lower than 4 bits develop a different representation from the original models. Finally, [31], based on the Teaching Assistant distillation framework [32], quantize LLM models to W1A1. Similar to this work, the authors use a progressive strategy, however limited to W1A4 → W1A2 → W1A1, where intermediate models (W1A2) are used as teachers. Combined with a series of techniques (gradient clipping, elastic binarization, etc.) to stabilize optimization, they achieve a binary quantized model — but since the quantization is binary, the model is not deployable to edge devices. This work shows progressive quantization is enough to quantize a model that *can* be deployed on edge devices.

Related to diffusion models, Bitfusion [16] combines different QAT techniques (distillation and fine-tuning) to convert SD1.5 [19] to 1.99 bits. Wang et al. [33] selectively fine-tune SD1.5 to handle activation distribution. BinaryDM [34] applies a multi-stage QAT approach to model quantization. None of these techniques showcase low-bit quantization of large-scale DiT models such as SD3.5-M [1] (2.2B) or FLUX.1-schnell [3] (12B) — this work is presented as the first QAT approach applied to such models.

**Post-Training Quantization.** SmoothQuant [35] proposes a PTQ approach injecting a smoothing factor in linear layers to reduce the impact of outliers in LLMs. AWQ [36] and MobileQuant [37] extend this approach to lower precision to W4A8, enabling an LLM to run on-device. These works have been extended to DiT models with specific focus on timesteps: PTQ4DiT [38] builds a calibration dataset by sampling timesteps before quantizing the diffusion model; DiTAS [39] proposes a temporal-aggregated smoothing technique combined with LoRA and grid search to reduce quantization errors of small DiT networks with W4A8 quantization; QuEST [33] achieves W4A4 quantization through layer-specific PTQ fine-tuning; Q-DiT [40], inspired by [36, 41, 37], combines fine-grained group quantization with a novel automatic allocation algorithm to account for weights' spatial variance. Most recently, SVDQuant [12] and FBQuant [42] have shown impressive preservation of image-generation quality when quantizing FLUX.1-schnell [3] to W4A16, relying on a low-rank approximation of the original weights and a residual branch to absorb outliers.

## 4. Experiments

**Models.** The evaluation focuses on recent text-to-image (T2I) models given the increasing interest in lowering their computational requirements. The authors assess the approach over 4 diffusion models: SD3.5-Medium [1], Sana [10], PixArt-Σ [43], and FLUX.1-schnell [3]. These models span a wide range of parameters (0.6B–12B) and architectural innovations (linear and non-linear attention, DiT, MM-DiT, etc.).

In all experiments, they start from a pre-trained W32A32 model and, through FraQAT, reduce it to W4A8. The student is bootstrapped at INT8 and optimized to replicate its FP32 counterpart — minimizing the initial gap between teacher (FP32) and student (INT8). After `T` epochs, the model's precision is lowered and optimization continues, repeating until precision reaches 4 bits. Since a fake-quantization process is used, arbitrary precisions with no hardware support (e.g., INT4.5) can be emulated. Unless stated otherwise, all experiments follow the same progression: `8 → 7 → 6 → 5.5 → 5 → 4.75 → 4.5 → 4.25 → 4`, targeting linear layers. In all cases a W4A8 model is distilled via knowledge-distillation loss, using dynamic quantization. The approach is applicable to any quantization precision (e.g., W2A8) and to static quantization.

**Baselines.** Compared against SOTA PTQ techniques: DiTAS [39] (W4A8) and SVDQuant [12] (W4A16), using publicly available code, calibrated on the training dataset. To further validate FraQAT, the authors implement a vanilla QAT (vQAT) — W4A8 quantization applied to all linear layers, optimized with the same loss as FraQAT — and an SVDQuant-like QAT (SVDQAT), which injects a LoRA-like layer in all linear layers (as in [12]) and optimizes both the low-rank and residual branch, almost doubling the parameter count. Naive quantization results (Dynamic Q.) are also reported via torchao.

**Datasets.** All models are trained/calibrated on the YE-POP dataset, split 97.5%/2.5% train/validation. Quantized models are evaluated on the PixArt-Σ Evaluation dataset and the MidJourney HQ (MJHQ) Evaluation dataset. Training and evaluation generate 512×512 images.

**Metrics.** Image quality is measured with ImageReward (IR) [45]; feature-distribution disparity between generated samples of the quantized vs. original model is measured with FID [46] and CLIP-FID [47] — lower FID/CLIP-FID indicates higher similarity to the original model.

### 4.1 Quantitative Evaluation

Table 1 compares FraQAT across the five models/methods, two SOTA QAT approaches, and three PTQ techniques, evaluated on two test datasets. Due to memory requirements, some techniques could not be applied to Flux-schnell [3] (12B model).

SVDQuant, developed and optimized for Sana, PixArt-Σ, and FLUX.1-schnell, achieves lower performance on SD3.5-Medium (worse FID and CLIP-FID on both test datasets). Results for Dynamic Quantization and DiTAS are mixed on both datasets: DiTAS outperforms Dynamic Quantization on PixArt-Σ but is overall worse for SD3.5-Medium and Sana — these models have different architectures (DiT, MM-DiT, linear attention), suggesting DiTAS is sensitive to model family. Among QAT approaches, SVDQAT consistently outperforms vanilla QAT (vQAT) — arguably the increased parameter count (LoRA + residual branch) better copes with lower precision. FraQAT outperforms even the strongest QAT baseline developed (SVDQAT), with overall higher gains for SD3.5-Medium and PixArt-Σ on both test datasets.

**Table 1 — Qualitative/quantitative evaluation (FID / CLIP-FID / ImageReward) on PixArt-Σ Evaluation dataset and MJHQ dataset:**

*PixArt-Σ Evaluation dataset*

| Method | Precision | SD3.5 Medium FID↓ | SD3.5 Medium CLIP-FID↓ | SD3.5 Medium IR↑ | Sana 600M FID↓ | Sana 600M CLIP-FID↓ | Sana 600M IR↑ | PixArt-Σ FID↓ | PixArt-Σ CLIP-FID↓ | PixArt-Σ IR↑ | Flux-schnell FID↓ | Flux-schnell CLIP-FID↓ | Flux-schnell IR↑ |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Dynamic Q. | W4A8 | 9.36 | 2.08 | 0.56 | 2.22 | 0.24 | 0.57 | 13.35 | 6.19 | 0.35 | 8.17 | 1.13 | -0.73 |
| DiTAS | W4A8 | 27.93 | 13.77 | 0.41 | 12.87 | 4.58 | 0.62 | 7.30 | 3.95 | 0.84 | - | - | - |
| SVDQuant | W4A16 | 14.42 | 3.14 | 0.66 | 2.43 | 0.24 | 0.60 | 6.80 | 2.02 | 0.79 | 2.26 | 0.36 | 0.84 |
| SVDQAT | W4A8 | 2.57 | 0.28 | 0.80 | 1.93 | 0.13 | 0.48 | 5.38 | 1.48 | 0.76 | - | - | - |
| vQAT | W4A8 | 2.67 | 0.31 | 0.78 | 2.13 | 0.16 | 0.45 | 7.00 | 2.52 | 0.79 | 3.40 | 0.66 | 0.87 |
| **FraQAT** | W4A8 | **2.54** | **0.27** | **0.82** | **2.17** | **0.19** | **0.48** | **4.48** | **1.07** | **0.79** | **2.55** | **0.30** | **0.86** |

*MJHQ dataset*

| Method | Precision | SD3.5 Medium FID↓ | SD3.5 Medium CLIP-FID↓ | SD3.5 Medium IR↑ | Sana 600M FID↓ | Sana 600M CLIP-FID↓ | Sana 600M IR↑ | PixArt-Σ FID↓ | PixArt-Σ CLIP-FID↓ | PixArt-Σ IR↑ | Flux-schnell FID↓ | Flux-schnell CLIP-FID↓ | Flux-schnell IR↑ |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Dynamic Q. | W4A8 | 10.29 | 2.11 | 0.65 | 2.40 | 0.28 | 0.63 | 15.04 | 5.55 | 0.44 | 8.66 | 1.24 | -0.90 |
| DiTAS | W4A8 | 32.04 | 14.06 | 0.41 | 12.91 | 5.59 | 0.68 | 8.63 | 4.07 | 1.04 | - | - | - |
| SVDQuant | W4A16 | 15.10 | 3.06 | 0.78 | 2.48 | 0.25 | 0.62 | 6.95 | 1.71 | 0.99 | 2.41 | 0.41 | 0.96 |
| SVDQAT | W4A8 | 2.85 | 0.32 | 0.91 | 2.04 | 0.16 | 0.53 | 5.83 | 1.44 | 0.96 | - | - | - |
| vQAT | W4A8 | 3.01 | 0.37 | 0.89 | 2.13 | 0.20 | 0.47 | 7.38 | 2.12 | 0.99 | 3.56 | 0.73 | 0.99 |
| **FraQAT** | W4A8 | **2.78** | **0.32** | **0.96** | **2.34** | **0.24** | **0.50** | **4.95** | **1.054** | **0.97** | **2.55** | **0.39** | **0.99** |

### 4.2 Qualitative Evaluation

Figure 4 depicts, for each of the four models: the original model, one PTQ representative (SVDQuant — chosen for its popularity over DiTAS/Dynamic Quantization), one QAT alternative (vQAT — chosen over SVDQAT for its overall popularity), and FraQAT. Images in each row share the same seed and prompt (sourced from MJHQ [44]).

As expected from a PTQ approach, SVDQuant under-performs when generating certain high-frequency image details — especially visible when multiple faces are present, as shown in the first row. QAT improves high-frequency image details but generates significantly different images than the original model for the same prompt/seed. FraQAT preserves both high-frequency details and generates images closest to the original model across all baselines.

### 4.3 Outlier Analysis

Activation outliers disrupt the quantization process by introducing artifacts or biases. By analyzing outliers across different models, the authors find that different models produce outliers in different layers — e.g., in SD3.5-M outliers emerge after Feed-Forward (FF) layers, while in PixArt-Σ outliers arise mostly from Attention (Attn) layers (Figure 5). Selectively training specific layers can reduce FraQAT's computational demand while still obtaining a deployable model. The authors analyze the impact of selective training — optimizing only certain layer types (Attn, FF, Transformer Blocks/TF) while the rest of the network is frozen and quantized (W4A8), compared with training the entire network (Full/All).

**Table 2 — Outlier analysis (FID / CLIP-FID on PixArt-Σ evaluation dataset, W4A8):**

| Model | Layer | FID↓ | CLIP-FID↓ |
|---|---|---|---|
| SD3.5-M | FF | 2.23 | 0.23 |
| SD3.5-M | Attn | 2.32 | 0.24 |
| SD3.5-M | TF | 2.49 | 0.28 |
| SD3.5-M | All | 2.54 | 0.22 |
| Sana 600M | FF | 2.18 | 0.17 |
| Sana 600M | Attn | 2.10 | 0.16 |
| Sana 600M | TF | 2.13 | 0.16 |
| Sana 600M | All | 2.17 | 0.19 |
| PixArt-Σ | FF | 5.34 | 1.55 |
| PixArt-Σ | Attn | 6.48 | 2.23 |
| PixArt-Σ | TF | 4.40 | 1.13 |
| PixArt-Σ | All | 4.48 | 1.07 |

Quantitative results show no clear winner — layer preference differs by model architecture — but the authors recommend starting from quantizing Transformer Blocks (TF), as it reduces memory requirements, lowers computational demands, and addresses all outliers.

### 4.4 On-Device Model Deployment

To demonstrate feasibility of on-device deployment, Sana 600M [10] was quantized to W4A8 and deployed on a Samsung S25U (Qualcomm SM8750-AB Snapdragon 8 Elite Hexagon Tensor Processor / HTP). Compared to CPUs/GPUs, integer accelerators support a limited range of precisions and exclusively support static quantization for both weights and activations.

FraQAT supports both dynamic and static quantization paradigms (unlike the baselines used in Section 4.1). To apply FraQAT to Sana 600M with static weight/activation quantization, scale and offset are pre-computed via statistical analysis of the features: 100 random samples are passed through the DiT, feature values per layer are recorded, and used to compute standard deviation and mean (following [48], a 3-σ range is used for inliers). These scale/offset values are then used during QAT; the overall training procedure is otherwise unchanged from Section 4.1.

All linear layers of the quantized model run at W4A8 except the last layer, which runs at W4A16 — a good compromise to preserve quality without impacting latency. The resulting model has a latency of 66ms per forward step, versus 95ms for the same model at W4A16 (the bit-width supported by SVDQuant) — a **30.5% latency improvement**. On-device generated samples were compared with the original model's GPU output (Figure 6); the quantized model produces high-quality pictures resembling both the original model and its GPU version.

## 5. Limitations and Future Work

FraQAT is a step forward compared to SOTA QAT, but — like most QAT techniques — is more computationally expensive than PTQ. Compared with multi-precision LLM SOTA QAT approaches such as MatryoshkaQAT, FraQAT's quantized model is tailored to a single bit precision; multi-precision support is left for future work. The intermediate precision levels used are hand-picked; a future goal is to design an algorithm to select the most impactful precisions automatically.

The training scheme may benefit from regularizers such as weight decay and data augmentation — preliminary regularization tests on Sana 0.6B [10] show weight decay boosts performance by ~10%, though a proper investigation is left to future work. Finally, FraQAT's networks are currently optimized using knowledge distillation only; different losses (e.g., feature and task loss) may further boost image-generation quality.

## 6. Conclusions

FraQAT is a novel Quantization-Aware-Training technique that exploits fractional bits while progressively reducing parameter precision during quantization. This curriculum-learning strategy addresses outliers as they arise at different precisions, achieving more stable and faster training. Evaluated over a variety of SOTA DiT and MM-DiT models, the quantized models achieve superior performance — both qualitatively and quantitatively — compared to SOTA QAT approaches. Such improved quality, if deployed on-device, may boost mobile users' productivity, preserve their privacy, and enable generation of personalized content.

## References

1. S. AI, "Stable diffusion 3.5." https://stability.ai/news/introducing-stable-diffusion-3-5, 2024.
2. E. Xie, J. Chen, Y. Zhao, J. Yu, L. Zhu, C. Wu, Y. Lin, Z. Zhang, M. Li, J. Chen, H. Cai, B. Liu, D. Zhou, and S. Han, "SANA 1.5: Efficient Scaling of Training-Time and Inference-Time Compute in Linear Diffusion Transformer," arXiv:2501.18427, 2025.
3. B. F. Labs, "Flux." https://github.com/black-forest-labs/flux, 2024.
4. A. Grattafiori et al., "The llama 3 herd of models," arXiv preprint arXiv:2407.21783, 2024.
5. G. Team et al., "Gemma 3 technical report," arXiv preprint arXiv:2503.19786, 2025.
6. Q. Snapdragon®, "Snapdragon 8 Elite Mobile Platform." https://docs.qualcomm.com/bundle/publicresource/87-83196-1_REV_D_Snapdragon_8_Elite_Mobile_Platform_Product_Brief.pdf.
7. X. Intel®, "Processors with Performance-Cores (P-Cores)." https://www.intel.com/content/www/us/en/products/details/processors/xeon/xeon6-p-cores.html.
8. A. Ryzen™, "AI 300 Series Processors." https://www.amd.com/content/dam/amd/en/documents/partner-hub/ryzen/amd-ryzen-ai-300-series-vs-qualcomm-snapdragon-x-elite-deck.pdf.
9. N. HGX™, "NVIDIA HGX Platform." https://www.nvidia.com/en-gb/data-center/hgx.
10. E. Xie et al., "SANA: Efficient High-Resolution Image Synthesis with Linear Diffusion Transformers," arXiv:2410.10629, 2024.
11. J. Chen et al., "SANA-Sprint: One-Step Diffusion with Continuous-Time Consistency Distillation," arXiv:2503.09641, 2025.
12. M. Li et al., "SVDQuant: Absorbing Outliers by Low-Rank Components for 4-Bit Diffusion Models," arXiv:2411.05007, 2024.
13. Z. Zhang, Y. Gao, J. Fan, Z. Zhao, Y. Yang, and S. Yan, "Selectq: Calibration data selection for post-training quantization," Machine Intelligence Research, pp. 1–12, 2025.
14. P. Nair, P. Datta, J. Dean, P. Jain, and A. Kusupati, "Matryoshka Quantization," arXiv:2502.06786, 2025.
15. A. Bulat and G. Tzimiropoulos, "Bit-Mixer: Mixed-precision networks with runtime bit-width selection," in ICCV, pp. 5188–5197, 2021.
16. Y. Sui et al., "Bitsfusion: 1.99 bits weight quantization of diffusion model," arXiv:2406.04333, 2024.
17. Y. Bengio, J. Louradour, R. Collobert, and J. Weston, "Curriculum learning," in Proceedings of the 26th annual international conference on machine learning, pp. 41–48, 2009.
18. P. Dhariwal and A. Nichol, "Diffusion models beat gans on image synthesis," NeurIPS vol. 34, pp. 8780–8794, 2021.
19. R. Rombach, A. Blattmann, D. Lorenz, P. Esser, and B. Ommer, "High-Resolution Image Synthesis with Latent Diffusion Models," in CVPR, pp. 10684–10695, 2022.
20. Y. Lipman, R. T. Q. Chen, H. Ben-Hamu, M. Nickel, and M. Le, "Flow Matching for Generative Modeling," arXiv:2210.02747, 2023.
21. I. Gat et al., "Discrete flow matching," NeurIPS vol. 37, pp. 133345–133385, 2024.
22. J. Achiam et al., "Gpt-4 technical report," arXiv preprint arXiv:2303.08774, 2023.
23. G. Team et al., "Gemini: a family of highly capable multimodal models," arXiv preprint arXiv:2312.11805, 2023.
24. T. Salimans and J. Ho, "Progressive distillation for fast sampling of diffusion models," arXiv preprint arXiv:2202.00512, 2022.
25. M. Noroozi, I. Hadji, B. Martinez, A. Bulat, and G. Tzimiropoulos, "You Only Need One Step: Fast Super-Resolution with Stable Diffusion via Scale Distillation," in ECCV, pp. 145–161, Springer, 2025.
26. M. Noroozi, A. G. Ramos, L. Morreale, R. Chavhan, M. Chadwick, A. Mehrotra, and S. Bhattacharya, "Guidance free image editing via explicit conditioning," arXiv preprint arXiv:2503.17593, 2025.
27. Q. Jin, L. Yang, and Z. Liao, "AdaBits: Neural Network Quantization with Adaptive Bit-Widths," in CVPR, pp. 2146–2156, 2020.
28. L. Yang and Q. Jin, "Fracbits: Mixed precision quantization via fractional bit-widths," in Proceedings of the AAAI Conference on Artificial Intelligence, vol. 35, pp. 10612–10620, 2021.
29. Z. Liu et al., "ParetoQ: Scaling Laws in Extremely Low-bit LLM Quantization," arXiv:2502.02631, 2025.
30. M. Nagel, M. Fournarakis, Y. Bondarenko, and T. Blankevoort, "Overcoming Oscillations in Quantization-Aware Training," in ICML, pp. 16318–16330, PMLR, 2022.
31. Z. Liu et al., "Bit: Robustly binarized multi-distilled transformer," NeurIPS vol. 35, pp. 14303–14316, 2022.
32. S. I. Mirzadeh, M. Farajtabar, A. Li, N. Levine, A. Matsukawa, and H. Ghasemzadeh, "Improved knowledge distillation via teacher assistant," in AAAI, vol. 34, pp. 5191–5198, 2020.
33. H. Wang, Y. Shang, Z. Yuan, J. Wu, J. Yan, and Y. Yan, "QuEST: Low-bit Diffusion Model Quantization via Efficient Selective Finetuning," arXiv:2402.03666, 2024.
34. X. Zheng et al., "BinaryDM: Accurate Weight Binarization for Efficient Diffusion Models," arXiv:2404.05662, 2024.
35. G. Xiao, J. Lin, M. Seznec, H. Wu, J. Demouth, and S. Han, "SmoothQuant: Accurate and Efficient Post-Training Quantization for Large Language Models," arXiv:2211.10438, 2024.
36. J. Lin et al., "AWQ: Activation-aware Weight Quantization for On-device LLM Compression and Acceleration," Proceedings of Machine Learning and Systems, vol. 6, pp. 87–100, 2024.
37. F. Tan et al., "MobileQuant: Mobile-friendly Quantization for On-device Language Models," arXiv:2408.13933, 2024.
38. J. Wu, H. Wang, Y. Shang, M. Shah, and Y. Yan, "PTQ4DiT: Post-training Quantization for Diffusion Transformers," arXiv:2405.16005, 2024.
39. Z. Dong and S. Q. Zhang, "DiTAS: Quantizing Diffusion Transformers via Enhanced Activation Smoothing," arXiv:2409.07756, 2024.
40. L. Chen et al., "Q-DiT: Accurate Post-Training Quantization for Diffusion Transformers," arXiv:2406.17343, 2024.
41. Y. Zhao et al., "Atom: Low-bit quantization for efficient and accurate llm serving," Proceedings of Machine Learning and Systems, vol. 6, pp. 196–209, 2024.
42. Y. Liu et al., "FBQuant: FeedBack Quantization for Large Language Models," arXiv:2501.16385, 2025.
43. J. Chen et al., "Pixart-σ: Weak-to-strong training of diffusion transformer for 4k text-to-image generation," in ECCV, pp. 74–91, Springer, 2024.
44. D. Li, A. Kamko, E. Akhgari, A. Sabet, L. Xu, and S. Doshi, "Playground v2.5: Three insights towards enhancing aesthetic quality in text-to-image generation," arXiv preprint arXiv:2402.17245, 2024.
45. J. Xu et al., "Imagereward: Learning and evaluating human preferences for text-to-image generation," NeurIPS vol. 36, pp. 15903–15935, 2023.
46. C. Szegedy, V. Vanhoucke, S. Ioffe, J. Shlens, and Z. Wojna, "Rethinking the inception architecture for computer vision," in CVPR, pp. 2818–2826, 2016.
47. T. Kynkäänniemi, T. Karras, M. Aittala, T. Aila, and J. Lehtinen, "The role of imagenet classes in fréchet inception distance," arXiv preprint arXiv:2203.06026, 2022.
48. R. Wang et al., "Optimizing large language model training using fp4 quantization," arXiv preprint arXiv:2501.17116, 2025.
49. Z. Lin et al., "Evaluating text-to-visual generation with image-to-text generation," arXiv preprint arXiv:2404.01291, 2024.
50. J. Wang, K. C. Chan, and C. C. Loy, "Exploring clip for assessing the look and feel of images," in AAAI, vol. 37, pp. 2555–2563, 2023.
51. G. Team, "Gemma," 2024.
52. C. Raffel et al., "Exploring the limits of transfer learning with a unified text-to-text transformer," Journal of machine learning research, vol. 21, no. 140, pp. 1–67, 2020.
53. C. Clark, K. Lee, M.-W. Chang, T. Kwiatkowski, M. Collins, and K. Toutanova, "Boolq: Exploring the surprising difficulty of natural yes/no questions," arXiv preprint arXiv:1905.10044, 2019.
54. A. Talmor, J. Herzig, N. Lourie, and J. Berant, "Commonsenseqa: A question answering challenge targeting commonsense knowledge," arXiv preprint arXiv:1811.00937, 2018.

## Appendix A — Experimental Evaluation

### A.1 Baselines

For state-of-the-art baselines, the authors rely on code released by the respective authors of SVDQuant (https://github.com/mit-han-lab/deepcompressor) and DiTAS (https://github.com/DZY122/DiTAS), using default parameters. For all approaches, pre-trained models with default resolution 512×512 are used, with baseline configurations changed where needed to match the same model.

### A.2 Hyper-Parameters for QAT

All QAT experiments use FuseAdam as optimizer, optimizing for 25 epochs. All experiments run on AMD MI300X, implemented using PyTorch (https://pytorch.org/), Lightning (https://lightning.ai/docs/pytorch/stable/), and torchao (https://github.com/pytorch/ao), with seed 1234.

**Table 3 — Hyper-parameters:**

| Method | Precision | SD3.5-M lr | SD3.5-M batch size | SD3.5-M low rank | Sana 600M lr | Sana 600M batch size | Sana 600M low rank | PixArt-Σ lr | PixArt-Σ batch size | PixArt-Σ low rank |
|---|---|---|---|---|---|---|---|---|---|---|
| SVDQAT | W4A8 | 10⁻⁵ | 128 | 32 | 10⁻⁶ | 128 | 16 | 10⁻⁶ | 256 | 16 |
| vQAT | W4A8 | 10⁻⁵ | 256 | - | 10⁻⁶ | 128 | - | 10⁻⁶ | 128 | - |
| FraQAT | W4A8 | 10⁻⁶ | 256 | - | 10⁻⁷ | 128 | - | 10⁻⁶ | 128 | - |

For all FraQAT experiments, the schedule in Table 4 is followed.

**Table 4 — Precision schedule:**

| Precision | 8 | 7 | 6 | 5.5 | 5 | 4.75 | 4.5 | 4.25 | 4 |
|---|---|---|---|---|---|---|---|---|---|
| # epochs | 1 | 1 | 1 | 1 | 1 | 2 | 2 | 2 | 14 |

Experiments with this configuration take on average 192 GPUh for Sana, 576 GPUh for PixArt-Σ, and 1008 GPUh for SD3.5-Medium.

### A.3 Qualitative Evaluation

Additional qualitative evaluation on the MJHQ dataset [44] is provided as HTML pages in an accompanying zip file (not included in this text extraction).

### A.4 Quantitative Evaluation

Additional evaluation uses a wider set of metrics: VQA [49] to measure adherence of generated samples to input prompts, and CLIP-IQA [50] to measure image quality.

**Table 5 — Extended qualitative/quantitative evaluation (FID / CLIP-FID / CLIP-IQA / ImageReward / VQA score) on PixArt-Σ Evaluation dataset and MJHQ dataset:**

*PixArt-Σ Evaluation dataset*

| Method | Precision | SD3.5-M FID↓ | CLIP-FID↓ | CLIP-IQA↑ | IR↑ | VQA↑ | Sana 600M FID↓ | CLIP-FID↓ | CLIP-IQA↑ | IR↑ | VQA↑ | PixArt-Σ FID↓ | CLIP-FID↓ | CLIP-IQA↑ | IR↑ | VQA↑ | Flux-schnell FID↓ | CLIP-FID↓ | CLIP-IQA↑ | IR↑ | VQA↑ |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Dynamic Q. | W4A8 | 9.36 | 2.08 | 0.44 | 0.56 | 0.84 | 2.22 | 0.24 | 0.46 | 0.57 | 0.82 | 13.35 | 6.19 | 0.44 | 0.35 | 0.82 | 8.17 | 1.13 | 0.43 | -0.73 | 0.77 |
| DiTAS | W4A8 | 27.93 | 13.77 | 0.47 | 0.41 | 0.82 | 12.87 | 4.58 | 0.45 | 0.62 | 0.82 | 7.30 | 3.95 | 0.46 | 0.84 | 0.86 | - | - | - | - | - |
| SVDQuant | W4A16 | 14.42 | 3.14 | 0.42 | 0.66 | 0.85 | 2.43 | 0.24 | 0.43 | 0.60 | 0.82 | 6.80 | 2.02 | 0.43 | 0.79 | 0.86 | 2.26 | 0.36 | 0.42 | 0.84 | 0.85 |
| SVDQAT | W4A8 | 2.57 | 0.28 | 0.45 | 0.80 | 0.85 | 1.93 | 0.13 | 0.43 | 0.48 | 0.82 | 5.38 | 1.48 | 0.43 | 0.76 | 0.86 | - | - | - | - | - |
| vQAT | W4A8 | 2.67 | 0.31 | 0.44 | 0.78 | 0.85 | 2.13 | 0.16 | 0.43 | 0.45 | 0.81 | 7.00 | 2.52 | 0.45 | 0.79 | 0.85 | 3.40 | 0.66 | 0.41 | 0.87 | 0.86 |
| **FraQAT** | W4A8 | **2.54** | **0.27** | **0.45** | **0.82** | **0.86** | **2.17** | **0.19** | **0.42** | **0.48** | **0.82** | **4.48** | **1.07** | **0.45** | **0.79** | **0.86** | **2.55** | **0.30** | **0.41** | **0.86** | **0.85** |

*MJHQ dataset*

| Method | Precision | SD3.5-M FID↓ | CLIP-FID↓ | CLIP-IQA↑ | IR↑ | VQA↑ | Sana 600M FID↓ | CLIP-FID↓ | CLIP-IQA↑ | IR↑ | VQA↑ | PixArt-Σ FID↓ | CLIP-FID↓ | CLIP-IQA↑ | IR↑ | VQA↑ | Flux-schnell FID↓ | CLIP-FID↓ | CLIP-IQA↑ | IR↑ | VQA↑ |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Dynamic Q. | W4A8 | 10.29 | 2.11 | 0.44 | 0.65 | 0.79 | 2.40 | 0.28 | 0.45 | 0.63 | 0.74 | 15.04 | 5.55 | 0.43 | 0.44 | 0.74 | 8.66 | 1.24 | 0.42 | -0.90 | 0.65 |
| DiTAS | W4A8 | 32.04 | 14.06 | 0.47 | 0.41 | 0.73 | 12.91 | 5.59 | 0.45 | 0.68 | 0.75 | 8.63 | 4.07 | 0.46 | 1.04 | 0.80 | - | - | - | - | - |
| SVDQuant | W4A16 | 15.10 | 3.06 | 0.42 | 0.78 | 0.78 | 2.48 | 0.25 | 0.42 | 0.62 | 0.75 | 6.95 | 1.71 | 0.43 | 0.99 | 0.80 | 2.41 | 0.41 | 0.42 | 0.96 | 0.79 |
| SVDQAT | W4A8 | 2.85 | 0.32 | 0.45 | 0.91 | 0.80 | 2.04 | 0.16 | 0.43 | 0.53 | 0.74 | 5.83 | 1.44 | 0.43 | 0.96 | 0.81 | - | - | - | - | - |
| vQAT | W4A8 | 3.01 | 0.37 | 0.44 | 0.89 | 0.80 | 2.13 | 0.20 | 0.43 | 0.47 | 0.74 | 7.38 | 2.12 | 0.44 | 0.99 | 0.80 | 3.56 | 0.73 | 0.41 | 0.99 | 0.80 |
| **FraQAT** | W4A8 | **2.78** | **0.32** | **0.45** | **0.96** | **0.81** | **2.34** | **0.24** | **0.42** | **0.50** | **0.74** | **4.95** | **1.05** | **0.44** | **0.97** | **0.80** | **2.55** | **0.39** | **0.41** | **0.99** | **0.80** |

Table 5 shows FraQAT outperforms even the strongest QAT baseline developed (SVDQAT), with overall higher gains for SD3.5-Medium and PixArt-Σ on both test datasets.

### A.5 Quantization Schedule

To validate the benefits of the fractional quantization schedule (Table 4), it is compared against an integer-only counterpart (`8 → 7 → 6 → 5 → 4`) and a simpler progressive schedule (`16 → 8 → 4`), all under the same computational budget. Validation loss is measured across training (Figure 7): the integer and simple schedules perform comparably to each other, while the fractional schedule consistently outperforms both, resulting in a sensibly lower validation loss (evaluated on SD3.5-M).

**Figure 7 (Fractional schedule)**: SD3.5-M trained with a simple progressive schedule (16→8→4), an integer schedule (8→7→6→5→4), and a fractional schedule (8→7→6→5.5→5→4.75→4.5→4.25→4). The fractional schedule achieves the lowest validation loss.

## Appendix B — Additional Evaluation

### B.1 Language Model

The method is architecture- and application-agnostic. FraQAT is applied to Gemma2 2B IT [51]: starting from the FP16 (original) model, it is quantized to 4 bits following the same schedule as Appendix A.2. The quantized model (W4A8) is compared against the original.

Training uses a subset of the C4 dataset [52]: 384K samples randomly selected for training, 38.4K for validation. Evaluation is zero-shot on two datasets: BoolQ [53] and CommonsenseQA [54]/Social IQA. Table 6 shows a minimal quality drop when FraQAT is applied to Gemma2 2B IT, demonstrating the technique generalizes beyond vision models to language models.

**Table 6 — Evaluation on Language Models:**

| Model | Precision | CommonSense QA↑ | BoolQ↑ | COQA↑ |
|---|---|---|---|---|
| Original | W16A32 | 0.70 ± 0.01 | 0.76 ± 0.01 | 0.66 ± 0.01 |
| FraQAT | W4A8 | 0.69 ± 0.01 | 0.72 ± 0.01 | 0.70 ± 0.01 |

## Relevance to this project

FraQAT is directly relevant to the Omni Claw / Novus Agenti on-device stack: it is a Samsung AI Center paper demonstrating a QAT technique that reduces generative model precision to W4A8 and deploys it on a Samsung S25U running the Qualcomm Snapdragon 8 Elite Hexagon Tensor Processor (HTP) — the same class of Qualcomm Hexagon NPU hardware referenced for the Rubik Pi 3 (Dragonwing QCS6490 / Hexagon 770) secondary hub described in the Edge AI Hub Integration dossier. The paper's core finding — that a curriculum-style progressive bit-precision schedule (fractional bits, e.g. INT5.5) yields more stable training and 4–7% lower FID than standard QAT at W4A8 — is a candidate technique for any future on-device fine-tuning or quantization work done on the Jetson Orin Nano hub (LoRA fine-tuning role) before deploying models to Hexagon-NPU-based edge devices.
