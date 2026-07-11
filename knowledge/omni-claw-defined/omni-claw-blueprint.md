# Novus Agenti {Omni Claw} — Master Vision Blueprint

Source: "Copy of Welcome to the birth of Novus Agenti - {Omni Claw}." (Gemini session export, Drive `1j0lQM69h4eENLX2LKWtRilyQZL1aScyA`). Category: **master architecture reference** — this is the target system; AESOP (see `aesop-full-reference.md`) is its current Termux-scoped implementation.

**Note on source reliability**: this is a Gemini brainstorming/design session, not verified working code — treat every mechanism below as design intent. The transcript's closing section (a long back-and-forth about how to export the chat thread out of the Gemini app) is meta-noise about capturing the conversation, not architecture, and has been excluded from this cleaned version.

## 1. Project Overview

Novus Agenti ("Omni Claw") is a localized, hyper-customized mobile operating environment that treats individual apps, terminal endpoints, and cloud runtimes as modular subroutines. By separating heavy math (model inference) from the UI layer, the phone doesn't act as a simple app container — it acts as an iron-clad local orchestrator for a multi-agent neural mesh.

Core stack: a **Kotlin UI app** (lightweight frontend orchestrator) paired with a **detached native C++ background daemon** (`ort_server`) that runs a **6.2GB Gemma-4 12B QAT model** on the phone's **Hexagon NPU**. The two communicate over a **local loopback WebSocket/HTTP connection at `127.0.0.1:8080`**.

Qualcomm QNN SDK static graph partitioning splits the workload across the SoC: ~5.5GB to the Hexagon NPU, ~0.6GB to the Adreno GPU for custom layers, ~0.1GB to the CPU for housekeeping.

## 2. Core Architecture — The Watchdog Recovery Bridge

To keep the system bulletproof during heavy runtime operations, an isolated **Watchdog Application** layer runs alongside the primary Kotlin UI without the core model weights ever being aware of it.

```
[Android Low Memory Killer (lmkd)] - Ignores Core Daemon
 |
 [Watchdog App] ---> [Local Loopback TCP: 127.0.0.1:8080] <--- [C++ Engine Daemon]
 |                                                              (Unkillable -950)
 +---> Monitors App State & Shared Memory Arena
```

- **The Sandbox Blindspot**: the 6.2GB model runs fully containerized inside the C++ daemon context, communicating purely via JSON over loopback. It has zero awareness of Android UI lifecycle, layout state, or Kotlin process constraints.
- **The Skeleton Crew Failback**: the Watchdog is a featherweight foreground service (~15MB) monitoring the process lifecycle of `NpuClient.kt`. If a memory spike or un-fused tensor forces a foreground UI crash, the Watchdog intercepts the teardown instantly.
- **Invisible Hot-Reboot**: because the C++ engine is anchored to the root init tree at a privileged **`-950 oom_score_adj`** score, it's unaffected by a foreground UI crash. The Watchdog reads the engine's persistent log cache, captures the exact state sequence, and silently hot-reboots the Kotlin Orchestrator container.
- **State Preservation via WebSockets**: a local loopback WebSocket connection continually pushes JSON state frames to a micro-database in local storage. Conversation history, active variables, and pipeline paths are recovered in milliseconds without reloading the 6.2GB tensor graph.

## 3. Agentic Capabilities Map

*(Built per user request — a structured inventory of the distinct agentic capabilities described across the session, not just prose.)*

| Capability | Trigger / Entry Point | Mechanism | Output / Effect |
|---|---|---|---|
| Global voice dictation | Standard Floating Microphone Tile, tap anywhere | Raw speech → local low-latency cleanup pass (strips filler/stutter/typos) → Android Accessibility Service | Types pristine text directly into active cursor field; no reasoning, no tool calls |
| Meta-prompt compilation (verified) | Internal mic icon inside AI Chat Floating Box | Speech → Silero VAD isolates audio → local HTTP/loopback stream to C++ engine → engine pulls Obsidian/Markor skill blocks → compiles structured meta-prompt | Streams back into the AI Chat Box as **editable text** — human reviews/edits before send, never auto-dispatched |
| Local file operations | Model emits tool-call JSON | Android IPC Intents → Tasker Relay (`intent://com.tasker.TRIGGER`) | Read/write Markdown in Markor or Obsidian vaults, scan directory trees, file sorting |
| System actions | Model emits tool-call JSON | Android Accessibility Service frameworks | Launch camera, initiate downloads, pull git repos — ~150MB app overhead target |
| Local shell execution | Agent needs bash/git/compile | Termux GLIBC-patched environment, authenticated JSON-RPC loopback on port `8022` | Bash shortcuts, Python data transformers, `patchelf-glibc` binary patching, GitHub/Hugging Face repo interaction |
| Cloud compute offload | Task needs heavy compute/web | Google Colab CLI with `--auth adc` (Application Default Credentials) | Headless remote T4 GPU provisioning (`colab new --gpu T4`), script transmission (`colab exec`), remote file infra management |
| Agnostic cloud API routing | Task needs external LLM/API | OpenAI-compatible translation layer over public tunnels | Scale-out to OpenRouter, fire scripts to remote worker nodes |
| Fast-pass reasoning (thinking suppression) | Any SSE token stream from a reasoning model (e.g. DeepSeek-R1) | C++ token parser intercepts `<think>`/`</think>` tags | Emits `{"status":"thinking"}` then `{"status":"generating"}` JSON frames instead of raw CoT dump — Claude-style "Thinking..." shimmer |
| Continuous screenshot parsing | Background, automatic | AOSP screen capture loop ingesting frames over time | Maps multi-app pipeline navigation (e.g. Obsidian → Markor → Drive → GitHub), flashes highlighted guides on workspace |
| Contextual point-and-shoot vision | Camera icon in AI Chat Floating Box | Single-frame screen capture + verbal prompt | Step-by-step troubleshooting directions for dense UI (e.g. Google Cloud Console IAM dashboards), detects UI/layout drift over time |
| Local storage directory sniffer | Automatic, standard Android screenshot gesture | File observer watching `/sdcard/Pictures/Screenshots/` | Pulls raw image bytes into model context instantly, zero manual upload |
| Session/share export | Share icon in AI Chat Floating Box | Bundles code fragments/terminal output/scripts | Exports directly to Google Drive or Google Docs |
| Multi-session management | Carbon Tabs in AI Chat Floating Box | Session/tab state switching | Hot-swap between separate conversation sessions or engineering branches |
| Crash recovery | Automatic, background | Watchdog service + WebSocket state frames | Hot-reboot Kotlin UI without reloading model weights |
| Multi-window / multi-device orchestration | Split-screen or TigerVNC cast (e.g. to Tab S9 FE at port `5901`) | 3-panel layout mimicking Cloud Shell Editor | Parallel cloud/vision + local shell + chat orchestration across devices |

## 4. The Dual-Tile Floating UI

Corrected mid-session by the user — the two tiles have **strictly separated scopes** to prevent accidental command dispatch:

**Tile A: Global Microphone Tile**
- Scope: system-wide, any active Android input field (Gboard-replacement behavior)
- Behavior: raw voice → local cleanup pass (filler words/stutters/typos removed) → Accessibility Service types directly into the active cursor field
- No reasoning, no meta-prompting, no tool calls — pure high-fidelity dictation

**Tile B: AI Chat Floating Box** (expandable sandbox)
- `[+]` Upload Tray — attach local files/schemas/images to context
- `[📷]` Camera Icon — point-and-shoot screen capture for vision troubleshooting
- `[🎙]` Internal/Sandbox Microphone Icon — dedicated agentic-command dictation (separate from Tile A)
- `[⏸]` Pause Button — freeze token streaming / halt cloud execution mid-flight
- `[📑]` Carbon Tabs — organize/hot-swap separate conversation sessions or engineering branches
- `[⬆]` Share Icon — bundle code/logs/output, export to Drive or Docs

**The Meta-Prompt Verification Loop** (the human-in-the-loop safety mechanism):
1. Tap internal mic in AI Chat Box, speak rough high-level intent
2. Silero VAD (sub-5MB ONNX, 30ms evaluation loop) isolates the speech
3. Raw text/audio routes over local loopback to the C++ engine
4. Engine synthesizes the request against skill blocks/API schemas/context pulled from local Obsidian/Markor directories — **this is the slot the OpenWiki+OB1+reasoning-bank memory system fills**
5. Compiled meta-prompt streams back into the AI Chat Box as **editable text**, not auto-executed
6. Human reviews, edits, clips parameters, or flags corrections
7. Manual send dispatches to the target local/cloud execution endpoint

## 5. Multi-Window Neural Mesh Layout

Whether in split-screen on a tablet or cast via TigerVNC to a secondary display (e.g. Samsung Tab S9 FE at port `5901`), the layout mimics a Cloud Shell Editor environment:

| Panel | Runtime | Purpose |
|---|---|---|
| Panel 1: Cloud & Vision Space | Android WebView / local browser | Live browser instances, remote Colab dev loops, cloud buckets, visualization graphs |
| Panel 2: Local Command Cockpit | Patched Termux GLIBC terminal multiplexer | Runs terminal CLIs side-by-side (e.g. `claude-code` CLI on Pro subscription alongside a DeepSeek-R1 logic solver via `ANTHROPIC_BASE_URL` override) |
| Panel 3: Omni-Claw Chat Panel | Kotlin frontend orchestrator client | Central conductor — STT input, thinking-state display, execution logs, file trays |

## 6. Local vs. Cloud Agentic Tool Execution Map

```
                +---> LOCAL LOOP:  Kotlin App -> Tasker Relay -> Local Files / Obsidian
[Omni-Claw Agent] ---+
                +---> CLOUD LOOP:  Termux Shell -> Colab CLI (ADC) -> Remote Compilation / Git
```

| Execution Zone | Integration Layer | Capabilities |
|---|---|---|
| Local Device Loop | Android IPC Intents & Tasker Relay | Fires payloads to `intent://com.tasker.TRIGGER` — read/write Markor/Obsidian, file sorting, native system status |
| Local Shell Loop | Termux GLIBC-patched environment | Authenticated JSON-RPC loopback on port `8022` — terminal workspace nav, bash shortcuts, `patchelf-glibc`, git hooks |
| Cloud Engine Loop | Google Colab CLI (`--auth adc`) | Headless T4 GPU provisioning (`colab new --gpu T4`), Python script transmission (`colab exec`), Qualcomm AI Engine SDK model compilation |

**Workload distribution rationale**:

| Function | Handler | Why |
|---|---|---|
| Floating windows & overlays | Kotlin (native) | Android WindowManager handles smooth persistent rendering over other apps |
| Screen vision ingestion | Kotlin (native) | Native AOSP capture loops pull frame bytes into memory faster than any script |
| Bash/git/CLI compiling | Termux (headless socket) | Native Linux ecosystem compiled for Android without OS restrictions |
| System automation & macros | Tasker/MacroDroid intents | Rapidly alters device settings, automates third-party UI steps via clean intent pipelines |

## 7. Triple-Mode Screen Vision Framework

```
              +---> Mode 1: Continuous Screenshot Parsing (AOSP Accessibility API Loop)
              |
[Screen Capture] -+---> Mode 2: Contextual Point-and-Shoot (Floating Chat Camera Icon)
              |
              +---> Mode 3: Local Storage Directory Sniffer (Automated File Observer Loop)
```

- **Mode 1 — Continuous Screenshot Parsing**: background AOSP screen capture loop ingests frames over time, tuned for structural navigation mapping across multiple interconnected apps (e.g. walking a user through an Obsidian → Markor → Drive → GitHub pipeline), cross-referencing the current step against the visual UI.
- **Mode 2 — Contextual Point-and-Shoot**: triggered only by the Camera Icon in the AI Chat Floating Box. Single-frame capture + verbal prompt, for troubleshooting dense cloud console/IAM-style dashboards. Detects UI/layout drift over time (e.g. a dated post-update navigation change) and reroutes accordingly.
- **Mode 3 — Local Storage Directory Sniffer**: fallback loop for when continuous streaming throttles performance. Watches `/sdcard/Pictures/Screenshots/`; the moment a hardware-button screenshot lands, the app pulls the raw image bytes straight into the model's visual context — zero manual upload lag.

## 8. Fast-Pass Reasoning (Thinking Token Suppression)

To avoid the interface feeling slow reading through raw chain-of-thought text before an answer prints, the C++ engine intercepts reasoning-model tag streams at the parser layer:

```
[Raw Streaming Tokens] ---> [C++ Token Parser Interceptor]
    |
    +---> Detects `<think>`  ---> Emits `{"status": "thinking"}`  (drops raw text)
    +---> Detects `</think>` ---> Emits `{"status": "output"}`    (streams live text)
```

Because the C++ daemon manages the SSE token stream over `127.0.0.1:8080`, it can block raw reasoning text at the source: the moment the opening `<think>` tag is seen, the daemon drops the text and emits a single lightweight frame; the Kotlin client shows a clean animated "Thinking..." indicator; the moment `</think>` appears, the state flips back and finalized answer tokens stream through normally.

## 9. Corrections Made Mid-Session (design decisions, not just brainstorm)

The user explicitly corrected two things during this session — worth preserving as locked-in decisions, not just one of several floated ideas:

1. **Tile separation is strict.** The global microphone tile must behave *exactly* like a Gboard-style dictation replacement — clean typo/filler correction, direct text injection, **no meta-prompting, no reasoning**. All meta-prompt compilation happens only via the internal mic inside the AI Chat Floating Box, specifically so the user can verify/edit before anything is dispatched.
2. **Thinking tokens should be suppressed**, not displayed verbatim — a simple "Thinking..." indicator (Claude-style) is preferred over dumping raw chain-of-thought text, since reading through it just slows the user down without adding value if the eventual answer quality doesn't depend on seeing it.

## 10. Open Questions / Not Yet Resolved

These were raised in the session but not locked down — flag as open design decisions:
- Whether local tool use/inference should route entirely through Tasker/Termux, or whether Android runtimes/Kotlin build code can streamline/replace parts of that split (the session leans toward the hybrid Kotlin-broker + Termux-headless-daemon + Tasker-macro-glue split described in §6, but this was presented as the recommended answer, not something the user had independently confirmed elsewhere).
- Exact desktop/laptop version of the app is described as "a future version" — not scoped in this session.
- P2P capabilities between the user's Motorola and other AI Dev SoCs/SoMs are mentioned as a target capability but not architected here.

## Relationship to AESOP

See `aesop-full-reference.md` for the detailed side-by-side. In short: AESOP is confirmed (by the user) as the **Termux-nerfed subset** of this vision — same voice-first / meta-prompt-compilation / device-profile concept, but running inside Termux+PRoot-Ubuntu with cloud Claude API delegation instead of a native Kotlin app with local Gemma-4 NPU inference, no Watchdog, no floating tiles, no screen vision, and no Tasker/Colab tool-execution split.

## Relationship to the OpenWiki + OB1 + reasoning-bank memory system

Section 4's "Local Knowledge Synthesis" step — the C++ engine pulling "skill blocks, API schemas, and historical code contexts directly out of your local Obsidian or Markor directories" before compiling a meta-prompt — is the exact function the wiki/memory synthesis project (see separate integration architecture doc, in progress) is meant to formalize and back with real infrastructure: OB1's `thoughts` table + remote MCP as the actual store, OpenWiki's `SKILL.md` convention as the maintenance layer, reasoning-bank's success/failure memory-item pattern as the retrieval strategy — replacing ad hoc "read some Obsidian files" with a structured, queryable memory backend.
