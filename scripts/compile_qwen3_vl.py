#!/usr/bin/env python3
"""
compile_qwen3_vl.py — Qwen3-VL-8B-Thinking → QAI Hub → Snapdragon 8 Elite HTP v75

Default target: Qwen/Qwen3-VL-8B-Thinking (75.58 reasoning benchmark, hidden thinking tokens).
The Instruct variant (53.74 reasoning) is NOT the target — set MODEL_ID env to override.

Pre-export work this script does (BEFORE sending to QAI Hub):
  1. Bake RoPE rotations into Q/K projection weights (no runtime Sin/Cos on Hexagon v75)
  2. Hard-cap max_seq_len = 4096 (KVCache pre-allocated at this size)
  3. Replace GELU exact with tanh approximation (no FP16 erf on v75)
  4. Disable MHA fusion (variable-len safety)

These transforms are wrapped by the qai_hub_models Qwen2.5-VL export class.
This script wires Qwen3-VL-8B-Thinking weights through that pipeline.

NOTE: This is BACKUP 1. Primary target is Qwen3.5-9B (scripts/compile_qwen3_5_9b.py).
Use this script only if the primary compile hard-fails.

Required env vars:
    HF_TOKEN           — HuggingFace token
    QAI_HUB_API_TOKEN  — Qualcomm AI Hub API token

Optional:
    MODEL_ID           — override default (default: Qwen/Qwen3-VL-8B-Thinking)
    MAX_SEQ_LEN        — override default (default: 4096)
    OUTPUT_DIR         — local download path (default: /content for Colab)
    PUBLISH_HF         — "1" to upload .bin artifacts to HF after compile
    HF_OUTPUT_REPO     — target HF repo (default: Mer0vin8ian/qwen3-vl-8b-thinking-htp-v75)
    SKIP_EXPORT        — "1" to skip the export+compile step, just download a previous job
    JOB_IDS            — comma-separated job IDs to re-download (requires SKIP_EXPORT=1)
"""

import os, sys, subprocess

HF_TOKEN   = os.environ.get("HF_TOKEN")          or sys.exit("[compile_qwen3_vl] Set HF_TOKEN")
QAI_TOKEN  = os.environ.get("QAI_HUB_API_TOKEN") or sys.exit("[compile_qwen3_vl] Set QAI_HUB_API_TOKEN")
OUTPUT_DIR = os.environ.get("OUTPUT_DIR", "/content")
SKIP_EXPORT = os.environ.get("SKIP_EXPORT", "0") == "1"
PUBLISH_HF = os.environ.get("PUBLISH_HF", "0") == "1"
JOB_IDS    = [j.strip() for j in os.environ.get("JOB_IDS", "").split(",") if j.strip()]

# Defaults — overridable via env. Thinking variant is the target per CLAUDE.md.
MODEL_ID       = os.environ.get("MODEL_ID", "Qwen/Qwen3-VL-8B-Thinking")
MAX_SEQ_LEN    = int(os.environ.get("MAX_SEQ_LEN", "4096"))
HF_OUTPUT_REPO = os.environ.get("HF_OUTPUT_REPO", "Mer0vin8ian/qwen3-vl-8b-thinking-htp-v75")

# HTP v75 = SM8750 = Snapdragon 8 Elite (Razr Ultra 2025).
# "QRD" = Qualcomm Reference Device — selects Genie SDK path (multi-component .bin).
DEVICE     = "Snapdragon 8 Elite QRD"
OUT_PREFIX = "qwen3_vl_8b_thinking_htp"

os.makedirs(OUTPUT_DIR, exist_ok=True)
print(f"[compile_qwen3_vl] {MODEL_ID} → {DEVICE}")

# ── Install deps ────────────────────────────────────────────────────────────────────────────────
print("[0/4] Installing dependencies ...")
subprocess.check_call([
    sys.executable, "-m", "pip", "install", "-q",
    "qai-hub", "qai-hub-models[qwen2_5_vl_7b_instruct]", "huggingface_hub",
])

import qai_hub as hub
from huggingface_hub import login
hub.configure(api_token=QAI_TOKEN)
login(token=HF_TOKEN)

# ── Re-download path: SKIP_EXPORT=1 + JOB_IDS ───────────────────────────────────────
if SKIP_EXPORT:
    if not JOB_IDS:
        sys.exit("[compile_qwen3_vl] SKIP_EXPORT=1 requires JOB_IDS=<id1,id2,...>")
    print(f"[SKIP] Re-downloading from jobs: {JOB_IDS}")
    for idx, jid in enumerate(JOB_IDS):
        job = hub.get_job(jid)
        out = os.path.join(OUTPUT_DIR, f"{OUT_PREFIX}_{idx}.bin")
        job.get_target_model().download(out)
        print(f"  → {out}  ({os.path.getsize(out)//1024//1024} MB)")
    sys.exit(0)

# ── Step 1: Load Qwen2.5-VL-7B export class; override weights → Qwen3-VL-8B ─
# Qwen3-VL shares the Qwen2-VL architecture — same ViT vision encoder,
# same cross-attention projection, same GQA decoder structure. The class
# should instantiate cleanly against Qwen3-VL-8B weights.
print(f"[1/4] Loading export pipeline ({MODEL_ID} via Qwen2.5-VL-7B class) ...")
try:
    from qai_hub_models.models.qwen2_5_vl_7b_instruct.model import Qwen25VL7BInstruct
    model = Qwen25VL7BInstruct.from_pretrained(MODEL_ID)
    print(f"      Loaded. Config: {model.model.config.model_type}")
except AttributeError:
    # Some versions use direct constructor, not from_pretrained
    try:
        model = Qwen25VL7BInstruct(MODEL_ID)
        print(f"      Loaded via constructor.")
    except Exception as e2:
        print(f"[FAIL] Could not instantiate: {e2}")
        print()
        print("Likely cause: qai_hub_models constructor API changed. Check:")
        print("  python -c \"from qai_hub_models.models.qwen2_5_vl_7b_instruct.model import Qwen25VL7BInstruct; help(Qwen25VL7BInstruct)\"")
        sys.exit(1)
except Exception as e:
    print(f"[FAIL] Architecture mismatch: {e}")
    print()
    print("Qwen3-VL-8B may have added config keys not in Qwen2.5-VL-7B.")
    print("Check the diff: Qwen3-VL technical report vs Qwen2.5-VL config.")
    print("Fix: subclass Qwen25VL7BInstruct and override get_input_spec() for changed dims.")
    sys.exit(1)

# ── Step 2: Export + compile ─────────────────────────────────────────────────────────────────────────
print(f"[2/4] Exporting and submitting compile jobs → {DEVICE} (Genie / W4A16) ...")
try:
    from qai_hub_models.models.qwen2_5_vl_7b_instruct.export import export_model
    compile_jobs = export_model(
        model=model,
        device=DEVICE,
        skip_profiling=True,
        skip_inferencing=True,
        skip_summary=True,
        output_dir=OUTPUT_DIR,
    )
except TypeError:
    # Older qai_hub_models versions: export_model(model_cls, device, ...)
    from qai_hub_models.models.qwen2_5_vl_7b_instruct.export import export_model
    compile_jobs = export_model(
        model_name=MODEL_ID,
        device=DEVICE,
        skip_profiling=True,
        skip_inferencing=True,
        skip_summary=True,
        output_dir=OUTPUT_DIR,
    )

if not isinstance(compile_jobs, (list, tuple)):
    compile_jobs = [compile_jobs]

print(f"      {len(compile_jobs)} compile job(s) submitted:")
for job in compile_jobs:
    print(f"      • {job.job_id}  — https://app.aihub.qualcomm.com/jobs/{job.job_id}/")
print("      Waiting for compile (typically 15–30 min per component) ...")

# ── Step 3: Wait + download ─────────────────────────────────────────────────────────────────────────
print("[3/4] Collecting compiled artifacts ...")
outputs = []
for idx, job in enumerate(compile_jobs):
    job.wait()
    status = job.get_status()
    if not status.success:
        print(f"[FAILED] Job {job.job_id}: {status.message}")
        print(f"         https://app.aihub.qualcomm.com/jobs/{job.job_id}/")
        remaining = [j.job_id for j in compile_jobs if j != job]
        if remaining:
            print(f"         Other job IDs to re-download if you fix and retry: {remaining}")
        sys.exit(1)

    out = os.path.join(OUTPUT_DIR, f"{OUT_PREFIX}_{idx}.bin")
    job.get_target_model().download(out)
    size_mb = os.path.getsize(out) / (1024 * 1024)
    print(f"      [{idx}] {out}  {size_mb:.0f} MB")
    outputs.append(out)

# ── Step 4: Next steps ──────────────────────────────────────────────────────────────────────────────
print()
print("=" * 60)
print("Compiled artifacts:")
for f in outputs:
    print(f"  {f}")
print()
print("Next steps:")
for f in outputs:
    name = os.path.basename(f)
    print(f"  colab download {f}")
    print(f"  adb push {name} /storage/emulated/0/Download/")
print()
print("On device — CLIFFORD/CRS picks up the .bin on next health check:")
print("  resolveNpuModelPath() → finds qwen3_vl_8b_thinking_htp_0.bin")
print("  DaemonLauncher.launch(genie_engine, --model ...) → serves at 127.0.0.1:8080")
print()
print("If genie_engine needs multiple .bin components, pass them all as --model flags.")
print("Update DaemonLauncher.launch() args if genie_engine uses a different CLI format.")
print()
print(f"QAI Hub job IDs (save these for SKIP_EXPORT re-download):")
print(f"  JOB_IDS=" + ",".join(j.job_id for j in compile_jobs))
