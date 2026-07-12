# GenieX Daemon Plan — Qwen3.5-9B runtime on HTP SDK

> Runtime decision (session 15, LOCKED): **backend = HTP SDK (QAIRT),
> runtime = `GenieX`, wired to a SEPARATE detached daemon.** This doc scopes
> how GenieX replaces `ort_engine` behind the existing socket without
> touching the watchdog or the crash-loop fix. See CLAUDE.md §Single-Path
> Architecture and the QAIT>ORT decision block.

## What stays exactly the same

The whole point of the daemon/socket design is that **only the thing behind
the socket changes.** These do NOT change:

- **The wire contract** — `NpuClient.kt` still `POST`s to
  `http://127.0.0.1:8080/api/v1/generate` and polls `/health`. Whatever runs
  behind :8080 must speak that contract (or a thin shim maps GenieX's HTTP/CLI
  to it).
- **`CliffordService` (CLIFFORD == Watchdog)** — FGS, `START_STICKY`,
  `specialUse`, exponential-backoff + 5-strike relaunch, `oom_score_adj`
  anchoring. Untouched.
- **The crash-loop fix** — serve-first: bind :8080 immediately, load the model
  on a background thread, serve `/health` 503 until ready, never suicide on a
  bad/missing model. GenieX's launcher must preserve this "alive ≠ ready"
  property so the watchdog never thrashes.
- **`DaemonLauncher.kt`** — `sh -T-` detach, reparent to init, pushed to
  `/data/local/tmp`. Same launch mechanism, new binary.
- The hard rule: **no in-process tensor runtime.** GenieX runs as its own
  detached process, not a LiteRT `CompiledModel` inside the UI process. (That
  in-process option was explicitly rejected.)

## What changes

- **`ort_engine` → legacy.** It stays in the repo as one of several uploadable
  runtime binaries (`RUNTIME_FILES`), but it is no longer the Qwen3.5-9B path.
- **New runtime binary: `geniex_server`** (working name) — a GenieX-based
  daemon that loads the QAIRT/HTP artifact and serves the :8080 contract.
- **Model artifact format** shifts from ORT's `qnn_context_binary` loaded via
  ONNX Runtime + QNN EP to whatever GenieX consumes on the HTP backend
  (QAIRT context binary / GenieX bundle — confirm against the GenieX repo +
  Drive `#QAIRT/` manual once the fork lands).

## GenieX facts (verified this session)

- Repo: **`github.com/qualcomm/GenieX`** — official Qualcomm, ~8k★, **Rust**.
- Scope: "run frontier LLMs and VLMs locally on Qualcomm devices across
  **NPU, GPU, and CPU**." Topics include `hexagon`, `qwen3`, `qwen3vl`,
  `snapdragon`, `on-device-ai`, `sdk`.
- It is **NOT** QNN's `genie-t2t-run` ("Genie"). The runtime is **GenieX**.
- Exposes C / Python / Kotlin APIs and a CLI — the CLI/HTTP surface is the
  candidate seam for the daemon.

## Open questions to resolve against the GenieX repo + `#QAIRT/` manual

1. **Does GenieX expose an HTTP server**, or only a CLI / library? If CLI/lib,
   the daemon is a thin Rust (or C-ABI) wrapper that owns the GenieX session
   and serves `/api/v1/generate` + `/health` itself.
2. **Model conversion path** — how does `Mer0vin8ian/Qwen3.5-9B` get to a
   GenieX-loadable HTP artifact? Reconcile with the existing
   `scripts/compile_qwen3_5_9b.py` QAI Hub pipeline (does GenieX consume the
   same `qnn_context_binary`, or its own bundle?).
3. **Quantization** — hold the W4A16 / mixed INT4-weight + FP16-activation line
   (per FraQAT + the NPU test-time-scaling paper in `knowledge/`); confirm the
   flag names GenieX/QAIRT use.
4. **SoC / HTP arch** — CLAUDE.md's mission says "Hexagon HTP v75 (SM8750)",
   but `knowledge/` sources (FraQAT, the NPU scaling paper) put SM8750 =
   Snapdragon 8 Elite = **Hexagon v79**. Confirm the real `--soc_model` target
   before compiling. (Flagged, not silently changed.)
5. **Cross-compile in CI** — `build-apk.yml` currently builds `ort_engine` via
   CMake/NDK. GenieX is Rust → the daemon step needs a Rust aarch64-android
   cross-compile (or ship a prebuilt GenieX binary + a small C-ABI shim).

## Next steps (in order)

1. Fork `qualcomm/GenieX` → `c10vis-poem/GenieX` (blocked from the agent
   session's GitHub scope — operator forks it, then `add_repo` pulls it in).
2. Read the GenieX README + examples to answer Q1/Q2 above.
3. Ingest Drive `#QAIRT/` (Context/Backend/Api/Graph/Tensor/HTP/Overview) into
   `knowledge/qairt-sdk/` as the HTP integration reference.
4. Prototype `geniex_server` behind the :8080 contract; keep `ort_engine`
   untouched as the fallback runtime until GenieX is verified on-device.
