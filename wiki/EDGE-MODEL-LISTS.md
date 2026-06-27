# **📱 On-Device Local Model Directory**

> **PRIMARY TARGET: `Qwen/Qwen3.5-9B`** (multimodal-native, 9.65B params, Apache 2.0).
> Confirmed by user 2026-06-27. Source-of-truth = `CLAUDE.md` + `models/manifest.yaml`.
> Models listed below as "primary visual logic" are now **backup-only**.

> **Provenance:** Imported verbatim from Google Drive "EDGE AND CLOUD MODELS / EDGE MODEL LISTS"
> (file_id `1e28xHaiTjaR4cb3OKQhdS72nIuaMAmmSBen2wLIGcx8`, last modified 2026-06-24).
> Header above amended in commit to reflect user override. Do not paraphrase this
> doc elsewhere; cite this file.

---

### **These are the models explicitly mapped to fit within your local system boundaries, hardware NPUs, and your \~6GB to 7.2GB file-size limits**

# **The Expert Visual Logic Tier (\~4.5GB – 5.5GB )** *(BACKUP TIER — Qwen3.5-9B is primary)*

1. # **Qwen3-VL-8B-Thinking (Qwen/Qwen3-VL-8B-Thinking)Local Format: Q4\_K\_M GGUF (\~5.2 GB)** *(BACKUP)*

### **Hardware Fit:  Jetson Orin Nano Super (via TensorRT-LLM container)**

### **Motorola Razr Ultra (via CPU/GPU offload).**

### **Why it's here:  Holds the highest MMMU reasoning benchmark score (69.6%).  It stops and generates hidden thinking tokens to map out layouts before it answers you..**

2. # **Qwen2.5-VL-7B (Qwen/Qwen2.5-VL-7B-Instruct)Local Format: Q4\_0 or FP8 (\~4.5 GB)** *(BACKUP)*

### **Hardware Fit: Fully supported natively by the Qualcomm AI Hub Workbench / QNN compiler for your Snapdragon 8 Elite NPU*.***

### ***Why it's here:                                                              Faster than the   Quen thinking version. A standard, stable transformer geometry that translates cleanly into static ONNX paths.***
