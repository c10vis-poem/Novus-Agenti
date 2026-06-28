#!/usr/bin/env python3
"""
compile_qwen3_5_9b.py — Qwen/Qwen3.5-9B (multimodal) → Hexagon HTP NPU binary

PRIMARY TARGET. First-ever compile of the Qwen3.5-9B native-multimodal architecture
to Snapdragon 8 Elite Hexagon HTP. Produces a single unified NPU artifact:

    qwen3_5_9b_unified.bin  — full model (vision + decoder) as one qnn_context_binary

Qwen3.5 uses deepstack vision injection (vision_config.deepstack_visual_indexes),
embedding vision features directly into decoder layers — NOT a separate encoder
pipeline. The model is exported as one ONNX graph and QAI Hub handles partitioning
(matmuls → DSP/NPU, Softmax/TopK → CPU, convolutions → GPU/CPU).

At runtime the daemon (ORT+QNN-EP or Genie SDK) loads the single .bin into a QNN
context and serves inference.

SKIP_VISION=1 exports text-only (decoder submodule only) as a fallback.

Sources of truth (do not paraphrase elsewhere):
  - wiki/GPT-OSS-Reference.md         — Hexagon failure modes + mitigations
  - models/manifest.yaml              — build order, target devices, expected sizes

What WE handle (clean ONNX for QAI Hub):
  - RoPE fold          precompute cos/sin to FP16 tables (Hexagon has no FP16 Sin/Cos)
  - M-RoPE shape fix   bypass 5D/4D cat mismatch from interleaved M-RoPE
  - Static shapes      batch=1, MAX_SEQ_LEN compile-time (Hexagon requires static dims)
  - Stateless prefill  use_cache=False (Qwen3.5 hybrid attention rejects DynamicCache)

What QAI Hub handles (we just specify device + quantization):
  - Quantization (W4A16), fusion, bias scaling, partitioning, scratch/tensor sizing

Required env (HF Jobs injects via --secrets or -e):
    HF_TOKEN              HuggingFace write token (Mer0vin8ian)
    QAI_HUB_API_TOKEN     Qualcomm AI Hub token

Optional env:
    MODEL_ID              default Mer0vin8ian/Qwen3.5-9B
    MAX_SEQ_LEN           default 4096
    OUTPUT_DIR            default /tmp
    HF_OUTPUT_REPO        default Mer0vin8ian/qwen3-5-9b-npu-sm8750
    QAI_HUB_DEVICE        default "Snapdragon 8 Elite"
    PUBLISH_HF            "1" to upload artifact after compile
    SKIP_VISION           "1" to export decoder-only (text, no vision inputs)
    SKIP_EXPORT           "1" to re-download a previous job (requires JOB_ID)
    JOB_ID                re-download id for a previous compile

Run on HF Jobs (cpu-xl, ~$1/hr, 124GB RAM — no GPU needed, compile is server-side):
    hf jobs uv run --flavor cpu-xl --timeout 2h \\
        --with torch --with transformers --with onnx --with onnxruntime \\
        --with onnxscript --with qai-hub --with numpy \\
        --with huggingface_hub --with accelerate \\
        --secrets HF_TOKEN -e QAI_HUB_API_TOKEN=$QAI_HUB_API_TOKEN \\
        -e MODEL_ID=Mer0vin8ian/Qwen3.5-9B -e PUBLISH_HF=1 -e OUTPUT_DIR=/tmp \\
        https://raw.githubusercontent.com/c10vis-poem/Novus-Agenti/claude/session-8-closeout-hf-review-thl2gj/scripts/compile_qwen3_5_9b.py
"""

import os
import sys
import subprocess

# -- env -----------------------------------------------------------------------
HF_TOKEN  = os.environ.get("HF_TOKEN")          or sys.exit("[qwen3_5] Set HF_TOKEN")
QAI_TOKEN = os.environ.get("QAI_HUB_API_TOKEN") or sys.exit("[qwen3_5] Set QAI_HUB_API_TOKEN")

MODEL_ID       = os.environ.get("MODEL_ID", "Mer0vin8ian/Qwen3.5-9B")
MAX_SEQ_LEN    = int(os.environ.get("MAX_SEQ_LEN", "4096"))
OUTPUT_DIR     = os.environ.get("OUTPUT_DIR", "/tmp")
HF_OUTPUT_REPO = os.environ.get("HF_OUTPUT_REPO", "Mer0vin8ian/qwen3-5-9b-npu-sm8750")
QAI_HUB_DEVICE = os.environ.get("QAI_HUB_DEVICE", "Snapdragon 8 Elite")
PUBLISH_HF     = os.environ.get("PUBLISH_HF", "0") == "1"
SKIP_VISION    = os.environ.get("SKIP_VISION", "0") == "1"
SKIP_EXPORT    = os.environ.get("SKIP_EXPORT", "0") == "1"

JOB_ID = os.environ.get("JOB_ID", "").strip()

os.makedirs(OUTPUT_DIR, exist_ok=True)

OUT_BIN = os.path.join(OUTPUT_DIR, "qwen3_5_9b_unified.bin")

print(f"[qwen3_5] {MODEL_ID} -> {QAI_HUB_DEVICE} (Hexagon HTP), max_seq_len={MAX_SEQ_LEN}")
if SKIP_VISION:
    print("          SKIP_VISION=1 — text-only decoder export (no vision inputs)")

# -- deps ----------------------------------------------------------------------
print("[0/7] Installing dependencies ...")
subprocess.check_call([
    sys.executable, "-m", "pip", "install", "-q",
    "torch>=2.3.0", "transformers>=4.51.0", "onnx>=1.16.0",
    "onnxruntime", "onnxscript", "qai-hub>=0.28.0",
    "huggingface_hub", "accelerate",
])

import torch
import qai_hub as hub
from huggingface_hub import login, HfApi

login(token=HF_TOKEN)
if hasattr(hub, "configure"):
    hub.configure(api_token=QAI_TOKEN)
else:
    os.environ.setdefault("QAI_HUB_API_TOKEN", QAI_TOKEN)


# -- helpers -------------------------------------------------------------------
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


COMPILE_OPTIONS_BASE = " ".join([
    "--target_runtime qnn_context_binary",
    "--quantize_full_type w4a16",
    "--quantize_weight_bits 4",
    "--disable_fusion",
    "--bias_as_int32",
    "--scratch_size_mib 16",
    "--max_dynamic_tensor_size_mib 64",
])


def write_partition_overrides():
    """Softmax + TopK have no FP16 kernel on Hexagon HTP — force them to CPU.
    QAI Hub reads this JSON to override the automatic partitioner."""
    import json
    overrides = {
        "op_type_overrides": {
            "Softmax": {"target": "cpu"},
            "TopK": {"target": "cpu"},
        }
    }
    path = os.path.join(OUTPUT_DIR, "partition_override.json")
    with open(path, "w") as f:
        json.dump(overrides, f, indent=2)
    print(f"      partition overrides -> {path}")
    return path


def submit_qai_hub_compile(onnx_path, name, extra_options=""):
    partition_json = write_partition_overrides()
    raw_model = hub.upload_model(onnx_path)
    print(f"      qai_hub model_id={raw_model.model_id}")
    options = (COMPILE_OPTIONS_BASE +
               f" --partition_overrides {partition_json}" +
               (" " + extra_options if extra_options else ""))
    job = hub.submit_compile_job(
        model=raw_model,
        device=hub.Device(QAI_HUB_DEVICE),
        name=name,
        options=options,
    )
    print(f"      job_id={job.job_id}")
    print(f"      https://app.aihub.qualcomm.com/jobs/{job.job_id}/")
    print(f"      Waiting for compile (~25-40 min) ...")
    job.wait()
    status = job.get_status()
    if not status.success:
        print(f"\n[FAILED/{name}] {status.message}")
        print(f"  Re-download later: SKIP_EXPORT=1 JOB_ID={job.job_id}")
        sys.exit(1)
    print(f"      [{name}] compile SUCCESS")
    return job


def download_target_model(job, out_path):
    job.get_target_model().download(out_path)
    sz_mb = os.path.getsize(out_path) / 1024 / 1024
    print(f"      -> {out_path} ({sz_mb:.0f} MB)")
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


# -- SKIP_EXPORT fast path -----------------------------------------------------
if SKIP_EXPORT:
    if not JOB_ID:
        sys.exit("[qwen3_5] SKIP_EXPORT=1 requires JOB_ID")
    print(f"[skip] Re-downloading from job {JOB_ID} ...")
    job = hub.get_job(JOB_ID)
    download_target_model(job, OUT_BIN)
    sys.exit(0)


# -- Step 1: load model --------------------------------------------------------
print(f"[1/7] Loading {MODEL_ID} (FP16, CPU) ...")
from transformers import AutoConfig
import transformers as _tf

config   = AutoConfig.from_pretrained(MODEL_ID, token=HF_TOKEN, trust_remote_code=False)
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


# Qwen3_5ForConditionalGeneration structure (transformers >=5.x):
#   full.model          = Qwen3_5Model (vision + language wrapper)
#   full.model.visual   = vision encoder
#   full.model.language_model = Qwen3_5TextModel (decoder layers + embed + norm)
#   full.lm_head        = nn.Linear (hidden -> vocab logits)
#
# For text-only export we need language_model + lm_head.
# "model" alone is WRONG — it's the full multimodal wrapper with vision assertions.
decoder_module, decoder_path = find_submodule(full, [
    "model.language_model", "language_model"])

print(f"      decoder:    {decoder_path or 'NOT FOUND'} -> {type(decoder_module).__name__ if decoder_module else 'None'}")
print(f"      lm_head:    {hasattr(full, 'lm_head')} -> {type(getattr(full, 'lm_head', None)).__name__}")

if decoder_module is None:
    print("[qwen3_5] Could not find language_model submodule. Model structure:")
    for name, child in full.named_children():
        print(f"          full.{name} = {type(child).__name__}")
        for sub_name, sub_child in child.named_children():
            print(f"              .{sub_name} = {type(sub_child).__name__}")
    sys.exit("[qwen3_5] Could not locate language decoder submodule")

lm_head = getattr(full, "lm_head", None)
if lm_head is None:
    sys.exit("[qwen3_5] Could not locate lm_head on top-level model")

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

vis_cfg = getattr(config, "vision_config", None)
if vis_cfg and not SKIP_VISION:
    PATCH_SIZE  = getattr(vis_cfg, "patch_size", 14)
    IMG_SIZE    = getattr(vis_cfg, "image_size", 448)
    TEMPORAL    = getattr(vis_cfg, "temporal_patch_size", 2)
    IN_CHANNELS = getattr(vis_cfg, "in_channels", 3)
    GRID_H = IMG_SIZE // PATCH_SIZE
    GRID_W = IMG_SIZE // PATCH_SIZE
    GRID_T = 1
    NUM_PATCHES = GRID_T * GRID_H * GRID_W
    PATCH_DIM   = TEMPORAL * PATCH_SIZE * PATCH_SIZE * IN_CHANNELS
    print(f"      vision: img={IMG_SIZE} patch={PATCH_SIZE} "
          f"grid={GRID_T}x{GRID_H}x{GRID_W} ({NUM_PATCHES} patches, dim={PATCH_DIM})")
elif not SKIP_VISION:
    print("[qwen3_5] No vision_config found and SKIP_VISION!=1")
    print("          Set SKIP_VISION=1 for text-only, or check model config")
    sys.exit(1)

# -- Step 2: RoPE fold ---------------------------------------------------------
print("[2/7] Folding RoPE on language decoder ...")
cos_table, sin_table = build_rope_cache(HEAD_DIM, ROPE_THETA, MAX_SEQ_LEN, ROPE_SCALING)
patched = patch_rope(decoder_module, cos_table, sin_table, "decoder")
if patched == 0:
    sys.exit("[qwen3_5] RoPE fold patched 0 modules — inspect rotary_emb location")


# -- Step 2.5: Module-level apply_rotary_pos_emb patch (M-RoPE 5D/4D fix) ------
print("[2.5/7] Patching apply_rotary_pos_emb at module level ...")
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
print(f"      apply_rotary_pos_emb -> precomputed [1,1,S,{HEAD_DIM}] FP16 Gather")


# -- Step 3: static-shape wrapper ----------------------------------------------
print("[3/7] Building static-shape wrapper ...")


class UnifiedWrapper(torch.nn.Module):
    """Full model — vision + decoder composed via direct sub-module calls.
    Bypasses Qwen3_5Model.forward() which contains untraceable dynamic ops
    (.tolist(), torch._check, itertools.groupby in get_rope_index).
    Instead: visual → embed → masked_scatter → language_model → lm_head."""
    def __init__(self, full_model, image_token_id):
        super().__init__()
        self.visual = full_model.model.visual
        self.embed_tokens = full_model.model.language_model.embed_tokens
        self.language_model = full_model.model.language_model
        self.lm_head = full_model.lm_head
        self.register_buffer("_image_token_id",
                             torch.tensor(image_token_id, dtype=torch.int64))

    def forward(self, input_ids, attention_mask, position_ids,
                pixel_values, image_grid_thw):
        pixel_values = pixel_values.to(self.visual.dtype)
        vis_out = self.visual(pixel_values, grid_thw=image_grid_thw)
        image_embeds = vis_out.pooler_output

        inputs_embeds = self.embed_tokens(input_ids)
        image_embeds = image_embeds.to(inputs_embeds.dtype)

        image_mask = (input_ids == self._image_token_id).unsqueeze(-1)
        inputs_embeds = inputs_embeds.masked_scatter(image_mask, image_embeds)

        out = self.language_model(
            input_ids=None,
            inputs_embeds=inputs_embeds,
            position_ids=position_ids,
            attention_mask=attention_mask,
            use_cache=False,
        )
        hidden = out[0]
        return self.lm_head(hidden)


class TextOnlyWrapper(torch.nn.Module):
    """Decoder-only — text inputs, no vision. For SKIP_VISION=1.
    Wraps Qwen3_5TextModel (returns last_hidden_state) + lm_head (projects to logits)."""
    def __init__(self, language_model, lm_head):
        super().__init__()
        self.language_model = language_model
        self.lm_head = lm_head

    def forward(self, input_ids, attention_mask, position_ids):
        out = self.language_model(
            input_ids=input_ids,
            attention_mask=attention_mask,
            position_ids=position_ids,
            use_cache=False,
        )
        hidden = out[0] if not hasattr(out, "last_hidden_state") else out.last_hidden_state
        return self.lm_head(hidden)


# -- Step 4: ONNX export -------------------------------------------------------
ONNX_MODEL = os.path.join(OUTPUT_DIR, "qwen3_5_9b_unified.onnx")

ids   = torch.zeros((1, MAX_SEQ_LEN), dtype=torch.int64)
amask = torch.ones((1, MAX_SEQ_LEN),  dtype=torch.int64)
# Pre-expand to 4D [4, B, S] to avoid tracer branches on ndim in Qwen3_5TextModel.
# Channels: [text, temporal, height, width] — all identical for text-only input.
pos_1d = torch.arange(MAX_SEQ_LEN, dtype=torch.int64)
pos   = pos_1d.view(1, 1, -1).expand(4, 1, -1).contiguous()

if SKIP_VISION:
    print(f"[4/7] Exporting text-only decoder -> {ONNX_MODEL} ...")
    wrapped = TextOnlyWrapper(decoder_module, lm_head).eval()
    dummy_inputs = (ids, amask, pos)
    input_names  = ["input_ids", "attention_mask", "position_ids"]
else:
    print(f"[4/7] Exporting unified model (text + vision) -> {ONNX_MODEL} ...")
    spatial_merge = getattr(vis_cfg, "spatial_merge_size", 2)
    n_vis_tokens = (GRID_T * GRID_H * GRID_W) // (spatial_merge ** 2)
    image_token_id = getattr(config, "image_token_id", 248056)
    pixel_values   = torch.zeros((NUM_PATCHES, PATCH_DIM), dtype=torch.float16)
    image_grid_thw = torch.tensor([[GRID_T, GRID_H, GRID_W]], dtype=torch.int64)
    ids[:, :n_vis_tokens] = image_token_id
    print(f"      pixel_values: {list(pixel_values.shape)}, "
          f"image_grid_thw: {list(image_grid_thw.shape)} = [{GRID_T},{GRID_H},{GRID_W}]")
    print(f"      n_vis_tokens={n_vis_tokens} (spatial_merge={spatial_merge}), "
          f"image_token_id={image_token_id}")
    wrapped = UnifiedWrapper(full, image_token_id).eval()
    dummy_inputs = (ids, amask, pos, pixel_values, image_grid_thw)
    input_names  = ["input_ids", "attention_mask", "position_ids",
                    "pixel_values", "image_grid_thw"]

import traceback
with torch.no_grad():
    try:
        torch.onnx.export(wrapped, dummy_inputs, ONNX_MODEL,
            input_names=input_names, output_names=["logits"],
            opset_version=17, do_constant_folding=True,
            dynamic_axes=None, dynamo=False)
    except Exception as e:
        print(f"\n[FAIL] ONNX export failed: {type(e).__name__}: {e}")
        traceback.print_exc()
        sys.exit(1)

assert_no_trig(ONNX_MODEL, "unified")
onnx_size_mb = os.path.getsize(ONNX_MODEL) // 1024 // 1024
print(f"      ONNX size: {onnx_size_mb} MB")


# -- Step 5: QAI Hub compile ---------------------------------------------------
print("[5/7] Submitting QAI Hub compile job ...")

compile_name = "qwen3_5_9b_unified_compile"
job = submit_qai_hub_compile(ONNX_MODEL, compile_name,
    extra_options=f"--max_seq_len {MAX_SEQ_LEN}")


# -- Step 6: download ----------------------------------------------------------
print("[6/7] Downloading compiled artifact ...")
download_target_model(job, OUT_BIN)
verify_dsp_placement(job, "unified")


# -- Step 7: publish -----------------------------------------------------------
if PUBLISH_HF:
    print(f"[7/7] Publishing -> {HF_OUTPUT_REPO} ...")
    api = HfApi(token=HF_TOKEN)
    api.create_repo(HF_OUTPUT_REPO, repo_type="model", exist_ok=True, private=True)
    repo_filename = "qwen3_5_9b_unified.bin"
    api.upload_file(
        path_or_fileobj=OUT_BIN,
        path_in_repo=repo_filename,
        repo_id=HF_OUTPUT_REPO,
        commit_message=f"Add {repo_filename} ({MODEL_ID}, job: {job.job_id})",
    )
    print(f"      -> https://huggingface.co/{HF_OUTPUT_REPO}/blob/main/{repo_filename}")
else:
    print("[7/7] PUBLISH_HF!=1 — skipping upload")


# -- summary -------------------------------------------------------------------
print()
print("=" * 64)
print(f"Artifact ready: {OUT_BIN}")
sz = os.path.getsize(OUT_BIN) / 1024 / 1024
print(f"  Size: {sz:.0f} MB")
if PUBLISH_HF:
    print(f"\nhf download {HF_OUTPUT_REPO} --local-dir ~/Downloads")
    print(f"adb push ~/Downloads/*.bin /storage/emulated/0/Download/")
print()
print(f"Re-download this build:")
print(f"  SKIP_EXPORT=1 JOB_ID={job.job_id} \\")
print(f"  hf jobs uv run --flavor cpu-xl ... compile_qwen3_5_9b.py")
