---
name: novus-agenti-wiki
description: |
  Provides the Novus Agenti architecture-of-record and current session
  state. Load this skill when working on any Novus Agenti module:
  compile pipeline (Qwen3.5-9B → qnn_context_binary), ort_engine daemon,
  Android app (CliffordService, NpuManager, GameManager), or QAI Hub
  integration. Skill returns CLAUDE.md + the latest session handoff as
  one bundle so the agent can answer "where does X live" and "what was
  decided about Y" without re-searching the codebase.
version: 0.2.0
license: project-private
tags: [novus-agenti, qnn, hexagon-htp-v75, android, ort-engine, qai-hub]
---

# Novus Agenti Wiki Skill

This skill packages two documents as a single context bundle:

1. `CLAUDE.md` — stable architecture-of-record and compile spec
2. `wiki/SESSION{N}-HANDOFF.md` — latest session handoff (rolling state)

Both files live in the `c10vis-poem/Novus-Agenti` repo. The skill follows
the open SKILL.md standard so it is consumable from any compliant tool
(Claude Code, Codex, Cursor, etc.).

## When to use

  - Starting any Novus Agenti sub-agent (compile-runner, ort-engine-builder,
    android-integrator, diagnostics). Load this skill first so the cacheable
    prefix sits in the system block before any user message.
  - Answering questions about compile params, Android wiring, deepstack vision
    architecture, or design decisions captured in CLAUDE.md.

## How to use

The agent host should:

1. Read `CLAUDE.md` from repo root via `mcp__github__get_file_contents`.
2. Read latest `wiki/SESSION{N}-HANDOFF.md` (highest N).
3. Concatenate stable-then-volatile: CLAUDE.md first, then handoff.
4. Pass as the `system` block with
   `cache_control: {type: "ephemeral", ttl: "1h"}` on the last entry.
5. Use this skill's name as a cache-key correlator in logs so cache
   hit/miss can be attributed per skill invocation.

## What NOT to do

  - Do not edit `CLAUDE.md` mid-session — any byte change invalidates the
    cache and forces a 2× re-write at the next call.
  - Do not embed agent-specific task instructions in this skill — those go
    in the first user message so they stay out of the cached prefix.
  - Do not search the container filesystem for these files — the repo is not
    mounted. Use the GitHub MCP tool.

## Files referenced

  - `../../CLAUDE.md`
  - `../../wiki/SESSION{N}-HANDOFF.md` (latest N)
  - `../../wiki/GPT-OSS-Reference.md` (for compile-related at-bats)
