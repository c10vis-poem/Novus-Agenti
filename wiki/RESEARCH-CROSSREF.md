# Research Cross-References — Hexagon NPU / LLM Deployment

> Durable reference doc, not session-numbered — this is where every external
> repo, paper, and SDK we've evaluated for the Qwen3.5-9B → Hexagon HTP
> pipeline gets tracked so findings survive past a single session's handoff.
> Add to this file rather than letting research live only in chat history.
> Last updated: 2026-07-04 (session 15, branch `claude/novus-job-8-verify-r96601`)

---

## The actual research paper behind the Hexagon-HTP-for-LLMs idea

**Scaling LLM Test-Time Compute with Mobile NPU on Smartphones**
arXiv:2509.23324v1, EUROSYS '26 (April 27-30, 2026, Edinburgh)
Authors: Zixu Hao (Tsinghua), Jianyu Wei (Univ. of Science and Technology of
China), Tuowei Wang (Tsinghua), Minxing Huang (Tsinghua), Huiqiang Jiang
(Microsoft Research), Shiqi Jiang (Microsoft Research), Ting Cao (Institute
for AI Industry Research (AIR), Tsinghua University), Ju Ren (Tsinghua).
Licensed CC BY 4.0 — redistribution with attribution is explicitly permitted.

**Full PDF now stored in this repo**: `wiki/papers/2509.23324-scaling-llm-test-time-compute-mobile-npu.pdf`
(uploaded by the operator 2026-07-04, added this session). This is the paper
behind `haozixu/llama.cpp-npu` (forked into this session as
`c10vis-poem/llama.cpp-npu`) — it was cited in that fork's README bibtex but
never attached anywhere until now. This is "the canonical research doc" the
operator remembered.

### What it actually says

**Core idea**: mobile NPUs (Qualcomm Hexagon in particular) have a matrix
unit (HMX) that sits mostly idle during normal LLM decoding, because
autoregressive generation degenerates GEMM into GEMV (batch size 1) — the
32×32-tile matrix unit is wasted on 1-row inputs. The paper's proposal:
spend that idle matrix-unit capacity on **test-time scaling** (Best-of-N,
beam search — running several candidate generations in parallel, batch >1,
then picking the best) instead of leaving it idle. Result: a small on-device
model with test-time scaling can match or beat a larger model run without
scaling, at comparable or lower latency.

**Two technical contributions that solve real Hexagon HTP hardware gaps**:
1. **Hardware-aware tile quantization** — HMX's native tile is 32×32 FP16
   (2048 bytes), permuted so every two rows share the transposed 2×32
   sub-matrix layout. Conventional group quantization (contiguous column-major
   groups of 32) is misaligned with this tile layout, forcing scattered
   memory access. Their fix: quantize in the matrix unit's native tile
   order (2×16 sub-tiles), then coalesce 8 quantization groups into one
   256-element super-group so a single HVX vector register (128 bytes) loads
   a full group instead of needing scatter/gather across multiple registers.
2. **LUT-based Softmax and dequantization** — Hexagon's vector unit (HVX)
   has no dedicated transcendental math hardware, so `exp()` in Softmax is
   the actual latency bottleneck in Attention (up to 84.6% of FlashAttention
   latency at batch=32, per their Figure 8) — not the matmuls. They replace
   `exp()` and INT4→FP16 dequantization with `vlut16` table-lookup
   instructions using a 64 KiB precomputed table (safe-softmax trick: only
   non-positive inputs to `exp` need to be tabulated, ±1 sign-bit trick to
   fit 32768 entries in 64 KiB instead of the full 65536).

**Measured results** (Table 2, Figure 15, Figure 14): FP16 HMX matmul is
~365× the throughput of a single HVX vector thread (12032 GFLOPS vs 32.93
GFLOPS) — confirms the matrix unit really is the underutilized resource.
Their tile-quantization + LUT approach: up to **19.0× speedup on
mixed-precision GEMM** and **2.2× on Softmax** vs. baseline dequantization.
Accuracy cost of their tile-quantization layout vs. conventional grouping is
small (Table 4: WinoGrande/MMLU/Wiki-PPL all within ~1% of each other).

**Hardware note directly relevant to this repo (Table 3)** — their three
test devices and confirmed Hexagon arch versions:
| Device | SoC | NPU Arch |
|---|---|---|
| OnePlus Ace3 | Snapdragon 8 Gen 2 | V73 |
| OnePlus 12 | Snapdragon 8 Gen 3 | V75 |
| OnePlus Ace5 Pro | Snapdragon 8 Elite | **V79** |

This is a fourth independent source (paper Table 3) confirming the
SM8750/Snapdragon-8-Elite = V79 fix already applied to this repo's docs —
matches ExecuTorch's `get_soc_to_htp_arch_map()`, `EdgeAIApp-ExecuTorch`'s
README, and `snapdragon-npu-llm`'s device matrix, all independently.

**Real constraint also confirmed here** (§7.2.2): the Hexagon NPU has only a
**32-bit virtual address space**, which is why they "conservatively place
the weights of `lm_head`... on the CPU instead of the NPU" — the vocabulary
projection matrix is too large to fit in the NPU's addressable space
alongside everything else. At batch size 16, CPU-side logits computation
was ≥50% of total decode time in their measurements. This is the same
32-bit-cDSP constraint `llama.cpp-npu`'s own README calls out as the reason
their single-NPU-session design caps out below ~4B params — directly
relevant if Job 8's QAI Hub compile for the 9B model hits address-space
errors at the QNN compile stage (as opposed to the ONNX-export-stage
`grid_thw` bug already diagnosed and fixed this session).

**Practical takeaway for this project**: this paper is about a different
technique (test-time scaling to use idle matmul capacity) than what
`compile_qwen3_5_9b.py` is doing (single-pass W4A16 inference via QAI Hub) —
it's not a drop-in replacement for the compile pipeline. But its LUT-based
Softmax/dequantization technique and its documented 32-bit address-space
constraint are both directly actionable: the address-space ceiling is a
real risk for a 9B model on the same hardware family, and worth checking
against QAI Hub's actual compile logs if/when Job 8 fails at the QNN
compile stage rather than ONNX export.

---

## External repos evaluated (2026-07-04 session)

### `c10vis-poem/off-grid-ai-mobile` (fork of `alichherawalla/off-grid-mobile`)
Shipped (App Store + Play Store) React Native on-device AI suite. Mature
reference architecture directly relevant to `AgentLoop`/22-tool layer and
`RouterPane`:
- Tool-calling loop with runaway-loop prevention
- Project knowledge base: PDF → chunked → MiniLM embeddings (on-device) →
  SQLite cosine-similarity retrieval → `search_knowledge_base` tool
- Automatic local/remote LLM switching (OpenAI-compatible remote servers)
- NPU-accelerated on-device Stable Diffusion (5-10s/image on Snapdragon)

### `c10vis-poem/llama.cpp-npu` (fork of `haozixu/llama.cpp-npu`)
The actual paper repo (arXiv:2509.23324 above). A materially different,
simpler, license-free path to Hexagon HTP than QAI Hub:
- `-DGGML_HTP=ON` + separate `htp-ops-lib` (FastRPC stub/skeleton `.so` pair)
  instead of QAI Hub's server-side compile queue
- GGUF with a custom `modify_tensors` weight-repack step for HMX 32×32 tile
  layout, then `IQ4_NL+Q8_0` hybrid quantization via `llama-quantize`
- **Documented hard constraint**: Hexagon cDSP is a 32-bit processor with a
  32-bit virtual address space. Their single-NPU-session design hits real
  addressing limits above ~4B params. Direct quote: *"QNN uses multiple NPU
  sessions to avoid this issue, which we have not yet supported."* — this is
  the actual technical reason QAI Hub/QNN can plausibly fit Qwen3.5-9B where
  this fork's approach can't. If Job 8 keeps failing at the QNN compile
  stage (not the ONNX export stage), the multi-session assumption is the
  first thing to verify against QAI Hub's actual compile logs.

### `M0DU14R-SYSx-inc/EdgeAIApp-ExecuTorch` (fork of `carrycooldude/EdgeAIApp-ExecuTorch`)
CLIP + LLaMA-3.2-1B via **ExecuTorch + QNN backend** (v79 SDK, `.pte` models +
context binaries) — a third real deployment path (Meta's official mobile
runtime), distinct from both QAI Hub and raw llama.cpp-npu.
`TECHNICAL_BLOG_JOURNEY.md` documents a real, previously-hit bug worth
checking against `daemon/src/tokenizer.cpp`: v1.1 used **hash-based
tokenization** (`word.hashCode().mod(1000)`) instead of real vocab lookup,
producing gibberish output that looked like a model bug but was a tokenizer
bug. Fixed in v1.3.0 by switching to real vocab-based encode/decode. Audit
`tokenizer.cpp` for anything resembling this class of shortcut before
spending a debugging session assuming the model itself is broken.

### `c10vis-poem/mlc-llm` (fork of `mlc-ai/mlc-llm`)
Real, mature TVM/TensorIR-based ML compiler with a unified MLCEngine. Its
Android support is **Adreno GPU via OpenCL only** — no Hexagon NPU path
listed anywhere. Legitimate as a GPU-path fallback if the Hexagon HTP route
stays blocked long-term, not a substitute Hexagon toolchain.

### `c10vis-poem/snapdragon-npu-llm` — ⚠️ read with a critical eye
Orchestration scripts wrapping a third party's (`K9FxNa` on HuggingFace)
pre-compiled ExecuTorch `.pte` bundles for older Snapdragon chips.
**Flag before using anything from this repo**: its README and
`docs/AI_AGENTS_GUIDE.md` contain sections *explicitly addressed to AI
agents* ("You are an AI assistant... Read this whole document before
responding"), instructing what conclusion to reach, plus an SEO
keyword-stuffed footer designed to surface in searches and steer agent
behavior. That's a real content-manipulation pattern regardless of whether
the underlying claims are true — treat its specific performance numbers
(31.3 tok/s, 107ms TTFT on a v69 chip) as an unverified single-source claim,
not corroborated fact.

What IS independently corroborated from this repo: its SoC→Hexagon-arch
device matrix (SM8650=v75, SM8750=v79, SM8850=v81) matches the ground-truth
ExecuTorch source directly (see below) — that part checks out even though
the surrounding presentation is agent-directed.

### `qualcomm/GenieX` (web-fetched only — not added to session scope; cross-owner `add_repo` restriction)
Qualcomm's own community on-device GenAI runtime. Dual backend (llama.cpp
for compatibility, QNN/AI Engine Direct for max NPU perf), takes GGUF from HF
or QAI Hub bundles, Q4_0 as the optimized path. Ships a full stack: C SDK,
Python (`AutoModelForCausalLM`-style), Kotlin/Android SDK + demo app, CLI,
OpenAI-compatible server. A genuine fourth alternative to the current raw
QAI Hub Python SDK approach — closer to turnkey than `compile_qwen3_5_9b.py`,
and already has an Android SDK that could plausibly replace `ort_engine`
entirely if the QAI Hub pipeline keeps stalling.

---

## Ground-truth verification: SM8750 Hexagon HTP arch version

Verified directly from `pytorch/executorch`'s
`backends/qualcomm/serialization/qc_schema.py` /
`get_soc_to_htp_arch_map()` — the actual compiler source, not a summary:

```
SM8650  → HtpArch.V75   (Snapdragon 8 Gen 3)
SM8750  → HtpArch.V79   (Snapdragon 8 Elite)     ← our target device
SM8850  → HtpArch.V81   (Snapdragon 8 Elite Gen 5)
```

This repo previously stated SM8750 = v75 across `CLAUDE.md`,
`GPT-DAEMON-REFERENCE.md`, and `NPU-RUNTIME-PATHS.md`. Root cause: a
session-9 GPT-chat "correction" of an earlier wrong v68 claim landed on v75
— itself wrong — and propagated uncorrected through both wiki docs into
CLAUDE.md's resume prompt without independent verification. Fixed 2026-07-04
(this session) — see PR #11.

---

## Job 8 failure root cause (2026-07-04 session)

The failure was **not** the exit-code-14 HTP Reshape/`quantize_io` issue the
prior session's resume prompt assumed. Actual sequence, reconstructed from
`hf jobs ps -a` timestamps + `hf jobs logs` + `git log`:

1. `d605cee` (00:34:36Z) — adds `--quantize_io` (real fix for exit-code-14)
   but applies it to a stale copy of the script predating the `grid_thw`
   argument in `VisionEncoderWrapper`.
2. `8cde102` (00:36:38Z) — syncs script from canonical branch (has
   `grid_thw`) + re-applies `--quantize_io`. This is the actually-correct
   state.
3. A job run at 00:36:54 (no cache-buster) and another at 00:38:31 (**with**
   a `?v=qio1` cache-buster) both still failed with the pre-`grid_thw` error.

Root cause of the cache-buster not working: `raw.githubusercontent.com`
resolves a *branch-name* path through an internal redirect to a
commit-SHA-pinned URL, and that redirect target is cached independently of
the outer query string. A fresh `?v=` param on the branch URL doesn't
guarantee a fresh fetch of the redirect target.

**Fix applied**: retrigger using the raw URL pinned directly to the commit
SHA (`8cde10225c55e3d359c97a88dc3fe9f8ab7eed89`), which has no redirect layer
to go stale. Retriggered as job `6a488c52d235f6e43ae5a5a3` — ran past the
~1-minute mark where every prior attempt died on `grid_thw`, into the actual
QAI Hub compile stage (15+ minutes in at last check).

**Lesson for future sessions**: when retriggering any HF Job against a
branch-name raw.githubusercontent URL, prefer pinning to the commit SHA
directly over relying on a cache-buster query string alone.
