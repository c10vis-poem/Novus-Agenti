# Compile Pipeline — Qwen3.5-9B → ONNX → QAI Hub (fallback path)

> **Status: DORMANT.** This is the fallback path, not the active plan.
> The primary path is the **Q4_0 GGUF served directly by GenieX** (GGML
> backend, no compile step) on Hexagon HTP — see `wiki/GENIEX-DAEMON-PLAN.md`.
> This pipeline only gets triggered if that GGUF path hits a **hard
> failure** on real hardware. Do not run Job 8 pre-emptively "just to have
> it ready" — confirm the hard failure first. See `compile/manifest.yaml`'s
> header for the same disclaimer at the manifest level.

## Why this exists at all

ONNX is not a preference — it's what QAI Hub's own compile workbench
requires as input to produce the **max-performance QAIRT/AI-Engine-Direct
NPU-only** backend bundle (as opposed to GenieX's GGML/llama.cpp backend,
which just eats a GGUF directly, no ONNX/QAI-Hub round-trip needed). If the
GGUF-via-GGML path is fast enough, this entire pipeline is unnecessary.
If it isn't, this is the BYOM route to the higher-performance backend.

## Single-Path Architecture — Qwen3.5-9B

| Layer | What | Status |
|---|---|---|
| **Model** | `Mer0vin8ian/Qwen3.5-9B` — 9.65B params, `qwen3_5` arch. Multimodal via **deepstack vision injection** at decoder layers. NOT a separate encoder pipeline. | Source on HF Hub |
| **ONNX export** | `compile/compile_qwen3_5_9b.py` on HF Jobs `cpu-xl` | M-RoPE fix committed. Job 8 ready but not triggered — on hold pending a hard GGUF failure. |
| **QAI Hub compile** | ONNX → `qnn_context_binary` (W4A16) server-side. | On hold, same condition. |
| **Runtime: `ort_engine`** | ONNX Runtime + QNN Execution Provider on aarch64-android. Serves `POST http://127.0.0.1:8080/api/v1/generate`. Legacy runtime — GenieX is primary. | Scaffolded — `daemon/src/` has a real implementation (`engine.cpp`/`http_server.cpp`/`tokenizer.cpp`/`sampler.h`/`main.cpp`), CI cross-compiles and packages it. Not verified on-device against a real compiled model. |
| **Daemon guardian** | `CliffordService` FGS — CLIFFORD == Watchdog. `START_STICKY`, `specialUse`, 15s CRS recovery loop. | In codebase |
| **Bridge** | `NpuClient.kt` → `POST http://127.0.0.1:8080/api/v1/generate` | In codebase |
| **Agent layer** | `AgentLoop` + tools | In codebase |

## Size Envelope (Hard Caps)

| | Size | Notes |
|---|---|---|
| Target | **5.5 GB** | Shoot for this |
| Ideal ceiling | 6.0 GB | Acceptable |
| Redline | **7.0–7.2 GB** | Non-negotiable |
| W4A16 at max_seq=4096 | ~5.7 GB | ✓ inside ideal |
| W4A16 at max_seq=2048 | ~5.4 GB | ✓ inside target |

## Hexagon HTP Constraints (for this pipeline specifically)

| Constraint | Applied as |
|---|---|
| RoPE fold (no FP16 Sin/Cos on Hexagon) | `make_folded_rope_forward` + `_patched_apply_rotary_pos_emb` |
| Static shapes | batch=1, MAX_SEQ_LEN fixed |
| `--disable_fusion` | In `COMPILE_OPTIONS_BASE` |
| `--bias_as_int32` | In `COMPILE_OPTIONS_BASE` |
| Scratch: 16 MiB | `--scratch_size_mib 16` |
| Dynamic tensor: 64 MiB (canonical) | `--max_dynamic_tensor_size_mib 64` — stays at 64 until empirically verified |
| Single NPU context | `QNN_GRAPH_CONFIG_MAX_CONTEXTS=1` |
| Stateless prefill | `use_cache=False` in `HtpDecodeWrapper` |

## Job 8 Trigger Command (do not run until the GGUF path has hard-failed)

> The URL below intentionally still says `scripts/compile_qwen3_5_9b.py`,
> not `compile/compile_qwen3_5_9b.py` — it fetches from the `lf615p` branch
> (the dormant compile track), which still has the pre-reorg `models/`+
> `scripts/` layout. This branch's `models/`→`scripts/`→`compile/` merge
> was never applied there. Verify the path is still correct on that branch
> before actually triggering Job 8 — don't assume this URL is current
> without checking.

```bash
hf jobs uv run --flavor cpu-xl --timeout 2h \
  --with torch --with transformers --with onnx --with onnxruntime --with onnxscript \
  --with qai-hub --with datasets --with numpy --with huggingface_hub --with accelerate \
  --secrets HF_TOKEN --secrets QAI_HUB_API_TOKEN \
  -e MODEL_ID=Mer0vin8ian/Qwen3.5-9B -e PUBLISH_HF=1 -e OUTPUT_DIR=/tmp \
  https://raw.githubusercontent.com/c10vis-poem/Novus-Agenti/claude/project-scope-review-lf615p/scripts/compile_qwen3_5_9b.py
```

`SKIP_VISION` is NOT set — all three artifacts attempted.

Results (success or failure) go in `wiki/JOB_EXECUTION_LOG.md`, not here.

## Hard rules specific to this pipeline

- No CPU fallback for this model (NPU or nothing).
- Do NOT set `SKIP_VISION=1` in the default trigger command.
- `--max_dynamic_tensor_size_mib` stays at **64** until empirically verified.
- Branch: `claude/project-scope-review-lf615p`, PR #4.
