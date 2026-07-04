# Session 12 Handoff

**Branch:** `claude/horizons-closeout-hf-review-ycjkm3`
**PR:** #8 (draft, open, CI green)
**Date:** 2026-07-02

## What was done

### 1. Fixed a false "HF egress always works" claim in CLAUDE.md

Two commits landed 3 minutes apart on 2026-06-30: `d91a714` correctly
documented that `huggingface.co` is proxy-blocked in some sessions, then
`1afa04f` overwrote it with an unverified "all routes verified working"
claim — no passing command output attached, just an assertion. That false
claim sat in CLAUDE.md and cost this session real time before the actual
cause was tracked down.

**The real mechanism:** network egress policy is a property of the *remote
session's container*, set at container creation time from the environment's
config. It is not a fixed fact about "this repo" or "this environment
generally" — it can differ session to session, and editing the environment's
allowlist does not retroactively affect an already-running session. Verify
fresh every session with `curl -sS "$HTTPS_PROXY/__agentproxy/status"` and
check `recentRelayFailures` — don't trust a prior session's claim about what
was reachable.

CLAUDE.md's `§HuggingFace Access` section now says this instead of asserting
a blanket "works."

### 2. UI: filled concrete FEATURE-SPEC.md gaps (commit `f7ceea9`)

- Added real timing to `LlmRuntime` (`PerfMetrics`: first-token latency,
  tokens/sec, token count) via `onEach` on the existing SSE stream — no new
  plumbing, both `NpuClient` and `CloudLlmRuntime` now report it.
- `RouterPane`: new "Performance" section showing the above plus live
  device memory from `ActivityManager`. Was missing entirely — spec asked
  for "tokens/sec, latency, memory usage from active backend."
- `MonitorPane`: new "Prompt / Script Library" section wired to the
  existing `SavedCommandStore`, tap-to-clipboard. Spec's Monitor tile
  explicitly wants "saved system prompts... tap to load into chat or
  terminal" — this was completely absent before.
- `SettingsPane`: added `onNavigate` param + quick links to Terminal
  (scripts) and Artifacts (failure logs), wired through `MainActivity`.

Deliberately **not** built: RouterPane "routing rules" (use-cloud-when-NPU-
unavailable toggles) and SettingsPane "Themes" — both would need real
backend wiring (a routing-rule engine, a switchable color-palette system)
that doesn't exist yet. Building fake toggles that don't affect behavior
would be dishonest UI; flagging as real gaps instead.

Could not run a local Gradle build to verify — this container has no
Android SDK/Maven cache and `dl.google.com` is also proxy-blocked in this
session (same class of issue as #1). Verified manually (full re-read of
every changed file, balanced-brace/paren check, import resolution) and let
CI be the actual gate — it came back green (`84691978729`, commit
`f7ceea98`) about 3 minutes after push.

### 3. Documentation consolidation

- **Fixed `wiki/GPT-OSS-Reference.md` → `wiki/GPT-DAEMON-REFERENCE.md`** in
  CLAUDE.md's resume prompt, `/memory` sequence, and SOTU/file-map — that
  filename never existed in this repo (confirmed via `git log` and
  directory listing going back to session 5). The real distilled reference
  file has always been `GPT-DAEMON-REFERENCE.md`. Every session since at
  least session 11 was reading a required-reading list pointing at a
  nonexistent file and presumably skipping it silently.
- Added `wiki/NPU-RUNTIME-PATHS.md` — the six runtime-format paths to
  Hexagon HTP, and critically: **the Hexagon/QNN SDK is a host-only
  cross-compile toolchain (QPM3, desktop Windows/Linux), never installed
  on the phone.** Only the compiled `.so`/binary output goes on-device.
  Given this project's "phone only, no laptop" rule, that means CI
  (GitHub Actions, e.g. `ghcr.io/snapdragon-toolchain/arm64-android:v0.7`,
  tag existence verified via the GHCR registry API this session) is the
  only realistic build path for `ort_engine` / `llama-server`, not local
  installation.

## Job 8 status — still not triggered

Confirmed (again) this session: `huggingface.co` is 403-blocked by this
container's egress policy. User added it to the environment's allowlist
mid-session, but that only applies to *new* sessions created against that
environment going forward — it did not retroactively unblock this already-
running container (re-tested, still 403 after the edit). Next session
started fresh against the updated environment should have it working;
verify with the status-endpoint check in §1 before assuming so.

## Resume block

```
Project: Novus Agenti (Omni Claw). Mission: compile Mer0vin8ian/Qwen3.5-9B
→ Hexagon HTP v75 (SM8750) qnn_context_binary via QAI Hub.
Canonical repo: c10vis-poem/Novus-Agenti, branch claude/horizons-closeout-hf-review-ycjkm3.

READ THESE IN ORDER BEFORE ANY ACTION:
  1. CLAUDE.md (full read, all sections)
  2. wiki/GPT-DAEMON-REFERENCE.md (full read)
  3. wiki/NPU-RUNTIME-PATHS.md (full read)
  4. wiki/FEATURE-SPEC.md (UI tile spec)
  5. wiki/SESSION12-HANDOFF.md (THIS FILE)
  6. models/manifest.yaml
  7. scripts/compile_qwen3_5_9b.py

Before assuming huggingface.co egress works: run
curl -sS "$HTTPS_PROXY/__agentproxy/status" and check recentRelayFailures.
Do not trust this file's or any prior session's claim about network access —
it is per-session-container, verify fresh.

Next: trigger Job 8 (if HF egress is confirmed open), then build ort_engine
via CI using the verified toolchain image, then wire NpuManager+GameManager
perf locks and the two deliberately-skipped UI sections (routing rules,
themes) once their backends exist.
```
