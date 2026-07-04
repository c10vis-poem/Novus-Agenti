# Session 13 Handoff

**Branch:** `claude/horizons-closeout-hf-review-ycjkm3`
**PR:** #8 (draft, open, CI green)
**Date:** 2026-07-03

## What was done — full repo audit for dead weight and false claims

The operator asked for a genuine second-pass cleanup: find everything
still dead, redundant, bloated, or stated as true when it's actually
false. Dispatched an Explore agent for a repo-wide audit (not just the
docs already touched in session 12), then two parallel general-purpose
agents to execute the fixes on disjoint file sets (`agents/` vs `rules/`),
plus direct fixes for the highest-stakes/smallest items.

### The two big findings

1. **CLAUDE.md's resume prompt, sub-agent template, and Repo Policy all
   claimed the working branch was `claude/project-scope-review-lf615p`
   (PR #4), while the SOTU and actual checked-out branch were
   `horizons-closeout-hf-review-ycjkm3` (PR #8).** Root cause: there are
   genuinely **two active tracks with two different branches** — compile
   (PR #4) and app (PR #8) — and this file only ever documented one as
   "the" branch. Every new session copying the old resume prompt verbatim
   would've landed on the wrong branch for app work. Fixed: the resume
   prompt now states both tracks explicitly and tells you to pick the one
   matching your task.

2. **`ort_engine` was marked "Not yet built" in four places** (three in
   CLAUDE.md, one in `wiki/GPT-DAEMON-REFERENCE.md`) **but it's real** —
   `daemon/src/` has a 735-line implementation (`engine.cpp`,
   `http_server.cpp`, `tokenizer.cpp`, `sampler.h`, `main.cpp`), and CI
   already cross-compiles and packages it into the release artifact
   (`build-apk.yml`'s "Build ort_engine daemon" step). `models/manifest.yaml`
   had the correct `status: scaffolded` the entire time — CLAUDE.md was
   just 4+ days stale and nobody caught it because nothing was actually
   broken, the doc just misdirected every session's sense of what was
   left to do.

### Everything else fixed

- `watchdog/` — removed stale TODO (dir was deleted 2026-06-30, doc
  still mentioned it)
- `models/manifest.yaml` — dead `wiki/GPT-OSS-Reference.md` reference
  fixed, dead `notebooks/*.ipynb` reference noted as never-existed
- `horizons/core/README.md` — deleted (referenced nonexistent
  `GREENFIELD_PLAN.md` and `nexa/` package, described already-shipped
  code as "coming soon")
- `VoiceLoopController.kt` — removed a dead string-match on
  `"[LiteRtRuntime"`, a runtime deleted since session 8
- `skills/termux-mobile-dev/SKILL.md` — removed a link to
  `wiki/Termux-VNC-Matrix-Environment.md`, never created
- `.github/workflows/stage-colab.yml` — disabled (renamed `.disabled`),
  hardcoded a notebook path (`notebooks/compile_qwen3_vl.ipynb`, wrong
  model name too) that has never existed; would've misfired a PR
  comment linking to a 404 on the next compile-script push
- Removed a stale CI "publish target needs repointing" TODO — checked
  `build-apk.yml`'s full history, found no evidence it was ever wrong
- `agents/neuralmash-builder.system.md`, `agents/sub-agent.system.md`,
  `sub-agent.agent.yaml` — full rewrites, dead Nexa/OmniNeural/v79/Genie
  content and dead file pointers replaced with the real stack
- `agents/.snapshots/*.yaml` — left as historical captures, added a
  disclaiming README rather than rewriting history
- `agents/build-runner.yaml` — session 8's SOTU claimed this was renamed
  to `novus-compile-runner`; **that claim was itself false**, file is
  still `horizons-build-runner`. Its actual scope (Android CI) is
  legitimate on its own merits, so it wasn't renamed — but two real bugs
  were fixed (wrong JDK version, hardcoded `main` branch)
- `rules/README.md`, `rules/GIT_HYGIENE.md` — dead pickup-file names
  fixed to match the pattern already corrected in `AT_BAT_PROTOCOL.md`
- `rules/AAR_DECOMPILE.md` — archived (Nexa-specific, kept only for the
  reusable decompile technique)

### Flagged but NOT touched — needs operator input

- **Three fully-implemented, never-wired classes**: `core/log/InteractionLogger.kt`,
  `core/shell/SecureResourceRelay.kt`, `core/screen/ScreenshotCapture.kt`.
  These look like real mid-flight features (screenshot capture for the
  Vision-Agent tile, a secure shell token relay, structured interaction
  logging), not accidental cruft — confirm with the operator whether
  they're intentional before deleting or wiring them in.
- `wiki/FEATURE-SPEC.md` lists "System STT engine (Qwen3.5-9B)" under
  Settings → System registrations — Qwen3.5-9B is the vision-language
  model, not an STT engine, and there's already a distinct
  `HorizonsRecognitionService` for ASR. Likely a copy/paste slip in an
  early spec draft. Low urgency, needs a one-line clarification from the
  operator on what the actual STT engine name should be there.

## Commits this session

```
c1f9966 fix: rewrite stale agents/ files to reflect real Novus Agenti stack
8ceb9a5 rules: retire dead pickup-file names and archive Nexa AAR procedure
60de654 docs+fix: session 13 audit — branch contradiction, false ort_engine claim, dead refs
5bc6207 docs: fix AT_BAT_PROTOCOL dead file refs, correct strike model, add FAILURE_LOG
c046272 security: remove hardcoded tokens from CLAUDE.md, move to environment config
9e300ff docs: fix GPT-OSS-Reference dead pointer, add NPU runtime doc, session 12 handoff
```

Also this session (not doc-related): fixed a false claim that
`huggingface.co` is always reachable (network egress is per-session-
container, not fixed), diagnosed and explained a CI release-publish
failure (likely a Workflow Permissions setting flip, operator to
verify), and walked through cloud environment setup (network allowlist,
env vars, token rotation) at length in chat — no separate commit for
that, it's operator-side configuration.

### Later same session — token security, priority tree, OpenWiki

- **Removed hardcoded HF_TOKEN/QAI_HUB_API_TOKEN from CLAUDE.md entirely**
  (commit `c046272`). They'd been split across shell variables specifically
  to dodge the git secret scanner — not real security, since the repo is
  public and a split string is trivially reconstructable. Tokens now live
  only in the cloud environment's Environment Variables config. **Both
  tokens need rotation** — operator was walked through this, unclear if
  completed by end of session, check first thing next session.
- **Added an explicit priority tree to the Order of Operations** (commit
  `27b5d35`). A fresh session spun up right after the operator
  reconfigured the environment's network access read CLAUDE.md fully
  (correctly) then started working the general Pending list instead of
  the obvious actual reason it existed — verify HF egress, fire Job 8.
  Had to be manually interrupted. Fixed: `§Order of Operations` now has
  an explicit if/then check — HF egress confirmed working → Job 8 first,
  before any general cleanup work.
- **`c10vis-poem/openwiki` added to this session** (separate repo, not
  part of Novus-Agenti). It's a real LangChain-AI CLI tool that writes/
  maintains agent-facing docs for a codebase. Built and shipped a real
  feature on the operator's fork: an optional `openwiki/SKILL.md`
  convention letting a target repo customize OpenWiki's system prompt
  per-project ("memory as a skill") without forking OpenWiki itself.
  Verified with `tsc`/eslint/prettier plus a direct functional check
  (no live LLM call available in this environment). Pushed to branch
  `claude/project-skill-file`, opened as draft PR c10vis-poem/openwiki#1,
  subscribed for CI/review monitoring. **Not yet merged** — needs a real
  end-to-end run against a repo with a `SKILL.md` before merging, which
  wasn't possible here (no LLM API key configured in this container).
- **Job 8 status is unclear at end of session.** Operator reported
  launching a separate session after reconfiguring the environment, that
  session got sidetracked doing redundant cleanup work, operator
  interrupted it and told it to trigger Job 8 directly. Whether that
  actually succeeded (job accepted, running, completed, or failed) was
  never confirmed back in *this* session — **check HF Jobs status /
  `Mer0vin8ian/qwen3-5-9b-npu-sm8750` on HF Hub first thing next
  session**, don't assume it's still "pending, never triggered."

## Resume block

```
Project: Novus Agenti (Omni Claw). Mission: compile Mer0vin8ian/Qwen3.5-9B
→ Hexagon HTP v75 (SM8750) qnn_context_binary via QAI Hub.
Canonical repo: c10vis-poem/Novus-Agenti. Two tracks, two branches — see
CLAUDE.md's resume prompt for which one matches your task.

READ THESE IN ORDER BEFORE ANY ACTION:
  1. CLAUDE.md (full read, all sections — includes the new priority tree
     under Order of Operations, check it before acting)
  2. wiki/GPT-DAEMON-REFERENCE.md (full read)
  3. wiki/NPU-RUNTIME-PATHS.md (full read)
  4. wiki/SESSION13-HANDOFF.md (THIS FILE)
  5. models/manifest.yaml
  6. scripts/compile_qwen3_5_9b.py

FIRST ACTION, before anything else: check whether Job 8 was actually
triggered and what happened to it (operator reported telling a separate
session to fire it, but that was never confirmed back here). Check
HF Hub for Mer0vin8ian/qwen3-5-9b-npu-sm8750 and/or ask the operator
directly. Do not assume it's still untriggered, and do not re-trigger
it if it's already running/done.

Also check: did the operator rotate HF_TOKEN/QAI_HUB_API_TOKEN and put
the new values in the environment's Environment Variables field? The
old values were removed from CLAUDE.md this session for being exposed
in a public repo's history — if unrotated, that's still an open item.

Separately, c10vis-poem/openwiki#1 (draft PR, different repo, a
project-skill-file feature) is open and unmerged — needs a live
end-to-end test before merging, not just the static checks already run.

CLAUDE.md was carrying two significant false claims (wrong branch, wrong
ort_engine build status) that are now fixed — don't assume anything else
in it is automatically trustworthy either; verify against actual repo
state before repeating a claim from any doc, including this one.
```
