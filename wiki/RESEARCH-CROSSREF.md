# Research Cross-References — Hexagon NPU / LLM Deployment

> Durable reference doc, not session-numbered — this is where every external
> repo, paper, and SDK we've evaluated for the Qwen3.5-9B → Hexagon HTP
> pipeline gets tracked so findings survive past a single session's handoff.
> Add to this file rather than letting research live only in chat history.
> Last updated: 2026-07-04 (session 15, branch `claude/novus-job-8-verify-r96601`)

---

## The actual research paper behind the Hexagon-HTP-for-LLMs idea

**Scaling LLM Test-Time Compute with Mobile NPU on Smartphones**
arXiv:2509.23324 (2025)
Authors: Zixu Hao, Jianyu Wei, Tuowei Wang, Minxing Huang, Huiqiang Jiang,
Shiqi Jiang, Ting Cao, Ju Ren — several MSRA (Microsoft Research Asia)
affiliated, plus Tsinghua University.

This is the paper behind `haozixu/llama.cpp-npu` (forked into this session as
`c10vis-poem/llama.cpp-npu`). It was never attached to this repo directly —
only cited in that fork's README bibtex. If you're looking for "the canonical
research doc," this is it. No PDF or paper file lives in this repo; only the
citation does, here and in the fork's README.

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
