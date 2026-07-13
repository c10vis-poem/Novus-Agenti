# CLAUDE.md — Novus Agenti / Omni Claw

> **RESUME PROMPT — COPY THIS BLOCK VERBATIM TO START ANY NEW SESSION**
>
> ```
> Project: Novus Agenti (Omni Claw). Mission: compile Mer0vin8ian/Qwen3.5-9B
> → Hexagon HTP v79 (SM8750, Snapdragon 8 Elite) qnn_context_binary via QAI Hub.
> Canonical repo: c10vis-poem/Novus-Agenti. TWO ACTIVE TRACKS —
> pick the one matching your actual task, don't assume there's only one:
>   - COMPILE track (ONNX export, QAI Hub, Job 8): branch
>     claude/project-scope-review-lf615p, PR #4
>   - APP track (Android app, daemon, UI, knowledge layer): branch
>     claude/on-device-inference-openwiki-sae7cy, PR #15 — CURRENT app
>     work (session 15: daemon crash-loop fix, in-app browser, knowledge/
>     corpus). PR #8 (claude/horizons-closeout-hf-review-ycjkm3) already
>     MERGED to main — do not reuse it.
>
> READ THESE IN ORDER BEFORE ANY ACTION:
>   1. CLAUDE.md (full read, all sections)
>   2. wiki/GPT-DAEMON-REFERENCE.md (full read)
>   3. wiki/NPU-RUNTIME-PATHS.md (full read)
>   4. wiki/SESSION{N}-HANDOFF.md (latest N)
>   5. knowledge/README.md (byte-faithful master-wiki corpus — the operator's
>      finished portable knowledge layer; NEVER re-process or re-summarize it)
>   6. models/manifest.yaml
>   7. scripts/compile_qwen3_5_9b.py
>
> Use /memory slash command to reload full project context.
> After reading: state current SOTU, last job result, next action. Then wait.
> HF_TOKEN / QAI_HUB_API_TOKEN come from the environment config (§Tokens
> below) — already exported, no manual step needed. Never hardcode them.
> ```

---

## /memory — Slash Command

Type `/memory` in any Claude Code session to reload full project context.

**Sequence:**
1. Read `CLAUDE.md` (this file, all sections)
2. Read `wiki/GPT-DAEMON-REFERENCE.md`
3. Read `wiki/NPU-RUNTIME-PATHS.md`
4. Read latest `wiki/SESSION{N}-HANDOFF.md`
5. Read `knowledge/README.md` (master-wiki corpus map — copy, never re-process)
6. Read `models/manifest.yaml`
7. Read `scripts/compile_qwen3_5_9b.py`
8. Produce a SOTU summary and confirm next action before touching any file

---

## Order of Operations — Non-Negotiable

1. **Read** — the files above, in order, completely
2. **State** — current SOTU and next action
3. **Act** — check the priority tree below FIRST, before touching the
   general Pending list
4. **Document** — before ending: update SOTU in this file, write `wiki/SESSION{N+1}-HANDOFF.md`

### Act — priority tree (check top to bottom, stop at the first match)

Reading this file is not the same as acting on every item in it.
"Act" means: find which of these applies, do that, THEN fall through
to the general Pending list.

1. **HF egress just became available?** Run
   `curl -sS "$HTTPS_PROXY/__agentproxy/status"`. If `huggingface.co`
   is NOT in `recentRelayFailures` → run `hf auth whoami`, and if that
   returns `Mer0vin8ian`, your first action is triggering **Job 8**
   (command in `§Job 8 Trigger Command`). This is almost certainly why
   a session exists right after the operator reconfigures an
   environment's network access — don't spend that session on general
   cleanup before checking this.
2. **User gave an explicit task in their first message?** Do that.
   It overrides the Pending list below.
3. **Neither of the above?** Work `§State of the Union`'s Pending list,
   top to bottom.

If this file and a handoff disagree, **this file wins**.

---

## Cache Prompting + Sub-Agent Rules

### When to spawn
- Open-ended exploration spanning more than 3 files → `Explore` agent
- Independent background research that doesn't block current work → background agent
- Any task finishable inline in under 3 tool calls → do NOT spawn

### How to brief every sub-agent
Every sub-agent prompt must include:
- Repo: `c10vis-poem/Novus-Agenti`, and the correct branch for the track
  (`claude/project-scope-review-lf615p` for compile work, PR #4;
  `claude/horizons-closeout-hf-review-ycjkm3` for app work, PR #8)
- Instruction to read CLAUDE.md before acting
- The exact task (not open-ended)
- What NOT to do (no commits to main, no pushing other branches)

**Template:**
```
Repo: c10vis-poem/Novus-Agenti, branch <the branch matching your track — see
CLAUDE.md's resume prompt for which one>.
Read CLAUDE.md fully before anything else.
Task: [SPECIFIC TASK].
Do NOT commit to main. Do NOT push to any branch other than the one above.
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
- Two active working branches, one per track — see the resume prompt at
  the top of this file for which one matches your task:
  `claude/project-scope-review-lf615p` (compile, PR #4) and
  `claude/horizons-closeout-hf-review-ycjkm3` (app, PR #8).

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

## Single-Path Architecture — Qwen3.5-9B Build

Scoped to the Qwen3.5-9B → Hexagon HTP compile/deploy pipeline. The Horizons
app itself is multi-runtime by design: each model family ships with its own
uploadable runtime binary (e.g. `ort_engine` for ORT+QNN, future binaries for
ExecuTorch / SNPE / TFLite / Jetson Tensor targets). Adding a new runtime is
"drop the binary in via ModelImportActivity, register it in `RUNTIME_FILES`."

| Layer | What | Status |
|---|---|---|
| **Model** | `Mer0vin8ian/Qwen3.5-9B` — 9.65B params, `qwen3_5` arch. Multimodal via **deepstack vision injection** at decoder layers. NOT a separate encoder pipeline. | Source on HF Hub |
| **ONNX export** | `scripts/compile_qwen3_5_9b.py` on HF Jobs `cpu-xl` | M-RoPE fix committed. Job 8 pending. |
| **QAI Hub compile** | ONNX → `qnn_context_binary` (W4A16) server-side. | Job 8 pending |
| **Runtime: `ort_engine`** | ONNX Runtime + QNN Execution Provider on aarch64-android. Serves `POST http://127.0.0.1:8080/api/v1/generate`. | Scaffolded — `daemon/src/` has real `engine.cpp`/`http_server.cpp`/`tokenizer.cpp`/`sampler.h`/`main.cpp` (735 lines), CI cross-compiles it (`build-apk.yml` "Build ort_engine daemon" step) and packages it into the release artifact. Not yet verified on-device against a real compiled model. |
| **Daemon guardian** | `CliffordService` FGS — CLIFFORD == Watchdog. `START_STICKY`, `specialUse`, 15s CRS recovery loop. | In codebase |
| **Bridge** | `NpuClient.kt` → `POST http://127.0.0.1:8080/api/v1/generate` | In codebase |
| **Agent layer** | `AgentLoop` + 22 tools | In codebase |

---

## Size Envelope (Hard Caps)

| | Size | Notes |
|---|---|---|
| Target | **5.5 GB** | Shoot for this |
| Ideal ceiling | 6.0 GB | Acceptable |
| Redline | **7.0–7.2 GB** | Non-negotiable |
| W4A16 at max_seq=4096 | ~5.7 GB | ✓ inside ideal |
| W4A16 at max_seq=2048 | ~5.4 GB | ✓ inside target |

---

## Hexagon HTP Constraints

| Constraint | Applied as |
|---|---|
| RoPE fold (no FP16 Sin/Cos on Hexagon) | `make_folded_rope_forward` + `_patched_apply_rotary_pos_emb` |
| Static shapes | batch=1, MAX_SEQ_LEN fixed |
| `--disable_fusion` | In `COMPILE_OPTIONS_BASE` |
| `--bias_as_int32` | In `COMPILE_OPTIONS_BASE` |
| Scratch: 16 MiB | `--scratch_size_mib 16` |
| Dynamic tensor: 64 MiB (canonical) | `--max_dynamic_tensor_size_mib 64` |
| Single NPU context | `QNN_GRAPH_CONFIG_MAX_CONTEXTS=1` |
| Stateless prefill | `use_cache=False` in `HtpDecodeWrapper` |

---

## Android App / Battery Rules

### NpuManager Performance Lock (PENDING — NOT YET WIRED)
```kotlin
val npuManager = getSystemService(NpuManager::class.java)
val lock = npuManager.acquirePerformanceLock(NpuManager.PERF_MODE_HIGH)
```

### Game SDK Performance Mode (PENDING — NOT YET WIRED)
```kotlin
GameManager.getInstance(this).setGameMode(GameMode.PERFORMANCE)
```

### Manifest (PENDING)
```xml
<uses-feature android:name="android.hardware.game" android:required="true" />
<uses-permission android:name="android.permission.HIGH_PERFORMANCE" />
<service android:name=".CliffordService" android:foregroundServiceType="specialUse" />
```

**Both NpuManager + GameManager are required together.** Game SDK boosts UI scheduler only; NpuManager lock is what gives the NPU daemon full performance.

---

## State of the Union — 2026-07-11 (session 15)

> **APP STATUS — read `wiki/APP-SOTU-AUDIT.md` first.** Device-grounded honest
> audit (what's done / broken / priority order) + the operator's explicit
> remaining needs (cloud connectors, Chromium WebView socket, GCS/OpenRouter/
> OmniRoute/QAIHub/HF/GitHub, Tailscale to home node, chat-history export) + the
> real device inventory (QAIRT/Hexagon SDKs, GenieX-bench prebuilt w/ vision,
> Q4_0 GGUF, HTP v79 libs). **STT model = Moonshine small**, runs in the media
> daemon (CPU, no HTP). The voice/UI stack runs WITHOUT the on-device model —
> HTP/GenieX is an optional backend, not a boot requirement.

### Done — session 15
- **`knowledge/` corpus landed** (branch `claude/on-device-inference-openwiki-sae7cy`,
  PR #15). A **byte-faithful copy** of the operator's finished `Claude_master_wiki`
  Google Drive folder — 23 distilled `.md`/`.jsonl`/`.txt`/doc artifacts across six
  topic folders (omni-claw-defined, research-npu, proofs, fragmented-qat,
  google-dev-docs, gemini-query). Decoded base64→exact bytes, every size verified
  against Drive `fileSize`. See `knowledge/README.md` for folder↔Drive provenance.
  This IS the operator's portable knowledge layer, hand-distilled over ~18h — it is
  the ingestion source for the OpenWiki/OB1/reasoning-bank memory backend, NOT
  something to re-summarize, reflow, or re-compile. Skip nothing; copy faithfully.
- **Daemon crash-loop fixed + in-app browser** (same branch/PR, from earlier this
  session): `daemon/src/main.cpp` now loads the model on a background thread and
  binds :8080 immediately (a model-less daemon stays UP serving /health 503 instead
  of suiciding → no watchdog thrash); `CliffordService.kt` never relaunches a LIVE
  process and backs off/caps relaunch of a dead one; `TerminalPanel.kt` gained a
  real multi-page WebView "Browser" tab with a `window.OmniClaw` JS bridge.

### Standing directives from the operator (session 15) — these are LAW
- **QAIT>ORT — DECIDED (session 15).** Runtime layer for Qwen3.5-9B (and
  anything on QAIRT) is: **backend = HTP SDK (QAIRT), runtime = `GenieX`,
  wired to a SEPARATE detached daemon** (keeps the daemon/watchdog model +
  crash-loop fix + the "no in-process tensor runtime" hard rule — the LiteRT
  in-process `CompiledModel` option was rejected for breaking that rule). The
  framing that settled it: this is a **what-do-we-CONNECT-the-app-to**
  decision (backend/runtime behind the socket), not a what-do-we-compile-the-
  app-as decision.
  - `GenieX` is REAL and official: **`github.com/qualcomm/GenieX`** (~8k★,
    Rust, "run frontier LLMs/VLMs on Qualcomm NPU/GPU/CPU"; topics include
    `hexagon`, `qwen3`, `qwen3vl`, `snapdragon`). It is NOT the same as
    QNN's `genie-t2t-run` "Genie" — the runtime is **GenieX**, not Genie.
    A prior session wrongly flagged the repo as fabricated; corrected here.
  - **`ort_engine` becomes legacy** — one of several uploadable runtime
    binaries, not the Qwen3.5-9B path. The daemon/HTTP contract
    (`127.0.0.1:8080` per `NpuClient.kt`) stays; only what runs behind it
    changes.
  - **Not yet forked:** `qualcomm/GenieX` is not in the `c10vis-poem`
    profile yet (verified via GitHub search) — forking it is the next
    runtime step. QAIRT SDK reference lives in Drive `#QAIRT/` (full
    manual: Context/Backend/Api/Graph/Tensor/HTP/Overview chapters, PDF+mht)
    and `QAIRT>ORT/` (screenshots + numbered Gemini reply docs).
- **Never invent priority.** The operator's labels and ordering ARE the priority.
  Do not reorder, re-scope, or substitute your own judgement for what he flagged.
  If unsure what's next, ask — don't improvise a roadmap.
- **OmniNeural / Nexa SDK = DEAD.** Confirmed abandoned (Nexa's HF presence is
  effectively gone; the model is dead). Do not try to download a Nexa SDK or build
  on OmniNeural. Reference-only for reverse-engineering, nothing more.
- **Orchestrator model.** When the operator says to spin up parallel agents, brief
  each per the sub-agent template, give a 1h cache TTL, and hold them to it — if a
  background agent hasn't returned in hours, it's dead; do the work inline instead
  of waiting.

### Done — session 14
- **PR #8 merged to `main`** (merge commit `6188398`) — all of session 12-13's
  work (crash fixes, UI/perf metrics, security cleanup, full doc audit,
  priority tree) is now on `main`, not just a feature branch.
- **Fixed the actual reason the SessionStart hook kept not-committing**:
  `.claude/` was entirely gitignored. Carved out `.claude/hooks/` and
  `.claude/settings.json`; both are now really tracked on `main`
  (verified directly via `git ls-tree origin/main`).
- **`c10vis-poem/openwiki` fork**: merged a real feature to its `main`
  (`openwiki/SKILL.md` project-customization convention, "memory as a
  skill"), and started an `--audit` mode branch with an explicit
  guardrail against a real failure mode: a doc/code mismatch is NOT
  automatically "stale" — it's often a parallel session's newer,
  unseen work. Audit reports chronological evidence, never auto-deletes.
- **Three pathways identified for this project going forward** (see
  below) — the next session should orient around these, not just the
  flat Pending list.

### Three Pathways — orient here first, every session

1. **Compile pipeline (Qwen3.5-9B → Hexagon HTP)** — branch
   `claude/project-scope-review-lf615p`, PR #4. Has its own clean merge
   point and handoff already. Job 8 is the live blocker — check status
   before assuming it's untriggered.
2. **OpenWiki dual-role** — `c10vis-poem/openwiki` (separate repo). Two
   jobs here, not one: (a) build out OpenWiki itself (audit mode next,
   see `AUDIT_MODE_TODO.md` on branch `claude/audit-mode`), AND (b) **run
   OpenWiki against Novus-Agenti's own docs** — the operator's explicit
   observation this session: the discipline we've been hand-applying to
   CLAUDE.md/wiki/ all session (quickstart entrypoint, section pages,
   stale-claim detection, a SKILL.md-style memory layer) IS what OpenWiki
   is built to automate. Whoever works this pathway is coordinating BOTH:
   don't just develop the CLI, start actually using it here too.
3. **Horizons Android app** — **most stale pathway.** The app should
   already be a working assistant right now. Sessions have been
   "shooting from the hip" — making changes without grounding them in
   documented state, attached repos, or verified current progress. The
   next session on this pathway's explicit mandate: **audit first, off
   real sources** — this file, the wiki, the actual repo state, actual
   CI results — not assumption or improvisation. No new feature work
   until an honest audit confirms what's actually true.

### Done — session 12 (2026-07-02)
- App-side crash fixes: multi-process crash loop, ChatHistoryStore, Monitor
  card overflow — PR #8, CI green
- Real Performance Metrics (tokens/sec, first-token latency, device memory)
  wired into RouterPane from actual LlmRuntime stream timing
- Prompt/Script Library added to MonitorPane (real `SavedCommandStore`)
- Settings → Terminal/Artifacts quick-nav wired
- `wiki/GPT-OSS-Reference.md` → corrected to `wiki/GPT-DAEMON-REFERENCE.md`
  everywhere in this file — that filename never existed in the repo
- `wiki/NPU-RUNTIME-PATHS.md` added
- `wiki/SESSION12-HANDOFF.md` written
- Repo confirmed **public** via the GitHub API (`"private": false`)
- Hardcoded `HF_TOKEN`/`QAI_HUB_API_TOKEN` removed from this file entirely
  — moved to the cloud environment's Environment Variables config. Both
  tokens need rotation (they were exposed in this public repo's history).
- `rules/AT_BAT_PROTOCOL.md` fixed (dead file pointers, corrected 2-repair-
  attempt-then-orchestrator-then-gate strike model), `wiki/FAILURE_LOG.md`
  created, `rules/CACHE_PROMPT_RULES.md` dangling ref removed

### Done — session 13 (2026-07-03), full repo audit for dead weight / false claims
Dispatched an Explore agent to re-audit the whole repo (not just the docs
already touched) for dead references, redundant content, and claims stated
as fact that are actually false. Findings, ranked, and fixes applied:
- **This file had a real self-contradiction**: resume prompt / sub-agent
  template / Repo Policy all said the working branch was
  `claude/project-scope-review-lf615p` (PR #4), while the SOTU and the
  actual checked-out branch were `horizons-closeout-hf-review-ycjkm3`
  (PR #8). Root cause: there are genuinely **two active tracks with two
  different branches** (compile vs app) and this file only ever
  documented one of them as "the" branch. Fixed — resume prompt now
  states both tracks explicitly.
- **`ort_engine` "not yet built" was false** — `daemon/src/` has a real
  735-line implementation (engine.cpp, http_server.cpp, tokenizer.cpp,
  sampler.h, main.cpp) and CI already cross-compiles and packages it.
  `models/manifest.yaml` had the correct `status: scaffolded` the whole
  time; this file was 4+ days out of date. Fixed throughout this file.
- `watchdog/` — was already deleted 2026-06-30 (commit `1ab4e7a`); this
  file still listed it as a pending TODO. Removed.
- `models/manifest.yaml`'s comment pointing at the nonexistent
  `wiki/GPT-OSS-Reference.md` — fixed to `wiki/GPT-DAEMON-REFERENCE.md`.
- `horizons/src/main/java/com/horizons/core/README.md` was entirely
  stale (referenced a nonexistent `GREENFIELD_PLAN.md`, a nonexistent
  `nexa/` package, and listed `voice/`/`screen/`/`log/`/`shell/` as
  "coming in follow-up commits" when all four already exist with more
  content than described) — deleted.
- `agents/neuralmash-builder.system.md`, `agents/sub-agent.system.md`,
  top-level `sub-agent.agent.yaml` — rewritten to describe the real
  stack (Qwen3.5-9B → qnn_context_binary → ort_engine, both real
  branches) instead of dead Nexa/OmniNeural/v79 content and pointers at
  the reference-only repo as source-of-truth.
- `agents/.snapshots/*.yaml` — left as historical captures (not
  corrected in place, that would be dishonest); added
  `agents/.snapshots/README.md` disclaiming them as dead-stack,
  audit-trail-only.
- `agents/build-runner.yaml` — session 8's SOTU claimed this was
  "rewritten as `novus-compile-runner`... scoped to HF Jobs + QAI Hub
  pipeline." **That claim was itself false** — the file is still named
  `horizons-build-runner`, scoped to Android CI, and was never touched.
  That scope is legitimate on its own merits (Android CI is a real,
  separate concern from the compile pipeline) so it wasn't renamed, but
  two real bugs in it were fixed: `environment_hint` said JDK 21
  (CLAUDE.md says 17), and `Working branch: main` ignored the two-track
  branch setup.
- `rules/README.md` / `rules/GIT_HYGIENE.md` — dead pickup-file names
  (`SOTU.md`/`PROMPT_PREFIX.md`/`EXECUTION_BOARD.md`/
  `CLAUDE_AT_HORIZONS.md`) replaced with real files, matching the
  pattern already fixed in `AT_BAT_PROTOCOL.md`.
- `rules/AAR_DECOMPILE.md` — archived-header added (Nexa-specific,
  not applicable to current stack, kept only for the reusable
  javap-decompile technique).
- Full findings (including lower-confidence items intentionally left
  untouched pending operator input) are in the session 13 handoff.

### Pending — in order
1. **Job 8** — trigger command below. Blocked in some remote sessions by
   `huggingface.co` egress policy (per-session-container, not fixed —
   verify with `curl -sS "$HTTPS_PROXY/__agentproxy/status"` before
   assuming either way)
2. **`ort_engine` on-device verification** — the daemon builds and is
   packaged by CI; what's still open is verifying it actually loads and
   serves a real compiled model on a physical Hexagon HTP v79 device,
   which needs Job 8's output first.
3. **NpuManager lock** — wire into `CliffordService.kt`
4. **GameManager** — wire into `HorizonsApplication.kt`
5. **Manifest** — `uses-feature` + `HIGH_PERFORMANCE`
6. **RouterPane "routing rules"** — use-cloud-when-NPU-unavailable etc.
   Deliberately not built yet; needs a real rule engine, not UI toggles
   that don't affect behavior
7. **SettingsPane "Themes"** — deliberately not built; needs a switchable
   palette system, `HorizonsColors` is currently a flat hardcoded object
8. **Three orphaned-but-real classes**: `core/log/InteractionLogger.kt`,
   `core/shell/SecureResourceRelay.kt`, `core/screen/ScreenshotCapture.kt`
   — fully implemented, never wired to any caller. Likely mid-flight
   features (screenshot capture for Vision-Agent tile, secure shell
   token relay, structured interaction logging), not accidental cruft —
   confirm with operator before deleting.
9. **CI publish-target TODO below** — could not find evidence it's still
   real (checked `build-apk.yml`'s full history, no foreign repo ever
   hardcoded, `softprops/action-gh-release@v2` has no `repository:` field
   so it already defaults to this repo). Verify against a live release
   page before removing the TODO outright.

---

## Job Execution Log

| Job | Error | Fix | Result |
|---|---|---|---|
| 1–4 | Various load/submodule errors | Iterative | Done |
| 5 | `has_previous_state on LinearAttention` | `use_cache=False` | Done |
| 6–7 | `cat(): got 5 and 4` M-RoPE shape | Two-pronged fix `2af893b` | Done |
| **8** | pending | — | **Ready** |

---

## Job 8 Trigger Command

```bash
hf jobs uv run --flavor cpu-xl --timeout 2h \
  --with torch --with transformers --with onnx --with onnxruntime --with onnxscript \
  --with qai-hub --with datasets --with numpy --with huggingface_hub --with accelerate \
  --secrets HF_TOKEN --secrets QAI_HUB_API_TOKEN \
  -e MODEL_ID=Mer0vin8ian/Qwen3.5-9B -e PUBLISH_HF=1 -e OUTPUT_DIR=/tmp \
  https://raw.githubusercontent.com/c10vis-poem/Novus-Agenti/claude/project-scope-review-lf615p/scripts/compile_qwen3_5_9b.py
```

`SKIP_VISION` is NOT set — all three artifacts attempted.

---

## Repo File Map

```
c10vis-poem/Novus-Agenti  (public — confirmed via GitHub API, not private)

CLAUDE.md                     ← THIS FILE
agents/
  build-runner.yaml             novus-compile-runner
  neuralmash-builder.system.md  Novus-Agenti stack
  sub-agent.system.md           Novus-Agenti stack
daemon/                          ort_engine C++ daemon (scaffolded, CI-built)
  src/engine.cpp, http_server.cpp, tokenizer.cpp, sampler.h, main.cpp
rules/
  AAR_DECOMPILE.md              QNN artifact inspection
  AT_BAT_PROTOCOL.md
  CACHE_PROMPT_RULES.md
  GIT_HYGIENE.md
skills/
  horizons-wiki/SKILL.md        novus-agenti-wiki
  project-memory/SKILL.md
  termux-mobile-dev/SKILL.md
knowledge/                       byte-faithful master-wiki corpus (see README.md)
  omni-claw-defined/  research-npu/  proofs/  fragmented-qat/
  google-dev-docs/  gemini-query/   ← distilled .md/.jsonl/.txt, DO NOT re-process
models/manifest.yaml
scripts/compile_qwen3_5_9b.py   PRIMARY
wiki/
  GPT-DAEMON-REFERENCE.md         distilled daemon/architecture notes
  NPU-RUNTIME-PATHS.md            runtime formats + SDK distribution model
  FEATURE-SPEC.md                 UI tile spec
  FAILURE_LOG.md                  append-only strike/failure ledger
  SESSION{5,6,8,9,10,11,12,13}-HANDOFF.md
horizons/                        Android app
  fgs/CliffordService.kt         Watchdog daemon
  core/llm/NpuClient.kt
  core/shell/DaemonLauncher.kt
  core/agent/AgentLoop.kt
.github/workflows/build-apk.yml
release/debug.keystore           committed by design
```
`watchdog/` was already deleted (2026-06-30) — don't look for it.
`.github/workflows/build-apk.yml`'s publish-target TODO could not be
confirmed still real as of session 13 (no foreign repo found hardcoded
anywhere in its history) — verify against a live release page before
assuming it needs work.

---

## Hard Rules

- Never push `main` without explicit user permission
- Never `--no-verify`, `push --force`, `reset --hard` without confirming
- No CPU fallback in the Qwen3.5-9B path (NPU or nothing for that model)
- No in-process tensor runtime — every model runs via its own uploadable daemon binary
- `M0DU14R-SYSx-inc/NeuroOmni.Vag-Agenti` is REFERENCE-ONLY
- Do NOT set `SKIP_VISION=1` in the default trigger command
- `--max_dynamic_tensor_size_mib` stays at **64** until empirically verified

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
| Nexa SDK, OmniNeural, Moonshine | dead |
| Cloud failover in app LLM | HttpFetch agent tool |
