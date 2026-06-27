# CLAUDE.md — Novus Agenti / Omni Claw

> **SESSION KICKOFF RULE — NON-NEGOTIABLE:**
> Read THIS FILE completely before any tool call, search, or edit.
> Then read these in order, also completely:
>   1. `wiki/GPT-OSS-Reference.md` — Hexagon HTP constraints from the canonical Drive source. Every compile parameter is rooted here. Do NOT paraphrase this doc anywhere else.
>   2. `wiki/EDGE-MODEL-LISTS.md` — model identity (Qwen3.5-9B primary).
>   3. `models/manifest.yaml` — current build target + size envelope.
>   4. `wiki/SESSION{N}-HANDOFF.md` — latest handoff if present.
> Slash command: type `/memory` in any Claude Code session to load full project context.
> If a session-{N-1} handoff and this file disagree, this file wins; raise the conflict explicitly.

---

## What this is

**Novus Agenti** — "the unprecedented driving force" — fully on-device agentic AI assistant for the Motorola Razr Ultra 2025 (Snapdragon 8 Elite SM8750, Adreno 830, Hexagon HTP v75). Inference runs on the NPU via a detached native daemon. No cloud LLM in the main app runtime. No CPU fallback. Cloud APIs (SambaNova, OpenRouter, etc.) ARE callable from the agent layer via `HttpFetch`; that's the neuromesh contract, not in-app inference.

App package: `com.horizons` (pending rebrand). Codebase lives under the **Omni Claw** banner.

**Project identity:** Novus Agenti · Omni Claw · Cl0vis × Mer0vin6ian production.
**HuggingFace:** `Mer0vin8ian` · **Personal GitHub:** `c10vis-poem` · **Org GitHub:** `M0DU14R-SYSx-inc`.

## Repo policy — non-negotiable

- **`c10vis-poem/Novus-Agenti`** — THE canonical repo. All commits, all pushes, all CI, all artifacts go here. Repo name has a **hyphen** (`Novus-Agenti`), not a dot.
- **`M0DU14R-SYSx-inc/NeuroOmni.Vag-Agenti`** — REFERENCE-ONLY. Never push, commit, or modify. Treat as archived.
- Working branch: `claude/project-scope-review-lf615p`. PR #4 tracks it.

---

## Single-path architecture

There is **one model, one runtime, one pipeline.** No tracks, no placeholders, no fallbacks.

| Layer | What | Status |
|---|---|---|
| **Model** | `Mer0vin8ian/Qwen3.5-9B` (user's duplicate of `Qwen/Qwen3.5-9B`) | 9.65B params. `qwen3_5` arch. Apache 2.0. Multimodal natively via **deepstack vision injection** — vision tokens injected at decoder layers specified by `vision_config.deepstack_visual_indexes`, NOT prepended through a separate encoder stage. |
| **Compile output** | `Mer0vin8ian/qwen3-5-9b-npu-sm8750/qwen3_5_9b_language_decoder.bin` (QAI Hub `qnn_context_binary`) | **Job 8 pending** — M-RoPE shape fix applied (commit `2af893b`). Jobs 6+7 failed at ONNX export with `got 5 and 4` dim error. |
| **Runtime** | **`ort_engine`** daemon — ONNX Runtime + QNN Execution Provider on aarch64-android | binary not yet built. ORT not Genie: the `.bin` is a `qnn_context_binary` which ORT+QNN-EP loads directly. |
| **Daemon launch** | `DaemonLauncher.kt` → `sh -T-` detach + root `oom_score_adj=-1000` + `NpuManager.acquirePerformanceLock(PERF_MODE_HIGH)` (Android 13+, API 33) | partial — `DaemonLauncher` exists, `NpuManager` lock NOT yet wired. |
| **Game SDK boost** | `<uses-feature android:name="android.hardware.game" android:required="true"/>` + `GameManager.setGameMode(GameMode.PERFORMANCE)` at `Application.onCreate` | partial — manifest has `appCategory` + `isGame` on PR #3, missing `uses-feature` and SDK call wiring. |
| **Daemon guardian** | `CliffordService` FGS — CLIFFORD and Watchdog are THE SAME daemon. `:clifford` process, `START_STICKY`, `specialUse` FGS type, 15s CRS recovery loop on daemon PID. | committed on PR #3. Watchdog module should fold into Clifford or be deleted. |
| **Bridge** | `NpuClient.kt` → `POST http://127.0.0.1:8080/api/v1/generate` → SSE-JSON token stream | committed on PR #3. |
| **Agent layer** | `AgentLoop` + 22 tools (20 Android + `HttpFetch` for cloud APIs + `WebSearch`) | committed on PR #3. |

### Size envelope (hard caps)

| | Size | Notes |
|---|---|---|
| Target | **5.5 GB** | what we shoot for |
| Ideal ceiling | 6.0 GB | acceptable |
| Redline | **7.0–7.2 GB** | non-negotiable; refuse anything over |
| Achieved at W4A16 | ~5.0 GB weights + 300 MB metadata + ~512 MB KV cache (max_seq=4096) = **~5.7 GB** total | ✓ inside ideal |
| At max_seq=2048 | ~5.4 GB | ✓ inside target |

### Quantization

**W4A16** — INT4 per-channel weights, FP16 activations. FP16 matches Hexagon HTP native datatype. INT4 weights fit inside the 7 GB redline.

### Hexagon HTP constraints (per `wiki/GPT-OSS-Reference.md`)

Applied at compile time in `scripts/compile_qwen3_5_9b.py`:
- **RoPE fold** — precompute cos/sin to FP16 lookup tables, replace rotary with Gather (no FP16 `Sin`/`Cos` kernel on Hexagon → graph-build abort). Two-pronged: `patch_rope` replaces `rotary_emb.forward`; `_patched_apply_rotary_pos_emb` patched at module level to bypass M-RoPE 5D/4D shape issue.
- **Pre-LN rewrite** — post-LN with FP16 γ/β rejected.
- **GELU tanh approximation** — no FP16 `erf`.
- **Static shapes** — batch=1, MAX_SEQ_LEN compile-time; runtime valid-length scalar.
- **KV cache pre-allocated at MAX_SEQ_LEN** — `QnnTensorUpdate` is buggy >4 MiB.
- **`--disable_fusion`** — MHA fusion only works for static seq + INT8 weights.
- **`--bias-as-int32`** — INT8 bias overflow guard.
- **`partition_override.json`** — Softmax + TopK forced to CPU.
- **`--scratch_size_mib 16`** + **`--max_dynamic_tensor_size_mib 32`**.
- **Single NPU context per daemon** — `QNN_GRAPH_CONFIG_MAX_CONTEXTS=1`.
- **Stateless prefill** — `use_cache=False` in `HtpDecodeWrapper`. Qwen3.5 hybrid attention rejects `DynamicCache`.

---

## State of the Union — 2026-06-27 (session 8)

### Job history

| Job | Error | Fix applied |
|---|---|---|
| Jobs 1–4 | Various load/submodule errors | Resolved |
| Job 5 | `ValueError: has_previous_state can only be called on LinearAttention` | Stateless prefill (`use_cache=False`) |
| Jobs 6–7 | `RuntimeError: Tensors must have same number of dimensions: got 5 and 4` at `modeling_qwen3_5.py:603` | M-RoPE two-pronged shape fix (commit `2af893b`) |
| **Job 8** | **pending** — trigger command below | — |

### Root cause of Jobs 6+7 (documented for next session)

`Qwen3_5TextRotaryEmbedding.forward` routes through `apply_interleaved_mrope`, returning `[3, B, S, D]` (4D). `apply_rotary_pos_emb` does `cos.unsqueeze(1)` → 5D. Multiply with 4D `q_rot` broadcasts to 5D `q_embed`. `cat([q_embed_5D, q_pass_4D])` → `got 5 and 4`.

Fix: (1) `make_folded_rope_forward` returns `[B, S, D]` (3D) by indexing `cos_buf` with 2D `pos_2d`. (2) `apply_rotary_pos_emb` monkey-patched at module level — ignores cos/sin from `rotary_emb`, uses precomputed `[1,1,S,D]` tables broadcasting cleanly with `[B,H,S,D]` q/k.

### Confirmed working
- PR #4 open: https://github.com/c10vis-poem/Novus-Agenti/pull/4
- `scripts/compile_qwen3_5_9b.py` — M-RoPE shape fix committed (`2af893b`).
- `wiki/GPT-OSS-Reference.md` committed.
- `wiki/EDGE-MODEL-LISTS.md` committed.
- HF Hub source: `Mer0vin8ian/Qwen3.5-9B`.
- Tokens: STATIC per user policy; user provides at session start; rotate after build ships.

### Open work
- `ort_engine` daemon binary — not yet built. Needs C++ aarch64-android (ONNX Runtime + QNN EP).
- `NpuManager.acquirePerformanceLock(PERF_MODE_HIGH)` not wired into `CliffordService.kt`.
- `GameManager.setGameMode(GameMode.PERFORMANCE)` not wired into `HorizonsApplication.kt`.
- `<uses-feature android:name="android.hardware.game" android:required="true"/>` missing from `AndroidManifest.xml`.
- `build-apk.yml` CI publish target — must repoint to `${{ github.repository }}`.
- `Watchdog` module — fold into `CliffordService` or delete.
- PRs #2 and #3 still need merging into `main`.

---

## What was ripped out — do NOT reference these

| Old | Replaced by | Notes |
|---|---|---|
| **Track 1 / Track 2 split** | single path | One model, one daemon, one path. |
| **LiteRT / LiteRT-LM / `litertlm-android`** | ort_engine daemon | Removed. No in-process tensor runtime. |
| **`Backend.GPU()` / `Backend.NPU()`** | NpuClient → ort_engine | Adreno 830 not used for inference; HTP only via daemon. |
| **E2B / Gemma Nano placeholder** | nothing | Skipped. |
| **Qwen3-VL-8B-Thinking as primary** | Qwen3.5-9B | User override 2026-06-27. VL-8B-Thinking is BACKUP only. |
| **Qwen2.5-VL-7B as primary** | Qwen3.5-9B | Backup only. |
| **3-artifact vision split** | single decoder `.bin` + deepstack runtime injection | Qwen3.5 deepstack: language decoder IS the multimodal artifact. Script still exports vision encoder + projection if multimodal submodules are found; SKIP_VISION=1 is documented but NOT set in the default trigger command. |
| **`genie_engine` daemon** | `ort_engine` | ORT + QNN EP loads `qnn_context_binary` directly. No Genie SDK. |
| **Separate `Watchdog` service** | folded into CliffordService | CLIFFORD === Watchdog. One daemon guardian. |
| **Nexa SDK, OmniNeural, Moonshine, JitPack, SystemTtsClient** | dead | Pre-session-1 cleanup. |
| **Cloud failover in app LLM** | HttpFetch agent tool | Cloud APIs via agent layer only. |

---

## Compile pipeline (the mission)

No Qwen3.5-9B NPU build targeting SM8750 HTP exists publicly. We are building it.

```
PyTorch (HF Jobs cpu-xl)  →  ONNX export (RoPE fold, stateless prefill, dynamo=False)
                                                   │
                                                   ▼
                          QAI Hub upload  →  Compile (W4A16, --disable_fusion,
                                              --bias_as_int32, 16/32 MiB caps)
                                                   │
                                                   ▼
                          qnn_context_binary  →  Mer0vin8ian/qwen3-5-9b-npu-sm8750
                                                   │
                                                   ▼
                          adb push  →  /storage/emulated/0/Download/
                                                   │
                                                   ▼
                          ort_engine daemon (ONNX RT + QNN EP)  →  127.0.0.1:8080
                                                   │
                                                   ▼
                          NpuClient (Kotlin)  →  agent layer + UI
```

Primary host: HF Jobs `cpu-xl` (~$1/hr, 124 GB RAM, no GPU needed — compile is server-side at QAI Hub).

### Trigger command (Job 8)

```
hf jobs uv run --flavor cpu-xl --timeout 2h \
  --with torch --with transformers --with onnx --with onnxruntime --with onnxscript \
  --with qai-hub --with datasets --with numpy --with huggingface_hub --with accelerate \
  --secrets HF_TOKEN --secrets QAI_HUB_API_TOKEN \
  -e MODEL_ID=Mer0vin8ian/Qwen3.5-9B -e PUBLISH_HF=1 -e OUTPUT_DIR=/tmp \
  https://raw.githubusercontent.com/c10vis-poem/Novus-Agenti/claude/project-scope-review-lf615p/scripts/compile_qwen3_5_9b.py
```

`SKIP_VISION` is NOT set — script attempts all three artifacts (vision encoder, projection, language decoder).

---

## Key files

```
horizons/src/main/java/com/horizons/
  HorizonsApplication.kt                    app singleton
  core/llm/LlmRuntime.kt                    interface
  core/llm/NpuClient.kt                     LlmRuntime socket → ort_engine @ 127.0.0.1:8080
  core/shell/DaemonLauncher.kt              sh -T- detach + root oom_score_adj -1000
                                            TODO: NpuManager.acquirePerformanceLock(PERF_MODE_HIGH)
  core/perf/GameModeBoost.kt                placeholder
                                            TODO: GameManager.setGameMode(GameMode.PERFORMANCE)
  core/agent/AgentLoop.kt                   22 tools; HttpFetch + WebSearch
  core/agent/AgentNotificationListener.kt   notification shade read
  core/state/AppStateStore.kt               encrypted KV (cloud tokens, model path, TTS)
  fgs/CliffordService.kt                    daemon guardian + CRS 15s recovery (== Watchdog)
  audio/ + accessibility/ + ui/             voice loop, dock, panes

scripts/
  compile_qwen3_5_9b.py                     PRIMARY compile pipeline — M-RoPE fix (commit 2af893b)

wiki/
  GPT-OSS-Reference.md                      Hexagon HTP failure modes — source of truth
  EDGE-MODEL-LISTS.md                       model directory (Qwen3.5-9B primary)
  SESSION{N}-HANDOFF.md                     per-session handoffs

models/
  manifest.yaml                             build order; Qwen3.5-9B primary, VL-8B + Gemma backups

.github/workflows/
  build-apk.yml                             TODO: repoint release target to ${{ github.repository }}
  stage-colab.yml                           Colab one-click trigger
```

---

## Hard rules — non-negotiable

- Never push `main` without explicit user permission. Working branch is the only commit target.
- Never commit credentials. `release/debug.keystore` is the documented exception.
- Never `--no-verify`, never `push --force`, never `reset --hard` without confirming.
- No CPU fallback. No cloud inference in the main app LLM runtime. Cloud APIs via AgentTool.HttpFetch only.
- No in-process tensor runtime — daemon path or nothing.
- No new abstractions beyond what the task requires.
- No comments unless the WHY is non-obvious.
- No piecemealing — multi-part work fans out in parallel.
- No re-litigating decisions captured in this file. Update the doc first, then act.
- `M0DU14R-SYSx-inc/NeuroOmni.Vag-Agenti` is REFERENCE-ONLY. No commits, no pushes, ever.

---

## Tokens / secrets policy

- `HF_TOKEN` and `QAI_HUB_API_TOKEN` are STATIC across sessions per user policy. User provides them at session start if not already in env. Rotation happens AFTER the build ships, not between sessions.
- No GitHub PAT required — GitHub MCP server handles auth.
- Cloud API tokens (SambaNova, OpenRouter, etc.) live in `AppStateStore` encrypted slots.

---

## Build / CI

- AGP 8.8.0 · Kotlin 2.1.0 · compileSdk 35 · minSdk 31 · JDK 17
- ABI: arm64-v8a only
- Signing: `release/debug.keystore` (committed by design — stable signature = APK installs as update)
- CI: `.github/workflows/build-apk.yml` — needs `latest-debug` release target repointed from NeuroOmni to `${{ github.repository }}`

---

## Brand

- Background `#222C34` · Surface `#35414A` · Primary teal `#2DD4D9`
- Highlight teal `#4FE7EC` · Icon backplate `#050709` · Action yellow `#F5C518`
- Backdrop: pure Compose `Brush.radialGradient` — NOT XML shape (painterResource on `<shape>` crashes)

---

## Termux environment (user's phone)

**Device:** Motorola Razr Ultra 2025 · SM8750 · 16GB LPDDR5X · Adreno 830 · Hexagon HTP v75
**User is on phone only.** No laptop. No root. No Shizuku.

### Mobile paste rules — never violate
- Never embed tokens or long URLs in commands the user has to paste.
- Use shell variables: paste `T=TOKEN` as one line, then reference `$T`.
- For git auth: interactive prompts — user pastes token at `Password:` only.
- Long URLs in `.git/config`: edit via nano, not paste.
- Keep every paste-able command under ~50 chars where possible.

### Future Termux setup (separate track)
- VNC + XFCE via `sabamdarif/termux-desktop`
- proot-distro Ubuntu for Claude Code Android (`ferrumclaudepilgrim/claude-code-android` fork at `c10vis-poem`)
- Matrix theme (green `#00FF41` on black), Termux:Float + Termux:Styling from F-Droid
- HF CLI + qai-hub CLI for direct compile from phone
