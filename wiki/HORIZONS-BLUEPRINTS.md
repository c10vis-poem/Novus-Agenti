# HORIZONS BLUEPRINTS — AESOP Reference Manual

> Structured as an AESOP-format technical manual: 11 sections covering
> project identity through troubleshooting, populated with real Horizons
> source code, real module symbols, and real device profiles.
>
> Canonical. Every session reads this alongside CLAUDE.md before touching code.

---

## 1. Project Overview

**Horizons** is an NPU-first agentic AI assistant running as a native
Android app on the Motorola Razr Ultra 2025 (Snapdragon 8 Elite SM8750).

It is NOT a chat wrapper with an API key. It is a system that:
- Ships empty, boots empty. No model baked in.
- Watches for runtime binaries and model files to land via `ModelImportActivity`.
- Runs inference on the Hexagon NPU via a detached native daemon process.
- Hot-swaps runtimes: GGUF today, QNN context binary tomorrow.
- Works in airplane mode. Cloud API is an explicit fallback, not a dependency.

### Architecture Flow
```
User Input
  ↓
VoiceInteractionSession / ChatPane UI
  ↓
HorizonsApplication (orchestrator)
  ↓
AgentLoop (22 tools, ReAct cycle)
  ↓
LlmRuntime interface
  ├── NpuClient → llama-server daemon (127.0.0.1:8080, NPU via ggml-hexagon)
  └── CloudLlmRuntime → OpenRouter / SambaNova / HF Inference (HTTPS)
```

### Identity
- Package: `com.horizons`
- Banner: Omni Claw
- Project: Novus Agenti — Cl0vis × Mer0vin6ian production
- Device: Motorola Razr Ultra 2025, SM8750, 16GB, Hexagon HTP v75

---

## 2. File Directory

### Source Tree
```
horizons/src/main/java/com/horizons/
├── HorizonsApplication.kt          App entry, runtime wiring, chat dispatch
├── MainActivity.kt                 Single-activity Compose host
├── ModelImportActivity.kt          SAF file picker for .gguf/.bin/.onnx/.so
│
├── accessibility/
│   └── HorizonsAccessibilityService.kt   Screen element identification
│
├── assist/
│   ├── HorizonsRecognitionService.kt     Voice recognition service
│   ├── HorizonsVoiceInteractionService.kt   System assistant binding
│   ├── HorizonsVoiceInteractionSession.kt   Assistant session handler
│   └── HorizonsVoiceInteractionSessionService.kt
│
├── audio/
│   ├── AudioRecorder.kt            PCM capture, 16kHz mono
│   ├── RmsVadDetector.kt           RMS-based voice activity (fallback)
│   ├── SileroVadDetector.kt        Silero VAD ONNX (<1ms, 2MB)
│   ├── VadDetector.kt              VAD interface
│   ├── VadFactory.kt               Silero-first, RMS fallback
│   └── VoiceLoopController.kt      Continuous listen → transcribe → respond
│
├── core/
│   ├── agent/
│   │   ├── AgentLoop.kt            ReAct cycle, 22 tools, 12 max turns
│   │   ├── AgentNotificationListener.kt   Reads active notifications
│   │   ├── AgentSystemPrompt.kt    System prompt + tool schema
│   │   ├── AgentTool.kt            Sealed class: 22 tool types
│   │   ├── AgentToolParser.kt      XML tag parser for <tool>...</tool>
│   │   └── tools/
│   │       ├── AlarmTool.kt        Set alarm / timer
│   │       ├── AppTool.kt          Launch / list apps
│   │       ├── CalendarTool.kt     Read / create events
│   │       ├── ClipboardTool.kt    Read / write clipboard
│   │       ├── ContactsTool.kt     Search contacts
│   │       ├── DeviceTool.kt       WiFi, BT, volume, brightness, DND, flashlight
│   │       ├── MediaTool.kt        Play / pause / skip media
│   │       ├── NotificationTool.kt Read / post notifications
│   │       └── SystemInfoTool.kt   Battery, network, storage
│   │
│   ├── diag/
│   │   └── Breadcrumb.kt           Crash breadcrumb trail
│   │
│   ├── llm/
│   │   ├── LlmRuntime.kt           Interface: stream(), capabilities, perfMetrics, thinkingActive
│   │   ├── NpuClient.kt            Daemon bridge (ORT + OpenAI-compat SSE)
│   │   └── CloudLlmRuntime.kt      OpenRouter / SambaNova / HF cloud
│   │
│   ├── log/
│   │   ├── CrashRecorder.kt        Uncaught exception handler
│   │   └── InteractionLogger.kt    Structured interaction log (unwired)
│   │
│   ├── perf/
│   │   ├── GameModeBoost.kt        ADPF GameState boost for inference
│   │   └── PerfHintSession.kt      PerformanceHintManager wrapper
│   │
│   ├── screen/
│   │   └── ScreenshotCapture.kt    MediaProjection screenshot (unwired)
│   │
│   ├── shell/
│   │   ├── DaemonLauncher.kt       Launch/stop/check native daemon
│   │   ├── NativeBinaryInstaller.kt  Binary presence check (ORT + llama)
│   │   ├── SecureResourceRelay.kt  Secure shell token relay (unwired)
│   │   └── TaskerBridge.kt         Shell exec + Tasker task trigger
│   │
│   ├── state/
│   │   ├── AppStateStore.kt        Encrypted KV store (API keys, prefs)
│   │   ├── ChatHistoryStore.kt     JSON-backed chat session persistence
│   │   └── SavedCommandStore.kt    Prompt/script library
│   │
│   └── voice/
│       ├── HorizonsTtsService.kt   TTS service wrapper
│       ├── KokoroModelManager.kt   Kokoro ONNX model lifecycle
│       └── SherpaOnnxTtsClient.kt  sherpa-onnx TTS/STT client
│
├── fgs/
│   ├── CliffordBootReceiver.kt     BOOT_COMPLETED → start watchdog
│   ├── CliffordService.kt          Foreground service: daemon watchdog, 15s CRS loop
│   ├── LiveChatService.kt          Voice conversation FGS
│   └── ScreenShareService.kt       MediaProjection FGS
│
├── provider/
│   └── SettingsStore.kt            ContentProvider for settings
│
├── tiles/
│   └── TerminalTile.kt             Quick Settings tile → terminal
│
└── ui/
    ├── HomeGrid.kt                 7-tile hub layout
    ├── PaneBackgrounds.kt          Radial gradient backgrounds
    ├── panels/
    │   ├── ArtifactsPane.kt        Generated content archive
    │   ├── ChatPane.kt             AI conversation (Standard/Live/Voice)
    │   ├── HorizonsPane.kt         System identity tile
    │   ├── MonitorPane.kt          Model library + API vault + prompt cache
    │   ├── RouterPane.kt           NPU/Cloud routing dashboard
    │   ├── SettingsPane.kt         Config, permissions, diagnostics
    │   └── TerminalPanel.kt        Shell interface, Matrix aesthetic
    └── theme/
        └── HorizonsTheme.kt        Colors, typography, shapes
```

### Supporting Files
```
daemon/src/                          ort_engine C++ daemon (735 lines)
  engine.cpp, http_server.cpp, tokenizer.cpp, sampler.h, main.cpp

.github/workflows/
  build-apk.yml                      APK + ort_engine CI
  build-llama-server.yml             llama-server + ggml-hexagon skel CI

skills/
  memory-as-skill/                   Session-persistent project memory
  horizons-wiki/SKILL.md             Wiki maintenance skill
  project-memory/SKILL.md            Project context skill
  termux-mobile-dev/SKILL.md         Phone dev environment skill

wiki/
  HORIZONS-BLUEPRINTS.md             THIS FILE
  REFERENCE-IMPLEMENTATIONS.md       Fork/paper dissection
  GPT-DAEMON-REFERENCE.md            Daemon architecture notes
  NPU-RUNTIME-PATHS.md              Runtime formats + SDK paths
  FEATURE-SPEC.md                    UI tile visual spec

models/manifest.yaml                 Model registry + compile config
scripts/compile_qwen3_5_9b.py        ONNX export + QAI Hub compile
release/debug.keystore               APK signing (committed by design)
```

---

## 3. First-Time Setup

### Build from Source
```
Prerequisites: JDK 17, Android SDK (compileSdk 35), NDK (arm64-v8a)

git clone https://github.com/c10vis-poem/Novus-Agenti.git
cd Novus-Agenti
./gradlew assembleDebug
# APK at horizons/build/outputs/apk/debug/horizons-debug.apk
```
Build config: AGP 8.8.0, Kotlin 2.1.0, minSdk 31, arm64-v8a only.

### Install from Release
Download `horizons-debug.apk` from the `latest-debug` GitHub Release.
Install via `adb install` or SAF file manager on-device.

### Post-Install
1. **Grant All Files access** — Settings → Apps → Horizons → Permissions →
   Files and media → Allow management of all files. Required for model
   import from Download/.
2. **Import GGUF model** — Open Horizons → tap + in Chat → Attach File →
   navigate to the .gguf file (e.g. `Qwen3.5-9B-Q4_0.gguf` in Download/).
   ModelImportActivity copies it to app storage and registers it.
3. **Import runtime libs** (if not APK-packaged) — same flow for
   `libggml-htp-v79.so` and other skel files. They register into
   `RUNTIME_FILES` so `DSP_LIBRARY_PATH` finds them.

---

## 4. Launching Horizons

### Standard Launch (app icon)
Tap the Horizons icon → HomeGrid shows 7 tiles → tap CHAT to start.
CliffordService starts automatically on app launch and begins the daemon
watchdog loop.

### Voice Assistant Launch (hold assistant button)
Horizons registers as a `VoiceInteractionService`. Long-press the assistant
button → `HorizonsVoiceInteractionSession.onHandleAssist()` fires →
opens the voice conversation flow.

### Share Intent Launch (model import)
Share a `.gguf`, `.bin`, `.onnx`, or `.so` file to Horizons →
`ModelImportActivity` receives the intent, copies the file, and registers
it for the appropriate runtime family.

### CLI Launch (adb)
```sh
adb shell am start -n com.horizons/.MainActivity
```

---

## 5. Device Profiles

| Device | SoC | NPU | NDEV | DSP Skel | Status |
|--------|-----|-----|------|----------|--------|
| Razr Ultra 2025 | SM8750 (8 Elite) | Hexagon HTP v75, 45 TOPS | 2 | libggml-htp-v75.so | Primary target |
| Snapdragon 8 Elite (generic) | SM8750 | Hexagon HTP v79 | 2 | libggml-htp-v79.so | Supported |
| Snapdragon 8 Gen 3 (generic) | SM8650 | Hexagon HTP v73 | 2 | libggml-htp-v73.so | Supported |
| Non-Snapdragon / older | — | None | 0 | — | Cloud-only fallback |

### Key Environment Variables (llama-server daemon)
| Variable | Value | Purpose |
|----------|-------|---------|
| `GGML_HEXAGON_NDEV` | `2` | Split model across 2 HTP sessions (~2.85GB each) |
| `DSP_LIBRARY_PATH` | `nativeLibraryDir:filesDir` | Where ggml-hexagon searches for skel .so files |
| `LD_LIBRARY_PATH` | `nativeLibraryDir:filesDir` | Shared library search path (libonnxruntime, etc.) |

### Size Envelope (Hard Caps)
| | Size | Notes |
|---|---|---|
| Target | 5.5 GB | Shoot for this |
| Ideal ceiling | 6.0 GB | Acceptable |
| Redline | 7.0-7.2 GB | Non-negotiable |

### NPU Constraints
- Static quantization ONLY (dynamic quant not supported on Hexagon HTP)
- NPU-compatible quants: Q4_0, IQ4_NL, W4A16 (per-group)
- NOT compatible: Q4_K_M, Q5_K_M, Q6_K (K-quants fall back to CPU)
- Single NPU context: `QNN_GRAPH_CONFIG_MAX_CONTEXTS=1`
- 4GB per HTP session (32-bit cDSP ceiling) — NDEV=2 splits to stay under

---

## 6. Module Reference

### HorizonsApplication.kt — App Entry
| Symbol | Type | Purpose |
|--------|------|---------|
| `llmRuntime` | `LlmRuntime` | Active inference backend |
| `cloudRuntime` | `CloudLlmRuntime` | Cloud API fallback |
| `chatMessages` | `MutableStateFlow<List<ChatMessage>>` | Live message state |
| `chatBusy` | `MutableStateFlow<Boolean>` | Generation lock |
| `pendingScreenJpeg` | `MutableStateFlow<ByteArray?>` | Screenshot for vision Q&A |
| `chatMode` | `MutableStateFlow<ChatMode>` | Standard / Live / Voice |
| `voiceLoop` | `VoiceLoopController` | Continuous voice pipeline |
| `chatHistory` | `ChatHistoryStore` | Session persistence |
| `sendChat(text)` | Function | Dispatch text to LLM via agent loop |
| `screenAsk(jpeg, text)` | Function | Vision Q&A with attached image |
| `activateNpuRuntime()` | Function | Switch to NpuClient backend |
| `stopAll()` | Function | Cancel active generation |

### LlmRuntime.kt — Inference Interface
| Symbol | Type | Purpose |
|--------|------|---------|
| `stream(prompt)` | `Flow<String>` | Token stream |
| `streamAudio(wav, prompt)` | `Flow<String>` | Audio input (default: text fallback) |
| `streamImage(jpeg, prompt)` | `Flow<String>` | Image input (default: text fallback) |
| `backendStatus` | `StateFlow<String>` | Human-readable backend identity |
| `perfMetrics` | `StateFlow<PerfMetrics?>` | Tokens/sec, first-token latency |
| `thinkingActive` | `StateFlow<Boolean>` | True during `<think>` blocks |
| `capabilities` | `Capabilities` | Vision, thinking, tool-calling, context length |
| `preWarm()` | Function | Runtime warm-up (daemon startup) |

### NpuClient.kt — Daemon Bridge
| Symbol | Type | Purpose |
|--------|------|---------|
| `openAiProtocol` | `Boolean` | true=llama-server, false=ort_engine |
| `rawStream(prompt)` | `Flow<String>` | SSE parsing + think-token suppression |
| `isDaemonReachable()` | `Boolean` | GET /health → 200 check |
| `_thinkingActive` | `MutableStateFlow<Boolean>` | Structured think state |

### DaemonLauncher.kt — Native Process Lifecycle
| Symbol | Type | Purpose |
|--------|------|---------|
| `launch(engineArgs)` | `suspend → Result<DaemonHandle>` | Start daemon, return PID |
| `isRunning()` | `Boolean` | Check via `pidof` |
| `stop()` | `Unit` | SIGTERM to daemon PID |
| `resolveEngineFile()` | `File` | nativeLibraryDir (primary) or filesDir (legacy) |
| `ENGINE_BINARY` | `String` | `"ort_engine"` |
| `LLAMA_BINARY` | `String` | `"llama-server"` |
| `PACKAGED_ENGINE_LIB` | `String` | `"libort_engine.so"` |
| `PACKAGED_LLAMA_LIB` | `String` | `"libllama_server.so"` |
| `familyFor(modelPath)` | `String` | .gguf → llama-server, else → ort_engine |

### CliffordService.kt — Daemon Watchdog
| Symbol | Type | Purpose |
|--------|------|---------|
| `onStartCommand()` | FGS lifecycle | `startForeground()` first, then daemon launch |
| CRS loop (15s) | Handler | Check PID → check /health → relaunch if dead |
| `acquireNpuPerfLock()` | Function | Reflection: `npu` system service, PERF_MODE_HIGH |
| `releaseNpuPerfLock()` | Function | Release NPU perf lock |

### AgentLoop.kt — Agentic Tool Dispatch
| Symbol | Type | Purpose |
|--------|------|---------|
| `run(userMessage)` | `Flow<String>` | Full ReAct cycle |
| `toolApprover` | `suspend (String) -> Boolean` | Tool confirmation callback (default: auto-approve) |
| `executeTool(tool)` | `suspend → ToolResult` | Dispatch to 22 tool implementations |
| `MAX_TURNS` | `Int` | 12 |

### NativeBinaryInstaller.kt — Binary Presence
| Symbol | Type | Purpose |
|--------|------|---------|
| `isInstalled(context)` | `Boolean` | Check for ort_engine OR llama-server |
| `installedBinaryName(context)` | `String?` | Which binary is present |
| `hasLlamaDeps(context)` | `Boolean` | Check ggml-hexagon skel files |
| `QNN_LIBS` | `List<String>` | QNN runtime libraries |
| `LLAMA_LIBS` | `List<String>` | ggml-hexagon libraries |

### GameModeBoost.kt — Performance
| Symbol | Type | Purpose |
|--------|------|---------|
| `enter(context)` | Function | ADPF GameState MODE_GAMEPLAY_UNINTERRUPTIBLE |
| `exit(context)` | Function | End boost |
| `gameBoosted(context)` | `Flow<T>` extension | Wrap a flow in enter/exit |

---

## 7. Model Guide

### Primary: GGUF + llama-server + ggml-hexagon
| Model | Size | Quant | NDEV | Runtime |
|-------|------|-------|------|---------|
| Qwen3.5-9B | ~5.7 GB | Q4_0 | 2 | llama-server |
| Gemma 4 12B QAT | ~6.0 GB | Q4_0 | 2 | llama-server |
| Qwen3-4B | ~2.5 GB | Q4_0/IQ4_NL | 1 | llama-server |

### Secondary: ORT + QNN (compile track)
| Model | Size | Format | Runtime |
|-------|------|--------|---------|
| Qwen3.5-9B | ~5.5 GB | qnn_context_binary (W4A16) | ort_engine |

### Reference: ExecuTorch + QNN
| Model | Format | Runtime | Source |
|-------|--------|---------|--------|
| Various | .pte | executorch_engine | EdgeAIApp-ExecuTorch |

### Sidecar (always-on, CPU)
| Model | Size | Format | Purpose |
|-------|------|--------|---------|
| Silero VAD | ~2 MB | ONNX | Voice activity detection (<1ms) |
| Moonshine STT | ~80 MB | ONNX | Speech-to-text |
| Kokoro TTS | ~320 MB | ONNX | Text-to-speech |

### Quant Compatibility (Hexagon HTP v75)
- **NPU-compatible:** Q4_0, IQ4_NL, W4A16 (per-group), QAT INT4
- **CPU-only:** Q4_K_M, Q5_K_M, Q6_K (K-quants), GPTQ

---

## 8. Agent Commands

### Tool List (22 tools)
| Tool | Args | Category |
|------|------|----------|
| `launch_app` | `app: String` | Device control |
| `list_apps` | — | Device control |
| `set_alarm` | `time, label, days` | Device control |
| `set_timer` | `duration_seconds, label` | Device control |
| `read_calendar` | `lookahead_days` | Information |
| `create_event` | `title, start, end, description, location` | Information |
| `search_contacts` | `query` | Information |
| `wifi` | `action: on/off/status` | Device control |
| `bluetooth` | `action: on/off/status` | Device control |
| `volume` | `stream, level` | Device control |
| `brightness` | `level: 0-255` | Device control |
| `dnd` | `mode: on/off/priority` | Device control |
| `flashlight` | `action: on/off` | Device control |
| `media` | `action: play/pause/next/prev` | Device control |
| `read_notifications` | — | Information |
| `post_notification` | `title, body, channel` | Device control |
| `read_clipboard` | — | Information |
| `write_clipboard` | `text` | File/data |
| `shell` | `command` | Shell |
| `http_fetch` | `url, method, body, bearer_token_key` | Network |
| `web_search` | `query, max_results` | Network |
| `battery` / `network` / `storage` | — | Information |
| `tasker_task` | `task_name, param1, param2` | Shell |
| `done` | — | Control |

### ReAct Cycle
1. User message → build prompt with system instructions + tool schemas
2. LLM generates response, may include `<tool>{...}</tool>` tags
3. `AgentToolParser` extracts tool calls from stream
4. `toolApprover` callback fires (default: auto-approve)
5. Tool executes → result injected as `<result>...</result>`
6. LLM re-invoked with updated context
7. Repeat until `done` tool or MAX_TURNS (12)

---

## 9. Editing Artifacts & Skills

### Add a New Runtime Binary
1. Cross-compile the binary for `aarch64-linux-android` (NDK)
2. Rename to `lib<name>.so` (required for jniLibs packaging)
3. Place in `horizons/src/main/jniLibs/arm64-v8a/`
4. Add the constant to `DaemonLauncher.kt` companion object
5. Update `NativeBinaryInstaller.kt` to check for it
6. Update `DaemonLauncher.familyFor()` to route the model format

### Add a New Agent Tool
1. Add a variant to `sealed class AgentTool` in `AgentTool.kt`
2. Add parsing in `AgentToolParser.kt`
3. Add execution in `AgentLoop.executeTool()`
4. Add serialization in `AgentLoop.toolToJson()`
5. Add the tool schema to `AgentSystemPrompt.SYSTEM`

### Modify Device Profiles
Edit `DaemonLauncher.kt`:
- `GGML_HEXAGON_NDEV` controls HTP session split
- `DSP_LIBRARY_PATH` controls skel search
- Engine args in `launch(engineArgs)` control context size, flash attention

### Swap Models
Use `ModelImportActivity` to import new .gguf/.bin files. The app detects
the format via `DaemonLauncher.familyFor()` and routes to the correct
runtime automatically.

---

## 10. Troubleshooting

### "NPU daemon not reachable"
**Cause:** llama-server/ort_engine not running or not responding on port 8080.
**Fix:** Check CLIFFORD notification (foreground service indicator). If
missing, CliffordService crashed. Check logcat for `DaemonLauncher` errors.
Verify the binary exists in `nativeLibraryDir`:
```sh
adb shell ls -la /data/app/*/com.horizons*/lib/arm64/
```

### "Engine binary not found"
**Cause:** Neither the APK-packaged binary nor the legacy filesDir copy exists.
**Fix:** Reinstall from latest-debug release. For llama-server, verify
`build-llama-server.yml` CI produced `libllama_server.so` in the release.

### "NPU offload inactive" (CPU-only decode speeds)
**Cause:** DSP skel files missing or DSP_LIBRARY_PATH wrong.
**Fix:**
1. Verify skel files exist: `adb shell ls /data/app/*/com.horizons*/lib/arm64/libggml-htp-v*.so`
2. If missing, import via ModelImportActivity or reinstall APK
3. Check logcat for `ggml_hexagon` errors about missing skels
4. Verify `GGML_HEXAGON_NDEV=2` in DaemonLauncher env prefix

### "Model too large"
**Cause:** Model exceeds the 7.2 GB redline or single HTP session 4GB ceiling.
**Fix:** Use NDEV=2 to split across 2 HTP sessions. Verify model is Q4_0
(not Q4_K_M). Check: `5.7GB / 2 sessions = 2.85GB < 4GB ceiling`.

### "Cloud API error"
**Cause:** API key missing, expired, or endpoint unreachable.
**Fix:** Settings → API Keys → verify the key is set. Check network
connectivity. CloudLlmRuntime tries OpenRouter → SambaNova → HF in order.

### "App crash on launch" (historical, fixed)
**Cause:** :clifford process raced main process to open EncryptedSharedPreferences.
**Fix:** Already fixed — AppStateStore degrades to in-memory on Keystore failure.
Verified: 75s stability test PASS, zero clifford kills.

### "Thinking text visible in chat"
**Cause:** Old NpuClient emitted `[Thinking…]` as flat text.
**Fix:** Updated to structured `thinkingActive` StateFlow. ChatPane shows
a "Thinking · · ·" indicator bar. No text emitted to chat messages.

---

## 11. Script & File Log

| File | Location | Purpose | Last Updated |
|------|----------|---------|-------------|
| CLAUDE.md | `/` | Operational manual, SOTU, resume prompt | Session 18 |
| HORIZONS-BLUEPRINTS.md | `wiki/` | This file — architectural blueprint | Session 18 |
| REFERENCE-IMPLEMENTATIONS.md | `wiki/` | Fork/paper dissection | Session 17 |
| GPT-DAEMON-REFERENCE.md | `wiki/` | Daemon architecture notes | Session 12 |
| NPU-RUNTIME-PATHS.md | `wiki/` | Runtime formats + SDK paths | Session 12 |
| FEATURE-SPEC.md | `wiki/` | UI tile visual specification | Session 12 |
| manifest.yaml | `models/` | Model registry + compile config | Session 11 |
| compile_qwen3_5_9b.py | `scripts/` | ONNX export + QAI Hub compile | Session 11 |
| build-apk.yml | `.github/workflows/` | APK + ort_engine CI | Session 12 |
| build-llama-server.yml | `.github/workflows/` | llama-server + skel CI | Session 17 |
| SKILL.md (memory) | `skills/memory-as-skill/` | Session-persistent memory | Session 18 |
| novus-agenti.md | `skills/memory-as-skill/memory/active/` | Live project state | Session 18 |
| HorizonsApplication.kt | `horizons/.../` | App entry, runtime wiring | Session 17 |
| DaemonLauncher.kt | `horizons/.../core/shell/` | Daemon lifecycle | Session 18 |
| NativeBinaryInstaller.kt | `horizons/.../core/shell/` | Binary presence check | Session 18 |
| CliffordService.kt | `horizons/.../fgs/` | Daemon watchdog FGS | Session 17 |
| NpuClient.kt | `horizons/.../core/llm/` | Daemon bridge | Session 18 |
| LlmRuntime.kt | `horizons/.../core/llm/` | Inference interface | Session 18 |
| CloudLlmRuntime.kt | `horizons/.../core/llm/` | Cloud API runtime | Session 18 |
| AgentLoop.kt | `horizons/.../core/agent/` | Agentic tool dispatch | Session 18 |
| ChatPane.kt | `horizons/.../ui/panels/` | Chat UI + thinking indicator | Session 18 |
| GameModeBoost.kt | `horizons/.../core/perf/` | ADPF performance boost | Session 12 |
| engine.cpp | `daemon/src/` | ort_engine HTTP server | Session 11 |
| tokenizer.cpp | `daemon/src/` | BPE tokenizer (needs fixes) | Session 11 |

---

## Revision History

| Date | Session | Change |
|------|---------|--------|
| 2026-07-06 | 17 | Initial creation from Research Dossier + canonical papers + project history |
| 2026-07-07 | 18 | Restructured to AESOP 11-section format with real code, symbol tables, and device profiles. Added structured thinking (E1), capabilities model (E3), agent confirmation (E2), DSP_LIBRARY_PATH fix (D1), NativeBinaryInstaller ggml-hexagon awareness (D3). |
