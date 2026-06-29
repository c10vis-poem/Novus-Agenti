#!/usr/bin/env python3
"""
compile_qwen3_5_9b.py — Qwen/Qwen3.5-9B (multimodal) → Hexagon HTP NPU binaries

PRIMARY TARGET. First-ever compile of the Qwen3.5-9B native-multimodal architecture
to Snapdragon 8 Elite Hexagon HTP. Produces chunked NPU artifacts:

    qwen3_5_9b_vision_encoder.bin         — vision tower (image patches → embeddings)
    qwen3_5_9b_projection.bin             — vision→language adapter (embeddings → token space)
    qwen3_5_9b_decoder_chunk_{0..N-1}.bin — language decoder split into N layer-group chunks

The decoder is split into NUM_CHUNKS (default 4) layer groups to keep each ONNX
upload and QAI Hub compile job within practical size limits. At runtime the
ort_engine daemon loads all chunks sequentially.

At runtime the ort_engine daemon (ONNX Runtime + QNN execution provider) loads the
.bin files and serves inference. The compiled qnn_context_binary is compatible with
ORT+QNN-EP; no Genie SDK required.

Sources of truth (do not paraphrase elsewhere):
  • wiki/GPT-OSS-Reference.md         — Hexagon failure modes + mitigations
  • models/manifest.yaml              — build order, target devices, expected sizes

Hexagon constraints applied (per GPT-OSS reference, §1-§3):
  • RoPE fold          precompute cos/sin to FP16 tables, replace rotary with Gather
  • Static shapes      batch=1, MAX_SEQ_LEN compile-time, valid-length scalar at runtime
  • KV cache           pre-allocated at MAX_SEQ_LEN (QnnTensorUpdate buggy >4 MiB)
  • --disable_fusion   MHA fusion only works for static seq + INT8 weights
  • --bias-as-int32    INT8 bias overflow guard
  • partition override Softmax + TopK forced to CPU (partition_override.json)
  • Scratch 16 MiB / dynamic tensor 64 MiB / single NPU context
  • W4A16              per-channel INT4 weights, FP16 activations
  • Chunked decoder    split into NUM_CHUNKS layer groups for manageable compile sizes

Required env (HF Jobs injects via --secrets):
    HF_TOKEN              HuggingFace write token (Mer0vin8ian)
    QAI_HUB_API_TOKEN     Qualcomm AI Hub token

Optional env:
    MODEL_ID              default Mer0vin8ian/Qwen3.5-9B
    MAX_SEQ_LEN           default 4096
    OUTPUT_DIR            default /tmp
    HF_OUTPUT_REPO        default Mer0vin8ian/qwen3-5-9b-npu-sm8750
    QAI_HUB_DEVICE        default "Snapdragon 8 Elite"
    NUM_CHUNKS            default 4 — number of decoder layer-group chunks
    CALIB_TOKENS          default 10000
    CALIB_DATASET         default wikitext-2-raw-v1
    PUBLISH_HF            "1" to upload all three artifacts after compile
    SKIP_VISION           "1" to compile language-decoder only (debug fallback)
    SKIP_EXPORT           "1" to re-download a previous job (requires JOB_ID*)
    JOB_ID_VISION         re-download id for vision encoder
    JOB_ID_PROJECTION     re-download id for projection
    JOB_ID_DECODER_*      re-download ids for decoder chunks (JOB_ID_DECODER_0, etc.)

Run on HF Jobs (cpu-xl, ~$1/hr, 124GB RAM — no GPU needed, compile is server-side):
    hf jobs uv run --flavor cpu-xl --timeout 2h \\
        --with torch --with transformers --with onnx --with onnxruntime \\
        --with qai-hub --with datasets --with numpy --with huggingface_hub \\
        --secrets HF_TOKEN --secrets QAI_HUB_API_TOKEN \\
        -e MODEL_ID=Mer0vin8ian/Qwen3.5-9B -e PUBLISH_HF=1 -e OUTPUT_DIR=/tmp \\
        https://raw.githubusercontent.com/c10vis-poem/Novus-Agenti/claude/project-scope-review-lf615p/scripts/compile_qwen3_5_9b.py
"""

import os
import sys
import json
import subprocess

# ── env ────────────────────────────────────────────────────────────────────────────
HF_TOKEN  = os.environ.get("HF_TOKEN")          or sys.exit("[qwen3_5] Set HF_TOKEN")
QAI_TOKEN = os.environ.get("QAI_HUB_API_TOKEN") or sys.exit("[qwen3_5] Set QAI_HUB_API_TOKEN")

MODEL_ID       = os.environ.get("MODEL_ID", "Mer0vin8ian/Qwen3.5-9B")
MAX_SEQ_LEN    = int(os.environ.get("MAX_SEQ_LEN", "4096"))
OUTPUT_DIR     = os.environ.get("OUTPUT_DIR", "/tmp")
HF_OUTPUT_REPO = os.environ.get("HF_OUTPUT_REPO", "Mer0vin8ian/qwen3-5-9b-npu-sm8750")
QAI_HUB_DEVICE = os.environ.get("QAI_HUB_DEVICE", "Snapdragon 8 Elite")
CALIB_TOKENS   = int(os.environ.get("CALIB_TOKENS", "10000"))
CALIB_DATASET  = os.environ.get("CALIB_DATASET", "wikitext-2-raw-v1")
PUBLISH_HF     = os.environ.get("PUBLISH_HF", "0") == "1"
SKIP_VISION    = os.environ.get("SKIP_VISION", "0") == "1"
SKIP_EXPORT    = os.environ.get("SKIP_EXPORT", "0") == "1"

JOB_ID_VISION     = os.environ.get("JOB_ID_VISION", "").strip()
JOB_ID_PROJECTION = os.environ.get("JOB_ID_PROJECTION", "").strip()

os.makedirs(OUTPUT_DIR, exist_ok=True)

OUT_VISION     = os.path.join(OUTPUT_DIR, "qwen3_5_9b_vision_encoder.bin")
OUT_PROJECTION = os.path.join(OUTPUT_DIR, "qwen3_5_9b_projection.bin")

def decoder_chunk_out(i):
    return os.path.join(OUTPUT_DIR, f"qwen3_5_9b_decoder_chunk_{i}.bin")

print(f"[qwen3_5] {MODEL_ID} → {QAI_HUB_DEVICE} (Hexagon HTP), max_seq_len={MAX_SEQ_LEN}")

# ── deps ───────────────────────────────────────────────────────────────────────────
print("[0/11] Installing dependencies ...")
subprocess.check_call([
    sys.executable, "-m", "pip", "install", "-q",
    "torch>=2.3.0", "transformers>=4.51.0", "onnx>=1.16.0",
    "onnxruntime", "onnxscript", "qai-hub>=0.28.0",
    "huggingface_hub", "datasets", "accelerate",
])

import torch
import qai_hub as hub
from huggingface_hub import login, HfApi

login(token=HF_TOKEN)
if hasattr(hub, "configure"):
    hub.configure(api_token=QAI_TOKEN)
else:
    os.environ.setdefault("QAI_HUB_API_TOKEN", QAI_TOKEN)


# ── helpers ────────────────────────────────────────────────────────────────────────
def build_rope_cache(head_dim, theta, max_seq_len, scaling, dtype=torch.float16):
    inv_freq = 1.0 / (theta ** (torch.arange(0, head_dim, 2, dtype=torch.float32) / head_dim))
    if isinstance(scaling, dict):
        factor = scaling.get("factor", 1.0)
        stype  = scaling.get("rope_type", scaling.get("type", ""))
        if stype == "linear":
            inv_freq = inv_freq / factor
        elif stype in ("dynamic", "ntk"):
            base = theta * (factor ** (head_dim / (head_dim - 2)))
            inv_freq = 1.0 / (base ** (torch.arange(0, head_dim, 2, dtype=torch.float32) / head_dim))
        elif stype:
            print(f"      [warn] rope_type='{stype}' not specially handled")
    pos   = torch.arange(max_seq_len, dtype=torch.float32)
    freqs = torch.outer(pos, inv_freq)
    emb   = torch.cat([freqs, freqs], dim=-1)
    return emb.cos().to(dtype), emb.sin().to(dtype)


def make_folded_rope_forward(cos_buf, sin_buf):
    def fwd(x, position_ids=None, *args, **kwargs):
        # Returns [B, S, D] (3D) so apply_rotary_pos_emb's unsqueeze(1)
        # produces [B, 1, S, D] — broadcasts cleanly with [B, H, S, D] q/k.
        # M-RoPE [3,B,S,D] would unsqueeze to 5D and break cat with 4D q_pass.
        if position_ids is not None and position_ids.dim() == 3:
            pos_2d = position_ids[0].to(torch.long)
        elif position_ids is not None and position_ids.dim() == 2:
            pos_2d = position_ids.to(torch.long)
        elif position_ids is not None:
            pos_2d = position_ids.to(torch.long).unsqueeze(0)
        else:
            seq = x.shape[-2] if x.dim() >= 2 else cos_buf.shape[0]
            pos_2d = torch.arange(seq, device=cos_buf.device).unsqueeze(0)
        return cos_buf[pos_2d].to(x.dtype), sin_buf[pos_2d].to(x.dtype)
    return fwd


def patch_rope(module, cos_table, sin_table, label):
    patched = 0
    seen = set()
    for sub in module.modules():
        rot = getattr(sub, "rotary_emb", None)
        if rot is None:
            continue
        rot.register_buffer("cos_folded", cos_table.clone(), persistent=False)
        rot.register_buffer("sin_folded", sin_table.clone(), persistent=False)
        rot.forward = make_folded_rope_forward(rot.cos_folded, rot.sin_folded)
        seen.add(type(rot).__name__)
        patched += 1
    top_rot = getattr(module, "rotary_emb", None)
    if top_rot is not None and type(top_rot).__name__ not in seen:
        top_rot.register_buffer("cos_folded", cos_table.clone(), persistent=False)
        top_rot.register_buffer("sin_folded", sin_table.clone(), persistent=False)
        top_rot.forward = make_folded_rope_forward(top_rot.cos_folded, top_rot.sin_folded)
        seen.add(type(top_rot).__name__ + "(top)")
        patched += 1
    print(f"      [{label}] RoPE fold patched {patched} module(s): {sorted(seen)}")
    return patched


def assert_no_trig(onnx_path, stage_label):
    import onnx
    m = onnx.load(onnx_path, load_external_data=False)
    trig = [n.name for n in m.graph.node if n.op_type in ("Sin", "Cos")]
    if trig:
        print(f"[FAIL/{stage_label}] {len(trig)} Sin/Cos node(s) survived RoPE fold: {trig[:5]}")
        sys.exit(1)
    print(f"      [{stage_label}] ONNX OK — 0 Sin/Cos, {len(m.graph.node)} nodes total")


def write_partition_override(path):
    partition = {
        "partition_overrides": [
            {"op_types": ["Softmax"], "target": "CPU"},
            {"op_types": ["TopK"],    "target": "CPU"},
        ]
    }
    with open(path, "w") as f:
        json.dump(partition, f, indent=2)

PARTITION_JSON = os.path.join(OUTPUT_DIR, "partition_override.json")
write_partition_override(PARTITION_JSON)

NUM_CHUNKS = int(os.environ.get("NUM_CHUNKS", "4"))

COMPILE_OPTIONS_BASE = " ".join([
    "--target_runtime qnn_context_binary",
    "--quantize_full_type w4a16",
    "--quantize_weight_bits 4",
    "--disable_fusion",
    "--bias_as_int32",
    "--scratch_size_mib 16",
    "--max_dynamic_tensor_size_mib 64",
    f"--partition_overrides {PARTITION_JSON}",
])


def submit_qai_hub_compile(onnx_path, name, extra_options="", calibration_data=None):
    raw_model = hub.upload_model(onnx_path)
    print(f"      qai_hub model_id={raw_model.model_id}")
    options = COMPILE_OPTIONS_BASE + (" " + extra_options if extra_options else "")
    kwargs = dict(model=raw_model, device=hub.Device(QAI_HUB_DEVICE),
                  name=name, options=options)
    if calibration_data is not None:
        kwargs["calibration_data"] = calibration_data
    job = hub.submit_compile_job(**kwargs)
    print(f"      job_id={job.job_id}")
    print(f"      https://app.aihub.qualcomm.com/jobs/{job.job_id}/")
    print(f"      Waiting for compile (~25-40 min per artifact) ...")
    job.wait()
    status = job.get_status()
    if not status.success:
        print(f"\n[FAILED/{name}] {status.message}")
        print(f"  Re-download later: SKIP_EXPORT=1 JOB_ID_*={job.job_id}")
        sys.exit(1)
    print(f"      [{name}] compile SUCCESS")
    return job


def download_target_model(job, out_path):
    job.get_target_model().download(out_path)
    sz_mb = os.path.getsize(out_path) / 1024 / 1024
    print(f"      → {out_path} ({sz_mb:.0f} MB)")
    return out_path


def verify_dsp_placement(job, label):
    try:
        manifest = job.get_target_model().get_manifest()
        matmuls = [n for n in manifest.get("nodes", []) if n.get("op_type") == "MatMul"]
        on_cpu = [n for n in matmuls if n.get("target") != "DSP"]
        if on_cpu:
            print(f"      [{label}] WARN {len(on_cpu)}/{len(matmuls)} MatMul nodes NOT on DSP")
        else:
            print(f"      [{label}] all {len(matmuls)} MatMul nodes on DSP")
    except Exception:
        print(f"      [{label}] (manifest inspection unavailable)")


# ── SKIP_EXPORT fast path ──────────────────────────────────────────────────────────
if SKIP_EXPORT:
    any_found = False
    for job_id, out_path, label in [
        (JOB_ID_VISION, OUT_VISION, "vision"),
        (JOB_ID_PROJECTION, OUT_PROJECTION, "projection"),
    ]:
        if job_id:
            print(f"[skip/{label}] Re-downloading from job {job_id} ...")
            job = hub.get_job(job_id)
            download_target_model(job, out_path)
            any_found = True
    for i in range(NUM_CHUNKS):
        jid = os.environ.get(f"JOB_ID_DECODER_{i}", "").strip()
        if jid:
            print(f"[skip/decoder_chunk_{i}] Re-downloading from job {jid} ...")
            job = hub.get_job(jid)
            download_target_model(job, decoder_chunk_out(i))
            any_found = True
    if not any_found:
        sys.exit("[qwen3_5] SKIP_EXPORT=1 requires at least one JOB_ID_*")
    sys.exit(0)


# ── Step 1: load model ─────────────────────────────────────────────────────────────
print(f"[1/11] Loading {MODEL_ID} (FP16, CPU) ...")
from transformers import AutoConfig, AutoTokenizer
import transformers as _tf

config   = AutoConfig.from_pretrained(MODEL_ID, token=HF_TOKEN, trust_remote_code=False)
tokenizer = AutoTokenizer.from_pretrained(MODEL_ID, token=HF_TOKEN, trust_remote_code=False)
text_cfg = getattr(config, "text_config", config)

LOAD_KW = dict(
    torch_dtype=torch.float16, device_map="cpu", low_cpu_mem_usage=True,
    token=HF_TOKEN, trust_remote_code=False,
)

LOADER_PREFERENCE = ["AutoModelForMultimodalLM", "AutoModelForVision2Seq",
                     "AutoModelForImageTextToText"]
if SKIP_VISION:
    LOADER_PREFERENCE.append("AutoModelForCausalLM")

full = None
loader_used = None
for loader_name in LOADER_PREFERENCE:
    if not hasattr(_tf, loader_name):
        continue
    try:
        full = getattr(_tf, loader_name).from_pretrained(MODEL_ID, **LOAD_KW)
        loader_used = loader_name
        break
    except (ValueError, KeyError, RuntimeError) as e:
        print(f"      {loader_name} unavailable ({type(e).__name__}); trying next ...")
if full is None:
    sys.exit(f"[qwen3_5] No Auto* loader accepted {MODEL_ID}")
print(f"      Loaded via {loader_used}: {type(full).__name__}")


def find_submodule(root, names):
    for path in names:
        obj = root
        ok = True
        for attr in path.split("."):
            if not hasattr(obj, attr):
                ok = False; break
            obj = getattr(obj, attr)
        if ok and obj is not None:
            return obj, path
    return None, None


vision_module,     vision_path     = find_submodule(full, [
    "visual", "model.visual", "vision_tower", "model.vision_tower",
    "vision_model", "model.vision_model", "vision_encoder"])
projection_module, projection_path = find_submodule(full, [
    "visual.merger", "model.visual.merger", "multi_modal_projector",
    "model.multi_modal_projector", "mm_projector", "model.mm_projector"])
decoder_module,    decoder_path    = find_submodule(full, [
    "language_model", "model.language_model", "model"])

print(f"      vision:     {vision_path or 'NOT FOUND'}")
print(f"      projection: {projection_path or 'NOT FOUND'}")
print(f"      decoder:    {decoder_path or 'NOT FOUND'}")

if SKIP_VISION:
    if decoder_module is None:
        sys.exit("[qwen3_5] Could not locate language decoder")
elif vision_module is None or projection_module is None or decoder_module is None:
    print(f"[qwen3_5] Missing submodule(s). Set SKIP_VISION=1 or add path.")
    print(f"          model attrs: {[a for a in dir(full) if not a.startswith('_')][:20]}")
    sys.exit(1)

NUM_LAYERS   = getattr(text_cfg, "num_hidden_layers")
NUM_HEADS    = getattr(text_cfg, "num_attention_heads")
NUM_KV       = getattr(text_cfg, "num_key_value_heads", NUM_HEADS)
HIDDEN       = getattr(text_cfg, "hidden_size")
HEAD_DIM     = getattr(text_cfg, "head_dim", HIDDEN // NUM_HEADS)
ROPE_THETA   = getattr(text_cfg, "rope_theta", 10000.0)
VOCAB        = getattr(text_cfg, "vocab_size")
ROPE_SCALING = getattr(text_cfg, "rope_scaling", None)
print(f"      arch={config.model_type}  layers={NUM_LAYERS}  heads={NUM_HEADS}  "
      f"kv={NUM_KV}  hidden={HIDDEN}  head_dim={HEAD_DIM}  theta={ROPE_THETA}")

print(f"      compile_options: {COMPILE_OPTIONS_BASE}")
print(f"      decoder chunks: {NUM_CHUNKS}")


# ── Step 2: RoPE fold ──────────────────────────────────────────────────────────────
print("[2/11] Folding RoPE on language decoder ...")
cos_table, sin_table = build_rope_cache(HEAD_DIM, ROPE_THETA, MAX_SEQ_LEN, ROPE_SCALING)
patched = patch_rope(decoder_module, cos_table, sin_table, "decoder")
if patched == 0:
    sys.exit("[qwen3_5] RoPE fold patched 0 modules — inspect rotary_emb location")


# ── Step 2.5: Module-level apply_rotary_pos_emb patch (M-RoPE 5D/4D fix) ─────────
# Root cause: M-RoPE returns [3,B,S,D]; unsqueeze(1) → 5D; cat(q_embed_5D, q_pass_4D)
# → "got 5 and 4". Fix: bypass cos/sin from rotary_emb, use [1,1,S,D] precomputed.
print("[2.5/11] Patching apply_rotary_pos_emb at module level ...")
import transformers.models.qwen3_5.modeling_qwen3_5 as _qm35

_ARP_COS = cos_table
_ARP_SIN = sin_table


def _patched_apply_rotary_pos_emb(q, k, cos, sin, unsqueeze_dim=1):
    seq_len = q.shape[2]
    c = _ARP_COS[:seq_len].unsqueeze(0).unsqueeze(0).to(q.dtype)  # [1,1,S,D]
    s = _ARP_SIN[:seq_len].unsqueeze(0).unsqueeze(0).to(q.dtype)
    rotary_dim = c.shape[-1]
    q_rot, q_pass = q[..., :rotary_dim], q[..., rotary_dim:]
    k_rot, k_pass = k[..., :rotary_dim], k[..., rotary_dim:]

    def _rot_half(x):
        h = x.shape[-1] // 2
        return torch.cat((-x[..., h:], x[..., :h]), dim=-1)

    q_embed = (q_rot * c) + (_rot_half(q_rot) * s)
    k_embed = (k_rot * c) + (_rot_half(k_rot) * s)
    return torch.cat([q_embed, q_pass], dim=-1), torch.cat([k_embed, k_pass], dim=-1)


_qm35.apply_rotary_pos_emb = _patched_apply_rotary_pos_emb
print(f"      apply_rotary_pos_emb → precomputed [1,1,S,{HEAD_DIM}] FP16 Gather")


# ── Step 3: static-shape wrappers ─────────────────────────────────────────────────
print("[3/11] Building static-shape wrappers ...")


class VisionEncoderWrapper(torch.nn.Module):
    def __init__(self, m): super().__init__(); self.m = m
    def forward(self, pixel_values, grid_thw):
        out = self.m(pixel_values, grid_thw=grid_thw)
        if hasattr(out, "last_hidden_state"): return out.last_hidden_state
        return out[0] if isinstance(out, (tuple, list)) else out


class ProjectionWrapper(torch.nn.Module):
    def __init__(self, m): super().__init__(); self.m = m
    def forward(self, vision_embeds): return self.m(vision_embeds)


# ── Locate decoder internals for chunking ─────────────────────────────────────────
model_inner = getattr(decoder_module, "model", decoder_module)
all_layers = None
for attr in ["layers", "decoder.layers"]:
    obj = model_inner
    for part in attr.split("."):
        obj = getattr(obj, part, None)
        if obj is None:
            break
    if obj is not None:
        all_layers = obj
        break
if all_layers is None:
    sys.exit("[qwen3_5] Could not locate decoder layers for chunking")

embed_tokens = getattr(model_inner, "embed_tokens", None)
final_norm = getattr(model_inner, "norm", getattr(model_inner, "final_layernorm", None))
lm_head = getattr(decoder_module, "lm_head", getattr(full, "lm_head", None))

total_layers = len(all_layers)
chunk_size = (total_layers + NUM_CHUNKS - 1) // NUM_CHUNKS
print(f"      {total_layers} layers → {NUM_CHUNKS} chunks of ~{chunk_size} layers")
print(f"      embed_tokens: {'found' if embed_tokens else 'NOT FOUND'}")
print(f"      final_norm:   {'found' if final_norm else 'NOT FOUND'}")
print(f"      lm_head:      {'found' if lm_head else 'NOT FOUND'}")

if embed_tokens is None or lm_head is None:
    sys.exit("[qwen3_5] Missing embed_tokens or lm_head — cannot chunk decoder")


class DecoderChunkWrapper(torch.nn.Module):
    def __init__(self, layer_list, chunk_idx, num_chunks,
                 embed_tokens=None, final_norm=None, lm_head=None):
        super().__init__()
        self.chunk_idx = chunk_idx
        self.is_first = (chunk_idx == 0)
        self.is_last = (chunk_idx == num_chunks - 1)
        self.layers = torch.nn.ModuleList(layer_list)
        if self.is_first and embed_tokens is not None:
            self.embed_tokens = embed_tokens
        if self.is_last:
            if final_norm is not None:
                self.final_norm = final_norm
            if lm_head is not None:
                self.lm_head = lm_head

    def forward(self, x, attention_mask, position_ids):
        if self.is_first:
            x = self.embed_tokens(x)
        for layer in self.layers:
            out = layer(x, attention_mask=attention_mask,
                        position_ids=position_ids, use_cache=False)
            x = out[0] if isinstance(out, tuple) else out
        if self.is_last:
            if hasattr(self, "final_norm"):
                x = self.final_norm(x)
            if hasattr(self, "lm_head"):
                x = self.lm_head(x)
        return x


# ── Steps 4-5: ONNX export — vision + projection ───────────────────────────────────
ONNX_VISION     = os.path.join(OUTPUT_DIR, "qwen3_5_9b_vision_encoder.onnx")
ONNX_PROJECTION = os.path.join(OUTPUT_DIR, "qwen3_5_9b_projection.onnx")

if not SKIP_VISION:
    print(f"[4/11] Exporting vision encoder → {ONNX_VISION} ...")
    vis_cfg = getattr(config, "vision_config", None)
    patch_size = getattr(vis_cfg, "patch_size", 14) if vis_cfg else 14
    temporal_patch = getattr(vis_cfg, "temporal_patch_size", 2) if vis_cfg else 2
    spatial_merge = getattr(vis_cfg, "spatial_merge_size", 2) if vis_cfg else 2
    in_channels = getattr(vis_cfg, "in_channels", 3) if vis_cfg else 3
    num_t, num_h, num_w = 1, 16, 16
    num_patches = num_t * num_h * num_w
    channel_dim = in_channels * temporal_patch * patch_size * patch_size
    vision_input = torch.zeros((num_patches, channel_dim), dtype=torch.float16)
    grid_thw = torch.tensor([[num_t, num_h, num_w]], dtype=torch.int64)
    print(f"      vision input: patches={num_patches}, channel_dim={channel_dim}, grid_thw={grid_thw.tolist()}")
    vision_wrapped = VisionEncoderWrapper(vision_module).eval()
    with torch.no_grad():
        torch.onnx.export(vision_wrapped, (vision_input, grid_thw), ONNX_VISION,
            input_names=["pixel_values", "grid_thw"], output_names=["vision_embeds"],
            opset_version=17, do_constant_folding=True, dynamo=False)
    print(f"      {os.path.getsize(ONNX_VISION)//1024//1024} MB")

    print(f"[5/11] Exporting projection → {ONNX_PROJECTION} ...")
    with torch.no_grad():
        sample = vision_wrapped(vision_input, grid_thw)
    proj_wrapped = ProjectionWrapper(projection_module).eval()
    with torch.no_grad():
        torch.onnx.export(proj_wrapped, (torch.zeros_like(sample),), ONNX_PROJECTION,
            input_names=["vision_embeds"], output_names=["lang_embeds"],
            opset_version=17, do_constant_folding=True, dynamo=False)
    print(f"      {os.path.getsize(ONNX_PROJECTION)//1024//1024} MB")


# ── Step 6: ONNX export — chunked language decoder ───────────────────────────────
print(f"[6/11] Exporting language decoder in {NUM_CHUNKS} chunks ...")
import numpy as np

amask = torch.ones((1, MAX_SEQ_LEN), dtype=torch.int64)
pos   = torch.arange(MAX_SEQ_LEN, dtype=torch.int64).unsqueeze(0)

onnx_decoder_chunks = []
for ci in range(NUM_CHUNKS):
    start = ci * chunk_size
    end = min(start + chunk_size, total_layers)
    layer_list = [all_layers[j] for j in range(start, end)]

    wrapper = DecoderChunkWrapper(
        layer_list, ci, NUM_CHUNKS,
        embed_tokens=embed_tokens if ci == 0 else None,
        final_norm=final_norm if ci == NUM_CHUNKS - 1 else None,
        lm_head=lm_head if ci == NUM_CHUNKS - 1 else None,
    ).eval()

    onnx_path = os.path.join(OUTPUT_DIR, f"qwen3_5_9b_decoder_chunk_{ci}.onnx")

    if ci == 0:
        dummy_in = torch.zeros((1, MAX_SEQ_LEN), dtype=torch.int64)
        in_names = ["input_ids", "attention_mask", "position_ids"]
    else:
        dummy_in = torch.zeros((1, MAX_SEQ_LEN, HIDDEN), dtype=torch.float16)
        in_names = ["hidden_states", "attention_mask", "position_ids"]

    if ci == NUM_CHUNKS - 1:
        out_names = ["logits"]
    else:
        out_names = ["hidden_states"]

    print(f"      chunk {ci}/{NUM_CHUNKS}: layers {start}-{end-1}  "
          f"({'embed→' if ci == 0 else ''}{end-start} layers"
          f"{'→norm→lm_head' if ci == NUM_CHUNKS - 1 else ''})")

    with torch.no_grad():
        torch.onnx.export(wrapper, (dummy_in, amask, pos), onnx_path,
            input_names=in_names, output_names=out_names,
            opset_version=17, do_constant_folding=True,
            dynamic_axes=None, dynamo=False)
    sz = os.path.getsize(onnx_path) // 1024 // 1024
    print(f"      → {onnx_path} ({sz} MB)")

    if ci == 0:
        assert_no_trig(onnx_path, f"decoder_chunk_{ci}")

    onnx_decoder_chunks.append(onnx_path)


# ── Step 7: PTQ calibration data ──────────────────────────────────────────────────
print(f"[7/11] Building calibration set ({CALIB_TOKENS} tokens) ...")
CALIB_CHUNK = min(MAX_SEQ_LEN, 1024)
calib_ids = []
try:
    from datasets import load_dataset
    ds = load_dataset("wikitext", CALIB_DATASET, split="train", streaming=True)
    buf = []
    for row in ds:
        t = row.get("text", "").strip()
        if not t: continue
        buf.extend(tokenizer(t, add_special_tokens=False)["input_ids"])
        if len(buf) >= CALIB_TOKENS: break
    for start in range(0, min(len(buf), CALIB_TOKENS) - CALIB_CHUNK, CALIB_CHUNK):
        calib_ids.append(buf[start:start + CALIB_CHUNK])
except Exception as e:
    print(f"      [warn] dataset failed ({e}); using synthetic tokens")
    rng = np.random.default_rng(0)
    for _ in range(max(1, CALIB_TOKENS // CALIB_CHUNK)):
        calib_ids.append(rng.integers(0, VOCAB, size=CALIB_CHUNK).tolist())

calib_input_ids = []
calib_attention_mask = []
calib_position_ids = []
for ids_row in calib_ids[:32]:
    sl = min(len(ids_row), MAX_SEQ_LEN)
    calib_input_ids.append((ids_row[:MAX_SEQ_LEN] + [0] * (MAX_SEQ_LEN - sl))[:MAX_SEQ_LEN])
    calib_attention_mask.append([1] * sl + [0] * (MAX_SEQ_LEN - sl))
    calib_position_ids.append(list(range(MAX_SEQ_LEN)))
chunk0_calib = dict(
    input_ids=np.array(calib_input_ids, dtype=np.int64),
    attention_mask=np.array(calib_attention_mask, dtype=np.int64),
    position_ids=np.array(calib_position_ids, dtype=np.int64),
)
print(f"      {len(calib_input_ids)} rows × {MAX_SEQ_LEN} tokens (chunk 0 calibration)")


# ── Step 8: QAI Hub compile ────────────────────────────────────────────────────────
print(f"[8/11] Submitting QAI Hub compile jobs ({2 + NUM_CHUNKS} total) ...")
jobs = {}

if not SKIP_VISION:
    print("      → vision encoder ...")
    jobs["vision"] = submit_qai_hub_compile(ONNX_VISION, "qwen3_5_9b_vision_compile")
    print("      → projection ...")
    jobs["projection"] = submit_qai_hub_compile(ONNX_PROJECTION, "qwen3_5_9b_projection_compile")

for ci in range(NUM_CHUNKS):
    label = f"decoder_chunk_{ci}"
    print(f"      → {label} ...")
    calib = chunk0_calib if ci == 0 else None
    jobs[label] = submit_qai_hub_compile(
        onnx_decoder_chunks[ci], f"qwen3_5_9b_{label}_compile",
        calibration_data=calib)


# ── Steps 9-10: download + publish ────────────────────────────────────────────────
print("[9/11] Downloading compiled artifacts ...")
if not SKIP_VISION:
    download_target_model(jobs["vision"], OUT_VISION)
    verify_dsp_placement(jobs["vision"], "vision")
    download_target_model(jobs["projection"], OUT_PROJECTION)
    verify_dsp_placement(jobs["projection"], "projection")
for ci in range(NUM_CHUNKS):
    label = f"decoder_chunk_{ci}"
    download_target_model(jobs[label], decoder_chunk_out(ci))
    verify_dsp_placement(jobs[label], label)

if PUBLISH_HF:
    print(f"[10/11] Publishing → {HF_OUTPUT_REPO} ...")
    api = HfApi(token=HF_TOKEN)
    api.create_repo(HF_OUTPUT_REPO, repo_type="model", exist_ok=True, private=True)
    to_upload = []
    for ci in range(NUM_CHUNKS):
        to_upload.append((decoder_chunk_out(ci), f"qwen3_5_9b_decoder_chunk_{ci}.bin"))
    if not SKIP_VISION:
        to_upload += [(OUT_VISION, "qwen3_5_9b_vision_encoder.bin"),
                      (OUT_PROJECTION, "qwen3_5_9b_projection.bin")]
    job_ids_str = ", ".join(f"{k}={v.job_id}" for k, v in jobs.items())
    for local_path, repo_path in to_upload:
        api.upload_file(path_or_fileobj=local_path, path_in_repo=repo_path,
            repo_id=HF_OUTPUT_REPO,
            commit_message=f"Add {repo_path} ({MODEL_ID}, jobs: {job_ids_str})")
        print(f"      → https://huggingface.co/{HF_OUTPUT_REPO}/blob/main/{repo_path}")
else:
    print("[10/11] PUBLISH_HF!=1 — skipping upload")


# ── Step 11: summary ──────────────────────────────────────────────────────────────
print()
print("=" * 64)
print("Artifacts ready:")
if not SKIP_VISION:
    print(f"  {OUT_VISION}")
    print(f"  {OUT_PROJECTION}")
for ci in range(NUM_CHUNKS):
    print(f"  {decoder_chunk_out(ci)}")
if PUBLISH_HF:
    print(f"\nhf download {HF_OUTPUT_REPO} --local-dir ~/Downloads")
    print(f"adb push ~/Downloads/*.bin /storage/emulated/0/Download/")
print()
print("Re-download this build:")
print(f"  SKIP_EXPORT=1 \\")
if not SKIP_VISION:
    print(f"  JOB_ID_VISION={jobs.get('vision', type('', (), {'job_id': 'N/A'})()).job_id} \\")
    print(f"  JOB_ID_PROJECTION={jobs.get('projection', type('', (), {'job_id': 'N/A'})()).job_id} \\")
for ci in range(NUM_CHUNKS):
    label = f"decoder_chunk_{ci}"
    print(f"  JOB_ID_DECODER_{ci}={jobs[label].job_id} \\")
print(f"  hf jobs uv run --flavor cpu-xl ... compile_qwen3_5_9b.py")
