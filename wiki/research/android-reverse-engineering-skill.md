# android-reverse-engineering-skill — Reference

Source: `c10vis-poem/android-reverse-engineering-skill` (fork of `SimoneAvogadro/android-reverse-engineering-skill`), README.md, cloned and read directly from GitHub. Apache-2.0. Category: **Claude Code skill — Android reverse engineering / API extraction**. Flagged by the user as one of the two "most important skills" on the whole repo-fork list.

## What it does

Decompiles Android APK/XAPK/JAR/AAR files and extracts the HTTP APIs an app uses — Retrofit endpoints, OkHttp calls, hardcoded URLs, authentication headers/tokens — so those APIs can be documented and reproduced without the original source code.

| Capability | Description |
|---|---|
| Decompile | APK/XAPK/JAR/AAR via jadx and Fernflower/Vineflower (single engine or side-by-side comparison) |
| Extract APIs | Retrofit endpoints, OkHttp calls, hardcoded URLs, auth headers/tokens |
| Trace call flows | Activities/Fragments → ViewModels → repositories → HTTP calls |
| Analyze structure | Manifest, packages, architecture patterns |
| Handle obfuscation | Strategies for navigating ProGuard/R8 output |

## Requirements

Java JDK 17+, jadx (CLI). Optional: Vineflower or Fernflower (better output on complex Java), dex2jar (needed to run Fernflower on APK/DEX files directly).

## Usage

Slash command: `/decompile path/to/app.apk` runs the full workflow (dependency check → decompile → structure analysis). Also activates on natural language ("Decompile this APK", "Extract API endpoints from this app", "Follow the call flow from LoginActivity").

Manual scripts (all under `plugins/android-reverse-engineering/skills/android-reverse-engineering/scripts/`, bash + experimental PowerShell): `check-deps.sh`, `install-dep.sh jadx|vineflower`, `decompile.sh app.apk` (add `--engine fernflower` or `--engine both --deobf`), `find-api-calls.sh output/sources/` (add `--retrofit` or `--urls`).

## Repository structure

```
android-reverse-engineering-skill/
├── .claude-plugin/marketplace.json
├── plugins/android-reverse-engineering/
│   ├── .claude-plugin/plugin.json
│   ├── skills/android-reverse-engineering/
│   │   ├── SKILL.md                 # Core workflow (5 phases)
│   │   ├── references/              # setup-guide, jadx-usage, fernflower-usage,
│   │   │                             # api-extraction-patterns, call-flow-analysis
│   │   └── scripts/                 # check-deps, install-dep, decompile, find-api-calls (.sh + .ps1)
│   └── commands/decompile.md        # /decompile slash command
```

## Install

Claude Code: `/plugin marketplace add SimoneAvogadro/android-reverse-engineering-skill` then `/plugin install android-reverse-engineering@android-reverse-engineering-skill`. Or clone locally and point the marketplace add at the local path.

## Disclaimer (from source, carried over faithfully)

Provided strictly for lawful purposes: security research and authorized penetration testing, interoperability analysis permitted under applicable law (EU Directive 2009/24/EC, US DMCA §1201(f)), malware analysis/incident response, education/CTF. User is solely responsible for legal compliance; unauthorized reverse engineering of software you don't own/have permission to analyze may violate IP law and computer fraud statutes.

## Relevance to this project

This is the direct execution engine for the "off-grid-ai-mobile" and general Android-app API-reverse-engineering research thread already in this wiki (`repo-fork-asset-list.md` Cross-Reference section). Where `claude-android-skill` teaches Claude how to *build* a NowInAndroid-style app, this skill teaches Claude how to *reverse* an existing one — directly useful for reproducing/understanding third-party Android AI apps (like off-grid-ai-mobile or any closed-source competitor) as research input for Omni Claw's own design.
