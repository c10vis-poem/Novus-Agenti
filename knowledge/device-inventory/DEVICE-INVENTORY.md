# Device Inventory — Motorola Razr Ultra 2025 (point-in-time snapshot)

> **Snapshot date: 2026-07-13.** Produced by the operator's on-device audit
> session (openwiki chat · z-ai/glm-5.2, running in Termux) reading the real
> device directly — not something to re-derive or guess at. This is ground
> truth *as of that date*; re-verify on-device before relying on exact
> versions/sizes for anything time-sensitive. Recovered here (session 16)
> from the former `wiki/APP-SOTU-AUDIT.md`, which bundled this factual
> inventory together with a now-superseded status narrative — only the
> inventory itself is preserved; the narrative parts (what's done/broken,
> priority order) are stale or already captured in CLAUDE.md's Pending list.

Path: `/storage/emulated/0/Download/` on-device unless noted otherwise.

## SDKs

- **QAIRT v2.48.0** (2.2 GB) — downloaded, not extracted.
- **Hexagon SDK 6.6.0.0** (2.9 GB) — downloaded, not extracted.

## Runtime binaries / libraries

- **`qnn_llama_runner.zip`** (34 MB) — `libQnnHtp.so` + HTP skel/stub for
  v69/v73/v75/**v79** + `libQnnSystem.so` + `libqnn_executorch_backend.so`.
- **`geniex-bench-android-arm64 v0.3.14`** (86 MB) — prebuilt `geniex-bench`
  with dual backends: **llama.cpp** (ggml, HTP v68–v81 + CPU + OpenCL) and
  **QAIRT** (`libgeniex_core`, `libgeniex_vlm`, `libgeniex-proc`,
  `libgeniex-proc-vision`, HTP v79/v81).

## Models

- `Qwen_Qwen3.5-9B-Q4_0.gguf` (5.4 GB) — the main LLM, Q4_0 GGUF.
- `mtp-gemma-4-E2B-it-BF16.gguf` (163 MB).
- `hybrid_llama_qnn.pte` (888 MB) — ExecuTorch hybrid LLaMA for QNN NPU.
- `model.safetensors` (535 MB) — unidentified, possibly a VLM.
- `tokenizer.json` (11 MB), `download.bin` (3.6 MB).
- Qwen2.5-VL-7B compiled folders — empty, `sample_inputs` only, not a real
  compiled artifact.

## Prebuilt APKs (as of snapshot date)

- `app-arm64-v8a-release-signed.apk` (22 MB, built Jul 10)
- `app-release.apk` (2.6 MB, built Jul 12)

## Termux toolchain

- **Installed:** Python 3.14.6, Node 24.17, Deno 2.9, Rust 1.96.1,
  Clang/LLVM 21.1.8, CMake 4.4, Ninja 1.13, Make 4.4, ONNX Runtime (Python)
  1.27.1, NumPy, SciPy, soundfile, espeak, ffmpeg 8.1.2, adb 35.0.2.
- **Not installed:** JDK/Java, Gradle, Android SDK, QAI Hub CLI.

## Repos on device

- `~/repos/Novus-Agenti` (was at HEAD `ec0ac49` as of the snapshot — almost
  certainly behind the current branch tip by now; re-clone/pull before
  trusting this).
- `~/repos/openclaude` (OmniRoute proxy).
