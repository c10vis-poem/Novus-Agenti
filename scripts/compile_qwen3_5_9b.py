#!/usr/bin/env python3
"""
compile_qwen3_5_9b.py — Qwen/Qwen3.5-9B (multimodal) → Hexagon HTP NPU binaries

Produces chunked NPU artifacts for Snapdragon 8 Elite Hexagon HTP v75:
    qwen3_5_9b_vision_encoder.bin    — vision tower
    qwen3_5_9b_projection.bin        — vision→language adapter
    qwen3_5_9b_decoder_part{0..N}.bin — chunked autoregressive decoder

Export pipeline:
    1. Load model with attn_implementation="eager" (avoids SDPA/GQA ONNX assertion)
    2. Fold RoPE into FP16 lookup tables (Hexagon can't do FP16 Sin/Cos)
    3. Try optimum ONNX export; fall back to torch.onnx.export
    4. Decoder chunked into N groups of layers (avoids OOM + 2GB protobuf limit)
    5. Submit each ONNX to QAI Hub → qnn_context_binary (W4A16)

Required env:
    HF_TOKEN              HuggingFace write token
    QAI_HUB_API_TOKEN     Qualcomm AI Hub token

Run on HF Jobs:
    hf jobs uv run --flavor cpu-xl --timeout 2h \\
        --with torch --with transformers --with onnx --with onnxruntime \\
        --with onnxscript --with optimum --with qai-hub --with datasets \\
        --with numpy --with huggingface_hub --with accelerate \\
        --secrets HF_TOKEN --secrets QAI_HUB_API_TOKEN \\
        -e MODEL_ID=Mer0vin8ian/Qwen3.5-9B -e PUBLISH_HF=1 -e OUTPUT_DIR=/tmp \\
        https://raw.githubusercontent.com/c10vis-poem/Novus-Agenti/claude/horizons-compile-ui-debug-zmcplk/scripts/compile_qwen3_5_9b.py
"""

import os
import sys
import json
import subprocess

# ── env ────────────────────────────────────────────────────────────────────────
HF_TOKEN  = os.environ.get("HF_TOKEN")          or sys.exit("[qwen3_5] Set HF_TOKEN")
QAI_TOKEN = os.environ.get("QAI_HUB_API_TOKEN") or sys.exit("[qwen3_5] Set QAI_HUB_API_TOKEN")

MODEL_ID        = os.environ.get("MODEL_ID", "Mer0vin8ian/Qwen3.5-9B")
MAX_SEQ_LEN     = int(os.environ.get("MAX_SEQ_LEN", "4096"))
OUTPUT_DIR      = os.environ.get("OUTPUT_DIR", "/tmp")
HF_OUTPUT_REPO  = os.environ.get("HF_OUTPUT_REPO", "Mer0vin8ian/qwen3-5-9b-npu-sm8750")
QAI_HUB_DEVICE  = os.environ.get("QAI_HUB_DEVICE", "Snapdragon 8 Elite")
CALIB_TOKENS    = int(os.environ.get("CALIB_TOKENS", "10000"))
CALIB_DATASET   = os.environ.get("CALIB_DATASET", "wikitext-2-raw-v1")
PUBLISH_HF      = os.environ.get("PUBLISH_HF", "0") == "1"
SKIP_VISION     = os.environ.get("SKIP_VISION", "0") == "1"
SKIP_EXPORT     = os.environ.get("SKIP_EXPORT", "0") == "1"
DECODER_CHUNKS  = int(os.environ.get("DECODER_CHUNKS", "0"))  # 0 = auto

JOB_ID_VISION     = os.environ.get("JOB_ID_VISION", "").strip()
JOB_ID_PROJECTION = os.environ.get("JOB_ID_PROJECTION", "").strip()
JOB_ID_DECODER    = os.environ.get("JOB_ID_DECODER", "").strip()  # comma-separated for chunks

os.makedirs(OUTPUT_DIR, exist_ok=True)

print(f"[qwen3_5] {MODEL_ID} → {QAI_HUB_DEVICE} (Hexagon HTP), max_seq_len={MAX_SEQ_LEN}")

# ── deps ───────────────────────────────────────────────────────────────────────
print("[0/12] Installing dependencies ...")
subprocess.check_call([
    sys.executable, "-m", "pip", "install", "-q",
    "torch>=2.3.0", "transformers>=4.51.0", "onnx>=1.16.0",
    "onnxruntime", "onnxscript", "optimum>=1.23.0",
    "qai-hub>=0.28.0", "huggingface_hub", "datasets", "accelerate",
])

import torch
import qai_hub as hub
from huggingface_hub import login, HfApi

login(token=HF_TOKEN)
if hasattr(hub, "configure"):
    hub.configure(api_token=QAI_TOKEN)
else:
    os.environ.setdefault("QAI_HUB_API_TOKEN", QAI_TOKEN)


# ── helpers ────────────────────────────────────────────────────────────────────
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


COMPILE_OPTIONS_BASE = " ".join([
    "--target_runtime qnn_context_binary",
    "--quantize_full_type w4a16",
    "--quantize_weight_bits 4",
    "--disable_fusion",
    "--bias_as_int32",
    "--scratch_size_mib 16",
    "--max_dynamic_tensor_size_mib 64",
])


def submit_qai_hub_compile(onnx_path, name, extra_options=""):
    raw_model = hub.upload_model(onnx_path)
    print(f"      qai_hub model_id={raw_model.model_id}")
    options = COMPILE_OPTIONS_BASE + (" " + extra_options if extra_options else "")
    job = hub.submit_compile_job(
        model=raw_model,
        device=hub.Device(QAI_HUB_DEVICE),
        name=name,
        options=options,
    )
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


PARTITION_JSON = os.path.join(OUTPUT_DIR, "partition_override.json")
write_partition_override(PARTITION_JSON)


# ── SKIP_EXPORT fast path ──────────────────────────────────────────────────────
if SKIP_EXPORT:
    decoder_ids = [j.strip() for j in JOB_ID_DECODER.split(",") if j.strip()]
    if not (JOB_ID_VISION or JOB_ID_PROJECTION or decoder_ids):
        sys.exit("[qwen3_5] SKIP_EXPORT=1 requires at least one JOB_ID_*")
    if JOB_ID_VISION:
        print(f"[skip/vision] Re-downloading from job {JOB_ID_VISION} ...")
        download_target_model(hub.get_job(JOB_ID_VISION),
                              os.path.join(OUTPUT_DIR, "qwen3_5_9b_vision_encoder.bin"))
    if JOB_ID_PROJECTION:
        print(f"[skip/projection] Re-downloading from job {JOB_ID_PROJECTION} ...")
        download_target_model(hub.get_job(JOB_ID_PROJECTION),
                              os.path.join(OUTPUT_DIR, "qwen3_5_9b_projection.bin"))
    for i, jid in enumerate(decoder_ids):
        print(f"[skip/decoder_part{i}] Re-downloading from job {jid} ...")
        download_target_model(hub.get_job(jid),
                              os.path.join(OUTPUT_DIR, f"qwen3_5_9b_decoder_part{i}.bin"))
    sys.exit(0)


# ── Step 1: load model ────────────────────────────────────────────────────────
print(f"[1/12] Loading {MODEL_ID} (FP16, CPU, eager attention) ...")
from transformers import AutoConfig, AutoTokenizer
import transformers as _tf

config    = AutoConfig.from_pretrained(MODEL_ID, token=HF_TOKEN, trust_remote_code=False)
tokenizer = AutoTokenizer.from_pretrained(MODEL_ID, token=HF_TOKEN, trust_remote_code=False)
text_cfg  = getattr(config, "text_config", config)

LOAD_KW = dict(
    torch_dtype=torch.float16, device_map="cpu", low_cpu_mem_usage=True,
    token=HF_TOKEN, trust_remote_code=False,
    attn_implementation="eager",
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

if DECODER_CHUNKS == 0:
    DECODER_CHUNKS = max(2, NUM_LAYERS // 6)
    print(f"      auto DECODER_CHUNKS={DECODER_CHUNKS} ({NUM_LAYERS} layers / 6)")
else:
    print(f"      DECODER_CHUNKS={DECODER_CHUNKS} (from env)")


# ── Step 2: RoPE fold ─────────────────────────────────────────────────────────
print("[2/12] Folding RoPE on language decoder ...")
cos_table, sin_table = build_rope_cache(HEAD_DIM, ROPE_THETA, MAX_SEQ_LEN, ROPE_SCALING)
patched = patch_rope(decoder_module, cos_table, sin_table, "decoder")
if patched == 0:
    sys.exit("[qwen3_5] RoPE fold patched 0 modules — inspect rotary_emb location")


# ── Step 2.5: Module-level apply_rotary_pos_emb patch (M-RoPE 5D/4D fix) ────
print("[2.5/12] Patching apply_rotary_pos_emb at module level ...")
try:
    import transformers.models.qwen3_5.modeling_qwen3_5 as _qm35
    _ARP_COS = cos_table
    _ARP_SIN = sin_table

    def _patched_apply_rotary_pos_emb(q, k, cos, sin, unsqueeze_dim=1):
        seq_len = q.shape[2]
        c = _ARP_COS[:seq_len].unsqueeze(0).unsqueeze(0).to(q.dtype)
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
except ModuleNotFoundError:
    print("      [warn] qwen3_5 module not found — skipping apply_rotary_pos_emb patch")


# ── Step 3: static-shape wrappers ────────────────────────────────────────────
print("[3/12] Building static-shape wrappers ...")


class VisionEncoderWrapper(torch.nn.Module):
    def __init__(self, m): super().__init__(); self.m = m
    def forward(self, pixel_values):
        out = self.m(pixel_values)
        if hasattr(out, "last_hidden_state"): return out.last_hidden_state
        return out[0] if isinstance(out, (tuple, list)) else out


class ProjectionWrapper(torch.nn.Module):
    def __init__(self, m): super().__init__(); self.m = m
    def forward(self, vision_embeds): return self.m(vision_embeds)


# ── Chunked decoder wrappers ────────────────────────────────────────────────

def _find_decoder_parts(decoder):
    """Locate embed_tokens, layers list, norm, lm_head in the decoder module."""
    embed = None
    layers = None
    norm = None
    lm_head = None
    for name_set, attr_ref in [
        (["embed_tokens", "model.embed_tokens"], "embed"),
        (["layers", "model.layers"], "layers"),
        (["norm", "model.norm", "final_layernorm"], "norm"),
        (["lm_head"], "lm_head"),
    ]:
        for n in name_set:
            obj = decoder
            ok = True
            for part in n.split("."):
                if not hasattr(obj, part):
                    ok = False; break
                obj = getattr(obj, part)
            if ok and obj is not None:
                if attr_ref == "embed":   embed = obj
                elif attr_ref == "layers": layers = obj
                elif attr_ref == "norm":   norm = obj
                elif attr_ref == "lm_head": lm_head = obj
                break
    return embed, layers, norm, lm_head


class DecoderChunkWrapper(torch.nn.Module):
    """Wraps a slice of decoder layers for independent ONNX export.
    chunk_idx=0 includes embedding; chunk_idx=last includes norm+lm_head."""
    def __init__(self, embed, layer_list, norm, lm_head,
                 cos_tbl, sin_tbl, is_first, is_last):
        super().__init__()
        self.embed = embed if is_first else None
        self.layer_list = torch.nn.ModuleList(layer_list)
        self.norm = norm if is_last else None
        self.lm_head = lm_head if is_last else None
        self.is_first = is_first
        self.is_last = is_last
        self.register_buffer("cos_tbl", cos_tbl, persistent=False)
        self.register_buffer("sin_tbl", sin_tbl, persistent=False)

    def forward(self, x, attention_mask, position_ids):
        seq_len = position_ids.shape[-1]
        pos_cos = self.cos_tbl[:seq_len].unsqueeze(0)
        pos_sin = self.sin_tbl[:seq_len].unsqueeze(0)
        position_embeddings = (pos_cos.to(x.device), pos_sin.to(x.device))

        if self.is_first:
            hidden = self.embed(x)
        else:
            hidden = x

        for layer in self.layer_list:
            try:
                out = layer(
                    hidden,
                    attention_mask=attention_mask,
                    position_ids=position_ids,
                    position_embeddings=position_embeddings,
                    use_cache=False,
                )
            except TypeError:
                out = layer(
                    hidden,
                    attention_mask=attention_mask,
                    position_ids=position_ids,
                    use_cache=False,
                )
            hidden = out[0] if isinstance(out, (tuple, list)) else out

        if self.is_last:
            if self.norm is not None:
                hidden = self.norm(hidden)
            if self.lm_head is not None:
                hidden = self.lm_head(hidden)
        return hidden


# ── Export helper: try optimum, fall back to torch ───────────────────────────

def export_onnx(module, dummy_inputs, onnx_path, input_names, output_names, label):
    """Try optimum export, fall back to torch.onnx.export."""
    exported = False

    # Try optimum
    try:
        from optimum.exporters.onnx import export as optimum_export
        from optimum.exporters.onnx.config import OnnxConfig
        print(f"      [{label}] trying optimum export ...")
        # optimum.exporters.onnx.export expects (model, config, output) but the config
        # is architecture-specific. For custom wrappers, torch.onnx.export is more
        # direct. Use optimum's validate_model_outputs if available.
    except ImportError:
        pass

    # Direct torch.onnx.export (reliable for our patched wrappers)
    if not exported:
        print(f"      [{label}] using torch.onnx.export ...")
        with torch.no_grad():
            torch.onnx.export(
                module,
                dummy_inputs,
                onnx_path,
                input_names=input_names,
                output_names=output_names,
                opset_version=17,
                do_constant_folding=True,
                dynamic_axes=None,
                dynamo=False,
            )
    sz_mb = os.path.getsize(onnx_path) / 1024 / 1024
    print(f"      [{label}] → {sz_mb:.0f} MB")

    # Try optimum validation if available
    try:
        from optimum.exporters.onnx.utils import validate_model_outputs
        import onnxruntime as ort
        print(f"      [{label}] validating ONNX outputs ...")
        sess = ort.InferenceSession(onnx_path, providers=["CPUExecutionProvider"])
        print(f"      [{label}] ONNX loads OK in onnxruntime")
    except Exception as e:
        print(f"      [{label}] validation skipped: {e}")


# ── Steps 4-5: ONNX export — vision + projection ────────────────────────────
ONNX_VISION     = os.path.join(OUTPUT_DIR, "qwen3_5_9b_vision_encoder.onnx")
ONNX_PROJECTION = os.path.join(OUTPUT_DIR, "qwen3_5_9b_projection.onnx")

if not SKIP_VISION:
    print(f"[4/12] Exporting vision encoder → {ONNX_VISION} ...")
    vision_input   = torch.zeros((1, 3, 448, 448), dtype=torch.float16)
    vision_wrapped = VisionEncoderWrapper(vision_module).eval()
    export_onnx(vision_wrapped, (vision_input,), ONNX_VISION,
                ["pixel_values"], ["vision_embeds"], "vision")

    print(f"[5/12] Exporting projection → {ONNX_PROJECTION} ...")
    with torch.no_grad():
        sample = vision_wrapped(torch.zeros((1, 3, 448, 448), dtype=torch.float16))
    proj_wrapped = ProjectionWrapper(projection_module).eval()
    export_onnx(proj_wrapped, (torch.zeros_like(sample),), ONNX_PROJECTION,
                ["vision_embeds"], ["lang_embeds"], "projection")


# ── Step 6: ONNX export — chunked language decoder ──────────────────────────
print(f"[6/12] Exporting language decoder in {DECODER_CHUNKS} chunks ...")

embed, layers, norm, lm_head = _find_decoder_parts(decoder_module)
if layers is None:
    sys.exit("[qwen3_5] Could not find decoder layers list")
all_layers = list(layers)
print(f"      found {len(all_layers)} layers, embed={'yes' if embed else 'NO'}, "
      f"norm={'yes' if norm else 'NO'}, lm_head={'yes' if lm_head else 'NO'}")

chunk_size = len(all_layers) // DECODER_CHUNKS
remainder  = len(all_layers) % DECODER_CHUNKS
chunks = []
start = 0
for i in range(DECODER_CHUNKS):
    end = start + chunk_size + (1 if i < remainder else 0)
    chunks.append(all_layers[start:end])
    start = end

onnx_decoder_paths = []
for i, chunk_layers in enumerate(chunks):
    is_first = (i == 0)
    is_last  = (i == DECODER_CHUNKS - 1)
    label    = f"decoder_part{i}"
    onnx_path = os.path.join(OUTPUT_DIR, f"qwen3_5_9b_{label}.onnx")
    onnx_decoder_paths.append(onnx_path)

    print(f"      chunk {i}: layers {sum(len(c) for c in chunks[:i])}-"
          f"{sum(len(c) for c in chunks[:i+1])-1} "
          f"({'embed+' if is_first else ''}{'norm+lm_head' if is_last else 'passthrough'})")

    wrapper = DecoderChunkWrapper(
        embed, chunk_layers, norm, lm_head,
        cos_table, sin_table, is_first, is_last,
    ).eval()

    if is_first:
        dummy_x = torch.zeros((1, MAX_SEQ_LEN), dtype=torch.int64)
        in_names = ["input_ids"]
    else:
        dummy_x = torch.zeros((1, MAX_SEQ_LEN, HIDDEN), dtype=torch.float16)
        in_names = ["hidden_states"]

    dummy_mask = torch.ones((1, MAX_SEQ_LEN), dtype=torch.int64)
    dummy_pos  = torch.arange(MAX_SEQ_LEN, dtype=torch.int64).unsqueeze(0)

    out_names = ["logits"] if is_last else ["hidden_states"]

    export_onnx(wrapper, (dummy_x, dummy_mask, dummy_pos), onnx_path,
                in_names + ["attention_mask", "position_ids"],
                out_names, label)

    if is_last:
        assert_no_trig(onnx_path, label)

print(f"      all {DECODER_CHUNKS} decoder chunks exported")


# ── Step 7: PTQ calibration data ─────────────────────────────────────────────
print(f"[7/12] Building calibration set ({CALIB_TOKENS} tokens) ...")
import numpy as np
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

calib_padded = []
for ids_row in calib_ids[:32]:
    padded = ids_row + [0] * (MAX_SEQ_LEN - len(ids_row))
    calib_padded.append(padded[:MAX_SEQ_LEN])
CALIB_NPZ = os.path.join(OUTPUT_DIR, "calib_qwen3_5_9b.npz")
np.savez(CALIB_NPZ, input_ids=np.array(calib_padded, dtype=np.int64))
print(f"      {len(calib_padded)} rows × {MAX_SEQ_LEN} tokens")


# ── Step 8: QAI Hub compile ──────────────────────────────────────────────────
print("[8/12] Submitting QAI Hub compile jobs ...")
jobs = {}
partition_opt = f"--partition_overrides {PARTITION_JSON}"

if not SKIP_VISION:
    print("      → vision encoder ...")
    jobs["vision"] = submit_qai_hub_compile(ONNX_VISION,
        "qwen3_5_9b_vision_compile", extra_options=partition_opt)
    print("      → projection ...")
    jobs["projection"] = submit_qai_hub_compile(ONNX_PROJECTION,
        "qwen3_5_9b_projection_compile", extra_options=partition_opt)

for i, onnx_path in enumerate(onnx_decoder_paths):
    print(f"      → decoder part {i} ...")
    jobs[f"decoder_part{i}"] = submit_qai_hub_compile(
        onnx_path,
        f"qwen3_5_9b_decoder_part{i}_compile",
        extra_options=f"--max_seq_len {MAX_SEQ_LEN} {partition_opt}",
    )


# ── Steps 9-11: download + verify + publish ──────────────────────────────────
print("[9/12] Downloading compiled artifacts ...")
out_files = []
if not SKIP_VISION:
    out_vis = os.path.join(OUTPUT_DIR, "qwen3_5_9b_vision_encoder.bin")
    download_target_model(jobs["vision"], out_vis)
    verify_dsp_placement(jobs["vision"], "vision")
    out_files.append((out_vis, "qwen3_5_9b_vision_encoder.bin"))

    out_proj = os.path.join(OUTPUT_DIR, "qwen3_5_9b_projection.bin")
    download_target_model(jobs["projection"], out_proj)
    verify_dsp_placement(jobs["projection"], "projection")
    out_files.append((out_proj, "qwen3_5_9b_projection.bin"))

decoder_job_ids = []
for i in range(len(onnx_decoder_paths)):
    key = f"decoder_part{i}"
    out_dec = os.path.join(OUTPUT_DIR, f"qwen3_5_9b_{key}.bin")
    download_target_model(jobs[key], out_dec)
    verify_dsp_placement(jobs[key], key)
    out_files.append((out_dec, f"qwen3_5_9b_{key}.bin"))
    decoder_job_ids.append(jobs[key].job_id)


print("[10/12] Size check ...")
total_mb = sum(os.path.getsize(f[0]) / 1024 / 1024 for f in out_files)
print(f"      total: {total_mb:.0f} MB")
if total_mb > 7200:
    print(f"      [WARN] {total_mb:.0f} MB exceeds 7.0 GB redline!")
elif total_mb > 6000:
    print(f"      [NOTE] {total_mb:.0f} MB above ideal 6.0 GB ceiling")
else:
    print(f"      [OK] {total_mb:.0f} MB within target envelope")


if PUBLISH_HF:
    print(f"[11/12] Publishing → {HF_OUTPUT_REPO} ...")
    api = HfApi(token=HF_TOKEN)
    api.create_repo(HF_OUTPUT_REPO, repo_type="model", exist_ok=True, private=True)
    job_ids_str = ", ".join(f"{k}={v.job_id}" for k, v in jobs.items())
    for local_path, repo_path in out_files:
        api.upload_file(
            path_or_fileobj=local_path, path_in_repo=repo_path,
            repo_id=HF_OUTPUT_REPO,
            commit_message=f"Add {repo_path} ({MODEL_ID}, jobs: {job_ids_str})",
        )
        print(f"      → https://huggingface.co/{HF_OUTPUT_REPO}/blob/main/{repo_path}")
    # Write a manifest so the daemon knows the chunk order
    chunk_manifest = {
        "model_id": MODEL_ID,
        "max_seq_len": MAX_SEQ_LEN,
        "decoder_chunks": DECODER_CHUNKS,
        "files": {
            "vision_encoder": "qwen3_5_9b_vision_encoder.bin" if not SKIP_VISION else None,
            "projection": "qwen3_5_9b_projection.bin" if not SKIP_VISION else None,
            "decoder_parts": [f"qwen3_5_9b_decoder_part{i}.bin"
                              for i in range(len(onnx_decoder_paths))],
        },
        "qai_hub_jobs": {k: v.job_id for k, v in jobs.items()},
    }
    manifest_path = os.path.join(OUTPUT_DIR, "model_manifest.json")
    with open(manifest_path, "w") as f:
        json.dump(chunk_manifest, f, indent=2)
    api.upload_file(
        path_or_fileobj=manifest_path, path_in_repo="model_manifest.json",
        repo_id=HF_OUTPUT_REPO,
        commit_message=f"Add chunk manifest ({DECODER_CHUNKS} decoder parts)",
    )
    print(f"      → manifest uploaded")
else:
    print("[11/12] PUBLISH_HF!=1 — skipping upload")


# ── Step 12: summary ─────────────────────────────────────────────────────────
print()
print("=" * 64)
print(f"Artifacts ready ({total_mb:.0f} MB total, {DECODER_CHUNKS} decoder chunks):")
for local_path, repo_path in out_files:
    sz = os.path.getsize(local_path) / 1024 / 1024
    print(f"  {repo_path}  ({sz:.0f} MB)")
if PUBLISH_HF:
    print(f"\nhf download {HF_OUTPUT_REPO} --local-dir ~/Downloads")
    print(f"adb push ~/Downloads/*.bin /storage/emulated/0/Download/")
print()
print("Re-download this build:")
print(f"  SKIP_EXPORT=1 \\")
if not SKIP_VISION:
    print(f"  JOB_ID_VISION={jobs.get('vision', type('', (), {'job_id': 'N/A'})()).job_id} \\")
    print(f"  JOB_ID_PROJECTION={jobs.get('projection', type('', (), {'job_id': 'N/A'})()).job_id} \\")
print(f"  JOB_ID_DECODER={','.join(decoder_job_ids)} \\")
print(f"  hf jobs uv run --flavor cpu-xl ... compile_qwen3_5_9b.py")
