# Overlayd-AI: Technical Architecture Guide

Source: "Copy of overlayd-ai-technical-guide" (Drive `16a40fUUaBnTuZ98mam40ZvGeCS6PEjh-`, located in the PROOF/REVERSE ENGINEERING MATERIAL subfolder of RESEARCH DOSSIER {CANNON}). Category: **proof-of-concept case study** — this is exactly the kind of "ironclad proof that something similar has already been built" material the user's content roadmap calls for. Subtitle: "Offline AI-Powered Android Device Control." Version 1.0, 2024. No external URLs found in this document (internal `[#N]` markers are page-anchor references, not links).

## 1. The Core Problem: Android Sandbox Architecture

Android's security model isolates every app inside a sandbox to prevent malicious apps from accessing system resources or interfering with other apps. Termux, despite being a powerful Linux environment emulator, runs within these same constraints — any process inside Termux, including AI models, is trapped within the sandbox boundary. Consequences:

- **Screen Access**: AI cannot read or interact with the device's display.
- **App Control**: cannot launch, close, or manipulate other apps.
- **System Settings**: cannot toggle WiFi, Bluetooth, or modify system preferences.
- **Hardware Control**: limited access to sensors, cameras, other hardware.

True AI-driven device automation requires "breaking out" of this sandbox while maintaining system stability and security.

## 2. Breaking the Sandbox: Shizuku & Rish

**Shizuku** (Privilege Broker) — an app that uses Android's Wireless Debugging feature to acquire ADB permissions without root access.

**Rish** (Command Wrapper) — a special shell command exported from Shizuku into Termux that executes commands with elevated system privileges.

The "God Mode" execution model: once the necessary files are exported from Shizuku into Termux, the `rish` command becomes available, and any command wrapped in `rish` executes with system-level privileges:

```bash
# Force home screen navigation
rish -c "input keyevent 3"

# Launch YouTube application
rish -c "monkey -p com.google.android.youtube 1"

# Toggle WiFi state
rish -c "svc wifi disable"
```

This is the critical bridge out of Termux: the AI, running in the sandbox, can execute commands that directly manipulate the Android system through the `rish` tunnel.

## 3. The Brain: llama.cpp Inference Engine

**Why not Python frameworks or cloud APIs**: Python frameworks add interpreted-execution overhead and higher memory use; cloud APIs require internet, add latency, and raise privacy concerns; generic binaries are suboptimal for the target CPU architecture.

**Solution**: automatically download and compile **llama.cpp** (pure C/C++ inference engine), using `cmake` to target the device's specific processor architecture for near-optimal execution speed. Configured to run as a persistent server exposing an OpenAI-compatible API on `localhost:8080`, so any component in the stack can talk to the AI over standard HTTP.

```bash
# Server startup configuration
./server -m model.gguf \
  --host 127.0.0.1 \
  --port 8080 \
  -c 4096 \
  --timeout 300
```

## 4. The Logic Glue: Node.js & Telegram Bridge

A lightweight JavaScript bridge (`telegram_bot.js`, Node.js) maps user chat messages to Shizuku/`rish` commands — the orchestration layer between natural-language input and system-level execution.

**Command processing flow**: User Input → Telegram Bot → Few-Shot Prompting → LLM Inference → Command Parsing → Rish Execution → System Action.

**Few-shot prompting strategy, step by step**:
1. Telegram bot receives natural-language command (e.g. "Open YouTube").
2. Node.js constructs a prompt with few-shot examples mapping intents to ADB commands.
3. Local AI processes the prompt, returns a structured response.
4. Response parsed for a `CMD:` prefix, raw command extracted.
5. Command wrapped in the `rish` tunnel and executed with system privileges.

**Example execution trace**:
```bash
# User sends: "Open YouTube"
# AI Response Format: CMD: monkey -p com.google.android.youtube 1
# Node.js Processing:
const command = aiResponse.replace("CMD:", "").trim();
const rishCommand = `rish -c "${command}"`;
exec(rishCommand);
# Result: YouTube launches on device
```

Achievement claimed: fully offline AI control — YouTube launched using only local resources, no internet connection required; the entire inference pipeline runs natively on-device.

<!-- Note on code safety: the `exec(rishCommand)` pattern interpolates a raw command string from LLM output directly into a shell exec call. This is a command-injection-shaped pattern if the LLM's output were ever adversarially influenced (e.g. via prompt injection from screen content in the vision pipeline below) — worth flagging if this pattern is ever adapted into this project's own build, since OmniRoute's own CLAUDE.md hard rules call out exactly this class of risk (never string-interpolate untrusted values into shell exec calls). -->

## 5. Vision Integration: OpenClaw & Multimodal AI

Qwen (the vision model used here) is multimodal — processes and understands visual information alongside text, letting the AI "see" the screen and decide based on visual context.

**OpenClaw** is integrated as a third-party automation framework providing: rapid screenshot capture (high-frequency, for real-time visual analysis), UI layout analysis (extraction of interactive elements and coordinates), element detection (buttons, text fields, other UI components).

**The OpenClaw-Local Bridge**: OpenClaw is designed to connect to ChatGPT over the internet, which conflicts with the offline-first requirement. Solution: a custom executable `openclaw-local` intercepts OpenClaw's environment paths and redirects all API calls to the local llama.cpp server:

```bash
# Intercepted endpoint configuration
BASE_URL = "http://127.0.0.1:8080/v1"
API_KEY = "sk-local-offline"  # Dummy key for compatibility

# All OpenAI-compatible endpoints redirected:
# - /v1/chat/completions
# - /v1/models
# - /v1/embeddings
```

**Vision-to-action pipeline**: Take Screenshot → UI Analysis → Send to LLM → Vision Inference → Extract Coordinates → Rish Tap Command. Result: a closed-loop system where OpenClaw captures the screen, feeds visual data entirely offline into the local llama.cpp engine, determines target UI element locations, and uses `rish` to tap them natively.

## 6. System Architecture Overview

Layered stack:

| Layer | Component | Function |
|---|---|---|
| User Interface | Telegram Bot | Natural-language input/output interface |
| Orchestration | Node.js Bridge | Prompt engineering, command parsing, execution flow |
| Orchestration | OpenClaw Local | Visual capture, UI analysis, coordinate extraction |
| AI Inference | llama.cpp + Qwen | Local LLM inference, multimodal understanding |
| Privilege | Shizuku + Rish | ADB privilege acquisition, command execution |
| System | Android APIs | Hardware control, app management, settings |

## 7. Executive Summary

"We are deploying a lightweight C++ AI inference server directly on the mobile device, communicating with it through a Telegram bot interface running in the background. By granting this system Shizuku (Developer) privileges, the AI can break out of the command terminal sandbox and natively control phone settings, applications, and hardware — all while completely disconnected from the internet."

**Key claimed achievements**: 100% offline operation (no data leaves the device, all inference local); native C++ optimization (architecture-specific compilation for max inference speed); no root required (leverages official Android debugging features only); vision-capable multimodal AI (sees, understands, and interacts with on-screen elements).

<!-- UNVERIFIED: this is the project's own technical marketing document; performance/capability claims are as stated by the authors, not independently benchmarked here. -->

## Relevance to this project

This is one of the strongest available proof-of-concept precedents for the entire Omni Claw / Novus Agenti vision — it demonstrates, with a named working architecture, several of the exact mechanisms Omni Claw's blueprint describes as design intent rather than confirmed code:

- **Sandbox-breaking via privilege escalation** (Shizuku/Rish, no root) is a concrete, real answer to how Omni Claw's Kotlin app could gain the "System Actions" / Accessibility-Service-class capabilities described in the Agentic Capabilities Map (`omni-claw-blueprint.md` §3), via an alternate mechanism (ADB-based privilege broker) instead of/alongside Accessibility Service.
- **A local llama.cpp OpenAI-compatible server on `127.0.0.1:8080`** is the same core pattern as Omni Claw's C++ daemon `ort_server` on `127.0.0.1:8080`, and the same pattern used throughout the `llm-wiki-termux-setup` skill package — strong convergent validation that this is the standard shape for on-device local inference serving.
- **OpenClaw's vision-to-action pipeline** (screenshot → UI analysis → LLM → coordinate extraction → tap) is a real, working version of Omni Claw's "Triple-Mode Screen Vision Framework" Mode 2 (Contextual Point-and-Shoot), and is also the same "OpenClaw" project named in the Final Memory Layer/P2P doc as one of the repos the user is weaving together (there recalled as "Mimir Claw / OpenClaw," the dual-agent session persistence engine) — this document is likely the clearest existing description of what that OpenClaw integration actually does.
- The flagged command-injection-shaped pattern in §4 (raw LLM output string-interpolated into `exec()`) is exactly the class of risk OmniRoute's own hard rules explicitly warn against — worth keeping in mind if any of this gets adapted into the user's own build, so the eventual implementation passes untrusted values via safer mechanisms (e.g. argument arrays, not string interpolation).
