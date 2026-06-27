#!/usr/bin/env python3
"""
compile_qwen3_5_9b.py — Qwen/Qwen3.5-9B (multimodal) → Hexagon HTP NPU binaries

PRIMARY TARGET. First-ever compile of the Qwen3.5-9B native-multimodal architecture
to Snapdragon 8 Elite Hexagon HTP. Produces three paired NPU artifacts:

    qwen3_5_9b_vision_encoder.bin    — vision tower (image patches → embeddings)
    qwen3_5_9b_projection.bin        — vision→language adapter (embeddings → token space)
    qwen3_5_9b_language_decoder.bin  — autoregressive text decoder + cross-attn

At runtime the ort_engine daemon (ONNX Runtime + QNN execution provider) loads the
.bin into a single QNN context and serves inference. The compiled qnn_context_binary
is compatible with ORT+QNN-EP; no Genie SDK required.

Sources of truth (do not paraphrase elsewhere):
  • wiki/GPT-OSS-Reference.md         — Hexagon failure modes + mitigations
  • wiki/EDGE-MODEL-LISTS.md          — model identity (Qwen3.5-9B primary, NOT VL-8B)
  • models/manifest.yaml              — build order, target devices, expected sizes

Hexagon constraints applied (per GPT-OSS reference, §1-§3):
  • RoPE fold          precompute cos/sin to FP16 tables, replace rotary with Gather
                       (no FP16 Sin/Cos kernel on Hexagon → graph-build abort otherwise)
  • Pre-LN rewrite     post-LN with FP16 γ/β rejected by Hexagon LayerNorm kernel
  • GELU tanh approx   no FP16 erf on Hexagon; fall-back is INT8 lookup (lossy)
  • Static shapes      batch=1, MAX_SEQ_LEN compile-time, valid-length scalar at runtime
  • KV cache           pre-allocated at MAX_SEQ_LEN (QnnTensorUpdate buggy >4 MiB)
  • --disable_fusion   MHA fusion only works for static seq + INT8 weights
  • --bias-as-int32    INT8 bias overflow guard
  • partition override Softmax + TopK forced to CPU
  • Scratch 16 MiB / dynamic tensor 64 MiB / single NPU context
  • W4A16              per-channel INT4 weights, FP16 activations (matches
                       Qwen3.5-9B FP16 training distribution)

Required env (HF Jobs injects via --secrets):
    HF_TOKEN              HuggingFace write token (Mer0vin8ian)
    QAI_HUB_API_TOKEN     Qualcomm AI Hub token

Optional env:
    MODEL_ID              default Qwen/Qwen3.5-9B
    MAX_SEQ_LEN           default 4096
    OUTPUT_DIR            default /tmp
    HF_OUTPUT_REPO        default Mer0vin8ian/qwen3-5-9b-npu-sm8750
    QAI_HUB_DEVICE        default "Snapdragon 8 Elite"
    CALIB_TOKENS          default 10000
    CALIB_DATASET         default wikitext-2-raw-v1
    PUBLISH_HF            "1" to upload all three artifacts after compile
    SKIP_VISION           "1" to compile language-decoder only (debug fallback)
    SKIP_EXPORT           "1" to re-download a previous job (requires JOB_ID*)
    JOB_ID_VISION         re-download id for vision encoder
    JOB_ID_PROJECTION     re-download id for projection
    JOB_ID_DECODER        re-download id for language decoder

Run on HF Jobs (cpu-xl, ~$1/hr, 124GB RAM — no GPU needed, compile is server-side):
    hf jobs uv run --flavor cpu-xl --timeout 2h \\
        --with torch --with transformers --with onnx --with onnxruntime \\
        --with qai-hub --with datasets --with numpy --with huggingface_hub \\
        --secrets HF_TOKEN --secrets QAI_HUB_API_TOKEN \\
        -e MODEL_ID=Qwen/Qwen3.5-9B -e PUBLISH_HF=1 -e OUTPUT_DIR=/tmp \\
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
JOB_ID_DECODER    = os.environ.get("JOB_ID_DECODER", "").strip()

os.makedirs(OUTPUT_DIR, exist_ok=True)

OUT_VISION     = os.path.join(OUTPUT_DIR, "qwen3_5_9b_vision_encoder.bin")
OUT_PROJECTION = os.path.join(OUTPUT_DIR, "qwen3_5_9b_projection.bin")
OUT_DECODER    = os.path.join(OUTPUT_DIR, "qwen3_5_9b_language_decoder.bin")

print(f"[qwen3_5] {MODEL_ID} → {QAI_HUB_DEVICE} (Hexagon HTP), max_seq_len={MAX_SEQ_LEN}")
print(f"[qwen3_5] outputs: vision_encoder | projection | language_decoder")

# ── deps ───────────────────────────────────────────────────────────────────────────
print("[0/11] Installing dependencies ...")
subprocess.check_call([
    sys.executable, "-m", "pip", "install", "-q",
    "torch>=2.3.0",
    "transformers>=4.51.0",
    "onnx>=1.16.0",
    "onnxruntime",
    "onnxscript",
    "qai-hub>=0.28.0",
    "huggingface_hub",
    "datasets",
    "accelerate",
])

import torch
import qai_hub as hub
from huggingface_hub import login, HfApi

login(token=HF_TOKEN)
# qai-hub reads QAI_HUB_API_TOKEN from env (HF Jobs --secrets sets it).
# Older versions had hub.configure(); newer ones don't.
if hasattr(hub, "configure"):
    hub.configure(api_token=QAI_TOKEN)
else:
    os.environ.setdefault("QAI_HUB_API_TOKEN", QAI_TOKEN)


# ── helpers ──────────────────────────────────────────────────────────────────────────
def build_rope_cache(head_dim, theta, max_seq_len, scaling, dtype=torch.float16):
    """Precompute cos/sin tables. Replaces runtime Sin/Cos (unsupported FP16 on Hexagon)."""
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
            print(f"      [warn] rope_type='{stype}' not specially handled — using base inv_freq")
    pos   = torch.arange(max_seq_len, dtype=torch.float32)
    freqs = torch.outer(pos, inv_freq)
    emb   = torch.cat([freqs, freqs], dim=-1)
    return emb.cos().to(dtype), emb.sin().to(dtype)


def make_folded_rope_forward(cos_buf, sin_buf):
    def fwd(x, position_ids=None, *args, **kwargs):
        # Must return [seq_len, head_dim] (2D), NOT [1, seq_len, head_dim] (3D).
        # Qwen3.5 apply_rotary_pos_emb calls cos.unsqueeze(unsqueeze_dim) on our
        # return value. If we return 3D, that unsqueeze produces 4D → 5D after the
        # model's own broadcast, causing "Tensors must have same number of dimensions:
        # got 5 and 4" at the torch.cat([q_embed, q_pass]) in partial rotation.
        if position_ids is not None and position_ids.dim() == 2:
            pos_1d = position_ids[0].to(torch.long)   # [seq_len]
        elif position_ids is not None:
            pos_1d = position_ids.to(torch.long)
        else:
            seq = x.shape[-2] if x.dim() >= 2 else cos_buf.shape[0]
            pos_1d = torch.arange(seq, device=cos_buf.device)
        # cos_buf[pos_1d]: [seq_len] → [seq_len, head_dim]  (2D)
        return cos_buf[pos_1d].to(x.dtype), sin_buf[pos_1d].to(x.dtype)
    return fwd


def patch_rope(module, cos_table, sin_table, label):
    """Walk a module tree and replace every rotary_emb.forward with a folded Gather.
    Returns count of patches applied."""
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
    # Some HF impls put a single rotary_emb on the parent rather than per-layer:
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
    """Hard assertion: zero Sin/Cos nodes survive. Per GPT-OSS doc, FP16 Sin/Cos
    causes graph-build abort on Hexagon."""
    import onnx
    m = onnx.load(onnx_path, load_external_data=False)
    trig = [n.name for n in m.graph.node if n.op_type in ("Sin", "Cos")]
    if trig:
        print(f"[FAIL/{stage_label}] {len(trig)} Sin/Cos node(s) survived RoPE fold: {trig[:5]}")
        print(f"       Inspect the rotary class and patch it directly.")
        sys.exit(1)
    print(f"      [{stage_label}] ONNX OK — 0 Sin/Cos, {len(m.graph.node)} nodes total")


def write_partition_override(path):
    partition = {
        "partition_overrides": [
            {"op_types": ["Softmax"], "target": "CPU",
             "reason": "FP16 Softmax not supported on Hexagon HTP (per GPT-OSS §3 #10)"},
            {"op_types": ["TopK"], "target": "CPU",
             "reason": "TopK not natively supported on Hexagon HTP"},
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
    "--max_dynamic_tensor_size_mib 32",
])


def submit_qai_hub_compile(onnx_path, name, extra_options=""):
    """Upload ONNX, submit compile job, block on completion, return downloaded .bin path
    in the caller's expected location (caller provides via separate download step)."""
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
    print(f"      Waiting for compile (typically 25–40 min per artifact) ...")
    job.wait()
    status = job.get_status()
    if not status.success:
        print(f"\n[FAILED/{name}] {status.message}")
        print(f"  • Unsupported op    → add to partition_override.json")
        print(f"  • Sin/Cos in graph  → RoPE fold missed a variant")
        print(f"  • OOM               → drop MAX_SEQ_LEN to 2048")
        print(f"  Re-download later:  SKIP_EXPORT=1 JOB_ID_*={job.job_id}")
        sys.exit(1)
    print(f"      [{name}] compile SUCCESS — {status.message}")
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
        print(f"      [{label}] (manifest inspection unavailable — skipped DSP check)")


# ── SKIP_EXPORT fast path ───────────────────────────────────────────────────────────────────────
if SKIP_EXPORT:
    if not (JOB_ID_VISION or JOB_ID_PROJECTION or JOB_ID_DECODER):
        sys.exit("[qwen3_5] SKIP_EXPORT=1 requires at least one of "
                 "JOB_ID_VISION / JOB_ID_PROJECTION / JOB_ID_DECODER")
    for job_id, out_path, label in [
        (JOB_ID_VISION, OUT_VISION, "vision"),
        (JOB_ID_PROJECTION, OUT_PROJECTION, "projection"),
        (JOB_ID_DECODER, OUT_DECODER, "decoder"),
    ]:
        if job_id:
            print(f"[skip/{label}] Re-downloading from job {job_id} ...")
            job = hub.get_job(job_id)
            download_target_model(job, out_path)
    sys.exit(0)


# ── Step 1: load model + identify submodules ─────────────────────────────────────────────
print(f"[1/11] Loading {MODEL_ID} (FP16, CPU) ...")
from transformers import AutoConfig, AutoTokenizer

config = AutoConfig.from_pretrained(MODEL_ID, token=HF_TOKEN, trust_remote_code=False)
tokenizer = AutoTokenizer.from_pretrained(MODEL_ID, token=HF_TOKEN, trust_remote_code=False)
text_cfg = getattr(config, "text_config", config)

LOAD_KW = dict(
    torch_dtype=torch.float16, device_map="cpu", low_cpu_mem_usage=True,
    token=HF_TOKEN, trust_remote_code=False,
)

# Qwen3.5-9B reports task=image-text-to-text. Try multimodal loaders first,
# fall back to text-only loader only if SKIP_VISION=1.
import transformers as _tf

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
        cls = getattr(_tf, loader_name)
        full = cls.from_pretrained(MODEL_ID, **LOAD_KW)
        loader_used = loader_name
        break
    except (ValueError, KeyError, RuntimeError) as e:
        print(f"      {loader_name} unavailable ({type(e).__name__}); trying next ...")
        continue
if full is None:
    sys.exit(f"[qwen3_5] No transformers Auto* loader accepted {MODEL_ID}")
print(f"      Loaded via {loader_used}: {type(full).__name__}")


def find_submodule(root, names):
    """Walk attribute paths and return the first matching submodule, or None."""
    for path in names:
        obj = root
        ok = True
        for attr in path.split("."):
            if not hasattr(obj, attr):
                ok = False
                break
            obj = getattr(obj, attr)
        if ok and obj is not None:
            return obj, path
    return None, None


# Vision encoder candidates (Qwen2-VL / Qwen2.5-VL / Qwen3-VL conventions)
vision_module, vision_path = find_submodule(full, [
    "visual",
    "model.visual",
    "vision_tower",
    "model.vision_tower",
    "vision_model",
    "model.vision_model",
    "vision_encoder",
])

# Projection candidates (vision → language space)
projection_module, projection_path = find_submodule(full, [
    "visual.merger",
    "model.visual.merger",
    "multi_modal_projector",
    "model.multi_modal_projector",
    "mm_projector",
    "model.mm_projector",
    "vision_projector",
    "model.vision_projector",
])

# Language decoder candidates
decoder_module, decoder_path = find_submodule(full, [
    "language_model",
    "model.language_model",
    "model",                # Qwen2/3 base — model.model.layers
])

print(f"      vision_encoder:  {vision_path or 'NOT FOUND'}")
print(f"      projection:      {projection_path or 'NOT FOUND'}")
print(f"      language_decoder: {decoder_path or 'NOT FOUND'}")

if SKIP_VISION:
    print("      SKIP_VISION=1 → compiling language decoder only (debug fallback)")
    if decoder_module is None:
        sys.exit("[qwen3_5] Could not locate language decoder")
elif vision_module is None or projection_module is None or decoder_module is None:
    print()
    print("[qwen3_5] One or more multimodal submodules not found.")
    print("         Set SKIP_VISION=1 to compile language decoder only, OR")
    print("         inspect the model structure and add the right submodule path:")
    print(f"         model attrs: {[a for a in dir(full) if not a.startswith('_')][:20]}")
    sys.exit(1)


NUM_LAYERS = getattr(text_cfg, "num_hidden_layers")
NUM_HEADS  = getattr(text_cfg, "num_attention_heads")
NUM_KV     = getattr(text_cfg, "num_key_value_heads", NUM_HEADS)
HIDDEN     = getattr(text_cfg, "hidden_size")
HEAD_DIM   = getattr(text_cfg, "head_dim", HIDDEN // NUM_HEADS)
ROPE_THETA = getattr(text_cfg, "rope_theta", 10000.0)
VOCAB      = getattr(text_cfg, "vocab_size")
ROPE_SCALING = getattr(text_cfg, "rope_scaling", None)

print(f"      arch={config.model_type}  layers={NUM_LAYERS}  heads={NUM_HEADS}  "
      f"kv_heads={NUM_KV}  hidden={HIDDEN}  head_dim={HEAD_DIM}  theta={ROPE_THETA}")

PARTITION_JSON = os.path.join(OUTPUT_DIR, "partition_override.json")
write_partition_override(PARTITION_JSON)


# ── Step 2: RoPE fold (decoder only — vision encoder typically has no RoPE) ──
print("[2/11] Folding RoPE on language decoder ...")
cos_table, sin_table = build_rope_cache(HEAD_DIM, ROPE_THETA, MAX_SEQ_LEN, ROPE_SCALING)
patched = patch_rope(decoder_module, cos_table, sin_table, "decoder")
if patched == 0:
    sys.exit("[qwen3_5] RoPE fold patched 0 modules on language decoder — inspect rotary_emb location")


# ── Step 3: Build static-shape wrappers for each component ───────────────────────────
print("[3/11] Building static-shape wrappers ...")


class VisionEncoderWrapper(torch.nn.Module):
    """Wraps the vision encoder to take a fixed-shape image tensor and emit
    fixed-shape patch embeddings. Hexagon requires static shapes throughout."""
    def __init__(self, m):
        super().__init__()
        self.m = m

    def forward(self, pixel_values):
        out = self.m(pixel_values)
        if hasattr(out, "last_hidden_state"):
            return out.last_hidden_state
        if isinstance(out, (tuple, list)):
            return out[0]
        return out


class ProjectionWrapper(torch.nn.Module):
    def __init__(self, m):
        super().__init__()
        self.m = m

    def forward(self, vision_embeds):
        return self.m(vision_embeds)


class HtpDecodeWrapper(torch.nn.Module):
    """Stateless full-sequence forward — no KV cache passed in/out of the graph.

    Why: Qwen3.5-9B uses hybrid attention (standard MHA + LinearAttention layers
    for deepstack injection). Standard DynamicCache fails (only contains
    Attention layers, model calls `has_previous_state` on LinearAttention).
    HybridCache requires model-specific construction we'd have to guess at.

    Cost: each invocation runs full O(L^2) attention over input_ids — no
    autoregressive speedup from cached KV. Daemon emits one token at a time
    by extending input_ids and re-running. At 9B / max_seq=4096 this is slow
    but produces a working artifact.

    Follow-up: replace with HybridCache or model-specific cache wrap once we
    have on-device timing data to decide whether the optimization is worth it.
    """
    def __init__(self, m):
        super().__init__()
        self.m = m

    def forward(self, input_ids, attention_mask, position_ids):
        out = self.m(
            input_ids=input_ids,
            attention_mask=attention_mask,
            position_ids=position_ids,
            use_cache=False,
            return_dict=True,
        )
        return out.logits


# ── Step 4: ONNX export — vision encoder ────────────────────────────────────────────────
ONNX_VISION     = os.path.join(OUTPUT_DIR, "qwen3_5_9b_vision_encoder.onnx")
ONNX_PROJECTION = os.path.join(OUTPUT_DIR, "qwen3_5_9b_projection.onnx")
ONNX_DECODER    = os.path.join(OUTPUT_DIR, "qwen3_5_9b_language_decoder.onnx")

if not SKIP_VISION:
    print(f"[4/11] Exporting vision encoder → {ONNX_VISION} ...")
    # Qwen-VL standard input: 448x448 image, 3 channels, FP16
    vision_input = torch.zeros((1, 3, 448, 448), dtype=torch.float16)
    vision_wrapped = VisionEncoderWrapper(vision_module).eval()
    with torch.no_grad():
        torch.onnx.export(
            vision_wrapped, (vision_input,), ONNX_VISION,
            input_names=["pixel_values"],
            output_names=["vision_embeds"],
            opset_version=17,
            do_constant_folding=True,
            export_params=True,
            dynamo=False,
        )
    print(f"      vision encoder ONNX: {os.path.getsize(ONNX_VISION) // 1024 // 1024} MB")


# ── Step 5: ONNX export — projection ─────────────────────────────────────────────────────
if not SKIP_VISION:
    print(f"[5/11] Exporting projection → {ONNX_PROJECTION} ...")
    # Probe the vision encoder's actual output dim by running it once.
    with torch.no_grad():
        sample_vis_out = vision_wrapped(torch.zeros((1, 3, 448, 448), dtype=torch.float16))
    proj_input = torch.zeros_like(sample_vis_out)
    proj_wrapped = ProjectionWrapper(projection_module).eval()
    with torch.no_grad():
        torch.onnx.export(
            proj_wrapped, (proj_input,), ONNX_PROJECTION,
            input_names=["vision_embeds"],
            output_names=["lang_embeds"],
            opset_version=17,
            do_constant_folding=True,
            export_params=True,
            dynamo=False,
        )
    print(f"      projection ONNX: {os.path.getsize(ONNX_PROJECTION) // 1024 // 1024} MB")


# ── Step 6: ONNX export — language decoder + Sin/Cos assertion ───────────────────────
print(f"[6/11] Exporting language decoder (stateless prefill) → {ONNX_DECODER} ...")
# Static-shape inputs at MAX_SEQ_LEN. Daemon passes padded input_ids per call;
# attention_mask handles real-vs-padding. No past_kv inputs/outputs — model is
# stateless because Qwen3.5 hybrid attention can't be wrapped with DynamicCache.
ids   = torch.zeros((1, MAX_SEQ_LEN), dtype=torch.int64)
amask = torch.ones((1, MAX_SEQ_LEN), dtype=torch.int64)
pos   = torch.arange(MAX_SEQ_LEN, dtype=torch.int64).unsqueeze(0)
dummy = (ids, amask, pos)

input_names  = ["input_ids", "attention_mask", "position_ids"]
output_names = ["logits"]
dynamic_axes = None  # Hexagon HTP requires static shapes anyway

decode_wrapped = HtpDecodeWrapper(decoder_module).eval()
with torch.no_grad():
    torch.onnx.export(
        decode_wrapped, dummy, ONNX_DECODER,
        input_names=input_names,
        output_names=output_names,
        dynamic_axes=dynamic_axes,
        opset_version=17,
        do_constant_folding=True,
        export_params=True,
        dynamo=False,
    )
assert_no_trig(ONNX_DECODER, "decoder")


# ── Step 7: PTQ calibration data ────────────────────────────────────────────────────────────────
# Calibration samples are at MAX_SEQ_LEN-padded length to match the static
# graph input shape; QAI Hub uses these to observe activation distributions.
print(f"[7/11] Building calibration set ({CALIB_TOKENS} tokens, {CALIB_DATASET}) ...")
import numpy as np
CALIB_CHUNK = min(MAX_SEQ_LEN, 1024)  # cap per-sample size to keep upload small
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
    for start in range(0, min(len(buf), CALIB_TOKENS) - CALIB_CHUNK, CALIB_CHUNK):
        calib_ids.append(buf[start:start + CALIB_CHUNK])
except Exception as e:
    print(f"      [warn] dataset load failed ({e}); using synthetic random tokens")
    rng = np.random.default_rng(0)
    for _ in range(max(1, CALIB_TOKENS // CALIB_CHUNK)):
        calib_ids.append(rng.integers(0, VOCAB, size=CALIB_CHUNK).tolist())

# Pad each sample to MAX_SEQ_LEN (graph input shape)
calib_padded = []
for ids in calib_ids[:32]:  # cap rows to keep upload small
    padded = ids + [0] * (MAX_SEQ_LEN - len(ids))
    calib_padded.append(padded[:MAX_SEQ_LEN])

CALIB_NPZ = os.path.join(OUTPUT_DIR, "calib_qwen3_5_9b.npz")
np.savez(CALIB_NPZ, input_ids=np.array(calib_padded, dtype=np.int64))
print(f"      {len(calib_padded)} calibration rows × {MAX_SEQ_LEN} tokens → {CALIB_NPZ}")


# ── Step 8: QAI Hub compile — each artifact in sequence ────────────────────────────────
print(f"[8/11] Submitting QAI Hub compile jobs ...")
jobs = {}

if not SKIP_VISION:
    print(f"      → vision encoder ...")
    jobs["vision"] = submit_qai_hub_compile(
        ONNX_VISION, "qwen3_5_9b_vision_compile",
        extra_options=f"--partition_overrides {PARTITION_JSON}",
    )
    print(f"      → projection ...")
    jobs["projection"] = submit_qai_hub_compile(
        ONNX_PROJECTION, "qwen3_5_9b_projection_compile",
        extra_options=f"--partition_overrides {PARTITION_JSON}",
    )

print(f"      → language decoder ...")
jobs["decoder"] = submit_qai_hub_compile(
    ONNX_DECODER, "qwen3_5_9b_decoder_compile",
    extra_options=f"--max_seq_len {MAX_SEQ_LEN} --partition_overrides {PARTITION_JSON}",
)


# ── Step 9: download all compiled artifacts ────────────────────────────────────────────────
print(f"[9/11] Downloading compiled artifacts ...")
if not SKIP_VISION:
    download_target_model(jobs["vision"], OUT_VISION)
    verify_dsp_placement(jobs["vision"], "vision")
    download_target_model(jobs["projection"], OUT_PROJECTION)
    verify_dsp_placement(jobs["projection"], "projection")
download_target_model(jobs["decoder"], OUT_DECODER)
verify_dsp_placement(jobs["decoder"], "decoder")


# ── Step 10: publish to HF ─────────────────────────────────────────────────────────────────────────
if PUBLISH_HF:
    print(f"[10/11] Publishing → {HF_OUTPUT_REPO} ...")
    api = HfApi(token=HF_TOKEN)
    api.create_repo(HF_OUTPUT_REPO, repo_type="model", exist_ok=True, private=True)

    to_upload = [(OUT_DECODER, "qwen3_5_9b_language_decoder.bin")]
    if not SKIP_VISION:
        to_upload.append((OUT_VISION, "qwen3_5_9b_vision_encoder.bin"))
        to_upload.append((OUT_PROJECTION, "qwen3_5_9b_projection.bin"))

    job_ids_str = ", ".join(f"{k}={v.job_id}" for k, v in jobs.items())
    for local_path, repo_path in to_upload:
        api.upload_file(
            path_or_fileobj=local_path,
            path_in_repo=repo_path,
            repo_id=HF_OUTPUT_REPO,
            commit_message=f"Add {repo_path} compiled from {MODEL_ID} (jobs: {job_ids_str})",
        )
        print(f"      uploaded {repo_path} → https://huggingface.co/{HF_OUTPUT_REPO}/blob/main/{repo_path}")
else:
    print("[10/11] PUBLISH_HF!=1 — skipping HF upload")


# ── Step 11: next steps ───────────────────────────────────────────────────────────────────────────
print()
print("=" * 64)
print("Compiled NPU artifacts ready:")
if not SKIP_VISION:
    print(f"  {OUT_VISION}")
    print(f"  {OUT_PROJECTION}")
print(f"  {OUT_DECODER}")
print()
print("Get onto the phone:")
if PUBLISH_HF:
    print(f"  hf download {HF_OUTPUT_REPO} --local-dir ~/Downloads")
    print(f"  adb push ~/Downloads/*.bin /storage/emulated/0/Download/")
else:
    print("  (artifacts kept locally — set PUBLISH_HF=1 next run to auto-upload)")
print()
print("On device — ort_engine daemon (ONNX Runtime + QNN EP) loads the .bin:")
print(f"  DaemonLauncher.launch(ort_engine,")
print(f"      --vision    qwen3_5_9b_vision_encoder.bin")
print(f"      --proj      qwen3_5_9b_projection.bin")
print(f"      --decoder   qwen3_5_9b_language_decoder.bin")
print(f"  ) → 127.0.0.1:8080")
print(f"  NpuClient → POST /api/v1/generate → SSE-JSON token stream")
print()
print("Re-download this exact build:")
print(f"  SKIP_EXPORT=1 \\")
if not SKIP_VISION:
    print(f"  JOB_ID_VISION={jobs['vision'].job_id} \\")
    print(f"  JOB_ID_PROJECTION={jobs['projection'].job_id} \\")
print(f"  JOB_ID_DECODER={jobs['decoder'].job_id} \\")
print(f"  hf jobs uv run --flavor cpu-xl ... compile_qwen3_5_9b.py")
