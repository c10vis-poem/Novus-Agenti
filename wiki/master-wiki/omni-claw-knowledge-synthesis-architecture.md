# Omni Claw Local Knowledge Synthesis — Integration Architecture

**Status: architecture and framework only — no implementation code.** This document proposes how three already-surveyed systems (OpenWiki, OB1, reasoning-bank) combine into the "Local Knowledge Synthesis" backend that Omni Claw's C++ engine queries mid-inference, before compiling a meta-prompt. It follows the 11-section structure of `aesop-full-reference.md` as a reusable blueprint template — the same template this doc's own maintainer intends to reuse again for the eventual UI APK build blueprint. Every claim below traces back to material already condensed in this Drive folder; see the "Source & Provenance Log" (§11) for the map.

## 1. Project Overview

Omni Claw's blueprint describes a step it calls **Local Knowledge Synthesis**: before the C++ engine compiles a meta-prompt from a user's spoken intent, it "pulls designated skill blocks, API schemas, and historical code contexts directly out of your local Obsidian or Markor directories" (`omni-claw-blueprint.md` §4, the Meta-Prompt Verification Loop). As documented, that's a placeholder for real infrastructure — "read some Obsidian files" is not a queryable, structured, or retrievable memory system.

This architecture replaces that placeholder with three components, each already surveyed in this project, combined into one backend:

- **OpenWiki** supplies the *maintenance discipline* — the `SKILL.md` convention (per-project, doc-generation agent pattern) that keeps the knowledge base itself readable, versioned, and self-describing rather than an unstructured pile of notes.
- **OB1 (Open Brain)** supplies the *actual memory store* — a `thoughts` table (Supabase/pgvector), HNSW cosine similarity retrieval, SHA-256 dedup, and a remote-only MCP server as the query interface. This is the durable, queryable backend the Obsidian-file-reading placeholder was gesturing at.
- **reasoning-bank** supplies the *retrieval strategy* — not "search everything," but distilled natural-language memory items (Title/Description/Content) extracted from both successful AND failed past trajectories, retrieved by embedding similarity, capped small (n=1-10 items) so the executor's context stays lean.

The three map cleanly onto three separate concerns that are easy to conflate: **how the knowledge base stays maintainable** (OpenWiki), **where the knowledge actually lives and how it's queried** (OB1), and **what gets pulled into context for a given task and why** (reasoning-bank). Keeping them conceptually separate — rather than one monolithic "memory system" — is itself a design decision this document is making, because it matches how the Dual-Agent Talker-Reasoner pattern (see §6) already separates the Query/Talker agent's job (retrieval) from the Executor's job (using what it was given).

## 2. Architecture Map

Five logical layers, none of which currently exist as code — this is the target shape:

```
┌─────────────────────────────────────────────────────────────┐
│  Layer 5: Meta-Prompt Compiler (Omni Claw C++ engine)        │
│  — receives a distilled context payload, compiles the        │
│    final meta-prompt, streams it to the AI Chat Box for       │
│    human review before dispatch                               │
└───────────────────────────▲─────────────────────────────────┘
                             │ (context payload, ~1-10 memory items)
┌───────────────────────────┴─────────────────────────────────┐
│  Layer 4: Retrieval Strategy (reasoning-bank pattern)         │
│  — embeds the current query, cosine-matches against stored    │
│    memory items, caps retrieval count, distinguishes           │
│    success-pattern items from failure-pattern items             │
└───────────────────────────▲─────────────────────────────────┘
                             │ (query embedding → ranked memory items)
┌───────────────────────────┴─────────────────────────────────┐
│  Layer 3: Memory Store + Query Interface (OB1)                │
│  — thoughts table (Supabase/pgvector), HNSW index,             │
│    SHA-256 dedup on ingest, remote-only MCP server as the      │
│    ONLY access path (never local/stdio — hard rule)            │
└───────────────────────────▲─────────────────────────────────┘
                             │ (structured writes: skills / recipes / schemas / thoughts)
┌───────────────────────────┴─────────────────────────────────┐
│  Layer 2: Maintenance & Structure (OpenWiki convention)        │
│  — SKILL.md per capability area, doc-generation agent          │
│    keeps the wiki self-describing; taxonomy: skills/ (behavior)│
│    recipes/ + schemas/ + thoughts (memory) / extensions/       │
│    + primitives/ (curated reference)                            │
└───────────────────────────▲─────────────────────────────────┘
                             │ (raw sources: PDFs, transcripts, code, docs)
┌───────────────────────────┴─────────────────────────────────┐
│  Layer 1: Ingestion Pipeline (this project's standing rules)  │
│  — clean Markdown + JSONL chunks + deduped URL list per doc;   │
│    Gemini-sourced content gets code-repaired and flagged        │
└─────────────────────────────────────────────────────────────┘
```

Notably, this stack is **model-agnostic at Layers 1-4** — the ingestion pipeline, the OB1 store, and the reasoning-bank retrieval pattern don't care whether Layer 5 is Omni Claw's on-device Gemma-4 12B, AESOP's cloud Claude API, or the home-hub dual-agent executor described in the Final Memory Layer/P2P doc. The same backend can serve all three deployment profiles (§5).

## 3. Data & Memory Flow (Ingestion)

How raw material becomes queryable memory, end to end:

1. **Raw source arrives** (PDF, doc, transcript, code repo) — per this project's standing pipeline rules, it is never modified in place.
2. **Layer 1 ingestion**: clean Markdown reconstruction + JSONL chunking (one object per logical section: `id`, `section`, `heading`, `content`, `source_urls`, `tags`) + deduped URL extraction. Gemini-sourced content gets code repaired and flagged (`<!-- fixed -->`, `<!-- UNVERIFIED -->`).
3. **Layer 2 categorization**: the doc-generation agent (OpenWiki-style) decides which OB1 taxonomy bucket the content belongs in — `skills/` (a reusable behavior pack, gets a `SKILL.md`), `recipes/`/`schemas/` (structured reference data), or raw `thoughts` (freeform memory items destined for the vector store).
4. **Layer 3 storage**: content lands in OB1's `thoughts` table. Each row gets embedded (for HNSW cosine retrieval) and SHA-256 fingerprinted (for dedup — re-ingesting the same source twice doesn't duplicate the memory). This is a **write-time** step, not something the executor does per-query.
5. **Layer 4 distillation (ongoing, not one-time)**: as Omni Claw actually runs tasks, the reasoning-bank pattern extracts new memory items post-hoc from *both* successful and failed task trajectories — not just "what worked" but "what didn't and why." These get written back into OB1 as new `thoughts`, closing the loop: the memory store grows from the AI's own operating history, not just from human-fed documents.

This is why Layer 4 is drawn as a loop in §2's diagram in spirit (even though the ASCII shows a simple stack): retrieval and distillation are two directions through the same store, not a one-way pipe.

## 4. Runtime Query Flow (The Live Path)

What happens when the user actually triggers a meta-prompt compilation (mapping directly onto Omni Claw's existing Meta-Prompt Verification Loop, `omni-claw-blueprint.md` §4):

1. User taps the internal mic in the AI Chat Floating Box, speaks a rough intent.
2. Silero VAD isolates speech; raw text/audio routes over local loopback to the C++ engine (unchanged from the existing blueprint).
3. **New step**: instead of "read some Obsidian files," the engine embeds the (transcribed) intent and sends a retrieval request to OB1's MCP server — **remote MCP only**, per OB1's hard rule; there is no local/stdio path even for an on-device engine, so this hop is a real network round-trip, not a local call.
4. OB1 runs HNSW cosine similarity search against the `thoughts` table, returns a ranked list.
5. The reasoning-bank retrieval strategy caps this to n=1-10 items and returns them as the distilled payload — small enough that the executor's context doesn't bloat, matching the Dual-Agent Talker-Reasoner pattern's core complaint (uncapped retrieval defeats the purpose).
6. The C++ engine synthesizes the meta-prompt using this payload plus the user's raw intent, streams it into the AI Chat Box as **editable text** — the human-in-the-loop step from the existing blueprint is unchanged; retrieval augments what gets compiled, it doesn't bypass human review.
7. On dispatch (local tool call, cloud API, or CLI sub-agent spin-up), the task executes; its outcome (success or failure) becomes a candidate for the next Layer 4 distillation pass.

## 5. Deployment Profiles

Three profiles, corresponding to the three device topologies already documented across this project's sources — this section plays the same role AESOP's device profiles (`razr_ultra.env`, `tab_s9_fe.env`, etc.) play, but for knowledge-backend deployment scale rather than voice/TTS backend selection:

| Profile | Matches | Backend Placement | Notes |
|---|---|---|---|
| **Phone-Only** | AESOP's cloud-delegated mode, or Omni Claw running standalone without a home network | OB1 hosted remotely (cloud Supabase project) since remote-MCP-only is a hard rule anyway; retrieval happens over the phone's normal internet connection | Simplest profile; works away from home; latency depends entirely on cloud round-trip |
| **Home-Hub** | The Final Memory Layer/P2P doc's 3-device mesh (Razr Ultra + Jetson Nano + Rubik Pi 3) | OB1's Postgres/Supabase instance runs on the Jetson Nano's attached SSD; phone reaches it via Tailscale P2P when home | Matches the doc's "governed continuity memory" layer exactly — OB1 IS that layer, formalized |
| **Full Mesh** | Home-Hub profile plus the Rubik Pi 3 Red Agent Auditor | Same OB1 placement as Home-Hub, but retrieval payloads for code-generation tasks also get audited by the Rubik Pi's Gemma-4 E2B gatekeeper before execution | Adds the verification step the Final Memory Layer doc specifies for anything the executor generates and is about to run |

None of these profiles requires a different memory *architecture* — only where OB1's database physically runs and how the phone reaches it changes. This is a deliberate design property: the knowledge synthesis backend should not need to be rebuilt when the user moves from "just my phone" to "phone + home mesh."

## 6. Component Reference

**OpenWiki** (doc-generation CLI agent, LangGraph/DeepAgents-based): no memory/RAG/MCP of its own — it's the authoring/maintenance tool, not the runtime store. Its contribution here is purely the `SKILL.md` convention: a per-capability file that's both human-readable documentation and a machine-consumable entry point (this project's own `llm-wiki-termux-setup` skill package is a working example of the pattern). In this architecture, OpenWiki's role is confined to Layer 2 — deciding how new content gets organized — not to runtime retrieval.

**OB1 (Open Brain)**: the mature memory backend. Core schema is a `thoughts` table; retrieval is HNSW cosine similarity; ingestion dedup is SHA-256 fingerprint-based. Its MCP server is **remote-only by hard rule** — no `claude_desktop_config.json`, no `StdioServerTransport`, no local Node.js server; it deploys as a Supabase Edge Function and connects via a custom-connector URL. This is a real architectural constraint on Layer 3-4 interaction in this document: the C++ engine's retrieval call in §4 step 3 is a genuine network request even when running fully on-device, because OB1 will not run as an in-process/local server. Taxonomy: `skills/` (behavior packs) / `recipes/` + `schemas/` + `thoughts` (memory) / `extensions/` + `primitives/` (curated project reference) — this maps directly onto the "skill / memory / project reference" content buckets this whole ingestion project has been sorting incoming material into.

**reasoning-bank**: not an app — a research pattern (Google, ICLR 2026 submission) for distilled natural-language memory items (Title/Description/Content) extracted from agent trajectories, retrieved by embedding cosine similarity, capped small. The critical detail this architecture leans on: memory items come from **both success and failure** trajectories. A failed task isn't discarded — it becomes a retrievable "here's what didn't work and why" item, which is exactly the kind of thing a Rubik Pi Red Agent Auditor rejection (Final Memory Layer/P2P doc §5) should feed back into, closing the loop between the Auditor's verdicts and the memory store's growth.

**The Dual-Agent Talker-Reasoner pattern** (from the Final Memory Layer/P2P doc, not a separate repo but a named architecture pattern): the Query/Talker Agent's job — search the persistent store, isolate a slice, package a micro-payload — is precisely Layers 3-4 of this document. The Executor Agent's job — receive the micro-payload, do the task, wipe its own history — is Layer 5. This document is, in effect, a concrete instantiation of that pattern specifically for Omni Claw's meta-prompt compilation step.

## 7. Retrieval & Model Strategy Guide

This section plays AESOP's "Edge Model Guide" role, but for retrieval/caching strategy rather than STT/TTS model choice:

- **Embedding + retrieval**: HNSW cosine similarity (OB1's native mechanism) is the baseline; reasoning-bank's contribution is capping *how many* results get used (n=1-10), not changing the similarity mechanism itself.
- **Prompt caching interaction** (per `notebooklm-prompt-caching-planning-qa.md` and `anthropic-prompt-caching-docs.md`, both already in this folder): the retrieved memory-item payload should be treated as the **cache-eligible static prefix** whenever the downstream model is a Claude-API-class model (i.e. the AESOP cloud-delegation path, or a CLI sub-agent calling Claude). Concretely — if the same distilled context gets reused across a multi-turn session (which it will, since a user's working session tends to stay on-topic), placing it early in the prompt with a `cache_control` breakpoint captures the 90%-discount cache-read rate documented in that source. This is a direct, mechanical link between two previously-separate threads in this project: the memory-retrieval architecture (this document) and the prompt-caching research (already condensed, flagged HIGH PRIORITY by the user).
- **Sub-agent fan-out**: if a task spins up multiple CLI sub-agents (matching the Final Memory Layer/P2P doc's Cache Pre-warming pattern), they should all read the *same* retrieved payload rather than each independently querying OB1 — both to avoid the "concurrency trap" documented in the prompt-caching notes (concurrent cold cache writes) and to guarantee sub-agents don't silently diverge on what context they were given.
- **On-device vs. cloud retrieval cost**: retrieval itself (the OB1 query) is cheap and network-bound, not compute-bound — it doesn't touch the on-device NPU. The NPU/compute-cost concerns documented in `scaling-llm-test-time-compute-npu.md` apply to Layer 5 (the actual generation), not to Layers 3-4 of this architecture.

## 8. Interaction Patterns

The kinds of triggers this backend needs to serve, mapped from what's already documented elsewhere in this project rather than invented here:

- **Meta-prompt compilation** (primary case, detailed in §4) — triggered by the internal mic in the AI Chat Box.
- **Skill invocation** — when a compiled meta-prompt resolves to "this needs a known capability," the system should be able to retrieve the matching `SKILL.md` (Layer 2/3 boundary) rather than re-deriving how to do something the wiki already documents (e.g. the `llm-wiki-termux-setup` skill package itself).
- **Post-task distillation** — not user-triggered at all; happens automatically after any dispatched task completes or fails, per §3 step 5 and §6's reasoning-bank description.
- **Cross-device continuity queries** — the Home-Hub and Full Mesh profiles imply a case where a task started on the phone (away from home) needs its prior context once the user is back on the mesh; this is a straightforward OB1 retrieval (the store doesn't care which device asks), but the *routing* of "should I even try to reach OB1 remotely right now, or wait until I'm on Tailscale" is an open question (see §10).

## 9. Extending the System / Adding Skills

Mirrors AESOP's "Editing Artifacts & Skills" section, restated for this architecture:

- **Adding a new skill**: write a `SKILL.md` (OpenWiki convention), let the Layer 2 doc-generation agent ingest it into OB1's `skills/` bucket. No code changes to Layers 3-5 are needed — this is the entire point of keeping maintenance (Layer 2) separate from storage (Layer 3).
- **Adding a new memory source**: run it through the standing Layer 1 ingestion pipeline (clean md + JSONL + URLs) exactly as this project has been doing for every PDF/doc processed so far — this document itself is proof the pipeline generalizes to architecture docs, not just reference material.
- **Adjusting retrieval behavior**: the n=1-10 cap and success/failure distinction are reasoning-bank-pattern parameters, not OB1 schema changes — tunable without touching the memory store.
- **Adding a new deployment profile** (e.g. a future desktop/laptop version, flagged as an open question in the Omni Claw blueprint's own §10): per §5, this should only require deciding *where OB1's database runs* and *how the new device reaches it* — not redesigning Layers 3-4.

## 10. Known Gaps & Open Questions

Honest accounting of what this document does NOT resolve, mirroring AESOP's Troubleshooting section but for architectural rather than operational issues:

- **The MCP-between-CLI-and-Kotlin-app question** (raised directly by the user in the NotebookLM prompt-caching conversation, `notebooklm-prompt-caching-planning-qa.md` §11) is still open. This document assumes OB1's remote MCP server is reachable from the C++ engine, but whether the *Kotlin app itself* should also expose an MCP server (client/server roles reversed) for device-control purposes is a separate, unresolved architectural question NotebookLM flagged as needing independent research.
- **The "third repo" the user couldn't name** (mentioned in the Final Memory Layer/P2P doc, described as "in between" OB1 and Open-Wiki) remains unidentified. If it surfaces, it may fill a gap this document doesn't currently cover, or may overlap with reasoning-bank's role.
- **Retrieval routing when off-mesh**: per §8, whether/how the system should retry, queue, or gracefully degrade when the phone can't currently reach OB1 (no internet, or on Phone-Only profile with connectivity issues) isn't specified here.
- **Auditor-to-memory feedback loop mechanics**: §6 asserts that Rubik Pi Auditor rejections should become failure-pattern memory items, but the actual write path (does the Auditor write directly to OB1, or does the rejection route back through the Jetson executor first?) isn't specified — this is exactly the kind of detail that belongs in a future implementation-level doc, deliberately out of scope here.
- **Cost of remote-only MCP for latency-sensitive on-device interactions**: OB1's hard rule against local/stdio MCP is good for the security model but means every retrieval is a network round trip; whether this is acceptable for Omni Claw's presumably-fast meta-prompt compilation loop hasn't been measured against real latency numbers.

## 11. Source & Provenance Log

Every claim in this document traces to material already condensed in this Drive folder:

| Section | Primary source(s) |
|---|---|
| §1 Local Knowledge Synthesis concept | `omni-claw-blueprint.md` §4, §12 |
| §2-4 Architecture/data/query flow | `openwiki.md`, `ob1.md`, `reasoning-bank.md` (component surveys); `omni-claw-blueprint.md` §4 (runtime trigger) |
| §5 Deployment profiles | `aesop-full-reference.md` §5 (device profile pattern); `final-memory-layer-p2p-pipeline.md` §3, §4 (hardware topology) |
| §6 Component details | `openwiki.md`, `ob1.md`, `reasoning-bank.md`; `final-memory-layer-p2p-pipeline.md` §1 (Dual-Agent Talker-Reasoner) |
| §7 Retrieval & caching strategy | `notebooklm-prompt-caching-planning-qa.md`, `anthropic-prompt-caching-docs.md`; `final-memory-layer-p2p-pipeline.md` §6 (sub-agent caching); `scaling-llm-test-time-compute-npu.md` (compute-cost boundary) |
| §8 Interaction patterns | `omni-claw-blueprint.md` §4 (meta-prompt loop); `final-memory-layer-p2p-pipeline.md` §9 (task workflow phases) |
| §9 Extension pattern | `llm-wiki-termux-setup` skill package (worked example); `openwiki.md` |
| §10 Open questions | `notebooklm-prompt-caching-planning-qa.md` §11 (MCP question); `final-memory-layer-p2p-pipeline.md` §11 (unnamed third repo) |
| Format template itself | `aesop-full-reference.md` (11-section structure, reused per user's explicit instruction) |

No new external URLs are introduced by this document — it's a synthesis of already-ingested sources, not a new raw-source ingestion. All source docs' own URL lists remain the record of external references.
