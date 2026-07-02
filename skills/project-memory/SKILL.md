---
name: project-memory
description: |
  Quick-load Horizons project memory — just the SOTU section of CLAUDE.md
  plus the latest session handoff. Use when the at-bat is a narrow isolated
  change (single-file fix, lint cleanup, one-off CI fix) that doesn't need
  the full architecture wiki. For anything touching multiple subsystems or
  requiring architecture/design context, use the `horizons-wiki` skill
  instead — it loads CLAUDE.md in full plus the runtime-paths reference.
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Project memory — Horizons (quick variant)

This skill is the **lightweight session pickup**. It loads only the SOTU
section of `CLAUDE.md` plus the latest `wiki/SESSION{N}-HANDOFF.md` — not
the full architecture bundle. If the task needs more than that, stop and
use the `horizons-wiki` skill instead.

There is no separate `SOTU.md`, `PROMPT_PREFIX.md`, `EXECUTION_BOARD.md`,
or `CLAUDE_AT_HORIZONS.md` in this repo — those file names are leftovers
from an earlier project structure. The real SOTU lives inline in
`CLAUDE.md` under `## State of the Union`.

## What to read (in this order)

1. `CLAUDE.md` — read the `## State of the Union` section specifically
   (search for that heading; don't re-read the whole file for a narrow fix)
2. Latest `wiki/SESSION{N}-HANDOFF.md` — find the highest N in `wiki/`

## What this skill is for

- Fresh agent landing in the repo for a small, contained task.
- One-off CI fix — SOTU + the failing CI log is enough.
- Doc-only edits that don't touch architecture.

## What this skill is NOT for

- Multi-file coordination, architecture decisions, or anything touching
  the compile pipeline, daemon design, or UI spec — use `horizons-wiki`.

## Maintenance protocol

- The skill itself doesn't get edited per-session. `CLAUDE.md`'s SOTU
  section and the latest handoff file do.
- If you arrive in a session and the SOTU date is older than the most
  recent commit date, flag it — the prior session skipped close-out.
- Before trusting anything the SOTU or a handoff says about network
  reachability (HuggingFace, etc.), re-verify — see CLAUDE.md's
  `§HuggingFace Access` section. That kind of claim goes stale fast and
  is scoped per remote-session container, not project-wide.
