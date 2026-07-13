# NPU Runtime Paths — Novus Agenti / Omni Claw

> Canonical reference for every path from source model → running on Hexagon
> HTP v79 (SM8750), the Qualcomm SDK distribution model, and the Qualcomm
> documentation glossary. Supersedes any prior "GPT-OSS-Reference" pointer —
> that filename never existed in this repo; see `GPT-DAEMON-REFERENCE.md`
> for the distilled daemon/architecture notes instead.
> Verified against primary sources: Qualcomm SDK distribution mechanics,
> `ghcr.io/snapdragon-toolchain/arm64-android` (tag existence confirmed via
> the GHCR registry API), and QAI Hub's documented compile flow.
> Last updated: 2026-07-12 (session 15, branch `on-device-inference-openwiki-sae7cy`)

---

## Target Hardware

| Spec | Value |
|---|---|
| Device | Motorola Razr Ultra 2025 |
| SoC | Snapdragon 8 Elite (SM8750) |
| NPU | Hexagon HTP v79 |
| RAM | 16 GB LPDDR5X |
| Android | 15 (API 35) |

---

## Critical: the Hexagon/QNN SDK never runs on the phone

The single most common confusion in this project: **the Hexagon SDK and QNN
SDK are host-side cross-compilation toolchains, not something installed on
the device.** They run on an x86_64 Linux/Windows build machine (or CI),
produce ARM64 binaries and `.so` libraries, and only *those outputs* — never
the SDK itself — get copied to the phone.

Qualcomm distributes both SDKs through the Qualcomm Developer Network (QDN)
via Qualcomm Package Manager (QPM3), a desktop installer app. There is no
Android/Termux build of QPM3. Given this project's "phone only, no laptop"
constraint, the SDK cannot be installed by the user directly — the only
practical path is a CI job (GitHub Actions) that cross-compiles the daemon
binary and uploads it as an artifact, which the user then downloads to the
phone through the app's normal model-import flow like any other file.

A Docker image bundling the NDK + Hexagon toolchain exists and was verified
reachable this session via the GHCR registry API (anonymous pull token,
`tags/list` returned `v0.1` through `v0.7`):

```
ghcr.io/snapdragon-toolchain/arm64-android:v0.7
```

Its contents (whether it bundles QNN EP headers or just the raw NDK/Hexagon
toolchain) have not been inspected — confirm before wiring a CI job around
it.

| Component | Needs the SDK? | Runs where |
|---|---|---|
| Path 1 compile (QAI Hub) | No — compiles server-side | Cloud (QAI Hub) |
| Building `ort_engine` itself | Yes — QNN SDK headers/libs | Build host / CI only |
| Path 2 (`llama-server` + `GGML_HEXAGON`) | Yes — Hexagon SDK | Build host / CI only |
| Runtime on-device (any path) | No — just the compiled `.so`/binary | Phone |

---

## The Six Runtime Paths

Each model format has its own daemon binary. The app doesn't care which
format is loaded — it talks HTTP to `127.0.0.1:8080`. `CliffordService`
launches whatever binary is installed in `filesDir`.

### Path 0: GenieX on the HTP SDK (QAIRT) — PRIMARY, decided session 15

```
HF model (GGUF, e.g. Q4_0) or QAI Hub bundle → GenieX (`geniex serve`,
  OpenAI-compatible, 127.0.0.1:18181/v1) → HTP SDK / QAIRT (NPU-only) or
  GGML backend (NPU/GPU/CPU) → Hexagon HTP v79
```

`github.com/qualcomm/GenieX` — real, official Qualcomm repo (NOT the same as
QNN's `genie-t2t-run` "Genie"). Ships two backends: GGML/llama.cpp (runs
today via Q4_0 GGUF) and Qualcomm AI Engine Direct/QAIRT (NPU-only, max
perf — currently gated on BYOM-compiling the 9B via QAI Hub, since
Qualcomm's prebuilt library only ships smaller Qwen variants). See
`wiki/GENIEX-DAEMON-PLAN.md` for the full contract and open questions.
`ort_engine` (Path 1 below) is now the **legacy** runtime.

### Path 1: QNN Context Binary → `ort_engine` (legacy, Qwen3.5-9B)

```
HF model → ONNX export (RoPE fold, static shapes) → QAI Hub compile
  (server-side, W4A16) → .bin (qnn_context_binary) → ort_engine daemon
  (ORT + QNN EP) → Hexagon HTP v79
```

No local/CI SDK needed for the compile step — QAI Hub does it. `ort_engine`
itself still needs to be built once (CI, QNN SDK on the build host) before
it can load the resulting `.bin`. This daemon is already built and
CI-packaged (not scaffolding) — see the status table below.

### Path 2: GGUF → `llama-server` (llama.cpp + `GGML_HEXAGON`)

Needs the actual Hexagon SDK on the build host to cross-compile with
`-DGGML_HEXAGON=ON`. `GGML_HEXAGON_NDEV=2` for multi-HTP sessions on 9B+
models. Q4_0 / IQ4_NL run fully on HTP; Q4_K_M falls back to CPU (K-quants
unsupported on HTP as of this writing).

### Path 3: ONNX → `ort_engine` (runtime QNN compile)

Same binary as Path 1, loads `.onnx` directly and compiles to QNN at first
launch instead of pre-compiling via QAI Hub. Slower first load, no QAI Hub
step. Good for testing new ONNX models quickly.

### Path 4: TFLite → `tflite_engine` (not yet built)

TFLite + Hexagon delegate (SNPE/QNN under the hood). Needs INT8 quant for
full NPU offload; FP16 ops fall back to CPU.

### Path 5: DLC → `snpe_engine` (not yet built)

Qualcomm's legacy SNPE format. Converter (`snpe-onnx-to-dlc`) is part of the
SNPE SDK — same host-only distribution caveat as above.

### Path 6: PTE → `executorch_engine` (not yet built)

Meta's ExecuTorch with a QNN delegate. Actively changing API upstream.

---

## Size Envelope

| | Size |
|---|---|
| Target | 5.5 GB |
| Ideal ceiling | 6.0 GB |
| Redline | 7.0–7.2 GB |
| W4A16 @ max_seq=2048 | ~5.4 GB |
| W4A16 @ max_seq=4096 | ~5.7 GB |

---

## Qualcomm Documentation Glossary

| Term | Definition |
|---|---|
| QNN Context Binary | Pre-compiled model graph for a specific HTP version. Fastest load, no runtime compile. |
| QNN Execution Provider | ONNX Runtime backend routing ops to QNN/HTP. Supports pre-compiled binaries or runtime ONNX compile. |
| HTP | Hexagon Tensor Processor — the ML accelerator cores in the Hexagon DSP. v79 in SM8750. |
| VTCM | Vector Tightly Coupled Memory — on-chip scratch SRAM. `--scratch_size_mib` controls allocation. |
| W4A16 | Weight 4-bit int, activation 16-bit float. Primary LLM quantization for HTP. |
| Partition Override | JSON forcing specific ops to CPU/GPU when HTP lacks a kernel (e.g. FP16 Softmax). |
| SNPE / DLC | Qualcomm's legacy inference SDK / model format, being replaced by QNN. |
| GGML_HEXAGON | llama.cpp's Hexagon backend — programs HTP directly via the Hexagon SDK. |
| ADPF | Android Dynamic Performance Framework — perf hints, thermal, game mode. |
| QPM3 | Qualcomm Package Manager — the only distribution channel for QNN/Hexagon SDKs. Desktop-only. |

---

## QAI Hub Compile Options (used in `compile_qwen3_5_9b.py`)

```python
COMPILE_OPTIONS_BASE = [
    "--target_runtime", "qnn_context_binary",
    "--quantize_full_type", "w4a16",
    "--quantize_weight_bits", "4",
    "--disable_fusion",
    "--bias_as_int32",
    "--scratch_size_mib", "16",
    "--max_dynamic_tensor_size_mib", "64",
]
```

```python
import qai_hub as hub

job = hub.submit_compile_job(
    model=onnx_path,
    device=hub.Device("Snapdragon 8 Elite"),
    options=" ".join(COMPILE_OPTIONS_BASE),
    calibration_data=calibration_dict,
)
job.get_status()          # QUEUED, RUNNING, SUCCESS, FAILED
job.download_target_model(output_path)
```

---

## What's Built vs What's Needed

| Component | Status |
|---|---|
| Compile script (Qwen3.5-9B) | Written, all fixes committed, Job 8 not yet triggered |
| GenieX daemon | Decision locked (primary runtime); not yet forked/built — see `wiki/GENIEX-DAEMON-PLAN.md` |
| `ort_engine` daemon | Built — real implementation at `daemon/src/`, CI cross-compiles and packages it (legacy runtime) |
| `llama-server` daemon | CI workflow drafted, not built |
| `tflite_engine` / `snpe_engine` / `executorch_engine` | Not started |
| Horizons app | Built, CI green — model/runtime-agnostic, boots with zero model loaded (see `wiki/APP-SOTU-AUDIT.md`) |
| `CliffordService` | Built, needs NpuManager + GameManager perf locks |
