# Session 8 Handoff — 2026-06-28

> Load AFTER `/memory`, `CLAUDE.md`, and `wiki/SESSION6-HANDOFF.md`.

---

## Where we are

### Branches and PRs

| PR | State | Branch | What |
|---|---|---|---|
| #3 | merged → main | claude/migrate-horizons-app | Full Android app migration |
| #5 | draft open | claude/session-8-closeout-hf-review-thl2gj | Daemon scaffold, compile fix, content remodel |

### Key commits this session (branch `claude/session-8-closeout-hf-review-thl2gj`)

| Hash | What |
|---|---|
| `f2fc4dc` | UnifiedWrapper rewrite — bypasses untraceable forward(), composes sub-modules directly |
| `c55dcf7` | ort_engine C++ daemon scaffold (11 files in `daemon/`) |
| (pending) | Content remodel — LiteRT/Gemma/genie cleanup across horizons/ |

---

## Compile script status

**UnifiedWrapper fix is working.** Job ran far enough to produce TracerWarnings from vision encoder internals (non-fatal with static shapes) before HF billing ran out ($2 of $2 spent). User set up auto-pay.

### Next run: add `-e MAX_SEQ_LEN=2048`

Reduces memory footprint and cost. The trigger command in CLAUDE.md should be updated to include this flag:

```
hf jobs uv run --flavor cpu-xl --timeout 2h \
  --with torch --with transformers --with onnx --with onnxruntime --with onnxscript \
  --with qai-hub --with datasets --with numpy --with huggingface_hub --with accelerate \
  --secrets HF_TOKEN --secrets QAI_HUB_API_TOKEN \
  -e MODEL_ID=Mer0vin8ian/Qwen3.5-9B -e PUBLISH_HF=1 -e OUTPUT_DIR=/tmp \
  -e MAX_SEQ_LEN=2048 \
  https://raw.githubusercontent.com/c10vis-poem/Novus-Agenti/claude/session-8-closeout-hf-review-thl2gj/scripts/compile_qwen3_5_9b.py
```

### UnifiedWrapper architecture (for debugging if export fails)

```python
class UnifiedWrapper(torch.nn.Module):
    # Composes sub-modules directly — no call to Qwen3_5Model.forward()
    # Inputs: (input_ids, attention_mask, position_ids, pixel_values, image_grid_thw)
    # Flow: visual(pixel_values) → embed_tokens(input_ids) → masked_scatter → language_model → lm_head
```

Key: `masked_scatter` injects vision embeddings at `<|image|>` token positions in the text embedding sequence. This is Qwen3.5's "deepstack vision injection" — NOT a separate encoder pipeline.

---

## ort_engine daemon

Fully scaffolded in `daemon/`. Files:

| File | Purpose |
|---|---|
| `CMakeLists.txt` | NDK cross-compile config, arm64-v8a, links ORT+QNN |
| `build.sh` | One-shot build script (requires ANDROID_NDK + ORT_ROOT) |
| `src/main.cpp` | CLI entry, arg parsing, signal handling, JSON parse |
| `src/engine.cpp/h` | ORT session + QNN EP, static-shape inference, token loop |
| `src/http_server.cpp/h` | Minimal HTTP, SSE streaming, localhost-only |
| `src/tokenizer.cpp/h` | HF tokenizer.json loader (greedy match, placeholder BPE) |
| `src/sampler.cpp/h` | Temperature, top-k, top-p, nucleus sampling |

Wire protocol matches `NpuClient.kt`:
- `GET /health` → 200 "ok" / 503 "not ready"
- `POST /api/v1/generate` → `data: {"token":"...","index":N,"done":false}\n\n`

### To actually build

Need:
1. Android NDK r26+ (`ANDROID_NDK` env var)
2. ONNX Runtime Android AAR extracted (`ORT_ROOT` env var) — `jni/arm64-v8a/` + `headers/`
3. Optional: QNN SDK (`QNN_SDK` env var) for `libQnnHtp.so` headers
4. Run: `cd daemon && ./build.sh`
5. Deploy: `adb push build/ort_engine /data/data/com.horizons/files/ort_engine`

---

## Content remodel (this session)

Removed all old architecture references from horizons/ source:
- Deleted `LiteRtRuntime.kt` (entire file — dead, imports removed LiteRT-LM dep)
- Removed `litertlm-android` dependency from `build.gradle.kts`
- Removed `.litertlm` intent filters from manifest
- Added `uses-feature game` + `HIGH_PERFORMANCE` permission to manifest
- Removed `GENIE_BINARY` from `DaemonLauncher.kt`
- Updated all UI strings (ModelsPane, RouterPane, SettingsPane)
- Updated `HorizonsApplication.kt` — removed LiteRT model resolution, updated comments
- Updated `NativeBinaryInstaller.kt` — ort_engine only
- `agents/`, `rules/`, `skills/`, `watchdog/` were already clean

---

## Qualcomm ecosystem intel (for future sessions)

User forked these repos (browse for patterns if needed):
- `c10vis-poem/ai-hub-models` — export scripts for 100+ models to QNN
- `c10vis-poem/ai-hub-apps` — on-device QNN app examples
- `M0DU14R-SYSx-inc/EdgeAIApp-ExecuTorch` — ExecuTorch reference

Key finding from `qualcomm-linux` org:
- `meta-ai` — OE layer for AI/ML on Qualcomm Linux
- `docker-pkg-build` — arm64 cross-compile packaging
- `fastrpc` — CPU-DSP communication (abstracted by QNN EP)
- `snagboot` — SoC recovery tool (no Snapdragon support)

GenieX/nexa-sdk: dead upstream, wrapper-only, no native NPU code.

---

## Still pending

1. **Job 8** — re-run compile with MAX_SEQ_LEN=2048
2. **NpuManager.acquirePerformanceLock** — wire into CliffordService.kt
3. **GameManager.setGameMode(PERFORMANCE)** — wire into HorizonsApplication.onCreate()
4. **build-apk.yml** — repoint release target to `${{ github.repository }}`
5. **watchdog/** — fold into CliffordService or delete
6. **Dead weight** — delete `scripts/compile_qwen3_vl.py`, `wiki/EDGE-MODEL-LISTS.md`
7. **Daemon cross-compile** — needs NDK + ORT AAR

---

## What I would do first when resuming

1. Read CLAUDE.md → this handoff → confirm SOTU
2. Re-run compile Job 8 with `-e MAX_SEQ_LEN=2048`
3. While job runs (~30-40 min): wire NpuManager lock + GameManager boost
4. If compile succeeds: download .bin, build ort_engine, deploy to device
5. If compile fails: diagnose from logs, fix, re-run
