---
name: horizons-wiki
description: |
  Provides the Novus Agenti / Omni Claw architecture-of-record and current
  session handoff. Load this skill when working on any Horizons module
  (on-device Android app, Qwen3.5-9B → Hexagon HTP compile pipeline,
  ort_engine daemon, agent tools, Router/Monitor/Artifacts UI). Skill
  returns the stable architecture doc + rolling session handoff as one
  bundle so the agent can answer "where does X live" and "what was
  decided about Y" without re-searching the codebase.
version: 0.2.0
license: project-private
tags: [horizons, android, qnn, hexagon, npu, anthropic-caching]
---

# Horizons Wiki Skill

This skill packages the project's real architecture-of-record documents as
a single context bundle:

1. `CLAUDE.md` — stable architecture-of-record, tool/token authority,
   hard rules
2. `wiki/GPT-DAEMON-REFERENCE.md` — distilled daemon/architecture notes
3. `wiki/NPU-RUNTIME-PATHS.md` — runtime formats + SDK distribution model
4. Latest `wiki/SESSION{N}-HANDOFF.md` — rolling per-session state

All files live under the repo root / `wiki/`. The skill is designed for
the open SKILL.md standard (Claude Code, Codex, Cursor, etc.) so the same
wiki is consumable from any compliant tool.

## When to use

  - Starting any Horizons sub-agent (build-runner, code-review,
    diagnostics). Load this skill *first* before any user message so the
    cacheable prefix sits in the system block.
  - Answering questions about subsystem boundaries, file ownership,
    or design decisions captured in the wiki.

## How to use

The agent host should:

1. Read all four documents listed above, in the order listed.
2. Concatenate stable-then-volatile (CLAUDE.md, then reference docs, then
   the latest handoff).
3. Pass as the `system` block with
   `cache_control: {type: "ephemeral", ttl: "1h"}` on the last entry.
4. Use this skill's name as a cache-key correlator in logs so cache
   hit/miss can be attributed.

## What NOT to do

  - Do not edit any of these files mid-session — invalidates the cache
    and forces a 2x re-write.
  - Do not embed agent-specific task instructions in this skill —
    those go in the first user message so they stay out of the
    cached prefix.
  - Do not trust a prior session's claims about network reachability
    (e.g. HuggingFace egress) at face value — verify fresh per
    CLAUDE.md's `§HuggingFace Access` section. Network policy is set
    per remote-session container, not fixed project-wide.

## Files referenced

  - `../../CLAUDE.md`
  - `../../wiki/GPT-DAEMON-REFERENCE.md`
  - `../../wiki/NPU-RUNTIME-PATHS.md`
  - `../../wiki/SESSION{N}-HANDOFF.md` (latest N)
