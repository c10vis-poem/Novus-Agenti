# Session 12 Handoff

**Branch:** `claude/session-8-closeout-hf-review-thl2gj`
**PR:** #5 (draft, open — title/body updated to reflect pivot)
**Date:** 2026-06-30

## Architecture pivot — QAI Hub → llama.cpp

After 21 failed QAI Hub compile jobs, the project pivoted to **llama.cpp with official Snapdragon Hexagon NPU backend**. This is the defining change of sessions 12+.

| Before | After |
|---|---|
| ONNX → QNN context binary via QAI Hub | **GGUF** model format |
| `ort_engine` (ORT + QNN EP) daemon | **llama-server** (llama.cpp + Hexagon backend) |
| `/api/v1/generate` endpoint | **`/v1/chat/completions`** (OpenAI-compatible) |
| QNN context binary compile | **GitHub Actions** cross-compile |

## What was done

### 1. CI workflow — `build-llama-server.yml`
- Cross-compiles `llama-server`, `llama-quantize`, `llama-cli` for arm64-v8a
- Uses `ghcr.io/snapdragon-toolchain/arm64-android:v0.7` Docker image
- Publishes binaries + shared libs to `latest-debug` GitHub Release
- Run 1 failed only because `file` command missing in Docker image — fixed in `634241d`
- Run 2 triggered by push of fix, should succeed

### 2. App crash fixes (commit `c8e31b4`)
- `NpuClient.kt` — endpoint updated to `/v1/chat/completions`, OpenAI-compatible request/response
- `DaemonLauncher.kt` — binary changed from `ort_engine` to `llama-server`, added `GGML_HEXAGON_NDEV=2`
- `CliffordService.kt` — cross-process IPC via `sendBroadcast(Intent("com.horizons.NPU_READY"))`
- `ChatHistoryStore.kt` — `.first{}` → `.firstOrNull{}` crash fix
- Removed `shared`/`watchdog` module references from build configs
- Deleted dead code: `ModelsPane.kt`, `LibraryPane.kt`

### 3. Process guard (commit `a50c601`)
- `HorizonsApplication.kt` — `getProcessName()` guard at top of `onCreate()` prevents `:clifford` process from re-initializing the app (was causing ANR infinite loop)
- `NPU_READY` BroadcastReceiver registered for cross-process daemon activation

### 4. CI fix (commit `634241d`)
- Replaced `file` command with `ls -la` in verify step
- Added `bin/*.so` copy alongside `lib/*.so` for shared libraries
- Simplified release files glob

## Commits this session

```
634241d fix: build-llama-server verify step — replace missing `file` cmd with ls
a50c601 fix: add process guard and NPU_READY receiver to HorizonsApplication
c8e31b4 fix: multi-process crash, llama-server endpoint, build cleanup
8b5e6b3 feat: add GitHub Actions workflow for cross-compiling llama-server
```

## What's next (priority order)

1. **Verify CI run 2 passes** — should auto-trigger from `634241d` push
2. **GGUF HTP conversion** — verify `convert_hf_to_gguf_htp.py` supports Qwen3.5 arch (DeltaNet layers)
3. **Model picker UI** — user wants UI that can select between backends and model sizes
4. **NpuManager lock** — wire `acquirePerformanceLock(PERF_MODE_HIGH)` into CliffordService
5. **GameManager** — wire into HorizonsApplication
6. **Update CLAUDE.md SOTU** — reflect the architecture pivot

## Key technical notes

- **Q4_K_M does NOT work on HTP** — k-quants misalign with HMX 32x32 tiles. Need IQ4_NL or Q4_0 for NPU.
- **DeltaNet layers fall back to CPU** — hybrid CPU+NPU is expected architecture per arxiv paper 2509.23324v1
- **Multi-HTP sessions** — `GGML_HEXAGON_NDEV=2` splits model across 2x 2048MB sessions for 9B models
- User already downloading `Qwen3.5-9B-Q4_K_M.gguf` on device for CPU testing while NPU build is in progress

## Resume block

```
Project: Novus Agenti (Omni Claw). PIVOTED from QAI Hub to llama.cpp.
Runtime: llama-server + Snapdragon Hexagon NPU backend.
Canonical repo: c10vis-poem/Novus-Agenti, branch claude/session-8-closeout-hf-review-thl2gj.

READ THESE IN ORDER BEFORE ANY ACTION:
  1. CLAUDE.md (full read, all sections)
  2. wiki/GPT-OSS-Reference.md (full read)
  3. wiki/SESSION12-HANDOFF.md (THIS FILE)
  4. models/manifest.yaml

Architecture pivot complete. CI workflow building llama-server binaries.
App crash fixes committed and pushed. Next: verify CI, GGUF HTP conversion, model picker UI.
```
