# CLAUDE.md — Novus Agenti / Omni Claw

> **RESUME PROMPT — COPY THIS BLOCK VERBATIM TO START ANY NEW SESSION**
>
> ```
> Project: Novus Agenti (Omni Claw) — fully on-device agentic AI assistant.
> Canonical repo: c10vis-poem/Novus-Agenti.
>
> ONE ACTIVE TRACK: the app/UI-fork work (local-first UI, model+vision
> daemon / media daemon split). As of session 17 all prior app-track
> branches (sae7cy/PR #15, claude/notice-agent-ui-local-xa14op) are MERGED
> to main — do not reuse them. Each new session works on the branch the
> harness cut from main for it (session 17: mer0vin6ian/practical-bell-3t735g);
> don't resurrect old branch names from this file's history.
>
> The compile pipeline (ONNX → QAI Hub, branch claude/project-scope-review-lf615p,
> PR #4) is DORMANT — not a second parallel track to pick up by default. It
> only becomes active if the primary path (Q4_0 GGUF via GenieX's GGML
> backend, no compile needed) hits a hard failure on real hardware. See
> wiki/COMPILE-PIPELINE.md. Don't run Job 8 pre-emptively.
>
> READ THESE IN ORDER BEFORE ANY ACTION — this list IS the `horizons-wiki`
> skill's bundle (items 1, 3, 4) plus `project-memory`'s always-read tier
> (item 2); load them now, from the start of the session, not on demand
> partway through:
>   1. CLAUDE.md (full read, all sections, including current State of the Union)
>   2. knowledge/omni-claw-defined/ (always-read core project definition)
>   3. knowledge/daemon-reference/GPT-DAEMON-REFERENCE.md
>   4. knowledge/daemon-reference/NPU-RUNTIME-PATHS.md
>   5. compile/manifest.yaml
>
> For anything beyond this initial bundle (compile-pipeline detail, QAIRT
> SDK reference, prior research, device inventory) use `project-memory`'s
> retrieval pattern — Grep/Glob the relevant `knowledge/*.jsonl` on demand,
> don't preload the whole corpus.
>
> Use /memory slash command to reload full project context. There is no
> separate per-session handoff file — CLAUDE.md's State of the Union IS
> the current-state doc, updated in place each session.
> After reading: state current SOTU, next action. Then wait.
> HF_TOKEN / QAI_HUB_API_TOKEN come from the environment config (§Tokens
> below) — already exported, no manual step needed. Never hardcode them.
> ```

---

## /memory — Slash Command

Type `/memory` in any Claude Code session to reload full project context.

**Sequence:**
1. Read `CLAUDE.md` (this file, all sections, including the current
   `## State of the Union` — there is no separate handoff file)
2. Read `knowledge/omni-claw-defined/` (always-read core project definition)
3. Read `knowledge/daemon-reference/GPT-DAEMON-REFERENCE.md`
4. Read `knowledge/daemon-reference/NPU-RUNTIME-PATHS.md`
5. Read `compile/manifest.yaml`
6. For anything else (compile pipeline, QAIRT SDK detail, prior research),
   retrieve on demand from `knowledge/*.jsonl` per
   `skills/project-memory/SKILL.md` — don't preload it
7. Produce a SOTU summary and confirm next action before touching any file

---

## Order of Operations — Non-Negotiable

1. **Read** — the files above, in order, completely
2. **State** — current SOTU and next action
3. **Act** — check the priority tree below FIRST, before touching the
   general Pending list
4. **Hygiene check** — before AND after every push (not a full repo audit
   every session — see `§Hygiene Protocol` below for the actual scope)
5. **Document** — before ending: update `## State of the Union` in this
   file **in place** (no separate handoff file — see `§Cache Prompting`'s
   file-edit-batching rule for when in the session this should happen),
   then commit AND push. A local-only commit is invisible to the next
   session.

### Hygiene Protocol — lightweight, tied to every push, not a one-off audit

This is NOT a mandate to re-run a full repo-wide audit every session — that
was a one-time session-16 initiative. Going forward, hygiene is a small,
continuous check tied to the normal commit/push cycle:

- **Before every push:** check that whatever you just changed doesn't leave
  a dangling reference elsewhere (a moved/deleted file's old path, a
  renamed branch, a doc that now contradicts your own edit). Scope: the
  files you touched and their direct cross-references — not the whole repo.
- **After every CI push:** once CI results land, a quick sanity check that
  nothing you assumed would work actually broke — not a fresh audit, just
  closing the loop on what you just shipped.
- **If you discover something stale, redundant, or contradictory at ANY
  point — even outside these two checkpoints — STOP.** Do not silently fix
  it, and do not silently ignore it and move on. Flag it to the operator
  and get their read before taking any further action. This applies even
  mid-task, even if it's not what you were asked to look at.

### Act — priority tree (check top to bottom, stop at the first match)

Reading this file is not the same as acting on every item in it.
"Act" means: find which of these applies, do that, THEN fall through
to the general Pending list.

1. **HF egress just became available?** That is NOT automatically a
   signal to run Job 8 anymore — the compile pipeline is dormant (see the
   resume prompt above). Only trigger Job 8 if the operator has confirmed
   the primary GGUF/GenieX path hard-failed. Otherwise treat HF egress
   news as just useful information, not an action trigger.
2. **User gave an explicit task in their first message?** Do that.
   It overrides the Pending list below.
3. **Neither of the above?** Work `§State of the Union`'s Pending list,
   top to bottom.

If this file and a handoff disagree, **this file wins**.

---

## Cache Prompting + Sub-Agent Rules

### Prompt-cache warm-up — happens ONCE, at session start, and is mandatory there
- The initial prompt is where the cacheable prefix (system context + tools)
  gets established — this warm-up is not optional, it's the entire point of
  a session start. Don't skip it or treat "warming up" as busywork to avoid.
- Once established, that prefix is reused for the rest of the session under
  the **1-hour prompt-cache TTL** — no need to re-touch it or re-warm it
  mid-session just because time passed. Only schedule a wakeup/check-in for
  something you're actually waiting on (CI, a background job, an external
  event); match the delay to that thing, not to the cache window.
- If a session goes quiet long enough to fall out of the 1h window (or the
  account enters usage overage, which drops the TTL to 5 minutes), that's a
  cost to notice, not a problem to prevent — don't restructure work around
  avoiding it.

### Tool loading — attach at the start, don't fragment the cache mid-session
- **Tools the task is known to need get attached with the initial prompt**,
  at session start — that's part of the same warm-up as above. Loading a
  tool's schema mid-session (each `ToolSearch` resolution) changes the tool
  list, which perturbs/fragments the cached prefix from that point forward.
- **If something unanticipated comes up mid-session that needs a tool not
  loaded at the start, don't fetch it immediately.** Flag it, and request it
  at the start of the **next** session/prompt cycle instead — don't break
  the current cached prefix mid-stream for a one-off need.
- MCP and other deferred tools still arrive as names only (in
  `<system-reminder>` deferred-tool lists) until resolved via
  `ToolSearch(query="select:<tool_name>")` — the point above is about
  *when* to resolve them (up front, not scattered through the session), not
  whether the deferred mechanism itself exists.

### File edits — batch at the end, same reasoning as tool-loading
- File edits perturb the working session the same way tool-loading does.
  Gather everything that needs to change, get it confirmed, THEN execute
  the edits together at the end of the review — don't interleave edits
  throughout an open-ended review/discussion.

### Hard rules — Anthropic API `cache_control` mechanics (moved here from
### the former standalone `rules/CACHE_PROMPT_RULES.md`, session 16)

These apply when code in this project calls the Anthropic API directly
with explicit `cache_control` headers (e.g. a future orchestrator) — not
to this Claude Code session's own tool-call mechanics, which the sections
above already cover.

1. Max 4 `cache_control` markers per request: tools → system block →
   history-summary → reserved mid-conversation.
2. `cache_control` lands on the last block of the prefix to cache —
   everything before it is the key.
3. Pre-warm before sub-agent fan-out: one `max_tokens: 1` call so the
   cache is written before parallel reads begin — without it, agents
   fired at the same millisecond all miss and all pay a full write.
   Caches are workspace-scoped, not per-agent.
4. Never edit the cached prefix mid-session — batch edits between
   sessions (mid-session edits invalidate at 1.25x/5m TTL or 2x/1h TTL).
5. TTL selection: 5m for single sessions ≤5 min between turns; 1h for
   sub-agent fan-out or multi-turn over an hour. Below ~3 reads, 1h
   doesn't pay back.
6. Verify hits — check `lastUsage.cacheReadTokens > 0` after each call.
7. Cache key minimum is 1024 tokens; below that, no cache activity even
   with `cache_control` set.
8. 20-block lookback — a miss walks back at most 20 blocks; a growing
   conversation that outruns this needs a second breakpoint before the
   first falls out of the window.
9. Mixing TTLs in one prompt: longer-TTL blocks sit before shorter-TTL
   blocks (1h segments at the top, 5m segments below).
10. The cache is not portable — in-memory only, gone once a session goes
    quiet past its TTL. Carry over the source text between
    sessions/days, not the cache itself; re-send it to re-warm.
11. **1h TTL is not guaranteed** even on a Claude subscription — verify
    per-call which TTL actually applied (`usage.cache_creation
    .ephemeral_1h_input_tokens` vs `.ephemeral_5m_input_tokens` in the
    API response) rather than trusting it blindly. On API-key/Bedrock/
    GCP/Foundry auth (5m by default), `ENABLE_PROMPT_CACHING_1H=1` opts
    into 1h, `FORCE_PROMPT_CACHING_5M=1` forces 5m.
12. **Three separate "sub-agent cache" systems in this repo — don't
    conflate them:** (a) rules #3/#5 above apply only when this
    project's own code calls the Anthropic API directly with its own
    `cache_control` headers; (b) a Claude Code session's own `Agent`-tool
    sub-agents always start a cold 5-minute-TTL cache regardless of the
    parent session's TTL — no setting changes this, only a *fork*
    (inherits the parent's exact system prompt/tools/history) reads the
    parent's warm cache; (c) `sub-agent.agent.yaml`/`agents/build-runner.yaml`'s
    `metadata.cache_ttl_default: 1h` is a third, unrelated system — the
    separate `ant beta:agents create` deployment path's own knob.

### When to spawn
- Open-ended exploration spanning more than 3 files → `Explore` agent
- Independent background research that doesn't block current work → background agent
- Any task finishable inline in under 3 tool calls → do NOT spawn

### How to brief every sub-agent
Every sub-agent prompt must include:
- Repo: `c10vis-poem/Novus-Agenti`, and the correct branch for the track
  (`claude/project-scope-review-lf615p` for compile work, PR #4; for
  app/UI-fork work, the CURRENT session's harness-cut branch — see the
  resume prompt above; old app branches are merged, do not reuse)
- Instruction to read CLAUDE.md before acting
- The exact task (not open-ended)
- What NOT to do (no commits to main, no pushing other branches)
- **The same warm-up rules as the parent session**: attach the tools this
  task is known to need at the start (with this brief), don't fragment the
  cache by resolving new ones mid-task unless truly unanticipated — and if
  something unanticipated comes up, flag it for the next at-bat rather than
  fetching it ad hoc. Batch any file edits at the end once the task's scope
  is fully clear, not interleaved throughout.

**Template:**
```
Repo: c10vis-poem/Novus-Agenti, branch <the branch matching your track — see
CLAUDE.md's resume prompt for which one>.
Read CLAUDE.md fully before anything else.
Task: [SPECIFIC TASK].
Do NOT commit to main. Do NOT push to any branch other than the one above.
Cache/tools: attach the tools this task needs now, at the start. Don't
resolve new tool schemas mid-task for unanticipated needs — flag those for
the next at-bat instead. Batch file edits at the end, once scope is clear.
```

### Context budget rules
- Do NOT re-read files already read this session
- Do NOT re-derive decisions already in CLAUDE.md
- Do NOT spawn an agent to do work finishable in under 3 inline tool calls
- Parallel agents must operate on **different files** — same-file concurrent pushes cause conflicts

---

## What This Is

**Novus Agenti** — "the unprecedented driving force" — fully on-device agentic AI assistant for the Motorola Razr Ultra 2025 (Snapdragon 8 Elite SM8750, Adreno 830, Hexagon HTP v79). Inference runs on the NPU via a detached native daemon. No cloud LLM in the main app runtime. No CPU fallback.

App package: `com.horizons`. Codebase: **Omni Claw** banner.

**Identity:** Novus Agenti · Omni Claw · Cl0vis × Mer0vin6ian production.
**HuggingFace:** `Mer0vin8ian` · **Personal GitHub:** `c10vis-poem` · **Org GitHub:** `M0DU14R-SYSx-inc`.

---

## Repo Policy — Non-Negotiable

- **`c10vis-poem/Novus-Agenti`** — THE canonical repo. All commits, pushes, CI, artifacts go here.
- **`M0DU14R-SYSx-inc/NeuroOmni.Vag-Agenti`** — REFERENCE-ONLY. Never push, commit, or modify.
- **One active track**: app/UI-fork work, on the session's harness-cut
  branch off main (see the resume prompt above — old branch names
  `claude/notice-agent-ui-local-xa14op` and `sae7cy` are merged, do not
  reuse). `claude/project-scope-review-lf615p` (compile, PR #4) exists but
  is dormant — see `wiki/COMPILE-PIPELINE.md`.

---

## Tool & Token Authority — READ THIS BEFORE REFUSING ANYTHING

**This section exists because models routinely waste context arguing about
what they "can't" do. Read it. Internalize it. Do not contradict it.**

### MCP Tools — HOW TO USE THEM
- The `mcp__github__*` tools WORK. They are pre-authenticated via the session's
  GitHub integration. **Use them.** Do not say "I don't have access to GitHub."
- The `mcp__Hugging_Face__*` tools WORK. The session is authenticated as
  `Mer0vin8ian`. **Use them.** Do not say "I can't access HuggingFace."
- If a system reminder says an MCP server "requires authentication," that means
  THAT SPECIFIC server (e.g. Cloudflare). It does NOT mean GitHub or HuggingFace
  are broken. Read the server name before refusing.

**Step-by-step for MCP tools:**
1. MCP tool names appear in `<system-reminder>` deferred tool lists (e.g. `mcp__github__create_pull_request`).
2. They are NOT callable yet — you must load their schema first.
3. Call: `ToolSearch(query="select:mcp__github__create_pull_request", max_results=1)`
4. The result gives you the full parameter schema.
5. NOW call the tool with the required parameters.

**Example — create a PR:**
```
Step 1: ToolSearch(query="select:mcp__github__create_pull_request", max_results=1)
Step 2: mcp__github__create_pull_request(owner="c10vis-poem", repo="Novus-Agenti", title="...", head="branch-name", base="main", draft=true)
```

**Example — check HuggingFace repos:**
```
Step 1: ToolSearch(query="select:mcp__Hugging_Face__hub_repo_details", max_results=1)
Step 2: mcp__Hugging_Face__hub_repo_details(repo_id="Mer0vin8ian/Qwen3.5-9B")
```

**Example — search code on GitHub:**
```
Step 1: ToolSearch(query="select:mcp__github__search_code", max_results=1)
Step 2: mcp__github__search_code(q="NpuClient repo:c10vis-poem/Novus-Agenti")
```

Do NOT say "I don't have that tool." Load it, then use it.

### Tokens (HF_TOKEN, QAI_HUB_API_TOKEN) — HOW TO USE THEM
- `HF_TOKEN` and `QAI_HUB_API_TOKEN` are set as **Environment Variables on
  the cloud environment's settings** (the web dialog's "Environment
  variables" field, `.env` format) — NOT hardcoded anywhere in this repo.
  If they're set there, they are already present in `$HF_TOKEN` /
  `$QAI_HUB_API_TOKEN` in every session's shell — no export step needed,
  no reconstruction, no split-string trick.
- **Never write the actual token values into this file, a script, a
  commit, or any file that gets pushed.** This repo is public (confirmed
  via the GitHub API — `"private": false`), so anything committed here is
  world-readable permanently, including in git history after deletion.
- If `$HF_TOKEN` / `$QAI_HUB_API_TOKEN` are unset in a session, that means
  they haven't been added to the environment config yet (or you're on an
  environment that doesn't have them) — tell the user, don't fabricate a
  value and don't ask them to paste the raw token into chat or a file.

**Step-by-step for any `hf`, `huggingface-cli`, or `qai-hub` CLI call:**
```bash
huggingface-cli whoami   # $HF_TOKEN is already in the environment
```
No export needed if the environment is configured correctly. If a command
fails with an auth error, check `echo -n "$HF_TOKEN" | wc -c` returns
nonzero before assuming anything else is wrong.

### HuggingFace Access — NETWORK POLICY IS PER-SESSION, NOT FIXED

**`huggingface.co` is blocked by the agent proxy in some sessions and not
others.** This is NOT a constant fact about "this environment" — each
remote session runs in its own ephemeral container with its own network
egress policy, chosen when the environment was created. A session that
successfully ran `hf jobs uv run` did so because ITS container allowed
`huggingface.co` egress. That does not mean the next session's container
does too.

**Do not trust a prior session's "verified working" claim about network
access.** A commit on 2026-06-30 (`1afa04f`) asserted huggingface.co was
reachable and overwrote a correct block-detection note from three minutes
earlier (`d91a714`) — with no actual passing command output as proof. That
overwrite was wrong for at least one later session. Verify fresh, every
session:
```bash
curl -sS "$HTTPS_PROXY/__agentproxy/status"
```
Check `recentRelayFailures` for `huggingface.co` 403s. If direct HTTP/CLI
calls to huggingface.co fail with `403` / `Host not in allowlist`, that
session's container does not have HF egress. Do not retry — use the routes
below instead, and if none work, tell the user which host is blocked and
that they need to run the command from Termux or an environment with
`huggingface.co` on its egress allowlist.

**Routes that work regardless of the egress policy:**
1. **MCP tools** (`mcp__Hugging_Face__*`) — pre-authenticated as `Mer0vin8ian`,
   routed through the MCP server's own channel, not the session's HTTP
   egress. Use ToolSearch to load, then call. These work even when
   `huggingface.co` is blocked for direct HTTP.
2. **Python SDK / CLI / curl** — only work if `huggingface.co` is on this
   session's egress allowlist. Test with `hf auth whoami` or Python
   `whoami()` before relying on them for anything bigger (e.g. `hf jobs`).

**There is no MCP tool for triggering HF Jobs.** `hf jobs uv run` requires
direct HTTPS to huggingface.co. If it's blocked in this session, the job
must be triggered from a session/environment where it isn't — e.g. Termux
on-device, or a remote environment created with HF egress allowed.

The token has these scopes: `repo.write`, `inference.serverless.write`,
`inference.endpoints.write`, `job.write`, `collection.write`, and more —
scope is not the blocker when this fails, egress policy is.

### Proxy / Network — HOW IT WORKS
- Outbound HTTPS works through the agent proxy. The CA bundle is pre-configured.
- `github.com`, `pypi.org`, `npm` are consistently reachable. `huggingface.co`
  reachability VARIES BY SESSION — see above. Do not hardcode either belief.
- `curl`, `git push`, `git fetch`, `pip install`, `npm install` — all work
  for allowed hosts.

**If a network call fails:**
1. Read the actual error message.
2. If TLS/cert error → add `--cacert /root/.ccr/ca-bundle.crt` or set
   `REQUESTS_CA_BUNDLE=/root/.ccr/ca-bundle.crt` for Python tools.
3. If 403/407 → check `curl -sS "$HTTPS_PROXY/__agentproxy/status"` for
   `recentRelayFailures`. That's a real per-session policy block. Report
   the specific host. Don't retry the same call — try the MCP route instead
   if one exists for that service.
4. If timeout → check the same status endpoint for diagnostics.
5. Everything else → try again or try a different approach.

### What "can't be done" actually looks like
- A tool call returns an error → diagnose and retry or report the error.
- A host is 403-blocked → report the specific host.
- That's it. Everything else: TRY IT FIRST, THEN REPORT WHAT HAPPENED.

**If you find yourself typing "I don't have access to," "I can't use," or
"that requires authentication" — STOP. Re-read this section. Then try the
tool call. If it fails, report the actual error. Do not pre-refuse.**

---

## Tokens / Secrets — LIVE IN THE ENVIRONMENT CONFIG, NOT HERE

`HF_TOKEN` and `QAI_HUB_API_TOKEN` are set as Environment Variables on the
cloud environment used for this repo's sessions (web UI → environment
settings → "Environment variables" field). They arrive pre-exported in
every session's shell — no reconstruction step, no split-string workaround.

**This file used to hardcode both tokens split across two shell variables
to dodge the git secret scanner.** That was removed on 2026-07-02: this
repo is confirmed **public** (`"private": false` via the GitHub API), so a
split string committed here was never actually hidden from anyone — it was
trivially reconstructable by concatenation, same as a scanner would flag a
contiguous one. Both tokens were rotated after this was caught; if you're
reading an older clone or cached version of this file with token halves in
it, those values are dead and rotation already happened.

If a session ever needs a token that isn't in its environment: tell the
user to add it via the environment's settings dialog. Never write a real
token value into this file, a commit, a script, or a chat reply that ends
up in a shareable transcript.

---

## Compile Pipeline — Qwen3.5-9B (dormant fallback)

Moved to `wiki/COMPILE-PIPELINE.md` — the Single-Path Architecture table,
Size Envelope, Hexagon HTP Constraints, and Job 8 Trigger Command all live
there now, rewritten to make clear this whole pipeline is the **fallback**,
not the primary plan (primary = Q4_0 GGUF via GenieX's GGML backend, no
compile step). Don't trigger Job 8 without confirming a hard failure on
that primary path first.

The Horizons app itself is multi-runtime by design regardless of which
pipeline produced a given model: each model family ships with its own
uploadable runtime binary (`ort_engine` for ORT+QNN today, `geniex serve`
once GenieX is forked and wired, future binaries for ExecuTorch/SNPE/TFLite/
Jetson Tensor targets). Adding a new runtime is "drop the binary in via
ModelImportActivity, register it in `RUNTIME_FILES`."

`daemon/src/` (`ort_engine`) is real, CI-built code, not scaffolding — but
it's the **legacy** runtime now that GenieX is the decided plan (session
15). Its wire contract (`POST http://127.0.0.1:8080/api/v1/generate`,
including the `image_b64` field added session 16 for vision) is what
`NpuClient.kt` speaks today; that contract shape is expected to carry
forward conceptually when GenieX replaces what's listening behind the
socket, not to require a branch merge — there's no separate GenieX branch
yet to merge with (it hasn't been forked into `c10vis-poem` at all yet).

---

## Android App / Battery Rules

**All three of the below are ALREADY WIRED — this section used to claim
otherwise (stale since at least session 12-13); verified directly against
the code during session 16's cleanup.** Both NpuManager + Game SDK boosts
are required together: Game SDK boosts UI/scheduler only; NpuManager lock
is what gives the NPU daemon full performance.

### NpuManager Performance Lock — WIRED
`CliffordService.acquireNpuPerfLock()`/`releaseNpuPerfLock()` (reflection-based,
since `NpuManager` is an `@hide` system service on Qualcomm BSPs) — called
from the CRS loop once the daemon reports healthy.

### Game SDK Performance Mode — WIRED (better API than this section used to show)
`core/perf/GameModeBoost.kt` — uses the modern per-thread ADPF API
(`GameManager.setGameState(GameState(...))` + `PerfHintSession`), not the
older `setGameMode(GameMode.PERFORMANCE)` call this section used to
document. Wrapped around every LLM stream (`HorizonsApplication.sendChat()`)
and the voice loop (`.gameBoosted()`).

### Manifest — WIRED
`android.permission.HIGH_PERFORMANCE` and `android.hardware.game`
(`uses-feature`) are both present in `AndroidManifest.xml`.
`CliffordService`'s `foregroundServiceType="specialUse"` is also present.

---

## State of the Union — 2026-07-14 (session 17)

This is the **only** current-state doc in this repo — updated in place
each session, not accumulated as a new file per session. Historical
session-by-session detail lives in git history (commit log + old PR
diffs), not here — if you need "what happened in session 12," check the
log, don't expect it copy-pasted in this file.

### Current state

- **Session 17**: session-16 branch merged to main; work continues on the
  harness-cut branch `mer0vin6ian/practical-bell-3t735g`. **Boot/loading
  sequence is now specified** — `wiki/BOOT-SEQUENCE.md` (was Pending 4):
  process topology, 3 entry points, Phases 1–4, the liveness-vs-readiness
  and serve-first invariants as a checklist for future boot-path changes.
  Sibling infra lane (AESOP T3 bring-up: Tailscale → Jetson → Rubik Pi)
  was briefed to the operator separately — it lives in the `aesop` repo's
  orbit, NOT here; don't pick it up from this file.
- **Pending 1 (GenieX) NEARLY DONE, session 17**: operator forked
  `c10vis-poem/geniex`; source read; QAIRT manual ingested (per operator
  directive — findings in GENIEX-DAEMON-PLAN.md). Shipped and CI-GREEN:
  `core/llm/GenieXClient.kt` (OpenAI wire, /v1/models readiness) +
  **`geniex-daemon/`** (thin C++ over libgeniex's C API — SSE streaming
  via token callback, chat template, serve-first) + CI building the
  GenieX SDK from the fork (llama_cpp/GGML plugin ON, qairt OFF in CI —
  device bundle already has that backend; Rust model-manager needs
  rustup android target + ANDROID_NDK_ROOT, fixed). Artifacts on
  latest-debug: `geniex_daemon`, `libgeniex.so`,
  `geniex-plugins-arm64.tar.gz`. REMAINING: on-device run (operator) +
  activation swap (CliffordService/HorizonsApplication still guard/
  activate ort_engine+NpuClient — swap after on-device verification).
- **Pending 2 (media daemon) BUILT, session 17, CI-GREEN**:
  `media-daemon/` — sherpa-onnx C API daemon (Moonshine STT + Kokoro
  TTS) on :8091 serving the existing DaemonSttClient/DaemonTtsClient
  contract; serve-first; Content-Length-safe HTTP. CI builds sherpa-onnx
  from `c10vis-poem/sherpa-onnx` (cached) + `media_daemon`;
  CliffordService launches/guards it (marker-file model-dir discovery).
  REMAINING: on-device run against the kokoro-multi-lang +
  moonshine-base-int8 bundles.
- **"Make the app work" pass (operator directive, session 17)**: Termux
  terminal mode (`TermuxRunner` — RUN_COMMAND, bash -lc, hard timeout;
  ShellTab toggle) so the on-device openwiki/node toolchain runs from
  the app; Silero VAD actually wired (model resolves from models//
  Download — the asset was never shipped, RMS fallback was silently
  active; drop `silero_vad.onnx` in /Download to activate); cloud-key
  saves refresh backend status; LLM-model scanner excludes aux models
  (vad/moonshine/kokoro); ModelImportActivity knows all new binaries.
- **App/UI-fork track is the one active track** (session-16 work merged).
  Horizons app is code-complete UI (Compose, 6 panels, home grid, chat,
  terminal, browser tab); daemon/watchdog architecture (`CliffordService`,
  `DaemonLauncher`) is real and working; a new additive local-first UI fork
  (`com.horizons.uilocal.LocalHomeActivity`) boots without gating on the
  model daemon and shows independent status for the model+vision daemon vs.
  the media (STT/TTS) daemon.
- **Compile pipeline is dormant** — fallback only, see
  `wiki/COMPILE-PIPELINE.md`. Don't trigger Job 8 without a confirmed hard
  failure on the primary GGUF/GenieX path.
- **NpuManager lock, Game SDK boost, and the manifest permission/feature
  entries are all already wired** — see `§Android App / Battery Rules`.
  This file used to claim otherwise for several sessions; corrected.
- **Vision lives in the same daemon/process as the LLM**; STT+TTS are a
  separate media daemon. `NpuClient.kt` carries an `image_b64` field
  end-to-end to `ort_engine`; `DaemonTtsClient` is the TTS half of the
  media-daemon client, mirroring `DaemonSttClient`. Neither GenieX nor a
  real media-daemon binary exist as processes yet — this is contract +
  scaffold work, not full runtime implementation.
- **Known gap**: `daemon/src/http_server.cpp` reads a single `recv()` into
  an 8KB buffer — image payloads (100KB+) will be truncated until it reads
  until Content-Length. Documented inline in that file.
- **Doc/knowledge/folder restructure (this session)**: `knowledge/daemon-reference/`
  + `knowledge/claude-code-reference/` (moved from `wiki/`), `knowledge/qairt-sdk/htp.jsonl`
  (new — completes that topic's triplet), `wiki/COMPILE-PIPELINE.md` and
  `wiki/JOB_EXECUTION_LOG.md` (new, replacing several older files),
  `skills/project-memory/SKILL.md` redesigned around the knowledge/ corpus
  instead of being a redundant CLAUDE.md/SOTU re-read. `rules/CACHE_PROMPT_RULES.md`
  merged into this file's Cache Prompting section (no longer a separate
  file). `models/` + `scripts/` merged into `compile/` (same dormant-pipeline
  domain). Top-level folder count: 13 → 12 (`compile/` absorbed two).
  `RESOURCE-DOCS-WIKI.md` (402KB duplicate of the knowledge/ corpus) deleted.
  `knowledge/device-inventory/DEVICE-INVENTORY.md` recovered from the
  deleted `wiki/APP-SOTU-AUDIT.md` — that file bundled a real device
  inventory (SDKs, model files, Termux toolchain, 2026-07-13 snapshot)
  together with a now-stale status narrative; only the factual inventory
  was worth keeping, so it's preserved here rather than lost entirely.

### Standing decisions — LAW, not to re-litigate

- **Runtime: GenieX on the QAIRT/HTP SDK backend**, wired to a separate
  detached daemon (`geniex serve`, OpenAI-compatible wire on `:18181/v1`).
  `ort_engine` is the legacy runtime — real, CI-built, still in the repo,
  not the Qwen3.5-9B path going forward. `GenieX` (`github.com/qualcomm/GenieX`)
  is NOT yet forked into `c10vis-poem` — that's the next real runtime step.
  Full detail: `wiki/GENIEX-DAEMON-PLAN.md`.
- **Never invent priority.** The operator's labels and ordering ARE the
  priority. Don't reorder, re-scope, or substitute your own judgement. If
  unsure what's next, ask.
- **OmniNeural / Nexa SDK = dead.** Reference-only for reverse-engineering.
- **Orchestrator/sub-agent model**: brief per the sub-agent template, hold
  sub-agents to their brief; a background agent that hasn't returned in
  hours is dead — do the work inline instead of waiting.

### Pending — in order

1. **GenieX fork + wire** — fork `qualcomm/GenieX` → `c10vis-poem/GenieX`,
   then work `wiki/GENIEX-DAEMON-PLAN.md`'s next-steps list. Per
   `knowledge/device-inventory/DEVICE-INVENTORY.md`, the device already has
   the prebuilt `geniex-bench-android-arm64 v0.3.14` (both GGML and QAIRT
   backends) + the Q4_0 GGUF + HTP v79 libs sitting in
   `/storage/emulated/0/Download/` as of 2026-07-13 — don't re-download,
   re-verify what's there first. Once wired, update `compile/manifest.yaml`'s
   `daemon_binaries.genie_x.status` (currently "not yet forked/wired").
2. **Real media-daemon binary** — Moonshine STT + Kokoro/Sherpa TTS as a
   detached process on `127.0.0.1:8091`; currently only client-side
   contracts exist (`DaemonSttClient`, `DaemonTtsClient`), nothing binds
   that port yet. Also update `compile/manifest.yaml` once this exists —
   it's not currently listed there at all.
3. **Fix `http_server.cpp`'s recv() truncation** before vision can actually
   round-trip end to end.
4. ~~Boot/loading-phase sequencing~~ — DONE session 17:
   `wiki/BOOT-SEQUENCE.md` is the authoritative init-order contract.
   Future boot-path changes check its invariants list.
5. **Cloud connectors** — OpenRouter works; OmniRoute, GitHub, HuggingFace,
   QAI Hub, GCS still need wiring (`CloudLlmRuntime`, agent tools).
6. **Tailscale** — route to home node, not yet installed/wired.
7. **Chat history export** — `ChatHistoryStore` saves locally, no export/sync.
8. **RouterPane "routing rules"** — deliberately not built; needs a real
   rule engine, not UI toggles with no behavioral effect.
9. **SettingsPane "Themes"** — deliberately not built; `HorizonsColors` is
   currently a flat hardcoded object, needs a switchable palette system.
10. **Three orphaned-but-real classes**: `core/log/InteractionLogger.kt`,
    `core/shell/SecureResourceRelay.kt`, `core/screen/ScreenshotCapture.kt`
    — fully implemented, never wired to any caller. Confirm with operator
    before deleting; likely mid-flight features, not cruft.
11. **CI publish-target TODO** in `build-apk.yml` — unconfirmed as still
    real; verify against a live release page before touching.

---

## Repo File Map

```
c10vis-poem/Novus-Agenti  (public — confirmed via GitHub API, not private)

CLAUDE.md                     ← THIS FILE (architecture-of-record + current SOTU)
agents/
  build-runner.yaml             horizons-build-runner (Android CI, separate from compile)
  sub-agent.system.md           Novus-Agenti stack (single canonical agent brief)
daemon/                          ort_engine C++ daemon (legacy runtime, CI-built)
  src/engine.cpp, http_server.cpp, tokenizer.cpp, sampler.h, main.cpp
rules/
  AAR_DECOMPILE.md              QNN artifact inspection (archived, Nexa-era)
  AT_BAT_PROTOCOL.md
  CACHE_PROMPT_RULES.md
  GIT_HYGIENE.md
skills/
  horizons-wiki/SKILL.md        architecture bundle (CLAUDE.md + daemon-reference)
  project-memory/SKILL.md       knowledge/ corpus retrieval (two-tier)
  termux-mobile-dev/SKILL.md
knowledge/                       project knowledge corpus (see README.md)
  omni-claw-defined/             ALWAYS-READ core project definition
  research-npu/  proofs/  fragmented-qat/  google-dev-docs/  gemini-query/
                                  Drive-mirrored, retrieve-on-demand
  qairt-sdk/                      Drive-mirrored (QNN HTP manual, .md + .jsonl)
  daemon-reference/               repo-native (moved from wiki/): GPT-DAEMON-REFERENCE.md,
                                  NPU-RUNTIME-PATHS.md
  claude-code-reference/          general Claude Code knowledge (moved from wiki/):
                                  PROMPT-CACHING.md — reference only, hard rules are in
                                  this file's Cache Prompting section, not there
  device-inventory/               recovered on-device audit snapshot (2026-07-13):
                                  DEVICE-INVENTORY.md — SDKs, model files, Termux
                                  toolchain actually on the Razr Ultra; re-verify
                                  before trusting exact versions/sizes
compile/                        dormant compile-pipeline domain (was models/ + scripts/,
                                  merged since both only ever served this one pipeline)
  manifest.yaml                  FALLBACK ONLY — see its own header
  compile_qwen3_5_9b.py          fallback compile script (dormant, see wiki/COMPILE-PIPELINE.md)
  requirements-compile.txt       pip deps for the staged Colab compile
wiki/
  BOOT-SEQUENCE.md               authoritative boot/loading-phase contract (session 17)
  COMPILE-PIPELINE.md            dormant fallback pipeline (Single-Path Architecture,
                                  Size Envelope, Hexagon HTP Constraints, Job 8 command)
  GENIEX-DAEMON-PLAN.md          GenieX runtime plan + model/vision daemon split
  JOB_EXECUTION_LOG.md           combined compile-job + strike/failure ledger
  FEATURE-SPEC.md                UI tile spec
  BUILD-ACTION-PLAN.md
  research/                      reference notes on forked tools (android-reverse-engineering-skill,
                                  claude-skills) — not project architecture, kept separate from knowledge/
horizons/                        Android app
  fgs/CliffordService.kt         Watchdog daemon
  core/llm/NpuClient.kt          model+vision daemon client
  core/stt/DaemonSttClient.kt    media daemon client (STT half)
  core/tts/DaemonTtsClient.kt    media daemon client (TTS half, contract only)
  core/shell/DaemonLauncher.kt
  core/agent/AgentLoop.kt
  uilocal/LocalHomeActivity.kt   local UI fork (session 16), additive
.github/workflows/build-apk.yml
release/debug.keystore           committed by design
```
`watchdog/` was already deleted — don't look for it. There is no
per-session handoff file (`wiki/SESSION{N}-HANDOFF.md`) or standalone
`wiki/APP-SOTU-AUDIT.md`/`wiki/FAILURE_LOG.md` anymore — consolidated
into this file's SOTU and `wiki/JOB_EXECUTION_LOG.md` respectively.
`.github/workflows/build-apk.yml`'s publish-target TODO could not be
confirmed still real (no foreign repo found hardcoded anywhere in its
history) — verify against a live release page before assuming it needs
work.

---

## Hard Rules

- Never push `main` without explicit user permission
- Never `--no-verify`, `push --force`, `reset --hard` without confirming
- No CPU fallback in the Qwen3.5-9B path (NPU or nothing for that model)
- No in-process tensor runtime — every model runs via its own uploadable daemon binary
- `M0DU14R-SYSx-inc/NeuroOmni.Vag-Agenti` is REFERENCE-ONLY
- Don't trigger the dormant compile pipeline pre-emptively — see
  `wiki/COMPILE-PIPELINE.md` for its own hard rules (`SKIP_VISION`,
  `max_dynamic_tensor_size_mib`), which only matter if/when that pipeline
  actually runs

---

## Build / CI

- AGP 8.8.0 · Kotlin 2.1.0 · compileSdk 35 · minSdk 31 · JDK 17 · arm64-v8a only
- Signing: `release/debug.keystore` (committed by design)
- `build-apk.yml` cross-compiles `ort_engine` (daemon/) via CMake/NDK,
  builds the APK, publishes both plus `libonnxruntime.so` to a
  `latest-debug` GitHub Release. Publish target already defaults to
  this repo (`softprops/action-gh-release@v2` has no `repository:`
  override) — an old TODO here claiming otherwise could not be verified
  as still real; if a CI run's release step actually misfires, check
  the repo's Settings → Actions → General → Workflow permissions first
  (that's what broke it once this session, not the publish-target).

---

## Brand

- Background `#222C34` · Surface `#35414A` · Primary teal `#2DD4D9`
- Highlight teal `#4FE7EC` · Icon backplate `#050709` · Action yellow `#F5C518`
- Backdrop: pure Compose `Brush.radialGradient` — NOT XML shape

---

## Termux / Mobile Rules

**Device:** Motorola Razr Ultra 2025 · SM8750 · 16GB · Hexagon HTP v79. **Phone only. No laptop.**

- No tokens or long URLs in paste-able commands
- Shell variables: short alias then `$VAR`
- Every paste-able command under ~50 chars where possible

---

## What Was Ripped Out — Do NOT Reference

Scoped to the Qwen3.5-9B build path. Other model families ship their own
runtime binaries; this table is not a constraint on future runtimes.

| Old | Replaced by |
|---|---|
| Track 1 / Track 2 (for Qwen3.5-9B) | single path: ONNX → QNN context binary → Hexagon HTP |
| LiteRT / LiteRT-LM (for Qwen3.5-9B) | ort_engine daemon |
| genie_engine (for Qwen3.5-9B) | ort_engine (ORT + QNN EP) |
| Separate Watchdog | CliffordService (CLIFFORD == Watchdog) |
| Nexa SDK, OmniNeural | dead |
| Cloud failover in app LLM | HttpFetch agent tool |
