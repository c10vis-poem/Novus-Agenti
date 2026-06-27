# CLAUDE.md — Novus Agenti / Omni Claw

> **RESUME PROMPT — COPY THIS BLOCK VERBATIM TO START ANY NEW SESSION**
>
> ```
> Project: Novus Agenti (Omni Claw). Mission: compile Mer0vin8ian/Qwen3.5-9B
> → Hexagon HTP v75 (SM8750) qnn_context_binary via QAI Hub.
> Canonical repo: c10vis-poem/Novus-Agenti, branch claude/project-scope-review-lf615p.
>
> READ THESE IN ORDER BEFORE ANY ACTION:
>   1. CLAUDE.md (full read, all sections)
>   2. wiki/GPT-OSS-Reference.md (full read)
>   3. wiki/EDGE-MODEL-LISTS.md
>   4. models/manifest.yaml
>   5. wiki/SESSION{N}-HANDOFF.md (latest N)
>
> Use /memory slash command to reload full project context.
> After reading: state current SOTU, last job result, next action. Then wait.
> Tokens: HF_TOKEN and QAI_HUB_API_TOKEN — ask user if not in env.
> ```

---

## /memory — Slash Command

Type `/memory` in any Claude Code session to reload full project context.

**Sequence:**
1. Read `CLAUDE.md` (this file, all sections)
2. Read `wiki/GPT-OSS-Reference.md`
3. Read `wiki/EDGE-MODEL-LISTS.md`
4. Read `models/manifest.yaml`
5. Read latest `wiki/SESSION{N}-HANDOFF.md`
6. Produce a SOTU summary and confirm next action before touching any file

**Use it whenever:** session is new, context is stale, or re-anchoring is needed.

---

## Order of Operations — Non-Negotiable

Every session, every sub-agent, every new context:

1. **Read** — the five files above, in order, completely
2. **State** — current SOTU and next action
3. **Act** — fan out parallel work only after confirming read is done
4. **Document** — before ending: update SOTU in this file, write `wiki/SESSION{N+1}-HANDOFF.md`

If this file and a handoff disagree, **this file wins**. Raise the conflict explicitly.

---

## Cache Prompting + Sub-Agent Rules

### When to spawn
- Open-ended exploration spanning more than 3 files → `Explore` agent
- Independent background research that doesn't block current work → background agent
- Any task finishable inline in under 3 tool calls → do NOT spawn

### How to brief every sub-agent
Every sub-agent prompt must include:
- Repo: `c10vis-poem/Novus-Agenti`, branch `claude/project-scope-review-lf615p`
- Instruction to read CLAUDE.md before acting
- The exact task (not open-ended)
- What NOT to do (no commits to main, no pushing other branches)
- Where to put output

**Template:**
```
Repo: c10vis-poem/Novus-Agenti, branch claude/project-scope-review-lf615p.
Read CLAUDE.md fully before anything else.
Task: [SPECIFIC TASK].
Output: [LOCATION].
Do NOT commit to main. Do NOT push to any branch other than claude/project-scope-review-lf615p.
```

### Context budget rules
- Do NOT re-read files already read this session
- Do NOT re-derive decisions already in CLAUDE.md
- Do NOT spawn an agent to do work finishable in under 3 inline tool calls
- If a prior session's sub-agent output is in `tasks/`, read it before re-researching the same topic
- Parallel agents must operate on **different files** — same-file concurrent pushes cause conflicts

### Deferred tools
Tools requiring ToolSearch before use: `mcp__github__*`, `TaskCreate`, `TaskUpdate`, `WebFetch`, `WebSearch`. Fetch schema once per session, do not re-fetch.

---

## What This Is

**Novus Agenti** — "the unprecedented driving force" — fully on-device agentic AI assistant for the Motorola Razr Ultra 2025 (Snapdragon 8 Elite SM8750, Adreno 830, Hexagon HTP v75). Inference runs on the NPU via a detached native daemon. No cloud LLM in the main app runtime. No CPU fallback. Cloud APIs (SambaNova, OpenRouter, etc.) are callable from the agent layer via `HttpFetch` — that is the neuromesh contract.

App package: `com.horizons` (pending rebrand). Codebase: **Omni Claw** banner.

**Identity:** Novus Agenti · Omni Claw · Cl0vis × Mer0vin6ian production.
**HuggingFace:** `Mer0vin8ian` · **Personal GitHub:** `c10vis-poem` · **Org GitHub:** `M0DU14R-SYSx-inc`.

---

## Repo Policy — Non-Negotiable

- **`c10vis-poem/Novus-Agenti`** — THE canonical repo. All commits, pushes, CI, artifacts go here. Hyphen in name (`Novus-Agenti`), not a dot.
- **`M0DU14R-SYSx-inc/NeuroOmni.Vag-Agenti`** — REFERENCE-ONLY. Never push, commit, or modify.
- Working branch: `claude/project-scope-review-lf615p`. PR #4 tracks it.

---

## Single-Path Architecture

One model, one runtime, one pipeline. No tracks, no placeholders, no fallbacks.

| Layer | What | Status |
|---|---|---|
| **Model** | `Mer0vin8ian/Qwen3.5-9B` — 9.65B params, `qwen3_5` arch, Apache 2.0. Multimodal via **deepstack vision injection** at decoder layers (`vision_config.deepstack_visual_indexes`). NOT a separate encoder pipeline. | Source on HF Hub |
| **ONNX export** | `scripts/compile_qwen3_5_9b.py` on HF Jobs `cpu-xl` | M-RoPE fix committed (`2af893b`). Job 8 pending. |
| **QAI Hub compile** | ONNX → `qnn_context_binary` (W4A16) server-side. QAI Hub owns quantization, partitioning, hardware optimization. We provide clean ONNX. | Job 8 pending |
| **Compile output** | `Mer0vin8ian/qwen3-5-9b-npu-sm8750/*.bin` | Job 8 pending |
| **Runtime: `ort_engine`** | ONNX Runtime + QNN Execution Provider on aarch64-android. Loads `qnn_context_binary`, manages QNN context, serves `POST http://127.0.0.1:8080/api/v1/generate` → SSE-JSON. | Not yet built |
| **Daemon launch** | `DaemonLauncher.kt` → `sh -T-` detach + root `oom_score_adj=-1000` + `NpuManager.acquirePerformanceLock(PERF_MODE_HIGH)` | `NpuManager` lock not yet wired |
| **Game SDK boost** | `GameManager.setGameMode(GameMode.PERFORMANCE)` at `Application.onCreate` + `<uses-feature android:name="android.hardware.game"/>` | Not yet wired |
| **Daemon guardian** | `CliffordService` FGS — CLIFFORD == Watchdog. `START_STICKY`, `specialUse` FGS type, 15s CRS recovery loop on daemon PID. | Committed on PR #3 |
| **Bridge** | `NpuClient.kt` → `POST http://127.0.0.1:8080/api/v1/generate` | Committed on PR #3 |
| **Agent layer** | `AgentLoop` + 22 tools (20 Android + `HttpFetch` + `WebSearch`) | Committed on PR #3 |

---

## Size Envelope (Hard Caps)

| | Size | Notes |
|---|---|---|
| Target | **5.5 GB** | Shoot for this |
| Ideal ceiling | 6.0 GB | Acceptable |
| Redline | **7.0–7.2 GB** | Non-negotiable; refuse anything over |
| W4A16 at max_seq=4096 | ~5.7 GB total | ✓ inside ideal |
| W4A16 at max_seq=2048 | ~5.4 GB total | ✓ inside target |

**Quantization:** W4A16 — INT4 per-channel weights, FP16 activations.

---

## Hexagon HTP Constraints (per `wiki/GPT-OSS-Reference.md`)

All applied at compile time in `scripts/compile_qwen3_5_9b.py`.

| Constraint | Why | Applied as |
|---|---|---|
| **RoPE fold** | No FP16 Sin/Cos kernel on Hexagon → graph-build abort | Two-pronged: `make_folded_rope_forward` returns `[B,S,D]`; `_patched_apply_rotary_pos_emb` at module level bypasses M-RoPE 5D/4D shape issue |
| **Static shapes** | Hexagon requires compile-time static dims | batch=1, MAX_SEQ_LEN fixed; valid-length scalar at runtime |
| **KV cache pre-alloc** | `QnnTensorUpdate` buggy >4 MiB | Pre-allocated at MAX_SEQ_LEN |
| **`--disable_fusion`** | MHA fusion only works static seq + INT8 weights | Flag in `COMPILE_OPTIONS_BASE` |
| **`--bias_as_int32`** | INT8 bias overflow | Flag in `COMPILE_OPTIONS_BASE` |
| **Softmax + TopK → CPU** | No FP16 NPU kernel | `partition_override.json` |
| **Scratch: 16 MiB** | Attention score matrix budget | `--scratch_size_mib 16` |
| **Dynamic tensor: 64 MiB** | Canonical GPT-OSS value. The dual-core soft-cap-at-32 hypothesis is **unverified** — needs empirical testing before reducing. | `--max_dynamic_tensor_size_mib 64` |
| **Single NPU context** | Serialize inference, avoid public QNN 2-context cap | `QNN_GRAPH_CONFIG_MAX_CONTEXTS=1` |
| **Stateless prefill** | Qwen3.5 hybrid attention rejects `DynamicCache` | `use_cache=False` in `HtpDecodeWrapper` |

---

## Android App Behavior / Battery Rules

**Source:** `wiki/GPT-OSS-Reference.md` §6 — do not paraphrase elsewhere.

### NpuManager Performance Lock (API 33+)

```kotlin
// CliffordService.kt — acquire before inference, release when done
val npuManager = getSystemService(NpuManager::class.java)
val lock = npuManager.acquirePerformanceLock(NpuManager.PERF_MODE_HIGH)
// ... inference ...
lock.release()
```

**Status: NOT wired into `CliffordService.kt`. PENDING.**

### Game SDK Performance Mode

```kotlin
// HorizonsApplication.kt — call in Application.onCreate()
val gameManager = GameManager.getInstance(this)
gameManager.setGameMode(GameMode.PERFORMANCE)
```

**Status: NOT wired into `HorizonsApplication.kt`. PENDING.**

### Manifest Entries (both required)

```xml
<uses-feature android:name="android.hardware.game" android:required="true" />
<uses-permission android:name="android.permission.HIGH_PERFORMANCE" />

<service
    android:name=".CliffordService"
    android:exported="false"
    android:foregroundServiceType="specialUse" />
```

**Status: `uses-feature` + `HIGH_PERFORMANCE` NOT in manifest. PENDING.**

### Key Rule

Game SDK gives scheduler boost for UI processes only. Android restricted mode disables the GPU-boost flag for non-UI daemons. The NPU still runs, but GPU sub-graphs throttle to background frequency. **Both** `NpuManager.acquirePerformanceLock` AND the Game SDK boost are required together.

### FGS + Daemon Lifecycle

```
HorizonsApplication.onCreate()
  → GameManager.setGameMode(PERFORMANCE)             [TODO]
  → startForegroundService(CliffordService)

CliffordService.onStartCommand()
  → startForeground(NOTIFICATION_ID, ...)            [FGS sticky, specialUse]
  → DaemonLauncher.launch(ort_engine)
    → NpuManager.acquirePerformanceLock(HIGH)        [TODO]
    → sh -T- detach + oom_score_adj=-1000
  → 15s CRS recovery loop watching ort_engine PID

ort_engine (aarch64-android)
  → ONNX Runtime + QNN EP loads qnn_context_binary
  → HTTP server at 127.0.0.1:8080

NpuClient.kt → POST /api/v1/generate → SSE-JSON
```

---

## State of the Union — 2026-06-27 (session 8)

### Done
- M-RoPE two-pronged shape fix committed (`2af893b`) — Jobs 6+7 root cause resolved
- `wiki/GPT-OSS-Reference.md` committed and fully cross-referenced
- `wiki/EDGE-MODEL-LISTS.md` committed
- `models/manifest.yaml` committed
- Full Android app in PR #3 (53 .kt files, CliffordService FGS, NpuClient, AgentLoop, 22 tools)
- PR #4 open tracking this branch
- `--max_dynamic_tensor_size_mib` restored to 64 MiB (canonical)

### Pending — in order
1. **Job 8** — trigger command below. Runs ~30–40 min on HF Jobs cpu-xl.
2. **`ort_engine` C++ daemon** — build aarch64-android binary (ORT + QNN EP). Not yet scaffolded.
3. **NpuManager lock** — wire into `CliffordService.kt`
4. **GameManager.setGameMode** — wire into `HorizonsApplication.kt`
5. **Manifest** — add `uses-feature` + `HIGH_PERFORMANCE` permission
6. **`build-apk.yml`** — repoint release target to `${{ github.repository }}`
7. **PRs #2 and #3** — merge into main
8. **Watchdog module** — fold into CliffordService or delete

---

## Job Execution Log

| Session | Job | Error | Root Cause | Fix | Result |
|---|---|---|---|---|---|
| 1–4 | 1–4 | Various load/submodule errors | Iterative | Resolved | Done |
| 5 | 5 | `ValueError: has_previous_state can only be called on LinearAttention` | Qwen3.5 hybrid attention rejects DynamicCache | `use_cache=False` in `HtpDecodeWrapper` | Done |
| 6 | 6 | `RuntimeError: got 5 and 4` at `modeling_qwen3_5.py:603` | M-RoPE `apply_interleaved_mrope` returns `[3,B,S,D]`; `unsqueeze(1)` → 5D; cat with 4D q_pass fails | Two-pronged fix — commit `2af893b` | Done |
| 7 | 7 | Same error | Prior fix returned 2D `[S,D]` → wrong unsqueeze result | Same commit covers it | Done |
| **8** | **8** | **Pending** | — | — | **Ready** |

### M-RoPE fix detail (for future sessions)

**Prong 1:** `make_folded_rope_forward` returns `[B,S,D]` (3D) by indexing `cos_buf[pos_2d]` where `pos_2d` is `[B,S]`. After library's `unsqueeze(1)` → `[B,1,S,D]` which broadcasts cleanly with `[B,H,S,D]` q/k.

**Prong 2:** `apply_rotary_pos_emb` patched at module level in `transformers.models.qwen3_5.modeling_qwen3_5` — ignores the `cos/sin` args entirely, uses precomputed `[1,1,S,D]` FP16 tables. No M-RoPE shape issues possible.

---

## Compile Pipeline

```
HF Jobs cpu-xl (~$1/hr, 124 GB RAM)
  compile_qwen3_5_9b.py
  → ONNX export (RoPE fold, stateless prefill, static shapes)
                          │
                          ▼
          QAI Hub (server-side)
          → ONNX → QNN convert
          → W4A16 quantization
          → Partition (matmuls→DSP, Softmax+TopK→CPU)
          → qnn_context_binary
                          │
                          ▼
          Mer0vin8ian/qwen3-5-9b-npu-sm8750  (HF Hub, private)
          qwen3_5_9b_language_decoder.bin
          qwen3_5_9b_vision_encoder.bin
          qwen3_5_9b_projection.bin
                          │
                          ▼
          adb push → /storage/emulated/0/Download/
                          │
                          ▼
          ort_engine daemon (ORT + QNN EP, aarch64-android)
          → loads qnn_context_binary
          → http://127.0.0.1:8080/api/v1/generate
                          │
                          ▼
          NpuClient.kt → AgentLoop + UI
```

### Job 8 Trigger Command

```
hf jobs uv run --flavor cpu-xl --timeout 2h \
  --with torch --with transformers --with onnx --with onnxruntime --with onnxscript \
  --with qai-hub --with datasets --with numpy --with huggingface_hub --with accelerate \
  --secrets HF_TOKEN --secrets QAI_HUB_API_TOKEN \
  -e MODEL_ID=Mer0vin8ian/Qwen3.5-9B -e PUBLISH_HF=1 -e OUTPUT_DIR=/tmp \
  https://raw.githubusercontent.com/c10vis-poem/Novus-Agenti/claude/project-scope-review-lf615p/scripts/compile_qwen3_5_9b.py
```

`SKIP_VISION` is NOT set — all three artifacts attempted.

---

## Complete File Listing

```
c10vis-poem/Novus-Agenti  (branch: claude/project-scope-review-lf615p)

CLAUDE.md                                    ← THIS FILE
README.md

models/
  manifest.yaml                              build order; Qwen3.5-9B primary

scripts/
  compile_qwen3_5_9b.py                      PRIMARY compile pipeline (commit 2af893b)
  compile_qwen3_vl.py                        BACKUP (Qwen3-VL-8B)

wiki/
  GPT-OSS-Reference.md                       Hexagon HTP failure modes — source of truth
  EDGE-MODEL-LISTS.md                        model directory
  SESSION5-HANDOFF.md
  SESSION6-HANDOFF.md

horizons/src/main/java/com/horizons/
  HorizonsApplication.kt                     app singleton
  core/llm/LlmRuntime.kt                     interface
  core/llm/NpuClient.kt                      socket → ort_engine @ 127.0.0.1:8080
  core/shell/DaemonLauncher.kt               sh -T- detach + oom_score_adj=-1000
                                             TODO: NpuManager.acquirePerformanceLock
  core/perf/GameModeBoost.kt                 placeholder
                                             TODO: GameManager.setGameMode(PERFORMANCE)
  core/agent/AgentLoop.kt                    22 tools
  core/agent/AgentNotificationListener.kt
  core/state/AppStateStore.kt                encrypted KV
  fgs/CliffordService.kt                     daemon guardian == Watchdog
  audio/ + accessibility/ + ui/

.github/workflows/
  build-apk.yml                              TODO: repoint to ${{ github.repository }}
  stage-colab.yml
```

---

## Hard Rules — Non-Negotiable

- Never push `main` without explicit user permission
- Never commit credentials (`release/debug.keystore` is the documented exception)
- Never `--no-verify`, never `push --force`, never `reset --hard` without confirming
- No CPU fallback in app LLM. Cloud APIs via `AgentTool.HttpFetch` only
- No in-process tensor runtime — daemon path or nothing
- No new abstractions beyond what the task requires
- No piecemealing — multi-part work fans out in parallel
- No re-litigating decisions in this file. Update the doc, then act
- `M0DU14R-SYSx-inc/NeuroOmni.Vag-Agenti` is REFERENCE-ONLY. No commits, no pushes, ever
- Do NOT set `SKIP_VISION=1` in the default trigger command
- `--max_dynamic_tensor_size_mib` stays at **64** until dual-core soft-cap is verified empirically

---

## Tokens / Secrets Policy

- `HF_TOKEN` and `QAI_HUB_API_TOKEN` are **STATIC across sessions** per user policy. Rotation happens AFTER the build ships.
- Never commit token values. Never hardcode in any pushed file. GitHub secret scanning will block and expose. Use `--secrets HF_TOKEN` (no value) — reads from `hf auth login` config.
- No GitHub PAT needed — GitHub MCP server handles auth.
- Cloud API tokens live in `AppStateStore` encrypted slots.

---

## Build / CI

- AGP 8.8.0 · Kotlin 2.1.0 · compileSdk 35 · minSdk 31 · JDK 17
- ABI: arm64-v8a only
- Signing: `release/debug.keystore` (committed by design — stable signature for APK updates)
- CI: `build-apk.yml` needs `latest-debug` release target repointed to `${{ github.repository }}`

---

## Brand

- Background `#222C34` · Surface `#35414A` · Primary teal `#2DD4D9`
- Highlight teal `#4FE7EC` · Icon backplate `#050709` · Action yellow `#F5C518`
- Backdrop: pure Compose `Brush.radialGradient` — NOT XML shape (`painterResource` on `<shape>` crashes)

---

## Termux Environment (User's Phone)

**Device:** Motorola Razr Ultra 2025 · SM8750 · 16GB LPDDR5X · Adreno 830 · Hexagon HTP v75
**User is on phone only.** No laptop. No root. No Shizuku.

### Mobile Paste Rules — Never Violate
- No tokens or long URLs in paste-able commands
- Shell variables: `T=TOKEN` as one line, then `$T`
- Git auth: user pastes at `Password:` prompt only
- Long URLs: edit via nano, not paste
- Every paste-able command under ~50 chars where possible

### Future Termux Setup (Separate Track)
- VNC + XFCE via `sabamdarif/termux-desktop`
- proot-distro Ubuntu for Claude Code Android (`ferrumclaudepilgrim/claude-code-android` at `c10vis-poem`)
- Matrix theme (green `#00FF41` on black), Termux:Float + Termux:Styling from F-Droid
- HF CLI + qai-hub CLI for direct compile from phone

---

## What Was Ripped Out — Do NOT Reference

| Old | Replaced by | Notes |
|---|---|---|
| Track 1 / Track 2 split | single path | One model, one daemon, one path |
| LiteRT / LiteRT-LM | ort_engine daemon | No in-process tensor runtime |
| `Backend.GPU()` / `Backend.NPU()` | NpuClient → ort_engine | Adreno 830 not used for inference |
| E2B / Gemma Nano placeholder | nothing | Skipped |
| Qwen3-VL-8B-Thinking as primary | Qwen3.5-9B | VL-8B-Thinking is BACKUP only |
| Qwen2.5-VL-7B as primary | Qwen3.5-9B | Backup only |
| `genie_engine` daemon | `ort_engine` | ORT + QNN EP loads `qnn_context_binary` directly |
| Separate `Watchdog` service | folded into CliffordService | CLIFFORD == Watchdog |
| Nexa SDK, OmniNeural, Moonshine, JitPack, SystemTtsClient | dead | Pre-session-1 cleanup |
| Cloud failover in app LLM | HttpFetch agent tool | Cloud APIs via agent layer only |
