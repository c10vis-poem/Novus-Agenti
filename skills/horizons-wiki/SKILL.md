---
name: horizons-wiki
description: |
  Provides the Novus Agenti / Omni Claw architecture-of-record. Load this
  skill when working on any Horizons module (on-device Android app,
  Qwen3.5-9B → Hexagon HTP compile pipeline, ort_engine/GenieX daemon,
  agent tools, Router/Monitor/Artifacts UI). Skill returns the stable
  architecture doc bundle so the agent can answer "where does X live" and
  "what was decided about Y" without re-searching the codebase. Current
  state (SOTU) is NOT part of this bundle — that's the SessionStart hook's
  job; this skill is architecture reference only.
version: 0.3.0
license: project-private
tags: [horizons, android, qnn, hexagon, npu, anthropic-caching]
---

# Horizons Wiki Skill

This skill packages the project's real architecture-of-record documents as
a single context bundle:

1. `CLAUDE.md` — stable architecture-of-record, tool/token authority,
   hard rules, current SOTU
2. `knowledge/daemon-reference/GPT-DAEMON-REFERENCE.md` — distilled
   daemon/architecture notes
3. `knowledge/daemon-reference/NPU-RUNTIME-PATHS.md` — runtime formats +
   SDK distribution model

There is no separate handoff file to load — CLAUDE.md's own
`## State of the Union` section is the single current-state source, kept
up to date in place rather than accumulating one file per session. The
skill is designed for the open SKILL.md standard (Claude Code, Codex,
Cursor, etc.) so the same wiki is consumable from any compliant tool.

## When to use

  - Starting any Horizons sub-agent (build-runner, code-review,
    diagnostics). Load this skill *first* before any user message so the
    cacheable prefix sits in the system block.
  - Answering questions about subsystem boundaries, file ownership,
    or design decisions captured in the wiki.

## How to use

The agent host should:

1. Read all three documents listed above, in the order listed.
2. Concatenate in that order (CLAUDE.md first — it's both the stable
   architecture-of-record and the current SOTU).
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
  - `../../knowledge/daemon-reference/GPT-DAEMON-REFERENCE.md`
  - `../../knowledge/daemon-reference/NPU-RUNTIME-PATHS.md`
