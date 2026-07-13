# NotebookLM Prompt Caching Planning Q&A

Source: "NotebookLm Planning query" (Drive `1HErOV4efI2XPbc05kqvN_IBzKZtwz39PjD6se76NXHo`, Google Doc; a near-duplicate PDF export of the same content also exists at Drive `1s6w_IVeApBqGhWkIRk-m3yKaAoDG4e9-` titled "Copy of ookLm Planning query .pdf" — not separately processed since it is the same conversation). Category: **HIGH PRIORITY — user-specific strategy session**, per the user "right up there with the skills." This is a NotebookLM Q&A conversation where the user works through, in real time, how to apply Anthropic's prompt caching to their own multi-agent CLI + on-device assistant project. Preserved as a full back-and-forth because the follow-up questions and corrections are where the useful detail lives, not just the final answers.

**Source reliability note**: NotebookLM is answering based on Anthropic's prompt caching documentation (the companion doc, see `anthropic-prompt-caching-docs.md`), filtered through the user's own project questions — treat specific mechanics (pricing, TTLs, block rules) as grounded in that documentation, but treat the *strategic advice* (pre-warming patterns, multi-day workflows, wiki-as-cache-prefix) as NotebookLM's synthesis, worth validating against the official docs directly.

## Phase 1: Core Concepts (Training Plan)

**Two caching methods**: Automatic caching (single `cache_control` field at the top level of the request; system auto-applies the breakpoint to the last cacheable block and moves it forward as the conversation grows — best starting point) vs. Explicit caching (place `cache_control` on individual content blocks for fine-grained control — needed when parts of a prompt change at different frequencies, e.g. tools rarely change but context updates daily).

**Prefix hierarchy**: cache prefixes build in strict order — **tools**, then **system**, then **messages**. Static, reusable content belongs at the beginning; the cache breakpoint goes on the *last* block that stays identical across requests.

**The "20-block lookback" rule**: when a request comes in, the system hashes the prompt at the breakpoint and, if there's no direct match, walks backward one block at a time looking for a previous cache write — but stops after 20 blocks. If a growing conversation pushes the breakpoint 20+ blocks past the last write, the system misses it and charges for a fresh (expensive) cache write.

**Pricing multipliers**: cache reads cost only 10% of base input token price (the actual savings); 5-minute-TTL cache writes cost 25% *more* than base input price; 1-hour-TTL cache writes cost 2x base input price.

## Phase 2: Testing Parameters & Audit Framework

**Monitor the `usage` field** on every API response: `cache_creation_input_tokens` (tokens written to cache — upfront cost), `cache_read_input_tokens` (tokens successfully read from cache — proof caching is working), `input_tokens` (tokens after the last breakpoint, billed normally). Test: send an identical prompt twice — first response should show high `cache_creation_input_tokens`; second should show 0 creation tokens and high `cache_read_input_tokens`.

**Cache invalidation triggers** — deliberately test these: changing a tool definition (name/description/parameter) invalidates the *entire* cache (tools, system, messages); adding/removing an image invalidates the message-blocks cache; modifying `tool_choice` also invalidates message blocks.

**Minimum token limits**: the system silently ignores cache requests on prompts that are too short — e.g. Claude 3.5 Sonnet-class models need a minimum of 1,024 tokens to cache (for Opus/Haiku-class models the threshold is higher, 4,096 tokens per this conversation — cross-check against the official pricing table). Test: send a short prompt with a `cache_control` marker; both creation and read tokens will be 0 if it was too short.

**Cache Diagnostics (beta)**: if `cache_read_input_tokens` keeps coming back 0 unexpectedly, this feature lets the API compare consecutive requests and report exactly where the prompt prefix diverged.

## Deep Dive: What Exactly Is a "Block"?

A block is an individual element within the API request's arrays — a specific tool definition in `tools`, a specific system instruction in `system`, or a user message / assistant reply / image / tool result within `messages`. Going past the 20-block window means the last cache write is now 21+ blocks behind the current breakpoint — the system gives up looking, treats it as a cache miss, and reprocesses the entire prompt as a fresh (expensive) write.

**Detecting a miss**: watch `cache_read_input_tokens` (0 when expecting a hit, with high `cache_creation_input_tokens`, means a miss occurred) or use Cache Diagnostics.

**Fixing it**: two options — (1) Manual/Explicit: up to 4 cache breakpoints are allowed per request; add a *second* breakpoint closer to the end of the new messages before the first one falls out of the 20-block window; (2) Automatic (recommended for most cases): one marker at the top of the request, and the system moves the breakpoint to the last eligible block on every new message — no manual block-counting needed.

## Minimum Token Threshold Clarified

The 1,024-token (or 4,096 for some models) minimum applies to the **entire cumulative prompt** up to the breakpoint — system instructions + tool definitions + all previous messages summed together, not each individual new message. Once the cumulative total crosses the threshold, everything gets cached and small subsequent messages just get appended, with the automatic breakpoint continuing to move forward. There's no error/warning if a prompt is too short — check `cache_creation_input_tokens`/`cache_read_input_tokens` (both 0 = not cached). **Pro tip from the source doc**: if a prompt falls just short of the minimum (e.g. 900 tokens), it can be worth padding it (more system instructions/examples) to cross the threshold, since the resulting 90% discount on cache reads outweighs the cost of the padding for frequently-reused prompts.

## Cache Portability: Cannot Export, Save, or Migrate

The cache is **strictly in-memory only** — the system computes Key-Value (KV) representations and cryptographic hashes held in active memory, never stored at rest (no hard drive, no database). It cannot be exported, downloaded, or handed off, and is **strictly isolated per workspace** and **tied to the specific model** that computed it — a cache built cheaply on one model cannot be transferred to a different, smarter model. There is no Anthropic feature (the user asked about an "ANT feature" / Managed Agents) that bypasses this. When starting a new session or day, there's no memory of the previous cache — the correct approach is to carry over the **source text** (the compiled file), not try to carry over the cache itself: re-upload the file, re-enable caching, pay once for a fresh cache write, then get discounted reads for the rest of that session.

## Compaction: The Right Use for an Agent, Instead of Manual Block-Counting

You cannot manually force a growing project to "break every 20 blocks" (the hard cap is 4 explicit breakpoints per request, and automatic caching already handles this dynamically). Instead, the recommended pattern — which the source documentation calls **"Compaction"** — is to have an agent periodically summarize/rewrite older parts of a conversation into a concise document, then place that streamlined static content at the very beginning of the prompt array for the system to cache. This keeps token count down and caching efficient as a project scales.

## Multi-Facet Projects: Modular Caching With the 4 Breakpoints

For a large multifaceted project, use the 4 explicit breakpoints to maintain separate "pillars," e.g.: Module 1 = core project instructions/SOPs, Module 2 = dataset/codebase summary for Facet A, Module 3 = summary for Facet B, etc. Each module gets its own breakpoint. Benefit: if Facet A's code updates, only that block's hash changes and gets invalidated — the other modules (core instructions, Facet B) keep their 90%-discounted cached reads intact.

**1-hour TTL for agentic work**: recommended specifically when an "agentic side-agent" will take longer than 5 minutes to complete a processing loop, so the large project documents stay "warm" in memory between interactions. Always place the heaviest, most static context at the very top of the prompt (prefixes build top-down).

## Sub-Agents and Shared Caching

Sub-agents CAN use prompt caching — it is not restricted to the main orchestrating CLI. Caches are tied to the **workspace**, not to an individual script/agent instance, so any agent in that workspace can read a shared cache as long as their prompt prefixes are 100% identical up to the breakpoint.

**The concurrency trap**: a cache entry only becomes readable *after* the first API response begins. If a main CLI spins up 4 sub-agents simultaneously and they all fire requests at the same millisecond, none of them find an existing cache — all 4 process from scratch and get charged for 4 separate expensive cache writes instead of 1.

**Fix — Cache Pre-warming**:
1. **Pre-warm**: main CLI sends a request containing all shared project data/sub-agent instructions with `max_tokens: 0` — this reads the prompt into the model, writes the cache at the breakpoint, and returns immediately with no generated output text.
2. **Wait**: for that pre-warm request to complete so the cache is established.
3. **Execute**: spin up the sub-agents in parallel — they all hit the now-warm, heavily-discounted cache read simultaneously.

**Bundling directions for multiple sub-agents**: since a cache hit requires a 100% identical prefix up to the breakpoint, the practical pattern is one giant system prompt containing the shared project context *and* baseline instructions for all sub-agents, with the breakpoint at the end of that block — each sub-agent then appends only its unique task as the final user message. This lets all sub-agents share one cached prefix even though their individual tasks differ.

**Actual discount math confirmed in this conversation**: cache reads are 10% (0.1x) of base input price — a 90% discount, not the 5% the user initially guessed. 5-minute cache writes cost 1.25x base price; 1-hour cache writes cost 2x base price. Every cache-read hit resets/refreshes the 5-minute TTL for free — as long as an agent calls at least every ~4.9 minutes, the cache stays alive indefinitely without ever paying another write cost.

## Multi-Day Projects and Mixed TTLs

Caches do **not** survive across multiple days — a 1-hour cache expires 1 hour after last use, and if a project is paused overnight, all cached segments are gone by the next day. On "Day 4," the system has completely forgotten the cache: the first request that day is a fresh (expensive) write, and subsequent requests during that day's session are discounted reads, as long as no more than an hour passes between calls.

**Mixing TTLs in one prompt**: allowed, but with a strict rule — **cache entries with a longer TTL must appear before shorter TTLs** in the prompt structure. So 1-hour segments (e.g. core project guidelines that update slowly) go at the top, with 5-minute segments (e.g. a specific sub-agent's fast-changing task) placed below them.

**The confirmed daily workflow** (this is the user's own proposed pattern, confirmed as correct): (1) End of day — have an agent review the day's thread, condense progress, update code/data pillars, and export everything to concise local text files; (2) Start of next day — inject those updated compiled files at the top of a new prompt and send via Cache Pre-warming (`max_tokens: 0`) to rebuild the cache before sub-agents start; (3) Daily operations — sub-agents fire in parallel against the now-warm cache all day, getting reduced latency and the 90% token discount.

## Terminology Clarification

"Schema" in AI/programming usually means strict data structures (tool definitions, JSON output formats) — not what the user meant. The correct terms for what the user is building: **System Prompt / System Instructions** = the organized set of rules/setup/baseline directions for agents to follow, placed at the top of the request. **Static Prefix / Cache Prefix** = that same block, once a cache breakpoint is placed at its end — it's "static" because it stays identical across requests, letting the system read it from memory instantly.

## Other Claude Console / Platform Features Surfaced

Beyond prompt caching, the platform (per its navigation, not deeply documented in this conversation's sources) includes: **Advanced Tool Use** — Web search tool, Code execution tool, Memory tool, Bash tool, Text editor tool, Computer use tool; **Workspaces** — strict organizational/security boundary, caches isolated per workspace; **Extended Thinking & Control** — Extended thinking, Adaptive thinking, Task budgets (beta), Fast mode (research preview); **Context Management** — Compaction, Context editing, Mid-conversation system messages, Cache diagnostics (beta); **Message Batch Processing** — Batch API discounts stack with prompt-caching multipliers for bulk async work; **Files** — Files API, native PDF support, image/vision capabilities; **Model Context Protocol (MCP)** — Remote MCP servers, MCP connectors, MCP tunnels for hooking Claude to local file systems/databases.

## Direct Relevance: The User's Own MCP + On-Device Assistant Question

The user directly asked in this conversation: *"I'm building a mobile application... an on-device assistant that has API hooks and cloud hooks and agentic device controls — can I create an MCP between my CLI and my on-device Kotlin app?"* — i.e., this is the exact Omni Claw architecture question. NotebookLM's answer: theoretically yes, MCP's standard client-server architecture would let a CLI act as the "client" requesting context/triggering actions while the Kotlin app acts as an "MCP server" exposing local mobile API hooks/device controls — but the loaded source documents (focused on prompt caching) didn't contain the actual MCP technical/architecture docs or Kotlin specifics, so this was flagged as needing independent verification/further research rather than answered definitively.

## The Payoff: Building an LLM Wiki as the Cacheable System Prompt

This is the single most directly relevant thread in the whole conversation — NotebookLM's own recommendation converges exactly on what the user is already building in this session:

- The user's Agent CLI should load a centralized **"Wiki"** — core caching rules, SOPs, shared project context — at the absolute beginning of every prompt, since caching builds prefixes top-down (tools, system, messages) and the thickest/most stable content needs to go first for all sub-agents to share the same cached sequence.
- Every day, the Agent CLI takes this Wiki (plus updated code/data) and sends it via Cache Pre-warming (`max_tokens: 0`) to write it into the 1-hour extended cache once; from then on, the main CLI and all sub-agents read it at a 90% discount.
- The Wiki also functions as a rulebook preventing accidental cache invalidation (reminding the CLI not to casually change tool definitions or add/remove images mid-session) and as a breakpoint-management guide (if using explicit caching, the Wiki's own content occupies the first of 4 breakpoint slots, leaving 3 for modular data pillars).
- NotebookLM explicitly generated ("spun up") a **"Technical Wiki Tailored Report"** for the user's Agent CLI during this session, describing it as ready to "export and plug directly into your system as its core rulebook" — this is a NotebookLM-native artifact, separate from and prior to the actual OpenWiki/OB1/reasoning-bank system being built in the current Claude Code session, but it independently validates the same design: **the wiki IS the cache prefix**.

## Relevance to This Project

This document is a direct, load-bearing justification for the OpenWiki + OB1 + reasoning-bank memory system already being built: it confirms, from Anthropic's own documented caching mechanics (not just architectural intuition), that a well-maintained, static, top-loaded "wiki" document is precisely the artifact that makes prompt caching cost-effective for a multi-agent CLI. Concretely, this means: (1) the eventual Omni Claw / Agent CLI system prompt should load the wiki/skill content as the first, most-static block of any request; (2) a daily "compile and pre-warm" cycle (agent condenses progress → exports to text → next session pre-warms the cache with `max_tokens: 0`) is the correct operational pattern for keeping a long-running project's context both cheap and fast; (3) if/when sub-agents are spun up (matching the "dual-agent" and "3-device neural mesh" architectures documented elsewhere in this project), they should share one pre-warmed cache rather than firing concurrent cold requests; (4) the MCP question the user asked here (CLI-as-client, Kotlin-app-as-MCP-server) is still open and worth resolving directly against MCP's official spec rather than this NotebookLM session's admitted guess.
