# Session 5 Handoff — 2026-06-25

> Load this file AFTER `/memory` and `CLAUDE.md`. It overrides stale SOTU entries.
> Branch: `claude/epic-wozniak-7b0ban`
> Active repo: `c10vis-poem/novus-agenti` — NeuroOmni.Vag-Agenti is reference only.

---

## P0 Right Now

**Write and push `scripts/compile_qwen3_5_9b.py`** — the full custom PyTorch export + QAI Hub
compile pipeline for `Qwen/Qwen3.5-9B`. This is a novel, first-ever pipeline. No
`qai_hub_models` scaffold class exists for the `qwen3_5` architecture. Build from scratch.

This is what the user paid credits to unblock at the end of session 4. Pick up here immediately.

---

## Repo Clarification

- **`c10vis-poem/novus-agenti`** — THE active repo. All work goes here.
- **`M0DU14R-SYSx-inc/NeuroOmni.Vag-Agenti`** — reference artifact only. Do not build on it.

---

## Confirmed Compile Order (user-specified, non-negotiable)

| Priority | Model | Script | Status |
|---|---|---|---|
| **1 — PRIMARY** | `Qwen/Qwen3.5-9B` | `scripts/compile_qwen3_5_9b.py` | **NOT YET WRITTEN** |
| 2 — backup | `Qwen/Qwen3-VL-8B-Thinking` | `scripts/compile_qwen3_vl.py` | Exists, partially correct |
| 3 — backup | `google/gemma-4-12B-it-qat-q4_0-unquantized` | `scripts/compile_gemma_qairt.py` | Exists, correct |

Backups are only used if primary compile hard-fails. E2B is dead — never mentioned again.

---

## Qwen3.5-9B Model Facts (confirmed via HF MCP search)

- **HF repo:** `Qwen/Qwen3.5-9B`
- **Architecture:** `qwen3_5` (brand new, Feb 27 2026)
- **Type:** image-text-to-text (unified multimodal — vision + reasoning)
- **Downloads:** 9.8M
- **License:** Apache 2.0
- **No existing qai_hub_models scaffold** — must build custom PyTorch export
- **No QAT variant** — must do PTQ W4A16 with ≥10K calibration tokens
- **Size:** ~9B params, ~18 GB in FP16, ~4.5 GB in W4A16

---

## What `compile_qwen3_5_9b.py` Must Do (full spec)

### Environment variables
```
HF_TOKEN            — required
QAI_HUB_API_TOKEN   — required
MODEL_ID            — default: Qwen/Qwen3.5-9B
MAX_SEQ_LEN         — default: 4096
OUTPUT_DIR          — default: /content
HF_OUTPUT_REPO      — default: Mer0vin8ian/qwen3-5-9b-npu-sm8750
SKIP_EXPORT         — "1" to re-download a previous job
JOB_ID              — job ID to re-download (requires SKIP_EXPORT=1)
CALIB_TOKENS        — default: 10000
PUBLISH_HF          — "1" to upload .bin to HF after download
```

### Step 0 — Install deps
```
torch>=2.3.0
transformers>=4.51.0   # Qwen3.5 support
onnx>=1.16.0
onnxruntime
qai-hub>=0.28.0
huggingface_hub
datasets
tokenizers
```

### Step 1 — Load model
```python
config    = AutoConfig.from_pretrained(MODEL_ID, token=HF_TOKEN)
tokenizer = AutoTokenizer.from_pretrained(MODEL_ID, token=HF_TOKEN)
model     = AutoModelForCausalLM.from_pretrained(
    MODEL_ID, torch_dtype=torch.float16,
    device_map="cpu", trust_remote_code=False,
    token=HF_TOKEN, low_cpu_mem_usage=True,
)
model.eval()
```

Print: arch, num_hidden_layers, num_attention_heads, num_key_value_heads, hidden_size, rope_theta.

### Step 2 — RoPE fold (CRITICAL — Hexagon v75 hard requirement)

Hexagon v75 has **no FP16 Sin/Cos kernel**. Runtime Sin/Cos will make the job fail.
Replace with precomputed lookup tables registered as FP16 buffers.

```python
def build_rope_cache(config, max_seq_len, dtype=torch.float16):
    head_dim  = config.hidden_size // config.num_attention_heads
    rope_dim  = getattr(config, "rope_head_dim", head_dim)
    theta     = getattr(config, "rope_theta", 10000.0)
    # Handle yarn / NTK dynamic scaling if config.rope_scaling is set
    inv_freq  = 1.0 / (theta ** (torch.arange(0, rope_dim, 2).float() / rope_dim))
    positions = torch.arange(max_seq_len, dtype=torch.long)
    freqs     = torch.outer(positions.float(), inv_freq)   # [max_seq, rope_dim/2]
    emb       = torch.cat([freqs, freqs], dim=-1)          # [max_seq, rope_dim]
    return emb.cos().to(dtype), emb.sin().to(dtype)

cos_table, sin_table = build_rope_cache(config, MAX_SEQ_LEN)

for layer in model.model.layers:
    attn = layer.self_attn
    if hasattr(attn, "rotary_emb"):
        attn.rotary_emb.register_buffer("cos_cached_folded", cos_table, persistent=False)
        attn.rotary_emb.register_buffer("sin_cached_folded", sin_table, persistent=False)
        # Monkey-patch forward to use Gather (table lookup) instead of computing sin/cos
        def make_folded_fwd(cos_buf, sin_buf):
            def fwd(x, position_ids):
                return cos_buf[position_ids], sin_buf[position_ids]
            return fwd
        attn.rotary_emb.forward = make_folded_fwd(
            attn.rotary_emb.cos_cached_folded,
            attn.rotary_emb.sin_cached_folded,
        )
```

After ONNX export, assert: `len([n for n in graph.node if n.op_type in ("Sin","Cos")]) == 0`
If Sin/Cos survive, the patch missed a rotary class variant — check
`type(model.model.layers[0].self_attn.rotary_emb).__name__` and patch that class directly.

### Step 3 — Static-shape wrapper

Hexagon v75 requires static batch + seq. Wrap the model:

```
Inputs (all static except within-compile seq — use dynamic_axes then freeze):
  input_ids:      [1, seq]           int32
  attention_mask: [1, seq]           int32
  position_ids:   [1, seq]           int32
  past_key_{i}:   [1, kv_heads, past, head_dim]  float16  (× num_layers)
  past_val_{i}:   same shape                               (× num_layers)

Outputs:
  logits:         [1, seq, vocab_size]  float16
  new_key_{i}:    [1, kv_heads, past+seq, head_dim]  float16  (× num_layers)
  new_val_{i}:    same                                         (× num_layers)
```

KVCache pre-allocated at max size. Shape: `(1, num_kv_heads, MAX_SEQ_LEN, head_dim)`.
Total KVCache size at FP16 = `num_layers × 2 × 1 × num_kv_heads × MAX_SEQ_LEN × head_dim × 2 bytes`.

Flatten past KV to positional args for ONNX (ONNX doesn't support list inputs).

### Step 4 — ONNX export

```python
torch.onnx.export(
    wrapped, dummy_inputs, ONNX_PATH,
    input_names=input_names, output_names=output_names,
    dynamic_axes=dynamic_axes,   # seq + past_seq dynamic for export only
    opset_version=17,
    do_constant_folding=True,
    export_params=True,
)
```

Verify with `onnx.checker.check_model()`. Print Sin/Cos op count (must be 0).

### Step 5 — PTQ W4A16 calibration

Use `wikitext-2-raw-v1` from HuggingFace datasets. Fallback: synthetic random tokens.
Cap at 200 passages for upload size. Save as `.npz`.

QAI Hub does the actual quantization server-side. We pass `--quantize_full_type w4a16`
in compile options. The calibration data tells QAI Hub which activations to observe.

### Step 6 — partition_override.json

```json
{
  "partition_overrides": [
    {"op_types": ["Softmax"], "target": "CPU",
     "reason": "FP16 Softmax numerics degrade reasoning accuracy on v75"},
    {"op_types": ["TopK"],    "target": "CPU",
     "reason": "TopK not natively supported on Hexagon HTP v75"}
  ]
}
```

### Step 7 — QAI Hub compile

```python
raw_model = hub.upload_model(ONNX_PATH)

compile_options = " ".join([
    "--target_runtime qnn_context_binary",
    "--quantize_full_type w4a16",
    "--quantize_weight_bits 4",
    "--disable_fusion",              # prevents MHA fusion breaking variable-len paths
    "--bias_as_int32",               # INT8 bias overflow guard (underscore, not hyphen)
    f"--max_seq_len {MAX_SEQ_LEN}",
    "--scratch_size_mib 16",         # Hexagon v75 SRAM scratch cap
    "--max_dynamic_tensor_size_mib 64",
    f"--partition_overrides {PARTITION_JSON}",
])

compile_job = hub.submit_compile_job(
    model=raw_model,
    device=hub.Device("Snapdragon 8 Elite"),
    name=f"{OUT_PREFIX}_compile",
    options=compile_options,
)
compile_job.wait()
```

Download compiled binary. Check manifest.json: all MatMul nodes must show `"target": "DSP"`.
Warn (don't fail) if any MatMul lands on CPU — throughput penalty only.

### Step 8 — HF publish (if PUBLISH_HF=1)

Upload to `Mer0vin8ian/qwen3-5-9b-npu-sm8750`.
Commit message: `Add qwen3_5_9b_htp.bin compiled from Qwen/Qwen3.5-9B (job {job_id})`.

### Step 9 — Print next steps

```
colab download /content/qwen3_5_9b_htp.bin
adb push qwen3_5_9b_htp.bin /storage/emulated/0/Download/
On device: DaemonLauncher.launch(genie_engine, --model qwen3_5_9b_htp.bin) → 127.0.0.1:8080
```

---

## Common Failure Modes (print these on job failure)

1. **Unsupported op on DSP** → add to partition_override.json, move to CPU
2. **Sin/Cos escaped RoPE fold** → check `rotary_emb` class name, patch that class directly
3. **Dynamic shapes not frozen** → verify ONNX has no symbolic dims after export
4. **W4A16 calibration rejected** → try passing calibration_data kwarg to submit_compile_job
5. **Memory OOM** → reduce MAX_SEQ_LEN to 2048, or split prefill/decode into two separate ONNX graphs
6. **`transformers` too old** → needs ≥4.51.0 for Qwen3.5 architecture support

---

## Also Needed After the Script

### Update `models/manifest.yaml`

Current manifest lists Qwen3-VL-8B-Thinking as Step 1. Must be corrected:

```yaml
build_order:
  - qwen3_5_9b          # PRIMARY — vision + reasoning, novel first-ever compile
  - qwen3_vl_8b_thinking  # backup
  - gemma4_12b_qat        # backup

models:
  qwen3_5_9b:
    hf_repo: Qwen/Qwen3.5-9B
    architecture: qwen3_5
    params: 9000000000
    max_seq_len: 4096
    quantization:
      strategy: ptq
      type: w4a16
      calibration_tokens: 10000
      calibration_dataset: wikitext-2-raw-v1
    output_repo: Mer0vin8ian/qwen3-5-9b-npu-sm8750
    output_artifacts:
      - qwen3_5_9b_htp.bin
    expected_compile_minutes: 35
    qai_hub_device: "Snapdragon 8 Elite"
    runtime: genie_engine
```

### Create `notebooks/compile_qwen3_5_9b.ipynb`

The `stage-colab.yml` workflow fires on changes to `scripts/compile_*.py` and
`notebooks/*.ipynb`. Create a companion notebook for one-click Colab execution.

Notebook structure:
1. Cell: Install deps
2. Cell: Set env vars (HF_TOKEN, QAI_HUB_API_TOKEN, etc.)
3. Cell: `%run scripts/compile_qwen3_5_9b.py`
4. Cell: Download / ADB push instructions

---

## Hexagon v75 Hard Constraints (never violate)

From `wiki/GPT-OSS-Reference.md` (v69 baseline — v75 is same or stricter):

| Constraint | Value | Why |
|---|---|---|
| RoPE fold | mandatory | No FP16 Sin/Cos kernel on HTP |
| Static shapes | batch=1, seq=4096 | Dynamic shapes unsupported |
| KVCache pre-alloc | full max size | QnnTensorUpdate buggy >4MB |
| GELU → tanh approx | mandatory | No FP16 erf on v75 |
| LayerNorm | pre-LN only | post-LN not supported |
| `--disable_fusion` | mandatory | MHA fusion breaks variable-len |
| `--bias-as-int32` | mandatory | INT8 bias overflow guard |
| Softmax / TopK | CPU only | HTP v75 limit |
| SRAM scratch | 16 MiB | Hard cap |
| Dynamic tensor | 64 MiB max | Hard cap |
| NPU contexts | 1 | Daemon serializes all inference |

---

## Corrections — Never Re-Hallucinate These

| Wrong | Correct |
|---|---|
| DeepSeek V4 Flash = free | PAID — ~$0.07/M on OpenRouter |
| SambaNova 405B active | DEAD — removed April 2025. Use `gpt-oss-120b` |
| Phi-3.5 9B exists | DOES NOT EXIST — Phi-3.5-Mini = 3.8B only |
| NVIDIA Alpamayo = desktop automation | WRONG — autonomous vehicle VLA model |
| Omnara = integrate it | ARCHIVED Feb 2 2026 — dead project |
| PWA = PXA | PXA was a typo. PWA = Progressive Web Apps |
| Startup crash = current P0 | The crash was the REASON for the daemon pivot. Architecture moved past it. |
| NeuroOmni.Vag-Agenti = active repo | REFERENCE ARTIFACT ONLY. Active repo = c10vis-poem/novus-agenti |
| System TTS works | Permanently faulty on device — use Kokoro/Sherpa-ONNX |
| System STT accurate | Inaccurate — use Whisper.cpp |
| Screen reader works | Permanently bugged on device |
| "no cloud" in neuromesh | WRONG — cloud API calls via HttpFetch ARE the primary use case |
| Qwen3-VL = 7B | Qwen3-VL is 8B. 7B was Qwen2.5 generation. |
| Qwen3.5 = 8B | Qwen3.5 family: 4B and 9B. No 8B exists. Primary target = 9B. |

---

## Mobile Paste Rules (never violate in Termux instructions)

- NEVER embed tokens or long URLs directly in paste-able commands
- Use shell variables: `T=TOKEN` as one line, then `$T` in short commands
- For git auth: use interactive prompt — user pastes token at a short `Password:` prompt
- Edit `.git/config` directly in nano for long URLs
- Keep every paste-able command under ~50 chars

---

## Architecture Reference (daemon paths)

```
Qwen3.5-9B / Qwen3-VL-8B:
  QAI Hub → .bin → genie_engine daemon → 127.0.0.1:8080
  [DaemonLauncher.launch(genie_engine, --model qwen3_5_9b_htp.bin)]

Gemma 4 12B QAT:
  QAI Hub → .dlc → ort_engine daemon → 127.0.0.1:8080
  [DaemonLauncher.launch(ort_engine, --model gemma4_12b_qat_htp.dlc)]

Kotlin app:
  NpuClient → POST /api/v1/generate → SSE-JSON token stream
  CliffordService → monitors daemon PID → 15s CRS recovery loop
```

Neither `genie_engine` nor `ort_engine` binary exists yet. Both must be built.
`genie_engine` has never been built publicly.

---

## Session 5 — What to Do

In order:

1. **Write `scripts/compile_qwen3_5_9b.py`** — full spec above. PyTorch from scratch, no scaffold.
2. **Update `models/manifest.yaml`** — Qwen3.5-9B as Step 1, VL-8B as backup, Gemma as backup.
3. **Create `notebooks/compile_qwen3_5_9b.ipynb`** — 4-cell Colab notebook wrapping the script.
4. **Commit and push** to `claude/epic-wozniak-7b0ban` on `c10vis-poem/novus-agenti`.
5. **Open a draft PR** if one doesn't exist for this branch.
6. **Update CLAUDE.md SOTU** — session 5 entry, new commits, current P0.
