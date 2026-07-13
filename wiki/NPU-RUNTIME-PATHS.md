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

## Size envelope

| | Size |
|---|---|
| Target | 5.5 GB |
| Redline | 7.0–7.2 GB |
| Q4_0 GGUF (current) | ~5.4 GB |
| W4A16 @ max_seq=2048 (fallback compile) | ~5.4 GB |
