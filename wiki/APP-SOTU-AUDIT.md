# App SOTU — Honest Audit (device-grounded)

> **State of the Union for the Horizons app.** This is the grounded audit the
> project's own rules demand ("no new feature work until an honest audit
> confirms what's actually true" — session-14 mandate). Produced by the
> operator's on-device session (openwiki chat · z-ai/glm-5.2, running in Termux)
> reading the **real repo + real device**, cross-checked here.
>
> Audit baseline: repo `HEAD ec0ac49`. **Note:** commits after `ec0ac49` on
> `claude/on-device-inference-openwiki-sae7cy` (STT moved to a daemon client,
> README/agnostic reframe, v79 sweep, QAIRT reference) are newer than this
> audit and already address parts of item #1/#2 below.
>
> **STT model = Moonshine small** (operator-confirmed). Runs in the media
> daemon, not in the app; `DaemonSttClient` is model-agnostic.

---

## Operator's remaining needs (explicit)

The app must have:
- **Cloud connectors** + a **WebView socket with a Chromium browser**.
- Connect to: **GCS** (Google Cloud Storage), **OpenRouter**, **OmniRoute**,
  **QAI Hub**, **HuggingFace**, **GitHub**, etc. — *via Termux is fine* if the
  app doesn't do it natively.
- **Tailscale to the home node.**
- **Save chat history for export.**
- Run the whole voice/UI stack **without** the on-device model/HTP (that's
  optional; cloud reasoning + CPU voice is enough to be a working assistant).

---

## What's already done (working or written)

| Layer | Status | Notes |
|---|---|---|
| Android UI (Horizons) | ✅ code complete | 64 Kotlin files, Compose UI, home grid, 6 panels, chat, terminal, browser tab. CI builds it; APK exists. |
| Daemon crash-loop fix | ✅ committed | serve-first: bind :8080 before model loads, /health 503, watchdog doesn't thrash. |
| CliffordService watchdog | ✅ written | 15s CRS loop, exponential backoff, 5-strike cap, BinaryMissing/ModelMissing/Failed states. |
| DaemonLauncher | ✅ written | `sh -T-` detach, reparent to init, OOM immunity, PID resolution. |
| NpuClient | ✅ written (needs update) | HTTP client for :8080 SSE; GenieX plan swaps to :18181 OpenAI format. |
| CloudLlmRuntime | ✅ written | OpenAI-compatible SSE streaming (OpenRouter/SambaNova/HuggingFace). |
| LlmRuntime fallback chain | ✅ written | NpuClient → Cloud → "no backend" fallback. |
| Agent loop + 25 tools | ✅ written | ReAct loop, XML tool parser, 9 executor classes. |
| TTS (Kokoro/Sherpa-ONNX) | ✅ code complete | download/extract/synthesize, 28 voices, system TTS service. **AAR missing from repo** (CI-only). |
| Audio capture | ✅ written | AudioRecorder (16kHz mono PCM), VAD (Silero + RMS fallback). |
| Screen capture | ✅ written | 3 paths: MediaProjection, Accessibility takeScreenshot, Assist API. |
| Voice interaction | ✅ registered | VoiceInteractionService, RecognitionService, AccessibilityService + floating dock. |
| In-app browser | ✅ written | TerminalPanel WebView + `window.OmniClaw` JS bridge. |
| Model import | ✅ written | ModelImportActivity handles .bin/.onnx/.gguf/.tflite/.dlc/.pte/.qnn. |
| GenieX daemon plan | ✅ documented | decision locked: GenieX on HTP SDK, `geniex serve` detached, OpenAI wire on :18181. |
| Knowledge corpus | ✅ committed | 23 files from Drive, byte-faithful. |
| Compile script | ✅ written | `compile_qwen3_5_9b.py` with M-RoPE fix, QAI Hub options. |

## What's broken / missing (remaining needs)

1. **STT — NOT IMPLEMENTED (at ec0ac49).** `LlmRuntime.streamAudio()` was a
   stub discarding audio bytes; no ASR anywhere. **Model = Moonshine small.**
   → *Since addressed:* `DaemonSttClient` now routes PCM→media daemon `/stt`
   (Moonshine runs in the daemon, CPU, no HTP). Media daemon itself still TODO.
2. **Screen vision — STUBBED.** `LlmRuntime.streamImage()` discards JPEG, passes
   text only; no runtime overrides it. `ScreenshotCapture` is dead code (no UI
   launch path for MediaProjection consent). **Need:** override `streamImage()`
   → base64 JPEG in OpenAI vision format (Cloud) and/or GenieX VLM
   (`libgeniex_vlm.so`). Qwen2.5-VL-7B compiled folders exist but are empty.
3. **TTS — CAN'T BUILD LOCALLY.** `horizons/libs/sherpa-onnx-1.13.2.aar` is
   git-ignored, CI-only. **Fix:** `mkdir -p horizons/libs && curl -fL -o horizons/libs/sherpa-onnx-1.13.2.aar https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.2/sherpa-onnx-1.13.2.aar`
4. **Build env — MISSING ON DEVICE.** No JDK/Gradle/Android SDK/gradlew/
   local.properties in Termux. ORT mismatch: `build.gradle.kts` hardcodes 1.20.0
   vs catalog 1.22.0. **Choice:** install openjdk-17 + gradle + SDK for local
   builds, OR keep CI for APKs and focus device effort on the daemon + connectors.
5. **GenieX daemon — NOT BUILT/WIRED.** Plan locked, not implemented. Need
   `geniex serve` running on device (you have prebuilt `geniex-bench` + the
   Q4_0 GGUF + HTP v79 libs; GenieX recommends Q4_0), then swap `NpuClient`
   :8080 `/api/v1/generate` → :18181 `/v1/chat/completions`.
6. **Cloud connectors — PARTIALLY THERE.**
   - Exists: `CloudLlmRuntime` (OpenRouter/SambaNova/HF), `HttpFetch` tool,
     `WebSearch` tool (DuckDuckGo).
   - OpenRouter ✅ works (set key in Settings). HuggingFace ✅ partial (needs HF_TOKEN).
   - OmniRoute ⚠️ not wired (openclaude repo on device — point CloudLlmRuntime at
     its local endpoint or add as provider).
   - QAI Hub ⚠️ CLI not installed (`.qai-hub/config.json` exists; `pip install qai-hub`).
   - GitHub ⚠️ not wired (gh CLI in Termux — agent tool or Termux bridge).
   - GCS ❌ missing (Termux `gsutil` or HTTP API).
   - Tailscale ❌ missing (install in Termux + route to home node).
   - Chat history export ❌ missing (`ChatHistoryStore` saves locally, no export/sync).
7. **WebView/Chromium socket — PARTIALLY THERE.** TerminalPanel WebView +
   `window.OmniClaw` bridge exist; Chromium 149 in Termux. **Need:** wire the
   JS bridge to expose the agent tools + cloud connectors to the browser context,
   not just render pages.
8. **Performance wiring — NOT DONE.** NpuManager perf lock (partial reflection
   impl), GameManager PERFORMANCE mode, manifest `uses-feature`/`HIGH_PERFORMANCE`
   declared but not wired.
9. **App audit — the standing mandate.** Most stale pathway; ground everything
   in real sources (this file) before feature work.

## Priority order (from the audit)

1. **GenieX running on device** — prebuilt `geniex-bench` + Q4_0 GGUF + HTP v79
   libs; unblocks the LLM.
2. **STT** — biggest gap; no voice input works. (Now: media-daemon client done,
   daemon TODO; model = Moonshine small.)
3. **TTS build** — just needs the AAR; code is done.
4. **Screen vision** — override `streamImage()` → base64 JPEG → cloud API or GenieX VLM.
5. **Cloud connectors** — OpenRouter works; add OmniRoute, GitHub, HuggingFace, QAI Hub, GCS.
6. **Tailscale** — route to home node.
7. **Chat history export** — export/sync from `ChatHistoryStore`.
8. **WebView socket** — connect browser to cloud connectors via the JS bridge.
9. **Build env** — install JDK/Gradle/SDK for local builds, or keep CI.

## Device inventory (ground truth, in `/storage/emulated/0/Download/`)

- **SDKs:** QAIRT v2.48.0 (2.2 GB), Hexagon SDK 6.6.0.0 (2.9 GB) — downloaded, not extracted.
- **`qnn_llama_runner.zip`** (34 MB) — `libQnnHtp.so` + HTP skel/stub for v69/v73/v75/**v79** + `libQnnSystem.so` + `libqnn_executorch_backend.so`.
- **`geniex-bench-android-arm64 v0.3.14`** (86 MB) — prebuilt `geniex-bench` + dual backends: **llama.cpp** (ggml HTP v68–v81 + CPU + OpenCL) and **QAIRT** (`libgeniex_core`, **`libgeniex_vlm`**, `libgeniex-proc`, **`libgeniex-proc-vision`**, HTP v79/v81).
- **`hybrid_llama_qnn.pte`** (888 MB) — ExecuTorch hybrid LLaMA for QNN NPU.
- **`model.safetensors`** (535 MB) — unknown (possibly a VLM).
- **Models:** `Qwen_Qwen3.5-9B-Q4_0.gguf` (5.4 GB, main LLM), `mtp-gemma-4-E2B-it-BF16.gguf` (163 MB), `tokenizer.json` (11 MB), `download.bin` (3.6 MB), Qwen2.5-VL-7B compiled folders (empty; sample_inputs only).
- **Prebuilt APKs:** `app-arm64-v8a-release-signed.apk` (22 MB, Jul 10), `app-release.apk` (2.6 MB, Jul 12).
- **Termux tools:** Python 3.14.6, Node 24.17, Deno 2.9, **Rust 1.96.1**, **Clang/LLVM 21.1.8**, CMake 4.4, Ninja 1.13, Make 4.4, ONNX Runtime (Python) 1.27.1, NumPy, SciPy, soundfile, espeak, ffmpeg 8.1.2, adb 35.0.2. **Not installed:** JDK/Java, Gradle, Android SDK, QAI Hub CLI.
- **Repos on device:** `~/repos/Novus-Agenti` (HEAD ec0ac49), `~/repos/openclaude` (OmniRoute proxy).

## Coordination note

Two sessions touch this project: **this repo session** (the committer — pushes
to `claude/on-device-inference-openwiki-sae7cy`) and the **device session** (the
inventory/eyes, running in Termux). The device clone is behind the pushed branch;
to avoid collisions, the device session should stay read-only (audit/inventory),
not a second writer.
