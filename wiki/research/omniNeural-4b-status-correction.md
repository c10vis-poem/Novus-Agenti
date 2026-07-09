# OmniNeural-4B / Nexa SDK — Status Correction (2026-07-08)

Source: live verification by the user during this session, corroborated by their own note in "REPO FORK ASSET LIST DO NOT SKIP THIS" (Drive `1xz7WmtUZbYOrNomEFixWxobSroAb_vT8d9XTwiIxHks`). This supersedes/amends `omniNeural-4b-readme.md`, which was written faithfully from the official Nexa AI Hugging Face model card but did not know the project's current real-world status.

## What changed

The `omniNeural-4b-readme.md` entry in this wiki describes OmniNeural-4B as "a real, currently-shipping Nexa AI product" based on the official model card content. The user checked Nexa's Hugging Face presence directly during this session and found: the page is effectively abandoned — only one model listed, it won't even download, and the last visible activity was around December or January (i.e. several months stale as of this session's July 2026 date). The user's own aside in the REPO FORK ASSET LIST doc says the same thing independently: "nexa-sdk — I really don't think this works anymore but it could be worth looking at for reverse engineering and maybe it does support some active API key but I dk."

## Corrected status

**OmniNeural-4B and the Nexa SDK should be treated as effectively dead/abandoned, not as an active dependency or comparison point.** The `omniNeural-4b-readme.md` document's content (features, benchmarks, architecture) should still be kept as a historical record — it's useful for understanding what AESOP's Edge Model Guide and the early Omni Claw blueprint discussions were referencing at the time those docs were written — but nothing in the eventual Omni Claw build should assume Nexa's SDK, model weights, or API are reachable or maintained going forward. Any future NPU-aware multimodal model choice should be evaluated fresh rather than defaulting to OmniNeural.

## Relevance to this project

This confirms a pattern worth watching for elsewhere in this ingestion project: several of the AI-generated/Gemini-sourced source documents (AESOP, the early Omni Claw blueprint discussion) reference specific vendor SDKs/models as if they're stable dependencies, when in practice the small-model/NPU-tooling space moves fast enough that a tool referenced even a few months earlier may already be abandoned. The `nexa-sdk` GitHub repo is also on the REPO FORK ASSET LIST as a "resources" entry, explicitly flagged by the user as possibly worth reverse-engineering rather than using live — that framing (dead-but-worth-studying-for-parts) should carry over to how it's treated if/when it's eventually cloned.
