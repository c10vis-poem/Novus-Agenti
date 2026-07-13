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
11. **1h TTL for `ant`-CLI sub-agent deployments.** `sub-agent.agent.yaml`
    and `agents/build-runner.yaml` already set `metadata.cache_ttl_default:
    1h` — that's the real, confirmed knob for agents deployed via
    `ant beta:agents create`. There is no `ant` CLI / `ant/config` slash
    command available inside a Claude Code session itself (checked — no
    such skill exists here); for Agent-tool sub-agents spawned from a
    Claude Code session, set the TTL by rule #5 above instead of assuming
    an `ant` command will run.
