# Novus-agenti (Omni Claw) — Active

## What it is
NPU-first agentic AI assistant as a native Android app. Repo: `c10vis-poem/Novus-Agenti`. Package: `com.horizons`. Device: Motorola Razr Ultra 2025 (SM8750, Hexagon HTP v75, 16GB).

Stack: GGUF model (Qwen3.5-9B Q4_0 ~5.7GB) + llama-server daemon + ggml-hexagon hybrid CPU/NPU scheduler. NDEV=2 splits across 2 HTP sessions (~2.85GB each, under 4GB 32-bit cDSP ceiling). Flash attention (-fa), -c 4096. CliffordService FGS watchdog. AgentLoop + 22 tools. NpuClient bridge to localhost:8080.

Active branch: `claude/novus-agenti-setup-rr8jvz`, PR #13 (draft, open).

## State
- App crash fix DONE: multi-process EncryptedSharedPreferences race resolved, 75s stability verified
- Daemon packaging DONE: llama-server + ort_engine as jniLibs/*.so, exec from nativeLibraryDir
- Qwen3.5 arch confirmed in llama.cpp binary (strings check)
- Hexagon skel fix: CI build-llama-server.yml fixed across 4 iterations. Run #8 (commit 6d8cc0f) dropped cmake --install, stages artifacts by direct cp. Skels: libggml-htp-{v73,v75,v79,v81}.so
- NpuManager perf lock + ADPF GameModeBoost both wired
- wiki/REFERENCE-IMPLEMENTATIONS.md committed (21 sections, all operator materials)
- PR #8 merged to main (crash fixes, UI/perf, security cleanup, doc audit)

### Known gaps
- DSP_LIBRARY_PATH bug: DaemonLauncher.kt:73 points to filesDir, skels in nativeLibraryDir
- NativeBinaryInstaller only knows QNN libs, missing ggml-hexagon skel awareness
- NpuClient emits "[Thinking...]" text instead of structured events
- AgentLoop auto-executes tools (spec says model NEVER auto-executes)
- LlmRuntime missing capability model (supportsVision, supportsThinking, etc.)
- ScreenshotCapture.kt, InteractionLogger.kt, SecureResourceRelay.kt: implemented but unwired
- daemon/src/tokenizer.cpp: missing GPT-2 byte-to-unicode, BPE merge loop unused, JSON unescaping absent
- Job 8 (compile track): ERROR — vision-encoder compile exit code 14, calibration dataset fallback. Out of scope for app track.

## Next step
- Push memory-as-skill to repo (this session)
- Label canonical docs in CLAUDE.md reading sequence
- Restructure HORIZONS-BLUEPRINTS.md in AESOP 11-section format
- Code repairs: DSP_LIBRARY_PATH, thinking tokens, agent verification loop, capabilities model, Termux bridge
- Wire: ScreenshotCapture, VoiceInteractionSession, model hot-swap, crash persistence
