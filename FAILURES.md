# FAILURES.md — Omni Claw Failure Ledger

Single, GitHub-native place any CLI sandbox can read to answer "what's
currently broken, and where do I look?" — without cloning a device or
digging through Actions logs by hand.

There are **three** failure surfaces; this file is the index to all of them.

| Surface | Where | How to read it |
|---|---|---|
| **CI / build** | `build-apk` + `failure-monitor` workflows | `tools/failures.sh ci` · `report` artifact in the Actions tab |
| **On-device** | `externalFilesDir/failures/` on the phone | `tools/failures.sh device` (adb) — `report.json` + `REPORT.md` |
| **This ledger** | this file | human-maintained rollup of known/recurring failure modes |

### Quick start (CLI sandbox in the GitHub workbench)

```bash
tools/failures.sh          # ledger + latest CI outcome + newest report artifact
tools/failures.sh ci       # just the last 5 build-apk conclusions
tools/failures.sh report   # download newest failure-report artifact
tools/failures.sh device   # adb pull the on-device failure report
```

### How the pieces connect

- The app's `core/diag/FailureMonitor` consolidates `CrashRecorder`,
  `Breadcrumb`, and `InteractionLogger` errors into one adb-pullable
  `failures/` dir (`report.json` machine-readable, `REPORT.md` human).
  Call `FailureMonitor.record(tag, msg, throwable)` from any `catch` that
  would otherwise swallow a failure silently.
- `.github/workflows/failure-monitor.yml` watches `build-apk`; on any
  non-success it extracts the failing steps into a `failure-report`
  artifact (30-day retention) and writes highlights to the run summary.
- `tools/failures.sh` is the single fetch entry point for both.

---

## Known / recurring failure modes

_Append newest first. Keep entries short: symptom → cause → fix/status._

### Open

- **`http_server.cpp` recv() truncation** — image payloads >8 KB are cut
  off because the daemon reads a single `recv()` into an 8 KB buffer
  instead of reading to `Content-Length`. Blocks vision round-trip.
  Status: OPEN (Pending item #3). Documented inline in that file.

### Resolved / historical

- **PR #21 merged without visual verification** — HomeGrid redesign
  shipped without a screenshot/emulator check against operator reference
  images. Not a build failure; a process failure. Mitigation: any visual
  task must end with a rendered-screenshot diff (see CLAUDE.md SOTU).

---

_Last updated: 2026-07-20 (session 18, branch
`claude/omni-claws-ui-setup-nl1573`)._
