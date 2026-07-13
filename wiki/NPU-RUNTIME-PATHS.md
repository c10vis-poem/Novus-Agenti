# NPU Runtime Paths — Novus Agenti / Omni Claw

> Target hardware: Motorola Razr Ultra 2025, Snapdragon 8 Elite (SM8750),
> Hexagon HTP v79, 16 GB RAM, Android 15.

## What we're actually running

**Primary path (in use now): Q4_0 GGUF via GenieX.**
`Qwen_Qwen3.5-9B-Q4_0.gguf` served by `geniex serve` (GenieX,
`github.com/qualcomm/GenieX`, OpenAI-compatible HTTP on
`127.0.0.1:18181/v1`), running on GenieX's GGML backend. No QAI Hub compile
needed for this path — GenieX loads the GGUF directly. See
`wiki/GENIEX-DAEMON-PLAN.md` for the daemon contract.

> ⚠️ **The Qwen3.5-9B QAI Hub compile below is a FALLBACK ONLY.** We only
> touch it if the Q4_0 GGUF fails to run acceptably on HTP via GenieX. Do
> not start the compile pipeline pre-emptively — confirm the GGUF path
> failed first.

## Fallback: Qwen3.5-9B → QAI Hub compile → `ort_engine`

If GGUF-on-GenieX doesn't work out:

```
Mer0vin8ian/Qwen3.5-9B → ONNX export (RoPE fold, static shapes) → QAI Hub
  compile (server-side, W4A16) → qnn_context_binary → ort_engine daemon
  (ORT + QNN EP) → Hexagon HTP v79
```

- Compile script: `scripts/compile_qwen3_5_9b.py`. Manifest:
  `models/manifest.yaml`.
- The Hexagon/QNN SDK is a **host-side cross-compile toolchain**, not
  something installed on the phone — QAI Hub does the compile server-side,
  so no local SDK is needed for this step.
- `ort_engine` (the daemon that loads the resulting `.bin`) is already
  built and CI-packaged — see `daemon/src/`.

## Reference: other model formats, if one shows up

We're not building any of these — just what you'd need if a future model
only ships in one of these formats instead of GGUF/QAI-Hub-bundle.

| Format | What's needed |
|---|---|
| **GGUF** (any) | Nothing extra — `geniex serve` loads it directly (GGML backend). This is the format we use now. |
| **QAI Hub bundle** | `geniex serve` loads it too (QAIRT/AI-Engine-Direct backend, NPU-only). If the model isn't in Qualcomm's prebuilt AI Hub library, you BYOM-compile it first via QAI Hub (the fallback pipeline above). |
| **ONNX** | Compile to `qnn_context_binary` via QAI Hub, then load with `ort_engine` (legacy) — or convert/quantize it to GGUF instead and use GenieX directly. |
| **TFLite** | Needs a `tflite_engine` daemon (not built) + Hexagon delegate; requires INT8 quant for full NPU offload, FP16 falls back to CPU. |
| **DLC** (SNPE, legacy Qualcomm format) | Needs `snpe-onnx-to-dlc` (host-only, part of the SNPE SDK) + a `snpe_engine` daemon (not built). |
| **PTE** (ExecuTorch) | Needs an `executorch_engine` daemon (not built) with a QNN delegate; upstream API still moving. |

## Size envelope

| | Size |
|---|---|
| Target | 5.5 GB |
| Redline | 7.0–7.2 GB |
| Q4_0 GGUF (current) | ~5.4 GB |
| W4A16 @ max_seq=2048 (fallback compile) | ~5.4 GB |
