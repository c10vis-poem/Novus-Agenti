# SESSION 18 HANDOFF — Horizons APK, NPU offload wired end-to-end

**Track:** Horizons Android app (GGUF / llama-server / ggml-hexagon).
**Branch:** `claude/horizons-ui-apk-0i0adk` — **PR #12 (draft, open).**
**Compile track (Job 8 / QAI Hub / ONNX):** untouched, out of scope, per operator.

---

## Where we are RIGHT NOW (the pickup point)

- CI `build-apk.yml` ran **green** on `4c410f4` (run #119).
- Operator **downloaded `horizons.apk`** from the `latest-debug` release.
- Next session starts at **on-device install + first real NPU decode test.**
  Nothing is blocking that — the APK in the operator's hands is the one to test.

## What this session changed (two fixes, one commit `4c410f4`)

Both fixes target the same symptom: the APK installed and the daemon reported
"started," but Hexagon NPU offload was **silently dead** — decode fell back to
CPU with no error surfaced.

1. **`.github/workflows/build-apk.yml`** — the DSP-side skels
   (`libggml-htp-v79.so`, `libggml-htp-v75.so`) were published to the
   `latest-debug` release by `build-llama-server.yml` but **never downloaded
   into `jniLibs/arm64-v8a/`** by `build-apk.yml`. So the APK shipped without
   them and `ggml-hexagon` had no skel to FastRPC-load. Added the skel
   download to the "Package llama.cpp runtime" step, next to the other
   llama libs.

2. **`horizons/src/main/java/com/horizons/core/shell/DaemonLauncher.kt`** —
   `DSP_LIBRARY_PATH` was set to `filesDir` only, but APK-packaged skels
   extract to **`nativeLibraryDir`**. Changed to
   `nativeLibraryDir:filesDir` so `ggml-hexagon` finds the skel whether it
   came from the APK (normal install) or a manual import (legacy).

PR #12 description was rewritten to cover the full change set (crash-loop fix,
self-contained daemon, DSP skels + DSP_LIBRARY_PATH, sherpa AAR vendored).

## Branch reality (resolves the old #12-vs-#13 confusion)

`claude/horizons-ui-apk-0i0adk` (PR #12) already had **PR #13's commits
fast-forward merged into it** at the start of this session — the two tracks
converged. This branch is now the single source of truth for the app track.
The old resume-prompt claim that PR #12 was "SUPERSEDED by #13" is stale and
inverted; #12 is the live PR. (Resume prompt in CLAUDE.md updated to match.)

## Known-good facts, do NOT re-derive

- `build-llama-server.yml` run #8 (`6d8cc0f`) is green and publishes all four
  skels (`libggml-htp-v73/v75/v79/v81.so`) to `latest-debug`. The v79 + v75
  ones are what `build-apk.yml` now pulls (SM8750 / 8 Elite is Hexagon v79;
  v75 kept for older SoCs).
- Daemon is fully self-contained in the APK: `libort_engine.so`,
  `libllama_server.so`, the ggml/llama `.so` set, and now the two skels all
  ride in `jniLibs` and extract to `nativeLibraryDir` at install
  (`useLegacyPackaging=true`). No manual `adb push` of any runtime file.
- Startup crash-loop (`:clifford` racing the Keystore) was fixed in an
  earlier session and is on this branch — lazy `appState`, main-process-only,
  in-memory fallback.
- ORT native-lib dedup: `pickFirst("**/libonnxruntime.so")` in
  `build.gradle.kts` handles the sherpa-AAR-vs-Maven-1.20.0 overlap. Left as
  is this session — not the crash cause, and forcing it further risked
  changing which .so wins. If TTS or VAD misbehaves on-device, this is the
  first thing to re-examine (which `libonnxruntime.so` version actually
  landed in the APK).

## On-device NEXT STEPS (session 19 opener)

1. Install the downloaded `horizons.apk` (self-contained — no pushes).
2. Grant All Files access. GGUF (Qwen3.5-9B Q4_0, ~5.7GB) is already in the
   phone's `Download/`.
3. Open chat → watch the CLIFFORD foreground-service notification + first
   token. `GGML_HEXAGON_NDEV=2` splits the model across 2 HTP sessions
   (~2.85GB each, under the 4GB/session 32-bit cDSP ceiling).
4. **Confirm NPU, not CPU:** pull the llama-server log
   (`getExternalFilesDir/llama-server.log`) and check for Hexagon backend
   init / device lines, not a CPU-only fallback. Watch tokens/sec.
5. If decode is CPU-only despite the skels shipping: verify the skel actually
   extracted (`ls nativeLibraryDir` on device) and that
   `DSP_LIBRARY_PATH`/`ADSP_LIBRARY_PATH` env reaches the child process.

## Acceptance bar (unchanged)

"Done" = on-device chat produces tokens **with Hexagon NPU offload confirmed
from the log**, not just "the app runs." CPU fallback that happens to work is
NOT the goal for the Qwen3.5-9B path.
