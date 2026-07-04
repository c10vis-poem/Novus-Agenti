# Session 15 Handoff

**Branch:** `claude/novus-job-8-verify-r96601`
**PR:** #11 (draft, open) — doc-only fix, CI green
**Date:** 2026-07-04

## What was done

### 1. External research sweep
Reviewed five external repos plus one web-fetched reference against the
compile pipeline and daemon architecture: `off-grid-ai-mobile`,
`llama.cpp-npu`, `EdgeAIApp-ExecuTorch`, `mlc-llm`, `snapdragon-npu-llm`
(added via `add_repo`/cloned to `/workspace/`), and `qualcomm/GenieX`
(web-fetched only — cross-owner `add_repo` restriction blocked adding it to
session scope). Full findings in new file `wiki/RESEARCH-CROSSREF.md`.

Identified the arXiv paper behind the whole Hexagon-HTP-for-LLMs approach:
**arXiv:2509.23324**, "Scaling LLM Test-Time Compute with Mobile NPU on
Smartphones" (Hao, Wei, Wang, Huang, H. Jiang, S. Jiang, Cao, Ren — MSRA +
Tsinghua). This is what the operator remembered as "the canonical research
paper" — it was never lost, just never attached to this repo; only cited in
the `llama.cpp-npu` fork's README.

### 2. Fixed SM8750 Hexagon HTP arch version: v75 → v79
Verified against `pytorch/executorch`'s actual `get_soc_to_htp_arch_map()`
source (not a summary): SM8650=v75 (8 Gen 3), **SM8750=v79 (8 Elite)**,
SM8850=v81 (8 Elite Gen 5). This repo had v75 hardcoded in `CLAUDE.md`,
`wiki/GPT-DAEMON-REFERENCE.md`, and `wiki/NPU-RUNTIME-PATHS.md`. Root cause
traced to a session-9 GPT-chat "correction" of an earlier wrong v68 claim
that landed on v75 — itself wrong — and was never independently verified
before propagating into all three files. Fixed in commit `dde9a94`, PR #11
(CI green, no review comments as of this handoff).

### 3. Diagnosed the real Job 8 failure and retriggered
The prior resume prompt assumed Job 8 was blocked on the exit-code-14 HTP
Reshape/`quantize_io` issue. Actual failure (confirmed via `hf jobs logs` on
job `6a485607d235f6e43ae5a50a`) was a `TypeError: Qwen3_5VisionModel.forward()
missing 1 required positional argument: 'grid_thw'` — happening during ONNX
export, before ever reaching QAI Hub.

Root cause: a `--quantize_io` fix (commit `d605cee`) was applied to a stale
script copy that predated the `grid_thw` argument; a follow-up commit
(`8cde102`) synced+re-fixed it correctly — but subsequent job runs, even
with a `?v=` cache-buster on the raw.githubusercontent branch-name URL,
still hit the old error. Root cause of *that*: raw.githubusercontent
resolves branch-name paths through an internal redirect to a commit-SHA
URL, and that redirect target caches independently of the outer query
string — a fresh cache-buster on the branch URL doesn't guarantee a fresh
fetch of the redirect target.

**Fix**: retriggered pointing directly at the commit SHA
(`8cde10225c55e3d359c97a88dc3fe9f8ab7eed89`), bypassing the redirect
entirely. New job `6a488c52d235f6e43ae5a5a3` ran past the ~1-minute mark
where every prior attempt died — was 15+ minutes in (into the actual QAI Hub
compile stage) at last check, survived a container restart (job runs on HF's
infra, not locally). **Not yet confirmed complete as of this handoff — check
`hf jobs ps -a` / `hf jobs logs 6a488c52d235f6e43ae5a5a3` first thing next
session.**

### 4. New durable reference doc
`wiki/RESEARCH-CROSSREF.md` — not session-numbered, meant to persist as the
place all external-repo/paper findings get tracked going forward, rather
than living only in session handoffs that get harder to search over time.

## Commits (chronological)
1. `dde9a94` — fix: correct SM8750 Hexagon HTP arch from v75 to v79
2. (this session, uncommitted as of writing) — add RESEARCH-CROSSREF.md,
   cross-link from NPU-RUNTIME-PATHS.md and GPT-DAEMON-REFERENCE.md, this
   handoff, SOTU update

## What's next

1. **Check Job 8's actual outcome** (`6a488c52d235f6e43ae5a5a3`) — if it
   succeeded, verify artifacts on `Mer0vin8ian/qwen3-5-9b-npu-sm8750`, mark
   PR #4 (compile track) ready. If it failed at the QAI Hub compile stage
   (not ONNX export), check whether it's the 32-bit-cDSP/multi-session
   constraint documented in `llama.cpp-npu`'s README — see
   `RESEARCH-CROSSREF.md`.
2. **PR #11** — currently draft, CI green, no review comments. Merge or take
   out of draft when ready.
3. **A parallel session will audit the Horizons Android app pathway** — see
   `wiki/HORIZONS-APP-DEBUG-PLAN.md` (new, this session) for the pre-built
   plan. Do not duplicate that work here.
4. Everything else from session 14's Pending list (NpuManager lock,
   GameManager, Manifest perms, RouterPane routing rules, SettingsPane
   Themes, three orphaned classes) is unchanged — this session didn't touch
   the app pathway directly, only planned for it.
