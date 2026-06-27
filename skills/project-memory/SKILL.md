---
name: project-memory
description: |
  Bundles Novus Agenti project memory — CLAUDE.md, session handoffs,
  compile script, GPT-OSS reference, and model manifest — as a single
  cacheable context block. Use when the at-bat needs full project context
  (architecture decisions, compile parameter changes, Android wiring, job
  failure diagnosis). Skip when the at-bat is a narrow isolated change
  (single-file fix, lint cleanup) — read CLAUDE.md only then.
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Project memory — Novus Agenti

This skill is the canonical project memory bundle. It loads the session
pickup files plus the stable spec as one block so agents that need full
context get it in one cache hit.

## What to read (in this order)

1. `CLAUDE.md` — master spec. Always read first. One screen.
2. `wiki/SESSION{N}-HANDOFF.md` — latest handoff. Current blockers + pending.
3. `wiki/GPT-OSS-Reference.md` — Hexagon HTP compile params and failure modes.
4. `scripts/compile_qwen3_5_9b.py` — authoritative compile script.
5. `models/manifest.yaml` — artifact registry.

## What this skill is for

- Fresh agent landing in the repo, needs to know what's going on.
- Compile pipeline work (patches to `compile_qwen3_5_9b.py`, ONNX export,
  QAI Hub job parameters).
- Android app integration (`CliffordService`, `NpuManager`, `GameManager` wiring).
- Multi-file changes that touch compile + app layers simultaneously.
- Job failure diagnosis (cross-reference handoffs + GPT-OSS reference).

## What this skill is NOT for

- Single-file lint cleanup. Read `CLAUDE.md` only.
- Doc-only edits. Read `CLAUDE.md` + the doc.
- One-off CI fix. Read `CLAUDE.md` + the failing CI log.

## Maintenance protocol

- The skill itself doesn't get edited per-session. `CLAUDE.md` and
  session handoffs do.
- If a new key file is added, update §"What to read".
- Cache: this skill's content is cacheable. Edits invalidate — batch
  between sessions, never mid-session.

## Pickup file cadence (operator-maintained)

| File | Updated when | Cadence |
|---|---|---|
| `CLAUDE.md` | Architecture changes, compile param updates | between sessions |
| `wiki/SESSION{N}-HANDOFF.md` | End of every session | per-session |
| `models/manifest.yaml` | New artifact published | per-artifact |

If you arrive and the latest handoff is older than the most recent commit
date, flag it — the prior session skipped close-out.
