# Novus Agenti · Omni Claw

**A fully on-device agentic AI assistant for the Motorola Razr Ultra 2025.**
Inference runs on the phone's NPU — no cloud LLM in the main runtime, no CPU
fallback for the target model. *"The unprecedented driving force."*

> Cl0vis × Mer0vin6ian · Project POEM. App package: `com.horizons`. Codebase
> banner: **Omni Claw**.

---

## What this is

Novus Agenti treats apps, terminals, and cloud runtimes as modular
subroutines and orchestrates them from the phone. Heavy math (model inference)
is split off from the UI: a lightweight **Kotlin app** drives a **detached
native inference daemon** that runs the model on the **Hexagon NPU**, and the
two talk over a local loopback HTTP socket.

**Target hardware:** Snapdragon 8 Elite `SM8750` · Adreno 830 · **Hexagon HTP
v79** · 16 GB. Phone-only build; `arm64-v8a`.

## Architecture

```
┌─────────────────────────┐        ┌──────────────────────────────┐
│  Kotlin app (com.horizons)│  HTTP  │  detached inference daemon    │
│  Compose UI · AgentLoop   │◄──────►│  runs the model on Hexagon NPU│
│  NpuClient · CliffordSvc  │ :8080  │  (GenieX ⟶ HTP SDK / QAIRT)   │
└─────────────────────────┘        └──────────────────────────────┘
        │  CliffordService (watchdog) keeps the daemon alive:
        │  START_STICKY · specialUse FGS · exponential-backoff relaunch
```

- **App** — Jetpack Compose UI, an `AgentLoop` with a tool layer, `NpuClient`
  as the bridge to the daemon, `DaemonLauncher` to spawn it detached.
- **Daemon** — serves the model behind `127.0.0.1:8080`. The daemon is
  **serve-first**: it binds the port immediately and reports "alive but not
  ready" (`/health` 503) while the model loads, so the watchdog never
  crash-loops it. (See `wiki/GENIEX-DAEMON-PLAN.md` and `daemon/src/`.)
- **Watchdog** — `CliffordService` (CLIFFORD == Watchdog): a foreground
  service that relaunches only a genuinely dead daemon, with backoff and a
  strike cap.

### Runtime (in transition)

The runtime behind the socket is a swappable, uploadable binary. Current
direction (decided session 15): **backend = HTP SDK / QAIRT, runtime =
[GenieX](https://github.com/qualcomm/GenieX)**, run as `geniex serve` (an
OpenAI-compatible server) behind the same watchdog. The legacy `ort_engine`
C++ daemon (ONNX Runtime + QNN EP) remains as a fallback runtime. See
`wiki/GENIEX-DAEMON-PLAN.md`.

## The model

[`Mer0vin8ian/Qwen3.5-9B`](https://huggingface.co/Mer0vin8ian/Qwen3.5-9B)
→ Hexagon HTP. Two supported paths under GenieX: a **Q4_0 GGUF** (Q4_0 is
Qualcomm's recommended precision for Hexagon), or a **Qualcomm AI Hub bundle**
produced by `scripts/compile_qwen3_5_9b.py`. Size envelope target **≈ 5.5 GB**
(hard redline 7.0–7.2 GB).

## Repo layout

| Path | What |
|---|---|
| `horizons/` | the Android app (`com.horizons`) — UI, agent loop, NpuClient, CliffordService, DaemonLauncher |
| `daemon/` | `ort_engine` C++ inference daemon (legacy runtime; CI-built) |
| `scripts/compile_qwen3_5_9b.py` | ONNX export + QAI Hub compile pipeline |
| `models/manifest.yaml` | model + runtime manifest |
| `knowledge/` | byte-faithful hand-distilled research corpus (frozen — see its README) |
| `wiki/` | architecture notes, runtime plans, session handoffs |
| `rules/`, `agents/`, `skills/` | operating rules, sub-agent briefs, agent skills |
| `.github/workflows/build-apk.yml` | builds the APK + daemon, publishes a debug release |

## Build

AGP 8.8.0 · Kotlin 2.1.0 · compileSdk 35 · minSdk 31 · JDK 17 · `arm64-v8a`.
CI (`build-apk.yml`) cross-compiles the daemon, builds the APK, and publishes
both to a `latest-debug` GitHub Release. Signing uses `release/debug.keystore`
(committed by design for debug builds).

## Status (honest)

- App: **scaffolded** and building; UI, watchdog, and daemon bridge exist.
- Daemon: `ort_engine` **builds in CI**; not yet verified on a physical
  HTP v79 device against a real compiled model.
- Compile pipeline: ONNX export / QAI Hub compile (**Job 8**) pending.
- GenieX runtime integration: **in progress** (planning + repo wiring; the
  on-device install/run is done via Termux on the target phone).

Not a shipping product yet — this is an active build.

## More context

Everything an agent or contributor needs to resume is in **`CLAUDE.md`**
(full project context + live state) and the **`wiki/`** handoffs. The
canonical repo is `c10vis-poem/Novus-Agenti`.
