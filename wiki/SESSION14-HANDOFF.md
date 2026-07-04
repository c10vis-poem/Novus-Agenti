# Session 14 Handoff

**Branch:** `main` (PR #8 merged this session)
**Date:** 2026-07-04

## What was done
- Merged PR #8 into `main` — everything from sessions 12-13 (crash fixes,
  UI/perf work, token security cleanup, full doc audit, priority tree)
  is now live on `main`, verified via CI green before merge.
- Fixed the real bug behind "the hook never gets committed": `.claude/`
  was fully gitignored. Fixed, both hook files now tracked on `main`.
- `c10vis-poem/openwiki`: merged the SKILL.md feature to its `main`,
  opened `claude/audit-mode` branch with `AUDIT_MODE_TODO.md` for the
  next build phase (Fable-targeted), with an explicit guardrail baked
  in against auto-deleting parallel-session work that just looks stale.
- Naming convention going forward: new branches get a Merovingian
  leetspeak front-label (matching existing `Mer0vin8ian`/`Mer0vin6ian`
  usage); credit line "Mer0vin6ian Production — Cl0vis/Claude collab."

## Three Pathways — the operator's framing, use this to orient

1. **Compile (Qwen3.5-9B → HTP)** — `claude/project-scope-review-lf615p`,
   PR #4. Has its own handoff/merge point. Job 8 status unconfirmed —
   check before assuming anything.
2. **OpenWiki, dual role** — separate repo `c10vis-poem/openwiki`. Build
   it out (audit mode next) AND start running it against this repo's own
   docs — the operator's key observation: what we've been doing by hand
   to CLAUDE.md/wiki/ all session is exactly what OpenWiki automates.
3. **Horizons app — most stale, most important.** Sessions have been
   "shooting from the hip." Mandate: audit off real documented state
   before any new feature work. No improvising.

## Resume block

```
Project: Novus Agenti (Omni Claw). Read CLAUDE.md fully (all sections,
including the new Three Pathways framing in the SOTU), then
wiki/SESSION14-HANDOFF.md (this file).

Your first action: figure out which of the three pathways this session
is actually for (compile / openwiki / horizons-app), based on what the
operator says or what's most urgent. Do not default to the flat Pending
list without checking the pathway framing first.

If working the Horizons app pathway specifically: audit stage first.
Ground every claim in CLAUDE.md, the wiki, actual repo state, and actual
CI results — not assumption. This pathway has a documented history of
sessions improvising without checking real state; don't repeat it.

If working the OpenWiki pathway: remember it's two jobs, not one — build
the tool AND start using it on this repo's own docs.
```
