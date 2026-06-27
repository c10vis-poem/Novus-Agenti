# GPT-OSS Reference — Hexagon HTP Failure Modes & Mitigations

> **Source:** Canonical reference document retrieved from Google Drive
> (file_id: `1hGgRgJV3YxtCAosGJHwNvluL-W8_aQcz70KKQfN-54Y`).
> **Scope:** Snapdragon 8 Elite (Gen 4) / Hexagon v69–v75 NPU, system-daemon
> + Game-SDK deployment.
> **Do NOT paraphrase this document elsewhere — read it directly when making
> compile decisions.**

---

Below is a road-map of the **hard-core failure modes** you will hit when you
try to ship a **Qwen-3.5-9B** model on a **Snapdragon 8 Elite Hexagon NPU**
as a **system-level daemon** that is also registered as a **Game-SDK app**.
Everything is grouped by the stage where the problem appears, the exact
symptom you will see, and a concrete mitigation you can bake into the
build/deployment pipeline.

---

## 1. Model-to-Hexagon Translation (QNN SDK → Hexagon NN)

| # | Failure Mode | Why it Happens | Symptom on Device | Fix / Guard-rail |
|---|---|---|---|---|
| 1 | **Unsupported Op / Variant** | QNN's Hexagon backend only implements a subset of ONNX ops. Qwen-3.5 uses LayerNorm (post-LN), GELU, RoPE, SparseAttention, and a custom KVCache op not in the stock Hexagon lib. | `QNN_ERROR: Op <X> not supported` → graph build abort. | Pre-flight `qnn_convert.py --dry-run` that parses the ONNX graph and flags ops not in `hexagon_ops.txt`. For each: replace with supported pattern, write custom kernel, or CPU fallback. |
| 2 | **Dynamic Shape / Variable Sequence Length** | Hexagon NN expects static tensor dimensions at graph build time. | Graph build succeeds, but inference crashes with `Invalid tensor shape`. | Compile with a hard ceiling (`max_seq_len = 2048`). Allocate KV-cache as static buffer and pass a runtime scalar for effective length. |
| 3 | **Mixed-Precision Mismatch** | Qwen-3.5 is trained in FP16; the Hexagon NPU only supports INT8 (or INT16 for some ops). Direct conversion without calibration yields >10% accuracy loss. | Output text is garbled, perplexity spikes, or model refuses after first few tokens. | Two-step quantization: per-tensor symmetric INT8 for matmuls (weights), per-channel INT8 for large projection matrices (Q/K/V). Use `QnnQuantize` API with a representative dataset. |
| 4 | **Operator Fusion Incompatibility** | QNN graph optimizer may fuse MatMul + Add + LayerNorm into a single kernel. Fusion only exercised for batch-size=1 and static shapes. | `SIGSEGV` inside `hexagon_nn_fused_op` after the second inference call. | **Disable automatic fusion** (`--disable_fusion` flag) and manually insert "identity" ops where you need a barrier. |
| 5 | **Tensor Layout / Alignment** | Hexagon expects NHWC for 2-D tensors; ONNX export of Qwen uses `[seq, hidden]` (row-major). | `QNN_ERROR: Invalid tensor stride` → graph build abort. | Insert a `Transpose` node in the ONNX graph before conversion. Verify all static tensors are 8-byte aligned. |
| 6 | **Large Attention Matrix Memory** | Self-attention score matrix (`seq_len²`) allocated as temp tensor. For `seq_len=2048` this is ~16 MiB per layer, exceeding Hexagon on-chip SRAM (~8 MiB). | DDR spill → latency spike 10–20 ms per token. | Set `QNN_GRAPH_CONFIG_MAX_SCRATCH_SIZE` to 16 MiB to comfortably fit worst-case attention matrix. |

---

## 2. Operator-Level Cheat Sheet (Hexagon v69–v75 / Snapdragon 8 Elite)

| # | Operator / Pattern | Target after AI-Hub partitioning | Why it can break on Gen-4 | Quick mitigation |
|---|---|---|---|---|
| **1** | **RoPE (Rotary Positional Embedding)** | NPU (expanded to element-wise Sin/Cos + Mul) | Hexagon has no FP16 Sin/Cos kernels; FP16 inputs cause type-mismatch at graph-build time. | **Fold RoPE into precomputed FP16 tables** for max sequence length. Replace Sin/Cos ops with Gather from precomputed tables. |
| **2** | **SparseAttention / Block-Sparse** | CPU (fallback) or GPU | Gen-4 ships no official sparse-matmul kernel; converter silently replaces with dense matmul. | Replace sparse attention with dense attention for the mobile target. |
| **3** | **LayerNorm (post-LN with FP16 γ/β)** | NPU (default) | Hexagon built-in LayerNorm only supports pre-LN and INT8 γ/β. Post-LN with FP16 params is rejected. | Move the bias before the norm. Or split: `LayerNormNoBias` + `Add`. |
| **4** | **GELU (exact, uses `erf`)** | NPU (fallback) | Hexagon has no FP16 `erf`; converter falls back to INT8 lookup-table that is inaccurate. | Replace GELU with fast approximation (`0.5*x*(1+tanh(√(2/π)*(x+0.044715*x³)))`) before conversion. |
| **5** | **Multi-Head Attention (fused MHA kernel)** | NPU (default) | Fused kernel only accepts static sequence length and INT8 weights. Variable length or FP16 weights → silent fallback to CPU. | **Disable automatic fusion** (`--disable_fusion`) or insert dummy `NoOp` after each projection. |
| **6** | **KV-Cache (mutable, growing tensor)** | NPU (dynamic tensor) | Hexagon can only update static-size tensors. `QnnTensorUpdate` is buggy for tensors >4 MiB (silently truncates). | **Pre-allocate KV-cache at maximum size.** Pass a scalar "valid-length" to attention kernels. |
| **7** | **Large attention score matrix (`seq_len²`)** | NPU (temporary tensor) | If `seq_len² * sizeof(FP16) > max_scratch_size` (default ~8 MiB), runtime spills matrix to DDR. | Set `QNN_GRAPH_CONFIG_MAX_SCRATCH_SIZE` to 16 MiB. |
| **8** | **Tensor layout mismatches** | NPU (static weight tensors) | Hexagon expects NHWC for 4-D and row-major for attention weights. | Insert explicit `Transpose` nodes. Verify alignment with `qnn_check_alignment`. |
| **9** | **Dynamic shape (variable batch / seq)** | CPU (fallback) | Hexagon only supports static shapes for the NPU. | Hard-code max sequence length, keep batch size = 1. |
| **10** | **Softmax & Top-K** | CPU (default) | These ops are tiny but branch-heavy; no dedicated softmax kernel for FP16. | **Leave on CPU** (latency is negligible). Force via partition override JSON. |
| **11** | **Quantisation-aware bias scaling** | NPU (INT8 matmul) | When weights are quantised to INT8/INT4, biases must be INT32 with a different scale. Converter sometimes leaves bias in FP16 → overflow. | Run QNN quantiser with `--bias-as-int32` flag. |
| **12** | **Cross-processor tensor copies (NPU ↔ GPU ↔ CPU)** | All three | QNN copies tensors automatically but only if they live in DDR. | Force all large intermediate tensors as dynamic DDR tensors (`QNN_TENSOR_FLAG_DYNAMIC`). |
| **13** | **Concurrent contexts limit** | NPU (hardware threads) | Hexagon allows up to 4 concurrent contexts, but public QNN runtime caps it at 2 per process. | **Serialize inference** inside the daemon (single context, queue requests). |
| **14** | **Privileged-daemon / Game-SDK interaction** | OS level | Registering as a Game gives scheduler boost, but restricted-mode for system daemons disables GPU-boost for non-UI processes. | Mark daemon as foreground service (`START_STICKY`, `specialUse` FGS type). Acquire `NpuManager.acquirePerformanceLock(PERF_MODE_HIGH)` before inference. |

---

## 3. Memory Management — Spill-to-DDR on Hexagon

| Concept | How it works on Gen-4 | What you must guarantee |
|---|---|---|
| **On-chip SRAM** | ~12 MiB of fast, low-latency memory. QNN runtime treats this as the "scratch buffer". | Any temporary tensor exceeding the scratch budget is automatically allocated in DDR and accessed via DMA. |
| **Static tensors** (weights, embeddings) | Stored in read-only sections of the DSP image. | Declare the correct `QNN_TENSOR_FLAG_STATIC` flag and provide accurate `max_scratch_size` when calling `QnnGraphCreate`. |
| **Dynamic tensors** (KV-cache, token-mask) | Allocated at runtime via `QnnTensorCreate`. Handle is a 64-bit pointer. | Size you request must be ≤ the maximum you advertised in the graph's `max_dynamic_tensor_size`. |
| **Spill-to-DDR policy** | Controlled by `QNN_GRAPH_CONFIG_MAX_SCRATCH_SIZE` and `QNN_GRAPH_CONFIG_MAX_TENSOR_SIZE`. | Never rely on the slow-path for latency-critical ops. An op that spills will show a 10× latency jump. |
| **Shared-memory route** (DSP ↔ CPU/GPU) | Both pathways use `QnnTensorCopy` under the hood. | The copy must be scheduled by the QNN runtime; you cannot manually `memcpy` into the DSP's SRAM. |

**Bottom line:** As long as every tensor you allocate fits inside the limits you tell the runtime, the spill-to-DDR mechanism is safe and transparent. The only thing you cannot hide is the latency penalty when a large temporary tensor is forced into DDR.

---

## 4. AI Hub / QNN Pre-Compilation Pipeline

### 4.1 The pipeline

```
ONNX (or TorchScript)  →  QNN Model Converter  →  QNN "Compiled Graph" (hexagon.bin)
                                                     |
                                                     | (uploaded to AI Hub)
                                                     v
             AI Hub (cloud)  →  Quantization + Partitioning  →  Hexagon-Ready binary
```

| Stage | What the tool does | What you must provide / verify |
|---|---|---|
| **Model conversion (`qnn_convert`)** | Parses the ONNX graph, inserts layout-conversion nodes, generates a QNN graph descriptor (`.json`). | Run with `--dry-run` first. It will emit an `unsupported_ops.txt` list. Fix those ops. |
| **Quantization (`qnn_quantize`)** | Takes a representative dataset and produces per-tensor or per-channel scales for INT8/INT16. | Use ≥ 10k tokens covering the full Unicode range. |
| **Partitioning (`qnn_partition`)** | Runs a cost model that decides which sub-graph runs on Hexagon, GPU, or CPU. | Default prefers NPU for matmuls, CPU for control-flow (softmax, argmax, sampling). Override via partition JSON. |
| **Binary generation (`qnn_compile`)** | Emits a hexagon binary containing static weight blobs + scratch-size metadata + runtime manifest. | **Check the manifest** (`model.manifest.json`) for max-scratch and max-dynamic-tensor sizes. |
| **Upload to AI Hub** | The Hub stores the binary, signs it, and makes it available to the device. | The Hub **does not re-quantize**; any mistake made earlier is reproduced on every device. |

### 4.2 Why the Hub separates the layers

- **Large dense matmuls** (bulk of a transformer) → NPU-friendly (Hexagon DSP sustains >2 TOPS on FP16).
- **Softmax + top-k + token sampling** → control-flow heavy with tiny tensors → CPU.
- **Vision-to-text encoders** (if applicable) → convolution-heavy → GPU.

---

## 5. Hexagon Capabilities Reference

| Feature | What the silicon actually does | What the QNN SDK currently exposes |
|---|---|---|
| **Data-type support** | FP16 (native), INT8, INT16, BFloat16 (experimental) | FP16, INT8, INT16 are stable. BFloat16 requires `experimental-bf16` flag and QNN v2.13+. |
| **Vector width** | 128-bit SIMD (8 × FP16 per lane) | All matmul kernels are already vectorised for FP16. |
| **On-chip SRAM** | ~12 MiB (shared between L2 cache and fast scratch) | QNN will automatically spill to DDR if a tensor exceeds the `MAX_SCRATCH_SIZE` you set at graph-build time. |
| **64-bit addressing** | Yes — the DSP can address >4 GiB of DDR. | Dynamic tensors (e.g. KV-cache) must be allocated via `QnnTensorCreate` API which returns a 64-bit handle. |
| **Parallel inference** | Up to 4 concurrent contexts per process (hardware). | QNN runtime enforces a global context limit of 2 per process. |

---

## 6. Android System-Level: Game SDK + NPU Lock

### AndroidManifest.xml (required entries)

```xml
<!-- Game SDK feature declaration -->
<uses-feature android:name="android.hardware.game" android:required="true" />
<uses-permission android:name="android.permission.HIGH_PERFORMANCE" />
```

### Kotlin: acquire NPU performance lock (API 33+)

```kotlin
import android.hardware.npu.NpuManager

val npuManager = getSystemService(NpuManager::class.java)
val lock = npuManager.acquirePerformanceLock(NpuManager.PERF_MODE_HIGH)
// keep the lock while inference is running
// ... when done ...
lock.release()
```

### Kotlin: Game SDK performance mode

```kotlin
import com.google.android.gms.games.GameManager
import com.google.android.gms.games.GameManager.GameMode

class HorizonsApplication : Application() {
    private lateinit var gameManager: GameManager
    override fun onCreate() {
        super.onCreate()
        gameManager = GameManager.getInstance(this)
        gameManager.setGameMode(GameMode.PERFORMANCE)
    }
}
```

### Foreground service declaration (CliffordService)

```xml
<service
    android:name=".CliffordService"
    android:exported="false"
    android:foregroundServiceType="specialUse" />
```

### Key rule from the doc

> Registering the app as a **Game** gives you a high-priority scheduler boost,
> but Android's restricted-mode for system daemons disables the GPU-boost flag
> for non-UI processes. The NPU still runs, but any GPU sub-graph will be
> throttled to the background frequency. Combine
> `NpuManager.acquirePerformanceLock(PERF_MODE_HIGH)` with the Game SDK boost
> for the best result.

---

## 7. Runtime Limits to Set

```c
QNN_GRAPH_CONFIG_MAX_SCRATCH_SIZE        = 16 * 1024 * 1024;   // 16 MiB
QNN_GRAPH_CONFIG_MAX_DYNAMIC_TENSOR_SIZE = 64 * 1024 * 1024;   // 64 MiB (canonical)
QNN_GRAPH_CONFIG_MAX_CONTEXTS            = 1;                   // single NPU context per daemon
```

> **Canonical value is 64 MiB.** The hypothesis that HTP v75 (SM8750) dual-core
> soft-caps `MAX_DYNAMIC_TENSOR_SIZE` at 32 MiB is **unverified and was never
> user-confirmed**. 64 MiB is the documented value in this reference and matches
> `--max_dynamic_tensor_size_mib 64` in `compile_qwen3_5_9b.py`. If empirical
> testing on device reveals contention at 64 MiB, narrow it with a measured
> result — but do not change it based on an unverified dual-core hypothesis.

---

## 8. Performance Expectations (Snapdragon 8 Elite reference board)

| Stage | Execution target | Approx. latency (ms) |
|---|---|---|
| Input embedding + positional encoding | NPU (FP16) | 0.8 – 1.0 |
| Q/K/V projection (3 × matmul) | NPU (FP16) | 1.2 – 1.5 |
| Scaled-dot-product attention (score matrix) | NPU (FP16, may spill) | 1.5 – 2.0 |
| Feed-forward network (2 × matmul) | NPU (FP16) | 1.0 – 1.3 |
| Softmax + top-k + token sampling | CPU (AArch64) | 0.2 – 0.3 |
| KV-cache update (dynamic tensor) | NPU (FP16, DDR) | 0.3 – 0.5 |
| **Total per token** | — | **~5 ms (≈ 200 tokens/s)** |

> GPU-only: 8–10 ms/token. CPU-only: >30 ms/token.

---

## 9. Novus-Agenti Specific: Known Job Failures

| Job | Error | Root Cause | Fix Applied |
|---|---|---|---|
| Job 1 | `ModuleNotFoundError: No module named 'onnxscript'` | Missing dep in HF Jobs env | `--with onnxscript` in trigger command |
| Job 2 | `Qwen3_5VisionModel.forward() missing grid_thw` | Deepstack vision = runtime injection | `SKIP_VISION=1` (later removed; all artifacts now attempted) |
| Job 3 | `RuntimeError: Failed to convert dynamic_axes` | `dynamo=True` rejects dynamic export | `dynamo=False` on all `torch.onnx.export()` calls |
| Job 4 | `AttributeError: tuple has no get_seq_length` | Legacy KV cache tuple format | `DynamicCache.from_legacy_cache()` wrap |
| Job 5 | `ValueError: has_previous_state on LinearAttention` | Qwen3.5 hybrid attention rejects `DynamicCache` | `use_cache=False`, stateless prefill |
| Jobs 6–7 | `RuntimeError: cat(): got 5 and 4 tensors` | M-RoPE `apply_interleaved_mrope` returns `[3,B,S,D]`; `unsqueeze(1)` → 5D; cat with 4D `q_pass` fails | Two-pronged fix: folded RoPE forward + module-level patch of `apply_rotary_pos_emb` |
| **Job 8** | pending | — | Trigger with Job 8 command from CLAUDE.md |
