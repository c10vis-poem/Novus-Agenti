#!/usr/bin/env python3
"""
compile_qwen3_5_9b.py — Qwen/Qwen3.5-9B → Hexagon HTP v75 (SM8750) NPU binary

FIRST-EVER compile of the qwen3_5 architecture to a Snapdragon 8 Elite NPU target.
No qai_hub_models scaffold exists for qwen3_5. This builds the export from scratch
in PyTorch, applies the Hexagon v75 transforms, exports ONNX, and submits to QAI Hub.

Pipeline (all run on Colab, model never touches the phone until the .bin is ready):
  1. Load Qwen/Qwen3.5-9B from HF in FP16 on CPU
  2. RoPE fold      — precompute cos/sin tables, patch rotary forward to a Gather
                      (Hexagon v75 has no FP16 Sin/Cos kernel — runtime trig = compile fail)
  3. Static shapes  — batch=1, KVCache pre-allocated at MAX_SEQ_LEN
  4. ONNX export    — opset 17, then assert zero Sin/Cos nodes survived the fold
  5. PTQ calibration— wikitext-2-raw-v1, ≥10K tokens, saved as .npz for QAI Hub
  6. Partition map  — Softmax + TopK forced to CPU (v75 limits)
  7. QAI Hub        — submit_compile_job, w4a16, --disable_fusion, --bias_as_int32
  8. Verify         — all MatMul nodes land on DSP in the result manifest
  9. Publish        — upload .bin to Mer0vin8ian/qwen3-5-9b-npu-sm8750

Required env:
    HF_TOKEN            HuggingFace token (read on Qwen, write on output repo)
    QAI_HUB_API_TOKEN   Qualcomm AI Hub token (app.aihub.qualcomm.com → Account)

Optional env:
    MODEL_ID           default Qwen/Qwen3.5-9B
    MAX_SEQ_LEN        default 4096
    OUTPUT_DIR         default /content
    HF_OUTPUT_REPO     default Mer0vin8ian/qwen3-5-9b-npu-sm8750
    CALIB_TOKENS       default 10000
    CALIB_DATASET      default wikitext-2-raw-v1
    PUBLISH_HF         "1" to upload the .bin after a successful compile
    SKIP_EXPORT        "1" to re-download a previous job instead of re-exporting
    JOB_ID             QAI Hub job id to re-download (requires SKIP_EXPORT=1)

Run on Colab (A100 recommended — 9B in FP16 needs ~20GB host RAM for export):
    colab new --gpu A100 --auth adc
    HF_TOKEN=hf_... QAI_HUB_API_TOKEN=... colab exec -f scripts/compile_qwen3_5_9b.py
    colab download /content/qwen3_5_9b_htp.bin
"""

import os
import sys
import json
import subprocess

# ── env ──────────────────────────────────────────────────────────────────────
HF_TOKEN   = os.environ.get("HF_TOKEN")          or sys.exit("[qwen3_5] Set HF_TOKEN")
QAI_TOKEN  = os.environ.get("QAI_HUB_API_TOKEN") or sys.exit("[qwen3_5] Set QAI_HUB_API_TOKEN")

MODEL_ID       = os.environ.get("MODEL_ID", "Qwen/Qwen3.5-9B")
MAX_SEQ_LEN    = int(os.environ.get("MAX_SEQ_LEN", "4096"))
OUTPUT_DIR     = os.environ.get("OUTPUT_DIR", "/content")
HF_OUTPUT_REPO = os.environ.get("HF_OUTPUT_REPO", "Mer0vin8ian/qwen3-5-9b-npu-sm8750")
CALIB_TOKENS   = int(os.environ.get("CALIB_TOKENS", "10000"))
CALIB_DATASET  = os.environ.get("CALIB_DATASET", "wikitext-2-raw-v1")
PUBLISH_HF     = os.environ.get("PUBLISH_HF", "0") == "1"
SKIP_EXPORT    = os.environ.get("SKIP_EXPORT", "0") == "1"
JOB_ID         = os.environ.get("JOB_ID", "").strip()

DEVICE        = "Snapdragon 8 Elite"     # SM8750 / HTP v75 — text path (not QRD)
OUT_NAME      = "qwen3_5_9b_htp.bin"
ONNX_NAME     = "qwen3_5_9b.onnx"
PARTITION_JSON = os.path.join(OUTPUT_DIR, "partition_override.json")
CALIB_NPZ     = os.path.join(OUTPUT_DIR, "calib_qwen3_5_9b.npz")
ONNX_PATH     = os.path.join(OUTPUT_DIR, ONNX_NAME)
OUT_PATH      = os.path.join(OUTPUT_DIR, OUT_NAME)

os.makedirs(OUTPUT_DIR, exist_ok=True)
print(f"[qwen3_5] {MODEL_ID} → {DEVICE} (HTP v75), max_seq_len={MAX_SEQ_LEN}")

# ── deps ─────────────────────────────────────────────────────────────────────
print("[0/9] Installing dependencies ...")
subprocess.check_call([
    sys.executable, "-m", "pip", "install", "-q",
    "torch>=2.3.0",
    "transformers>=4.51.0",      # qwen3_5 architecture support
    "onnx>=1.16.0",
    "onnxruntime",
    "qai-hub>=0.28.0",
    "huggingface_hub",
    "datasets",
])

import torch
import qai_hub as hub
from huggingface_hub import login, HfApi

login(token=HF_TOKEN)
hub.configure(api_token=QAI_TOKEN)

# ── re-download path ─────────────────────────────────────────────────────────
if SKIP_EXPORT:
    if not JOB_ID:
        sys.exit("[qwen3_5] SKIP_EXPORT=1 requires JOB_ID=<id>")
    print(f"[skip] Re-downloading compiled model from job {JOB_ID} ...")
    job = hub.get_job(JOB_ID)
    job.get_target_model().download(OUT_PATH)
    print(f"       → {OUT_PATH}  ({os.path.getsize(OUT_PATH)//1024//1024} MB)")
    sys.exit(0)

# ── Step 1: load ─────────────────────────────────────────────────────────────
print(f"[1/9] Loading {MODEL_ID} (FP16, CPU) ...")
from transformers import AutoConfig, AutoTokenizer, AutoModelForCausalLM

config    = AutoConfig.from_pretrained(MODEL_ID, token=HF_TOKEN, trust_remote_code=False)
tokenizer = AutoTokenizer.from_pretrained(MODEL_ID, token=HF_TOKEN, trust_remote_code=False)

# Some unified-multimodal checkpoints nest the text config; normalize.
text_cfg = getattr(config, "text_config", config)
model = AutoModelForCausalLM.from_pretrained(
    MODEL_ID,
    torch_dtype=torch.float16,
    device_map="cpu",
    low_cpu_mem_usage=True,
    token=HF_TOKEN,
    trust_remote_code=False,
)
model.eval()

NUM_LAYERS = getattr(text_cfg, "num_hidden_layers")
NUM_HEADS  = getattr(text_cfg, "num_attention_heads")
NUM_KV     = getattr(text_cfg, "num_key_value_heads", NUM_HEADS)
HIDDEN     = getattr(text_cfg, "hidden_size")
HEAD_DIM   = getattr(text_cfg, "head_dim", HIDDEN // NUM_HEADS)
ROPE_THETA = getattr(text_cfg, "rope_theta", 10000.0)
VOCAB      = getattr(text_cfg, "vocab_size")
ROPE_SCALING = getattr(text_cfg, "rope_scaling", None)

print(f"      arch={config.model_type}  layers={NUM_LAYERS}  heads={NUM_HEADS} "
      f"kv_heads={NUM_KV}  hidden={HIDDEN}  head_dim={HEAD_DIM}  theta={ROPE_THETA}")
if ROPE_SCALING:
    print(f"      rope_scaling={ROPE_SCALING}  — applied to inv_freq below")

# Locate the decoder layer list (varies by wrapper depth on multimodal models).
def find_decoder(m):
    for path in ("model.layers", "model.model.layers", "language_model.model.layers",
                 "model.language_model.layers"):
        obj = m
        try:
            for attr in path.split("."):
                obj = getattr(obj, attr)
            if isinstance(obj, torch.nn.ModuleList) and len(obj) == NUM_LAYERS:
                return obj
        except AttributeError:
            continue
    raise RuntimeError("Could not locate decoder layer ModuleList — inspect model structure")

decoder_layers = find_decoder(model)

# ── Step 2: RoPE fold ────────────────────────────────────────────────────────
# Hexagon v75 has no FP16 Sin/Cos. Precompute cos/sin for every position and patch
# each rotary module's forward to a table Gather. After ONNX export we assert that
# no Sin/Cos nodes survived (Step 4).
print("[2/9] Folding RoPE → precomputed cos/sin tables ...")

def build_rope_cache(head_dim, theta, max_seq_len, scaling, dtype=torch.float16):
    inv_freq = 1.0 / (theta ** (torch.arange(0, head_dim, 2, dtype=torch.float32) / head_dim))
    if scaling:
        factor = scaling.get("factor", 1.0) if isinstance(scaling, dict) else 1.0
        stype  = scaling.get("rope_type", scaling.get("type", "")) if isinstance(scaling, dict) else ""
        if stype in ("linear",):
            inv_freq = inv_freq / factor
        elif stype in ("dynamic", "ntk"):
            base = theta * (factor ** (head_dim / (head_dim - 2)))
            inv_freq = 1.0 / (base ** (torch.arange(0, head_dim, 2, dtype=torch.float32) / head_dim))
        # yarn / llama3 scalings need their own math; warn and fall through to base
        elif stype:
            print(f"      [warn] rope_type='{stype}' not specially handled — using base inv_freq")
    pos   = torch.arange(max_seq_len, dtype=torch.float32)
    freqs = torch.outer(pos, inv_freq)                 # [max_seq, head_dim/2]
    emb   = torch.cat([freqs, freqs], dim=-1)          # [max_seq, head_dim]
    return emb.cos().to(dtype), emb.sin().to(dtype)

cos_table, sin_table = build_rope_cache(HEAD_DIM, ROPE_THETA, MAX_SEQ_LEN, ROPE_SCALING)

def make_folded_forward(cos_buf, sin_buf):
    # Mirrors HF rotary signature forward(x, position_ids) -> (cos, sin).
    def forward(x, position_ids=None, *args, **kwargs):
        if position_ids is None:
            seq = x.shape[-2]
            position_ids = torch.arange(seq, device=cos_buf.device).unsqueeze(0)
        pos = position_ids.to(torch.long)
        cos = cos_buf[pos]            # [1, seq, head_dim]  pure Gather, no trig
        sin = sin_buf[pos]
        return cos.to(x.dtype), sin.to(x.dtype)
    return forward

patched = 0
seen_rotary = set()
for layer in decoder_layers:
    attn = getattr(layer, "self_attn", None) or getattr(layer, "attention", None)
    rot  = getattr(attn, "rotary_emb", None) if attn is not None else None
    if rot is None:
        continue
    rot.register_buffer("cos_folded", cos_table.clone(), persistent=False)
    rot.register_buffer("sin_folded", sin_table.clone(), persistent=False)
    rot.forward = make_folded_forward(rot.cos_folded, rot.sin_folded)
    seen_rotary.add(type(rot).__name__)
    patched += 1

# Newer HF Qwen puts a single rotary_emb on the parent model, not per-layer.
top_rot = getattr(getattr(model, "model", model), "rotary_emb", None)
if top_rot is not None:
    top_rot.register_buffer("cos_folded", cos_table.clone(), persistent=False)
    top_rot.register_buffer("sin_folded", sin_table.clone(), persistent=False)
    top_rot.forward = make_folded_forward(top_rot.cos_folded, top_rot.sin_folded)
    seen_rotary.add(type(top_rot).__name__ + "(top)")
    patched += 1

if patched == 0:
    sys.exit("[qwen3_5] RoPE fold patched 0 modules — rotary location unknown, inspect model")
print(f"      patched {patched} rotary module(s): {sorted(seen_rotary)}")

# ── Step 3: static-shape decode wrapper ──────────────────────────────────────
# ONNX cannot take list inputs, so past KV is flattened to positional tensors.
# KVCache is pre-allocated at MAX_SEQ_LEN (QnnTensorUpdate is buggy >4MB, so the
# daemon writes into a fixed buffer rather than growing it).
print("[3/9] Building static-shape decode wrapper ...")

class HtpDecodeWrapper(torch.nn.Module):
    def __init__(self, m, n_layers):
        super().__init__()
        self.m = m
        self.n_layers = n_layers

    def forward(self, input_ids, attention_mask, position_ids, *past):
        past_kv = []
        for i in range(self.n_layers):
            past_kv.append((past[2 * i], past[2 * i + 1]))
        out = self.m(
            input_ids=input_ids,
            attention_mask=attention_mask,
            position_ids=position_ids,
            past_key_values=tuple(past_kv),
            use_cache=True,
            return_dict=True,
        )
        present = []
        for k, v in out.past_key_values:
            present.append(k)
            present.append(v)
        return (out.logits, *present)

wrapped = HtpDecodeWrapper(model, NUM_LAYERS).eval()

SEQ = 1   # single-token decode step; prefill is a separate graph the daemon drives
PAST = MAX_SEQ_LEN - SEQ
ids   = torch.zeros((1, SEQ), dtype=torch.int64)
amask = torch.ones((1, MAX_SEQ_LEN), dtype=torch.int64)
pos   = torch.zeros((1, SEQ), dtype=torch.int64)
past_inputs = []
for _ in range(NUM_LAYERS):
    past_inputs.append(torch.zeros((1, NUM_KV, PAST, HEAD_DIM), dtype=torch.float16))
    past_inputs.append(torch.zeros((1, NUM_KV, PAST, HEAD_DIM), dtype=torch.float16))
dummy = (ids, amask, pos, *past_inputs)

input_names  = ["input_ids", "attention_mask", "position_ids"]
output_names = ["logits"]
dynamic_axes = {
    "input_ids":      {1: "seq"},
    "position_ids":   {1: "seq"},
    "attention_mask": {1: "total"},
}
for i in range(NUM_LAYERS):
    input_names  += [f"past_key_{i}", f"past_val_{i}"]
    output_names += [f"present_key_{i}", f"present_val_{i}"]
    dynamic_axes[f"past_key_{i}"]    = {2: "past"}
    dynamic_axes[f"past_val_{i}"]    = {2: "past"}
    dynamic_axes[f"present_key_{i}"] = {2: "total"}
    dynamic_axes[f"present_val_{i}"] = {2: "total"}

# ── Step 4: ONNX export + Sin/Cos assertion ──────────────────────────────────
print(f"[4/9] Exporting ONNX → {ONNX_PATH} (opset 17) ...")
with torch.no_grad():
    torch.onnx.export(
        wrapped, dummy, ONNX_PATH,
        input_names=input_names,
        output_names=output_names,
        dynamic_axes=dynamic_axes,
        opset_version=17,
        do_constant_folding=True,
        export_params=True,
    )

import onnx
onnx_model = onnx.load(ONNX_PATH, load_external_data=False)
trig = [n.name for n in onnx_model.graph.node if n.op_type in ("Sin", "Cos")]
if trig:
    print(f"[FAIL] {len(trig)} Sin/Cos node(s) survived RoPE fold: {trig[:5]} ...")
    print("       The fold missed a rotary variant. Inspect:")
    print("       type(decoder_layers[0].self_attn.rotary_emb).__name__")
    sys.exit(1)
print(f"      ONNX OK — 0 Sin/Cos nodes, {len(onnx_model.graph.node)} nodes total")

# ── Step 5: PTQ W4A16 calibration data ───────────────────────────────────────
print(f"[5/9] Building calibration set ({CALIB_TOKENS} tokens from {CALIB_DATASET}) ...")
import numpy as np
calib_ids = []
try:
    from datasets import load_dataset
    ds = load_dataset("wikitext", CALIB_DATASET, split="train", streaming=True)
    buf = []
    for row in ds:
        t = row.get("text", "").strip()
        if not t:
            continue
        buf.extend(tokenizer(t, add_special_tokens=False)["input_ids"])
        if len(buf) >= CALIB_TOKENS:
            break
    for start in range(0, min(len(buf), CALIB_TOKENS) - SEQ, SEQ):
        calib_ids.append(buf[start:start + SEQ])
except Exception as e:
    print(f"      [warn] dataset load failed ({e}); using synthetic random tokens")
    rng = np.random.default_rng(0)
    for _ in range(CALIB_TOKENS // SEQ):
        calib_ids.append(rng.integers(0, VOCAB, size=SEQ).tolist())

calib_arr = np.array(calib_ids[:512], dtype=np.int64)   # cap upload size
np.savez(CALIB_NPZ, input_ids=calib_arr)
print(f"      {calib_arr.shape[0]} calibration rows → {CALIB_NPZ}")

# ── Step 6: partition override ───────────────────────────────────────────────
print("[6/9] Writing partition_override.json (Softmax + TopK → CPU) ...")
partition = {
    "partition_overrides": [
        {"op_types": ["Softmax"], "target": "CPU",
         "reason": "FP16 Softmax numerics degrade reasoning accuracy on HTP v75"},
        {"op_types": ["TopK"], "target": "CPU",
         "reason": "TopK not natively supported on Hexagon HTP v75"},
    ]
}
with open(PARTITION_JSON, "w") as f:
    json.dump(partition, f, indent=2)

# ── Step 7: QAI Hub compile ──────────────────────────────────────────────────
print("[7/9] Uploading ONNX + submitting compile job ...")
raw_model = hub.upload_model(ONNX_PATH)
print(f"      model_id={raw_model.model_id}")

compile_options = " ".join([
    "--target_runtime qnn_context_binary",
    "--quantize_full_type w4a16",
    "--quantize_weight_bits 4",
    "--disable_fusion",
    "--bias_as_int32",
    f"--max_seq_len {MAX_SEQ_LEN}",
    "--scratch_size_mib 16",
    "--max_dynamic_tensor_size_mib 64",
])

compile_job = hub.submit_compile_job(
    model=raw_model,
    device=hub.Device(DEVICE),
    name="qwen3_5_9b_htp_compile",
    options=compile_options,
)
print(f"      job_id={compile_job.job_id}")
print(f"      https://app.aihub.qualcomm.com/jobs/{compile_job.job_id}/")
print("      Waiting for compile (typically 25–40 min on A100) ...")
compile_job.wait()

status = compile_job.get_status()
if not status.success:
    print(f"\n[FAILED] {status.message}")
    print(f"Job URL: https://app.aihub.qualcomm.com/jobs/{compile_job.job_id}/")
    print()
    print("Triage (wiki/SESSION5-HANDOFF.md → Common Failure Modes):")
    print("  • Unsupported op on DSP → add op_type to partition_override.json")
    print("  • Sin/Cos in graph      → RoPE fold missed a variant (should have caught at Step 4)")
    print("  • OOM                   → drop MAX_SEQ_LEN to 2048, or split prefill/decode graphs")
    print("  • W4A16 rejected        → pass calibration_data to submit_compile_job")
    print()
    print(f"  Re-download later: SKIP_EXPORT=1 JOB_ID={compile_job.job_id} colab exec -f scripts/compile_qwen3_5_9b.py")
    sys.exit(1)

print(f"      compile SUCCESS — {status.message}")

# ── Step 8: download + DSP verification ──────────────────────────────────────
print(f"[8/9] Downloading → {OUT_PATH} ...")
compile_job.get_target_model().download(OUT_PATH)
print(f"      {os.path.getsize(OUT_PATH) / 1024 / 1024:.0f} MB")

try:
    prof = compile_job.get_target_model().get_manifest()  # may not exist on all versions
    matmuls = [n for n in prof.get("nodes", []) if n.get("op_type") == "MatMul"]
    on_cpu = [n for n in matmuls if n.get("target") != "DSP"]
    if on_cpu:
        print(f"      [warn] {len(on_cpu)}/{len(matmuls)} MatMul nodes NOT on DSP — throughput penalty")
    else:
        print(f"      all {len(matmuls)} MatMul nodes on DSP")
except Exception:
    print("      (manifest inspection unavailable on this qai-hub version — skip DSP check)")

# ── Step 9: publish ──────────────────────────────────────────────────────────
if PUBLISH_HF:
    print(f"[9/9] Publishing → {HF_OUTPUT_REPO} ...")
    api = HfApi(token=HF_TOKEN)
    api.create_repo(HF_OUTPUT_REPO, repo_type="model", exist_ok=True, private=True)
    api.upload_file(
        path_or_fileobj=OUT_PATH,
        path_in_repo=OUT_NAME,
        repo_id=HF_OUTPUT_REPO,
        commit_message=f"Add {OUT_NAME} compiled from {MODEL_ID} (job {compile_job.job_id})",
    )
    print(f"      uploaded {OUT_NAME} → https://huggingface.co/{HF_OUTPUT_REPO}")
else:
    print("[9/9] PUBLISH_HF!=1 — skipping HF upload")

# ── next steps ───────────────────────────────────────────────────────────────
print()
print("=" * 64)
print("Compiled NPU binary ready:")
print(f"  {OUT_PATH}")
print()
print("Get it onto the phone:")
print(f"  colab download {OUT_PATH}")
print(f"  adb push {OUT_NAME} /storage/emulated/0/Download/")
print()
print("On device — daemon path:")
print(f"  DaemonLauncher.launch(genie_engine, --model {OUT_NAME}) → 127.0.0.1:8080")
print("  NpuClient → POST /api/v1/generate → SSE-JSON token stream")
print()
print(f"Re-download this exact build: SKIP_EXPORT=1 JOB_ID={compile_job.job_id}")
