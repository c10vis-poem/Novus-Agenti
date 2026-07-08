# Novus-agenti (Omni Claw) — Active

## What it is
NPU-first agentic AI assistant as a native Android app. Repo: `c10vis-poem/Novus-Agenti`. Package: `com.horizons`. Device: Motorola Razr Ultra 2025 (SM8750, Hexagon HTP v75, 16GB).

Stack: GGUF model (Qwen3.5-9B Q4_0 ~5.7GB) + llama-server daemon + ggml-hexagon hybrid CPU/NPU scheduler. NDEV=2 splits across 2 HTP sessions (~2.85GB each, under 4GB 32-bit cDSP ceiling). Flash attention (-fa), -c 4096. CliffordService FGS watchdog. AgentLoop + 22 tools. NpuClient bridge to localhost:8080.

Active branch: `claude/omni-claw-inference-build-k7f5wz`, PR #14 (draft, open) —
converges the old #12 (session-18 APK fixes) and #13 (docs) lines.

## OPERATOR STANDING DECISIONS — DO NOT RE-ASK THESE
Recorded 2026-07-08 from the operator directly. Any future session asking
these again is wasting the operator's time and will get yelled at:

1. **Workstream order: APK first**, then browser/websocket connector,
   then the custom OpenWiki memory system. Not negotiable, already decided.
2. **Performance bar: 12–13 tok/s minimum** on-device (16 would be ideal,
   6–8 is failure). If the GGUF/llama-server/hexagon path can't clear it,
   THEN AND ONLY THEN the QAI Hub compile track (Job 8) comes back into
   play — "we're going and throwing as much on the NPUs we can."
3. **Job 8 / compile track: conditional-parked, not dead.** Do not fire it,
   do not mark it abandoned. It's the fallback if GGUF NPU offload
   disappoints.
4. **Browser connector: build it** — a frontend connector that runs its own
   browser in the UI (Chromium-engine WebView panel; the blueprint's
   Panel 1 "Cloud & Vision Space"), instead of only hitting cloud shell.
5. **OpenWiki: custom tailor-made fork** combining ALL the memory layers
   the operator collected — OpenWiki (wiki compiler) + OB1/Open Brain
   (thoughts table + remote MCP) + Mem0 (fact extraction/graph memory,
   the dual-agent talker/reasoner pattern) + reasoning-bank
   (success/failure memory items). Downstream commits on
   `c10vis-poem/openwiki`. This is a MAJOR deliverable, not a side task.
6. Operator has ~51 forked repos available as reference; ask them to
   attach specific ones rather than claiming something is unreachable.

## State
- App crash fix DONE: multi-process EncryptedSharedPreferences race resolved, 75s stability verified
- Daemon packaging DONE: llama-server + ort_engine as jniLibs/*.so, exec from nativeLibraryDir
- Qwen3.5 arch confirmed in llama.cpp binary (strings check)
- Hexagon skel fix: CI build-llama-server.yml fixed across 4 iterations. Run #8 (commit 6d8cc0f) dropped cmake --install, stages artifacts by direct cp. Skels: libggml-htp-{v73,v75,v79,v81}.so
- Session 18: skels packaged into APK jniLibs + DSP_LIBRARY_PATH fixed → run #119 green, operator downloaded horizons.apk
- 2026-07-08 (this session): NpuTuning (filesDir/npu-tuning.json — NDEV,
  ctx, -fa, verbose, extra env/args; RouterPane knobs + Apply & Restart
  Daemon) and NpuOffloadProbe (Hexagon-vs-CPU-fallback verdict in the
  CLIFFORD notification + npu-status.json + RouterPane card). PR #14.
- NpuManager perf lock + ADPF GameModeBoost both wired
- wiki/REFERENCE-IMPLEMENTATIONS.md committed (21 sections, all operator materials)
- PR #8 merged to main (crash fixes, UI/perf, security cleanup, doc audit)

### Known gaps
- DSP_LIBRARY_PATH bug: FIXED session 18 (nativeLibraryDir:filesDir)
- NativeBinaryInstaller only knows QNN libs, missing ggml-hexagon skel awareness
- NpuClient emits "[Thinking...]" text instead of structured events
- AgentLoop auto-executes tools (spec says model NEVER auto-executes)
- LlmRuntime missing capability model (supportsVision, supportsThinking, etc.)
- ScreenshotCapture.kt, InteractionLogger.kt, SecureResourceRelay.kt: implemented but unwired
- daemon/src/tokenizer.cpp: missing GPT-2 byte-to-unicode, BPE merge loop unused, JSON unescaping absent
- Job 8 (compile track): ERROR — vision-encoder compile exit code 14, calibration dataset fallback. Conditional-parked (see standing decisions).

## Next step
- On-device test of the PR #14 APK: CLIFFORD notification now states the
  Hexagon verdict directly; RouterPane shows tok/s + offload ratio.
  If tok/s < 12: crank NDEV / ctx / extra flags from the new RouterPane
  tuning section (no rebuild needed per experiment).
- Browser connector (WebView panel + agent tools) — in progress this session
- OpenWiki custom memory system — next after that
- Code repairs still open: thinking tokens, agent verification loop,
  capabilities model, Termux bridge
- Wire: ScreenshotCapture, VoiceInteractionSession, model hot-swap, crash persistence
