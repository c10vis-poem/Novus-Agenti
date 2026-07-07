# Session 16 Handoff — Horizons App Debug (pathway 3)

**Branch:** `claude/novus-agenti-setup-rr8jvz` (based on `main` @ `be3789b`)
**Track:** APP ONLY. This session did not touch the compile pipeline —
Job 8 / QAI Hub work belongs to the compile-track session (operator
directive, restated mid-session).
**Date:** 2026-07-04
**Input plan:** `wiki/HORIZONS-APP-DEBUG-PLAN.md` (session 15, branch
`claude/novus-job-8-verify-r96601`, PR #11 — not yet merged as of this
session, so that file is not on `main` yet).

## What was done (in the plan's suggested order)

### 1. CLAUDE.md stale-PENDING fix (plan step 1) — DONE
Re-verified all three claims against source before editing (grep + read,
not trusted from the plan):
- `CliffordService.kt` — `acquireNpuPerfLock()` at line ~170, reflection
  on the `@hide` `npu` service, `PERF_MODE_HIGH`, no-op fallback. Wired
  (called at service start, line ~119).
- `GameModeBoost.kt` + `PerfHintSession.kt` in `core/perf/` — ADPF
  `GameState` hot-loop scoped boost; genuinely called from
  `HorizonsApplication.kt` (enterHotLoop/reportWork/exitHotLoop) and
  imported by `AgentLoop.kt`.
- `AndroidManifest.xml` — `HIGH_PERFORMANCE` permission (line 50) and
  `android.hardware.game` uses-feature (line 52) present.

CLAUDE.md's "Android App / Battery Rules" section rewritten as done-notes
pointing at the real files; the three items removed from the Pending list.

### 2. Tokenizer audit (plan step 2) — DONE, real findings
`daemon/src/tokenizer.cpp` is **not** the EdgeAIApp-ExecuTorch
hash-tokenization bug — it loads the real vocab from `tokenizer.json` and
does genuine map lookups. But it has three gaps in the same
"tokenizer bug that looks like a model bug" class:

1. **No GPT-2 byte-to-unicode mapping.** Qwen-family `tokenizer.json`
   stores tokens in byte-level form (`Ġ` = space, `Ċ` = newline). The
   greedy matcher compares raw text against the vocab, so `" hello"`
   never matches `"Ġhello"`. Every space/newline (and any non-ASCII
   byte) falls into the "unknown byte — skip" branch (`encode()`,
   ~line 133) and is **silently dropped**. `decode()` has the mirror
   bug: output contains literal `Ġ`/`Ċ` instead of spaces/newlines.
2. **No BPE merge loop.** `merges` is declared in the Impl struct but
   never parsed or applied; encode is greedy longest-match, which
   diverges from training tokenization even where lookups succeed.
3. **JSON escapes never unescaped.** The regex captures escaped strings
   (`\"`, `\uXXXX`) but stores them raw in `token_to_id`/`id_to_token`.

**Consequence:** even a perfectly compiled model would produce garbled
output through this daemon. This MUST be fixed before any on-device
verification result is trusted — added to CLAUDE.md's Pending list as
item 3. (Fix not implemented this session: pathway-3 mandate was audit
first, no feature work until the audit is confirmed.)

### 3. RouterPane / SettingsPane re-verification (plan step 3) — DONE
- `RouterPane.kt` (773 lines): sections are NPU Runtime, Performance,
  Cloud Model Selector, OpenRouter Catalog (key-gated), Cloud APIs. No
  routing rule engine anywhere. Session-14 claim stands.
- `HorizonsTheme.kt`: `HorizonsColors` is still a flat hardcoded
  `object` (brand + tile + status colors); `SettingsPane.kt` has no
  theme picker. Session-14 claim stands.

### 4. Orphaned classes (plan step 4) — CONFIRMED, question open
`core/log/InteractionLogger.kt`, `core/shell/SecureResourceRelay.kt`,
`core/screen/ScreenshotCapture.kt` each match only their own definition
file under `horizons/src/main/java` — still fully unwired. **Operator
question posed this session (unanswered as of this handoff): which of
the three are still wanted (wire them) vs. droppable (delete)?** Do
nothing irreversible until answered.

### 5. Job 8 gate check (plan step 5) — observed read-only, NOT worked
This session only *observed* status because the plan gates ort_engine
on-device verification on it (HF egress happened to be open in this
container; `hf jobs ps -a` + `hf jobs logs`, no writes, nothing
triggered):
- Job `6a488c52d235f6e43ae5a5a3`: **ERROR after 39m53s.**
- ONNX export stage fully succeeded — the `grid_thw`/commit-SHA-URL fix
  from session 15 is confirmed good (vision 793 MB, projection 76 MB,
  4 decoder chunks exported clean).
- Failure is at QAI Hub: vision-encoder compile job `jpeler3og` →
  **exit code 14, "Conversion to context binary failed"** — the
  original HTP context-binary issue, now surfacing at the compile
  stage on the vision artifact despite `--quantize_io`.
- Secondary: calibration dataset load failed (`Invalid HF URI
  'hf://datasets/wikitext@...'`) → silent synthetic-token fallback.
- `app.aihub.qualcomm.com` is egress-blocked (403) in this session's
  container, so the detailed QNN log was not retrievable from here.

**All of the above is compile-track work** (branch
`claude/project-scope-review-lf615p`, PR #4). Leads for that session:
the job log's own hint `Re-download later: SKIP_EXPORT=1
JOB_ID_*=jpeler3og`; `scripts/fetch_qai_logs.py` on branch
`claude/proxy-hf-auth-status-3pcwbu`; and the 32-bit-cDSP/multi-session
constraint documented in `wiki/RESEARCH-CROSSREF.md` (PR #11 branch).
Consequently **ort_engine on-device verification stays blocked.**

## What's next (app track)

1. **Fix `tokenizer.cpp`** (Pending item 3) — byte-level mapping, merge
   loop, JSON unescape. Testable off-device: encode/decode round-trip
   against HF `tokenizers` output for the same `tokenizer.json`.
2. **Operator answer on the three orphaned classes** — then wire or
   delete accordingly.
3. RouterPane rule engine / SettingsPane themes — still deliberately
   deferred, unchanged.
4. On-device ort_engine verification — blocked on the compile track
   producing artifacts.
