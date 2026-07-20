# EXECUTIONS.md — Omni Claw Session Execution Ledger

Running, append-only log of what each coding session actually **did** —
distinct from `CLAUDE.md`'s State of the Union (which is the *current*
state, rewritten in place). This file is the *history*: one block per
session, newest first, never edited after the session that wrote it.

**Update contract (every session):**
1. At session **start**, read the top entry to see where the last one left off.
2. At session **end**, prepend a new `## Session N` block using the template
   below, then commit + push it alongside your work.
3. Never rewrite a prior session's block. Corrections go in the new block.

Keep each block scannable. Link PRs/commits/CI runs; don't paste logs.

```
## Session N — YYYY-MM-DD — <branch>
**Goal:** one line.
**Did:** bullets of concrete actions (files, PRs, CI).
**Build/CI:** run link + green/red.
**Failures:** anything that broke (cross-ref FAILURES.md).
**Handoff:** what the next session should pick up first.
```

---

## Session 18 — 2026-07-20 — claude/omni-claws-ui-setup-nl1573
**Goal:** Designate this branch/env as the Omni Claw UI environment and set
up a master coding-session scaffold: advanced-agent config, build
organization/monitoring, this executions ledger, and built-in failure
monitoring readable by any CLI sandbox via the GitHub workbench.

**Did:**
- App failure monitoring: added `core/diag/FailureMonitor.kt` —
  consolidates `CrashRecorder` + `Breadcrumb` + `InteractionLogger` errors
  into one adb-pullable `externalFilesDir/failures/` dir (`report.json` +
  `REPORT.md`), with a `record()` hook for silent-catch sites. Wired into
  `HorizonsApplication.onCreate()` and surfaced via a "Failure Report"
  action in `ArtifactsPane`'s Boot Diagnostics.
- GitHub-native failure surface: `FAILURES.md` ledger,
  `.github/workflows/failure-monitor.yml` (watches `build-apk`, uploads a
  `failure-report` artifact on any non-success), and `tools/failures.sh`
  (one fetch entry point: `ci` / `report` / `device`).
- Master session scaffold: `agents/omni-claws-master.agent.yaml` (Fable 5
  orchestrator) + `wiki/MASTER-SESSION.md` (roles, fan-out rules,
  build-monitor loop).
- Created this `EXECUTIONS.md` and its update contract.

**Build/CI:** push to branch triggers `build-apk`; monitored this session
(see PR). `failure-monitor` runs on its completion.

**Failures:** none introduced (additive code + config only). Pre-existing
open item carried in FAILURES.md: `http_server.cpp` recv() truncation.

**Hygiene flag raised:** an active branch `claude/app-redesign-layered-t55d47`
(CI green ~2026-07-19) exists but is NOT mentioned in CLAUDE.md's SOTU.
Flagged to operator; not touched.

**Handoff:** verify `build-apk` stays green on this branch, then confirm
with operator whether the UI visual-debug pass (CLAUDE.md SOTU session-17)
or the daemon/runtime track runs first under the master-session setup.
