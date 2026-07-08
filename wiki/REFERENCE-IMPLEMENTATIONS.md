# Reference Implementations — Dissection & Applicable Patterns

> Companion to `HORIZONS-BLUEPRINTS.md`. Raw findings from binary analysis,
> source inspection, architecture review, research papers, and Google Drive
> planning documents. Every section ends with **APPLICABLE TO HORIZONS**.
>
> Covers: reference apps (SocketSweep, OverlayD-AI, scrcpy, Genymobile),
> all forks (llama.cpp-npu, snapdragon-npu-llm, EdgeAIApp-ExecuTorch,
> mlc-llm, nexa-sdk, off-grid-ai-mobile, qcom-build-utils, ai-hub-models),
> research papers (NPU Scaling 2509.23324, FraQAT 2510.14823), and
> Google Drive planning sessions (Gemini notebook, system daemon
> architecture, NPU deployment paths, CI workflows).

---

## Table of Contents

1. [SocketSweep](#1-socketsweep)
2. [OverlayD-AI](#2-overlayd-ai)
3. [scrcpy (Genymobile)](#3-scrcpy-genymobile)
4. [Genymobile GitHub Ecosystem](#4-genymobile-github-ecosystem)
5. [off-grid-ai-mobile](#5-off-grid-ai-mobile)
6. [llama.cpp-npu (haozixu)](#6-llamacpp-npu-haozixu)
7. [snapdragon-npu-llm](#7-snapdragon-npu-llm)
8. [EdgeAIApp-ExecuTorch](#8-edgeaiapp-executorch)
9. [mlc-llm](#9-mlc-llm)
10. [nexa-sdk / GenieX](#10-nexa-sdk--geniex)
11. [qcom-build-utils](#11-qcom-build-utils)
12. [ai-hub-models](#12-ai-hub-models)
13. [Research Paper: NPU Scaling (2509.23324)](#13-research-paper-npu-scaling-250923324)
14. [Research Paper: FraQAT (2510.14823)](#14-research-paper-fraqat-251014823)
15. [Google Drive: Gemini Notebook — Original Blueprint](#15-google-drive-gemini-notebook--original-blueprint)
16. [Google Drive: System Daemon Architecture](#16-google-drive-system-daemon-architecture)
17. [Google Drive: ADB/Shizuku Daemon Patterns](#17-google-drive-adbshizuku-daemon-patterns)
18. [Google Drive: NPU Deployment Paths](#18-google-drive-npu-deployment-paths)
19. [Google Drive: CI/CD Workflows](#19-google-drive-cicd-workflows)
20. [Google Drive: Reference Link Collection](#20-google-drive-reference-link-collection)
21. [Pattern Priority Summary](#21-pattern-priority-summary)

---

## 1. SocketSweep

**What it is:** Tauri (Rust) desktop app that bundles its own ADB binary +
ARM64 daemon. Scans a phone's filesystem over a TCP tunnel without MTP.

### Deployment Flow (reconstructed from binary strings)

```
1. check_adb        — verify USB debugging is on, device authorized
2. pkill -f socketsweep_daemon || true   — kill any prior instance
3. adb push daemon /data/local/tmp/socketsweep_daemon
4. appops set com.android.shell MANAGE_EXTERNAL_STORAGE allow
5. nohup ./socketsweep_daemon &          — daemon starts, detached
6. adb forward tcp:PORT tcp:PORT         — tunnel to host
7. PING health check over TCP            — confirm daemon responds
```

### MTP Bypass

SocketSweep never touches MTP. It uses ADB (the USB debugging protocol) to
push the daemon binary to `/data/local/tmp/`. The daemon then accesses
`/sdcard` via direct filesystem calls, not via MTP. The `appops set
com.android.shell MANAGE_EXTERNAL_STORAGE allow` command grants the shell
user (UID 2000) full storage access without any UI permission dialog.

### Kill-Switch Bypass

The daemon runs under the **shell user** (UID 2000) via ADB — NOT under any
app UID. Android's battery optimization, doze mode, app standby, and force
stop only target app processes (those running under app UIDs). Shell user
processes are treated like system processes. `nohup` ensures the daemon
survives the ADB session ending.

### Daemon Architecture (from `daemon.cpp` strings)

- Single-threaded TCP server on `127.0.0.1:PORT`
- Signal handler for clean shutdown (`on_signal`)
- `scan()` function: recursive filesystem traversal, sorts results
- JSON response: `{"status":"ok","scan_time_ms":...}`
- Log format: `[engine] Listening on %s:%u (pid %d)`
- Clean exit: `[engine] Shutdown complete.`
- Built with Android NDK (linker64, libc.so)

### APPLICABLE TO HORIZONS

1. **Daemon-as-TCP-server on localhost** — identical to our `ort_engine` /
   `llama-server` architecture. Validated pattern.
2. **`/data/local/tmp` deploy path** — only works with ADB access (desktop
   companion or Shizuku). Horizons uses jniLibs packaging instead (exec from
   nativeLibraryDir), which works without ADB but IS subject to Android
   lifecycle. Trade-off is correct for a standalone app.
3. **`MANAGE_EXTERNAL_STORAGE` via appops** — we could use this for model
   file access from Download/ if `requestLegacyExternalStorage` proves
   flaky. Requires Shizuku or ADB.
4. **Health-check pattern** — PING over TCP before considering daemon ready.
   CliffordService should implement this instead of assuming the daemon is
   alive after Process.start().

---

## 2. OverlayD-AI

**What it is:** Fully offline AI assistant running in Termux. Uses Shizuku
for privilege escalation, llama.cpp as the inference server, Node.js as
the orchestration bridge, Telegram as the UI.

### Architecture Stack (from the 13-page technical guide)

```
Layer 1: UI              — Telegram Bot (natural language I/O)
Layer 2: Orchestration   — Node.js bridge (prompt engineering, cmd parsing)
                         — OpenClaw Local (vision capture, UI analysis)
Layer 3: AI Inference    — llama.cpp + Qwen vision model
Layer 4: Privilege       — Shizuku + rish (ADB privileges without root)
Layer 5: System          — Android APIs (hardware, apps, settings)
```

### Sandbox Escape — Shizuku + rish

The core innovation: uses Android's Wireless Debugging feature to acquire
ADB permissions without root. Shizuku exports a `rish` command into Termux
that executes with system-level privileges:

```bash
rish -c "input keyevent 3"          # force home
rish -c "monkey -p com.youtube 1"   # launch app
rish -c "svc wifi disable"          # toggle WiFi
```

This creates the bridge: the AI (sandboxed in Termux) generates ADB
commands, Node.js wraps them in `rish`, and the system executes them.

### OpenClaw-Local Vision Interception

OpenClaw is designed for ChatGPT over the internet. OverlayD-AI intercepts
it with a custom `openclaw-local` executable that redirects all API calls
to `http://127.0.0.1:8080/v1` with a dummy API key (`sk-local-offline`).
The closed loop: screenshot → OpenClaw UI analysis → LLM inference →
coordinate extraction → `rish` tap command.

### Inference Server Config

```bash
./server -m model.gguf \
         --host 127.0.0.1 \
         --port 8080 \
         -c 4096 \
         --timeout 300
```

Identical to our llama-server configuration.

### APPLICABLE TO HORIZONS

1. **Shizuku/rish as optional privilege escalation** — Horizons' Agent tools
   (`ScreenshotCapture`, `SecureResourceRelay`) need system-level access
   that the app sandbox doesn't provide. Shizuku integration (optional,
   for power users) would unlock: screen capture without MediaProjection
   dialog, app launch/control, system setting toggles. Add as an optional
   capability, not a requirement.
2. **OpenAI-compatible localhost endpoint** — proven pattern. Our
   `llama-server` already exposes `/v1/chat/completions`. Any tool that
   speaks OpenAI format (OpenClaw, LangChain, etc.) works out of the box.
3. **Few-shot prompt → structured command extraction** — the `CMD:` prefix
   pattern for getting the LLM to emit executable commands. AgentLoop
   already does this with tool calls; the pattern validates it.
4. **Vision-to-action closed loop** — screenshot → LLM → tap coordinates.
   This is the Vision-Agent tile in FEATURE-SPEC.md. The OpenClaw approach
   (intercept a cloud tool and redirect to localhost) is how we'd integrate
   third-party vision frameworks.

---

## 3. scrcpy (Genymobile)

**What it is:** 145k-star tool that mirrors and controls an Android device's
screen from a desktop. The server-side Java process runs on the phone.

### Server Deployment Model

1. Desktop client pushes `scrcpy-server.jar` to the device via ADB
2. Server started via `adb shell app_process` (runs as a Java process, not
   an installed APK — so it has ADB shell privileges)
3. **Nothing is left installed on the device** — the server jar is temporary

### Process Lifecycle (`Server.java`)

- Entry: `main()` → `internalMain()` → `scrcpy()`
- Sets uncaught exception handler
- **`dropRootPrivileges()`** — if running as root (UID 0), drops to UID
  2000 (shell user). Comment: "Copy-paste does not work with root user."
- **`Looper.loop()`** — blocks the main thread, keeping the process alive.
  Only exits when `quitSafely()` is called by the Completion handler.
- Cleanup (finally block): stops all AsyncProcessors (audio, video,
  control), shuts down connection, joins threads, closes OpenGL
- **`System.exit(status)`** — explicit exit to kill non-daemon threads
  started by the Android SDK

### Kill-Switch Bypass

Same mechanism as SocketSweep: runs under the **shell user** (UID 2000),
not an app UID. Android's lifecycle management doesn't apply. The process
survives doze, battery optimization, and app standby because it's not an
app process.

When ADB disconnects: the Looper keeps the process alive briefly, but the
connection handler detects the socket closure and triggers the Completion
shutdown. Clean exit, no zombie.

### APPLICABLE TO HORIZONS

1. **`app_process` as a deployment vehicle** — scrcpy proves you can run
   arbitrary Java code on Android as a shell process via `app_process`.
   We could use this for a diagnostic/debugging server that runs alongside
   the main app but outside its lifecycle (useful for monitoring CliffordService
   from outside the app's process group).
2. **`dropRootPrivileges()` pattern** — always drop to lowest needed UID.
   Our daemon doesn't run as root, but if it ever does (e.g. via Shizuku),
   it should drop immediately.
3. **Looper.loop() for process persistence** — the Android way to keep a
   non-UI process alive. CliffordService already uses a foreground service
   (the Android-approved approach), which is correct for an app process.
4. **Explicit System.exit()** — prevents zombie processes from SDK threads.
   CliffordService should call this in its crash recovery path.

---

## 4. Genymobile GitHub Ecosystem

14 repositories. Key ones for Horizons:

| Repo | Stars | Language | Relevance |
|---|---|---|---|
| **scrcpy** | 145k | C | Screen mirroring. See section 3. |
| **gnirehtet** | 7.8k | Java | **Reverse tethering** — provides internet to a phone via USB from a PC. Relevant for development/testing when the phone has no WiFi. |
| **genymotion-device-web-player** | 147 | JS | Web-based device player. Interesting for remote Horizons testing — could embed a web view of the device for debugging. |
| **Magisk** | 1 | Rust | Fork of topjohnwu/Magisk. Root manager. NOT needed for Horizons (we use Shizuku, not root). But confirms Genymobile's deep Android internals expertise. |
| **genymotion-kernel** | 92 | C | Custom Android kernel. No direct Horizons applicability unless we need kernel-level NPU tuning. |
| **genymotion-saas-github-action** | 22 | JS | CI integration for Genymotion Cloud devices. Could use for Horizons CI testing on real Snapdragon hardware if Genymotion supports SM8750. |
| **genymotion-gradle-plugin** | 171 | Groovy | Gradle device control. Could integrate into our build pipeline for automated APK testing. |

### SaaS Offering

Genymotion Cloud SaaS provides virtual Android devices in the cloud for CI
testing. Available as: GitHub Action (`genymotion-saas-github-action`),
CircleCI Orb (`genymotion-saas-orb`), and Bitrise Step. Potentially useful
for Horizons CI — running our APK on a virtual Snapdragon device to verify
the daemon lifecycle without needing physical hardware.

### APPLICABLE TO HORIZONS

1. **gnirehtet** — reverse tethering could solve development connectivity
   (user's "phone only, no laptop" constraint means the phone sometimes
   needs internet from another device).
2. **Genymotion Cloud for CI** — if they support SM8750 or similar
   Snapdragon devices, we could run end-to-end daemon tests in CI.
3. **Device web player** — for remote debugging sessions, embed a web
   view of the device's screen.

---

## 5. off-grid-ai-mobile

**What it is:** React Native AI assistant app. The closest architectural
parallel to Horizons — same problem space, production-quality codebase.

### Key Architecture Patterns

#### Provider Abstraction (`src/services/providers/types.ts`)

Single `LLMProvider` interface that ALL backends implement:

```typescript
interface LLMProvider {
  id: string;
  type: ProviderType;        // 'local' | 'openai-compatible' | 'anthropic'
  capabilities: ProviderCapabilities;
  loadModel(modelId: string): Promise<void>;
  unloadModel(): Promise<void>;
  isModelLoaded(): boolean;
  generate(messages, options, callbacks): Promise<void>;
  stopGeneration(): Promise<void>;
  getTokenCount(text: string): Promise<number>;
  isReady(): Promise<boolean>;
  dispose?(): Promise<void>;
}
```

`ProviderCapabilities` declares what each backend CAN do (vision, tool
calling, thinking/reasoning, max context length) — the UI reads capabilities,
never checks which provider is active.

Key capability flags:
- `supportsVision: boolean`
- `supportsToolCalling: boolean`
- `supportsThinking: boolean`
- `acceptsThinkingKwarg?: boolean` (transport-level: does server honor
  `chat_template_kwargs.enable_thinking`?)
- `maxContextLength?: number`

#### ActiveModelService — Single Loading Authority

One class owns ALL model load/unload decisions. No other code path may
load a model directly. This prevents the race conditions and double-loads
that plagued earlier designs:

```
ActiveModelService
  ├── loadedTextModelId    (single source of truth)
  ├── loadedImageModelId
  ├── textLoadPromise      (coalesces concurrent load requests)
  ├── imageLoadPromise
  └── listeners            (change notification)
```

Engine-aware: checks both `llmService.isModelLoaded()` AND
`liteRTService.isModelLoaded()` — the service knows about multiple
engines, but callers don't.

#### Model Residency Manager

Tracks whether model memory is "clean" (loaded, ready) or "dirty" (partially
evicted, needs reload). Handles the Android case where the OS reclaims memory
from backgrounded models — the residency manager detects this and triggers a
reload transparently.

#### Memory Budget Service

Hardware-aware context sizing that steps down gracefully:
`4096 → 2048 → 1024` based on available RAM. Prevents OOM kills by
proactively reducing context window rather than crashing.

#### Zustand as Read-Only Projection

Reactive stores are ONLY for UI rendering. All imperative coordination
(model loads, audio sessions, playback control) lives in services.
The store is a thin projection of service state, never the source of truth.

### APPLICABLE TO HORIZONS

1. **LLMProvider interface** — Horizons should implement this EXACT pattern.
   Each runtime (ort_engine, llama-server, tflite_engine, etc.) implements
   a common interface. NpuClient dispatches to the active provider.
2. **ActiveModelService** — CliffordService + NpuClient should have a single
   authority that owns model load state. Current architecture has this
   roughly right but not formalized.
3. **Memory budget → context step-down** — critical for SM8750 (16GB shared
   with GPU/display). Our `max_seq_len` in manifest.yaml (2048 for target,
   4096 for ideal) already reflects this thinking.
4. **Capability-based routing** — RouterPane should query provider
   capabilities, not provider identity. "Does the current provider support
   vision?" not "Is the current provider ort_engine?"
5. **Zustand discipline** — applies directly to Horizons' Compose state
   management. ViewModel projections, not authoritative state.

---

## 6. llama.cpp-npu (haozixu)

**What it is:** Fork of llama.cpp with a custom Hexagon NPU backend. This
is the code repository for research paper 2509.23324 (EuroSys '26). THE
primary reference for our ggml-hexagon integration.

### Architecture

Two components:
1. **This repo** — llama.cpp with `-DGGML_HTP=ON` (Hexagon Tensor Processor
   backend, enabled by default)
2. **htp-ops-lib** (separate repo by haozixu) — custom HTP operator library
   that implements GGML ops as Hexagon DSP kernels

### Build Requirements

- Android NDK (cross-compilation, not on-device)
- Hexagon SDK 6.x (verified: 6.0.0.2)
- `-DGGML_OPENMP=OFF` required (CPU-side implementations incompatible
  with OpenMP in the hybrid backend)

### Key Build Artifacts

```
build/bin/llama-cli           — inference CLI
build/bin/llama-quantize      — model quantizer
build/src/libllama.so
build/ggml/libggml.so
build/ggml/libggml-base.so
build/ggml/libggml-cpu.so
build/ggml/src/ggml-htp/libggml-htp.so   — THE NPU backend library
```

### HTP Operator Library (htp-ops-lib)

Dual build process:
```sh
build_cmake android                    # ARM-side stub
build_cmake hexagon DSP_ARCH=v73       # DSP-side skel
```

Produces:
- `android_ReleaseG_aarch64/` — ARM-side `.so` (loaded by CPU process)
- `hexagon_ReleaseG_toolv87_v73/` — DSP-side skel (loaded on Hexagon via
  FastRPC/cdsprpc)

`DSP_ARCH=v73` is the default for broadest compatibility (8 Gen 2+).
Can be set to `v75` (8 Gen 3 / 8 Elite) or `v79` for newer hardware.

### CPU-NPU Hybrid Scheduling

The key innovation: the GGML backend scheduler splits ops between CPU and
NPU at the tensor level. Ops that can run efficiently on HMX (matrix
multiplies, quantized attention) go to the NPU. Ops that can't (complex
control flow, small tensors) stay on CPU. This is the `n_gpu_layers`-style
scheduling that nexa-sdk calls "hybrid mode."

### Quant Compatibility

- **Q4_0 / IQ4_NL** — full HTP offload, maximum NPU utilization
- **K-quants (Q4_K_M, Q5_K_M)** — CPU only, the shuffled block layout
  doesn't map to HMX tile geometry

### APPLICABLE TO HORIZONS

1. **THIS IS OUR PRIMARY RUNTIME'S UPSTREAM** — Horizons' `llama-server`
   + `ggml-hexagon` IS this code (or a close descendant). Every pattern
   here applies directly.
2. **Skel build separation** — the DSP-side skels are separate
   `ExternalProject_Add` targets. This is the exact bug we found and
   fixed in `build-llama-server.yml` runs #5-#8: skels don't ride the
   default `all` target and must be explicitly built.
3. **Q4_0 for NPU, K-quants for CPU** — informs model selection. The
   operator's Qwen3.5-9B Q4_0 GGUF is the correct choice for HTP offload.
4. **OpenMP must be OFF** — already wired in our CI, don't regress.
5. **htp-ops-lib as a separate dependency** — if we need to update or
   patch HTP operators, it's a different repo from llama.cpp.

---

## 7. snapdragon-npu-llm

**What it is:** Orchestration layer for running LLMs on Hexagon NPU via
ExecuTorch + QNN. Proves NPU inference works on devices as old as
Snapdragon 8 Gen 1 (Hexagon v69), contradicting conventional wisdom.

### Key Result

**31.3 tok/s decode, 107ms TTFT** on Snapdragon 8 Gen 1 with Qwen3-0.6B
via ExecuTorch's QNN backend. This disproves the claim that Hexagon v69
can't run LLMs — it can, with the right `.pte` file.

### Device Support Matrix

| SoC | Hexagon | Status | Tokens/sec |
|---|---|---|---|
| SM8450 (8 Gen 1) | v69 | Verified | 31.3 |
| SM8550 (8 Gen 2) | v73 | .pte available | ~50 est. |
| SM8650 (8 Gen 3) | v75 | Skel included | ~60 est. |
| SM8750 (8 Elite) | v79 | Skel included | ~80 est. |
| SM8850 | v81 | Compile via AI Hub | ~100+ est. |

### Architecture

```
Host (Linux) → ADB shell → /data/local/tmp/executorch_qualcomm_tutorial/
                             ├── qnn_llama_runner (ExecuTorch CLI)
                             ├── hybrid_llama_qnn.pte (model)
                             ├── libQnnHtpV69Stub.so (CPU side)
                             └── libQnnHtpV69Skel.so (DSP side, via FastRPC)
```

NPU reached through Qualcomm's `cdsprpc` (Compute DSP Remote Procedure
Call) — domain 3 in FastRPC parlance.

### Scripts

- `install.sh` — detects phone's SoC, downloads matching bundle, ADB push
- `run.sh` — prompts → adb exec → result

### APPLICABLE TO HORIZONS

1. **ExecuTorch is a viable Path 6** — proves `.pte` files run on Hexagon
   NPU with real performance. If we ever need an ExecuTorch runtime for
   Horizons, this is the reference.
2. **v69 works** — expands our potential device support far beyond SM8750.
   The SM8450/SM8550/SM8650 install base is massive (Galaxy S22/S23/S24,
   OnePlus 10/11/12, Xiaomi 12/13/14).
3. **Skel versioning matters** — the `.pte` file bundles skels for multiple
   HTP versions. Same pattern as our `libggml-htp-v{73,75,79,81}.so` set.
4. **`/data/local/tmp` deploy path** — same as SocketSweep. Works for
   development/testing, not for app distribution.

---

## 8. EdgeAIApp-ExecuTorch

**What it is:** Android CLIP application using ExecuTorch 0.7.0 + QNN v79
backend for zero-shot image classification and vision-language tasks.

### Key Details

- **Model:** CLIP (~400MB) for multimodal understanding
- **Backend:** Qualcomm QNN with HTP/DSP acceleration
- **Capabilities:** Zero-shot classification, image-text matching, visual Q&A
- **Camera integration** for real-time inference

### APPLICABLE TO HORIZONS

1. **ExecuTorch + QNN Android integration pattern** — shows how to package
   ExecuTorch models in an Android app with QNN backend delegation. Path 6
   (PTE) reference implementation.
2. **Vision model on NPU** — relevant for Horizons' Vision-Agent tile.
   CLIP on the NPU would enable on-device image understanding without
   cloud calls.
3. **QNN v79 targeting** — matches our SM8750 device's HTP version.

---

## 9. mlc-llm

**What it is:** Universal LLM deployment engine with ML compilation from
the MLC (Machine Learning Compilation) project. Supports GPU backends
across platforms.

### Platform Support

| Platform | Backends |
|---|---|
| Linux / Windows | Vulkan, ROCm, CUDA |
| macOS | Metal |
| Web Browser | WebGPU, WASM |
| iOS / Android | Metal / Vulkan / OpenCL |

### Architecture

Uses Apache TVM for model compilation. Compiles models into optimized
platform-specific code rather than interpreting them at runtime.

### APPLICABLE TO HORIZONS

1. **Vulkan/OpenCL backend for Adreno GPU** — if GGUF + NPU proves
   insufficient for certain models, MLC-LLM's Vulkan backend could target
   Adreno 830 directly. This is the GPU fallback path.
2. **WebGPU for browser-based inference** — if Horizons ever needs a web
   companion interface, MLC-LLM proves it's possible.
3. **Model compilation approach** — TVM-style ahead-of-time compilation
   is an alternative to GGUF's runtime interpretation. Different trade-off:
   faster inference but less model portability.

---

## 10. nexa-sdk / GenieX

**What it is:** Qualcomm-backed multi-platform AI inference runtime.
Featured 3 times in official Qualcomm blogs. Supports NPU/GPU/CPU with
clean compute-unit abstraction.

### Compute-Unit Alias Mapping (`sdk/src/device.cpp`)

Single source of truth for all bindings (Go CLI, Python, Android/JNI):

```
cpu     → CPU only
gpu     → GPUOpenCL (Adreno)
npu     → pin HTP0 (dedicated NPU)
hybrid  → empty device_id + n_gpu_layers=999
           (llama.cpp's per-tensor HTP+CPU scheduler — the fast path)
auto    → plugin-specific default
```

Default behavior:
- `hybrid` for `llama_cpp` plugin (per-tensor CPU/NPU scheduling)
- `npu` for `qairt` plugin (QAIRT is NPU-only)
- Model-specific override: `gpt-oss` family defaults to `npu` even on
  `llama_cpp` (incompatible with hybrid scheduler)

QAIRT coercion: if user passes `cpu` or `gpu` to QAIRT, it coerces to
`npu` with a warning — never an error.

### Build System

- **Bazel** for CLI
- **CMake** for SDK
- Languages: C/C++ (SDK), Go (CLI), Python (bindings), Java/JNI (Android)

### APPLICABLE TO HORIZONS

1. **Compute-unit abstraction** — Horizons should adopt this exact pattern
   for `NpuClient`. Instead of hardcoding "use NPU," expose `cpu`/`gpu`/
   `npu`/`hybrid` as selectable modes in RouterPane.
2. **`hybrid` as the default** — the per-tensor CPU/NPU scheduler is the
   fast path on Snapdragon. This validates our `ggml-hexagon` approach
   where some ops run on CPU and others on HTP.
3. **QAIRT coercion pattern** — graceful degradation: if a backend can't
   support the requested device, coerce with a warning instead of failing.
4. **Model-family incompatibility flags** — some model architectures can't
   run on hybrid. Our manifest.yaml should declare per-model device
   compatibility.

---

## 11. qcom-build-utils

**What it is:** Centralized build tooling and reusable GitHub workflows
for the Qualcomm Linux Debian package ecosystem.

### Architecture

```
Upstream Repos → Package Repos (pkg-*) → qcom-build-utils → Build Infra
                                          (reusable workflows)   (GHCR, ARM64
                                                                   runners, S3)
```

### Key Workflows

- `pkg-build-reusable-workflow.yml` — Debian package build (gbp + sbuild)
- `pkg-promote-reusable-workflow.yml` — promote packages between repos
- `pkg-release-reusable-workflow.yml` — cut releases
- `qcom-preflight-checks.yml` — pre-merge validation

### Composite Actions

- `abi_checker/` — ABI compatibility checks
- `build_package/` — Debian package build
- `push_to_repo/` — publish packages to staging APT repo

### APPLICABLE TO HORIZONS

1. **Reusable workflow pattern** — our `build-apk.yml` and
   `build-llama-server.yml` could adopt the caller/reusable split for
   shared build logic.
2. **ABI checker** — useful if we publish native `.so` libraries that
   external apps consume (future: if Horizons' daemon API becomes a
   platform for other apps).
3. **ARM64 runner infrastructure** — Qualcomm has dedicated ARM64 runners
   for native builds. If we need to build Hexagon skels natively (not
   cross-compile), this is the model.

---

## 12. ai-hub-models

**What it is:** Qualcomm's repository of ML models optimized for Qualcomm
chipsets. Model zoo with export, compile, profile, and on-device inference
tooling.

### Key Commands

```bash
qai_hub_models/scripts/run_codegen.py -m <model_id>  # generate export.py
python -m qai_hub_models.models.<model_id>.export     # export + profile
python -m pytest qai_hub_models/models/<model_id>/test.py  # run tests
```

### SDK API (`qai_hub` package)

- `submit_compile_job()` — compile model for target device
- `submit_profile_job()` — profile on real hardware
- `submit_inference_job()` — run inference on device
- `upload_model()` — upload to AI Hub
- Target devices include SM8750 (Snapdragon 8 Elite)

### APPLICABLE TO HORIZONS

1. **This is the compile pipeline's target** — Job 8 (ONNX → QNN context
   binary) uses `qai-hub` to compile our Qwen3.5-9B for Hexagon HTP.
2. **Model profiling** — once Job 8 produces a compiled model, we can use
   `submit_profile_job()` to get real performance numbers on SM8750 before
   deploying to the device.
3. **Reference model implementations** — the repo has working examples of
   models exported for QNN/HTP that we can study for our own export script.
4. **On-device debugging** — `.claude/docs/on-device-debugging.md` in the
   repo has guides for diagnosing rank errors, memory failures, and
   resolution search on Qualcomm hardware.

---

## 13. Research Paper: NPU Scaling (2509.23324)

**Paper:** "Scaling LLM Test-Time Compute with Mobile NPU on Smartphones"
(EuroSys '26, by haozixu et al.)

### Hardware Facts (Snapdragon 8 Gen 2, Hexagon v73)

| Component | Spec |
|---|---|
| HMX (Matrix Multiply) | 12,032 GFLOPs FP16 GEMM |
| HVX (Vector) single thread | 32.93 GFLOPs |
| TCM (Tightly Coupled Memory) | 8 MiB per HTP session |
| FastRPC shared memory | rpcmem / dmabuf, zero-copy CPU↔NPU |

### Tile Quantization on HMX

- **32×32 tile geometry** — HMX operates on 32×32 FP16 tiles
- **LUT-based Softmax** — 64 KiB lookup table in TCM (0.8% of 8 MiB),
  32,768 entries for non-positive inputs, avoids expensive exp() on DSP
- **FlashAttention** — FP16 throughout with FP32 accumulation
- **Dequantization bottleneck** — HMX layout optimization yields
  1.82-3.45x speedup by aligning group quant to NPU memory access patterns

### Memory Model

- ~300 MiB for 1.5B model on CPU side
- 1.3 GiB dmabuf for 1.5B on NPU
- 2.4 GiB dmabuf for 3B on NPU
- **32-bit address space** limits single NPU session to ~4 GiB
- **NDEV=2** splits model across 2 HTP sessions (~2.85 GiB each for our
  5.7 GB Qwen3.5-9B Q4_0)
- CPU utilization limited to 4 cores

### Test-Time Scaling Result

Qwen2.5-1.5B with Best-of-N (test-time compute scaling) **outperforms
3B baseline** at equal latency budget. This means: a smaller model with
more inference-time compute can beat a larger model with single-pass
inference. Directly relevant to choosing between Qwen3.5-9B Q4_0 (tight
fit) vs. a smaller model with test-time scaling.

### APPLICABLE TO HORIZONS

1. **NDEV=2 is the path for 9B on SM8750** — confirmed viable by this
   paper's memory analysis. Each HTP session gets ~2.85 GB, under the
   4 GB/session 32-bit ceiling.
2. **Q4_0 group quant must align to HMX tile geometry** — if we ever
   re-quantize, the block size must be a multiple of 32 for efficient
   HMX dequantization.
3. **LUT Softmax in TCM** — the 64 KiB table is already implemented in
   htp-ops-lib. No action needed, but explains why attention is fast.
4. **Test-time scaling as a fallback** — if 9B Q4_0 is too large, a 3B
   model with Best-of-N could achieve comparable quality.
5. **FastRPC shared memory** — `rpcmem_alloc()` for zero-copy data
   transfer. Critical for first-token latency.

---

## 14. Research Paper: FraQAT (2510.14823)

**Paper:** "Fractional Quantization Aware Training" (Samsung AI Center)

### Core Innovation

Progressive precision reduction during training:
**FP32 → INT8 → INT4** with fractional intermediate bit-widths.

Unlike standard QAT (which jumps directly to target precision), FraQAT
uses continuous "fractional bits" to gradually narrow the quantization
range, reducing the training shock that causes accuracy loss.

### Key Results

- **W4A8 quantization** — 4-bit weights, 8-bit activations
- **4-7% lower FID** than standard QAT on SD3.5-Medium, Sana, PixArt-Σ,
  FLUX.1-schnell
- **Static quantization ONLY on mobile** — dynamic quantization requires
  per-batch calibration that mobile NPUs can't do at inference time
- **Deployed Sana on Samsung S25U** (SM8750-AB Hexagon HTP) — proves
  W4A8 runs on the exact same SoC family as our target device

### APPLICABLE TO HORIZONS

1. **W4A8 is viable on SM8750 HTP** — Samsung proved it works on our
   target chip. If we need higher quality than Q4_0 GGUF, W4A8 via FraQAT
   is the path.
2. **Static quantization only** — Hexagon HTP requires static shapes and
   static quantization parameters. Dynamic quantization is not an option.
   This matches our `use_cache=False` + batch=1 + fixed `MAX_SEQ_LEN`
   constraints.
3. **Progressive QAT for future model training** — if we ever fine-tune
   Qwen3.5-9B for Horizons-specific tasks, FraQAT would produce a
   better-quality quantized model than direct quantization.

---

## 15. Google Drive: Gemini Notebook — Original Blueprint

**What it is:** A 72KB Gemini 3.5 Flash notebook session (June 21, 2026)
that IS the birth document of Novus Agenti / Omni Claw. Contains the
operator's original vision in their own words.

### Key Architectural Decisions (operator's original intent)

#### Dual Floating Tile System

1. **Standard Floating Microphone Tile** — global Gboard-style dictation
   replacement. Raw voice → local cleanup → direct text injection into any
   active cursor field via Accessibility Service. NO meta-prompting, NO
   tool calls. Just ultra-crisp text entry.
2. **AI Chat Floating Box** — modular workspace overlay with:
   - `[🎤]` Sandbox Microphone (agentic commands, NOT raw dictation)
   - `[📸]` Camera Icon (point-and-shoot screen capture for vision)
   - `[+]` Upload Tray
   - `[||]` Pause Button (freeze token streaming)
   - `[📑]` Carbon Tabs (session management)
   - `[📤]` Share Icon (export to Drive/Docs)

#### Meta-Prompt Verification Loop

```
[Tap Internal Mic] → [Speak Rough Prompt] → [Silero VAD isolates audio]
→ [Local HTTP 127.0.0.1:8080] → [C++ Engine parses Obsidian skills]
→ [Token compilation outputs to Box] → [Human reviews/edits]
→ [Send Command]
```

The model NEVER auto-executes. It streams compiled meta-prompts back to
the chat box as editable text. Human reviews, then manually sends.

#### Thinking Token Suppression

```
[Raw SSE Tokens] → [C++ Token Parser Interceptor]
  ├→ Detects <think> → Emits {"status":"thinking"} (drops raw text)
  └→ Detects </think> → Emits {"status":"output"} (streams answer)
```

UI shows a clean "Thinking..." shimmer instead of dumping reasoning tokens.
Operator explicitly requested this: "It seems like all it does is slow the
model down."

#### Triple-Mode Screen Vision

1. **Continuous Screenshot Parsing** — AOSP Accessibility API background
   loop for complex navigation mapping
2. **Point-and-Shoot** — Camera icon in AI Chat tile for troubleshooting
   dense UIs (e.g., Google Cloud Console)
3. **Local Storage Sniffer** — FileObserver on `/sdcard/Pictures/Screenshots/`,
   auto-attaches new screenshots to chat context

#### Three-Panel Layout

```
Panel 1 (Top):     Cloud & Vision Space — web browser, Colab, cloud buckets
Panel 2 (Middle):  Local Command Cockpit — Termux terminal, CLI tools
Panel 3 (Bottom):  Omni-Claw Chat Panel — Kotlin frontend, STT, token logs
```

#### Tool Execution Map

| Zone | Layer | Capabilities |
|---|---|---|
| Local Device | Android IPC + Tasker Relay | File ops, Obsidian/Markor, system actions |
| Local Shell | Termux GLIBC + JSON-RPC :8022 | Bash, git, patchelf, HF/GH CLIs |
| Cloud | Colab CLI (`--auth adc`) | T4 GPU, Python scripts, model compilation |

#### Watchdog Recovery Bridge

- ~15MB foreground service monitors NpuClient.kt lifecycle
- C++ daemon anchored at `-950 oom_score_adj` (unkillable)
- WebSocket state frames to micro-database for crash recovery
- Hot-reboot: reads persistent state cache, restarts Kotlin UI,
  reconnects to daemon socket — model weights never reload

### APPLICABLE TO HORIZONS

1. **This IS the product spec** — everything in FEATURE-SPEC.md should
   trace back to decisions made here.
2. **Dual tile system** — already partially built (7 tiles in current UI).
   The microphone-vs-chat-tile split is the operator's explicit design
   decision.
3. **Thinking token suppression** — needs to be implemented in the SSE
   parser in `NpuClient.kt` or the daemon's HTTP response handler.
4. **Verification loop** — the meta-prompt pattern (model outputs to editable
   text, human reviews before send) is core to the product identity. Never
   auto-execute.
5. **Three vision modes** — maps to the orphaned `ScreenshotCapture.kt`
   class (continuous + point-and-shoot) plus a FileObserver (not yet built).
6. **Watchdog hot-reboot** — CliffordService already does the 15s recovery
   loop; the state-preservation via persistent cache is the missing piece.

---

## 16. Google Drive: System Daemon Architecture

**What it is:** A Gemini-generated technical guide covering CI workflow,
Android manifest configuration, foreground service daemon implementation,
VoiceInteractionService, dual-app watchdog, dynamic APK sideloading,
Termux interop, and model hot-swapping.

### Key Patterns

#### CI Workflow Blueprint

```yaml
name: Build System Daemon APK
on: push/PR to main/dev
jobs:
  build: ubuntu-latest
    - checkout, JDK 21, gradlew test, assembleRelease
    - upload-artifact: app-release.apk
```

#### Manifest Configuration

Three simultaneous hooks:
1. **Video Game Registration** — `<meta-data android:name="android.game.category"
   android:value="true" />`
2. **Device Assistant API** — `VoiceInteractionService` with
   `BIND_VOICE_INTERACTION` permission, `assistant_interaction_info.xml`
3. **Persistent Daemon Service** — `foregroundServiceType="specialUse"`

#### Dual-App Ghost Watchdog Pattern

```
[App A: Main Engine]              [App B: Watchdog Engine]
(com.sys.daemon)                  (com.sys.watchdog)
         |                                |
  CRASH / OOM EVICTION                    |
         |==[Broadcast: PACKAGE_REPLACED]===>
         |                           (Wakes up)
         |                        Launches Service
         |<=[startForegroundService()]---|
```

Two separate APKs with `PACKAGE_REPLACED` / `PACKAGE_ADDED` broadcast
receivers watching each other. When one dies, the other revives it.

#### Termux Command Integration

```kotlin
Intent().apply {
    className = "com.termux"
    action = "com.termux.RUN_COMMAND"
    putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
    putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf(scriptPath) + arguments)
    putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
}
```

Requires `com.termux.permission.RUN_COMMAND`.

#### Model Hot-Swap

Synchronized swap: close current `FileChannel`, `System.gc()`, open new
model file, return `FileChannel` to LiteRT/QNN runtime. Must fully purge
before remapping — no inline weight reload.

#### ADB Provisioning

```bash
adb shell dumpsys deviceidle whitelist +com.example.systemdaemon
adb shell settings put secure assistant com.example.systemdaemon/.services.SystemAssistantVoiceService
adb shell appops set com.sys.daemon REQUEST_INSTALL_PACKAGES allow
```

### APPLICABLE TO HORIZONS

1. **VoiceInteractionService** — Horizons could register as the device
   assistant, intercepting the long-press home/power gesture. This is how
   we'd replace Google Assistant.
2. **Game category registration** — already done in our manifest
   (`uses-feature android.hardware.game`).
3. **Dual-app watchdog** — more resilient than single-app CliffordService,
   but requires shipping two APKs. Keep as a future option if single-app
   recovery proves insufficient.
4. **Termux RUN_COMMAND** — direct intent-based shell execution without
   requiring the user to open Termux. Useful for agent tool calls that
   need Linux commands.
5. **Model hot-swap pattern** — relevant when switching between Qwen3.5-9B
   and other models. Must fully close + GC before opening new model.

---

## 17. Google Drive: ADB/Shizuku Daemon Patterns

**What it is:** A technical guide exploring three methods to run persistent
background AI daemons on Android: ADB/Shizuku shell elevation, root/init.d,
and the jniLibs packaging trick.

### Method 1: ADB/Shizuku (No Root)

- Shizuku keeps an elevated ADB shell session active on-device
- Daemon spawns under shell user (UID 2000)
- Android LMK ignores it (not an app process)
- Phantom Process Killer disabled via:
  ```
  adb shell settings put global phantom_process_killer_enable false
  ```

### Method 2: Root / Init.d (Requires Root)

- Binary placed in `/data/adb/modules/`
- Boot script (`service.sh`) triggers on startup
- Runs at root/system init level — absolute priority
- NOT needed for Horizons (we use Shizuku, not root)

### Method 3: jniLibs Packaging (No Root, No ADB)

This is the method Horizons actually uses:

1. **Naming trick:** Rename compiled binary to `libllama-server.so`
2. **Gradle:** Drop into `src/main/jniLibs/arm64-v8a/`
3. **Manifest:** `android:extractNativeLibs="true"`
4. **Execution:** `ProcessBuilder(applicationInfo.nativeLibraryDir + "/libllama-server.so", "--port", "11434").start()`
5. **Persistence:** Wrap in Foreground Service to prevent background killing

The guide explicitly confirms: "Android explicitly allows exec() permissions
inside the read-only installation directory (`/data/app/`) for native
libraries." This is the `nativeLibraryDir` path that our current
architecture relies on.

### APPLICABLE TO HORIZONS

1. **jniLibs is the confirmed correct approach** — validated independently
   in this guide, matching our current `nativeLibraryDir` deployment.
2. **Shizuku as optional power-user mode** — for users who want to disable
   the Phantom Process Killer or run the daemon under shell user for
   maximum persistence.
3. **`phantom_process_killer_enable false`** — a one-time ADB command that
   prevents Android 12-16 from killing background processes. Could be
   offered as an optional setup step in Horizons' first-run flow.

---

## 18. Google Drive: NPU Deployment Paths

**What it is:** A comprehensive Gemini-generated guide covering three
deployment paths for LLMs on Snapdragon 8 Elite's Hexagon HTP, plus
precision strategy and SDK documentation links.

### Three Paths

1. **Google LiteRT with Qualcomm AOT Compilation** — `.tflite` → AOT-compiled
   NPU graphs. Linux/WSL host required for compilation.
   `litert_compile --delegate=QNN --qnn_backend=HTP`
2. **Qualcomm GenieX / QNN Runtime** — `qnn-pytorch-converter` →
   `qnn-model-quantizer` (HTP mixed FP16+INT4) → GenieX CLI.
   Best hardware-level access and power efficiency.
3. **llama.cpp NPU Forks** — cross-compile with Android NDK + Hexagon SDK.
   Generate custom `.so` skels that bridge GGML to FastRPC.

### Precision Strategy

- Pure FP16: ~2 GB per billion params. 9B = ~18 GB. **Not viable.**
- HTP optimal: **INT4/INT8 weights + FP16 activations** (mixed precision)
- HTP precision flag: `TfLiteQnnDelegateHtpPrecision.kHtpFp16`
- This guarantees FP16 internal math while keeping weights compressed

### Critical Verification Setting

```c
TfLiteQnnDelegateHtpBackendOptions options;
options.precision = TfLiteQnnDelegateHtpPrecision.kHtpFp16;
```

### APPLICABLE TO HORIZONS

1. **Path 3 is what we're doing** — llama.cpp-npu fork + ggml-hexagon
   with custom HTP skels.
2. **Mixed precision is mandatory** — pure FP16 doesn't fit. Our Q4_0
   (4-bit weights) + FP16 math matches the recommended strategy exactly.
3. **Path 1 and Path 2 are future options** — if we add LiteRT or GenieX
   runtimes, these are the compilation steps.
4. **kHtpFp16 precision flag** — relevant if we ever build an `ort_engine`
   that uses TFLite delegation instead of QNN EP directly.

---

## 19. Google Drive: CI/CD Workflows

**What it is:** Multi-variant Android CI/CD workflow with scalable secret
management. From a CodeTutorHub tutorial video.

### Key Pattern: Variant-Driven Secret Classification

```yaml
# Secrets classified by scope:
COMMON:              ANALYTICS_SDK_KEY (always injected)
ENVIRONMENT-SPECIFIC: BACKEND_TOKEN_{QA,STAGING,PROD} (always — Gradle
                     evaluates ALL flavors at config time)
TIER-SPECIFIC:       AD_SDK_KEY_{FREE,PAID} (always)
RELEASE-ONLY:        RELEASE_SIGNING_* (only when buildType=release)
```

Variant name encodes env+tier+buildType: `prodPaidRelease`,
`qaFreeDebug`, etc. No giant if-else — derive from the name.

### APPLICABLE TO HORIZONS

1. **Secret classification pattern** — our `build-apk.yml` currently has
   minimal secrets (debug keystore is committed). If we add production
   signing, this is the pattern.
2. **Multi-variant builds** — Horizons could have `debug`/`release` ×
   `npu`/`cpu` variants. The variant-name-driven approach scales cleanly.
3. **`resolveSecretOrNull` for optional secrets** — signing secrets are
   only present for release builds. Empty is OK for debug builds.

---

## 20. Google Drive: Reference Link Collection

**What it is:** The operator's curated collection of reference links for
the project, discovered across multiple research sessions.

### Key Links (categorized)

**NPU Runtime:**
- `haozixu/llama.cpp-npu` — our primary runtime upstream
- `llama.cpp/docs/backend/snapdragon/README.md` — official ggml-hexagon docs
- `qualcomm/GenieX` — Qualcomm's official LLM runtime
- `bpbonker/npurun` — community NPU runner

**App Architecture:**
- `off-grid-ai/off-grid-ai-mobile` — provider abstraction reference
- `off-grid-ai/off-grid-ai-mobile/pull/256` — specific PR of interest

**NPU Documentation:**
- `developers.google.com/edge/litert/next/qualcomm` — LiteRT + QNN guide
- `docs.qualcomm.com/.../QNN_general_overview.html` — QNN SDK overview
- `docs.qualcomm.com/nav/home/htp_htp.html` — HTP backend guides
- `emergentmind.com/topics/qualcomm-sm8750-ab-...` — SM8750 HTP reference

**Android Native Exec:**
- `r/androiddev: PSA Android Q blocks executing binaries` — the noexec
  discovery that led to jniLibs approach
- `r/androiddev: Executing compiled C binary in mobile app` — the
  `libllama-server.so` naming trick
- `greenaddress/abcore#97` — ProcessBuilder exec pattern
- `issuetracker.google.com/128554619` — Google's official stance on
  native binary execution
- `r/lowlevel: Exploring Android storage without MTP C daemon` — the
  SocketSweep Reddit thread

**scrcpy:**
- `Genymobile/scrcpy/doc/connection.md` — TCP/IP wireless debugging,
  AutoAdb auto-launch

### APPLICABLE TO HORIZONS

1. **This is the operator's research trail** — every link here influenced
   a design decision in HORIZONS-BLUEPRINTS.md. When a decision seems
   arbitrary, check this list for the original source.
2. **The noexec threads** — the `r/androiddev` posts are the empirical
   proof that jniLibs packaging works. Cite these if anyone questions the
   approach.

---

## 21. Pattern Priority Summary

| Priority | Pattern | Source | Where in Horizons |
|---|---|---|---|
| **1** | GGUF + ggml-hexagon hybrid CPU/NPU scheduler | llama.cpp-npu, Paper 2509.23324 | llama-server (PRIMARY runtime) |
| **2** | NDEV=2 for 9B model (split across 2 HTP sessions) | Paper 2509.23324 | CliffordService launch args |
| **3** | LLMProvider interface + single loading authority | off-grid-ai-mobile | NpuClient / CliffordService |
| **4** | jniLibs packaging (`lib*.so` naming trick) | Drive: ADB/Shizuku Patterns | Already implemented |
| **5** | TCP health check before declaring daemon ready | SocketSweep | CliffordService |
| **6** | OpenAI-compatible localhost endpoint | OverlayD-AI | llama-server (already done) |
| **7** | Thinking token suppression (`<think>` → shimmer) | Drive: Gemini Notebook | NpuClient SSE parser |
| **8** | Compute-unit abstraction (cpu/gpu/npu/hybrid) | nexa-sdk | RouterPane / NpuClient |
| **9** | Capability-based routing (not identity-based) | off-grid-ai-mobile | RouterPane |
| **10** | Memory budget → context step-down | off-grid-ai-mobile | manifest.yaml / daemon config |
| **11** | Dual floating tile (mic vs chat) | Drive: Gemini Notebook | UI tiles |
| **12** | Meta-prompt verification loop (human reviews) | Drive: Gemini Notebook | AgentLoop |
| **13** | Triple-mode screen vision | Drive: Gemini Notebook | ScreenshotCapture.kt |
| **14** | Q4_0 for NPU, K-quants for CPU | llama.cpp-npu | Model selection |
| **15** | Static quantization only on HTP | Paper FraQAT | Compile pipeline |
| **16** | Skel versioning (v73/v75/v79/v81) | llama.cpp-npu, snapdragon-npu-llm | build-llama-server.yml |
| **17** | ExecuTorch + QNN as alternative Path 6 | snapdragon-npu-llm, EdgeAIApp | Future runtime option |
| **18** | Shizuku/rish optional privilege escalation | OverlayD-AI | Agent tools (future) |
| **19** | Vision-to-action closed loop | OverlayD-AI | Vision-Agent tile (future) |
| **20** | VoiceInteractionService (replace Google Asst.) | Drive: System Daemon | Future |
| **21** | Termux RUN_COMMAND intent integration | Drive: System Daemon | Agent tools (future) |
| **22** | Shell-user daemon for diagnostics | scrcpy | Debugging tool (future) |
| **23** | Vulkan/OpenCL GPU fallback | mlc-llm | RouterPane fallback (future) |
| **24** | Genymotion Cloud CI | Genymobile | CI pipeline (future) |
| **25** | v69 device support (8 Gen 1 install base) | snapdragon-npu-llm | Future device matrix |
