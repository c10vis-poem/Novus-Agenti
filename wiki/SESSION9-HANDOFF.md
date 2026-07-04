# SESSION 9 HANDOFF — 2026-07-04

## TL;DR
Job 8's `exit code 14` root cause FOUND and FIXED (`--quantize_io`). A fixed
run is IN FLIGHT: HF job `6a485607d235f6e43ae5a50a`. First action of next
session: check its status.

## CRITICAL — tokens
The tokens in CLAUDE.md §Tokens are ROTATED/STALE. Do NOT export them — they
will 401 and shadow the good ones. The remote-session container already has
valid `HF_TOKEN` and `QAI_HUB_API_TOKEN` in the environment. Use them as-is.

## Root cause of exit code 14 (vision encoder compile)
QNN log (QAI Hub jobs `jp0v4vm2g`, `jpeloqlog`):
```
Unsupported input/output datatypes for HTP Op 'Reshape'
node /m/patch_embed/Reshape: in FLOAT_32 → out UFIXED_POINT_16
QnnBackend_validateOpConfig failed 3110 (0xc26)
```
w4a16 quantizes activations to u16 but graph I/O stayed FP32; HTP Reshape
can't convert FP32→u16 inline. Fix: `--quantize_io` in COMPILE_OPTIONS_BASE
(scripts/compile_qwen3_5_9b.py:208). This matches Qualcomm's own recipe:
qualcomm/ai-hub-models `src/qai_hub_models/models/_shared/llm/model.py`
always pairs 16-bit activations with `--quantize_io`.

Runtime consequence: context-binary float I/O is now 16-bit fixed point —
ort_engine must quantize/dequantize at the app boundary (standard QNN EP
behavior). Note this when scaffolding ort_engine.

## In-flight job
- HF job: `6a485607d235f6e43ae5a50a`
  (https://huggingface.co/jobs/Mer0vin8ian/6a485607d235f6e43ae5a50a)
- Script: branch `claude/proxy-hf-auth-status-3pcwbu`, commit `8cde102`
  = canonical script synced from `claude/project-scope-review-lf615p`
  + `--quantize_io`.
- Check: `hf jobs ps -a | head -3` then
  `hf jobs logs 6a485607d235f6e43ae5a50a | tail -40`
- Success = all 6 QAI Hub compiles pass (vision, projection, 4 decoder
  chunks) and artifacts publish to `Mer0vin8ian/qwen3-5-9b-npu-sm8750`.

## Gotchas learned this session (do not re-learn)
1. **raw.githubusercontent CDN caches ~5 min.** Always add a fresh
   cache-buster (`?v=<something-new>`) to the script URL after pushing,
   or the HF job runs the stale script.
2. Branch `claude/job-8-launch-emayt6` has a broken script (legacy ONNX
   exporter + GQA SDPA bug: "enable_gqa is True"). Ignore that branch.
3. This container's proxy blocks `app.aihub.qualcomm.com` and Qualcomm doc
   domains. To read QAI Hub compile logs, run `scripts/fetch_qai_logs.py`
   via HF Jobs (edit its job-id list first):
   `hf jobs uv run --flavor cpu-basic --timeout 10m --secrets QAI_HUB_API_TOKEN <raw-url-with-cache-buster>`
4. `hf jobs uv run` with a LOCAL file path hangs in this container — always
   run scripts from raw GitHub URLs.
5. wikitext calibration dataset load fails in-job ("Invalid HF URI"); script
   falls back to synthetic tokens. Non-fatal, but real calibration data is a
   TODO (datasets>=4 needs `Salesforce/wikitext` namespace or similar).

## Open items
- PR #10 (draft, https://github.com/c10vis-poem/Novus-Agenti/pull/10):
  the fix + fetch_qai_logs.py. CI `stage-colab` passed; `build` was running.
  Session was subscribed to PR activity — resubscribe if babysitting.
- If vision compile fails AGAIN: get the new QAI Hub job id from the HF job
  log ("job_id=..."), add it to fetch_qai_logs.py, push (cache-buster!),
  run it, read the QNN error.
- After Job 8 succeeds: merge the --quantize_io fix back toward the
  canonical branch / update CLAUDE.md SOTU + Job Execution Log.
- Then the usual pending list: ort_engine scaffold, NpuManager lock,
  GameManager, manifest entries, build-apk.yml repoint, watchdog/ fold.
