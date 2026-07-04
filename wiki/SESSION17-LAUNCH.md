# SESSION 17 LAUNCH PROMPT — APP TRACK ONLY. READ BEFORE ANY ACTION.

> Operator will return ~3.5h after 2026-07-04 ~11:00Z. Until then: keep
> working this list, don't ask permission, verify empirically, push to
> **branch `claude/novus-agenti-setup-rr8jvz` (PR #13) only**.

## HARD RULES (operator-stated, violated at your peril)
1. **DO NOT touch the compile track.** Job 8 / QAI Hub / Workbench /
   Qwen3.5 compile is a SEPARATE task owned elsewhere. Do not retrigger,
   "fix", or analyze it further. Reading status is already done — leave it.
2. **The app is an orchestrator UI.** It ships EMPTY and boots EMPTY. It
   must never depend on any specific model to run. Models + runtimes are
   drop-in packages (ModelImportActivity → RUNTIME_FILES). Three runtime
   families: GGUF (llama-server), LiteRT (+QNN delegate), ONNX (ort_engine).
   Plus cloud front-end (OpenRouter/OpenAI/Google) and Termux interface.
3. Don't ask permission for reversible work. Just do it and verify.
4. A doc/code mismatch is not automatically stale docs — verify in source.

## STATE (verified this session, 2026-07-04)
- Branch has PR #12's branch **merged in** (keystore fix, APK-packaged
  ort_engine, vendored sherpa AAR). PR #13 CI green, draft, subscribed.
- **Emulator verification done**: AVD `horizons` (x86_64 ATD, NO KVM —
  ~10x slow), `emulator-5554`. Local builds work:
  `gradle -p /home/user/Novus-Agenti :horizons:assembleDebug -PemuAbi=x86_64`
  (emuAbi flag = uncommitted edit in horizons/build.gradle.kts — commit it).
  SDK at /opt/android-sdk; sherpa AAR vendored in repo.
- **UI boots and stays up.** Mic permission dialog on first launch.
- **`:clifford` watchdog ANR-killed on BOTH main and PR#12 builds in the
  emulator**: `startForegroundService()` → 10s deadline missed →
  "Killing …:clifford (bg anr)". No Java crash (keystore fix works).
  On-device impact unconfirmed (emulator is 10x slow) but hardening is
  correct regardless: startForeground() must be the FIRST statement in
  CliffordService.onCreate(), before any Breadcrumb I/O.
- **Cross-process bug found (unfixed)**: CliffordService (`:clifford`
  process) calls `app.activateNpuRuntime()` directly — that activates
  NpuClient in the WRONG process's Application instance. The UI process
  never hears about it. PR #5's fix concept: `sendBroadcast(NPU_READY)`
  from clifford + receiver in main-process Application.onCreate.

## IN-FLIGHT WORK (continue exactly this, files already read)
On this branch, implement the GGUF/llama-server runtime family:
1. `CliffordService.kt`: startForeground-first in onCreate(); replace
   direct `app.activateNpuRuntime()` with NPU_READY broadcast (keep both).
2. `HorizonsApplication.kt` (main process only): register NPU_READY
   receiver → `activateNpuRuntime()`. `resolveNpuModelPath()` must accept
   `.gguf` and report the family.
3. `NpuClient.kt`: dual protocol — keep ort_engine `/api/v1/generate`,
   add OpenAI `/v1/chat/completions` SSE (llama-server) selected by
   runtime family; llama-server health = GET `/health`.
4. `DaemonLauncher.kt`: llama family launch — binary `llama-server`
   (release assets also ship `libllama-server-impl.so` + thin exe), args
   `-m <model.gguf> --host 127.0.0.1 --port 8080 -c 4096 -ngl 999`,
   env `GGML_HEXAGON_NDEV=2`, `LD_LIBRARY_PATH=nativeLibraryDir:filesDir`,
   `DSP_LIBRARY_PATH=<filesDir>`. Keep ort_engine path intact.
5. `NativeBinaryInstaller.kt` + `ModelImportActivity.RUNTIME_FILES`: add
   llama-server + libllama*/libggml*/libmtmd .so set;
   **fix stale `libQnnHtpV75Skel.so` → SM8750 is V79** (add V79, keep V75).
6. Family dispatch in CliffordService.ensureDaemonRunning(): model ends
   `.gguf` → llama family, else ort_engine.
7. Build (`-PemuAbi=x86_64`), install on emulator-5554, verify: both
   processes stay up, empty-state notification shows "waiting for
   binary/model" correctly, UI navigable. Screenshot. Commit + push.
   (NPU/HTP can NOT be verified in the emulator — arm64+Hexagon only.)

## AFTER THAT (operator's stated priorities, in order)
1. GGUF + ggml-hexagon Q4_0/Q8_0 path working end-to-end (on-device step
   needs operator's phone — prepare exact Termux/adb commands for them).
   Note: `Mer0vin8ian/Qwen3.5-9B-VLM-Q4_K_M-GGUF` = K-quant = CPU-only
   quant; NPU wants Q4_0/IQ4_NL. qwen3_5 arch is NEW — the June-30
   llama-server release binaries may predate its llama.cpp support; if so,
   rebuild upstream llama.cpp via CI (NDK-only for CPU; Hexagon SDK
   toolchain image for the hexagon backend — see PR #5's
   build-llama-server.yml concept).
2. LiteRT runtime package (QNN delegate = drop-in NPU w/ CPU fallback, NO
   QAI Hub/Genie needed; AOT precompile is an optimization only).
   Operator's Gemma-4 E2B/E4B are LiteRT-native.
3. Termux interface layer (orphaned `SecureResourceRelay.kt` is likely the
   unfinished half — operator hasn't answered wire-vs-delete yet).
4. Cloud front-end verification end-to-end (OpenRouter/OpenAI/Google keys
   exist in RouterPane; confirm chat actually routes).
5. Then: Gemma-4 NPU path + tuning toward ≥16 tok/s.

## TRAPS
- Emulator ABI: APK is arm64-only by default; without `-PemuAbi=x86_64`
  install fails NO_MATCHING_ABIS. Never ship the emuAbi variant.
- The operator pasted an external chat about LiteRT/QNN flags: its exact
  commands/flags are fabricated — concepts ok, don't copy commands into
  docs or code.
- `latest-debug` release = binary exchange point: llama.cpp+Hexagon set,
  horizons.apk, ort_engine, sherpa AAR mirror (mirror-sherpa-aar.yml).
- GitHub App token lacks `actions:write` — trigger workflows by push, not
  dispatch. HF egress OPEN this container; app.aihub.qualcomm.com BLOCKED.
- Emulator processes on this container: emulator-5554 still running.

## SESSION-END STATE (context exhausted mid-verification)
- Code for steps 1–6 of the in-flight list is DONE, compiles green,
  committed+pushed as `4f8dbe7` on this branch (PR #13).
- Step 7 (emulator re-verification of the hardened build) was IN FLIGHT:
  the container's emulator died and was restarted (`-read-only` flag added;
  AVD `horizons`). Re-run yourself:
  1. `pgrep -f emulator` — if dead:
     `/opt/android-sdk/emulator/emulator -avd horizons -no-window -no-accel -gpu swiftshader_indirect -no-audio -no-boot-anim -no-snapshot -memory 3072 -cores 4 &`
  2. wait for `adb shell getprop sys.boot_completed` = 1 (5–15 min, no KVM)
  3. `adb install -r horizons/build/outputs/apk/debug/horizons-debug.apk`
     (rebuild with `-PemuAbi=x86_64` if absent)
  4. launch com.horizons, wait 75s, then check
     `adb shell "ps -A | grep horizons"` shows BOTH processes and
     `adb logcat -d | grep -cE "Killing.*clifford|ANR"` is 0. Screenshot.
- PASS = :clifford survives (old builds died at ~12s). FAIL = pull logcat,
  diagnose, iterate — foreground-first is in CliffordService.onCreate now.
- Operator: "Next session we will just finish what you're doing."
