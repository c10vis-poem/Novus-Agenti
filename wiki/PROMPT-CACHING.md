# How Claude Code Uses Prompt Caching

> Condensed from Anthropic's official docs (`code.claude.com/docs/en/prompt-caching`).
> Covers Claude Code's own session-level caching behavior. For the
> lower-level Anthropic API `cache_control` mechanics (breakpoints, TTL
> pricing, 20-block lookback), see `rules/CACHE_PROMPT_RULES.md` — that
> file is the hard-rules contract for this project; this file is reference.

## How the cache is organized

Every message is a new API request. Claude Code re-sends the full context
each time — system prompt, project context, every prior message/tool
result, plus the new message — and the API caches by matching the **prefix**
of the request against what it already processed. The match is exact: a
change anywhere in the prefix recomputes everything after it. No per-file
or per-segment caching.

Requests are ordered so the least-volatile content comes first:

| Layer | Content | Changes when |
|---|---|---|
| System prompt | Core instructions, tool definitions, output style | Loaded tools change, or Claude Code upgrades |
| Project context | CLAUDE.md, auto memory, unscoped rules | Session start, or after `/clear`/`/compact` |
| Conversation | Messages, responses, tool results | Every turn |

Two things aren't in the prompt text but are still part of the cache key:
**model** (switching recomputes everything) and **effort level** (switching
mid-session does too — Claude Code asks for confirmation first).

### Where the cache lives

Server-side, in whichever infra serves the model: Anthropic's infra for API
key/Claude subscription/Claude Platform on AWS; the cloud provider's infra
for Bedrock/GCP Agent Platform; Anthropic's infra for Microsoft Foundry;
wherever requests route for a custom gateway.

## What invalidates the cache

- **Switching models** (`/model`) — new cache, zero hits next turn.
  `opusplan` (Opus in plan mode, Sonnet in execution) is a model switch on
  every toggle. Automatic model fallback (a safety-classifier re-run on
  Opus) is also a model switch.
- **Changing effort level** (`/effort`) — same effect; Claude Code confirms
  first (skipped if the change resolves to the same level already active).
- **Turning on fast mode** — adds a cache-key header; costs once per
  conversation (v2.1.86+ keeps the header across later toggles).
- **MCP server connect/disconnect** — only if the server's tools are
  loaded into the prefix (not deferred via tool search). Deferred tools
  (the default on supported models) just append, no invalidation.
  Non-deferred is the case on Haiku models, GCP Agent Platform, custom
  `ANTHROPIC_BASE_URL` gateways, or a server/tool marked `alwaysLoad`.
  The `/advisor` toggle is an exception even when non-deferred — its
  definition sits after the breakpoint.
- **Enabling/disabling a plugin that provides MCP servers** — same rule as
  above. Skills/commands/agents/hooks/LSP/monitors/themes never invalidate.
- **Denying an entire tool** (bare tool name / `Bash(*)` / a tool-name
  glob like `"*"`) — removes it from the system-prompt layer. A glob
  matching only MCP tools (`"mcp__*"`) has the same effect unless those
  tools are deferred. Scoped deny rules (`Bash(rm *)`) and all allow/ask
  rules don't touch the prefix.
- **`/compact`** — replaces history with a summary; conversation layer
  invalidates by design. System prompt + project context still cache-hit
  if CLAUDE.md/memory are unchanged. (The summarization call itself reads
  the existing cache since it shares the prefix — most of compaction's
  time is generating the summary, not a cache miss.)
- **Upgrading Claude Code** — new system prompt/tools; first request after
  upgrade rebuilds from scratch. Resuming a session after an upgrade is
  the expensive case: the whole history reprocesses with zero hits.

## What's safe (keeps the cache)

- **Editing repo files (source code, not CLAUDE.md) — explicitly safe.**
  File contents enter context only when Claude reads them, and reads
  append to conversation. Editing a file Claude already read does NOT
  retroactively change that earlier read — Claude Code appends a
  `<system-reminder>` noting the change and re-reads if needed.
- **Tool *usage* (the model calling a tool mid-conversation) — also safe.**
  A `tool_use`/`tool_result` pair just appends to the conversation layer
  like any other turn. Don't confuse this with tool *definitions*: those
  live in the system-prompt layer, and ADDING/REMOVING a definition is
  what invalidates (see above). Calling an existing tool never does.
- **Editing CLAUDE.md mid-session** — doesn't invalidate, but **also
  doesn't apply** until the next `/clear`, `/compact`, or restart (it's
  the project-context layer, read once at session start). Nested
  subdirectory CLAUDE.md / `paths:`-scoped rules load later and DO take
  effect once first read.
- **Changing output style** — same "doesn't apply until reload" behavior.
- **Changing permission mode** — safe (except `opusplan`, a model switch).
- **Invoking skills/commands** — injected as user messages, nothing
  earlier changes.
- **`/recap`** — appends as command output, doesn't replace history.
- **`/rewind`** — truncates to an earlier, already-cached prefix; every
  turn since kept that entry warm even if the original turn predates the TTL.
  Restoring file checkpoints alongside has no separate cache effect.
- **Spawning a sub-agent** — doesn't touch the parent's cache (see below).

## Cache lifetime (TTL)

- **Claude subscription: 1-hour TTL, automatic.** Claude Code requests it
  for you — no env var needed, no extra cost (plan-included usage, not
  per-token). If you go over your plan's usage limit and start drawing on
  usage credits (billed per-token), Claude Code automatically drops to
  the 5-minute TTL.
- **API key / Bedrock / GCP Agent Platform / Foundry / Claude Platform on
  AWS: 5-minute TTL by default** (per-token pricing). Set
  **`ENABLE_PROMPT_CACHING_1H=1`** to opt into the 1-hour TTL.
- **`FORCE_PROMPT_CACHING_5M=1`** overrides back to 5 minutes regardless
  of auth — for debugging or overriding a 1h setting from managed config.
- Each cache-hitting request resets the TTL timer — stays warm as long as
  you keep working; a long gap forces a full, slower recompute.
- Bedrock: caching support, minimum cacheable prefix length, and 1h
  availability vary by model — check Bedrock's docs if cache tokens stay
  at zero.

## Subagents and the cache — the important gotcha

**A sub-agent always uses the 5-minute TTL, even when the parent session
is on a subscription's automatic 1-hour TTL.** It starts its own
conversation with its own system prompt/tools, builds its own cache from
zero hits, and warms up across its own turns — but never gets the 1h TTL.
The parent's cache is unaffected either way (the sub-agent's call+result
just appends to the parent's conversation).

**A *fork* is different**: it inherits the parent's system prompt, tools,
and conversation history exactly, so its first request reads the parent's
already-warm cache. If a task needs the parent's warm cache, fork; a fresh
sub-agent always starts cold at the cheaper/shorter TTL regardless of what
you set.

## Cache scope

The cache is effectively scoped to one machine + directory: the system
prompt embeds working directory, platform, shell, OS version, and
auto-memory paths, so different directories (including different
worktrees of the same repo) build different prefixes and don't share
cache. Sequential sessions in the same directory only share a prefix if
the git status snapshot (branch + recent commits, also embedded) matches
at startup. Parallel sessions in the same directory do match.

## Check cache performance

Every response reports two token counts:

| Field | Meaning |
|---|---|
| `cache_creation_input_tokens` | Tokens written to cache this turn, billed at the cache-write rate |
| `cache_read_input_tokens` | Tokens served from cache this turn, billed at ~10% of standard input |

High read-to-creation ratio = caching is working. Creation staying high
turn after turn means something in the prefix keeps changing — check the
invalidation list above.

## Disable prompt caching (debugging only)

Env vars, set to `1`: `DISABLE_PROMPT_CACHING` (all models),
`DISABLE_PROMPT_CACHING_HAIKU`, `_SONNET`, `_OPUS`, `_FABLE` (per-model).
Leave caching enabled for normal use.

## Practical takeaway for this project

Pick model + effort level at the start of a session; save `/compact` for
natural breaks between tasks. On this operator's Claude subscription, the
1h TTL is already automatic for the main session — no `ant` command or env
var needed for that. What actually needs attention: **ordinary sub-agents
(the `Agent` tool) never get the 1h TTL no matter what** — they're always
5-minute, cold-started. If a dispatched task genuinely needs the parent's
warm cache, that requires a fork, not a fresh sub-agent spawn.
