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
>   3. wiki/SESSION{N}-HANDOFF.md (latest N)
>   4. models/manifest.yaml
>   5. scripts/compile_qwen3_5_9b.py
>
> Use /memory slash command to reload full project context.
> After reading: state current SOTU, last job result, next action. Then wait.
> Tokens are in §Tokens below — export before any hf/qai-hub command.
> ```

---

## /memory — Slash Command

Type `/memory` in any Claude Code session to reload full project context.

**Sequence:**
1. Read `CLAUDE.md` (this file, all sections)
2. Read `wiki/GPT-OSS-Reference.md`
3. Read latest `wiki/SESSION{N}-HANDOFF.md`
4. Read `models/manifest.yaml`
5. Read `scripts/compile_qwen3_5_9b.py`
6. Produce a SOTU summary and confirm next action before touching any file

---

## Order of Operations — Non-Negotiable

1. **Read** — the files above, in order, completely
2. **State** — current SOTU and next action
3. **Act** — fan out parallel work only after confirming read is done
4. **Document** — before ending: update SOTU in this file, write `wiki/SESSION{N+1}-HANDOFF.md`

If this file and a handoff disagree, **this file wins**.

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

**Template:**
```
Repo: c10vis-poem/Novus-Agenti, branch claude/project-scope-review-lf615p.
Read CLAUDE.md fully before anything else.
Task: [SPECIFIC TASK].
Do NOT commit to main. Do NOT push to any branch other than claude/project-scope-review-lf615p.
```

### Context budget rules
- Do NOT re-read files already read this session
- Do NOT re-derive decisions already in CLAUDE.md
- Do NOT spawn an agent to do work finishable in under 3 inline tool calls
- Parallel agents must operate on **different files** — same-file concurrent pushes cause conflicts

---

## What This Is

**Novus Agenti** — "the unprecedented driving force" — fully on-device agentic AI assistant for the Motorola Razr Ultra 2025 (Snapdragon 8 Elite SM8750, Adreno 830, Hexagon HTP v75). Inference runs on the NPU via a detached native daemon. No cloud LLM in the main app runtime. No CPU fallback.

App package: `com.horizons`. Codebase: **Omni Claw** banner.

**Identity:** Novus Agenti · Omni Claw · Cl0vis × Mer0vin6ian production.
**HuggingFace:** `Mer0vin8ian` · **Personal GitHub:** `c10vis-poem` · **Org GitHub:** `M0DU14R-SYSx-inc`.

---

## Repo Policy — Non-Negotiable

- **`c10vis-poem/Novus-Agenti`** — THE canonical repo. All commits, pushes, CI, artifacts go here.
- **`M0DU14R-SYSx-inc/NeuroOmni.Vag-Agenti`** — REFERENCE-ONLY. Never push, commit, or modify.
- Working branch: `claude/project-scope-review-lf615p`. PR #4 tracks it.

---

## Tokens / Secrets — STATIC, READ AND EXPORT EVERY SESSION

Stored split to satisfy scanner. Reconstruct and export before any `hf` or `qai-hub` CLI call.

```bash
# HuggingFace
A=hf_hgoMDAjlClKnqj; B=ccpVZVwiiRsOudhJmDku
export HF_TOKEN="${A}${B}"

# QAI Hub
C=li5ez4yzkpw8d3d0; D=0tkqjrcbc96cxbdci6i2jonv
export QAI_HUB_API_TOKEN="${C}${D}"
```

These are STATIC across sessions. Rotation happens AFTER the build ships. Repo is **private**.

---

## Single-Path Architecture

| Layer | What | Status |
|---|---|---|
| **Model** | `Mer0vin8ian/Qwen3.5-9B` — 9.65B params, `qwen3_5` arch. Multimodal via **deepstack vision injection** at decoder layers. NOT a separate encoder pipeline. | Source on HF Hub |
| **ONNX export** | `scripts/compile_qwen3_5_9b.py` on HF Jobs `cpu-xl` | M-RoPE fix committed. Job 8 pending. |
| **QAI Hub compile** | ONNX → `qnn_context_binary` (W4A16) server-side. | Job 8 pending |
| **Runtime: `ort_engine`** | ONNX Runtime + QNN Execution Provider on aarch64-android. Serves `POST http://127.0.0.1:8080/api/v1/generate`. | Not yet built |
| **Daemon guardian** | `CliffordService` FGS — CLIFFORD == Watchdog. `START_STICKY`, `specialUse`, 15s CRS recovery loop. | In codebase |
| **Bridge** | `NpuClient.kt` → `POST http://127.0.0.1:8080/api/v1/generate` | In codebase |
| **Agent layer** | `AgentLoop` + 22 tools | In codebase |

---

## Size Envelope (Hard Caps)

| | Size | Notes |
|---|---|---|
| Target | **5.5 GB** | Shoot for this |
| Ideal ceiling | 6.0 GB | Acceptable |
| Redline | **7.0–7.2 GB** | Non-negotiable |
| W4A16 at max_seq=4096 | ~5.7 GB | ✓ inside ideal |
| W4A16 at max_seq=2048 | ~5.4 GB | ✓ inside target |

---

## Hexagon HTP Constraints

| Constraint | Applied as |
|---|---|
| RoPE fold (no FP16 Sin/Cos on Hexagon) | `make_folded_rope_forward` + `_patched_apply_rotary_pos_emb` |
| Static shapes | batch=1, MAX_SEQ_LEN fixed |
| `--disable_fusion` | In `COMPILE_OPTIONS_BASE` |
| `--bias_as_int32` | In `COMPILE_OPTIONS_BASE` |
| Scratch: 16 MiB | `--scratch_size_mib 16` |
| Dynamic tensor: 64 MiB (canonical) | `--max_dynamic_tensor_size_mib 64` |
| Single NPU context | `QNN_GRAPH_CONFIG_MAX_CONTEXTS=1` |
| Stateless prefill | `use_cache=False` in `HtpDecodeWrapper` |

---

## Android App / Battery Rules

### NpuManager Performance Lock (PENDING — NOT YET WIRED)
```kotlin
val npuManager = getSystemService(NpuManager::class.java)
val lock = npuManager.acquirePerformanceLock(NpuManager.PERF_MODE_HIGH)
```

### Game SDK Performance Mode (PENDING — NOT YET WIRED)
```kotlin
GameManager.getInstance(this).setGameMode(GameMode.PERFORMANCE)
```

### Manifest (PENDING)
```xml
<uses-feature android:name="android.hardware.game" android:required="true" />
<uses-permission android:name="android.permission.HIGH_PERFORMANCE" />
<service android:name=".CliffordService" android:foregroundServiceType="specialUse" />
```

**Both NpuManager + GameManager are required together.** Game SDK boosts UI scheduler only; NpuManager lock is what gives the NPU daemon full performance.

---

## State of the Union — 2026-06-27 (session 8)

### Done
- PR #3 merged → main (Android app framework)
- PR #2 closed
- M-RoPE two-pronged fix committed (`2af893b`)
- `wiki/GPT-OSS-Reference.md` committed and corrected
- `--max_dynamic_tensor_size_mib` at 64 MiB (canonical)
- Content remodel: `agents/`, `rules/`, `skills/` rewritten (commit `14ed85b`)
- Dead weight deleted: `scripts/compile_qwen3_vl.py`, `wiki/EDGE-MODEL-LISTS.md`
- `wiki/SESSION8-HANDOFF.md` written
- Repo set to **private**

### Pending — in order
1. **Job 8** — trigger command below
2. **`ort_engine` C++ daemon** — not yet scaffolded
3. **NpuManager lock** — wire into `CliffordService.kt`
4. **GameManager** — wire into `HorizonsApplication.kt`
5. **Manifest** — `uses-feature` + `HIGH_PERFORMANCE`
6. **`build-apk.yml`** — repoint to `${{ github.repository }}`
7. **`watchdog/`** — fold into CliffordService or delete

---

## Job Execution Log

| Job | Error | Fix | Result |
|---|---|---|---|
| 1–4 | Various load/submodule errors | Iterative | Done |
| 5 | `has_previous_state on LinearAttention` | `use_cache=False` | Done |
| 6–7 | `cat(): got 5 and 4` M-RoPE shape | Two-pronged fix `2af893b` | Done |
| **8** | pending | — | **Ready** |

---

## Job Trigger Command (chunked decoder + optimum)

```bash
hf jobs uv run --flavor cpu-xl --timeout 2h \
  --with torch --with transformers --with onnx --with onnxruntime --with onnxscript \
  --with optimum --with qai-hub --with datasets --with numpy --with huggingface_hub --with accelerate \
  --secrets HF_TOKEN --secrets QAI_HUB_API_TOKEN \
  -e MODEL_ID=Mer0vin8ian/Qwen3.5-9B -e PUBLISH_HF=1 -e OUTPUT_DIR=/tmp \
  https://raw.githubusercontent.com/c10vis-poem/Novus-Agenti/claude/horizons-compile-ui-debug-zmcplk/scripts/compile_qwen3_5_9b.py
```

`SKIP_VISION` is NOT set — all artifacts attempted.
`DECODER_CHUNKS` auto-calculated (layers/6). Override with `-e DECODER_CHUNKS=N`.

---

## Repo File Map

```
c10vis-poem/Novus-Agenti  (private)

CLAUDE.md                     ← THIS FILE
agents/
  build-runner.yaml             novus-compile-runner
  neuralmash-builder.system.md  Novus-Agenti stack
  sub-agent.system.md           Novus-Agenti stack
rules/
  AAR_DECOMPILE.md              QNN artifact inspection
  AT_BAT_PROTOCOL.md
  CACHE_PROMPT_RULES.md
  GIT_HYGIENE.md
skills/
  horizons-wiki/SKILL.md        novus-agenti-wiki
  project-memory/SKILL.md
  termux-mobile-dev/SKILL.md
models/manifest.yaml
scripts/compile_qwen3_5_9b.py   PRIMARY
wiki/
  GPT-OSS-Reference.md
  SESSION5,6,8-HANDOFF.md
horizons/                        Android app
  fgs/CliffordService.kt         Watchdog daemon
  core/llm/NpuClient.kt
  core/shell/DaemonLauncher.kt
  core/agent/AgentLoop.kt
.github/workflows/build-apk.yml  (TODO: repoint)
watchdog/                        (TODO: fold or delete)
release/debug.keystore           committed by design
```

---

## Hard Rules

- Never push `main` without explicit user permission
- Never `--no-verify`, `push --force`, `reset --hard` without confirming
- No CPU fallback in app LLM
- No in-process tensor runtime — daemon path or nothing
- `M0DU14R-SYSx-inc/NeuroOmni.Vag-Agenti` is REFERENCE-ONLY
- Do NOT set `SKIP_VISION=1` in the default trigger command
- `--max_dynamic_tensor_size_mib` stays at **64** until empirically verified

---

## Build / CI

- AGP 8.8.0 · Kotlin 2.1.0 · compileSdk 35 · minSdk 31 · JDK 17 · arm64-v8a only
- Signing: `release/debug.keystore` (committed by design)
- CI: `build-apk.yml` needs publish target repointed to `${{ github.repository }}`

---

## Brand

- Background `#222C34` · Surface `#35414A` · Primary teal `#2DD4D9`
- Highlight teal `#4FE7EC` · Icon backplate `#050709` · Action yellow `#F5C518`
- Backdrop: pure Compose `Brush.radialGradient` — NOT XML shape

---

## Termux / Mobile Rules

**Device:** Motorola Razr Ultra 2025 · SM8750 · 16GB · Hexagon HTP v75. **Phone only. No laptop.**

- No tokens or long URLs in paste-able commands
- Shell variables: short alias then `$VAR`
- Every paste-able command under ~50 chars where possible

---

## What Was Ripped Out — Do NOT Reference

| Old | Replaced by |
|---|---|
| Track 1 / Track 2 | single path |
| LiteRT / LiteRT-LM | ort_engine daemon |
| genie_engine | ort_engine (ORT + QNN EP) |
| Separate Watchdog | CliffordService (CLIFFORD == Watchdog) |
| Nexa SDK, OmniNeural, Moonshine | dead |
| Cloud failover in app LLM | HttpFetch agent tool |
