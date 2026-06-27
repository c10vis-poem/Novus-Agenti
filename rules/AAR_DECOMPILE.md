# QNN artifact inspection — hard rule

`qnn_context_binary` artifacts do not ship API docs. The only ground truth
for tensor shapes, quantization parameters, execution context constraints,
and layer fallback decisions is direct inspection of the binary artifact.

**Inspect before assuming.** Use `qnn-context-binary-utility` and
`qnn-net-run` first, not last.

## When this applies

- Any at-bat debugging a QAI Hub compile failure or unexpected inference output.
- Any at-bat wiring `ort_engine` to a new `qnn_context_binary`.
- Any time a sub-agent reports "the compile log says X" without a binary dump.
- Any time W4A16 quantization or layer partitioning is in question.

## Procedure (on aarch64-android with Qualcomm AI Engine Direct SDK)

```bash
# 1. Push the artifact to device
adb push language_decoder.serialized.bin /data/local/tmp/

# 2. Inspect context binary metadata
qnn-context-binary-utility \
  --input_context /data/local/tmp/language_decoder.serialized.bin \
  --output_context_config /data/local/tmp/ctx_config.json
cat /data/local/tmp/ctx_config.json

# 3. Net-run a single forward pass to verify execution
qnn-net-run \
  --backend libQnnHtp.so \
  --input_list /data/local/tmp/input_list.txt \
  --context_config /data/local/tmp/ctx_config.json
```

## What to record

When the artifact differs from any prior documented assumption, capture
the diff in `wiki/FAILURE_LOG.md`:

- Actual input/output tensor names and shapes.
- Actual quantization type (W4A16 confirmed vs. silent fallback to W8A16 or FP16).
- Context memory usage vs. `--scratch_size_mib` limit.
- Any layer that fell back to CPU (HTP unsupported op — these are silent).
- Concurrent context count vs. the single-context limit we declared.

## What NOT to do

- Do not assume the compile succeeded because QAI Hub returned a job ID.
  Jobs can return success with a degraded artifact (layer fallbacks, precision
  downgrades).
- Do not skip this step because "W4A16 is what we compiled for."
  Quantization fallbacks from W4A16 → W8A16 → FP16 are silent unless
  you inspect the binary.
- Do not run `qnn-net-run` on the host — HTP only executes on device.

## Rationale

A previous HF Jobs run produced a `qnn_context_binary` with silent layer
fallbacks from HTP to CPU because `--disable_fusion` interacted unexpectedly
with RoPE gather ops. `qnn-context-binary-utility` would have surfaced the
fallbacks in one command before any time was spent debugging inference output.
The rule pays for itself the first time it catches a silent precision downgrade.
