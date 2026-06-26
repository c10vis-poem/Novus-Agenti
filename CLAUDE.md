# CLAUDE.md — Novus Agenti / Omni Claw

> **SESSION KICKOFF RULE — NON-NEGOTIABLE:**
> Run `/memory` (`.claude/commands/memory.md`) first — it reads all wiki files in order.
> Then read THIS FILE completely. Do NOT proceed until both are done.
> Slash command: type `/memory` in any Claude Code session to load full project context.
> Sub-agents: include the SOTU P0 blocker + branch name in every sub-agent prompt.
> The `wiki/`, `HANDOFF.md`, and `horizons/src/main/assets/CLAUDE_AT_HORIZONS.md`
> describe the **old Nexa SDK / OmniNeural / Moonshine / Kokoro stack — all ripped out.**
> This file is the authoritative current state.

---

## What this is

**Novus Agenti** — "the unprecedented driving force" — fully on-device AI assistant for the Motorola Razr Ultra 2025
(Snapdragon 8 Elite SM8750, Adreno 830, Hexagon HTP). No cloud inference in the main app.
No CPU fallback. If the backend fails, it fails loud.

App package: `com.horizons` (pending rebrand). Codebase lives under the **Omni Claw** banner.

**Project identity:** Novus Agenti · Omni Claw · Cl0vis × Mer0vin6ian production.
HuggingFace: `Mer0vin8ian`. GitHub org: `M0DU14R-SYSx-inc`.

**Two-track architecture (as of 2026-06-23):**

| | **Track 1 — Quick Ship** | **Track 2 — Omni Claw (mission)** |
|---|---|---|
| Repo | `NeuroOmni.Vag-Agenti` (this repo) | `Novus.Agenti` (hard fork) |
| Goal | Working agentic chat + 20 tools on phone ASAP | Qwen3-VL-8B verified → Gemma 4 12B QAT on Hexagon HTP |
| Model | **LiteRtRuntime placeholder only** — fix crash, ship; no E2B/Nano swap planned | **Step 1: Qwen3-VL-8B** via `genie_engine` + `.bin`. **Step 2: Gemma 4 12B QAT** via `ort_engine` + `.dlc`. E2B skipped entirely. |
| Runtime | `LiteRtRuntime` with `Backend.GPU()` (Adreno 830) — crash fix only | `genie_engine` daemon (Qwen3-VL) then `ort_engine` daemon (12B) + `NpuClient` — **zero LiteRT** |
| Validation step | Fix startup crash, ship | Qwen3-VL-8B compile + daemon verify → proves pipeline → then 12B |

**E2B and Gemma Nano are explicitly skipped.** E2B was only ever a quick compile-pipeline sanity check. Qwen3-VL-8B is the first real target because the primary use case requires vision + strong reasoning (Jetson flashing, complex troubleshooting). Once genie_engine + Qwen3-VL-8B is verified end-to-end, 12B QAT via ort_engine follows.

**Track 2 is LiteRT-free.** No `litertlm-android` dependency in the fork's `build.gradle.kts`, no `Backend.GPU()`, no in-process tensor runtime at all. All inference goes through `NpuClient` → daemon → HTP. The model lives outside the app process by design.

Track 1 uses this repo as-is. Track 2 is a hard fork (`c10vis-poem/Novus.Agenti` — private, exists):
- Push: `git push novus origin/claude/resume-review-3kxeh8:main` from `~/NeuroOmni` in Termux (blocked — see SOTU)

Code built on `claude/resume-review-3kxeh8` serves both tracks until the fork is created. The fork strips LiteRT after split.

---

## State of the Union (SOTU) — 2026-06-25 (session 4)

### What is working (committed and green)
- CI builds clean — APK published to `latest-debug` release on every green build
- Sherpa-ONNX AAR CI curl step correct; Kokoro TTS wired; all three LiteRT paths wired
- `AgentLoop` + full tool registry committed — 22 tools (20 Android + HttpFetch + WebSearch)
- `AgentTool.HttpFetch` — direct HTTP to any cloud API with encrypted bearer token lookup via AppStateStore
- `AgentTool.WebSearch` — DuckDuckGo Instant Answer, no API key needed
- `AgentSystemPrompt` — corrected: removed "No internet, no cloud" lie; now describes neuromesh architecture (cloud APIs are first-class tools)
- `AppStateStore` — cloud API token slots: `KEY_API_SAMBANOVA`, `KEY_API_OPENROUTER`, `KEY_API_QAI_HUB`
- `SettingsPane` — cloud API token fields (SambaNova, OpenRouter, QAI Hub) wired to encrypted store
- `DaemonLauncher.kt` — detached native daemon launcher with root oom_score_adj (-1000)
- `NpuClient.kt` — socket `LlmRuntime` stub → ort_engine daemon at 127.0.0.1:8080
- `fgs/CliffordService.kt` — CLIFFORD (BRD) FGS daemon guardian + CRS 15s recovery loop
- `scripts/compile_gemma_qairt.py` — QAT GGUF → QAI Hub → HTP v75
- `scripts/compile_qwen3_vl.py` — Qwen3-VL-8B → QAI Hub → Genie `.bin`
- `SecureResourceRelay.kt`, `NativeBinaryInstaller.kt` — committed
- **wiki/** — 4 docs committed:
  - `wiki/GPT-OSS-Reference.md` — Hexagon v69 operator failure modes (13 items, deployment checklist, RoPE fold)
  - `wiki/OmniClaw-Model-Targets.md` — full model map, corrected SambaNova lineup (405B dead), neuromesh cloud routing
  - `wiki/ANT-Context-Cache-Cheatsheet.md` — Anthropic caching CLI commands (5-min, 1-hour, max)
  - `wiki/STT-TTS-Stack.md` — Whisper.cpp + Kokoro-82M Termux install guide, voice loop script
- **Latest commits on `claude/epic-wozniak-7b0ban`:** `4e58a6f` (HttpFetch+cloud keys), `c11998d` (WebSearch+DeepSeek correction)

### What is broken / incomplete RIGHT NOW
- **App will not launch.** Pre-UI startup crash, undiagnosed.
  - `adb logcat -d | grep -E "FATAL|AndroidRuntime|com.horizons"` to get the stack.
  - Most likely: Sherpa-ONNX JNI not extracted from local AAR, or `CrashRecorder`/`AppStateStore` throwing in `onCreate()`.
- **Push to `c10vis-poem/Novus.Agenti` — INCOMPLETE.** Repo EXISTS (private, user confirmed).
  - Termux clone at `~/NeuroOmni` — done.
  - `novus` remote added — done, BUT with literal "YOURTOKEN" instead of real token.
  - **Fix:** `nano ~/NeuroOmni/.git/config` → find `[remote "novus"]` section → replace `YOURTOKEN` with real token → `Ctrl+X Y Enter` → `git push novus origin/claude/resume-review-3kxeh8:main`
- **STT not working on phone.** See `wiki/STT-TTS-Stack.md` — Whisper.cpp install not yet attempted.
- **TTS broken on phone for 12+ weeks.** Android system TTS likely disabled by OEM battery optimization or update. Fix first: `adb shell pm enable com.google.android.tts`. Fallback: Kokoro-82M via `pip install sherpa-onnx kokoro-onnx` in Termux.

### What was done in session 4 (2026-06-25)
- **Neuromesh architecture corrected** — "no cloud" was wrong; cloud API calls from AgentLoop are the primary use case
- `AgentTool.HttpFetch` added — direct cloud API calls with encrypted bearer token lookup
- `AgentTool.WebSearch` added — DuckDuckGo Instant Answer, no key needed
- `AgentSystemPrompt` rewritten — removed "No internet, no cloud" lie; neuromesh description
- `AppStateStore` — 3 new cloud API token keys (SambaNova, OpenRouter, QAI Hub)
- `SettingsPane` — cloud API token input fields added
- 4 wiki docs created and committed (GPT-OSS-Reference, OmniClaw-Model-Targets, ANT-Context-Cache-Cheatsheet, STT-TTS-Stack)
- **Key corrections made (do not re-hallucinate):**
  - DeepSeek V4 Flash is **paid**, not free (~$0.07/M in on OpenRouter)
  - Truly free OpenRouter reasoning: `openai/gpt-oss-120b:free`
  - SambaNova 405B **dead** — removed April 2025. Current best: `gpt-oss-120b`
  - Phi-3.5 9B **does not exist** — Phi-3.5-Mini=3.8B only
  - NVIDIA Alpamayo is an **autonomous vehicle VLA model**, not desktop automation
  - Omnara is **archived** (Feb 2, 2026) — dead project, do not integrate
  - PWA = Progressive Web Apps (was written as PXA in old docs)

### What was done in session 3 (2026-06-23)
- CLIFFORD/CRS architecture committed — FGS daemon guardian, no root, no Shizuku
- `compile_gemma_qairt.py` fixed deprecated target runtime flag
- `compile_qwen3_vl.py` new script for Qwen3-VL-8B Genie path
- README rewritten to be inference-agnostic
- Novus.Agenti personal repo (`c10vis-poem/Novus.Agenti`) created by user — private
- Termux push attempt started but NOT complete (auth token issue — see above)

### What was done in session 2 (2026-06-22)
- `NpuClient.kt` refactored to Doc 2 contract: `POST /api/v1/generate` + SSE-JSON
- `AgentLoop` runtime provider lambda — `llmProvider: () -> LlmRuntime`

### What was done in session 1 (prior)
- Rewrote `notebooks/gemma4_12b_npu_litertlm_build.ipynb`
- Built complete agentic layer — 20 native Android tools

### Crash history (chronological)
| Commit | Symptom | Root cause | Fixed? |
|---|---|---|---|
| pre-session | Nexa SDK / JitPack sherpa-onnx unresolvable | JitPack multi-module format rejected | Yes |
| `1df2ec6` | Kotlin compile errors in `SherpaOnnxTtsClient`, `LiveChatService`, `ScreenShareService` | `numSpeakers()`/`sampleRate()` property syntax on Java methods; `app.systemTts` stale ref | Yes (`54fc209`) |
| `718d98b` | `GPU FAILED: Failed to create engine: INTERNAL: litert_compiled_model.h:1472` | `latest.release` litertlm schema ahead of June-2026 model | Fixed in `1f65108` (unverified on device) |
| `718d98b` | App crashed after user cleared app data | Unknown — `AppStateStore`/`filesDir` state, or init order | Not investigated |
| `1f65108` | App will not launch at all (current blocker) | Unknown — pre-UI startup crash, need logcat | **OPEN** |
| `1f65108` | 12B model confirmed non-viable via LiteRT-LM in-process | LiteRT-LM in-process cannot hold 12B. Daemon path is the fix. | Architecture pivot to daemon path |

---

## Current stack (as of 2026-06-25)

**Target model: Gemma 4 12B QAT → QAI Hub compile → ONNX RT QNN daemon → localhost:8080**
LiteRT-LM is the current in-app placeholder runtime only. E2B is the fallback model path while the 12B daemon is built — it is NOT the goal.

| Layer | What | Status |
|---|---|---|
| **LLM (target)** | Gemma 4 12B QAT — `google/gemma-4-12B-it-qat-q4_0-unquantized` → `gemma4_12b_qat_htp.dlc` | **ACTIVE BUILD TARGET** — QAI Hub compile via `scripts/compile_gemma.py` |
| **LLM runtime (target)** | ONNX Runtime + QNN EP — native C++ daemon (`ort_engine`) | `NpuClient` bridges Kotlin app → daemon at `127.0.0.1:8080` |
| **LLM runtime (placeholder)** | Google LiteRT-LM `litertlm-android:0.13.1`, `Backend.GPU()` | In-app only; E2B fallback while daemon is built |
| Backend (daemon) | Hexagon HTP v75 → 5.5GB / Adreno GPU → 0.6GB / CPU → 0.1GB | QAI Hub static graph partitioning; QAT eliminates GPU fallback |
| Daemon launch | `DaemonLauncher.kt` → `sh -T-` detach, oom_score_adj = -1000 (root) | LMKD immunity; daemon survives at kernel level |
| Vision | `Backend.GPU()` via `streamImage(jpeg, prompt)` | wired (placeholder runtime) |
| Audio/STT | `Backend.GPU()` via `streamAudio(wav, prompt)` | wired (placeholder runtime) |
| TTS | Sherpa-ONNX → Kokoro multi-lang v1.0 (`SherpaOnnxTtsClient`) | wired; blocked by startup crash |
| VAD | Silero VAD via `onnxruntime-android` (`VadFactory`) | in project, wired |

### LLM init (two-phase)
- `preWarm()` — called from `Application.onCreate()`. Background IO coroutine builds `Engine`, calls `initialize()`. Two-attempt: first with shader `cacheDir`, then wipes cache and retries with `cacheDir = null`.
- `ensureInit()` — awaits the already-running `Deferred` before each inference.

### Model resolution (`resolveModelPath()`)
1. Explicit override from `AppStateStore.KEY_LITERT_MODEL_PATH` (Settings → model path field) — wins always
2. Shell `find /storage/emulated/0 -name '*.litertlm' -type f` — picks up any `.litertlm` in any subfolder
3. Java File API fallback — recurses Download + one level of subdirs
4. Hardcoded fallback: `/storage/emulated/0/Download/gemma-4-E2B-it.litertlm` — placeholder only; 12B NPU path uses `NpuClient` + daemon, not `.litertlm`

Paths are normalized from `/mnt/user/0/emulated/` → `/storage/emulated/` because LiteRT native code can't see the bind-mount.

### TTS stack (Sherpa-ONNX / Kokoro)
- `KokoroModelManager` — downloads `kokoro-multi-lang-v1_0.tar.bz2` from sherpa-onnx GitHub releases, extracts to `filesDir/sherpa_tts/kokoro-multi-lang-v1_0/`. States: `Idle → Downloading → Extracting → Ready / Error`.
- `SherpaOnnxTtsClient` — wraps `OfflineTts`. 28 English voices (`ENGLISH_VOICES` list). `AudioTrack MODE_STATIC` playback. Barge-in via `stopRequested` flag. `init()`, `speak()`, `stop()`, `shutdown()`.
- `RouterPane` shows Kokoro download progress / active voice + speed when Ready. TODO stub at line 138: voice picker, speed slider, pitch control (design-agent task).
- Persisted settings: `AppStateStore.KEY_TTS_VOICE`, `AppStateStore.KEY_TTS_SPEED`. Exposed as `app.ttsVoiceId: MutableStateFlow<String>`, `app.ttsSpeed: MutableStateFlow<Float>`.

### Sherpa-ONNX AAR (CI dependency)
- NOT in git. `horizons/libs/` is gitignored.
- CI downloads it in `build-apk.yml` step "Download sherpa-onnx AAR":
  ```
  curl -fL -o horizons/libs/sherpa-onnx-1.13.2.aar \
    https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.2/sherpa-onnx-1.13.2.aar
  ```
- Referenced in Gradle as `implementation(files("libs/sherpa-onnx-1.13.2.aar"))`.
- JitPack was removed from `settings.gradle.kts` entirely (it couldn't resolve the multi-module AAR format).

---

## What was ripped out — do NOT reference these

| Old thing | Replacement | Notes |
|---|---|---|
| Nexa SDK (`ai.nexa:core:0.0.24`) | LiteRT-LM | Gone. No `NexaVlmEngine`, no `NexaSdk.init()`, no `NEXA_TOKEN`. |
| OmniNeural-4B-mobile | Gemma 4 12B `.litertlm` | Gone. |
| Moonshine STT (ONNX) | Gemma audio-direct via LiteRT-LM (now); Whisper Base ONNX (roadmap) | Silero VAD stays. |
| Kokoro TTS (ONNX) — first attempt | Was ripped out initially; then **brought back** as Sherpa-ONNX / Kokoro multi-lang v1.0 (NOT the old ONNX pipeline, new one via Sherpa-ONNX AAR) | See TTS stack above. |
| `SystemTtsClient` (VoxSherpa / Android TextToSpeech broker) | `SherpaOnnxTtsClient` | File still in repo but unreferenced — delete it. |
| `Backend.NPU()` via LiteRT-LM | ONNX RT QNN native daemon | LiteRT-LM NPU path hit size wall. 12B NPU runs via ONNX RT + QNN C++ daemon, not LiteRT-LM. |
| Cloud failover / ProviderLibrary / OpenRouter auto-failover | **CORRECTION (2026-06-25)**: Cloud API calls ARE in scope. OmniClaw is a neuromesh framework — AgentLoop.HttpFetch tool calls SambaNova, OpenRouter, etc. "No cloud inference in the main app runtime" means the LLM inference *backend* is on-device (daemon), not that the agent tools can't hit cloud APIs. | AgentLoop.HttpFetch + AppStateStore cloud keys wired. SettingsPane has cloud API token fields. |
| JitPack repository | Removed from `settings.gradle.kts` | Was only for sherpa-onnx; replaced by local AAR curl. |

---

## 12B NPU build pipeline (active mission — first in the world)

No Gemma 4 12B NPU build targeting SM8750 HTP exists publicly. We are building it.

**Pipeline:**
1. Source: `google/gemma-4-12B-it-qat-q4_0-unquantized` (Apache 2.0, QAT pre-structured for INT4 NPU)
2. Compile: `scripts/compile_gemma.py` via `colab exec -f` → QAI Hub → Snapdragon 8 Elite target → `gemma4_12b_qat_htp.dlc`
3. Package: `ort_engine` C++ binary (ONNX RT + QNN EP for aarch64-android) bundled in APK assets
4. Deploy: `DaemonLauncher.launch()` → `sh -T-` detach → root oom_score_adj -1000 → LMKD immune
5. Bridge: `NpuClient` → `http://127.0.0.1:8080/generate` → Kotlin app streams tokens
6. Publish: `Mer0vin8ian/gemma-4-12B-it-npu-sm8750` on HF

Notebook: `notebooks/gemma4_12b_npu_litertlm_build.ipynb` — full QAI Hub + ADB tunnel pipeline.

**Layer split (SM8750 static graph partitioning):**
- Hexagon HTP (NPU): ~5.5GB — attention MatMul, dense layers
- Adreno GPU: ~0.6GB — non-standard activations (QAT minimizes this)
- CPU: ~0.1GB — token embedding, argmax, sync

QAT eliminates GPU fallback — no manual layer routing needed, QAI Hub handles it automatically.

---

## Roadmap (in order)

**IMMEDIATE (blocking everything else):**
0. **Fix Termux push to `c10vis-poem/Novus.Agenti`** — `nano ~/NeuroOmni/.git/config`, find `[remote "novus"]`, replace `YOURTOKEN` in url with real token, save, then `git push novus origin/claude/resume-review-3kxeh8:main`

**Track 1 (this repo — Quick Ship):**
1. **Fix startup crash** — `adb logcat -d | grep -E "FATAL|AndroidRuntime|com.horizons"` to get the stack. Most likely: Sherpa-ONNX JNI extraction or `CrashRecorder`/`AppStateStore` `onCreate()`. Track 1's only P0 blocker.
2. **Swap E2B → Gemma Nano V2** as the placeholder model (3.25B, fits Adreno 830 cleanly via raw LiteRT, no AICore needed)
3. **Mic tile gestures** — single floating mic tile: tap = Accessibility text inject; long-press = enter live chat mode. AI chat tile keeps `[🎥]` for vision Q&A
4. **RouterPane voice controls** — voice picker + speed slider (StateFlows + `ENGLISH_VOICES` already wired)

**Track 2 (Novus.Agenti fork — Omni Claw):**
5. **Validate NPU pipeline with E2B** — `MODEL_SIZE=E2B colab exec -f scripts/compile_gemma_qairt.py` → push `.dlc` → run via daemon
6. **Build `ort_engine` binary** — ONNX RT + QNN EP for aarch64-android; bundle in APK assets at `assets/ort_engine` + `assets/qnn/lib*.so`
7. **Build `genie_engine` binary** — C++ binary: loads Genie SDK + `.bin`, serves HTTP on 8080. Nobody has built this yet. Required for Qwen3-VL-8B path.
8. **Compile Qwen3-VL-8B** — `scripts/compile_qwen3_vl.py` on Colab. Needs HF_TOKEN + QAI_HUB_API_TOKEN. Output: `qwen3_vl_8b_instruct_htp_N.bin` → push to `/sdcard/Download/`
9. **Compile 12B QAT** — `MODEL_SIZE=12B colab exec -f scripts/compile_gemma_qairt.py` (after E2B proves the chain)
10. **WatchdogService** — separate-process FGS (`android:process=":watchdog"`), monitors daemon + main app PIDs, snapshots agent state, hot-rehydrates without daemon noticing
11. **Three-tier screen vision** — `ScreenshotObserver` (FileObserver on `/sdcard/Pictures/Screenshots/`) + camera tile in the AI chat floating tile
12. **Outbound desktop-cluster clients** — Razr is client-only in the P2P mesh (Razr → Jetson, never inbound). `JetsonClient` / `RubikClient` as `LlmRuntime` impls hitting `*.tailnet.ts.net:8080` over Tailscale. Daemon stays loopback `127.0.0.1:8080`. No bind change.

**Termux environment (parallel track):**
13. **VNC + XFCE** — `sabamdarif/termux-desktop`, obsidian GTK theme, AVNC from tablet on port 5901
14. **Claude Code Android** — `ferrumclaudepilgrim/claude-code-android` proot-Ubuntu path (stable); user has fork at `c10vis-poem`
15. **Matrix Termux theme** — green `#00FF41` on black, `cmatrix` background, Termux:Float overlay
16. **Colab CLI in Termux** — `pip install colab-cli && colab auth login` → run compile scripts from phone
17. **Two-session Claude Code pipe** — Run two `claude` CLI sessions side by side in Termux (split pane or separate sessions via tmux):
    - Session A: Claude Code on `NeuroOmni.Vag-Agenti` / `Novus.Agenti` — Android codebase work
    - Session B: Claude Code driving `colab exec -f scripts/compile_*.py` → QAI Hub compile jobs
    - Output piped between sessions via `claude --print` or shared temp files
    - Session resumption: `claude --resume <session-id>` continues any prior session by ID (including web sessions)
    - Full pipeline control from the phone — no laptop required

**Shared / future:**
17. **GenAI ecosystem integration** — ML Kit GenAI APIs, AI Edge Gallery, AI Studio MCP builder tools, Vertex AI for Project POEM fine-tuning
18. **Nano-wiki RAG** — per-project knowledge bases; SQLite FTS5 in `getExternalFilesDir()`; top-5 chunks injected at inference time. 4 files: `WikiStore.kt`, `WikiManager.kt`, updated `LibraryPane.kt`, hook in `sendChat()`
19. **MacroDroid bridge** (peer to TaskerBridge), **Storage architecture**, **GUI redesign**

---

## Key files (current)

```
horizons/src/main/java/com/horizons/
  HorizonsApplication.kt                    — app singleton; llmRuntime, kokoroManager, tts, voiceLoop, chatMode, resolveModelPath()
  core/llm/LiteRtRuntime.kt                 — Backend.GPU(), preWarm(), two-attempt buildEngine(), stream/streamAudio/streamImage
  core/llm/LlmRuntime.kt                    — interface
  core/voice/SherpaOnnxTtsClient.kt         — Kokoro TTS; 28 ENGLISH_VOICES; AudioTrack playback; barge-in stop
  core/voice/KokoroModelManager.kt          — downloads + extracts kokoro-multi-lang-v1_0; KokoroSetupState machine
  core/llm/NpuClient.kt                     — LlmRuntime socket stub → ort_engine daemon at 127.0.0.1:8080
  core/shell/DaemonLauncher.kt              — sh -T- detach launcher; root oom_score_adj -1000
  audio/AudioRecorder.kt                    — PCM16 mono @16kHz
  audio/VadFactory.kt                       — Silero VAD
  audio/VoiceLoopController.kt              — Mode B/A voice loop
  ui/panels/ModelsPane.kt                   — model status, download instructions, browse button
  ui/panels/RouterPane.kt                   — Kokoro download state machine, voice/speed TODO stub at line 138
  core/state/AppStateStore.kt               — persistent KV store (model path, TTS voice/speed, etc.)
  core/perf/GameModeBoost.kt                — Snapdragon Game Mode during inference
  core/shell/TaskerBridge.kt                — Tasker outbound bridge
  accessibility/HorizonsAccessibilityService.kt — dock (mic / screen / stop)
  fgs/LiveChatService.kt                    — Mode B FGS (microphone, voice loop)
  fgs/ScreenShareService.kt                 — Mode A FGS (mediaProjection + microphone, screen capture loop)

horizons/build.gradle.kts
  - litertlm-android:0.13.1 (pinned — do NOT change back to latest.release)
  - onnxruntime-android:1.20.0 (Silero VAD only)
  - sherpa-onnx-1.13.2.aar (local files() reference — CI downloads it)
  - commons-compress:1.27.1 (bzip2 extraction for Kokoro archive)
  - -Xskip-metadata-version-check (Kotlin 2.1 consuming litertlm 0.13.1 built with Kotlin 2.3)

.github/workflows/build-apk.yml
  - Downloads sherpa-onnx-1.13.2.aar via curl before Gradle build
  - Builds horizons debug APK + watchdog debug APK
  - Publishes both to latest-debug release tag
```

---

## 12B NPU daemon architecture (the mission — confirmed by ADB_CLOUD_BUILD doc)

Kotlin app (~150MB footprint) is orchestrator only. Model runs in a detached native daemon.

```
[Kotlin App UI]
  ↓ DaemonLauncher.launch() — sh -T- detach, oom_score_adj=-1000
[ort_engine daemon — aarch64-android native binary]
  ↓ loads gemma4_12b_qat_htp.dlc → Hexagon HTP (NPU)
  ↓ serves http://127.0.0.1:8080/generate
[NpuClient] ← Kotlin app streams tokens from localhost
[SecureResourceRelay.kt] ← Kotlin app proxies mic/screen/hardware to daemon (NOT YET BUILT)
```

**Why daemon, not in-process:** LiteRT-LM in-process hits Android LMKD at ~7.5GB. Standard app process oom_score_adj is 0–900+ — LMKD kills it. Native daemon via `sh -T-` inherits shell priority; with root, oom_score_adj = -1000 (unkillable). The 6.2GB model lives in the daemon's memory space, invisible to LMKD's app-level accounting.

**ADB shell -950 loophole (from ADB_CLOUD_BUILD doc):**
- Processes spawned via `adb shell` get oom_score_adj = -950 by kernel — only init/vold are lower
- `DaemonLauncher` uses `sh -T-` detach + root `oom_score_adj = -1000` to approximate this
- Full -950 without root requires Wireless Debugging enabled (Shizuku/LADB pattern — future)

**Kotlin app retains all Android APIs** — Game Boost hooks, mic, screen, TTS, STT, agentic tools all run normally inside the app. App footprint stays ~150MB so LMKD leaves it alone.

---

## Termux environment (user's phone — Razr Ultra 2025)

**Device:** Motorola Razr Ultra 2025 · SM8750 · 16GB LPDDR5X · Adreno 830 · Hexagon HTP v75
**User is on phone only — no laptop.**

### Current Termux state
- Repo cloned at `~/NeuroOmni` = `/data/data/com.termux/files/home/NeuroOmni`
- `novus` remote points to `c10vis-poem/Novus.Agenti` but has literal "YOURTOKEN" in URL — NOT fixed yet
- `~/.git-credentials` does NOT exist
- No root. No Shizuku (rejected — shuts off every 10 minutes).

### Termux setup TODO (in order, not yet done)
1. **Fix Novus.Agenti push** — edit `~/NeuroOmni/.git/config`, replace YOURTOKEN in novus remote URL
2. **Claude Code Android** — use `ferrumclaudepilgrim/claude-code-android` (3 paths: native+glibc-runner, proot-Ubuntu, AVF VM). proot-Ubuntu is most stable. v2.1.113+ broke native Termux (switched to glibc binary). User has a fork in `c10vis-poem` account.
3. **VNC + XFCE desktop** — `sabamdarif/termux-desktop` script handles everything including hardware accel. Access from Samsung tablet via AVNC app on port 5901. Target: XFCE with obsidian GTK theme.
4. **Termux:Float + Termux:Styling** — from F-Droid ONLY (not Play Store). Matrix theme: green `#00FF41` on black `#000000` in `~/.termux/colors.properties`. `cmatrix` package for rain effect.
5. **termux-gui plugin** — native Android GUI components from CLI (no VNC/X11 needed for simple UIs)
6. **proot-distro Ubuntu** — `proot-distro install ubuntu` — full Linux for Claude Code + compile tools
7. **Colab CLI** — `pip install colab-cli && colab auth login` — run compile scripts from Termux directly

### CRITICAL: Mobile paste limitations
URLs in Claude chat responses get auto-rendered as markdown hyperlinks on mobile. When the user copies a command containing a URL from the chat and pastes it into Termux, the markdown `[text](url)` syntax breaks the command. This is why `$T` didn't expand — the whole set-url command was malformed on paste.

**Rules for future sessions:**
- NEVER embed tokens or long URLs directly in commands for the user to paste
- Use shell variables: give `T=TOKEN` as one line (user pastes just the token), then use `$T` in subsequent short commands
- For git auth: use interactive prompt (unset credential helper, push, git asks username/password) so user only pastes the token at a short prompt
- For long git remote URLs: edit `~/.git/config` or `.git/config` directly in nano — easier on mobile than pasting a 70+ char command
- Keep every paste-able command under ~50 chars where possible

### Two daemon architectures — DO NOT CONFUSE
| | `ort_engine` + `.dlc` | `genie_engine` + `.bin` |
|---|---|---|
| **For** | Gemma 4 12B QAT | Qwen3-VL-8B, Qwen2.5-VL-7B |
| **Compiled by** | QAI Hub (GGUF → DLC) | QAI Hub via qai_hub_models export |
| **Runtime** | ONNX Runtime + QNN EP | Genie SDK (LLM-optimized, built on QNN) |
| **Output** | `gemma4_12b_qat_htp.dlc` | `qwen3_vl_8b_instruct_htp_N.bin` |
| **Binary exists?** | `ort_engine` — not yet built | `genie_engine` — nobody has built this yet |
| **QAI Hub device** | `"Snapdragon 8 Elite"` | `"Snapdragon 8 Elite QRD"` |

`genie_engine` binary: C++ binary that loads Genie SDK + `.bin` model, serves HTTP on 8080. Must be built from source — it does not exist publicly yet.

### Qwen3-VL-8B vs Qwen2.5-VL-7B
- Qwen3-VL-8B: 75.58 reasoning benchmark. Needed for complex tasks (Jetson flashing, troubleshooting).
- Qwen2.5-VL-7B: 53.74. Not adequate for the use case.
- Qwen3-VL is NOT in qai_hub_models yet. Compile script adapts Qwen2.5-VL class + Qwen3 weights.
- `--target_runtime qnn_context_binary` — correct current flag (was `qnn_lib_aarch64_android` — deprecated, fixed)

### Personal GitHub
- Username: `c10vis-poem`
- Personal fork target: `c10vis-poem/Novus.Agenti` (private, exists)
- Org: `M0DU14R-SYSx-inc` (main project home)
- HuggingFace: `Mer0vin8ian`
- Claude Code web session is scoped to `M0DU14R-SYSx-inc/NeuroOmni.Vag-Agenti` only — cannot push to personal repo from this session. Use Termux git for that.

---

## Hard rules (non-negotiable)

- Never push `main` without explicit user permission. Working branch: `claude/resume-review-3kxeh8`.
- Never commit credentials. `release/debug.keystore` is the documented exception.
- Never `--no-verify`, never `push --force`, never `reset --hard` without confirming.
- No CPU fallback. No cloud inference pathway in the main app runtime.
- No new abstractions beyond what the task requires.
- No comments unless the WHY is non-obvious.
- No piecemealing — multi-part work fans out in parallel.

---

## Build / CI

- **AGP** 8.8.0 · **Kotlin** 2.1.0 · **compileSdk** 35 · **minSdk** 31 · **JDK** 17
- **ABI:** arm64-v8a only
- **Signing:** `release/debug.keystore` (committed by design — stable signature = APK installs as update)
- **CI:** `.github/workflows/build-apk.yml` — builds APK on every push, publishes to `latest-debug` release
- **APK download:** `github.com/M0DU14R-SYSx-inc/NeuroOmni.Vag-Agenti/releases/tag/latest-debug`
- **Current green build:** `1f65108` (run #294)

---

## Brand

- Background: `#222C34` · Surface: `#35414A` · Primary teal: `#2DD4D9`
- Highlight teal: `#4FE7EC` · Icon backplate: `#050709` · Action yellow: `#F5C518`
- Backdrop: pure Compose `Brush.radialGradient` — NOT XML shape (painterResource on `<shape>` crashes)
