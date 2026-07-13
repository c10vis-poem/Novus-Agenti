# Cache prompting — hard rules

This file is the contract — self-contained, no separate directions doc.
(An earlier version pointed at `wiki/CACHE_PROMPTING.md`; that file was
never actually created in this repo.)

1. **Max 4 `cache_control` markers per request.** Layout:
   tools → system block → history-summary → reserved mid-conversation.
2. **`cache_control` lands on the last block of the prefix to cache.**
   Everything before is the key.
3. **Pre-warm before sub-agent fan-out.** One `max_tokens: 1` call so the
   cache is written before parallel reads begin. Anthropic's cache only
   becomes available after the first response begins streaming — without
   pre-warm, parallel sub-agents all miss (the concurrency trap: agents
   fired at the same millisecond all see no cache and all pay a full
   write). Caches are workspace-scoped, not per-agent — any agent sharing
   an identical prefix reads the same warm cache.
4. **Never edit cached prefix mid-session.** Wiki + prompt prefix edits
   are batched between sessions. Mid-session edits invalidate at 1.25x
   (5m TTL) or 2x (1h TTL).
5. **TTL selection:** 5m for single sessions ≤5 min between turns. 1h for
   sub-agent fan-out or multi-turn over an hour. Below ~3 reads, 1h
   doesn't pay back.
6. **Verify hits.** Read `lastUsage.cacheReadTokens > 0` after each call.
   Surface the value in the Router/Diagnostics tile so the operator can
   confirm without parsing JSON.
7. **Cache key minimum is 1024 tokens.** Below that, no cache activity
   even if `cache_control` is set. Falls short by a little (e.g. 900
   tokens)? Padding to cross the threshold is worth it for anything reused.
8. **20-block lookback.** A miss walks back at most 20 blocks looking for
   a prior write; past that the whole prefix is treated as new and
   rewritten at full cost. A growing conversation that outruns this needs
   a second breakpoint before the first falls out of the window.
9. **Mixing TTLs in one prompt:** longer-TTL blocks must sit before
   shorter-TTL blocks (1h segments at the top, 5m segments below).
10. **The cache is not portable.** In-memory only, tied to the specific
    workspace and model that wrote it — never exported, saved, or migrated,
    and gone the moment a session goes quiet past its TTL. Carry over the
    *source text* between sessions/days, not the cache itself; re-send it
    to re-warm.
11. **1h TTL — Anthropic's docs claim it's automatic on a Claude
    subscription; don't trust that claim blindly.** Real, dated GitHub
    issues confirm it doesn't reliably hold: `anthropics/claude-code#46829`
    reports a silent regression to 5-minute starting March 6–8, 2026
    (closed "not planned," never confirmed as bug vs new default), and
    `anthropics/claude-code#45381` (filed Apr 8, 2026) confirms
    `DISABLE_TELEMETRY=1` / `CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC=1`
    silently downgrades 1h to 5m even when a session would otherwise
    qualify. **Verify, don't assume**: check
    `usage.cache_creation.ephemeral_1h_input_tokens` vs
    `.ephemeral_5m_input_tokens` in the API response for which TTL is
    actually active. On API-key/Bedrock/GCP/Foundry auth (5m by default
    regardless), `ENABLE_PROMPT_CACHING_1H=1` opts into 1h;
    `FORCE_PROMPT_CACHING_5M=1` forces 5m. See `wiki/PROMPT-CACHING.md`.
12. **Scope note — two different "sub-agent" systems in this repo.**
    Rules #3 and #5 (pre-warm, 1h TTL for fan-out) apply when **Omni
    Claw's own code calls the Anthropic API directly** (e.g. a future
    orchestrator using `cache_control` headers itself) — there, TTL is
    fully our choice per request, 1h included.
    A **Claude Code session's own `Agent`-tool sub-agents (this repo's
    dev sessions, not the app) are different and NOT covered by rules
    #3/#5**: a fresh `Agent`-tool sub-agent always starts its own cold
    cache at the 5-minute TTL, even when the parent session has the
    automatic 1h TTL — no setting changes this. Only a *fork* (inherits
    the parent's system prompt/tools/history exactly) reads the parent's
    already-warm cache. Don't assume "set TTL to 1h" fixes a Claude Code
    sub-agent's cache misses; it can't.
    `sub-agent.agent.yaml` / `agents/build-runner.yaml`'s
    `metadata.cache_ttl_default: 1h` is a third, separate system — the
    `ant beta:agents create` deployment path's own knob, unrelated to
    either of the above.
