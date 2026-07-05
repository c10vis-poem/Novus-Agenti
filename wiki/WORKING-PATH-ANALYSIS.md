# Why the App Still Isn't a Working Assistant — and the Evidence-Based Fix Path

> Written 2026-07-04 (session 16, app track) after reading, end to end, the
> reference repos the operator attached to this workspace:
> `llama.cpp-npu` (the arXiv:2509.23324 paper repo), `EdgeAIApp-ExecuTorch`,
> `snapdragon-npu-llm`, `ai-hub-models` (**official Qualcomm**), `nexa-sdk`
> (geniex fork, org copy), `off-grid-ai-mobile`, `qcom-build-utils`,
> `mlc-llm`, plus `wiki/RESEARCH-CROSSREF.md` and the paper itself.
> Every claim below cites where it comes from. No guesses.

---

## 1. The actual state

The APK now installs and stays up (PR #12 fixed the multi-process
EncryptedSharedPreferences crash-loop and packed `ort_engine` +
`libonnxruntime.so` into the APK). But an assistant app with no model and no
serving runtime is a UI shell: `NpuClient` POSTs to
`http://127.0.0.1:8080/api/v1/generate` and nothing will ever answer it,
because the model that daemon is supposed to serve does not exist and cannot
exist in its current design. That is the brokenness. It is not another app
bug.

## 2. Why the current model pipeline cannot produce a working assistant

Five independent blockers, each fatal on its own. Ordered by depth:

### 2.1 Job 8 keeps failing where it always fails
Job `6a488c52d235f6e43ae5a5a3` (attempt 8): ONNX export now succeeds fully
(the `grid_thw` fix works), then the QAI Hub **vision-encoder** compile
(`jpeler3og`) dies with **exit code 14, "Conversion to context binary
failed"**. This is the same class of HTP failure as the original exit-14,
now at the compile stage, after `--quantize_io`.

### 2.2 The no-KV-cache design is disqualifying even if compile succeeded
`compile_qwen3_5_9b.py` exports the decoder with `use_cache=False`
("stateless prefill", per CLAUDE.md's own constraints table). Qualcomm's
official LLM pipeline (`ai-hub-models/tutorials/llm/onboarding.md`) compiles
**two graphs per model part — AR-128 (prompt processor) and AR-1 (token
generator) — with transposed KV-cache inputs/outputs, then LinkJobs them
into shared-weights QNN context binaries** consumed by the Genie runtime.
Without a KV cache, generating each token re-runs the full-context graph:
at max_seq=4096 that is seconds-per-token on any hardware. There is no
version of "working chat assistant" downstream of `use_cache=False`.

### 2.3 The size is over the platform's hard limit for the chosen runtime
The Hexagon cDSP is a **32-bit processor with a 32-bit (4 GB) virtual
address space**. Sources: the paper §7.2.2 (why they put `lm_head` on CPU);
`llama.cpp-npu/README.md` Known Issues #1 ("we recommend using models below
4B... QNN uses multiple NPU sessions to avoid this issue"). Qwen3.5-9B at
W4A16 is **5.4–5.7 GB of weights** (this repo's own size envelope) — it
cannot fit a single NPU session, and this repo pins
`QNN_GRAPH_CONFIG_MAX_CONTEXTS=1` by design. 8–9B models are possible on
Hexagon **only** via the official multi-part, multi-context Genie route
(that is how `ai-hub-models` ships `llama_v3_1_8b_instruct`). A 9B
single-context `ort_engine` is at odds with the vendor's own architecture.

### 2.4 Calibration is garbage-in
Job 8's log: dataset load failed (`Invalid HF URI 'hf://datasets/wikitext@…'`)
→ silent fallback to **synthetic random tokens** as the W4A16 calibration
set. Qualcomm quantizes these models with AIMET on a **40–80 GB datacenter
GPU** (`qwen2_5_vl_7b_instruct/README.md` says 80 GB VRAM for 8B-class).
Random-token calibration would produce a low-quality model even on a
successful compile.

### 2.5 The daemon tokenizer garbles everything anyway
Session 16 audit of `daemon/src/tokenizer.cpp`: no GPT-2 byte-to-unicode
mapping (spaces/newlines silently dropped on encode, emitted as literal
`Ġ`/`Ċ` on decode), `merges` parsed but never applied, JSON escapes never
unescaped. Identical failure *class* to EdgeAIApp-ExecuTorch's documented
v1.1 hash-tokenizer bug: output that looks like a broken model but is a
broken tokenizer.

### 2.6 The model itself is beyond everyone's envelope
The most ambitious multimodal LLM Qualcomm itself ships for phones is
**Qwen2.5-VL-7B** — with a 504×336 vision input, 128-token prompt chunks,
context capped at 2048, QNN ≥ 2.45, and model-internal monkey-patches done
by the vendor's own team. Nobody — not AI Hub, not nexa/geniex, not
llama.cpp-npu, not ExecuTorch — has a recipe for a 9.65B `qwen3_5`
deepstack-vision model on a phone NPU. This repo has been attempting a
vendor-scale onboarding project as a side quest.

## 3. What the attached references actually prove works

| Route | Evidence | Model ceiling | Effort from here |
|---|---|---|---|
| **A. Official Genie bundles** (`ai-hub-models` + `ai-hub-apps/tutorials/llm_on_genie`) | `qwen3_4b`, `qwen3_4b_instruct_2507`, `llama_v3_2_3b` published, `genie_compatible: true`, precompiled per-chipset context binaries downloadable; Genie runtime ships `genie-t2t-run` + an HTTP service | 8B (multi-context linked) · multimodal: Qwen2.5-VL-7B | Medium: QAIRT SDK runtime libs + bundle download, new `genie_engine`-style daemon (the thing this repo deliberately ripped out — it was the right tool) |
| **B. geniex / nexa-sdk** (org fork `M0DU14R-SYSx-inc/nexa-sdk`, Android AAR in flight per `HANDOFF-android-aar.md`) | Runs GGUF via llama.cpp **hybrid per-tensor HTP+CPU scheduler** (`ggml-hexagon`) or QAIRT `.bin` NPU models; measured **~27 tok/s decode, ~200 ms TTFT** on Qwen3-1.7B-Q8 (X Elite); OpenAI-compatible server | ~4B single-session; Qwen3-VL-4B GGUF exists for vision | Medium-low: AAR/binaries + point `NpuClient` at its server |
| **C. llama.cpp + Hexagon, already half-done IN THIS REPO** | `wiki/NPU-RUNTIME-PATHS.md` Path 2; **`libggml-hexagon.so`, `libllama*.so`, `llama-cli`, `llama-server`, `llama-quantize` were cross-compiled and published to this repo's `latest-debug` release on 2026-06-30** | ≤ ~4B (32-bit VA, single session); Q4_0 / IQ4_NL on HTP, K-quants fall back to CPU | **Low: binaries exist.** Need a ≤4B GGUF, `RUNTIME_FILES` registration, endpoint bridge (`/api/v1/generate` ↔ llama-server API), on-device verify |
| Research floor (`snapdragon-npu-llm`, ExecuTorch .pte) | Qwen3-0.6B at 31 tok/s on a 4-year-old v69 chip; SM8750 bundles include v79 Skels | 0.6–1B demos | Reference only (and its docs contain agent-directed manipulation — treat numbers as single-source) |

Convergent constraint across **all four independent sources** (paper Table 3
+ ExecuTorch source + EdgeAIApp + snapdragon-npu-llm): SM8750 = Hexagon
**v79** (not v75 — fix already in PR #11), and **≤ ~4B params for any
single-session HTP runtime**; 8B+ requires Genie's multi-context linked
binaries.

## 4. The plan

1. **Make the assistant work now — Route C** (days, not weeks; binaries
   already on the release):
   a. Pick **Qwen3-4B-Instruct GGUF, Q4_0 or IQ4_NL** (~2.3–2.5 GB, fits
      phone RAM trivially; K-quants stay off the HTP).
   b. Import `llama-server` + `libggml*`/`libllama*` via
      `ModelImportActivity` / `RUNTIME_FILES` exactly like `ort_engine`.
   c. Bridge the API: either teach `NpuClient` llama-server's
      `/completion` endpoint or put a 20-line shim in the daemon launcher.
      This is the only app-code change required.
   d. Verify on device with `llama-cli` first (one command, per
      `llama.cpp-npu/README.md` Running section), then the served path.
2. **Quality/robustness upgrade — Route A in parallel**: pull the official
   `qwen3_4b` Genie bundle for SM8750 (Samsung Galaxy S25 family device
   target), deploy the Genie HTTP service as a second runtime family.
   Official tokenizer, official quantization, vendor-supported.
3. **Multimodal, when text works**: Qwen2.5-VL-7B via Route A, or
   Qwen3-VL-4B GGUF via Route B. **Not** Qwen3.5-9B DIY.
4. **Compile track disposition**: stop re-running Job 8 as-is — every
   input to it is now known-broken or known-over-limit (§2). If
   Qwen3.5-9B remains the aspiration, the only credible path is the
   official `_shared/llm` pipeline (AIMET on 80 GB GPU, ONNX split,
   AR-1/AR-128, LinkJob, Genie) *plus* onboarding a brand-new model
   architecture — a vendor-scale project to schedule deliberately, not a
   nightly retry.
5. **`ort_engine`/tokenizer**: park it. If it's kept for future
   single-part models, `tokenizer.cpp` needs the byte-level BPE fixes
   (Pending item 3) before any on-device result can be trusted. Routes A/B/C
   all ship correct tokenizers and make the hand-rolled one unnecessary.

## 5. What was already fixed while getting here (this branch)

- CLAUDE.md stale "PENDING" perf claims corrected (all three were already
  implemented and wired — verified against source).
- `daemon/src/tokenizer.cpp` audit (§2.5) documented as Pending item 3.
- sherpa-onnx AAR (a build dependency CI fetched from a third-party GitHub
  release that remote containers cannot reach) is now mirrored to this
  repo's `latest-debug` release via `.github/workflows/mirror-sherpa-aar.yml`
  — local `gradle assembleDebug` works in restricted-egress containers.
