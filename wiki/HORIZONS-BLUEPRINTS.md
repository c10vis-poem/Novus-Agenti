# HORIZONS BLUEPRINTS — What We Are Building

> This document is the architectural foundation. Not a UI spec, not a compile
> pipeline runbook, not a session handoff. This is what Horizons IS, what it
> DOES, and why each choice was made — grounded in research, reference
> implementations, and hardware constraints. If CLAUDE.md is the operational
> manual (how to work on the project), this is the engineering blueprint (what
> the end product looks like and how it works).
>
> Canonical. Every session reads this alongside CLAUDE.md before touching code.

---

## 1. Identity

Horizons is a fully on-device agentic AI assistant. It runs on a Motorola
Razr Ultra 2025 (Snapdragon 8 Elite SM8750, 16GB LPDDR5X, Hexagon HTP v75,
Adreno 830). It is not a chat app with an API key. It is a system that:

- **Ships empty, boots empty.** No model baked in. No runtime hardcoded.
- **Watches for packages to land.** Runtime binaries and compiled models are
  drop-in packages. The app detects them, loads them, serves them.
- **Runs inference on the NPU via a detached native daemon.** Not in-process.
  Not on CPU. The daemon talks localhost HTTP; the app is the orchestrator UI.
- **Hot-swaps runtimes.** GGUF today, QNN context binary tomorrow, LiteRT
  next week. Same app, same UI, different engine binary.
- **Works in airplane mode.** If OverlayD-AI proved it with llama.cpp on
  localhost, Horizons does it with the NPU and a proper daemon lifecycle.

App package: `com.horizons`. Banner: Omni Claw.
Identity: Novus Agenti. Cl0vis x Mer0vin6ian production.

---

## 2. Foundational Research — Three Canonical Papers

Every inference architecture decision traces back to one of these. They are
not optional reading; they define the hardware constraints and performance
envelope.

### 2a. "Scaling LLM Test-Time Compute with Mobile NPU on Smartphones"
**Hao et al., Tsinghua/Microsoft Research, EuroSys 2026 (arXiv 2509.23324)**

What it proves:
- Hexagon NPU matrix units (HMX) are **massively underutilized** during
  standard LLM decoding — GEMM degenerates to GEMV at batch=1, wasting the
  32x32 tile architecture.
- Hardware-aware **tile quantization** (group quant aligned to HMX memory
  access patterns) preserves reasoning accuracy where QNN's per-channel
  quantization destroys it (MATH500: 15.9% AWQ vs 2.1% QNN per-channel).
- LUT-based Softmax and dequantization on HVX vector units: 2.2x Softmax
  speedup, 19x mixed-precision GEMM speedup.
- Test-time scaling (Best-of-N, Beam Search) on NPU lets a 1.5B model
  match or exceed a 7B model's accuracy — smaller model + more compute
  beats bigger model + single pass.

**What this means for Horizons:**
- The GGUF + GGML_HEXAGON path (llama.cpp-npu fork from this paper's
  authors) is not just "an alternative to ort_engine" — it may be the
  **higher-performance path** for models that fit the tile quantization
  scheme (Q4_0, IQ4_NL).
- Test-time scaling is a real feature, not academic — if the daemon supports
  batch>1 inference, a 3B model on NPU can outperform a 9B model on CPU.
- QNN's per-channel quantization is empirically proven to destroy reasoning
  accuracy on this exact hardware. Our ort_engine path uses W4A16 through
  QAI Hub, which is per-group — but we need to verify the group size aligns
  with HMX tile geometry or we eat the same penalty.
- The llama.cpp-npu tile quant scheme is purpose-built for Hexagon. It
  should be the default for any GGUF model running on HTP.

### 2b. "FraQAT: Fractional Quantization-Aware Training" 
**Samsung AI Center, arXiv 2510.14823**

What it proves:
- W4A8 quantization-aware training works on SM8750-AB Hexagon HTP.
  Deployed Sana 600M image generation model on Samsung S25 Ultra.
- Progressive precision: FP32 → INT8 → INT4 with fractional bit widths
  during training. Not post-training quantization — the model learns to
  be quantized.
- **Static quantization ONLY on edge devices.** Dynamic quantization is
  not supported on Hexagon HTP. This is a hard constraint.
- The key insight: QAT models skip the accuracy cliff that PTQ models hit
  at INT4. Google's Gemma 4 QAT variants exist for exactly this reason.

**What this means for Horizons:**
- Any model we compile for NPU must use static quantization. Dynamic
  shapes, dynamic ranges — none of that works on HTP.
- QAT models (like `google/gemma-4-12b-it-qat-q4_0-unquantized`) are
  first-class citizens, not fallbacks. They're pre-aligned for INT4 NPU
  deployment without the accuracy penalty of PTQ.
- Image generation on NPU is proven feasible (Sana 600M on the same
  chipset family). Stable Diffusion / SDXL variants with QAT are a
  realistic future path.
- The FP32→INT8→INT4 progressive pipeline is the right approach for any
  custom model training aimed at this device.

### 2c. OverlayD-AI Technical Guide v1.1
**Orail Noor, reference implementation**

What it proves:
- A fully offline AI assistant running on a stock Android phone (airplane
  mode demonstrated on video) using: Shizuku/Rish privilege escalation →
  llama.cpp localhost:8080 → Node.js orchestration bridge → OpenClaw
  vision → full device control.
- The inference server is a C++ binary (llama.cpp) serving OpenAI-compatible
  API on localhost. The orchestration layer is a separate process. The
  vision layer redirects existing apps to the local server.
- No root required (Shizuku provides ADB-level shell access without root).
- Few-shot prompting for tool use — the model doesn't need fine-tuning to
  control the phone; the orchestration layer wraps commands in prompts.

**What this means for Horizons:**
- The architecture is validated: daemon binary + localhost HTTP + separate
  orchestrator UI works on stock Android.
- We do NOT adopt Shizuku — it's a third-party dependency headed for
  obsolescence. But the PATTERN (elevated shell access for device control)
  is right. We use Android's own accessibility services, foreground service
  permissions, and Termux integration instead.
- The daemon lifecycle (start → serve → stop on demand) maps directly to
  CliffordService + DaemonLauncher. The scrcpy project's boot/unboot
  pattern refines this: run as boot service, then stop cleanly.
- OpenAI-compatible API on localhost is the correct protocol — it's what
  llama-server already serves, and what every cloud provider also speaks.
  NpuClient should speak this protocol natively.

---

## 3. The Three Inference Surfaces

Horizons is not single-backend. It has three surfaces with DIFFERENT rules
about what can run and how models get there.

### Surface 1: Edge / NPU
**What runs here:** Compiled model packages. QNN context binaries,
GGUF+GGML_HEXAGON, LiteRT+QNN delegate, ExecuTorch+QNN delegate.

**Fetch rules:** STRICT. Models must come through the compile pipeline
(QAI Hub, CI cross-compilation) or be pre-quantized in NPU-compatible
formats (Q4_0/IQ4_NL GGUF). No arbitrary downloads run on NPU — every
model is validated against the size envelope (5.5-7.0 GB redline) and
format compatibility before loading.

**Daemon:** Detached native binary (ort_engine, llama-server, future
tflite_engine/executorch_engine). Served via `127.0.0.1:8080`. Managed
by CliffordService.

**Performance contracts:**
- NpuManager perf lock (reflection against @hide npu system service)
- ADPF GameMode boost (GameState MODE_GAMEPLAY_UNINTERRUPTIBLE)
- Both required simultaneously — ADPF boosts CPU scheduling, NpuManager
  gives the NPU full clock

### Surface 2: Cloud
**What runs here:** Any model accessible via API. OpenRouter, OpenAI,
Google, Anthropic, SambaNova, custom endpoints.

**Fetch rules:** OPEN. User provides API keys (stored in encrypted vault,
MonitorPane). App makes HTTPS requests. No compile step, no format
constraints.

**Bridge:** Direct HTTP from the app process. No daemon involved. Cloud
responses stream back via SSE or WebSocket.

**When to use:** User's explicit choice (routing rules in RouterPane),
OR automatic fallback when NPU thermal governor triggers cloud-fallback
flag, OR for capabilities the local model can't handle (long context,
specific model families).

### Surface 3: Shell
**What runs here:** Local files, scripts, binaries executed via Termux
or the app's own shell interface.

**Fetch rules:** OPEN. Standard GGUFs can be fetched and run via
llama-server for quick testing. Scripts can download models, run them
through any runtime, pipe results back.

**Bridge:** `com.termux.permission.RUN_COMMAND` intent, Tasker task
triggers, MacroDroid macro links. The Terminal tile is the UI surface.

**When to use:** Power user operations, model testing, custom pipelines,
anything that doesn't fit the NPU or cloud surfaces.

---

## 4. Runtime Package System

### Core Concept
The app is an orchestrator, not a runtime. It contains zero inference code
in its own process. Every model format has a matching daemon binary, and
those binaries are independent, replaceable packages.

### Package Families

| Family | Binary | Format | NPU Path | Status |
|--------|--------|--------|----------|--------|
| GGUF | `llama-server` | `.gguf` | GGML_HEXAGON (Q4_0/IQ4_NL) | In-flight (session 17) |
| QNN | `ort_engine` | `.bin` (qnn_context_binary) | ORT + QNN EP | Scaffolded, CI-built |
| ONNX | `ort_engine` | `.onnx` | Runtime QNN compile | Same binary as QNN |
| LiteRT | `tflite_engine` | `.tflite` / `.litertlm` | QNN delegate | Not started |
| ExecuTorch | `executorch_engine` | `.pte` | QNN delegate | Not started |
| DLC | `snpe_engine` | `.dlc` | SNPE legacy | Not started |

### Landing Detection
When a new runtime binary or model file appears in the app's file space,
the system must detect it. Two mechanisms:

1. **ModelImportActivity** — explicit user action. SAF file picker, copies
   the binary/model into `filesDir`, registers it in `RUNTIME_FILES`.
2. **PackageManager API polling** — for APK-packaged runtimes distributed
   as separate apps:
   - `getChangedPackages(sequenceNumber)` — OS-level change detection,
     resets every boot. Polls on a timer in CliffordService.
   - `getPackageArchiveInfo(path, flags)` — inspect an APK without
     installing. Used to validate runtime packages before sideloading.
   - `getInstalledPackages(flags)` — enumerate what's installed. Used
     at boot to discover already-installed runtime packages.

### Hot-Swap Lifecycle
```
CliffordService detects new binary/model
  → kills current daemon (SIGTERM, 5s grace, then SIGKILL)
  → launches new daemon with appropriate args
  → health-checks endpoint (GET /health or GET /api/v1/status)
  → broadcasts NPU_READY to main process
  → NpuClient reconnects and verifies protocol (ORT vs OpenAI-compat)
```

The app NEVER crashes because a model isn't loaded. Empty state = valid
state. The UI shows "waiting for binary/model" and remains fully navigable.

---

## 5. Daemon Architecture

### Lifecycle: Boot / Serve / Unboot

Inspired by scrcpy's service pattern and OverlayD-AI's daemon model:

1. **Boot:** CliffordService (foreground service, `specialUse`,
   `START_STICKY`) starts on app launch. First statement in `onCreate()`
   is `startForeground()` — before any I/O, any breadcrumb write, anything
   that could ANR. The 10-second ANR deadline is non-negotiable.

2. **Serve:** CliffordService's 15-second CRS recovery loop:
   - Check daemon PID alive
   - If dead: relaunch via DaemonLauncher
   - If alive: check `/health` endpoint
   - If unhealthy: kill and relaunch
   - Read sysfs thermal state, throttle if > 80C

3. **Unboot:** Clean shutdown. SIGTERM → grace period → SIGKILL.
   CliffordService stays alive (START_STICKY re-creates it), but the
   daemon binary stops. This is the "unboot" — the watchdog is still
   watching, but there's nothing to watch. It waits for the next model
   to land.

### Process Architecture
```
com.horizons (main process)
  ├── UI (Compose)
  ├── NpuClient (HTTP bridge to daemon)
  ├── AgentLoop (agentic tool dispatch)
  ├── ChatHistoryStore, AppStateStore
  └── NPU_READY BroadcastReceiver

com.horizons:clifford (watchdog process)
  ├── CliffordService (FGS)
  ├── DaemonLauncher
  ├── NpuManager perf lock
  └── Thermal monitor (sysfs)

<daemon binary> (native process, detached)
  ├── HTTP server on 127.0.0.1:8080
  ├── Model engine (llama.cpp / ORT+QNN / etc.)
  ├── Tokenizer
  └── Sampler
```

Cross-process communication: CliffordService broadcasts `NPU_READY` intent
when the daemon is healthy. Main process receives it and activates NpuClient.
This was a bug in session 17 — CliffordService was calling
`app.activateNpuRuntime()` directly, which activated it in the WRONG
process's Application instance.

### Provider Abstraction (learned from off-grid-ai-mobile)

NpuClient must speak multiple protocols through a single interface:

```
LLMProvider interface
  ├── LocalOrtProvider    — POST /api/v1/generate (ort_engine protocol)
  ├── LocalLlamaProvider  — POST /v1/chat/completions SSE (OpenAI-compat)
  ├── CloudProvider       — POST to remote OpenAI-compat endpoint
  └── ShellProvider       — Termux command execution, stdout/stderr parsing
```

The generation service routes to the active provider based on:
1. User's explicit routing choice (RouterPane)
2. Routing rules engine (cloud when NPU unavailable, local for voice, etc.)
3. Model metadata (`engine` field on the downloaded model determines provider)

An ActiveModelService is the SOLE authority for loading/unloading models.
No other code path touches model state directly. This prevents the "two
writers to the same resource" bugs that off-grid-ai-mobile's architecture
explicitly guards against.

---

## 6. Hardware Topology

From the Research Dossier. Horizons is phone-first but mesh-aware.

### Primary Node: Motorola Razr Ultra 2025
| Spec | Value |
|------|-------|
| SoC | Snapdragon 8 Elite (SM8750) |
| NPU | Hexagon HTP v75, 45 TOPS INT8 |
| GPU | Adreno 830 |
| RAM | 16 GB LPDDR5X |
| Android | 15 (API 35) |
| Storage | 512 GB UFS 4.0 |

This is the ONLY node that matters for the initial release. Everything
else is future topology expansion.

### Secondary Nodes (Research Dossier, future)
| Node | NPU | TOPS | Role |
|------|-----|------|------|
| Rubik Pi 3 | QCS6490 Hexagon | 12 | Edge compute, always-on |
| Jetson Orin Nano Super | CUDA cores | 67 | Training, audit, PTL |

### Network (future)
- Tailscale over WiFi 6E: 1-2 Gbps, primary mesh link
- USB P2P RNDIS: 480 Mbps, secondary/backup
- WebSocket persistent connection layer (SocketSweep reference) for
  keep-alive between nodes

---

## 7. Model Hot-Swap Vault

Models the system should be able to load, run, and swap between. Not all
at once — the residency manager evicts based on RAM budget.

### Tier 1: Primary (NPU, compiled)
| Model | Size | Format | Quant | Runtime |
|-------|------|--------|-------|---------|
| Qwen3.5-9B | ~5.5 GB | QNN context binary | W4A16 | ort_engine |
| Gemma 4 12B QAT | ~6.0 GB | GGUF | Q4_0 | llama-server |

### Tier 2: Hot-swap (NPU, GGUF direct)
| Model | Size | Format | Quant | Runtime |
|-------|------|--------|-------|---------|
| Qwen3-4B | ~2.5 GB | GGUF | Q4_0/IQ4_NL | llama-server |
| Llama 3.1 8B | ~4.5 GB | GGUF | Q4_0 | llama-server |
| LFM2.5-Audio-1.5B | ~1.2 GB | GGUF | Q4_0 | llama-server |

### Tier 3: Sidecar (CPU/GPU, always-loaded or on-demand)
| Model | Size | Format | Runtime | Notes |
|-------|------|--------|---------|-------|
| Moonshine STT | ~80 MB | ONNX | sherpa-onnx | CPU/ARM NEON |
| Silero VAD | ~2 MB | ONNX | sherpa-onnx | CPU, voice activity |
| Kokoro-82M TTS | ~320 MB | ONNX | kokoro-onnx | Adreno GPU/Vulkan |

### Quant Compatibility (from the research papers)

**NPU-compatible (Hexagon HTP v75):**
- Q4_0: Full HTP offload via GGML_HEXAGON. Tile-aligned.
- IQ4_NL: Full HTP offload. Slightly better accuracy than Q4_0.
- W4A16 (QAI Hub): Per-group weight quantization. Verify group size
  alignment with HMX 32x32 tile geometry.
- QAT INT4: Pre-aligned during training. Best accuracy at INT4.

**CPU-only (NOT NPU-compatible):**
- Q4_K_M: K-quants unsupported on HTP. Falls back to CPU.
- Q5_K_M, Q6_K: Same — K-quant = CPU only.
- GPTQ: Not supported by GGML_HEXAGON.

This distinction is critical: the user's existing
`Mer0vin8ian/Qwen3.5-9B-VLM-Q4_K_M-GGUF` is a K-quant and will NOT
run on NPU. Q4_0 or IQ4_NL variants are needed for the GGUF+HTP path.

---

## 8. Agentic Layer

Horizons is not a chat wrapper. It is an agent that controls the phone.

### AgentLoop
22 tools, dispatched based on the model's tool-calling capability or
few-shot prompt wrapping (OverlayD-AI pattern). The agent loop:
1. User message → model generates response
2. If response contains tool calls → execute tools → feed results back
3. Repeat until model produces a final response (up to 5 iterations)
4. Tools can chain: "take screenshot → analyze with vision → tap element"

### Tool Categories
- **Device control:** accessibility actions, settings changes, app launch
- **Information:** screen capture, notification read, sensor data
- **File/data:** file read/write, clipboard, share sheet
- **Network:** HTTP fetch (cloud fallback path), API calls
- **Shell:** Termux command execution (Terminal tile surface)

### Vision Pipeline
- MediaProjection for screen capture (requires user grant)
- Accessibility service for element identification
- Vision model (MLLM) analyzes screenshots
- Few-shot prompts wrap observations into actionable commands

### Voice Loop
- Continuous mic capture (ASR indicator)
- Whisper STT (sherpa-onnx sidecar) or platform STT fallback
- Response → Kokoro TTS (GPU sidecar)
- Full conversation recorded into Chat tile history

---

## 9. UI Architecture

Seven tiles around a central hub. The FEATURE-SPEC
(`wiki/FEATURE-SPEC.md`) has the full visual spec. Key points:

| Tile | Purpose | What's Built | What's Missing |
|------|---------|--------------|----------------|
| HORIZONS | System identity | Done | — |
| MONITOR | Model library, prompt cache, API vault | Done | Console interface |
| CHAT | AI conversation (standard + live + condensed) | Done | Live mode (vision overlay) |
| ROUTER | Routing dashboard, provider selector | Done | Routing rules engine |
| ARTIFACTS | Archive of generated content | Placeholder | Real artifact storage |
| TERMINAL | Shell access, Matrix aesthetic | Done | Matrix waterfall theme |
| SETTINGS | Config, voice, themes, permissions | Done | Theme picker, voice selector |

Status indicators: ASR (green), LLM (blue), TTS (orange), MLLM (purple),
VAG (pink). States: solid bright = active, dim = idle, pulsing = init,
gray = unavailable.

Navigation: home grid default → tap tile for full-screen → back gesture
returns to grid. CORE_HUB crystal always accessible. Chat condensed overlay
pullable from any tile.

---

## 10. Reference Implementations

### off-grid-ai-mobile (upstream: off-grid-ai/off-grid-ai-mobile)
**What we took:** ~50% of the codebase. React Native app.
**Key patterns worth keeping:**
- Provider abstraction (`LLMProvider` interface + `ProviderRegistry`)
- ActiveModelService as single loading authority
- Model residency manager (clean vs dirty memory, eviction priority)
- Hardware-aware context sizing (`resolveSafeContext`: step down 4096→2048→1024)
- Download service with background recovery (reconcile on every foreground resume)
- Slot/hook registry for modular features (Pro system)
- Zustand stores as read-only UI projections, NOT state-of-truth

**Key differences from Horizons:**
- off-grid-ai runs inference IN-PROCESS (llama.rn native module, no daemon)
- off-grid-ai has no foreground service / watchdog pattern
- off-grid-ai is React Native; Horizons is native Kotlin + Jetpack Compose
- off-grid-ai has no NPU-specific path (CPU + GPU only via llama.cpp)

**Dependency management discipline** (observed in the developer's workflow):
Constant cycle of uploading dependencies → testing → uninstalling on
failure → stripping → repacking. Precise with ONNX syntax, ammo (AIMET)
syntax. Every dependency version pinned and verified before commit. This
discipline level is the standard for Horizons.

### OverlayD-AI (upstream: orailnoor/edge-overlay-ai)
**Architecture we adopt:** daemon binary + localhost HTTP + separate UI
**What we DON'T adopt:** Shizuku dependency (third-party, fragile, heading
for obsolescence). We use Android's own accessibility services and Termux
integration instead.

### scrcpy (upstream: Genymobile/scrcpy)
**Pattern we adopt:** boot/unboot service lifecycle. Start as boot service,
serve while needed, stop cleanly. CliffordService implements this pattern.
**Also:** Genymobile's ecosystem (Genymotion device player, Magisk fork)
for emulator testing and device control tooling.

### SocketSweep
**Pattern we adopt:** WebSocket persistent connection layer for
inter-device communication. Future use: keep-alive between mesh nodes
(phone ↔ Rubik Pi ↔ Jetson). Reference for the Tailscale + WebSocket
network mesh design.

### OB1 / Open Brain (upstream: NateBJones-Projects/OB1)
**What it is:** "One database, one AI gateway, one chat channel — any AI
plugs in." Infrastructure layer for unified AI access.
**Role in Horizons:** Persistence layer. Adopted after OpenWiki SKILL.md
system is proven. OB1 provides the single-database backing store that
SKILL.md files reference — knowledge persists across sessions, across
runtimes, across models.

---

## 11. Persistence & Knowledge System

### OpenWiki SKILL.md Architecture
Every buildable capability maps to a SKILL.md file. Layered:

```
skills/
  generic-android-build/SKILL.md    — How to build a generic Android APK
  gguf-runtime/SKILL.md             — How to run just a GGUF model
  onnx-runtime/SKILL.md             — How to run just an ONNX model
  multi-runtime/SKILL.md            — How to run all of them
  sm8750-npu/SKILL.md               — Chipset-specific: Hexagon HTP v75
  horizons-wiki/SKILL.md            — Project knowledge base
  project-memory/SKILL.md           — Session-persistent memory
  termux-mobile-dev/SKILL.md        — Phone-only dev environment
```

Each SKILL.md is a complete, standalone instruction set. A new session can
pick up any skill file and execute it without reading the entire project
history. This is "memory as a skill" — the project's knowledge is
organized as executable skills, not passive documentation.

### URL Glossary / KV Cache (planned)
The user has hundreds of valuable URLs extracted from Gemini research
sessions mixed with dead links. Plan:
1. Haiku CLI strips every URL from chat logs and repo content
2. URLs are validated (alive vs dead, relevant vs garbage)
3. Valid URLs organized into a KV cache system keyed by topic/capability
4. Any session can query the glossary: "what's the reference for Hexagon
   tile quantization?" → returns the paper URL + local notes

### Session Persistence
- CLAUDE.md: canonical project state, updated every session
- wiki/SESSION{N}-HANDOFF.md: per-session state transfer
- wiki/HORIZONS-BLUEPRINTS.md: THIS FILE, architectural truth
- OB1 (future): database-backed persistence layer

---

## 12. Build Pipeline

### CI/CD (GitHub Actions)

**build-apk.yml:**
1. Cross-compile ort_engine (CMake + NDK + QNN SDK)
2. Build Horizons APK (AGP 8.8.0, Kotlin 2.1.0, compileSdk 35, JDK 17)
3. Publish to `latest-debug` GitHub Release:
   - horizons-debug.apk
   - ort_engine (arm64-v8a binary)
   - libonnxruntime.so
   - sherpa AAR mirror

**build-llama-server.yml (planned):**
1. Cross-compile llama.cpp with GGML_HEXAGON=ON using Hexagon SDK toolchain
2. Produce: llama-server binary + libllama.so + libggml.so + libmtmd.so
3. Publish to same `latest-debug` release

**HF Jobs (compile pipeline):**
1. ONNX export on cpu-xl (RoPE fold, static shapes, M-RoPE fix)
2. QAI Hub compile (W4A16, qnn_context_binary)
3. Publish compiled model to `Mer0vin8ian/qwen3-5-9b-npu-sm8750`

### Release Artifact Exchange
The `latest-debug` GitHub Release is the binary exchange point between CI
and the device. The user downloads from the release page on the phone and
imports via ModelImportActivity. No adb, no laptop required.

---

## 13. What Makes Horizons Different

This is why we don't just fork off-grid-ai-mobile and call it done:

1. **NPU-first inference.** off-grid-ai runs on CPU/GPU. Horizons runs on
   the Hexagon NPU with hardware-aware quantization. This is not a
   checkbox — it's 10-50x performance difference for the right models.

2. **Daemon architecture.** off-grid-ai runs inference in-process (native
   module). Horizons runs a detached daemon with a watchdog. The daemon
   survives app restarts, the watchdog restarts the daemon, and the app
   never crashes because of an inference failure.

3. **Multi-runtime hot-swap.** off-grid-ai has two engines (llama.rn,
   ExecuTorch). Horizons has six runtime families, each as an independent
   replaceable binary. Adding a new runtime is "drop the binary, register
   it, done."

4. **Agentic phone control.** off-grid-ai is a chat app that happens to
   run locally. Horizons is an agent that controls the phone — screen
   vision, accessibility actions, shell commands, file manipulation,
   network requests.

5. **Mesh topology.** off-grid-ai is single-device. Horizons is designed
   for a network of devices — phone as primary, edge nodes for compute
   offload, training nodes for model refinement.

6. **Test-time scaling on NPU.** From the canonical research paper — 
   smaller models with batch>1 NPU inference can match larger models.
   This is a Horizons-specific capability enabled by the daemon
   architecture (the daemon controls batch size, the app doesn't need
   to know).

These features are non-negotiable. We take the best patterns from the
reference implementations, but we do NOT sacrifice these capabilities to
reduce implementation friction.

---

## 14. Evaluation Criteria for Integration Decisions

When choosing between implementation paths, evaluate in this order:

1. **Lock-in:** Does this path create a dependency on a third-party
   service, SDK, or library that could be deprecated, paywalled, or
   abandoned? (Shizuku: rejected. QAI Hub: accepted — it's a compile
   service, not a runtime dependency. llama.cpp: accepted — actively
   maintained, MIT licensed, we maintain our own fork.)

2. **Stability:** Has this path been proven on the target hardware? Not
   "should work in theory" — actually demonstrated. The three canonical
   papers all include empirical results on Snapdragon hardware.

3. **Friction:** How many setup steps, how many dependencies, how many
   configuration parameters? The off-grid-ai developer's discipline is
   the standard: if a dependency fails, strip it, repack, try again.
   Don't leave broken dependencies in the tree.

4. **Feature preservation:** Does this path sacrifice any of the six
   differentiators listed above? If yes, it's the wrong path regardless
   of how easy it is.

---

## 15. Repo Glossary (Key Forks)

| Fork | Upstream | Role |
|------|----------|------|
| `off-grid-ai-mobile` | `off-grid-ai/off-grid-ai-mobile` | Reference UI app, 50% code source |
| `llama.cpp` | `ggml-org/llama.cpp` | GGUF inference runtime |
| `llama.cpp-npu` | `haozixu/llama.cpp-npu` | NPU tile quantization (Paper 2a) |
| `edge-overlay-ai` | `orailnoor/edge-overlay-ai` | OverlayD-AI reference |
| `scrcpy` | `Genymobile/scrcpy` | Boot/unboot lifecycle reference |
| `nexa-sdk` | `qualcomm/GenieX` | Qualcomm's own inference runtime |
| `ai-hub-models` | `qualcomm/ai-hub-models` | QAI Hub model compilation |
| `sherpa-onnx` | `k2-fsa/sherpa-onnx` | STT/TTS/VAD via ONNX Runtime |
| `kokoro-onnx` | `thewh1teagle/kokoro-onnx` | Kokoro TTS engine |
| `snapdragon-npu-llm` | `avisre/snapdragon-npu-llm` | NPU LLM reference (31 tok/s) |
| `mlc-llm` | `mlc-ai/mlc-llm` | Universal LLM deployment |
| `EdgeAIApp-ExecuTorch` | `carrycooldude/EdgeAIApp-ExecuTorch` | ExecuTorch + QNN backend |
| `OB1` | `NateBJones-Projects/OB1` | Open Brain persistence layer |
| `openwiki` | `langchain-ai/openwiki` | SKILL.md documentation system |
| `anything-llm` | `Mintplex-Labs/anything-llm` | Local-first agent reference |
| `moonshine-tflite` | `moonshine-ai/moonshine-tflite` | Moonshine STT (TFLite) |

---

## Revision History

| Date | Session | Change |
|------|---------|--------|
| 2026-07-06 | 18 (blueprints) | Initial creation. Synthesized from Research Dossier, three canonical papers, off-grid-ai-mobile architecture analysis, OverlayD-AI guide, 50-repo library audit, and 17 sessions of project history. |
