# REPO FORK ASSET LIST — Reference

Source: "REPO FORK ASSET LIST DO NOT SKIP THIS" (Drive `1xz7WmtUZbYOrNomEFixWxobSroAb_vT8d9XTwiIxHks`, Google Doc, sitting directly in Claude_master_wiki). Category: **repo index / fork checklist**. This is the user's own master list of every GitHub repo relevant to the Omni Claw / Novus Agenti build. All repos on this list have been confirmed already forked under the `c10vis-poem` GitHub account (verified live against the account's 52 public repos this session) — this document is the index explaining *why* each one matters, not an outstanding to-do list.

## Important Assets

Core NPU/edge-inference tooling: `llama.cpp-npu`, `EdgeAIApp-ExecuTorch`, `mlc-llm`, `genymotion-device-web-player`, `off-grid-ai-mobile`, `qcom-build-utils`, `snapdragon-npu-llm`, `docker-pkg-build`.

## Important On-Device Layers (will be running)

`tmux`, `claude-code-android`, `termux-packages`, `OmniRoute`, `tmux-assistant-resurrect`.

## Very Important — Proof of Concept, Reverse Engineering, Troubleshoot Fallback, Research Guide

`off-grid-ai-mobile`, `VarientDemoApplication`, `Magisk`.

**User note on Genymobile**: "This is Genymobile, her entire repo and website deserve scraping. This will not be the last time you hear me mention her or see a resource of hers posted — if I were you I would task agents of scrubbing her entire online and GitHub libraries." (Not yet acted on — flagged as an open follow-up task, not something this project has done.)

`edge-overlay-ai`, `nexa-sdk` — user's own assessment: "I really don't think this works anymore but it could be worth looking at for reverse engineering and maybe it does support some active API key but I dk." This matches the independent live-verification finding recorded in `omniNeural-4b-status-correction.md` — Nexa's Hugging Face presence is effectively abandoned as of this session.

`scrcpy`.

## Resources

`qcom-build-utils`, `adk-docs`, `AI-Agents-Projects-Tutorials`, `trusted-firmware-a`, `ai-hub-apps`.

## Skills — "Don't sleep on these!"

`SuperClaude Framework`, `Claude-skills`, `leaked-system-prompts`, `Prompt-eng-interactive-tutorial`, `claude-code-best-practice`.

**Highlighted as probably the most important skill** — "teaches Claude how to reverse engineer Android apps": `claude-android-skill`, `android-reverse-engineering-skill`.

## Voice and Memory Layers

"Most likely get the version best suited to match, not sure of its tflite": `moonshine-tflite`, `silero-vad`, `Kokoro`. Memory layer: `OB1`, `reasoning-bank`.

## Honorable Mention

`kernel-topics`, `snagboot`.

## Cross-Reference: Repo READMEs Already Condensed Individually

Several repos from this list have their own dedicated reference docs elsewhere in this wiki, sourced from README exports found in the "ANDROID APK/RESEARCH DOCS" Drive folder (the "text reminders per fork repo" the user was referring to — a set of numbered `README (N) (Markor)` files, one per repo):

- `qcom-build-utils` — centralized build tooling for the Qualcomm Linux Debian package ecosystem (reusable GitHub Actions workflows, ABI checking, package promotion/release pipelines).
- `snapdragon-npu-llm` — runs real LLMs on Hexagon NPU including the "unsupported" Snapdragon 8 Gen 1 (Hexagon v69), built on a K9FxNa Hugging Face `.pte` artifact + ExecuTorch QNN backend; verified 31.3 tok/s on a OnePlus 10 Pro.
- `silero-vad` — pre-trained enterprise-grade Voice Activity Detector, MIT license, ONNX/PyTorch, sub-1ms per 30ms audio chunk on CPU, 8kHz/16kHz sampling, no telemetry/keys/lock-in.
- `off-grid-ai-mobile` — **this is the app on the user's phone** referenced earlier in this session ("underground LM" / model browser with storage indicator and local-server auto-discovery). Full offline AI suite (text/image generation, vision, voice, tool calling, document analysis) for Android/iOS/macOS. Runs Qwen 3, Llama 3.2, Gemma 3, Phi-4, or any GGUF; discovers and connects to local OpenAI-compatible servers (Ollama, LM Studio, LocalAI) automatically; on-device Stable Diffusion (NPU-accelerated on Snapdragon); Whisper STT; project knowledge base with on-device embedding + cosine retrieval in SQLite. MIT-licensed core, optional $50 lifetime Pro tier (custom personas, voice mode, calendar/email/MCP integration).
- `android-reverse-engineering-skill` — Claude Code skill (Apache-2.0) that decompiles APK/XAPK/JAR/AAR via jadx/Fernflower/Vineflower and extracts HTTP APIs (Retrofit endpoints, OkHttp calls, hardcoded URLs, auth patterns), traces call flows from UI through to network calls, handles ProGuard/R8 obfuscation.
- `EdgeAIApp-ExecuTorch` — on-device CLIP (OpenAI's vision-language model) via ExecuTorch + Qualcomm QNN backend on Android; zero-shot image classification, ~100-150ms inference, ~800MB RAM, requires Snapdragon 8 Gen 2/3/Elite (QNN v79).

## Relevance to This Project

This doc is the master index for everything in the GitHub repo-scraping phase of the project. Its own content maps directly onto Omni Claw's execution layers: the "on-device layers" section (`tmux`, `termux-packages`, `OmniRoute`, `claude-code-android`) matches the blueprint's Local Shell Loop; the "voice and memory layers" section (`silero-vad`, `Kokoro`, `OB1`, `reasoning-bank`) matches the Local Knowledge Synthesis backend already formalized in `omni-claw-knowledge-synthesis-architecture.md`; and `off-grid-ai-mobile` is a strong working precedent for Omni Claw's own model-browser/local-server-discovery UX (its "Remote LLM Servers" feature — auto-discover OpenAI-compatible servers on the local network — is effectively what Omni Claw's OmniRoute integration and cloud-fallback routing would need to replicate).
