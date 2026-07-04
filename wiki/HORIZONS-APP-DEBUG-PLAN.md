# Horizons App Debug Session — Pre-Task Plan

> Written session 15 (2026-07-04) for a **separate future session** to
> execute. This session did the planning/verification legwork; do not
> duplicate the audit below — start from its findings.
> Repo: `c10vis-poem/Novus-Agenti`. Per CLAUDE.md's "Three Pathways," this is
> pathway 3, flagged as **the most stale pathway** — audit first, off real
> sources, no new feature work until the audit confirms what's actually true.

---

## Why this plan exists

CLAUDE.md's own SOTU lists several Android app items as "PENDING — NOT YET
WIRED." This session spot-checked those claims against the actual source
files rather than trusting the doc, per the project's own stale-doc
guardrail (a doc/code mismatch is not automatically "stale docs are behind
reality" — verify before assuming either direction). Result: **two of the
four listed pending items are already done**, just implemented differently
than the snippet CLAUDE.md shows.

## Verified starting state (confirmed 2026-07-04, this session)

| CLAUDE.md claims | Actual state (verified via grep + file read) |
|---|---|
| "NpuManager Performance Lock (PENDING — NOT YET WIRED)" | **Already wired.** `CliffordService.kt` has `acquireNpuPerfLock()` / `releaseNpuPerfLock()` using reflection against the `@hide` `npu` system service (`PERF_MODE_HIGH`, `acquirePerformanceLock`), with a safe no-op fallback if unavailable. Real, working code. |
| "Game SDK Performance Mode (PENDING — NOT YET WIRED)" — literal `GameManager.setGameMode(GameMode.PERFORMANCE)` snippet | **Wired, but via a different (better) mechanism.** `core/perf/GameModeBoost.kt` uses ADPF's `GameState(false, GameState.MODE_GAMEPLAY_UNINTERRUPTIBLE)` scoped to inference hot-loops (reentrant enter/exit, `PerfHintSession` for per-thread scheduler hints) rather than a single app-lifetime `setGameMode` call. This matches session 9's handoff note that GameModeBoost is "more granular... engages Game Mode only during active LLM inference." The literal PENDING snippet in CLAUDE.md is simply obsolete. |
| "Manifest (PENDING)" — `uses-feature android.hardware.game` + `HIGH_PERFORMANCE` permission | **Already present** in `horizons/src/main/AndroidManifest.xml` (lines 50, 52 as of this session). |
| Three orphaned classes: `InteractionLogger.kt`, `SecureResourceRelay.kt`, `ScreenshotCapture.kt` | **Confirmed still unwired** — each only self-matches on its own definition file; no caller anywhere else in `horizons/src/main/java`. This part of CLAUDE.md is accurate. |

**First action for the debug session: correct CLAUDE.md's Android App /
Battery Rules section** to remove the stale PENDING snippets for
NpuManager/GameManager/Manifest and replace with a short "done, see
`GameModeBoost.kt` / `CliffordService.kt`" note. This is a doc fix, takes
five minutes, and should happen before anything else so the next session
after that one doesn't re-verify the same three items again.

## What's actually still open (verified or carried from session 14, not yet re-checked)

1. **The three orphaned classes** — confirmed still unwired (see above).
   Before wiring or deleting: these look like mid-flight features
   (screenshot capture for a Vision-Agent tile, secure shell token relay,
   structured interaction logging), not accidental cruft. **Ask the
   operator which of these are still wanted** before doing anything
   irreversible — do not delete without confirmation, per session 13's
   explicit note on this exact point.
2. **`ort_engine` on-device verification** — daemon builds and is packaged
   by CI (per session 13's audit), but has never been verified to actually
   load a real compiled model and serve inference on a physical device.
   **This is blocked on Job 8 succeeding first** (compile pipeline, tracked
   in `wiki/SESSION15-HANDOFF.md` — check that job's outcome before
   assuming this is unblocked).
3. **RouterPane "routing rules"** (use-cloud-when-NPU-unavailable etc.) —
   per session 14, deliberately not built; needs a real rule engine, not UI
   toggles that don't affect behavior. Not re-verified this session — audit
   `RouterPane` source before starting to confirm this is still accurate.
4. **SettingsPane "Themes"** — per session 14, deliberately not built;
   `HorizonsColors` is a flat hardcoded object needing a switchable palette
   system. Not re-verified this session.
5. **Tokenizer audit** (new this session, from `EdgeAIApp-ExecuTorch`'s
   documented hash-based-tokenization bug — see
   `wiki/RESEARCH-CROSSREF.md`) — check `daemon/src/tokenizer.cpp` for
   anything resembling a hash-based or placeholder token mapping instead of
   real vocabulary lookup. Cheap to check, and exactly the kind of bug that
   looks like a model problem but isn't. Do this before Job 8's output ever
   gets loaded on-device, so a real tokenizer bug doesn't get mistaken for a
   bad compile.

## Suggested order for the debug session

1. Fix the stale PENDING claims in CLAUDE.md (5 min, unblocks accurate SOTU
   for everything after).
2. Audit `tokenizer.cpp` against the hash-tokenization failure mode (cheap,
   high-value, blocks nothing else).
3. Re-verify RouterPane/SettingsPane claims directly against source (these
   weren't re-checked this session, only carried from session 14).
4. Ask the operator about the three orphaned classes before touching them.
5. `ort_engine` on-device verification — only once Job 8's artifacts exist.

## Fable 5 handoff note

Per the operator: this plan is for a **separate session** running Fable 5.
That session should start with `/memory` (per CLAUDE.md's own resume
sequence) and then read this file, `wiki/SESSION15-HANDOFF.md`, and
`wiki/RESEARCH-CROSSREF.md` before touching any app code.
