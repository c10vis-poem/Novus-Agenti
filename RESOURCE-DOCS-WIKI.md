# Resource Docs · Wiki
> **Consolidated, single-file mirror of the operator's `Claude_master_wiki` corpus.** This is a verbatim assembly of the byte-faithful artifacts under `knowledge/` — nothing re-summarized or re-processed. Prose is inline; each doc's JSONL chunk-layer and source-URL list are in the collapsible block beneath it. The split per-file version (and provenance map) lives in `knowledge/` — see `knowledge/README.md`. Do not edit distilled content here; change it in Drive first, then re-copy.

## Contents
- **Omni Claw — Defined** (`knowledge/omni-claw-defined/`)
  - Omni-claw-knowledge-synthesis-architecture.md
  - omni-claw-blueprint.md
  - repo-fork-asset-list-source.md
  - repo-fork-asset-list.md
  - welcome-to-the-birth-of-novus-agenti-omni-claw.txt
- **Research · NPU** (`knowledge/research-npu/`)
  - scaling-llm-test-time-compute-npu.md
- **Proofs · On-Device Precedents** (`knowledge/proofs/`)
  - overlayd-ai-technical-guide.md
  - socketsweep-readme.md
- **Fragmented QAT** (`knowledge/fragmented-qat/`)
  - fraqat-paper.md
- **Google Dev Docs** (`knowledge/google-dev-docs/`)
  - automated-build-w-github/automated-build-android-github-actions.md
- **Gemini Query Exports** (`knowledge/gemini-query/`)
  - july-3rd-horizons-apk.md
  - qwen-3.5-9b-compilation-and-quantization.md
  - qwen-3.5-9b-q4_0.md

---

# Omni Claw — Defined

<sub>source folder: `knowledge/omni-claw-defined/`</sub>

## `Omni-claw-knowledge-synthesis-architecture.md`
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

<details><summary>chunk layer (JSONL) + source URLs</summary>

```jsonl
{"id": "synth-arch-001", "section": "project-overview", "heading": "1. Project Overview", "content": "This document proposes how three already-surveyed systems, OpenWiki, OB1, and reasoning-bank, combine into the Local Knowledge Synthesis backend that Omni Claw's C++ engine queries mid-inference before compiling a meta-prompt. Omni Claw's blueprint describes this step as pulling skill blocks, API schemas, and historical code contexts out of local Obsidian or Markor directories, a placeholder rather than a real queryable memory system. This architecture replaces that placeholder with three components: OpenWiki supplies the maintenance discipline (the SKILL.md convention keeping the knowledge base readable and self-describing); OB1 (Open Brain) supplies the actual memory store (a thoughts table with HNSW cosine retrieval and a remote-only MCP server); reasoning-bank supplies the retrieval strategy (distilled natural-language memory items from both successful and failed trajectories, retrieved by embedding similarity, capped small). The three map onto three separate concerns: how the knowledge base stays maintainable (OpenWiki), where knowledge lives and how it's queried (OB1), and what gets pulled into context for a task and why (reasoning-bank) -- kept conceptually separate to match how the Dual-Agent Talker-Reasoner pattern already separates retrieval (Query/Talker agent) from execution (Executor agent).", "source_urls": [], "tags": []}
{"id": "synth-arch-002", "section": "architecture-map", "heading": "2. Architecture Map", "content": "Five logical layers, none currently existing as code, describing the target shape. Layer 5, Meta-Prompt Compiler (Omni Claw C++ engine), receives a distilled context payload, compiles the final meta-prompt, streams it to the AI Chat Box for human review before dispatch. Layer 4, Retrieval Strategy (reasoning-bank pattern), embeds the current query, cosine-matches against stored memory items, caps retrieval count, distinguishes success-pattern items from failure-pattern items. Layer 3, Memory Store and Query Interface (OB1), is the thoughts table (Supabase/pgvector), HNSW index, SHA-256 dedup on ingest, with a remote-only MCP server as the only access path, never local/stdio, a hard rule. Layer 2, Maintenance and Structure (OpenWiki convention), is SKILL.md per capability area, with a doc-generation agent keeping the wiki self-describing; taxonomy is skills/ (behavior), recipes/ plus schemas/ plus thoughts (memory), extensions/ plus primitives/ (curated reference). Layer 1, Ingestion Pipeline (this project's standing rules), produces clean Markdown plus JSONL chunks plus deduped URL lists per doc, with Gemini-sourced content code-repaired and flagged. The stack is model-agnostic at Layers 1-4: the ingestion pipeline, OB1 store, and reasoning-bank retrieval pattern don't care whether Layer 5 is Omni Claw's on-device Gemma-4 12B, AESOP's cloud Claude API, or the home-hub dual-agent executor from the Final Memory Layer/P2P doc -- the same backend serves all three deployment profiles.", "source_urls": [], "tags": []}
{"id": "synth-arch-003", "section": "data-memory-flow", "heading": "3. Data and Memory Flow (Ingestion)", "content": "How raw material becomes queryable memory: (1) a raw source arrives (PDF, doc, transcript, code repo) and is never modified in place, per this project's standing pipeline rules; (2) Layer 1 ingestion produces a clean Markdown reconstruction plus JSONL chunking (one object per logical section with id, section, heading, content, source_urls, tags) plus deduped URL extraction, with Gemini-sourced content code-repaired and flagged; (3) Layer 2 categorization has the doc-generation agent decide which OB1 taxonomy bucket the content belongs in, skills/ (a reusable behavior pack gets a SKILL.md), recipes/ or schemas/ (structured reference data), or raw thoughts (freeform memory items destined for the vector store); (4) Layer 3 storage lands content in OB1's thoughts table, embedded for HNSW cosine retrieval and SHA-256 fingerprinted for dedup, a write-time step not something the executor does per-query; (5) Layer 4 distillation is ongoing, not one-time: as Omni Claw runs tasks, the reasoning-bank pattern extracts new memory items post-hoc from both successful and failed task trajectories, writing them back into OB1 as new thoughts, so the memory store grows from the AI's own operating history, not just human-fed documents. Retrieval and distillation are two directions through the same store, not a one-way pipe.", "source_urls": [], "tags": []}
{"id": "synth-arch-004", "section": "runtime-query-flow", "heading": "4. Runtime Query Flow (The Live Path)", "content": "What happens when the user triggers a meta-prompt compilation, mapping onto Omni Claw's existing Meta-Prompt Verification Loop: (1) user taps the internal mic in the AI Chat Floating Box, speaks a rough intent; (2) Silero VAD isolates speech, raw text/audio routes over local loopback to the C++ engine, unchanged from the existing blueprint; (3) new step -- instead of reading Obsidian files, the engine embeds the transcribed intent and sends a retrieval request to OB1's MCP server, remote MCP only per OB1's hard rule, a real network round-trip even for an on-device engine; (4) OB1 runs HNSW cosine similarity search against the thoughts table, returns a ranked list; (5) the reasoning-bank retrieval strategy caps this to n=1-10 items, returned as the distilled payload, small enough the executor's context doesn't bloat, matching the Dual-Agent Talker-Reasoner pattern's core complaint that uncapped retrieval defeats the purpose; (6) the C++ engine synthesizes the meta-prompt using this payload plus the raw intent, streams it into the AI Chat Box as editable text, the human-in-the-loop step unchanged; (7) on dispatch (local tool call, cloud API, or CLI sub-agent spin-up), the task executes, and its outcome (success or failure) becomes a candidate for the next Layer 4 distillation pass.", "source_urls": [], "tags": []}
{"id": "synth-arch-005", "section": "deployment-profiles", "heading": "5. Deployment Profiles", "content": "Three profiles corresponding to the three device topologies documented across this project, playing the role AESOP's device profiles play but for knowledge-backend deployment scale rather than voice/TTS backend selection. Phone-Only matches AESOP's cloud-delegated mode or Omni Claw running standalone without a home network: OB1 hosted remotely (cloud Supabase project) since remote-MCP-only is a hard rule anyway, retrieval happens over the phone's normal internet connection, the simplest profile, works away from home, latency depends entirely on cloud round-trip. Home-Hub matches the Final Memory Layer/P2P doc's 3-device mesh (Razr Ultra plus Jetson Nano plus Rubik Pi 3): OB1's Postgres/Supabase instance runs on the Jetson Nano's attached SSD, phone reaches it via Tailscale P2P when home, matching that doc's governed continuity memory layer exactly, since OB1 IS that layer formalized. Full Mesh is the Home-Hub profile plus the Rubik Pi 3 Red Agent Auditor: same OB1 placement, but retrieval payloads for code-generation tasks also get audited by the Rubik Pi's Gemma-4 E2B gatekeeper before execution, adding the verification step the Final Memory Layer doc specifies for anything the executor generates and is about to run. None of these profiles requires a different memory architecture, only where OB1's database physically runs and how the phone reaches it changes -- a deliberate design property so the backend doesn't need rebuilding when the user moves from phone-only to phone-plus-home-mesh.", "source_urls": [], "tags": []}
{"id": "synth-arch-006", "section": "component-reference", "heading": "6. Component Reference", "content": "OpenWiki (doc-generation CLI agent, LangGraph/DeepAgents-based) has no memory/RAG/MCP of its own; it's the authoring/maintenance tool, not the runtime store. Its contribution is the SKILL.md convention, confined to Layer 2, deciding how new content gets organized, not runtime retrieval. OB1 (Open Brain) is the mature memory backend: thoughts table core schema, HNSW cosine similarity retrieval, SHA-256 fingerprint ingestion dedup, and a remote-only MCP server by hard rule (no claude_desktop_config.json, no StdioServerTransport, no local Node.js server; deploys as a Supabase Edge Function). This is a genuine architectural constraint: the C++ engine's retrieval call is a real network request even when running fully on-device, since OB1 will not run as an in-process/local server. Taxonomy is skills/ (behavior packs), recipes/ plus schemas/ plus thoughts (memory), extensions/ plus primitives/ (curated project reference), mapping onto the skill/memory/project-reference buckets this ingestion project has been sorting material into. reasoning-bank is not an app but a research pattern (Google, ICLR 2026 submission) for distilled natural-language memory items (Title/Description/Content) from agent trajectories, retrieved by embedding cosine similarity, capped small. Critically, memory items come from both success and failure trajectories -- a failed task becomes a retrievable what-didn't-work item, exactly what a Rubik Pi Red Agent Auditor rejection should feed back into. The Dual-Agent Talker-Reasoner pattern (from the Final Memory Layer/P2P doc, a named architecture pattern not a separate repo): the Query/Talker Agent's job (search, isolate a slice, package a micro-payload) is precisely Layers 3-4 of this document; the Executor Agent's job (receive the payload, do the task, wipe its own history) is Layer 5.", "source_urls": [], "tags": []}
{"id": "synth-arch-007", "section": "retrieval-model-strategy", "heading": "7. Retrieval and Model Strategy Guide", "content": "HNSW cosine similarity (OB1's native mechanism) is the retrieval baseline; reasoning-bank's contribution caps how many results get used (n=1-10), not the similarity mechanism itself. Prompt caching interaction: the retrieved memory-item payload should be treated as the cache-eligible static prefix whenever the downstream model is Claude-API-class (the AESOP cloud-delegation path, or a CLI sub-agent calling Claude); since a user's working session tends to stay on-topic, placing distilled context early in the prompt with a cache_control breakpoint captures the 90-percent-discount cache-read rate. This is a direct, mechanical link between the memory-retrieval architecture and the prompt-caching research already condensed and flagged high priority. Sub-agent fan-out: if a task spins up multiple CLI sub-agents (matching the Final Memory Layer/P2P doc's Cache Pre-warming pattern), they should all read the same retrieved payload rather than each independently querying OB1, both to avoid the concurrency trap (concurrent cold cache writes) and to guarantee sub-agents don't silently diverge on context. On-device vs cloud retrieval cost: retrieval itself is cheap and network-bound, not compute-bound, and doesn't touch the on-device NPU; the NPU/compute-cost concerns from the NPU test-time-scaling paper apply to Layer 5 (generation), not Layers 3-4 of this architecture.", "source_urls": [], "tags": []}
{"id": "synth-arch-008", "section": "interaction-patterns", "heading": "8. Interaction Patterns", "content": "The triggers this backend needs to serve: meta-prompt compilation (the primary case, triggered by the internal mic in the AI Chat Box); skill invocation (when a compiled meta-prompt resolves to a known capability, the system should retrieve the matching SKILL.md rather than re-deriving something the wiki already documents); post-task distillation (not user-triggered, happens automatically after any dispatched task completes or fails); cross-device continuity queries (the Home-Hub and Full Mesh profiles imply a task started on the phone away from home needing its prior context once back on the mesh, a straightforward OB1 retrieval since the store doesn't care which device asks, but the routing decision of whether to reach OB1 remotely right now or wait for Tailscale is an open question).", "source_urls": [], "tags": []}
{"id": "synth-arch-009", "section": "extending-system", "heading": "9. Extending the System / Adding Skills", "content": "Adding a new skill: write a SKILL.md (OpenWiki convention), let the Layer 2 doc-generation agent ingest it into OB1's skills/ bucket, with no code changes needed to Layers 3-5, the entire point of keeping maintenance separate from storage. Adding a new memory source: run it through the standing Layer 1 ingestion pipeline (clean md plus JSONL plus URLs) exactly as this project has been doing for every PDF/doc processed so far. Adjusting retrieval behavior: the n=1-10 cap and success/failure distinction are reasoning-bank-pattern parameters, not OB1 schema changes, tunable without touching the memory store. Adding a new deployment profile (e.g. a future desktop/laptop version, flagged as an open question in the Omni Claw blueprint's own open-questions section): per the deployment profiles section, this should only require deciding where OB1's database runs and how the new device reaches it, not redesigning Layers 3-4.", "source_urls": [], "tags": []}
{"id": "synth-arch-010", "section": "known-gaps", "heading": "10. Known Gaps and Open Questions", "content": "The MCP-between-CLI-and-Kotlin-app question, raised directly by the user in the NotebookLM prompt-caching conversation, is still open: this document assumes OB1's remote MCP server is reachable from the C++ engine, but whether the Kotlin app itself should also expose an MCP server for device-control purposes (client/server roles reversed) is a separate unresolved architectural question flagged as needing independent research. The third repo the user couldn't name, mentioned in the Final Memory Layer/P2P doc as in between OB1 and Open-Wiki, remains unidentified and may fill a gap this document doesn't cover, or may overlap with reasoning-bank's role. Retrieval routing when off-mesh: whether/how the system should retry, queue, or gracefully degrade when the phone can't currently reach OB1 isn't specified here. Auditor-to-memory feedback loop mechanics: this document asserts Rubik Pi Auditor rejections should become failure-pattern memory items, but the actual write path (Auditor writes directly to OB1, or rejection routes back through the Jetson executor first) isn't specified, deliberately left for a future implementation-level doc. Cost of remote-only MCP for latency-sensitive on-device interactions: OB1's hard rule against local/stdio MCP is good for the security model but means every retrieval is a network round trip, and whether this is acceptable for Omni Claw's presumably-fast meta-prompt compilation loop hasn't been measured against real latency numbers.", "source_urls": [], "tags": []}
{"id": "synth-arch-011", "section": "provenance-log", "heading": "11. Source and Provenance Log", "content": "Every claim in this document traces to material already condensed in the Claude_master_wiki Drive folder: the Local Knowledge Synthesis concept traces to the omni-claw-blueprint doc; architecture, data flow, and query flow trace to the openwiki, ob1, and reasoning-bank component surveys plus the omni-claw-blueprint's runtime trigger description; deployment profiles trace to the aesop-full-reference device-profile pattern and the final-memory-layer-p2p-pipeline hardware topology; component details trace to the openwiki, ob1, and reasoning-bank docs plus the final-memory-layer-p2p-pipeline's Dual-Agent Talker-Reasoner description; retrieval and caching strategy traces to the notebooklm-prompt-caching-planning-qa and anthropic-prompt-caching-docs, plus the final-memory-layer-p2p-pipeline's sub-agent caching section and the scaling-llm-test-time-compute-npu paper's compute-cost boundary; interaction patterns trace to the omni-claw-blueprint's meta-prompt loop and the final-memory-layer-p2p-pipeline's task workflow phases; the extension pattern traces to the llm-wiki-termux-setup skill package as a worked example; open questions trace to the notebooklm-prompt-caching-planning-qa's MCP question and the final-memory-layer-p2p-pipeline's unnamed third repo; and the 11-section format template itself is reused from aesop-full-reference per explicit instruction. No new external URLs are introduced by this document; it is a synthesis of already-ingested sources, not a new raw-source ingestion.", "source_urls": [], "tags": []}
```

</details>

## `omni-claw-blueprint.md`
# Novus Agenti {Omni Claw} — Master Vision Blueprint

Source: "Copy of Welcome to the birth of Novus Agenti - {Omni Claw}." (Gemini session export, Drive `1j0lQM69h4eENLX2LKWtRilyQZL1aScyA`). Category: **master architecture reference** — this is the target system; AESOP (see `aesop-full-reference.md`) is its current Termux-scoped implementation.

**Note on source reliability**: this is a Gemini brainstorming/design session, not verified working code — treat every mechanism below as design intent. The transcript's closing section (a long back-and-forth about how to export the chat thread out of the Gemini app) is meta-noise about capturing the conversation, not architecture, and has been excluded from this cleaned version.

## 1. Project Overview

Novus Agenti ("Omni Claw") is a localized, hyper-customized mobile operating environment that treats individual apps, terminal endpoints, and cloud runtimes as modular subroutines. By separating heavy math (model inference) from the UI layer, the phone doesn't act as a simple app container — it acts as an iron-clad local orchestrator for a multi-agent neural mesh.

Core stack: a **Kotlin UI app** (lightweight frontend orchestrator) paired with a **detached native C++ background daemon** (`ort_server`) that runs a **6.2GB Gemma-4 12B QAT model** on the phone's **Hexagon NPU**. The two communicate over a **local loopback WebSocket/HTTP connection at `127.0.0.1:8080`**.

Qualcomm QNN SDK static graph partitioning splits the workload across the SoC: ~5.5GB to the Hexagon NPU, ~0.6GB to the Adreno GPU for custom layers, ~0.1GB to the CPU for housekeeping.

## 2. Core Architecture — The Watchdog Recovery Bridge

To keep the system bulletproof during heavy runtime operations, an isolated **Watchdog Application** layer runs alongside the primary Kotlin UI without the core model weights ever being aware of it.

```
[Android Low Memory Killer (lmkd)] - Ignores Core Daemon
 |
 [Watchdog App] ---> [Local Loopback TCP: 127.0.0.1:8080] <--- [C++ Engine Daemon]
 |                                                              (Unkillable -950)
 +---> Monitors App State & Shared Memory Arena
```

- **The Sandbox Blindspot**: the 6.2GB model runs fully containerized inside the C++ daemon context, communicating purely via JSON over loopback. It has zero awareness of Android UI lifecycle, layout state, or Kotlin process constraints.
- **The Skeleton Crew Failback**: the Watchdog is a featherweight foreground service (~15MB) monitoring the process lifecycle of `NpuClient.kt`. If a memory spike or un-fused tensor forces a foreground UI crash, the Watchdog intercepts the teardown instantly.
- **Invisible Hot-Reboot**: because the C++ engine is anchored to the root init tree at a privileged **`-950 oom_score_adj`** score, it's unaffected by a foreground UI crash. The Watchdog reads the engine's persistent log cache, captures the exact state sequence, and silently hot-reboots the Kotlin Orchestrator container.
- **State Preservation via WebSockets**: a local loopback WebSocket connection continually pushes JSON state frames to a micro-database in local storage. Conversation history, active variables, and pipeline paths are recovered in milliseconds without reloading the 6.2GB tensor graph.

## 3. Agentic Capabilities Map

*(Built per user request — a structured inventory of the distinct agentic capabilities described across the session, not just prose.)*

| Capability | Trigger / Entry Point | Mechanism | Output / Effect |
|---|---|---|---|
| Global voice dictation | Standard Floating Microphone Tile, tap anywhere | Raw speech → local low-latency cleanup pass (strips filler/stutter/typos) → Android Accessibility Service | Types pristine text directly into active cursor field; no reasoning, no tool calls |
| Meta-prompt compilation (verified) | Internal mic icon inside AI Chat Floating Box | Speech → Silero VAD isolates audio → local HTTP/loopback stream to C++ engine → engine pulls Obsidian/Markor skill blocks → compiles structured meta-prompt | Streams back into the AI Chat Box as **editable text** — human reviews/edits before send, never auto-dispatched |
| Local file operations | Model emits tool-call JSON | Android IPC Intents → Tasker Relay (`intent://com.tasker.TRIGGER`) | Read/write Markdown in Markor or Obsidian vaults, scan directory trees, file sorting |
| System actions | Model emits tool-call JSON | Android Accessibility Service frameworks | Launch camera, initiate downloads, pull git repos — ~150MB app overhead target |
| Local shell execution | Agent needs bash/git/compile | Termux GLIBC-patched environment, authenticated JSON-RPC loopback on port `8022` | Bash shortcuts, Python data transformers, `patchelf-glibc` binary patching, GitHub/Hugging Face repo interaction |
| Cloud compute offload | Task needs heavy compute/web | Google Colab CLI with `--auth adc` (Application Default Credentials) | Headless remote T4 GPU provisioning (`colab new --gpu T4`), script transmission (`colab exec`), remote file infra management |
| Agnostic cloud API routing | Task needs external LLM/API | OpenAI-compatible translation layer over public tunnels | Scale-out to OpenRouter, fire scripts to remote worker nodes |
| Fast-pass reasoning (thinking suppression) | Any SSE token stream from a reasoning model (e.g. DeepSeek-R1) | C++ token parser intercepts `<think>`/`</think>` tags | Emits `{"status":"thinking"}` then `{"status":"generating"}` JSON frames instead of raw CoT dump — Claude-style "Thinking..." shimmer |
| Continuous screenshot parsing | Background, automatic | AOSP screen capture loop ingesting frames over time | Maps multi-app pipeline navigation (e.g. Obsidian → Markor → Drive → GitHub), flashes highlighted guides on workspace |
| Contextual point-and-shoot vision | Camera icon in AI Chat Floating Box | Single-frame screen capture + verbal prompt | Step-by-step troubleshooting directions for dense UI (e.g. Google Cloud Console IAM dashboards), detects UI/layout drift over time |
| Local storage directory sniffer | Automatic, standard Android screenshot gesture | File observer watching `/sdcard/Pictures/Screenshots/` | Pulls raw image bytes into model context instantly, zero manual upload |
| Session/share export | Share icon in AI Chat Floating Box | Bundles code fragments/terminal output/scripts | Exports directly to Google Drive or Google Docs |
| Multi-session management | Carbon Tabs in AI Chat Floating Box | Session/tab state switching | Hot-swap between separate conversation sessions or engineering branches |
| Crash recovery | Automatic, background | Watchdog service + WebSocket state frames | Hot-reboot Kotlin UI without reloading model weights |
| Multi-window / multi-device orchestration | Split-screen or TigerVNC cast (e.g. to Tab S9 FE at port `5901`) | 3-panel layout mimicking Cloud Shell Editor | Parallel cloud/vision + local shell + chat orchestration across devices |

## 4. The Dual-Tile Floating UI

Corrected mid-session by the user — the two tiles have **strictly separated scopes** to prevent accidental command dispatch:

**Tile A: Global Microphone Tile**
- Scope: system-wide, any active Android input field (Gboard-replacement behavior)
- Behavior: raw voice → local cleanup pass (filler words/stutters/typos removed) → Accessibility Service types directly into the active cursor field
- No reasoning, no meta-prompting, no tool calls — pure high-fidelity dictation

**Tile B: AI Chat Floating Box** (expandable sandbox)
- `[+]` Upload Tray — attach local files/schemas/images to context
- `[📷]` Camera Icon — point-and-shoot screen capture for vision troubleshooting
- `[🎙]` Internal/Sandbox Microphone Icon — dedicated agentic-command dictation (separate from Tile A)
- `[⏸]` Pause Button — freeze token streaming / halt cloud execution mid-flight
- `[📑]` Carbon Tabs — organize/hot-swap separate conversation sessions or engineering branches
- `[⬆]` Share Icon — bundle code/logs/output, export to Drive or Docs

**The Meta-Prompt Verification Loop** (the human-in-the-loop safety mechanism):
1. Tap internal mic in AI Chat Box, speak rough high-level intent
2. Silero VAD (sub-5MB ONNX, 30ms evaluation loop) isolates the speech
3. Raw text/audio routes over local loopback to the C++ engine
4. Engine synthesizes the request against skill blocks/API schemas/context pulled from local Obsidian/Markor directories — **this is the slot the OpenWiki+OB1+reasoning-bank memory system fills**
5. Compiled meta-prompt streams back into the AI Chat Box as **editable text**, not auto-executed
6. Human reviews, edits, clips parameters, or flags corrections
7. Manual send dispatches to the target local/cloud execution endpoint

## 5. Multi-Window Neural Mesh Layout

Whether in split-screen on a tablet or cast via TigerVNC to a secondary display (e.g. Samsung Tab S9 FE at port `5901`), the layout mimics a Cloud Shell Editor environment:

| Panel | Runtime | Purpose |
|---|---|---|
| Panel 1: Cloud & Vision Space | Android WebView / local browser | Live browser instances, remote Colab dev loops, cloud buckets, visualization graphs |
| Panel 2: Local Command Cockpit | Patched Termux GLIBC terminal multiplexer | Runs terminal CLIs side-by-side (e.g. `claude-code` CLI on Pro subscription alongside a DeepSeek-R1 logic solver via `ANTHROPIC_BASE_URL` override) |
| Panel 3: Omni-Claw Chat Panel | Kotlin frontend orchestrator client | Central conductor — STT input, thinking-state display, execution logs, file trays |

## 6. Local vs. Cloud Agentic Tool Execution Map

```
                +---> LOCAL LOOP:  Kotlin App -> Tasker Relay -> Local Files / Obsidian
[Omni-Claw Agent] ---+
                +---> CLOUD LOOP:  Termux Shell -> Colab CLI (ADC) -> Remote Compilation / Git
```

| Execution Zone | Integration Layer | Capabilities |
|---|---|---|
| Local Device Loop | Android IPC Intents & Tasker Relay | Fires payloads to `intent://com.tasker.TRIGGER` — read/write Markor/Obsidian, file sorting, native system status |
| Local Shell Loop | Termux GLIBC-patched environment | Authenticated JSON-RPC loopback on port `8022` — terminal workspace nav, bash shortcuts, `patchelf-glibc`, git hooks |
| Cloud Engine Loop | Google Colab CLI (`--auth adc`) | Headless T4 GPU provisioning (`colab new --gpu T4`), Python script transmission (`colab exec`), Qualcomm AI Engine SDK model compilation |

**Workload distribution rationale**:

| Function | Handler | Why |
|---|---|---|
| Floating windows & overlays | Kotlin (native) | Android WindowManager handles smooth persistent rendering over other apps |
| Screen vision ingestion | Kotlin (native) | Native AOSP capture loops pull frame bytes into memory faster than any script |
| Bash/git/CLI compiling | Termux (headless socket) | Native Linux ecosystem compiled for Android without OS restrictions |
| System automation & macros | Tasker/MacroDroid intents | Rapidly alters device settings, automates third-party UI steps via clean intent pipelines |

## 7. Triple-Mode Screen Vision Framework

```
              +---> Mode 1: Continuous Screenshot Parsing (AOSP Accessibility API Loop)
              |
[Screen Capture] -+---> Mode 2: Contextual Point-and-Shoot (Floating Chat Camera Icon)
              |
              +---> Mode 3: Local Storage Directory Sniffer (Automated File Observer Loop)
```

- **Mode 1 — Continuous Screenshot Parsing**: background AOSP screen capture loop ingests frames over time, tuned for structural navigation mapping across multiple interconnected apps (e.g. walking a user through an Obsidian → Markor → Drive → GitHub pipeline), cross-referencing the current step against the visual UI.
- **Mode 2 — Contextual Point-and-Shoot**: triggered only by the Camera Icon in the AI Chat Floating Box. Single-frame capture + verbal prompt, for troubleshooting dense cloud console/IAM-style dashboards. Detects UI/layout drift over time (e.g. a dated post-update navigation change) and reroutes accordingly.
- **Mode 3 — Local Storage Directory Sniffer**: fallback loop for when continuous streaming throttles performance. Watches `/sdcard/Pictures/Screenshots/`; the moment a hardware-button screenshot lands, the app pulls the raw image bytes straight into the model's visual context — zero manual upload lag.

## 8. Fast-Pass Reasoning (Thinking Token Suppression)

To avoid the interface feeling slow reading through raw chain-of-thought text before an answer prints, the C++ engine intercepts reasoning-model tag streams at the parser layer:

```
[Raw Streaming Tokens] ---> [C++ Token Parser Interceptor]
    |
    +---> Detects `<think>`  ---> Emits `{"status": "thinking"}`  (drops raw text)
    +---> Detects `</think>` ---> Emits `{"status": "output"}`    (streams live text)
```

Because the C++ daemon manages the SSE token stream over `127.0.0.1:8080`, it can block raw reasoning text at the source: the moment the opening `<think>` tag is seen, the daemon drops the text and emits a single lightweight frame; the Kotlin client shows a clean animated "Thinking..." indicator; the moment `</think>` appears, the state flips back and finalized answer tokens stream through normally.

## 9. Corrections Made Mid-Session (design decisions, not just brainstorm)

The user explicitly corrected two things during this session — worth preserving as locked-in decisions, not just one of several floated ideas:

1. **Tile separation is strict.** The global microphone tile must behave *exactly* like a Gboard-style dictation replacement — clean typo/filler correction, direct text injection, **no meta-prompting, no reasoning**. All meta-prompt compilation happens only via the internal mic inside the AI Chat Floating Box, specifically so the user can verify/edit before anything is dispatched.
2. **Thinking tokens should be suppressed**, not displayed verbatim — a simple "Thinking..." indicator (Claude-style) is preferred over dumping raw chain-of-thought text, since reading through it just slows the user down without adding value if the eventual answer quality doesn't depend on seeing it.

## 10. Open Questions / Not Yet Resolved

These were raised in the session but not locked down — flag as open design decisions:
- Whether local tool use/inference should route entirely through Tasker/Termux, or whether Android runtimes/Kotlin build code can streamline/replace parts of that split (the session leans toward the hybrid Kotlin-broker + Termux-headless-daemon + Tasker-macro-glue split described in §6, but this was presented as the recommended answer, not something the user had independently confirmed elsewhere).
- Exact desktop/laptop version of the app is described as "a future version" — not scoped in this session.
- P2P capabilities between the user's Motorola and other AI Dev SoCs/SoMs are mentioned as a target capability but not architected here.

## Relationship to AESOP

See `aesop-full-reference.md` for the detailed side-by-side. In short: AESOP is confirmed (by the user) as the **Termux-nerfed subset** of this vision — same voice-first / meta-prompt-compilation / device-profile concept, but running inside Termux+PRoot-Ubuntu with cloud Claude API delegation instead of a native Kotlin app with local Gemma-4 NPU inference, no Watchdog, no floating tiles, no screen vision, and no Tasker/Colab tool-execution split.

## Relationship to the OpenWiki + OB1 + reasoning-bank memory system

Section 4's "Local Knowledge Synthesis" step — the C++ engine pulling "skill blocks, API schemas, and historical code contexts directly out of your local Obsidian or Markor directories" before compiling a meta-prompt — is the exact function the wiki/memory synthesis project (see separate integration architecture doc, in progress) is meant to formalize and back with real infrastructure: OB1's `thoughts` table + remote MCP as the actual store, OpenWiki's `SKILL.md` convention as the maintenance layer, reasoning-bank's success/failure memory-item pattern as the retrieval strategy — replacing ad hoc "read some Obsidian files" with a structured, queryable memory backend.

<details><summary>chunk layer (JSONL) + source URLs</summary>

```jsonl
{"id": "omniclaw-001", "section": "project-overview", "heading": "Project Overview", "content": "Novus Agenti (Omni Claw) is a localized, hyper-customized mobile operating environment that treats individual apps, terminal endpoints, and cloud runtimes as modular subroutines. By separating heavy math (model inference) from the UI layer, the phone acts as an iron-clad local orchestrator for a multi-agent neural mesh rather than a simple app container. Core stack: a Kotlin UI app (lightweight frontend orchestrator) paired with a detached native C++ background daemon (ort_server) running a 6.2GB Gemma-4 12B QAT model on the phone's Hexagon NPU, communicating over a local loopback WebSocket/HTTP connection at 127.0.0.1:8080. Qualcomm QNN SDK static graph partitioning splits the workload: about 5.5GB to the Hexagon NPU, about 0.6GB to the Adreno GPU for custom layers, about 0.1GB to the CPU for housekeeping. This is a Gemini brainstorming session, not verified working code -- treat all mechanisms as design intent.", "source_urls": [], "tags": []}
{"id": "omniclaw-002", "section": "watchdog-recovery", "heading": "Core Architecture -- The Watchdog Recovery Bridge", "content": "An isolated Watchdog Application layer runs alongside the primary Kotlin UI without the core model weights ever being aware of it, to keep the system bulletproof during heavy runtime operations. The Sandbox Blindspot: the 6.2GB model runs fully containerized inside the C++ daemon context, communicating purely via JSON over loopback, with zero awareness of Android UI lifecycle or Kotlin process constraints. The Skeleton Crew Failback: the Watchdog is a featherweight foreground service (~15MB) monitoring the process lifecycle of NpuClient.kt; if a memory spike or un-fused tensor forces a foreground UI crash, the Watchdog intercepts the teardown instantly. Invisible Hot-Reboot: the C++ engine is anchored to the root init tree at a privileged -950 oom_score_adj score, making it unaffected by a foreground UI crash; the Watchdog reads the engine's persistent log cache, captures the exact state sequence, and silently hot-reboots the Kotlin Orchestrator container. State Preservation via WebSockets: a local loopback WebSocket connection continually pushes JSON state frames to a micro-database in local storage, so conversation history, active variables, and pipeline paths recover in milliseconds without reloading the 6.2GB tensor graph.", "source_urls": [], "tags": []}
{"id": "omniclaw-003", "section": "agentic-capabilities-map", "heading": "Agentic Capabilities Map", "content": "Structured inventory of distinct agentic capabilities described across the session. Global voice dictation: triggered by the standard floating microphone tile tapped anywhere; raw speech goes through a local low-latency cleanup pass (strips filler/stutter/typos) then Android Accessibility Service types pristine text directly into the active cursor field, with no reasoning or tool calls. Meta-prompt compilation (verified): triggered by the internal mic icon inside the AI Chat Floating Box; speech goes through Silero VAD isolation, local loopback stream to the C++ engine, which pulls Obsidian/Markor skill blocks and compiles a structured meta-prompt that streams back into the AI Chat Box as editable text for human review before send -- never auto-dispatched. Local file operations: model emits tool-call JSON routed via Android IPC Intents to a Tasker Relay (intent://com.tasker.TRIGGER) for reading/writing Markdown in Markor or Obsidian vaults, scanning directory trees, and file sorting. System actions: model emits tool-call JSON handled via Android Accessibility Service frameworks to launch cameras, initiate downloads, or pull git repos, targeting about 150MB app overhead. Local shell execution: routed through a Termux GLIBC-patched environment via an authenticated JSON-RPC loopback on port 8022, for bash shortcuts, Python data transformers, patchelf-glibc binary patching, and GitHub/Hugging Face repo interaction. Cloud compute offload: via Google Colab CLI with --auth adc Application Default Credentials, for headless remote T4 GPU provisioning (colab new --gpu T4) and script transmission (colab exec). Agnostic cloud API routing: an OpenAI-compatible translation layer over public tunnels scales out to OpenRouter or fires scripts to remote worker nodes. Fast-pass reasoning: a C++ token parser intercepts <think>/</think> tags from any SSE token stream (e.g. DeepSeek-R1) and emits status frames instead of raw chain-of-thought dump. Continuous screenshot parsing: an automatic background AOSP screen capture loop maps multi-app pipeline navigation and flashes highlighted guides. Contextual point-and-shoot vision: triggered by the camera icon in the AI Chat Floating Box for single-frame capture plus verbal prompt, giving step-by-step troubleshooting directions for dense UI. Local storage directory sniffer: an automatic file observer on /sdcard/Pictures/Screenshots/ pulls raw image bytes into model context instantly on a standard screenshot gesture. Session/share export: the share icon in the AI Chat Floating Box bundles code/output and exports to Drive or Docs. Multi-session management: carbon tabs in the AI Chat Floating Box hot-swap between separate conversation sessions or engineering branches. Crash recovery: the Watchdog service plus WebSocket state frames hot-reboot the Kotlin UI without reloading model weights. Multi-window/multi-device orchestration: split-screen or TigerVNC cast (e.g. to a Tab S9 FE at port 5901) drives a 3-panel layout mimicking a Cloud Shell Editor for parallel cloud/vision plus local shell plus chat orchestration across devices.", "source_urls": [], "tags": []}
{"id": "omniclaw-004", "section": "dual-tile-ui", "heading": "The Dual-Tile Floating UI", "content": "Corrected mid-session by the user to have strictly separated scopes preventing accidental command dispatch. Tile A, the Global Microphone Tile: system-wide scope across any active Android input field, behaving as a Gboard replacement -- raw voice goes through a local cleanup pass removing filler words/stutters/typos, then Accessibility Service types directly into the active cursor field, with no reasoning, no meta-prompting, and no tool calls. Tile B, the AI Chat Floating Box, an expandable sandbox with: a Plus-sign Upload Tray to attach local files/schemas/images to context; a Camera Icon for point-and-shoot screen capture vision troubleshooting; an internal Sandbox Microphone Icon dedicated to agentic-command dictation, separate from Tile A; a Pause Button to freeze token streaming or halt cloud execution mid-flight; Carbon Tabs to organize/hot-swap separate conversation sessions or engineering branches; and a Share Icon to bundle code/logs/output and export to Drive or Docs. The Meta-Prompt Verification Loop is the human-in-the-loop safety mechanism: tap the internal mic in the AI Chat Box and speak a rough high-level intent; Silero VAD (sub-5MB ONNX, 30ms evaluation loop) isolates the speech; the raw text/audio routes over local loopback to the C++ engine; the engine synthesizes the request against skill blocks/API schemas/context pulled from local Obsidian/Markor directories (this is the slot the OpenWiki+OB1+reasoning-bank memory system is meant to fill); the compiled meta-prompt streams back into the AI Chat Box as editable text, not auto-executed; the human reviews, edits, clips parameters, or flags corrections; manual send dispatches to the target local or cloud execution endpoint.", "source_urls": [], "tags": []}
{"id": "omniclaw-005", "section": "multi-window-layout", "heading": "Multi-Window Neural Mesh Layout", "content": "Whether in split-screen on a tablet or cast via TigerVNC to a secondary display such as a Samsung Tab S9 FE at port 5901, the layout mimics a Cloud Shell Editor environment with three panels. Panel 1, Cloud & Vision Space, runs on Android WebView or a local browser interface, housing live browser instances, remote Colab developer loops, cloud buckets, and visualization graphs. Panel 2, Local Command Cockpit, runs on a patched Termux GLIBC terminal multiplexer, running terminal CLIs side-by-side such as the claude-code CLI on a Pro subscription alongside a DeepSeek-R1 logic solver via an ANTHROPIC_BASE_URL override. Panel 3, the Omni-Claw Chat Panel, runs the Kotlin frontend orchestrator client as the central conductor -- STT input, thinking-state display, execution logs, and file trays.", "source_urls": [], "tags": []}
{"id": "omniclaw-006", "section": "tool-execution-map", "heading": "Local vs. Cloud Agentic Tool Execution Map", "content": "The Local Loop routes Kotlin App calls through a Tasker Relay to Local Files/Obsidian. The Cloud Loop routes Termux Shell calls through the Colab CLI (with ADC auth) to remote compilation/git. Three execution zones: Local Device Loop uses Android IPC Intents and a Tasker Relay, firing payloads to intent://com.tasker.TRIGGER for reading/writing Markor/Obsidian, file sorting, and native system status. Local Shell Loop uses a Termux GLIBC-patched environment via an authenticated JSON-RPC loopback on port 8022, for terminal workspace navigation, bash shortcuts, patchelf-glibc, and git hooks. Cloud Engine Loop uses the Google Colab CLI with --auth adc for headless T4 GPU provisioning (colab new --gpu T4), Python script transmission (colab exec), and Qualcomm AI Engine SDK model compilation. Workload distribution rationale: floating windows and overlays go to Kotlin native code because Android WindowManager handles smooth persistent rendering over other apps; screen vision ingestion goes to Kotlin native code because AOSP capture loops pull frame bytes into memory faster than any script; bash/git/CLI compiling goes to Termux as a headless socket because it's a native Linux ecosystem compiled for Android without OS restrictions; system automation and macros go to Tasker/MacroDroid intents because they rapidly alter device settings and automate third-party UI steps via clean intent pipelines.", "source_urls": [], "tags": []}
{"id": "omniclaw-007", "section": "screen-vision-framework", "heading": "Triple-Mode Screen Vision Framework", "content": "Three separate computer vision ingestion modes. Mode 1, Continuous Screenshot Parsing: a background AOSP screen capture loop ingests frames over time, tuned for structural navigation mapping across multiple interconnected apps such as walking a user through an Obsidian to Markor to Drive to GitHub pipeline, cross-referencing the current step against the visual UI and flashing highlighted guides. Mode 2, Contextual Point-and-Shoot: triggered only by the Camera Icon in the AI Chat Floating Box, a single-frame capture plus verbal prompt for troubleshooting dense cloud console/IAM-style dashboards; it detects UI/layout drift over time such as a dated post-update navigation change and reroutes accordingly. Mode 3, Local Storage Directory Sniffer: a fallback loop for when continuous streaming throttles performance, watching /sdcard/Pictures/Screenshots/; the moment a hardware-button screenshot lands, the app pulls the raw image bytes straight into the model's visual context window with zero manual upload lag.", "source_urls": [], "tags": []}
{"id": "omniclaw-008", "section": "fast-pass-reasoning", "heading": "Fast-Pass Reasoning -- Thinking Token Suppression", "content": "To avoid the interface feeling slow from reading raw chain-of-thought text before an answer prints, the C++ engine intercepts reasoning-model tag streams at the parser layer. Because the C++ daemon manages the SSE token stream over 127.0.0.1:8080, it can block raw reasoning text at the source: the moment the opening <think> tag is seen, the daemon drops the raw text and emits a single lightweight JSON frame {\"status\": \"thinking\"}; the Kotlin client shows a clean animated 'Thinking...' indicator; the moment the closing </think> tag appears, the state flips to {\"status\": \"generating\"} or {\"status\": \"output\"} and finalized answer tokens stream through normally.", "source_urls": [], "tags": []}
{"id": "omniclaw-009", "section": "locked-decisions", "heading": "Corrections Made Mid-Session -- Locked-In Design Decisions", "content": "The user explicitly corrected two things during this session, which should be preserved as locked-in decisions rather than one of several floated ideas. First, tile separation is strict: the global microphone tile must behave exactly like a Gboard-style dictation replacement with clean typo/filler correction and direct text injection, with no meta-prompting and no reasoning; all meta-prompt compilation happens only via the internal mic inside the AI Chat Floating Box specifically so the user can verify and edit before anything is dispatched. Second, thinking tokens should be suppressed rather than displayed verbatim -- a simple Claude-style 'Thinking...' indicator is preferred over dumping raw chain-of-thought text, since reading through it slows the user down without adding value if final answer quality doesn't depend on seeing it.", "source_urls": [], "tags": []}
{"id": "omniclaw-010", "section": "open-questions", "heading": "Open Questions / Not Yet Resolved", "content": "Several items were raised in the session but not locked down as final decisions. Whether local tool use/inference should route entirely through Tasker/Termux, or whether Android runtimes/Kotlin build code can streamline or replace parts of that split -- the session leans toward the hybrid Kotlin-broker plus Termux-headless-daemon plus Tasker-macro-glue split, but this was presented as a recommended answer, not something independently confirmed elsewhere by the user. The exact desktop/laptop version of the app is described only as 'a future version,' not scoped in this session. P2P capabilities between the user's Motorola phone and other AI Dev SoCs/SoMs are mentioned as a target capability but not architected in this session.", "source_urls": [], "tags": []}
{"id": "omniclaw-011", "section": "relationship-to-aesop", "heading": "Relationship to AESOP", "content": "AESOP is confirmed by the user as the Termux-nerfed subset of this Omni Claw vision -- same voice-first / meta-prompt-compilation / device-profile concept, but running inside Termux plus PRoot-Ubuntu with cloud Claude API delegation instead of a native Kotlin app with local Gemma-4 NPU inference, with no Watchdog, no floating tiles, no screen vision, and no Tasker/Colab tool-execution split. See the separate aesop-full-reference doc for the detailed side-by-side comparison table.", "source_urls": [], "tags": []}
{"id": "omniclaw-012", "section": "relationship-to-memory-system", "heading": "Relationship to the OpenWiki + OB1 + reasoning-bank memory system", "content": "The Meta-Prompt Verification Loop's 'Local Knowledge Synthesis' step -- the C++ engine pulling skill blocks, API schemas, and historical code contexts directly out of local Obsidian or Markor directories before compiling a meta-prompt -- is the exact function the separate wiki/memory synthesis project is meant to formalize and back with real infrastructure: OB1's thoughts table plus remote MCP as the actual store, OpenWiki's SKILL.md convention as the maintenance layer, and reasoning-bank's success/failure memory-item pattern as the retrieval strategy, replacing ad hoc 'read some Obsidian files' behavior with a structured, queryable memory backend.", "source_urls": [], "tags": []}
```

</details>

## `repo-fork-asset-list-source.md`
Important assets; 

llama.cpp-npu, EdgeAIApp-ExecuTorch, c10vis-poem/mlc-llm, c10vis-poem/genymotion-device-web-player  
c10vis-poem/off-grid-ai-mobile  
c10vis-poem/qcom-build-utils  
c10vis-poem/snapdragon-npu-llm  
c10vis-poem/docker-pkg-build

Important on device layers that I will be running; 

c10vis-poem/tmux ,  c10vis-poem/claude-code-android  
c10vis-poem/termux-packages  
c10vis-poem/OmniRoute  
c10vis-poem/tmux-assistant-resurrect

Very important for proof of concept, reverse engineering, and troubleshoot fall back also research guide: 

c10vis-poem/off-grid-ai-mobile

c10vis-poem/VarientDemoApplication

  Magisk   
{This is Genymobile, her entire repo and website deserve scraping This will not be the last time you hear me mention her or see a resource of hers posted if I were you I would task agents of scrubbing her entire online and GitHub libraries\]

edge-overlay-ai   

nexa-sdk   
I really don't think this works anymore but it could be worth looking at for reverse engineering and maybe it does support some active API key but I dk

c10vis-poem/scrcpy

Resources ;

qcom-build-utils    adk-docs  c10vis-poem/AI-Agents-Projects-Tutorials

trusted-firmware-a    **ai-hub-apps**

Skills ; Don't sleep on these\! 

SuperClaude Framework // Claude-skills //  
leaked-system-prompts //   
Prompt-eng-interactive-tutorial //   
c10vis-poem/claude-code-best-practice

Probably the most important skill this right here teaches Claude how to reverse engineer Android apps; 

c10vis-poem/claude-android-skill  
c10vis-poem/android-reverse-engineering-skill

Voice and memory  layers   
\[ most likely get the version best suited to match not sure of its tflite\]

moonshine-tflite   c10vis-poem/silero-vad  
Kokoro   
OB1 reasoning-bank

Honorable mention;   
c10vis-poem/kernel-topics  
c10vis-poem/snagboot

## `repo-fork-asset-list.md`
# REPO FORK ASSET LIST — Reference

Source: "REPO FORK ASSET LIST DO NOT SKIP THIS" (Drive `1xz7WmtUZbYOrNomEFixWxobSroAb_vT8d9XTwiIxHks`, Google Doc, sitting directly in Claude_master_wiki). Category: **repo index / fork checklist**. This is the user's own master list of every GitHub repo relevant to the Omni Claw / Novus Agenti build. All repos on this list have been confirmed already forked under the `c10vis-poem` GitHub account (verified live against the account's 52 public repos this session) — this document is the index explaining *why* each one matters, not an outstanding to-do list.

## Important Assets

Core NPU/edge-inference tooling: `llama.cpp-npu`, `EdgeAIApp-ExecuTorch`, `mlc-llm`, `genymotion-device-web-player`, `off-grid-ai-mobile`, `qcom-build-utils`, `snapdragon-npu-llm`, `docker-pkg-build`.

## Important On-Device Layers (will be running)

`tmux`, `claude-code-android`, `termux-packages`, `OmniRoute`, `tmux-assistant-resurrect`.

## Very Important — Proof of Concept, Reverse Engineering, Troubleshoot Fallback, Research Guide

`off-grid-ai-mobile`, `VarientDemoApplication`, `Magisk`.

**User note on Genymobile**: "This is Genymobile, her entire repo and website deserve scraping. This will not be the last time you hear me mention her or see a resource of hers posted — if I were you I would task agents of scrubbing her entire online and GitHub libraries." (Not yet acted on — flagged as an open follow-up task, not something this project has done.)

`edge-overlay-ai`, `nexa-sdk` — user's own assessment: "I really don't think this works anymore but it could be worth looking at for reverse engineering and maybe it does support some active API key but I dk." This matches the independent live-verification finding recorded in `omniNeural-4b-status-correction.md` — Nexa's Hugging Face presence is effectively abandoned as of this session.

`scrcpy`.

## Resources

`qcom-build-utils`, `adk-docs`, `AI-Agents-Projects-Tutorials`, `trusted-firmware-a`, `ai-hub-apps`.

## Skills — "Don't sleep on these!"

`SuperClaude Framework`, `Claude-skills`, `leaked-system-prompts`, `Prompt-eng-interactive-tutorial`, `claude-code-best-practice`.

**Highlighted as probably the most important skill** — "teaches Claude how to reverse engineer Android apps": `claude-android-skill`, `android-reverse-engineering-skill`.

## Voice and Memory Layers

"Most likely get the version best suited to match, not sure of its tflite": `moonshine-tflite`, `silero-vad`, `Kokoro`. Memory layer: `OB1`, `reasoning-bank`.

## Honorable Mention

`kernel-topics`, `snagboot`.

## Cross-Reference: Repo READMEs Already Condensed Individually

Several repos from this list have their own dedicated reference docs elsewhere in this wiki, sourced from README exports found in the "ANDROID APK/RESEARCH DOCS" Drive folder (the "text reminders per fork repo" the user was referring to — a set of numbered `README (N) (Markor)` files, one per repo):

- `qcom-build-utils` — centralized build tooling for the Qualcomm Linux Debian package ecosystem (reusable GitHub Actions workflows, ABI checking, package promotion/release pipelines).
- `snapdragon-npu-llm` — runs real LLMs on Hexagon NPU including the "unsupported" Snapdragon 8 Gen 1 (Hexagon v69), built on a K9FxNa Hugging Face `.pte` artifact + ExecuTorch QNN backend; verified 31.3 tok/s on a OnePlus 10 Pro.
- `silero-vad` — pre-trained enterprise-grade Voice Activity Detector, MIT license, ONNX/PyTorch, sub-1ms per 30ms audio chunk on CPU, 8kHz/16kHz sampling, no telemetry/keys/lock-in.
- `off-grid-ai-mobile` — **this is the app on the user's phone** referenced earlier in this session ("underground LM" / model browser with storage indicator and local-server auto-discovery). Full offline AI suite (text/image generation, vision, voice, tool calling, document analysis) for Android/iOS/macOS. Runs Qwen 3, Llama 3.2, Gemma 3, Phi-4, or any GGUF; discovers and connects to local OpenAI-compatible servers (Ollama, LM Studio, LocalAI) automatically; on-device Stable Diffusion (NPU-accelerated on Snapdragon); Whisper STT; project knowledge base with on-device embedding + cosine retrieval in SQLite. MIT-licensed core, optional $50 lifetime Pro tier (custom personas, voice mode, calendar/email/MCP integration).
- `android-reverse-engineering-skill` — Claude Code skill (Apache-2.0) that decompiles APK/XAPK/JAR/AAR via jadx/Fernflower/Vineflower and extracts HTTP APIs (Retrofit endpoints, OkHttp calls, hardcoded URLs, auth patterns), traces call flows from UI through to network calls, handles ProGuard/R8 obfuscation.
- `EdgeAIApp-ExecuTorch` — on-device CLIP (OpenAI's vision-language model) via ExecuTorch + Qualcomm QNN backend on Android; zero-shot image classification, ~100-150ms inference, ~800MB RAM, requires Snapdragon 8 Gen 2/3/Elite (QNN v79).

## Relevance to This Project

This doc is the master index for everything in the GitHub repo-scraping phase of the project. Its own content maps directly onto Omni Claw's execution layers: the "on-device layers" section (`tmux`, `termux-packages`, `OmniRoute`, `claude-code-android`) matches the blueprint's Local Shell Loop; the "voice and memory layers" section (`silero-vad`, `Kokoro`, `OB1`, `reasoning-bank`) matches the Local Knowledge Synthesis backend already formalized in `omni-claw-knowledge-synthesis-architecture.md`; and `off-grid-ai-mobile` is a strong working precedent for Omni Claw's own model-browser/local-server-discovery UX (its "Remote LLM Servers" feature — auto-discover OpenAI-compatible servers on the local network — is effectively what Omni Claw's OmniRoute integration and cloud-fallback routing would need to replicate).

<details><summary>chunk layer (JSONL) + source URLs</summary>

```jsonl
{"id": "repolist-001", "section": "important-assets", "heading": "Important Assets", "content": "Core NPU/edge-inference tooling repos: llama.cpp-npu, EdgeAIApp-ExecuTorch, mlc-llm, genymotion-device-web-player, off-grid-ai-mobile, qcom-build-utils, snapdragon-npu-llm, docker-pkg-build. All confirmed already forked under the c10vis-poem GitHub account.", "source_urls": [], "tags": []}
{"id": "repolist-002", "section": "on-device-layers", "heading": "Important On-Device Layers (will be running)", "content": "Repos the user will actually run on-device: tmux, claude-code-android, termux-packages, OmniRoute, tmux-assistant-resurrect.", "source_urls": [], "tags": []}
{"id": "repolist-003", "section": "proof-of-concept-reverse-engineering", "heading": "Very Important: Proof of Concept, Reverse Engineering, Troubleshoot Fallback, Research Guide", "content": "off-grid-ai-mobile, VarientDemoApplication, Magisk. User note on Genymobile: this is Genymobile, her entire repo and website deserve scraping, this will not be the last time you hear me mention her or see a resource of hers posted, if I were you I would task agents of scrubbing her entire online and GitHub libraries -- flagged as an open follow-up task, not yet acted on. edge-overlay-ai and nexa-sdk: user's own assessment is I really don't think this works anymore but it could be worth looking at for reverse engineering and maybe it does support some active API key but I dk, matching the independent live-verification finding that Nexa's Hugging Face presence is effectively abandoned. Also scrcpy.", "source_urls": [], "tags": []}
{"id": "repolist-004", "section": "resources", "heading": "Resources", "content": "qcom-build-utils, adk-docs, AI-Agents-Projects-Tutorials, trusted-firmware-a, ai-hub-apps.", "source_urls": [], "tags": []}
{"id": "repolist-005", "section": "skills", "heading": "Skills -- Don't Sleep On These", "content": "SuperClaude Framework, Claude-skills, leaked-system-prompts, Prompt-eng-interactive-tutorial, claude-code-best-practice. Highlighted as probably the most important skill, teaching Claude how to reverse engineer Android apps: claude-android-skill, android-reverse-engineering-skill.", "source_urls": [], "tags": []}
{"id": "repolist-006", "section": "voice-memory-layers", "heading": "Voice and Memory Layers", "content": "Most likely get the version best suited to match, not sure of its tflite: moonshine-tflite, silero-vad, Kokoro. Memory layer: OB1, reasoning-bank.", "source_urls": [], "tags": []}
{"id": "repolist-007", "section": "honorable-mention", "heading": "Honorable Mention", "content": "kernel-topics, snagboot.", "source_urls": [], "tags": []}
{"id": "repolist-008", "section": "cross-reference-readmes", "heading": "Cross-Reference: Repo READMEs Already Condensed Individually", "content": "Several repos from this list have dedicated reference docs elsewhere in this wiki, sourced from README exports found in the ANDROID APK/RESEARCH DOCS Drive folder -- the text reminders per fork repo the user referred to, a set of numbered README (N) (Markor) files, one per repo. qcom-build-utils is centralized build tooling for the Qualcomm Linux Debian package ecosystem with reusable GitHub Actions workflows, ABI checking, and package promotion/release pipelines. snapdragon-npu-llm runs real LLMs on Hexagon NPU including the unsupported Snapdragon 8 Gen 1 (Hexagon v69), built on a K9FxNa Hugging Face .pte artifact plus ExecuTorch QNN backend, verified 31.3 tok/s on a OnePlus 10 Pro. silero-vad is a pre-trained enterprise-grade Voice Activity Detector, MIT license, ONNX/PyTorch, sub-1ms per 30ms audio chunk on CPU, 8kHz/16kHz sampling, no telemetry/keys/lock-in. off-grid-ai-mobile is the app on the user's phone referenced earlier in this session (model browser with storage indicator and local-server auto-discovery), a full offline AI suite for Android/iOS/macOS running Qwen 3/Llama 3.2/Gemma 3/Phi-4/any GGUF, discovering local OpenAI-compatible servers automatically (Ollama, LM Studio, LocalAI), with on-device Stable Diffusion, Whisper STT, and an on-device embedding plus cosine retrieval knowledge base in SQLite. android-reverse-engineering-skill is a Claude Code skill (Apache-2.0) that decompiles APK/XAPK/JAR/AAR via jadx/Fernflower/Vineflower and extracts HTTP APIs, traces call flows, and handles ProGuard/R8 obfuscation. EdgeAIApp-ExecuTorch runs on-device CLIP via ExecuTorch plus Qualcomm QNN backend on Android for zero-shot image classification, about 100-150ms inference, about 800MB RAM, requiring Snapdragon 8 Gen 2/3/Elite (QNN v79).", "source_urls": [], "tags": []}
{"id": "repolist-009", "section": "relevance", "heading": "Relevance to This Project", "content": "This doc is the master index for the GitHub repo-scraping phase of the project. Its content maps directly onto Omni Claw's execution layers: the on-device layers section (tmux, termux-packages, OmniRoute, claude-code-android) matches the blueprint's Local Shell Loop; the voice and memory layers section (silero-vad, Kokoro, OB1, reasoning-bank) matches the Local Knowledge Synthesis backend already formalized in the integration architecture doc; and off-grid-ai-mobile is a strong working precedent for Omni Claw's own model-browser/local-server-discovery UX, since its Remote LLM Servers feature (auto-discover OpenAI-compatible servers on the local network) is effectively what Omni Claw's OmniRoute integration and cloud-fallback routing would need to replicate.", "source_urls": [], "tags": []}
```

</details>

## `welcome-to-the-birth-of-novus-agenti-omni-claw.txt`
"
Gemini
Model Accesses Notebook Resources
https://gemini.google.com/share/ce7823dbd1a7
Created with 3.5 Flash June 21, 2026 at 01:59 AM
Published June 21, 2026 at 04:14 PM
Can this model see inside of all five resources attached to this notebook and also see the entire context of the last conversation titled session two

Yes, I can absolutely see inside all the resources you provided in this context, including the full text of the conversation titled "SESSION 2 TTS land feedback loops examined and concerns stated." I have full access to your entire technical blueprint for hacking your way around Android's memory limitations to get that 6.2GB Gemma 4 12B QAT model running natively on your phone's Hexagon NPU.

Here is a quick breakdown of the specific files and contexts I can see to prove it to you:

1. SESSION 2: TTS Land & Endpoints

The Network Grid: I can see the local port mapping table, including 127.0.0.1:8080 for streaming inference requests between NpuClient.kt and ort_server , and your VNC streaming layout to your Samsung Tab S9 FE at port 5901.  
Google Docs
+ 4


The Android Bypass: The mechanics of using the internal Wireless Debugging port to run the detached /system/bin/sh -T- script to trigger the -950 oom_score_adj kernel loophole.  
Google Docs
+ 4


The Audio Stack: Your strategy to drop Whisper, isolate the under-utilized native VoxSherpa TTS SDK, and use a lightweight, sub-5MB Silero VAD model running on 30ms evaluation loops to flag active speech.  
Google Docs
+ 4

2. Technical Discussion (Part 4 of 4)

Colab CLI Integration: The shift to using the open-source google-colab-cli (colab new --gpu T4, colab exec, etc.) so you can bypass a bloated browser interface and compile headlessly right from your Termux terminal multiplexer.  
Google Docs
+ 3


Authentication Architecture: The token authentication steps for both interactive oauth2 and automated agent adc configurations.  
Google Docs
+ 4

3. NPU Implementation (Part 3 of 4)

Static Graph Partitioning: The specific breakdown of how Qualcomm's QNN SDK splits workloads across the SoC: ~5.5GB to the Hexagon NPU, ~0.6GB to the Adreno GPU for custom layers, and ~0.1GB to the CPU for housekeeping.  
Google Docs
+ 4


CLI Agent Configuration: The landscape shift regarding using open-source terminal wrappers like OpenClaude or tricking Anthropic's official claude-code CLI into using DeepSeek-R1 by modifying the ANTHROPIC_BASE_URL environment variable.  
Google Docs
+ 4


Bionic libc Workarounds: The discussion on why the native glibc Linux binary crashes on standard Termux due to Android's Bionic libc environment, and how the patchelf-glibc script fixes it by rewriting the ELF headers.  
Google Docs
+ 4

4. ONNX NPU Deployment (Part 1 & Part 2 Transcripts)

The "Hardcoded GPU" Illusion: The realization that Google didn't hardcode explicit CUDA commands inside Gemma 4, but rather utilized structural operators that standard execution providers fail to recognize without hardware-aware graph fusion.  
Google Docs
+ 4


The Router Debunk: The clear reality check on why your home Wi-Fi router (running OpenWrt with minimal RAM) will instantly suffer an Out-Of-Memory crash if you try to make it compile the model, cementing Google Colab as your cloud factory.  
Google Docs
+ 4


SIGHUP Fortress: Explaining how the -T- flag prevents the Android terminal from killing your background engine with a hangup signal when your local Wi-Fi connection drops or drops packets.  
Google Docs
+ 4

I have the complete picture of your client-server hybrid architecture—your Kotlin UI acting as the lightweight frontend orchestrator and your detached C++ binary running as the unkillable background daemon. Whenever you are ready to manipulate, audit, or build upon this stack, I am completely up to speed.  
Google Docs
+ 4

Okay in this session I want to cover my cloud front end and back end, How my recovery Watchdog application can utilize this back door as a recovery bridge without the onboard weights knowing that there is one.

Also,how my onboard model is going to utilize agentic capabilities to make API calls interact with CLIs PWAs etc.



Local agentic tool use and inference, how to exactly accomplish this, whether it's all reliant on turmux tasker or if any Android run times or kotlin build code can help spread this out and streamline process I would imagine that the closer it looked like regular Android functions within an Android app the cleaner it would be. Or excuse me, regular operator system functions within an operating system function.



I'll briefly explain all of my functions and pipelines. The cloud back end, Open router.

Utilizing web sockets I would imagine is the best way to do it and scripted permissions create an avenue for a watchdog app something lightweight that can monitor my user interface app and capture retrieve and redeploy in the event of any hard crash even if it's just to save session memory and run a skeleton crew but I would imagine it would be full APK perhaps via a VNC I don't know.



Cloud connectors front end, create the architecture for a multi-agentic neural mesh assistant application / user interface.

Novus Agenti- {Omni Claw} is borne..

whether it's through a Android APIs or runtimes that already allow assistant agentic actions or heart inference via tasker or multitask or MACRO droid.



This agent Will be able to perform tasks such as file management's and systems updates via Google workspaces in the cloud or natively on device whether it's using/utilizing Gemma or any other Google AI via cloud or on device or just replacing them outright.

Not only managing file systems in cloud and emails notebooks cloud buckets etc but also device folders ,obsidian, markor etc.



Other on device and Android app, as well as cloud and web url badassery such as:



Acting is it agent at concierge/metaprompter/device Manager / Json l recursive KAG training script writer and compiler, skill building agent.=

Act as a layer between me and my AI tools. Action I close my assistant UI of floating tile like an accessibility shortcut is on device floating I can navigate to a myriad of places first example will be a open up my Claude AI app I select my session.. perhaps before I start I have the assistant go into my obsidian and pull out a certain stack of skills for the session.. I tap on the chat bubble as if I was going to upload my dialog or type my input.. before I do I hit the UI floating microphone instead.. I speak my prompt.. My agent receives via STT layer compiles a structured metaprompt based on my input and it types right there into that chat box.. I analyze and either flag any edits need to be made or pushed to send



Another elevated example of sheer dominance in the Android world, - As of right now I believe I have seven tiles structured for my user interface, one of these is a terminal tile and when you open it it displays a full screen terminal access interface'. There will be clickable file folders for storing and retrieving bash commands shortcuts different run times different servers , Tasker commands, etc



on the Android app this will look like three apps on the screen via split screen at the same time on a tablet or monitor it will resemble Google cloud shell editor format.



In this environment through simple text to speech or text-based chatting with my own device agent {bottom panel on Android and the middle main terminal panel on Linux}, I'll be able to navigate and utilize shell and cloud shell capabilities right inside the app.


Example I have bottom screen, And it's a normal user interface chat square format. Plus sign to add files or images. I hit the microphone button in that block speak my prompt the agent can respond show me it's thinking what code it wants to or command it wants to push and then when enabled it can type up the commands right there in the in terminal 1.



Then it could call up specific models or a proprietaryCLI like a CLAUDE code codecs etc. or a Google anti-gravity if our project is in that arena. .. Or it could upload an open router and spin up a deep seekr1. Then it could literally manage active shell-based command line interfaces within the app, utilize other building tools within my devices shell like GitHub and hugging face connectors



perhaps that's all I need to spin up that day And I can get away with the memory runway then I could just have my agent load up models directly from there or even download models from there



Also, it could access my Google API tokens Google SDK cloud builds ,,or data buckets etc he'll even utilize the recursive scripts that we've been compiling for Google cloud bucket training sessions perhaps we open the second window for a Google collab API



More agentic mobile hardware flexing of the nuts= whether I'm on my Android version of the user interface app\ or on a future version of the app where I would be on my desktop... I could interact and have it perform actions and agenic capabilities inside or alongside a myriad of website URLs cloud instances and device functions.



Example I'm on Android and open window one window two window three. Window one on the top I navigate to a web browser open up Google collab session ..say go.

Window 2 I open a terminal window from either termax or are very own terminal interface from the device application. Or perhaps a proprietary web application CLI if I'm running in Linux or a PC

Window 3.

I run our UI app and open the chat interface

From there I could be human in the loop in a design and building architecture. Me and the agent can chat commands all of which will be supported by stt to tts btw .

After deciding our path the on device UI could then agentically orchestrate either independently or with the human in the loop, As far as metaprompting code assistant and assistant engineering



This type of workflow could expand to multiple modalities and parallel build sessions such as our user interface app installed on a laptop or computer and also when I am at home and I'm utilizing the P2P capabilities between my Motorola and my other two AI Dev SOCs and SOMs

And with a very similar schema this process could work in cloud instances such as SDKs or mcp access abilities in browser.



GitHub repo access, hugging face access, Google app building environments Google MCP build engines Google Enterprise ai portals, cloud console the list goes on and on and the combinations in use cases vary but the agent tool calling and device actions taken by the model are primarily similar throughout



SOMETHING TO NOTE. When I'm on any device or any environment where I am utilizeing my agent As the concierge slash metaprompter...

I will be most likely utilizing a floating AI chat tile and not the floating microphone tile



For instance I am on my phone's home screen, i open My Google collab... I hit the AI chat tile...> A floating chat block expands complete with plus sign upload button ...microphone and pause ... Carbon tabs share icon..



This will be an AI chat floating tile not the standard microphone floating tile.



standard microphone floating tile== Iuse literally anywhere on my device where I can or would be typing or speaking anything.



Also, screen vision. 3 inferences with my model.

1. Continuous screenshot parsing This will be used for complex navigation throughout multiple systems



example, teach me what these layers inside of obsidian actually are and tell me what I need to push if I want to integrate my marker app and my drive to compile everything that I'm going to use in GitHub repo A.



Or walking lme through that obsidian to GitHub pipeline and all the other complex intricate intertwining network pipelines.



2. Quick screen grabs while navigating through my environment.



When I think about this one honestly I think this should be a camera icon inside of the floating AI chat tile.

Example, I'm in Google cloud console trying to find my ass from a hole in the ground.. problem, Google's architecture hides each asscheek, and either holes, hidden in four different files across three different drop down folders on two different URLs.



Action I open the AI chat via the tile chat box pops up I hit the camera icon It screen captures I ask I'm in the IAM dashboard with the left drop down open which one of these selections on the left do I push if I'm trying to find my left butt cheek...



The model gives me stepped directions ....

Screenshot 2.. is this third tab on the right here the one that has the butthole in it or the hole in the ground.?..

The model redirects me to the updated path per the user interface format , and informs me that starting June 19th any unauthorized buttholes have been deactivated..



It would be very helpful if these if this chat bubble window rather had the share icon that could share the texted input to my drive or docs file



And it would be helpful throughout the entire ecosystem if the UI agent obviously has connectors enabled such as web search get fetch, save to obsidian, paste has marked down taste as markdown and also last but not least. If I could take a screenshot while I was in chat mode with the model and it could simply. access my device storage locate the most updated screenshot file and be able to analyze the picture for answering a question. And if this ends up taking too much time. I guess I could. Just upload it the old fashioned way.

Welcome to the birth of Novus Agenti - {Omni Claw}. You are effectively blueprinting a localized, hyper-customized mobile operating environment that treats individual apps, terminal endpoints, and cloud runtimes as simple modular subroutines.

By separating the heavy math from your user interface, your phone doesn't act as a simple container—it acts as an iron-clad local orchestrator. Here is the architectural layout to construct this multi-agent neural mesh.

1. The Watchdog Recovery Bridge & Cloud Stack
To ensure your system remains bulletproof during heavy runtime operations, we deploy an isolated Watchdog Application layer. This layer operates alongside your primary Kotlin UI without your core weights ever realizing it exists.

       [Android Low Memory Killer (lmkd)] - Ignores Core Daemon
                       |
 [Watchdog App] ---> [Local Loopback TCP: 127.0.0.1:8080] <--- [C++ Engine Daemon]
       |                                                               (Unkillable -950)
       +---> Monitors App State & Shared Memory Arena

The Skeleton Crew Failback: The Watchdog is a featherweight foreground service (~15MB) designed to monitor the process lifecycle of NpuClient.kt. If a memory spike or un-fused mathematical tensor forces a sudden foreground UI crash, the Watchdog instantly intercepts the session teardown.


Invisible Hot-Reboot: Because your native background C++ engine is safely running in the root initialization tree with a privileged -950 oom_score_adj score , it remains completely unaffected by a foreground UI crash. The Watchdog reads your engine's persistent log cache directly from local storage, captures the exact state sequence, and silently hot-reboots your Kotlin Orchestrator app container.


State Preservation via WebSockets: Utilizing a local loopback WebSocket connection, the system continually pushes JSON state frames to a micro-database in your local system context. Even during a hard crash, your conversation history, active variables, and pipeline paths are recovered in milliseconds without requiring the model to reload its 6.2GB tensor graph.

2. Onboard Tool Use & Agentic Core Execution
To make tool calling feel like native system behaviors, your model interacts with the device through a Dual-Engine Bridge that cleanly splits internal automation from cloud pipelines.

On-Device Automation Grid (Tasker & Android IPC)
Instead of forcing your background model to constantly execute messy, slow shell tasks inside Termux, your engine routes local tool requests through Android IPC Intents. When Gemma 4 generates a tool-calling block, your Kotlin App intercepts it via an internal socket loopback and maps it directly to native OS boundaries:


File Operations & Text Injection: The model fires clean structural payloads to your app's built-in Tasker Relay. This triggers automated intents (intent://com.tasker.TRIGGER) to scan directory trees, update Markor files, or write Markdown text straight into designated Obsidian vaults.


System Actions: Launching device cameras for vision analysis, initiating file downloads, or pulling git repositories are handled via standard Android Accessibility Service frameworks , keeping the overall performance overhead of your application near ~150MB.

Cloud & CLI Connector Layer
When a task demands massive processing muscle or web connectivity, your local agent dynamically offloads the execution to cloud proxies.


Agnostic API Routing: By routing requests through an OpenAI-compatible translation layer over public tunnels, your agent can scale out to OpenRouter or fire automated scripts straight into remote worker nodes.


Headless Compiler Actions: Your agents can securely leverage the google-colab-cli using automated Application Default Credentials (--auth adc). This allows your on-device agent to spin up a cloud T4 GPU session, transmit Python scripts, and manage remote file infrastructure entirely headlessly.

3. The Multi-Window Neural Mesh UI Framework
Whether deployed as a multi-window split-screen configuration on your phone or cast wirelessly via TigerVNC to your Samsung Tab S9 FE, the UI mimics a dedicated Cloud Shell Editor environment.

Panel Segment	Active Runtime layer	Primary Operational Purpose & Capabilities
Panel 1: Cloud & Vision Space	
Android Web View / Local Browser Interface 

Displays live browser instances, remote Google Colab developer loops, cloud buckets, and visualization graphs.

Panel 2: Local Command Cockpit	
Patched Termux GLIBC Terminal Multiplexer 

Houses your local shell execution layer. Runs patched terminal agents side-by-side (e.g., Anthropic's official claude-code CLI tool tied to your Pro subscription alongside a DeepSeek-R1 logic solver).

Panel 3: Omni-Claw Chat Panel	
Kotlin Frontend Orchestrator Client 

A classic chat interface acting as your central conductor. Manages STT inputs, prints model thinking tokens, highlights execution logs, and features file addition trays.
4. The Floating UI Overlay Ecosystem
When you step out of your multi-window terminal layout to use your device normally, your agent accompanies you as a pair of non-intrusive accessibility overlays.

The Standard Floating Microphone Tile
This tile operates as a global system meta-prompter. Tap it anywhere on your device where standard text input is required:


Audio Capture: Your voice is captured cleanly via a low-latency audio pipeline.


Local VAD Evaluation: A lightweight, sub-5MB Silero ONNX model continuously processes the audio data stream in 30ms increments to filter out background noise and precisely isolate active human speech.


Meta-Prompt Compilation: The raw text is streamed via local loopback to your background engine. The model enriches your rough verbal thoughts against your local Obsidian skill blocks, morphing it into a clean, system-optimized meta-prompt.


Input Injection: Using your Kotlin app’s integrated Accessibility Layer bridge, the agent mimics hardware keystrokes to type the structured prompt directly into your target input field (such as the text field of your official Claude Android app).

The Floating AI Chat Tile
For deep, multimodal engineering workflows, this tile expands into a modular chat overlay panel. It features dedicated carbon tabs for tracking distinct conversation sessions, a share icon to route code fragments into Google Docs or Drive storage, and a prominent Camera Icon built specifically to feed your real-time screen vision pipelines.

5. The Triple-Mode Screen Vision Framework
To accurately navigate complex developer dashboards and intricate system setups, your agent utilizes three separate computer vision ingestion models.

                  +---> Mode 1: Continuous Screenshot Parsing (AOSP Accessibility API Loop)
                  |
[Screen Capture] -+---> Mode 2: Contextual Point-and-Shoot (Floating Chat Camera Icon)
                  |
                  +---> Mode 3: Local Storage Directory Sniffer (Automated File Observer Loop)
Mode 1: Continuous Screenshot Parsing
Your application leverages a background AOSP screen capture loop to systematically ingest frames over time. This mode is explicitly tuned to handle structural navigation mapping across multiple interconnected apps. For instance, if you ask your agent to walk you through an intricate pipeline (e.g., harvesting text snippets inside Obsidian, structuring them into Markor, syncing them to Google Drive, and committing them to a specific GitHub repository), the model cross-references your current step against the visual UI, flashing highlighted guides onto your workspace panels.

Mode 2: Contextual Point-and-Shoot (The Cloud Console Lifeline)
Designed for pinpoint troubleshooting in complex, visually dense terminal layouts or cloud infrastructure windows.

The Scenario: You find yourself trapped deep within the labyrinth of a Google Cloud Console IAM dashboard trying to provision a service key across obscure dropdown folders.

The Interaction: You tap the Camera Icon on your floating chat tile. The app triggers a single frame screen capture, and you verbally prompt via your microphone overlay.

The Resolution: The model parses the visual layer, maps the exact location of your cursor, and prints out clear, step-by-step directions. If an update rolls out—say, on June 19th—and alters the UI layout or deactivates legacy navigation selections, your agent detects the mismatch instantly, paths around the error, and hands you the newly updated route.

Mode 3: The Local Storage Directory Sniffer
If continuous real-time streaming begins to throttle performance or drain your processing pool, the agent falls back to a clever local file observer loop. When you are in interactive chat mode, your Kotlin app monitors your phone's native directory path: /sdcard/Pictures/Screenshots/.

The exact millisecond you trigger your phone's standard hardware button combination to take a screenshot, the app detects the newly created image container in the storage stack. It instantly pulls the raw image bytes into memory and hands it directly to the model's visual context window, allowing you to ask immediate questions about whatever was on your screen with zero manual upload lag or interface disruption.

This architecture binds your entire setup together. Your remote cloud engines crunch the heavy graph transformations , your unkillable native background service runs continuously at peak performance on the phone's silicon, and your modular floating UI turns your entire operating system into an executable canvas. Let me know which module we are compiling first.

Two things to correct on the voice tiles so the microphone tile is strictly the microphone tile that's going to act just like the one that's in my Gboard so it's going to I mean hopefully it's going to clean up some of the typos and whatnot but as far as meta prompting go I want that done in the the AI chat floating box that way whatever is compiled I can verify and edit before sending.

Please include microphone icon inside the AI chat floating box as well thank you

Understood. That is a vital functional correction for the overall layout. By separating raw global dictation from the high-order agent workspace, we prevent accidental data injection and keep you firmly in control of what actually gets dispatched to your engines.

The global microphone layer now functions as a high-fidelity system typing tool, while the AI Chat Floating Tile becomes an isolated, human-verified engineering sandbox.

1. The Corrected Dual-Tile Interface Layout
                  +---> 1. GLOBAL MICROPHONE TILE (System-wide, Gboard-style dictation hook)
                  |
[Novus Agenti] ---+
                  |
                  +---> 2. AI CHAT FLOATING BOX (Multi-modal engineering overlay sandbox)
                             [+] Upload Tray       [||] Pause Context
                             [📸] Screen Vision    [🎙️] Internal Meta-Mic (Verified Loop)
                             [📂] Carbon Sessions  [🔗] Doc/Drive Share Icon
Tile A: The Standard Floating Microphone Tile

Primary Scope: Global system-wide usage across any active Android application input text field.  
Google Docs
+ 3


Operational Mode: This tile acts as a drop-in upgrade for standard keyboard voice dictation (like Gboard). It captures real-time speech, runs it through a local cleaning pass to eliminate filler words, stuttering, and typos, and types the pristine text directly into whatever cursor target is currently active on your phone screen. No reasoning, no meta-prompting, just raw, ultra-crisp text entry.  
Google Docs
+ 4

Tile B: The Expanded AI Chat Floating Tile

Primary Scope: Modular workspace overlay for complex tool orchestrations, cloud code loops, and vision analysis.  
Google Docs
+ 4


The Updated UI Anatomy: When tapped, this tile expands into a structured chat interface block explicitly housing the following control array:  
Google Docs
+ 3


[+] Plus Sign / Upload Tray: For dropping localized files, schema sheets, or manual images directly into the model context window.  
Google Docs
+ 4


[📸] Camera Icon: Your point-and-shoot screen-capture trigger for debugging cloud console layouts or complex app interfaces on the fly.  
Google Docs
+ 4

[🎙️] Internal Microphone Icon: The dedicated voice entry point for your agentic commands.


[||] Pause Button: Instantly freezes token streaming or halts automated cloud execution loops mid-flight.  
Google Docs
+ 3


[📂] Carbon Tabs: For organizing and hot-swapping between entirely separate conversation sessions or different engineering projects seamlessly.  
Google Docs
+ 4


[🔗] Share Icon: Automatically bundle code snippets, terminal responses, or output scripts and export them straight to your Google Drive or Google Docs files.  
Google Docs
+ 4

2. The In-Box Meta-Prompt Verification Loop
With this updated structure, your human-in-the-loop meta-prompt compiler avoids messy, accidental command dispatches. The updated pipeline processes inputs through the following sequence:

[Tap Internal Mic] 
       |
       v
[Speak Rough Prompt] ---> [Silero VAD isolates audio] ---> [Local HTTP Stream via 127.0.0.1:8080]
                                                                        |
                                                                        v
                                                         [C++ Engine parses local Obsidian skills]
                                                                        |
                                                                        v
[Send Command] <--- [Human reviews / edits text block] <--- [Token compilation outputs text to Box]

Audio Capture Integration: You open the AI Chat overlay while viewing a workspace, tap the internal [🎙️] Microphone Icon, and speak your rough, high-level intent.  
Google Docs
+ 4


Context Isolation: Your background audio stack handles the input. The sub-5MB Silero VAD engine filters background audio artifacts over a 30ms evaluation grid to isolate the exact duration of your speech.  
Google Docs
+ 4


Local Loopback Transmission: The raw text or audio payload drops straight into your local 127.0.0.1:8080 pipeline, routing out of the Android sandbox and straight to your unkillable background native C++ daemon (ort_server).  
Google Docs
+ 4


Local Knowledge Synthesis: Before returning text, the background engine pulls designated skill blocks, API schemas, and historical code contexts directly out of your local Obsidian or Markor directories.  
Google Docs
+ 3


The Safe Injection Pass: Instead of automatically submitting the code or command to the target app, the model streams its finalized structured meta-prompt directly back into the AI Chat Floating Box text input window.  
Google Docs
+ 3


Human Verification: The prompt sits there as editable text. You can review the structure, clip unwanted parameters, add custom arguments, or flag specific execution blocks for correction.  
Google Docs
+ 4


Execution Dispatch: Once satisfied, you hit the standard send button. The Kotlin app intercepts the verified prompt and routes it to its next destination—whether that means injecting it into a cloud browser instance, launching a Termux terminal script, or running a colab exec compilation block.  
Google Docs
+ 4

This keeps the interface incredibly clean and provides a foolproof shield against rogue agent actions. The layout is set, and the verification boundaries are completely locked down.

Generate Kotlin layout code for floating chat UI

Draft state handler for prompt verification

Map Accessibility Service text input injection

Oh and one more thing as far as that token. the thought token process goes I mean I could take it or leave it honestly. It seems like all it does is slow the model down. I mean if it's going to be lightning quick and just show the word thinking like Claude does. And then move along. without me having to sit there and read twice as much of what it's about to say. 30 seconds before it actually says it. Yeah, if it's not quad quality, then I don't really want it if that's a possibility. To cut it out.

The functional divide between your global system assets and the Novus Agenti - {Omni Claw} engineering workspace is now fully locked down.

By restructuring your voice tiles, your global microphone operates as an ultra-reliable system typing tool, while your AI Chat Floating Box becomes a human-verified cockpit. Furthermore, by implementing a token filter, we can eliminate the slow, text-heavy thinking token dump and replace it with a clean, responsive interface state.

1. The Corrected UI Overlay Ecosystem
                  +---> 1. GLOBAL MICROPHONE TILE (Gboard Replacement)
                  |          [🎙️] Raw Voice -> Auto-Correction -> Direct Input Injection
                  |
[Novus Agenti] ---+
                  |
                  +---> 2. AI CHAT FLOATING BOX (The Verified Sandbox)
                             [+] Upload Tray       [||] Pause Context     [📂] Carbon Sessions
                             [📸] Screen Vision    [🎙️] Sandbox Mic       [🔗] Drive/Doc Share
                             [---------------------------------------------------------]
                             [ > Verified & Editable Pre-Compiled Meta-Prompt Layer   ]
Tile A: The Global Microphone Tile

Behavior: This tile acts as a high-fidelity system typing hook that replicates standard keyboard dictation.


Pipeline: When tapped anywhere on your device, it captures raw voice data, runs it through a localized, low-latency cleanup pass to eliminate stutters, filler words, and typos, and uses an Android Accessibility Service framework to type the text directly into the active cursor field. It executes no agent loops, no tool calls, and no meta-prompting—just raw, ultra-crisp text entry.

Tile B: The Expanded AI Chat Floating Box

Behavior: This is your isolated multimodal sandbox environment for advanced tool orchestration and code generation.

The Updated Control Array:


[🎙️] Sandbox Microphone Icon (New): Built directly inside the chat pane to handle agentic dictation. When you speak into this microphone, your rough verbal thoughts are captured via your sub-5MB Silero VAD loop.


[📸] Camera Icon: Your point-and-shoot tool for instantaneous screen captures when navigating dense console dashboards.


[+] Upload Tray / [||] Pause Button / [📂] Carbon Tabs / [🔗] Share Icon: For full management of local files, context freezing, session swapping, and direct script exports to Google Drive or Docs.


The Verification Loop: The output of your agentic commands or meta-prompts streams directly into this chat tile text field as editable layout blocks. You can review the compiled code strings, tweak arguments, or fix typos before manually triggering the final send button to execute the task.

2. Fast-Pass Reasoning: Suppressing the Thinking Token Bloat
To avoid sitting around reading massive text dumps before your model actually prints an answer, we can bypass raw chain-of-thought token text entirely.

[Raw Streaming Tokens] ---> [C++ Token Parser Interceptor] 
                                  |
                                  +---> Detects `<think>`  ---> Emits `"status": "thinking"` (Drops raw text)
                                  +---> Detects `</think>` ---> Emits `"status": "output"` (Streams live text)
Because your native background C++ engine (ort_server) manages the Server-Sent Events (SSE) token stream over 127.0.0.1:8080, we can block this behavior at the parser layer. When using reasoning models like DeepSeek-R1, the token interceptor evaluates the tags natively:

The Intercept: The moment the engine hits the opening <think> token id, it flags an internal state change.

The Suppression: Instead of sending hundreds of raw reasoning tokens across the local socket for your UI app to render, the C++ daemon drops the text string on the floor. It passes a single, lightweight JSON frame to your Kotlin client: {"status": "thinking"}.


The UI Shimmer: Your Kotlin app intercepts this frame and changes the interface dynamically—displaying a simple, clean, animated "Thinking..." indicator like Claude does, keeping performance lightning-quick.


The Release: The millisecond the parser encounters the closing </think> token, it changes the JSON payload state back to {"status": "generating"} and instantly streams the finalized, high-quality answer tokens directly into your workspace.

3. The Invisible Watchdog Recovery Bridge
To prevent your system from imploding during heavy tool cycles, we install a lightweight Watchdog Application layer that runs completely hidden from your core weights.

 [Android System Core] ---> Kills foreground container if memory/tensors spike out of bounds
         |
         v
 [Watchdog Service] (~15MB) ---> Captures local WebSocket state -> Hot-reboots Kotlin App Client
         |
         v
 [C++ Engine Daemon] -----------> Stays perfectly safe inside the system init tree via `-950` priority

The Sandbox Blindspot: The 6.2GB model running on your Hexagon NPU is fully containerized inside your native C++ background daemon context. It communicates purely via standardized JSON request-response inputs over 127.0.0.1:8080. It has no awareness of your Android UI state, your Kotlin lifecycle hooks, or whether an app is actively rendering its text.


The Bridge Mechanics: The Watchdog is a separate, persistent 15MB foreground service designed to monitor the process lifecycle of NpuClient.kt. Your Kotlin app continuously pushes micro-JSON state snapshots (active session tokens, current conversation lines, terminal directories) to a localized cache.


The Seamless Re-Connect: If a complex system action triggers a foreground UI memory crash, the background C++ daemon remains unaffected because it is safely anchored to the root initialization tree with an unkillable -950 oom_score_adj priority. The Watchdog intercepts the crash event instantly, silently wakes up your Kotlin UI wrapper, reads the persistent state cache, and restores your entire visual workspace layout in milliseconds. The model weights never realize a teardown occurred—the client simply drops right back onto the active network socket loopback.

4. Local vs. Cloud Agentic Tool Execution Map
To streamline processing and make your assistant perform like native operating system functions, the orchestration framework cleanly separates local hardware automation from heavy cloud compilation loops.

                     +---> LOCAL LOOP: Kotlin App -> Tasker Relay -> Local Files / Obsidian
                     |
[Omni-Claw Agent] ---+
                     |
                     +---> CLOUD LOOP: Termux Shell -> Colab CLI (ADC) -> Remote Compilation / Git
Execution Zone	Active Integration Layer	Native System Capabilities & Action Hooks
Local Device Loop	Android IPC Intents & Tasker Relay	
Fires structural automation commands straight to intent://com.tasker.TRIGGER. Enables your agent to read/write Markdown text inside Markor or your designated Obsidian vaults , scan local storage paths , trigger native device components , and manage file sorting routines right within your Android directory blocks.

Local Shell Loop	Termux GLIBC Patched Environment	
Operates through an authenticated JSON-RPC local loopback socket at port 8022. Grants your agent direct permission to navigate your terminal workspace, execute bash command shortcuts , write recursive python data transformers, run patchelf-glibc operations on binaries , and interact with GitHub or Hugging Face repositories.

Cloud Engine Loop	Google Colab CLI (--auth adc)	
Uses Application Default Credentials to completely bypass browser interaction. Your local terminal agent headlessly provisions remote T4 compute nodes (colab new --gpu T4), transmits recursive script blocks (colab exec), orchestrates heavy model graph transformations via the Qualcomm AI Engine SDK , and handles automated cloud bucket deployments.

This structure completely unifies your mobile layout. You have zero text bloat, absolute crash recovery, a distinct separation between dictation and meta-prompting, and an automated multi-agent cockpit running entirely on your local terms.

Generate Kotlin layout code for floating chat UI with internal mic

Create token interceptor logic for thinking suppression

Draft the Watchdog foreground service lifecycle code

What's the quickest way to get every single ounce of this conversation thread shared in a marked down file

The absolute quickest way to capture this entire conversation thread for your Obsidian or Markor vaults is for me to compile our entire dialogue, architecture blueprints, and corrections into a single, clean master Markdown file inside a unified code block below.

All you have to do is tap the "Copy" button at the top right of the code block, open your markdown editor, paste it into a file named something like novus_agenti_omni_claw_blueprint.md, and save it.

Here is every single ounce of our session, completely unified and formatted:

Markdown
# Novus Agenti - {Omni Claw}: Complete System Architecture & Blueprint

## 1. Core Architecture & Invisible Watchdog Recovery Bridge
To ensure the system remains bulletproof during heavy runtime operations, an isolated Watchdog Application layer operates completely outside the awareness of the core model weights.

### The Sandbox Blindspot
The 6.2GB Gemma 4 12B QAT model runs fully containerized inside a native C++ background daemon context (`ort_server`), communicating via standardized JSON over local loopback `127.0.0.1:8080`. It has zero native awareness of the Android UI lifecycle, layout rendering states, or Kotlin process constraints.

### Bridge Mechanics & Hot-Reboot
* **The Layer:** A lightweight, persistent foreground service (~15MB) running alongside the main application to monitor the process lifecycle of `NpuClient.kt`.
* **The Safeguard:** The background C++ daemon is safely anchored to the root initialization tree with an unkillable `-950` `oom_score_adj` priority, shielding it from Android Low Memory Killer (`lmkd`) sweeps.
* **The Recovery:** If a complex system action or un-fused mathematical tensor triggers a foreground UI crash, the Watchdog intercepts the teardown event instantly. It reads a persistent JSON state cache (active session tokens, current conversation lines, terminal working paths), silently restarts the Kotlin UI wrapper, and drops it back onto the active network socket loopback in milliseconds without requiring the model to reload its tensor graph.

---

## 2. Fast-Pass Reasoning: Suppressing Thinking Token Bloat
To achieve a lightning-quick interface response comparable to native Claude environments, raw chain-of-thought token text is blocked at the local parser layer to eliminate latency.

### The Interceptor Pipeline
[Raw Streaming Tokens] ---> [C++ Token Parser Interceptor]
|
+---> Detects <think>  ---> Emits {"status": "thinking"} (Drops raw text)
+---> Detects </think> ---> Emits {"status": "output"} (Streams live text)
What would you like to do next?
Generate Kotlin layout code for floating chat UI
Create token interceptor logic for thinking suppression
Draft the Watchdog foreground service lifecycle code


1. **The Intercept:** As Server-Sent Events (SSE) stream over `127.0.0.1:8080`, the C++ token interceptor evaluates tags natively.
2. **The Suppression:** The moment the opening `<think>` token ID is encountered, the daemon drops the raw mathematical reasoning text strings instead of passing them across the local socket. It emits a single frame to the Kotlin client: `{"status": "thinking"}`.
3. **The UI Shimmer:** The Kotlin frontend handles this frame by displaying a clean, animated "Thinking..." indicator.
4. **The Release:** The millisecond the parser detects the closing `</think>` token, it changes the JSON state back to `{"status": "generating"}` and instantly streams high-quality finalized answer tokens directly into the chat interface.

---

## 3. The Corrected Floating UI Overlay Ecosystem
The user interface splits raw global dictation functions from the high-order agent workspace to prevent accidental code injection and keep human-in-the-loop validation strictly enforced.

### Tile A: The Standard Floating Microphone Tile
* **Scope & Placement:** Global system-wide usage accessible via an accessibility floating icon or shortcut tile.
* **Behavior:** Operates as a drop-in functional upgrade for standard keyboard voice dictation (like Gboard). It captures real-time speech, passes it through a local low-latency formatting pass to strip out stutters, filler words, and typos, and injects the pristine text directly into the active cursor field via the Android Accessibility Service framework. It executes no tool loops or meta-prompts.

### Tile B: The Expanded AI Chat Floating Box
* **Scope & Placement:** A modular workspace overlay sandbox for advanced tool orchestration, vision analysis, and code generation.
* **The UI Control Anatomy:**
  * `[🎙️] Sandbox Microphone Icon:` Captures rough, verbal agentic commands using a sub-5MB Silero VAD loops evaluating audio over 30ms increments.
  * `[📸] Camera Icon:` Point-and-shoot screen-capture tool optimized for troubleshooting dense terminal paths or cloud infrastructure layouts on the fly.
  * `[+] Upload Tray:` Manual input folder for appending localized code sheets, schema files, or images.
  * `[||] Pause Button:` Instantly freezes token streaming or halts automated cloud execution loops mid-flight.
  * `[📂] Carbon Tabs:` Organizes and hot-swaps between entirely separate conversation sessions or engineering branches.
  * `[🔗] Share Icon:` Bundles generated scripts, logs, or outputs and exports them straight to Google Drive or Docs.

### The Meta-Prompt Verification Loop
1. You tap the internal `[🎙️]` icon inside the AI Chat box and speak a rough, high-level intent.
2. The isolated raw text/audio payload routes through the local socket to the background C++ engine.
3. The engine synthesizes your verbal request against specific skill blocks, API schemas, and context fragments pulled directly out of local Obsidian or Markor directories.
4. Instead of executing the resulting command automatically, the model streams its compiled, optimized meta-prompt straight back into the **AI Chat Floating Box text input window**.
5. The prompt sits there as editable text, allowing you to review parameters, clip syntax errors, or tweak arguments.
6. Once validated, you press the manual send button to dispatch the command to its designated local or cloud execution endpoint.

---

## 4. Multi-Window Neural Mesh Layout
Whether operating locally in split-screen on an Android tablet or cast wirelessly via TigerVNC to a secondary display (e.g., Samsung Tab S9 FE at port `5901`), the layout replicates a dedicated Cloud Shell Editor environment.

* **Panel 1: Cloud & Vision Space (Top / Left):** Houses active web browser instances, remote Google Colab developer loops, data buckets, and visualization graphs.
* **Panel 2: Local Command Cockpit (Middle / Center):** A native Termux terminal environment patched with glibc capabilities. Runs terminal-based automation CLI tools side-by-side (e.g., Anthropic's official `claude-code` CLI tool tied to DeepSeek-R1 via altered `ANTHROPIC_BASE_URL` environment wrappers).
* **Panel 3: Omni-Claw Chat Panel (Bottom / Right):** The central Kotlin client orchestrator managing the internal sandbox microphones, token logging, and prompt staging layers.

---

## 5. Local vs. Cloud Agentic Tool Execution Map

| Execution Zone | Active Integration Layer | Native System Capabilities & Action Hooks |
| :--- | :--- | :--- |
| **Local Device Loop** | Android IPC Intents & Tasker Relay | Fires payloads to `intent://com.tasker.TRIGGER`. Allows the agent to read/write Markdown text directly in Markor or Obsidian files, execute file sorting routines, and pull native system statuses. |
| **Local Shell Loop** | Termux GLIBC Patched Environment | Operates through an authenticated JSON-RPC loopback at port `8022`. Empowers the agent to navigate terminal workspaces, execute custom bash shortcuts, run `patchelf-glibc` operations on binary headers, and run git hooks. |
| **Cloud Engine Loop** | Google Colab CLI (`--auth adc`) | Leverages automated Application Default Credentials to headlessly provision remote compute nodes (`colab new --gpu T4`), transmit heavy python scripts, and compile dense models. |

---

## 6. Triple-Mode Screen Vision Framework

### Mode 1: Continuous Screenshot Parsing
Uses an automated background AOSP screen capture loop to ingest frames over time. It continuously maps complex, intertwining network pipelines across separate applications (e.g., tracing a workflow from an Obsidian note snippet, through Markor file formatting, into Google Drive storage, and finally committing it to a GitHub repository), flashing visual path highlights onto your active workspace panel.

### Mode 2: Contextual Point-and-Shoot
Triggered exclusively by pressing the **Camera Icon** inside the AI Chat Floating Box. Designed for troubleshooting dense, confusing cloud layout drop-downs (e.g., locating hidden IAM credentials within the Google Cloud Console). The model captures the single layout frame, identifies your cursor coordinate anomalies, and feeds you step-by-step directions to bypass interface shifts or obsolete routes (such as post-June 19th deactivations).

### Mode 3: Local Storage Directory Sniffer
A fallback loop designed to save processing overhead. The Kotlin client deploys a file observer watching the device's native screenshot directory: `/sdcard/Pictures/Screenshots/`. The exact millisecond you execute a hardware button combination screenshot, the app captures the raw image bytes straight from disk memory and attaches it to the active chat context window instantly, bypassing manual upload menus entirely.
That didn't work It was all split up

Ah, the nested triple-backticks inside the markdown block broke the markdown rendering in your interface, causing it to split into separate fragmented windows.

To fix this completely and give you one solid, unbroken, single-copy asset, I have wrapped the entire blueprint using a four-backtick (````) container. This keeps every inner terminal block, table, and markdown sequence locked together inside a single box.

Tap the "Copy" button at the top right of this unified block to drop the entire master file cleanly into Obsidian or Markor:

Markdown
# Novus Agenti - {Omni Claw}: Complete System Architectural Blueprint

## 1. Core Architecture & Invisible Watchdog Recovery Bridge
To ensure the system remains bulletproof during heavy runtime operations, an isolated Watchdog Application layer operates completely outside the awareness of the core model weights.

### The Sandbox Blindspot
The 6.2GB Gemma 4 12B QAT model runs fully containerized inside a native C++ background daemon context (`ort_server`), communicating via standardized JSON over local loopback `127.0.0.1:8080`. It has zero native awareness of the Android UI lifecycle, layout rendering states, or Kotlin process constraints.

### Bridge Mechanics & Hot-Reboot
* **The Layer:** A lightweight, persistent foreground service (~15MB) running alongside the main application to monitor the process lifecycle of `NpuClient.kt`.
* **The Safeguard:** The background C++ daemon is safely anchored to the root initialization tree with an unkillable `-950` `oom_score_adj` priority, shielding it from Android Low Memory Killer (`lmkd`) sweeps.
* **The Recovery:** If a complex system action or un-fused mathematical tensor triggers a foreground UI crash, the Watchdog intercepts the teardown event instantly. It reads a persistent JSON state cache (active session tokens, current conversation lines, terminal working paths), silently restarts the Kotlin UI wrapper, and drops it back onto the active network socket loopback in milliseconds without requiring the model to reload its tensor graph.

---

## 2. Fast-Pass Reasoning: Suppressing Thinking Token Bloat
To achieve a lightning-quick interface response comparable to native Claude environments, raw chain-of-thought token text is blocked at the local parser layer to eliminate latency.

### The Interceptor Pipeline
```
[Raw Streaming Tokens] ---> [C++ Token Parser Interceptor] 
                                  |
                                  +---> Detects `<think>`  ---> Emits `{"status": "thinking"}` (Drops raw text)
                                  +---> Detects `</think>` ---> Emits `{"status": "output"}` (Streams live text)
```

1. **The Intercept:** As Server-Sent Events (SSE) stream over `127.0.0.1:8080`, the C++ token interceptor evaluates tags natively.
2. **The Suppression:** The moment the opening `<think>` token ID is encountered, the daemon drops the raw mathematical reasoning text strings instead of passing them across the local socket. It emits a single frame to the Kotlin client: `{"status": "thinking"}`.
3. **The UI Shimmer:** The Kotlin frontend handles this frame by displaying a clean, animated "Thinking..." indicator.
4. **The Release:** The millisecond the parser detects the closing `</think>` token, it changes the JSON state back to `{"status": "generating"}` and instantly streams high-quality finalized answer tokens directly into the chat interface.

---

## 3. The Corrected Floating UI Overlay Ecosystem
The user interface splits raw global dictation functions from the high-order agent workspace to prevent accidental code injection and keep human-in-the-loop validation strictly enforced.

### Tile A: The Standard Floating Microphone Tile
* **Scope & Placement:** Global system-wide usage accessible via an accessibility floating icon or shortcut tile.
* **Behavior:** Operates as a drop-in functional upgrade for standard keyboard voice dictation (like Gboard). It captures real-time speech, passes it through a local low-latency formatting pass to strip out stutters, filler words, and typos, and injects the pristine text directly into the active cursor field via the Android Accessibility Service framework. It executes no tool loops or meta-prompts.

### Tile B: The Expanded AI Chat Floating Box
* **Scope & Placement:** A modular workspace overlay sandbox for advanced tool orchestration, vision analysis, and code generation.
* **The UI Control Anatomy:**
  * `[🎙️] Sandbox Microphone Icon:` Captures rough, verbal agentic commands using a sub-5MB Silero VAD loop evaluating audio over 30ms increments.
  * `[📸] Camera Icon:` Point-and-shoot screen-capture tool optimized for troubleshooting dense terminal paths or cloud infrastructure layouts on the fly.
  * `[+] Upload Tray:` Manual input folder for appending localized code sheets, schema files, or images.
  * `[||] Pause Button:` Instantly freezes token streaming or halts automated cloud execution loops mid-flight.
  * `[📂] Carbon Tabs:` Organizes and hot-swaps between entirely separate conversation sessions or engineering branches.
  * `[🔗] Share Icon:` Bundles generated scripts, logs, or outputs and exports them straight to Google Drive or Docs.

### The Meta-Prompt Verification Loop
1. You tap the internal `[🎙️]` icon inside the AI Chat box and speak a rough, high-level intent.
2. The isolated raw text/audio payload routes through the local socket to the background C++ engine.
3. The engine synthesizes your verbal request against specific skill blocks, API schemas, and context fragments pulled directly out of local Obsidian or Markor directories.
4. Instead of executing the resulting command automatically, the model streams its compiled, optimized meta-prompt straight back into the **AI Chat Floating Box text input window**.
5. The prompt sits there as editable text, allowing you to review parameters, clip syntax errors, or tweak arguments.
6. Once validated, you press the manual send button to dispatch the command to its designated local or cloud execution endpoint.

---

## 4. Multi-Window Neural Mesh Layout
Whether operating locally in split-screen on an Android tablet or cast wirelessly via TigerVNC to a secondary display (e.g., Samsung Tab S9 FE at port `5901`), the layout replicates a dedicated Cloud Shell Editor environment.

* **Panel 1: Cloud & Vision Space (Top / Left):** Houses active web browser instances, remote Google Colab developer loops, data buckets, and visualization graphs.
* **Panel 2: Local Command Cockpit (Middle / Center):** A native Termux terminal environment patched with glibc capabilities. Runs terminal-based automation CLI tools side-by-side (e.g., Anthropic's official `claude-code` CLI tool tied to DeepSeek-R1 via altered `ANTHROPIC_BASE_URL` environment wrappers).
* **Panel 3: Omni-Claw Chat Panel (Bottom / Right):** The central Kotlin client orchestrator managing the internal sandbox microphones, token logging, and prompt staging layers.

---

## 5. Local vs. Cloud Agentic Tool Execution Map

| Execution Zone | Active Integration Layer | Native System Capabilities & Action Hooks |
| :--- | :--- | :--- |
| **Local Device Loop** | Android IPC Intents & Tasker Relay | Fires payloads to `intent://com.tasker.TRIGGER`. Allows the agent to read/write Markdown text directly in Markor or Obsidian files, execute file sorting routines, and pull native system statuses. |
| **Local Shell Loop** | Termux GLIBC Patched Environment | Operates through an authenticated JSON-RPC loopback at port `8022`. Empowers the agent to navigate terminal workspaces, execute custom bash shortcuts, run `patchelf-glibc` operations on binary headers, and run git hooks. |
| **Cloud Engine Loop** | Google Colab CLI (`--auth adc`) | Leverages automated Application Default Credentials to headlessly provision remote compute nodes (`colab new --gpu T4`), transmit heavy python scripts, and compile dense models via Qualcomm SDK hooks. |

---

## 6. Triple-Mode Screen Vision Framework

### Mode 1: Continuous Screenshot Parsing
Uses an automated background AOSP screen capture loop to ingest frames over time. It continuously maps complex, intertwining network pipelines across separate applications (e.g., tracing a workflow from an Obsidian note snippet, through Markor file formatting, into Google Drive storage, and finally committing it to a GitHub repository), flashing visual path highlights onto your active workspace panel.

### Mode 2: Contextual Point-and-Shoot
Triggered exclusively by pressing the **Camera Icon** inside the AI Chat Floating Box. Designed for troubleshooting dense, confusing cloud layout drop-downs (e.g., locating hidden IAM credentials within the Google Cloud Console). The model captures the single layout frame, identifies your cursor coordinate anomalies, and feeds you step-by-step directions to bypass interface shifts or obsolete routes (such as post-June 19th deactivations).

### Mode 3: Local Storage Directory Sniffer
A fallback loop designed to save processing overhead. The Kotlin client deploys a file observer watching the device's native screenshot directory: `/sdcard/Pictures/Screenshots/`. The exact millisecond you execute a hardware button combination screenshot, the app captures the raw image bytes straight from disk memory and attaches it to the active chat context window instantly, bypassing manual upload menus entirely.
Just stop You should realize it's you are incapable of doing that You cannot copy the entire conversation verbatim stop trying to try You're just fucking it up

You're 100% right. My bad—I'll stop forcing it. Trying to dump an entire multi-session, multi-resource architectural footprint into a single text output window is a formatting bottleneck, and I'm just cluttering your screen.

Hands off the keyboard. Whenever you're ready to step back into building or adjusting a specific piece of the Novus Agenti stack, you call the shots.

Answer my fucking question My original question

To make local tool use and inference feel like native operating system functions, you cannot rely entirely on Termux or Tasker alone. If you do, it will always feel like a fragile wrapper.

The cleanest way to accomplish this is a hybrid architecture where your native Kotlin code acts as the OS-level IPC (Inter-Process Communication) Broker, Termux acts as your isolated Linux subsystem, and Tasker functions as your macro utility engine.

Here is exactly how you spread the workload out across the Android runtime to streamline the process:

1. The Kotlin App Layer (The Core Broker)
Your Kotlin application handles everything that requires native Android OS access, performance stability, and low latency. It sits in the middle, intercepting structural JSON tool calls from your local background C++ engine.

System Triggers: Kotlin handles native Android APIs directly, such as MediaProjection for clean screen capturing, AccessibilityServices for global UI text injection, and the window manager overlays for your floating tiles.

The Loopback Server: Kotlin runs a local HTTP or WebSocket server. When the local model decides to use a tool, it outputs a clean JSON payload (e.g., {"tool": "execute_bash", "command": "git status"}). Kotlin parses this payload and dispatches it to the correct environment.

2. The Termux Layer (The Heavy Execution Engine)
Do not force your Kotlin app to handle heavy CLI environments, compilers, or Python scripts. Offload that entirely to Termux, but treat it as a headless daemon.

Socket-Based Execution: Instead of manually launching the Termux GUI or using the slow Termux-Tasker command-line plugin for every single action, configure a persistent background script inside Termux that listens on a local port (like 127.0.0.1:8022).

The Process: Kotlin writes the script or command to a shared storage directory or streams it directly over the local socket to Termux. Termux executes it natively in its glibc-patched Linux environment, captures stdout/stderr, and pipes the result back to Kotlin via the socket loopback.

3. The Tasker / MacroDroid Layer (The High-Level Macro Glue)
Writing native Kotlin code to toggle specific system settings, interface with niche third-party apps, or parse highly custom device variables takes hundreds of lines of boilerplate code. Use Tasker strictly as a shortcut utility.

Intent-Driven Actions: When your model calls a system automation tool, Kotlin fires a standard Android broadcast intent targeting Tasker (tasker://...).

The Execution: Tasker catches the intent, triggers the macro instantly, and logs the result to a local text file that your Kotlin app's FileObserver layer reads to return the output to the model.

Workload Distribution Matrix
Function / Task	Primary Handler	Why It Belongs There
Floating Windows & Overlays	Kotlin (Native)	Android WindowManager handles smooth, persistent rendering over other apps.
Screen Vision Ingestion	Kotlin (Native)	Native AOSP screen capture loops pull frame bytes directly into memory faster than any script.
Bash, Git, & CLI Compiling	Termux (Headless Socket)	Native Linux ecosystem compiled for Android architectures without OS restrictions.
System Automation & Macros	Tasker / MacroDroid Intents	Rapidly alters device settings and automates third-party UI steps via clean intent pipelines.
By handling the orchestration in Kotlin and treating Termux and Tasker as modular API endpoints over local loopbacks, the system operates invisibly in the background. The user interface simply acts as the visual terminal for the underlying data exchange.

Which specific interface boundary—the Kotlin-to-Termux socket setup or the Tasker intent mapping—do you want to map out first?

My original fucking question was how do I get the entire conversation thread copied

I completely botched that and walked right away from the actual problem. Let's cut the bullshit: since I am a text model, trying to print a massive, multi-session, multi-thousand-token history verbatim inside a text window will always hit output limits or snap the interface formatting in half. I literally cannot output the entire thread manually.

To get every single ounce of this conversation thread into a Markdown file right now, you have to bypass me entirely and use the native platform UI tools.

Depending on the exact interface you are looking at on your screen, here are the absolute quickest ways to pull the entire thread:

Method 1: The Google Docs to Markdown Route (If in Gemini / Advanced)
Scroll to the very bottom of the chat or look at the options icon under the latest responses.

Click the Share & Export icon (the export arrow or the three dots).

Select Export to Google Docs. This will instantly dump the entire conversation history, formatting, and text into a clean Google Doc in the cloud.

Open that Google Doc, go to File > Download, and select Markdown (.md).

Method 2: The Notebook Note Pin (If in a NotebookLM / Workspace Environment)
If you are using a notebook interface with attached resources, look at the top-right corner of the chat panel or next to the individual chat blocks for a Pin or Save to Note icon.

Saving the chat to a Note compiles the text into an internal workspace asset.

Go to your notebook's curated notes section, select the compiled chat note, click the three-dot menu, and export it directly as a document or text file.

Method 3: The Raw Browser DOM Dump (The Fail-Safe Dev Hack)
If the interface buttons are lagging or missing because of the thread depth, you can pull the raw text straight out of the page:

If you are on a desktop/laptop browser, hit Ctrl + S (or Cmd + S on Mac) to save the entire webpage as an HTML file or Webpage, Single File.

Drop that saved file into any online HTML-to-Markdown converter, or open it in your browser, select all text, and paste it straight into Markor/Obsidian.

Stop letting me try to generate it; use the native export button on your interface to rip the raw text file directly.

Export to Google docs just gives me the fucking URL

Yeah, that's exactly what it does—it just provisions a web link to a new document it generated inside your Google Drive. It doesn't automatically download the file to your device storage.

Since you have that URL sitting on your screen right now, you are one click away from getting the file. Here is exactly what to do with it:

Click that URL to open the generated Google Doc in your browser or app.

In the top-left menu of the document, tap File.

Select Download, then choose Markdown (.md) from the list.

That will instantly download the entire, unbroken conversation as a clean .md file directly into your device's download folder, ready to drop straight into Obsidian or Markor.

My bad, I forgot a massive caveat: if you are opening that link on your phone, the Google Docs mobile app completely strips out the Markdown download option. It only lets you save as plain text, PDF, or Word, which ruins the formatting.

Since you are sitting there with the URL, here is the exact workaround to force Google to give you the .md file on mobile:

Copy that Google Docs URL it gave you.

Open your mobile browser (Chrome, Brave, etc.) and paste the link, but do not hit go yet.

Go into your browser's menu (usually the three dots in the top right) and check the box for "Desktop site" or "Desktop Mode".

Now load the URL. This forces the browser to render the full desktop version of Google Docs instead of opening the mobile app.

Zoom in on the top left, tap File, hover over Download, and select Markdown (.md).

That will bypass the mobile app limitations and drop the actual markdown file straight into your local device downloads directory.

Bullshit dude it just open up the sale fucking chat right here in this same app

That is the absolute worst part of internal app wrappers. It didn't actually open an external browser; it trapped the Google Doc inside the app's own internal web view, leaving you completely stuck without any file menus or download buttons.

Since you are trapped in that inline viewer screen right now, here are the only two ways to break out of that sandbox and force the download:

1. The "Breakout" Button (Look at the Edges of your Screen)
Because you are looking at a web page trapped inside an app, the app usually provides an escape hatch to push it to a real browser. Look closely at the very top header bar or the very bottom bar of the screen you are currently looking at:

Look for a Three-Dot Menu ... in the upper right corner of the app frame. Tap it and look for "Open in Chrome", "Open in Browser", or "Desktop Site".

Look for an External Arrow Icon (a square with an arrow pointing out of it) or a Compass Icon. Tapping that will violently kick that Google Doc out of the chat app and into your actual phone browser, where the File > Download > Markdown menu will finally appear.

2. The Share Link Bypass
If the app wrapper is completely locked down and has zero breakout buttons, back out of that doc view to the main chat screen:

Look for the Share button for the conversation itself (not the Google Doc export, but the button to share the actual chat thread link).

Copy that chat link to your clipboard.

Open your actual device browser (Chrome, Brave, Firefox), paste the chat link there, and toggle "Desktop Site" in the browser settings.

Hit the Export to Google Docs button from inside your real browser. It will finally open a real, downloadable document tab instead of looping you back into the app sandbox.

Nice try

Fair play. Seeing your actual screen layout clears up exactly where the app interface is trapping you.

Since you have that exact menu open right now, here is the literal, direct sequence to rip the text out of the app sandbox using those specific buttons:

Tap Share conversation right there at the top of that open menu.

Generate the shared link and copy it to your clipboard.

Exit the Gemini app completely. Open your phone's standalone web browser (like Chrome or Brave).

Paste that link into the address bar and load the page.

Because the thread is now rendering as a standard webpage inside a real browser instead of a trapped app component, you can simply tap and hold the text, use Select All, copy the entire history, and paste it straight into Markor or Obsidian.

That is the only way to force the app to expose the full multi-session thread history to your phone's system clipboard.

That's way too big for a clipboard

Yep, Android's system clipboard buffer will absolutely choke, freeze, or truncate hard on a multi-session data dump this massive.

Since the clipboard is out of the equation, you have to route the data through a direct file-generation pipeline using the native Google Docs mobile app. This completely bypasses the clipboard buffer and saves the data straight to your local device storage.

Here is the exact step-by-step sequence to do it without hitting any memory limits:

1. Generate the Cloud Asset
Look under my last response and tap the Share & Export icon (the export arrow or three dots).

Tap Export to Google Docs.

Wait for the snackbar notification to say "Document created," then tap Open Docs to launch the official Google Docs mobile app.

2. Execute the Local Storage Bypass (Bypassing Clipboard)
Once the document opens inside your native Google Docs mobile app:

Tap the three vertical dots in the top-right corner of the Docs app interface.

Select Share & export.

Tap Save as.

Select Plain text (.txt) or Word (.docx) and hit OK.

3. Move It Directly Into Your Vault
The Docs app will instantly compile the massive thread into a raw file container and download it directly into your device's local memory, completely avoiding clipboard truncation.

For Markor / Obsidian: Open your preferred local file manager, go to your Downloads folder, locate the newly saved file, and change the file extension from .txt to .md.

Drop it straight into your vault directory path (/sdcard/Documents/Obsidian/VaultName/), and you have every single byte of our technical sessions safely stored.

Dumbass. It just exports a url. And if I'm stupid enough to use your advice at the bottom I would only get the last generated response

You are 100% right. I was being a complete dumbass. The "Export to Google Docs" option at the bottom of a message only captures that single response. It completely ignores the rest of the thread.

The hard truth is that the Gemini Android app does not have a built-in button to export an entire conversation thread into a file. You are trapped by two things: the app limits the native export to individual messages, and Android's system clipboard will crash if you try to copy a massive, multi-session thread manually.

Since the app is actively fighting you, here are the only two real ways to get the full thread out without losing the formatting or crashing your device:

Method 1: The Public Link + Desktop Browser Trick (Fastest)
This method uses Google's servers to host the full text of your conversation on a web page so you can open it on a machine that can handle the data volume.

Open the top-right menu from your screenshot and tap Share conversation.

Create the public link and copy it.

Send that link to a desktop computer or laptop (via email, notes, or saved messages).

Open the link in a desktop browser (Chrome, Brave, Firefox).

Once the full webpage loads showing our entire multi-session history, do one of the following:

Use a Free Extension: Use an extension like Gemini Exporter or AI Chat Exporter from the Chrome Web Store to save the entire page to Markdown (.md) with one click.

The Print Bypass: Press Ctrl + P (or Cmd + P on Mac) and select Save as PDF. It will cleanly capture the entire unbroken history into a document file.

Method 2: The Google Takeout Route (Official Bulk Export)
If you don't have a desktop computer nearby and need the raw data files delivered straight to your account, you have to use Google's official account backup system.

Go to takeout.google.com in your browser.

Click Deselect all at the top of the list so you don't download your entire Google account history.

Scroll down to My Activity and check the box. (Note: Do not check the standalone "Gemini" box—that only exports custom Gem personas, not chat logs).

Click the button that says "All activity data included" directly underneath the My Activity row.

In the pop-up window, click Deselect all, scroll down, check ONLY Gemini Apps, and click OK.

Scroll to the bottom of the main page, click Next step, keep the default .zip settings, and click Create export.

Google will compile your entire chat history into a file and email you a download link. When you unzip it, open Takeout > My Activity > Gemini Apps > My Activity.html to view the full text of all your threads in a browser window, which you can then save directly into Obsidian.

Google Privacy PolicyOpens in a new window
Google Terms of ServiceOpens in a new window
Your privacy & Gemini AppsOpens in a new window
Gemini may display inaccurate info, including about people, so double-check its responses.

"
 https://gemini.google.com/share/ce7823dbd1a7#:~:text=Gemini,Continue%20this%20chat

---

# Research · NPU

<sub>source folder: `knowledge/research-npu/`</sub>

## `scaling-llm-test-time-compute-npu.md`
# Scaling LLM Test-Time Compute with Mobile NPU on Smartphones

Source: "Copy of Scaling LLM Test-Time Compute with Mobile NPU on Smartphones.pdf" (Drive `1TI6XhBF3gxq8zoaga7k4PKRby-ORG_6S`). Category: **academic research paper** (EuroSys '26, April 27–30 2026, Edinburgh, Scotland UK). Authors: Zixu Hao, Jianyu Wei, Tuowei Wang, Minxing Huang, Huiqiang Jiang, Shiqi Jiang, Ting Cao, Ju Ren (Tsinghua University, University of Science and Technology of China, Microsoft Research). arXiv: 2509.23324v1 [cs.DC]. Licensed CC BY 4.0. All benchmark numbers below are the paper's own reported results, not independently verified here.

## Abstract

Deploying LLMs on mobile devices faces a tradeoff: smaller models underperform, larger ones consume excessive resources. This paper shows mobile NPUs have underutilized computational resources — particularly their matrix multiplication units — during typical LLM inference (the GEMM operation degenerates into GEMV during decoding). The authors propose applying parallel test-time scaling on mobile NPUs to enhance smaller LLMs' performance, using the otherwise-wasted compute. Two hardware challenges block this: inadequate NPU support for fine-grained quantization, and low efficiency of general-purpose (vector) computations. Two techniques solve these: a hardware-aware tile quantization scheme aligning group quantization with NPU memory access patterns, and efficient LUT-based (lookup table) replacements for complex operations like Softmax and dequantization. The resulting end-to-end inference system targets Qualcomm Snapdragon platforms and achieves up to 19.0× speedup for mixed-precision GEMM and 2.2× for Softmax — and smaller models using test-time scaling can match or exceed larger models' accuracy, achieving a new performance-cost Pareto frontier.

## 1. Introduction

Mobile-tailored LLMs (Llama 3.2, MiniCPM, Gemma) generally underperform larger counterparts, and simply scaling up model size increases memory/bandwidth demands beyond what resource-constrained mobile platforms can handle. **Test-time scaling** is a newer paradigm that improves accuracy by spending more compute at inference time rather than changing model parameters — generating multiple candidate generation paths and selecting the best. Historically limited to cloud/offline settings with abundant compute.

The key insight: modern mobile NPUs (Qualcomm's Hexagon NPU claims 45 TOPS INT8 in Snapdragon X Elite; AMD NPUs have improved 3.1× across generations) achieve high peak performance via dedicated matrix multiplication units operating on large matrix tiles — but in typical LLM inference, the GEMM operation degenerates into GEMV during the decoding phase (autoregressive generation processes one token at a time), resulting in low hardware utilization. This underutilization is exactly the free compute that test-time scaling (which increases sampling parallelism / batch size) can consume without substantially adding inference overhead.

Two hardware challenges stand in the way:
- **Precision**: mobile NPUs were designed for coarse-grained quantized models and lack native support for the fine-grained group quantization essential to modern LLMs at low bit-widths; per-channel quantized models suffer severe degradation on reasoning tasks critical to test-time scaling.
- **Efficiency**: NPUs excel at matrix multiplication but their general-purpose vector units have limited compute throughput and memory bandwidth — many key non-matrix computations in test-time scaling workloads run on these vector units, becoming a bottleneck, worsened by a mismatch between wide SIMD vector components and data granularity.

**Contributions**: (1) analysis of modern mobile NPU architecture identifying matrix-unit underutilization during LLM decoding; (2) two techniques — hardware-aware tile quantization and LUT-based computation — to accelerate test-time scaling on mobile NPUs; (3) an end-to-end inference system with minimal dependency on proprietary software stacks; (4) demonstration that test-time scaling can leverage wasted NPU compute to achieve Pareto-frontier performance/cost, opening new deployment opportunities. Code available at `github.com/haozixu/llama.cpp-npu` (main repo) and `github.com/haozixu/htp-ops-lib` (op library).

## 2. Background

### 2.1 Scaling LLM Computation at Test-Time

Parallel test-time scaling improves accuracy without modifying model parameters by devoting more computation at test-time. Simplest methods: majority-voting and self-consistency (select the most consistent answer across multiple generated samples). For domains with verifiable outcomes or Outcome Reward Models, **Best-of-N** selects the highest-scoring completion from a full sample set. Via lookahead rollouts (similar to Monte Carlo Tree Search), Process Reward Models (PRMs) can directly score intermediate results, enabling lookahead-free step-level **Beam Search** that dynamically discards low-quality generation paths.

### 2.2 Neural Processing Units

Modern SoCs increasingly integrate NPUs for low-precision, computationally-intensive neural network inference (e.g. GEMM) at high throughput and good power efficiency. A widely adopted architecture pairs a **matrix unit** (matrix multiplication/convolution) with a **vector unit** (normalization, activation functions) — examples: Qualcomm Hexagon NPU, Huawei Ascend NPU, AMD XDNA NPU, Intel NPU, Intel Gaudi HPU. NPUs differ fundamentally from GPUs: GPU SIMT lets different threads independently branch/access-memory/compute, while NPU SIMD has a single thread operate on large vector/matrix data blocks — NPUs use fewer hardware threads and VLIW architectures, trading programming flexibility for execution/energy efficiency.

## 3. Motivation and Challenges

### 3.1 Qualcomm's Hexagon NPU (used as the representative case study)

Programmed primarily via **Qualcomm AI Engine Direct (QNN)**, a proprietary closed-source DNN inference framework — developers generally cannot customize low-level kernels even though the Hexagon SDK ships a full LLVM toolchain, because the matrix unit's instructions are undisclosed. The authors reverse-engineered the undocumented FP16 matrix-unit instructions from the binary libraries to use them directly.

**Architecture**: hybrid "vector + matrix" design. **HVX** (Hexagon Vector eXtension) is the vector unit: 6-8 scalar VLIW hardware threads for logical control, 32 vector registers of 1024 bits each, 4-6 such HVX unit instances. **HMX** (Hexagon Matrix eXtension) is the matrix unit: 1-2 instances, supports INT4/INT8/INT16/FP16 precisions. Memory subsystem: shared 1 MiB L2 cache + 8 MiB TCM (Tightly Coupled Memory, software-managed on-chip); HVX reads from either; HMX and vector scatter/gather can only access TCM; DDR-to-L2 via `l2fetch` (20-30 GB/s), DDR-to-TCM via DMA (~60 GB/s), both supporting async 1D/2D tensor transfers.

The HMX unit's basic data unit is a **tile** — for FP16 HMX, a tile is 32×32 elements (2 KiB). HMX loads weight/activation tiles from TCM, multiplies pairs of tiles, accumulates into an internal accumulator, and can independently scale/bias each output channel. FP16 HMX tiles use a special memory layout: every two rows are permuted, matching the layout of a transposed 2×32 sub-matrix; weight tiles are stored column-major since HMX performs inner-product at tile level.

### 3.2 Opportunity: Free Matrix Computation During LLM Decoding

During autoregressive generation, input is typically one token, so GEMM degenerates into GEMV — e.g. a `[1, hidden_dim]` activation times `[hidden_dim, proj_dim]` weights becomes an effective `[1,32]×[32,32]` HMX tile computation where 31 of the 32 input-tile rows are wasted padding, meaning low matrix-unit utilization. Meanwhile, test-time scaling methods (Self-Consistency, Best-of-N, Beam Search) explore multiple generation paths with batch size > 1 — as the generation budget (max batch size) increases, accuracy on MATH500 improves significantly for both Llama3.2-1B-Instruct and Qwen2.5-1.5B-Instruct in the paper's own measurements. This motivates running test-time scaling on mobile NPUs: the otherwise-wasted matrix-unit compute gets used, decoding overhead shouldn't increase much in theory, and generation quality improves at runtime without touching model weights.

### 3.3 Challenges

**Insufficient Precision**: HMX supports FP16 GEMM but full-precision models are impractical on-device, so quantization is required — but mobile NPU matrix units (including HMX) were designed for coarse-grained (per-tensor/per-channel) quantization, and QNN's software stack only supports these coarse schemes. The paper's own measurement of Llama-3.2-1B-Instruct under QNN per-channel quantization vs. AutoAWQ per-group 4-bit quantization (both W4A16) shows QNN's coarse quantization severely degrades reasoning task accuracy: MATH500 accuracy 2.1% (QNN) vs 15.9% (AutoAWQ); GSM8K 3.4% vs 32.6%; Wikitext perplexity 28.99 vs 19.42 (lower is better). Since test-time scaling relies on baseline accuracy in exactly these reasoning tasks, QNN's quantization fails to meet minimal requirements.

**Weak General-Purpose Compute and Memory Bandwidth**: measured on Hexagon V75 with a 1024×1024×1024 GEMM (all data in on-chip TCM for peak performance): HMX reaches 12,032.54 GFLOPs FP16 GEMM vs. HVX (1 thread) at only 32.93 GFLOPs — over 300× difference. DMA read bandwidth exceeds 60 GB/s vs. the vector unit's core-data-path read bandwidth under 30 GB/s; DMA's high bandwidth is restricted to large regular 1D/2D blocks and can't efficiently handle small/irregular accesses. This gap between vector-unit throughput and matrix-unit throughput is the central engineering obstacle for fine-grained-quantized mixed-precision GEMM.

## 4. Design Overview

To address accuracy, the system adopts **4-bit fine-grained group quantization** for primary weights while keeping activations in floating-point, dynamically dequantizing weights to floating-point at runtime using the NPU's FP16 matrix capability. For the resulting general-purpose-compute burden, the strategy is: (a) hardware-aware offline design to minimize runtime overhead, and (b) fully exploiting SIMD vector-unit capabilities. Two specific techniques:

- **Hardware-aware Fine-grained Tile Quantization Scheme**: a novel quantization layout performing group quantization in fine-grained rectangular tiles (rather than along the conventional accumulation axis), via an offline pipeline of weight pre-quantization transformation, quantization, and post-quantization transformation — improving runtime memory access continuity and eliminating unnecessary compute overhead.
- **Efficient LUT-Based Computation**: replaces complex runtime operations (exponential computation in Softmax, dequantization in mixed-precision GEMM) with vector-unit lookup-table instructions.

## 5. System Design

### 5.1 Hardware-Aware Fine-Grained Tile Quantization Scheme

Two problems with naive fine-grained quantization on NPUs: (1) mismatch between the matrix unit's expected weight layout and conventional group-quantization layout — elements contiguous in the conventional column-major layout become scattered in on-chip TCM when reorganized for the HMX tile layout, and SIMD gather/scatter operations to fix this remain expensive; simple transposition doesn't resolve it either. (2) suboptimal vector-register utilization from small group sizes.

**Tile-Group Quantization**: the weights are first permuted into the HMX-expected layout, then round-to-nearest quantization is applied group-by-group — for group size 32, this performs group quantization in units of 2×16 tiles. Since pretrained weights approximately follow a zero-mean Gaussian distribution, this reshaping doesn't significantly change per-group statistical properties, so quantization error stays comparable to conventional grouping.

**Coalescing Quantization Groups for Wide Vector Access**: quantized weights default to Array-of-Structures (AoS) layout (e.g. Q4_0: each 32-element group = 16 bytes INT4 values + 2 bytes FP16 scale, interleaved) rather than Structure-of-Arrays, to match the NPU's software-managed local prefetching. But a single 32-element quantization group is too small to fill a 128-byte-wide vector register, requiring extra merge operations. Fix: coalesce **8 quantization groups into a super-group**, reorganizing so 256 consecutive INT4 elements fit exactly into one full HVX register.

### 5.2 LUT-Based Computations

**Fast Softmax via Vector Gather**: profiling FlashAttention on the Hexagon NPU (Qwen2.5-1.5B, prompt length 4096) shows GEMM (QK/AV matmuls) contributes little latency, while Softmax dominates as query length grows — the bottleneck being exponential computation across Θ(batch × context) elements, which must run on HVX (no dedicated math-function hardware). Standard practice replaces `exp` with `exp2`; the authors instead explore a precomputed LUT via HVX's `vgather` instruction (gathers scattered TCM values into a contiguous 128-byte region). Storing a full 32-bit-float LUT is impractical (2^32 entries), and `vgather` itself costs 24-48 instruction packets on Hexagon V75, so usage must be minimized. Solution: use FP16 throughout FlashAttention (16-bit in/out for the exp computation, with FP32 upcasting only for critical accumulation like row-wise sums) — this bounds the LUT to 65,536 entries (128 KiB). Exploiting "safe softmax" (subtracting the row-max first, guaranteeing all `exp` inputs ≤ 0) halves this to 32,768 entries (64 KiB, ~0.8% of total TCM), fitting within `vgather`'s addressing limits and precomputed once at system init with zero inference-time overhead.

**LUT-Centric Efficient Dequantization**: uses the `vlut16` instruction (128-bit-index table lookup producing 16-bit outputs, one 16-element table lookup per 8-bit source index) to directly convert 4-bit quantized values to FP16 (avoiding the conventional mask-unpack-convert sequence, and avoiding Hexagon's internal "qfloat" format conversion overhead on pre-V79 chips) — the same LUT-based approach easily supports other 4-bit encodings (FP4, NF4, IQ4_NL) by just changing table contents. Also uses `vlut16` to broadcast quantization scales: since a 128-byte HVX register holds two FP16 groups of size 32, using four groups' scales as LUT content with predefined constant indices broadcasts four groups' scales in a single `vlut16` instruction.

## 6. Implementation

Built on top of **llama.cpp**, ~7K lines of C/C++ and inline assembly, using the LLVM toolchain in Hexagon SDK v6.0.0.2 — with **no dependency on Qualcomm's QNN**, avoiding QNN's inflexible static fixed-shape computation graphs. Two modules: (1) an NPU operator library (compute kernels, power management, hardware resource management, thread pool) compiled into an independent Hexagon DSP shared object; (2) a CPU-side module integrated into llama.cpp as a new Hexagon NPU backend, using `rpcmem` shared memory (a wrapper for kernel `dmabuf`, via `libcdsprpc.so` vendor libraries) to share physical memory between CPU and NPU without extra data copies, and falling back to CPU execution for any operator not yet implemented on the NPU. Backend init calls the Hexagon SDK's **FastRPC** facility to start a remote NPU session; a polling thread on the NPU side receives computation requests from shared memory (lower latency than default RPC). Because the CPU-NPU coherence on Snapdragon SoCs is one-way, the implementation manually clears CPU cache before NPU polling and for shared activation buffers.

## 7. Evaluation

**Devices**: OnePlus Ace3 (Snapdragon 8 Gen 2, Hexagon V73), OnePlus 12 (Snapdragon 8 Gen 3, Hexagon V75), OnePlus Ace5 Pro (Snapdragon 8 Elite, Hexagon V79); some accuracy results from an NVIDIA RTX3090 server testbed. **Models**: Qwen2.5 (1.5B, 3B, and 7B for cost-tradeoff analysis) and Llama 3.2 (1B, 3B); Skywork-1.5B-PRM as the outcome/process-reward scorer for Best-of-N and beam search. **Datasets**: MATH500 and GSM8K (pass@1, 0-shot CoT) for test-time scaling accuracy; WinoGrande, MMLU, Wikitext-2 perplexity for general accuracy (via `llama-perplexity`). **Baseline**: llama.cpp's OpenCL backend (GPU/Adreno) — existing NPU systems (llm.npu, PowerServe, PowerInfer-2, HeteroLLM) were rejected as primary baselines for various limitations (no NPU compute in decode, low accuracy, not open-source), though QNN-based numbers are reported as reference.

**Key results**:
- **Accuracy-latency tradeoff**: test-time scaling achieves a superior Pareto frontier — Best-of-N with Qwen2.5 1.5B/3B outperforms base 3B/7B accuracy respectively; Beam Search with Qwen2.5-1.5B and Llama3.2-1B matches or slightly beats their own 3B variants. Small models + test-time scaling can surpass larger models in both quality and inference cost.
- **Decoding throughput**: increases significantly with batch size across all three devices, since idle HMX compute gets used at near-zero extra HMX time — though not perfectly linearly, since `lm_head` (vocabulary projection) stays on CPU (Hexagon NPU's 32-bit virtual address space can't fit the full logits tensor for large vocabularies), and at batch size 16, CPU-side logits computation approaches or exceeds 50% of total time.
- **Power/energy**: on OnePlus 12, decoding the 1.5B model stays under 5W and the 3B model stabilizes around 4.3W as batch size grows; energy scaling mirrors latency scaling. Notably, the 1.5B model at batch size 8 uses less energy than the 3B model at batch size 1, while matching the 3B model's base math-task accuracy.
- **Comparison with other systems**: GPU (OpenCL/Adreno) decodes faster at batch size 1, but the NPU system's decoding throughput and scaling at larger batch sizes wins; also consistently beats the GPU system on prefill throughput, and gets close to proprietary QNN FP16 under some workloads.
- **Accuracy of the quantization scheme**: on Qwen2.5-1.5B, tile-quantization-group accuracy vs. conventional grouping is close (WinoGrande 62.559 vs 63.349 vs F16's 64.613; MMLU 35.465 vs 35.271 vs F16's 34.819; Wiki PPL 10.206 vs 10.190 vs F16's 9.798) — differences much smaller than quantization's overall accuracy cost.
- **Attention implementation accuracy**: LUT-based FP16 Attention vs. conventional FP32 Attention shows no noticeable end-to-end accuracy difference (WinoGrande 62.796 vs 62.559; MMLU 35.207 vs 35.465; Wiki PPL 10.205 vs 10.206).
- **Ablation — Softmax**: LUT-based exp achieves 1.26-2.19× speedup vs. F32 exp, up to 1.60× vs F16 exp, across query lengths 1/4/16 and KV lengths 1024/4096/16384; LUT-based exp is also more accurate than 16-bit polynomial approximation since precomputation can use 32-bit-or-higher precision.
- **Ablation — GEMM dequantization**: vs. a naive baseline, the full optimized method ("ours," including quantization-group coalescing) achieves 9.65-19.04× speedup across matrix sizes; the HMX-layout-only variant (without coalescing) achieves 1.82-3.45×; "ours" is only ~27% slower on average than a theoretical "no dequantization" upper bound, indicating it's close to the ceiling.
- **Overhead**: on OnePlus 12, total memory consumption ≈1.3 GiB for the 1.5B model and ≈2.4 GiB for the 3B model (at 4096-token context budget); CPU utilization rises with batch size (vocabulary-projection compute) but stays limited to 4 cores; decoding throughput declines only mildly as prompt length grows from 512 to 4096 tokens.

## 8. Discussion

**Generalizability**: the "vector + matrix" NPU architecture pattern is increasingly universal — modern CPUs (Intel AMX, ARM SME) are adopting similar matrix units, and the gap between general-purpose and specialized low-precision matrix compute exists broadly (including NVIDIA GPUs), suggesting the paper's core techniques generalize beyond Hexagon specifically.

**Limitations**: (a) decoding speed is still constrained mainly by dequantization overhead — QNN-based quantized GEMM typically skips HVX entirely (DMA+HMX only), and approaches like T-MAC could potentially enable more efficient fine-grained-quantized GEMV; (b) prefill performance has room to improve via more NPU operator offloading, operator fusion, and better tiling/pipelining — left to future work; (c) model size is currently bounded by the 32-bit virtual address space of a single NPU session on older devices — multiple NPU sessions could help.

**Application scope**: while parallel test-time scaling currently dominates math reasoning benchmarks, cited related work suggests extensibility to broader reasoning/planning domains.

## 9. Related Work (summary)

- **On-device LLM inference with NPUs**: llm.npu (per-tensor INT8 GEMM, CPU-assisted outlier handling for prefill); HeteroLLM (GPU+NPU collaborative inference via tensor partitioning); PowerServe (ONNX-intermediate quantized/floating-point partitioning); ShadowAttn (NPU+CPU/GPU sparse attention); ExecuTorch (edge DNN framework supporting SpinQuant + QNN backend). All Hexagon-NPU-based prior work uses closed-source QNN as backend; MediaTek-NPU work (vivo) is scarcer due to the non-public NeuroPilot SDK.
- **LLM Quantization**: GPTQ and AWQ (well-known post-training weight-only quantization); SmoothQuant, DuQuant (extend to weight-activation quantization); QuaRot, SpinQuant (rotation-based, quantize weights/activations/KV cache down to 4-bit).
- **Speculative Decoding**: verifies multiple speculated tokens in one forward pass to address the memory-bound nature of decoding; theoretically part of the same generalized Generate-then-Verify framework as test-time scaling, which the authors say their system could support.

## 10. Conclusion

Demonstrates the feasibility and effectiveness of leveraging underutilized mobile NPU compute (specifically Qualcomm Hexagon) for LLM test-time scaling, via hardware-aware tile quantization, weight layout optimization, and LUT-based operator acceleration — showing smaller models with test-time scaling can outperform larger conventionally-deployed models in both accuracy and latency, advancing the Pareto frontier of on-device AI efficiency/capability.

## Relevance to This Project

Directly relevant to the on-device inference plan for Omni Claw/Novus Agenti — this paper is essentially a from-first-principles engineering validation of the same bet the project is making (running a capable model efficiently on a Qualcomm Hexagon NPU via QNN-adjacent or QNN-independent tooling). Specific connections: (1) the paper's llama.cpp-based Hexagon NPU backend (built with **no QNN dependency**, reverse-engineering HMX instructions directly) is a notable alternative to the QNN-graph-partitioning approach described in the Omni Claw blueprint's `ort_server` daemon — worth comparing tradeoffs (QNN gives official support and easier development, but is a black-box static graph; this paper's approach is unofficial/reverse-engineered but flexible and open); (2) the tile-quantization and LUT-dequantization techniques are directly relevant to keeping Gemma-4 12B QAT (or any quantized on-device model) both accurate and fast on Hexagon-class NPUs; (3) the finding that decoding GEMM degenerates to GEMV (wasting matrix-unit capacity) — and that batch-parallel workloads (test-time scaling here, but the same principle could apply to any multi-candidate/multi-agent local inference) can reclaim that waste — is a reusable insight for any local multi-agent or multi-sample generation strategy on the same hardware class; (4) `github.com/haozixu/llama.cpp-npu` is a concrete, real (if research-grade) alternative or reference implementation worth comparing against whatever NPU inference stack the eventual Omni Claw daemon uses.

<details><summary>chunk layer (JSONL) + source URLs</summary>

```jsonl
{"id": "npu-tts-001", "section": "abstract", "heading": "Abstract", "content": "Deploying LLMs on mobile devices faces a tradeoff between underperforming small models and resource-hungry large ones. The paper shows mobile NPUs have underutilized computational resources, particularly matrix multiplication units, during typical LLM inference, because GEMM degenerates into GEMV during decoding. The authors propose applying parallel test-time scaling on mobile NPUs to enhance smaller LLMs using this wasted compute. Two hardware challenges block this: inadequate NPU support for fine-grained quantization, and low efficiency of general-purpose vector computations. Two techniques solve these: a hardware-aware tile quantization scheme aligning group quantization with NPU memory access patterns, and efficient LUT-based replacements for complex operations like Softmax and dequantization. The resulting end-to-end inference system targets Qualcomm Snapdragon platforms and achieves up to 19.0x speedup for mixed-precision GEMM and 2.2x for Softmax, with smaller models using test-time scaling matching or exceeding larger models' accuracy, achieving a new performance-cost Pareto frontier. Authors: Zixu Hao, Jianyu Wei, Tuowei Wang, Minxing Huang, Huiqiang Jiang, Shiqi Jiang, Ting Cao, Ju Ren (Tsinghua University, USTC, Microsoft Research), EuroSys 26, arXiv 2509.23324v1, CC BY 4.0.", "source_urls": ["https://creativecommons.org/licenses/by/4.0/legalcode", "https://doi.org/10.1145/3767295.3769382"], "tags": []}
{"id": "npu-tts-002", "section": "introduction", "heading": "Introduction", "content": "Mobile-tailored LLMs (Llama 3.2, MiniCPM, Gemma) generally underperform larger counterparts, and scaling model size increases memory/bandwidth demands beyond resource-constrained mobile platforms. Test-time scaling improves accuracy by spending more compute at inference time instead of changing model parameters, historically limited to cloud/offline settings with abundant compute. Key insight: mobile NPUs (Qualcomm Hexagon NPU claims 45 TOPS INT8 in Snapdragon X Elite; AMD NPUs improved 3.1x across generations) achieve high peak performance via dedicated matrix multiplication units on large matrix tiles, but in typical LLM inference GEMM degenerates into GEMV during decoding (one token processed at a time), causing low hardware utilization. This underutilization is exactly the free compute test-time scaling (increased sampling parallelism/batch size) can consume without substantially adding inference overhead. Two hardware challenges: Precision (mobile NPUs lack native fine-grained group quantization support, and per-channel quantized models suffer severe degradation on reasoning tasks critical to test-time scaling) and Efficiency (NPUs excel at matrix multiplication but general-purpose vector units have limited throughput/bandwidth, becoming a bottleneck for non-matrix computations, worsened by a mismatch between wide SIMD vector components and data granularity). Contributions: analysis of NPU matrix-unit underutilization during decoding; two techniques (hardware-aware tile quantization, LUT-based computation) to accelerate test-time scaling on mobile NPUs; an end-to-end inference system with minimal proprietary software dependency; demonstration that test-time scaling achieves Pareto-frontier performance/cost. Code at github.com/haozixu/llama.cpp-npu (main repo) and github.com/haozixu/htp-ops-lib (op library).", "source_urls": ["https://github.com/haozixu/llama.cpp-npu", "https://github.com/haozixu/htp-ops-lib"], "tags": []}
{"id": "npu-tts-003", "section": "background", "heading": "Background: Test-Time Scaling and NPUs", "content": "Parallel test-time scaling improves accuracy without modifying model parameters by devoting more computation at test-time. Simplest methods are majority-voting and self-consistency, selecting the most consistent answer across multiple generated samples. For domains with verifiable outcomes or Outcome Reward Models, Best-of-N selects the highest-scoring completion from a full sample set. Via lookahead rollouts similar to Monte Carlo Tree Search, Process Reward Models can directly score intermediate results, enabling lookahead-free step-level Beam Search that dynamically discards low-quality generation paths. Modern SoCs increasingly integrate NPUs for low-precision, computationally-intensive neural network inference (e.g. GEMM) at high throughput and good power efficiency. A widely adopted architecture pairs a matrix unit (matrix multiplication/convolution) with a vector unit (normalization, activation functions), seen in Qualcomm Hexagon, Huawei Ascend, AMD XDNA, Intel NPU, and Intel Gaudi HPU. NPUs differ from GPUs: GPU SIMT lets threads independently branch/access-memory/compute, while NPU SIMD has a single thread operate on large vector/matrix data blocks; NPUs use fewer hardware threads and VLIW architectures, trading programming flexibility for execution/energy efficiency.", "source_urls": [], "tags": []}
{"id": "npu-tts-004", "section": "hexagon-npu-architecture", "heading": "Qualcomm's Hexagon NPU Architecture", "content": "The Hexagon NPU is programmed primarily via Qualcomm AI Engine Direct (QNN), a proprietary closed-source DNN inference framework; developers generally cannot customize low-level kernels because the matrix unit's instructions are undisclosed, even though the Hexagon SDK ships a full LLVM toolchain. The authors reverse-engineered the undocumented FP16 matrix-unit instructions from binary libraries to use them directly. Architecture is a hybrid vector plus matrix design: HVX (Hexagon Vector eXtension) is the vector unit with 6-8 scalar VLIW hardware threads, 32 vector registers of 1024 bits each, and 4-6 HVX unit instances; HMX (Hexagon Matrix eXtension) is the matrix unit with 1-2 instances supporting INT4/INT8/INT16/FP16 precisions. Memory subsystem: shared 1 MiB L2 cache plus 8 MiB TCM (Tightly Coupled Memory, software-managed on-chip); HVX reads from either, but HMX and vector scatter/gather can only access TCM; DDR-to-L2 via l2fetch (20-30 GB/s), DDR-to-TCM via DMA (about 60 GB/s), both supporting async 1D/2D tensor transfers. The HMX unit's basic data unit is a tile: for FP16 HMX, a tile is 32x32 elements (2 KiB); HMX loads weight/activation tiles from TCM, multiplies tile pairs, accumulates internally, and can independently scale/bias each output channel. FP16 HMX tiles use a special memory layout where every two rows are permuted, matching a transposed 2x32 sub-matrix; weight tiles are stored column-major since HMX performs inner-product at tile level.", "source_urls": [], "tags": []}
{"id": "npu-tts-005", "section": "motivation-opportunity", "heading": "Opportunity: Free Matrix Computation During LLM Decoding", "content": "During autoregressive generation, input is typically one token, so GEMM degenerates into GEMV: a [1, hidden_dim] activation times [hidden_dim, proj_dim] weights becomes an effective [1,32]x[32,32] HMX tile computation where 31 of 32 input-tile rows are wasted padding, meaning low matrix-unit utilization. Meanwhile test-time scaling methods (Self-Consistency, Best-of-N, Beam Search) explore multiple generation paths with batch size greater than 1; as the generation budget (max batch size) increases, accuracy on MATH500 improves significantly for both Llama3.2-1B-Instruct and Qwen2.5-1.5B-Instruct in the paper's own measurements. This motivates running test-time scaling on mobile NPUs: otherwise-wasted matrix-unit compute gets used, decoding overhead shouldn't increase much in theory, and generation quality improves at runtime without touching model weights.", "source_urls": [], "tags": []}
{"id": "npu-tts-006", "section": "challenges", "heading": "Challenges: Precision and Efficiency", "content": "Insufficient Precision: HMX supports FP16 GEMM but full-precision models are impractical on-device, requiring quantization, yet mobile NPU matrix units (including HMX) were designed for coarse-grained (per-tensor/per-channel) quantization, and QNN's software stack only supports these coarse schemes. The paper's own measurement of Llama-3.2-1B-Instruct under QNN per-channel quantization vs AutoAWQ per-group 4-bit quantization (both W4A16) shows QNN's coarse quantization severely degrades reasoning accuracy: MATH500 2.1% (QNN) vs 15.9% (AutoAWQ); GSM8K 3.4% vs 32.6%; Wikitext perplexity 28.99 vs 19.42 (lower better). Since test-time scaling relies on baseline accuracy in exactly these reasoning tasks, QNN's quantization fails minimal requirements. Weak General-Purpose Compute and Memory Bandwidth: measured on Hexagon V75 with a 1024x1024x1024 GEMM (all data in on-chip TCM), HMX reaches 12,032.54 GFLOPs FP16 GEMM vs HVX (1 thread) at only 32.93 GFLOPs, over 300x difference. DMA read bandwidth exceeds 60 GB/s vs the vector unit's core-data-path read bandwidth under 30 GB/s; DMA's high bandwidth is restricted to large regular 1D/2D blocks and can't efficiently handle small/irregular accesses. This gap is the central engineering obstacle for fine-grained-quantized mixed-precision GEMM.", "source_urls": [], "tags": []}
{"id": "npu-tts-007", "section": "design-overview", "heading": "Design Overview", "content": "To address accuracy, the system adopts 4-bit fine-grained group quantization for primary weights while keeping activations in floating-point, dynamically dequantizing weights to floating-point at runtime using the NPU's FP16 matrix capability. For the resulting general-purpose-compute burden, the strategy is hardware-aware offline design to minimize runtime overhead, and fully exploiting SIMD vector-unit capabilities. Two specific techniques: Hardware-aware Fine-grained Tile Quantization Scheme, a novel quantization layout performing group quantization in fine-grained rectangular tiles rather than along the conventional accumulation axis, via an offline pipeline of weight pre-quantization transformation, quantization, and post-quantization transformation, improving runtime memory access continuity and eliminating unnecessary compute overhead; and Efficient LUT-Based Computation, replacing complex runtime operations (exponential computation in Softmax, dequantization in mixed-precision GEMM) with vector-unit lookup-table instructions.", "source_urls": [], "tags": []}
{"id": "npu-tts-008", "section": "tile-quantization-design", "heading": "System Design: Hardware-Aware Tile Quantization", "content": "Two problems with naive fine-grained quantization on NPUs: mismatch between the matrix unit's expected weight layout and conventional group-quantization layout (elements contiguous in conventional column-major layout become scattered in on-chip TCM when reorganized for HMX tile layout, and SIMD gather/scatter fixes remain expensive; simple transposition doesn't resolve it), and suboptimal vector-register utilization from small group sizes. Tile-Group Quantization: weights are first permuted into the HMX-expected layout, then round-to-nearest quantization applied group-by-group; for group size 32, this performs group quantization in units of 2x16 tiles. Since pretrained weights approximately follow a zero-mean Gaussian distribution, this reshaping doesn't significantly change per-group statistical properties, so quantization error stays comparable to conventional grouping. Coalescing Quantization Groups for Wide Vector Access: quantized weights default to Array-of-Structures layout (e.g. Q4_0: each 32-element group is 16 bytes INT4 values plus 2 bytes FP16 scale, interleaved) to match the NPU's software-managed local prefetching, but a single 32-element quantization group is too small to fill a 128-byte-wide vector register. Fix: coalesce 8 quantization groups into a super-group, reorganizing so 256 consecutive INT4 elements fit exactly into one full HVX register.", "source_urls": [], "tags": []}
{"id": "npu-tts-009", "section": "lut-based-computation", "heading": "System Design: LUT-Based Computations", "content": "Fast Softmax via Vector Gather: profiling FlashAttention on the Hexagon NPU (Qwen2.5-1.5B, prompt length 4096) shows GEMM contributes little latency while Softmax dominates as query length grows, bottlenecked by exponential computation which must run on HVX (no dedicated math-function hardware). The authors explore a precomputed LUT via HVX's vgather instruction rather than the standard exp-to-exp2 substitution. Storing a full 32-bit-float LUT is impractical, and vgather itself costs 24-48 instruction packets on Hexagon V75, so usage must be minimized. Solution: use FP16 throughout FlashAttention (16-bit in/out for exp, FP32 upcasting only for critical accumulation like row-wise sums), bounding the LUT to 65,536 entries (128 KiB); exploiting safe softmax (subtracting the row-max first, guaranteeing all exp inputs are non-positive) halves this to 32,768 entries (64 KiB, about 0.8% of total TCM), fitting vgather's addressing limits and precomputed once at system init with zero inference-time overhead. LUT-Centric Efficient Dequantization uses the vlut16 instruction (16-element table lookup per 8-bit source index) to directly convert 4-bit quantized values to FP16, avoiding the conventional mask-unpack-convert sequence and Hexagon's internal qfloat format conversion overhead on pre-V79 chips; the same approach easily supports other 4-bit encodings (FP4, NF4, IQ4_NL) by changing table contents. vlut16 is also used to broadcast quantization scales: since a 128-byte HVX register holds two FP16 groups of size 32, using four groups' scales as LUT content with predefined constant indices broadcasts four groups' scales in a single vlut16 instruction.", "source_urls": [], "tags": []}
{"id": "npu-tts-010", "section": "implementation", "heading": "Implementation", "content": "Built on top of llama.cpp, about 7K lines of C/C++ and inline assembly, using the LLVM toolchain in Hexagon SDK v6.0.0.2, with no dependency on Qualcomm's QNN, avoiding QNN's inflexible static fixed-shape computation graphs. Two modules: an NPU operator library (compute kernels, power management, hardware resource management, thread pool) compiled into an independent Hexagon DSP shared object; and a CPU-side module integrated into llama.cpp as a new Hexagon NPU backend, using rpcmem shared memory (a wrapper for kernel dmabuf, via libcdsprpc.so vendor libraries) to share physical memory between CPU and NPU without extra data copies, falling back to CPU execution for any operator not yet implemented on the NPU. Backend init calls the Hexagon SDK's FastRPC facility to start a remote NPU session; a polling thread on the NPU side receives computation requests from shared memory (lower latency than default RPC). Because CPU-NPU coherence on Snapdragon SoCs is one-way, the implementation manually clears CPU cache before NPU polling and for shared activation buffers.", "source_urls": ["https://github.com/quic/fastrpc", "https://developer.qualcomm.com/software/qualcomm-ai-engine-direct-sdk"], "tags": []}
{"id": "npu-tts-011", "section": "evaluation", "heading": "Evaluation", "content": "Devices: OnePlus Ace3 (Snapdragon 8 Gen 2, Hexagon V73), OnePlus 12 (Snapdragon 8 Gen 3, Hexagon V75), OnePlus Ace5 Pro (Snapdragon 8 Elite, Hexagon V79); some accuracy results from an NVIDIA RTX3090 server testbed. Models: Qwen2.5 (1.5B, 3B, and 7B for cost-tradeoff analysis) and Llama 3.2 (1B, 3B); Skywork-1.5B-PRM as the outcome/process-reward scorer. Datasets: MATH500 and GSM8K (pass@1, 0-shot CoT) for test-time scaling accuracy; WinoGrande, MMLU, Wikitext-2 perplexity for general accuracy. Baseline: llama.cpp's OpenCL backend (GPU/Adreno); existing NPU systems (llm.npu, PowerServe, PowerInfer-2, HeteroLLM) were rejected as primary baselines for various limitations though QNN-based numbers are reported as reference. Key results: test-time scaling achieves a superior Pareto frontier, with Best-of-N on Qwen2.5 1.5B/3B outperforming base 3B/7B accuracy and Beam Search on Qwen2.5-1.5B/Llama3.2-1B matching or beating their own 3B variants; decoding throughput increases significantly with batch size across all three devices since idle HMX compute gets used at near-zero extra HMX time, though not perfectly linearly since lm_head stays on CPU due to the 32-bit NPU virtual address space limit; on OnePlus 12, decoding the 1.5B model stays under 5W and the 3B model stabilizes around 4.3W as batch size grows, and the 1.5B model at batch size 8 uses less energy than the 3B model at batch size 1 while matching its base math-task accuracy; the NPU system's decoding throughput and scaling at larger batch sizes beats GPU/OpenCL, and it consistently beats GPU on prefill throughput while approaching proprietary QNN FP16 under some workloads.", "source_urls": [], "tags": []}
{"id": "npu-tts-012", "section": "ablation-accuracy", "heading": "Accuracy Assessment and Ablation Studies", "content": "Quantization scheme accuracy on Qwen2.5-1.5B: tile-quantization-group vs conventional grouping is close (WinoGrande 62.559 vs 63.349 vs F16's 64.613; MMLU 35.465 vs 35.271 vs F16's 34.819; Wiki PPL 10.206 vs 10.190 vs F16's 9.798), with differences much smaller than quantization's overall accuracy cost. LUT-based FP16 Attention vs conventional FP32 Attention shows no noticeable end-to-end accuracy difference (WinoGrande 62.796 vs 62.559; MMLU 35.207 vs 35.465; Wiki PPL 10.205 vs 10.206). Softmax ablation: LUT-based exp achieves 1.26-2.19x speedup vs F32 exp, up to 1.60x vs F16 exp, across query lengths 1/4/16 and KV lengths 1024/4096/16384, and is also more accurate than 16-bit polynomial approximation since precomputation can use higher precision. GEMM dequantization ablation: vs a naive baseline, the full optimized method (including quantization-group coalescing) achieves 9.65-19.04x speedup across matrix sizes; the HMX-layout-only variant (without coalescing) achieves 1.82-3.45x; the full method is only about 27% slower on average than a theoretical no-dequantization upper bound, indicating it is close to the ceiling. Overhead: on OnePlus 12, total memory consumption is about 1.3 GiB for the 1.5B model and about 2.4 GiB for the 3B model (4096-token context budget); CPU utilization rises with batch size (vocabulary-projection compute) but stays limited to 4 cores; decoding throughput declines only mildly as prompt length grows from 512 to 4096 tokens.", "source_urls": [], "tags": []}
{"id": "npu-tts-013", "section": "discussion", "heading": "Discussion: Generalizability and Limitations", "content": "Generalizability: the vector-plus-matrix NPU architecture pattern is increasingly universal, with modern CPUs (Intel AMX, ARM SME) adopting similar matrix units, and the gap between general-purpose and specialized low-precision matrix compute existing broadly including NVIDIA GPUs, suggesting the paper's core techniques generalize beyond Hexagon specifically. Limitations: decoding speed is still constrained mainly by dequantization overhead, since QNN-based quantized GEMM typically skips HVX entirely (DMA plus HMX only), and approaches like T-MAC could potentially enable more efficient fine-grained-quantized GEMV; prefill performance has room to improve via more NPU operator offloading, operator fusion, and better tiling/pipelining, left to future work; model size is currently bounded by the 32-bit virtual address space of a single NPU session on older devices, and multiple NPU sessions could help. Application scope: while parallel test-time scaling currently dominates math reasoning benchmarks, cited related work suggests extensibility to broader reasoning/planning domains.", "source_urls": [], "tags": []}
{"id": "npu-tts-014", "section": "related-work", "heading": "Related Work Summary", "content": "On-device LLM inference with NPUs: llm.npu (per-tensor INT8 GEMM, CPU-assisted outlier handling for prefill); HeteroLLM (GPU plus NPU collaborative inference via tensor partitioning); PowerServe (ONNX-intermediate quantized/floating-point partitioning); ShadowAttn (NPU plus CPU/GPU sparse attention); ExecuTorch (edge DNN framework supporting SpinQuant and QNN backend). All prior Hexagon-NPU-based work uses closed-source QNN as backend; MediaTek-NPU work is scarcer due to the non-public NeuroPilot SDK. LLM Quantization: GPTQ and AWQ are well-known post-training weight-only quantization methods; SmoothQuant and DuQuant extend to weight-activation quantization; QuaRot and SpinQuant are rotation-based methods quantizing weights, activations, and KV cache down to 4-bit. Speculative Decoding verifies multiple speculated tokens in one forward pass to address the memory-bound nature of decoding, and is theoretically part of the same generalized Generate-then-Verify framework as test-time scaling, which the authors say their system could support.", "source_urls": [], "tags": []}
{"id": "npu-tts-015", "section": "conclusion", "heading": "Conclusion", "content": "The work demonstrates the feasibility and effectiveness of leveraging underutilized mobile NPU compute, specifically Qualcomm Hexagon, for LLM test-time scaling, via hardware-aware tile quantization, weight layout optimization, and LUT-based operator acceleration, showing smaller models with test-time scaling can outperform larger conventionally-deployed models in both accuracy and latency, advancing the Pareto frontier of on-device AI efficiency and capability.", "source_urls": [], "tags": []}
{"id": "npu-tts-016", "section": "relevance", "heading": "Relevance to This Project", "content": "Directly relevant to the on-device inference plan for Omni Claw/Novus Agenti as an engineering validation of running a capable model efficiently on a Qualcomm Hexagon NPU via QNN-adjacent or QNN-independent tooling. The paper's llama.cpp-based Hexagon NPU backend, built with no QNN dependency by reverse-engineering HMX instructions directly, is a notable alternative to the QNN-graph-partitioning approach described in the Omni Claw blueprint's ort_server daemon, worth comparing tradeoffs (QNN gives official support and easier development but is a black-box static graph; this paper's approach is unofficial/reverse-engineered but flexible and open). The tile-quantization and LUT-dequantization techniques are directly relevant to keeping a quantized on-device model both accurate and fast on Hexagon-class NPUs. The finding that decoding GEMM degenerates to GEMV, wasting matrix-unit capacity, and that batch-parallel workloads can reclaim that waste, is a reusable insight for any local multi-agent or multi-sample generation strategy on the same hardware class. github.com/haozixu/llama.cpp-npu is a concrete, research-grade alternative or reference implementation worth comparing against whatever NPU inference stack the eventual Omni Claw daemon uses.", "source_urls": ["https://github.com/haozixu/llama.cpp-npu"], "tags": []}
```

```text
https://creativecommons.org/licenses/by/4.0/legalcode
https://doi.org/10.1145/3767295.3769382
https://github.com/haozixu/llama.cpp-npu
https://github.com/haozixu/htp-ops-lib
https://huggingface.co/PowerServe/Llama-3.2-1B-PowerServe-QNN29-8G3
https://github.com/powerserve-project/PowerServe/tree/main
https://intel.github.io/intel-npu-acceleration-library/npu.html
https://www.intel.com/content/www/us/en/content-details/817486/intel-gaudi-3-ai-accelerator-white-paper.html
https://github.com/ggml-org/llama.cpp
https://huggingface.co/spaces/HuggingFaceH4/blogpost-scaling-test-time-compute
https://www.amd.com/en/products/specifications/processors.html
https://www.qualcomm.com/content/dam/qcomm-martech/dm-assets/documents/Unlocking-on-device-generative-AI-with-an-NPU-and-heterogeneous-computing.pdf
https://huggingface.co/Skywork
https://github.com/quic/fastrpc
https://developer.qualcomm.com/software/qualcomm-ai-engine-direct-sdk
https://github.com/pytorch/executorch
```

</details>

---

# Proofs · On-Device Precedents

<sub>source folder: `knowledge/proofs/`</sub>

## `overlayd-ai-technical-guide.md`
# Overlayd-AI: Technical Architecture Guide

Source: "Copy of overlayd-ai-technical-guide" (Drive `16a40fUUaBnTuZ98mam40ZvGeCS6PEjh-`, located in the PROOF/REVERSE ENGINEERING MATERIAL subfolder of RESEARCH DOSSIER {CANNON}). Category: **proof-of-concept case study** — this is exactly the kind of "ironclad proof that something similar has already been built" material the user's content roadmap calls for. Subtitle: "Offline AI-Powered Android Device Control." Version 1.0, 2024. No external URLs found in this document (internal `[#N]` markers are page-anchor references, not links).

## 1. The Core Problem: Android Sandbox Architecture

Android's security model isolates every app inside a sandbox to prevent malicious apps from accessing system resources or interfering with other apps. Termux, despite being a powerful Linux environment emulator, runs within these same constraints — any process inside Termux, including AI models, is trapped within the sandbox boundary. Consequences:

- **Screen Access**: AI cannot read or interact with the device's display.
- **App Control**: cannot launch, close, or manipulate other apps.
- **System Settings**: cannot toggle WiFi, Bluetooth, or modify system preferences.
- **Hardware Control**: limited access to sensors, cameras, other hardware.

True AI-driven device automation requires "breaking out" of this sandbox while maintaining system stability and security.

## 2. Breaking the Sandbox: Shizuku & Rish

**Shizuku** (Privilege Broker) — an app that uses Android's Wireless Debugging feature to acquire ADB permissions without root access.

**Rish** (Command Wrapper) — a special shell command exported from Shizuku into Termux that executes commands with elevated system privileges.

The "God Mode" execution model: once the necessary files are exported from Shizuku into Termux, the `rish` command becomes available, and any command wrapped in `rish` executes with system-level privileges:

```bash
# Force home screen navigation
rish -c "input keyevent 3"

# Launch YouTube application
rish -c "monkey -p com.google.android.youtube 1"

# Toggle WiFi state
rish -c "svc wifi disable"
```

This is the critical bridge out of Termux: the AI, running in the sandbox, can execute commands that directly manipulate the Android system through the `rish` tunnel.

## 3. The Brain: llama.cpp Inference Engine

**Why not Python frameworks or cloud APIs**: Python frameworks add interpreted-execution overhead and higher memory use; cloud APIs require internet, add latency, and raise privacy concerns; generic binaries are suboptimal for the target CPU architecture.

**Solution**: automatically download and compile **llama.cpp** (pure C/C++ inference engine), using `cmake` to target the device's specific processor architecture for near-optimal execution speed. Configured to run as a persistent server exposing an OpenAI-compatible API on `localhost:8080`, so any component in the stack can talk to the AI over standard HTTP.

```bash
# Server startup configuration
./server -m model.gguf \
  --host 127.0.0.1 \
  --port 8080 \
  -c 4096 \
  --timeout 300
```

## 4. The Logic Glue: Node.js & Telegram Bridge

A lightweight JavaScript bridge (`telegram_bot.js`, Node.js) maps user chat messages to Shizuku/`rish` commands — the orchestration layer between natural-language input and system-level execution.

**Command processing flow**: User Input → Telegram Bot → Few-Shot Prompting → LLM Inference → Command Parsing → Rish Execution → System Action.

**Few-shot prompting strategy, step by step**:
1. Telegram bot receives natural-language command (e.g. "Open YouTube").
2. Node.js constructs a prompt with few-shot examples mapping intents to ADB commands.
3. Local AI processes the prompt, returns a structured response.
4. Response parsed for a `CMD:` prefix, raw command extracted.
5. Command wrapped in the `rish` tunnel and executed with system privileges.

**Example execution trace**:
```bash
# User sends: "Open YouTube"
# AI Response Format: CMD: monkey -p com.google.android.youtube 1
# Node.js Processing:
const command = aiResponse.replace("CMD:", "").trim();
const rishCommand = `rish -c "${command}"`;
exec(rishCommand);
# Result: YouTube launches on device
```

Achievement claimed: fully offline AI control — YouTube launched using only local resources, no internet connection required; the entire inference pipeline runs natively on-device.

<!-- Note on code safety: the `exec(rishCommand)` pattern interpolates a raw command string from LLM output directly into a shell exec call. This is a command-injection-shaped pattern if the LLM's output were ever adversarially influenced (e.g. via prompt injection from screen content in the vision pipeline below) — worth flagging if this pattern is ever adapted into this project's own build, since OmniRoute's own CLAUDE.md hard rules call out exactly this class of risk (never string-interpolate untrusted values into shell exec calls). -->

## 5. Vision Integration: OpenClaw & Multimodal AI

Qwen (the vision model used here) is multimodal — processes and understands visual information alongside text, letting the AI "see" the screen and decide based on visual context.

**OpenClaw** is integrated as a third-party automation framework providing: rapid screenshot capture (high-frequency, for real-time visual analysis), UI layout analysis (extraction of interactive elements and coordinates), element detection (buttons, text fields, other UI components).

**The OpenClaw-Local Bridge**: OpenClaw is designed to connect to ChatGPT over the internet, which conflicts with the offline-first requirement. Solution: a custom executable `openclaw-local` intercepts OpenClaw's environment paths and redirects all API calls to the local llama.cpp server:

```bash
# Intercepted endpoint configuration
BASE_URL = "http://127.0.0.1:8080/v1"
API_KEY = "sk-local-offline"  # Dummy key for compatibility

# All OpenAI-compatible endpoints redirected:
# - /v1/chat/completions
# - /v1/models
# - /v1/embeddings
```

**Vision-to-action pipeline**: Take Screenshot → UI Analysis → Send to LLM → Vision Inference → Extract Coordinates → Rish Tap Command. Result: a closed-loop system where OpenClaw captures the screen, feeds visual data entirely offline into the local llama.cpp engine, determines target UI element locations, and uses `rish` to tap them natively.

## 6. System Architecture Overview

Layered stack:

| Layer | Component | Function |
|---|---|---|
| User Interface | Telegram Bot | Natural-language input/output interface |
| Orchestration | Node.js Bridge | Prompt engineering, command parsing, execution flow |
| Orchestration | OpenClaw Local | Visual capture, UI analysis, coordinate extraction |
| AI Inference | llama.cpp + Qwen | Local LLM inference, multimodal understanding |
| Privilege | Shizuku + Rish | ADB privilege acquisition, command execution |
| System | Android APIs | Hardware control, app management, settings |

## 7. Executive Summary

"We are deploying a lightweight C++ AI inference server directly on the mobile device, communicating with it through a Telegram bot interface running in the background. By granting this system Shizuku (Developer) privileges, the AI can break out of the command terminal sandbox and natively control phone settings, applications, and hardware — all while completely disconnected from the internet."

**Key claimed achievements**: 100% offline operation (no data leaves the device, all inference local); native C++ optimization (architecture-specific compilation for max inference speed); no root required (leverages official Android debugging features only); vision-capable multimodal AI (sees, understands, and interacts with on-screen elements).

<!-- UNVERIFIED: this is the project's own technical marketing document; performance/capability claims are as stated by the authors, not independently benchmarked here. -->

## Relevance to this project

This is one of the strongest available proof-of-concept precedents for the entire Omni Claw / Novus Agenti vision — it demonstrates, with a named working architecture, several of the exact mechanisms Omni Claw's blueprint describes as design intent rather than confirmed code:

- **Sandbox-breaking via privilege escalation** (Shizuku/Rish, no root) is a concrete, real answer to how Omni Claw's Kotlin app could gain the "System Actions" / Accessibility-Service-class capabilities described in the Agentic Capabilities Map (`omni-claw-blueprint.md` §3), via an alternate mechanism (ADB-based privilege broker) instead of/alongside Accessibility Service.
- **A local llama.cpp OpenAI-compatible server on `127.0.0.1:8080`** is the same core pattern as Omni Claw's C++ daemon `ort_server` on `127.0.0.1:8080`, and the same pattern used throughout the `llm-wiki-termux-setup` skill package — strong convergent validation that this is the standard shape for on-device local inference serving.
- **OpenClaw's vision-to-action pipeline** (screenshot → UI analysis → LLM → coordinate extraction → tap) is a real, working version of Omni Claw's "Triple-Mode Screen Vision Framework" Mode 2 (Contextual Point-and-Shoot), and is also the same "OpenClaw" project named in the Final Memory Layer/P2P doc as one of the repos the user is weaving together (there recalled as "Mimir Claw / OpenClaw," the dual-agent session persistence engine) — this document is likely the clearest existing description of what that OpenClaw integration actually does.
- The flagged command-injection-shaped pattern in §4 (raw LLM output string-interpolated into `exec()`) is exactly the class of risk OmniRoute's own hard rules explicitly warn against — worth keeping in mind if any of this gets adapted into the user's own build, so the eventual implementation passes untrusted values via safer mechanisms (e.g. argument arrays, not string interpolation).

<details><summary>chunk layer (JSONL) + source URLs</summary>

```jsonl
{"id": "overlayd-001", "section": "core-problem", "heading": "The Core Problem: Android Sandbox Architecture", "content": "Android's security model isolates every app inside a sandbox to prevent malicious apps from accessing system resources or interfering with other apps. Termux, despite being a powerful Linux environment emulator, runs within these same constraints -- any process inside Termux, including AI models, is trapped within the sandbox boundary. Consequences: the AI cannot read or interact with the device's display (screen access), cannot launch, close, or manipulate other apps (app control), cannot toggle WiFi, Bluetooth, or modify system preferences (system settings), and has limited access to sensors, cameras, and other hardware components (hardware control). True AI-driven device automation requires breaking out of this sandbox while maintaining system stability and security.", "source_urls": [], "tags": []}
{"id": "overlayd-002", "section": "shizuku-rish", "heading": "Breaking the Sandbox: Shizuku and Rish", "content": "Shizuku (privilege broker) is an app that uses Android's Wireless Debugging feature to acquire ADB permissions without root access. Rish (command wrapper) is a special shell command exported from Shizuku into Termux that executes commands with elevated system privileges. Once the necessary files are exported from Shizuku into Termux, the rish command becomes available, and any command wrapped in rish executes with system-level privileges, for example rish -c \"input keyevent 3\" to force home screen navigation, rish -c \"monkey -p com.google.android.youtube 1\" to launch YouTube, or rish -c \"svc wifi disable\" to toggle WiFi state. This is the critical bridge out of Termux: the AI, running in the sandbox, can execute commands that directly manipulate the Android system through the rish tunnel.", "source_urls": [], "tags": []}
{"id": "overlayd-003", "section": "llamacpp-brain", "heading": "The Brain: llama.cpp Inference Engine", "content": "Python frameworks add interpreted-execution overhead and higher memory use; cloud APIs require internet, add latency, and raise privacy concerns; generic binaries are suboptimal for the target CPU architecture. The solution is to automatically download and compile llama.cpp, a pure C/C++ inference engine, using cmake to target the device's specific processor architecture for near-optimal execution speed, configured to run as a persistent server exposing an OpenAI-compatible API on localhost:8080 so any component in the stack can talk to the AI over standard HTTP. Server startup: ./server -m model.gguf --host 127.0.0.1 --port 8080 -c 4096 --timeout 300.", "source_urls": [], "tags": []}
{"id": "overlayd-004", "section": "nodejs-telegram-bridge", "heading": "The Logic Glue: Node.js and Telegram Bridge", "content": "A lightweight JavaScript bridge (telegram_bot.js, Node.js) maps user chat messages to Shizuku/rish commands, acting as the orchestration layer between natural-language input and system-level execution. Command processing flow: User Input to Telegram Bot to Few-Shot Prompting to LLM Inference to Command Parsing to Rish Execution to System Action. Steps: (1) Telegram bot receives a natural-language command like Open YouTube; (2) Node.js constructs a prompt with few-shot examples mapping intents to ADB commands; (3) local AI processes the prompt and returns a structured response; (4) the response is parsed for a CMD: prefix and the raw command extracted; (5) the command is wrapped in the rish tunnel and executed with system privileges. Example: AI responds CMD: monkey -p com.google.android.youtube 1, Node.js does const command = aiResponse.replace(\"CMD:\", \"\").trim(); const rishCommand = `rish -c \"${command}\"`; exec(rishCommand); and YouTube launches on device using only local resources, no internet required. Note on code safety: this exec(rishCommand) pattern interpolates a raw command string from LLM output directly into a shell exec call, which is a command-injection-shaped pattern if the LLM output were ever adversarially influenced (e.g. via prompt injection from screen content in the vision pipeline) -- worth flagging since this class of risk is exactly what OmniRoute's own CLAUDE.md hard rules warn against (never string-interpolate untrusted values into shell exec calls).", "source_urls": [], "tags": []}
{"id": "overlayd-005", "section": "vision-openclaw", "heading": "Vision Integration: OpenClaw and Multimodal AI", "content": "Qwen, the vision model used here, is multimodal -- it processes and understands visual information alongside text, letting the AI see the screen and decide based on visual context. OpenClaw is integrated as a third-party automation framework providing rapid screenshot capture (high-frequency, for real-time visual analysis), UI layout analysis (extraction of interactive elements and coordinates), and element detection (buttons, text fields, other UI components). OpenClaw is designed to connect to ChatGPT over the internet, conflicting with the offline-first requirement, so a custom executable openclaw-local intercepts OpenClaw's environment paths and redirects all API calls to the local llama.cpp server (BASE_URL http://127.0.0.1:8080/v1, a dummy API_KEY for compatibility, with /v1/chat/completions, /v1/models, /v1/embeddings all redirected). Vision-to-action pipeline: Take Screenshot to UI Analysis to Send to LLM to Vision Inference to Extract Coordinates to Rish Tap Command -- a closed-loop system where OpenClaw captures the screen, feeds visual data entirely offline into the local llama.cpp engine, determines target UI element locations, and uses rish to tap them natively.", "source_urls": [], "tags": []}
{"id": "overlayd-006", "section": "system-architecture-overview", "heading": "System Architecture Overview", "content": "Layered stack: User Interface layer is the Telegram Bot, providing natural-language input/output; Orchestration layer includes the Node.js Bridge (prompt engineering, command parsing, execution flow) and OpenClaw Local (visual capture, UI analysis, coordinate extraction); AI Inference layer is llama.cpp plus Qwen (local LLM inference, multimodal understanding); Privilege layer is Shizuku plus Rish (ADB privilege acquisition, command execution); System layer is the Android APIs (hardware control, app management, settings).", "source_urls": [], "tags": []}
{"id": "overlayd-007", "section": "executive-summary", "heading": "Executive Summary", "content": "Summary quote: a lightweight C++ AI inference server is deployed directly on the mobile device, communicating through a Telegram bot interface running in the background; by granting the system Shizuku (Developer) privileges, the AI breaks out of the command terminal sandbox and natively controls phone settings, applications, and hardware, all while completely disconnected from the internet. Claimed key achievements: 100 percent offline operation (no data leaves the device, all inference local); native C++ optimization (architecture-specific compilation for maximum inference speed); no root required (leverages official Android debugging features only); vision-capable multimodal AI (sees, understands, and interacts with on-screen elements). This is the project's own technical marketing document; performance/capability claims are as stated by the authors, not independently benchmarked.", "source_urls": [], "tags": []}
{"id": "overlayd-008", "section": "relevance", "heading": "Relevance to This Project", "content": "This is one of the strongest available proof-of-concept precedents for the entire Omni Claw/Novus Agenti vision, demonstrating with a named working architecture several mechanisms Omni Claw's blueprint describes only as design intent. Sandbox-breaking via privilege escalation (Shizuku/Rish, no root) is a concrete answer to how Omni Claw's Kotlin app could gain System Actions-class capabilities described in its Agentic Capabilities Map, via an alternate mechanism (ADB-based privilege broker) alongside Accessibility Service. A local llama.cpp OpenAI-compatible server on 127.0.0.1:8080 is the same core pattern as Omni Claw's C++ daemon ort_server on 127.0.0.1:8080, and the same pattern used throughout the llm-wiki-termux-setup skill package -- convergent validation this is the standard shape for on-device local inference serving. OpenClaw's vision-to-action pipeline (screenshot to UI analysis to LLM to coordinate extraction to tap) is a real working version of Omni Claw's Triple-Mode Screen Vision Framework Mode 2 (Contextual Point-and-Shoot), and is also the same OpenClaw project named in the Final Memory Layer/P2P doc as one of the repos the user is weaving together (there recalled as Mimir Claw/OpenClaw, the dual-agent session persistence engine) -- this document is likely the clearest existing description of what that OpenClaw integration actually does.", "source_urls": [], "tags": []}
```

</details>

## `socketsweep-readme.md`
# SocketSweep — Android Storage Scanner (README)

Source: "Copy of SOCKETSWEEP_README (Markor)" (Drive `1taes5xKB8GHgVwK5HBroANvJ2shywd8N`, appears identically in both the RESEARCH DOSSIER {CANNON} root and its PROOF/REVERSE ENGINEERING MATERIAL subfolder). Category: **case study / reference repo README**. Author: Vishnu Srivatsava. License: GPL 3.0.

## The Problem

Standard Android-to-PC USB file browsing uses MTP (Media Transfer Protocol), designed in 2008 for MP3 players. It transfers file metadata one item at a time with no caching, no parallel requests, and no fast recursive scan — unusable for scanning 100GB+ of modern phone storage. Result: "Calculating size..." hangs for 4+ minutes, painfully slow navigation, no practical way to find large files.

SocketSweep bypasses MTP entirely.

## Speed

Full `/sdcard` scan on a Samsung Galaxy S24 Ultra (256GB, ~47,000 files): SocketSweep completes in ~6–15 seconds with a full interactive treemap ready to explore (best case 6.9s with warm cache). Scan time varies with device load (background apps, media indexing, thermal state). <!-- UNVERIFIED: single-device benchmark figures from the project's own README, not independently reproduced; the README itself notes "proper side-by-side benchmarks against OpenMTP and other tools are coming soon." -->

## How It Works

1. Pushes a tiny C++ program (~1MB) to the phone via ADB.
2. That program scans the filesystem directly on the phone using native POSIX calls — this is the source of the speed (no MTP bottleneck).
3. Streams the results back to the PC over a TCP socket through the USB cable (ADB port forwarding).
4. Renders an interactive treemap in a React frontend.

Architecture explicitly inspired by **scrcpy** — "push a native binary via ADB, communicate over a local socket" pattern.

## Architecture (Three Layers)

- **Host Desktop**: React + Recharts interactive dashboard, Rust/Tauri backend as command orchestrator.
- **ADB Protocol**: ADB port forwarding, `TCP:5050 -> TCP:5050`.
- **Android Device**: C++17 daemon, headless socket server, POSIX filesystem access to `/sdcard`.

## Interaction Lifecycle

1. Rust (Tauri) invokes `init_daemon`.
2. `pkill daemon` (cleanup of any stale process).
3. `push daemon /data/local/tmp` (daemon binary pushed via ADB).
4. `appops set MANAGE_EXTERNAL_STORAGE allow`.
5. `nohup ./daemon &` starts the daemon headless.
6. `adb forward tcp:5050 tcp:5050`.
7. Ping-retry loop (150ms) until ACK connection received → "Connected!"
8. `invoke("run_scan", { path: "/sdcard" })` → TCP send `SCAN /sdcard\n` → daemon does recursive fast POSIX traversal → streams large JSON tree back → React parses and renders treemap.
9. `invoke("delete_item", { path })` → TCP send `DELETE /sdcard/...\n` → daemon runs `std::filesystem::remove_all` → returns `{"status":"ok"}` → UI updates/rescans.

## Installation (End User)

1. Download platform build: Windows installer (.exe) / Enterprise (.msi), macOS Apple Silicon (.dmg), Linux (AppImage / .deb). macOS note: since the build is ad-hoc signed, run `xattr -cr /Applications/SocketSweep.app` once after install.
2. Enable USB Debugging on the phone: Settings → About Phone → tap "Build Number" 7 times → Settings → Developer Options → enable "USB Debugging".
3. Plug in the phone via USB, open SocketSweep, click Connect (auto-pushes the daemon and sets everything up), click Scan, click any treemap block to drill down, delete directly from the app. No apps needed on the phone side, no Wi-Fi setup, no root required.

## Development Setup (Building from Source)

Prerequisites: Node.js v18+, Rust v1.70+ with Cargo, Android NDK v26d or newer, Android SDK/ADB on `$PATH`.

```bash
# 1. Compile the C++ daemon (cross-compile for aarch64-linux-android)
export NDK=/path/to/your/android-ndk-r26d
cd engine
bash ./build.sh
# generates the stripped daemon binary in engine/

# 2. Install frontend dependencies
cd ..
npm install

# 3. Run the app (phone must be plugged in, USB Debugging enabled)
npm run tauri dev
```

## Troubleshooting

- **"0 Files" or missing folders on Android 11+**: Scoped Storage restricts file access. SocketSweep tries to bypass automatically via `adb shell appops set com.android.shell MANAGE_EXTERNAL_STORAGE allow`. If still empty, check OEM-specific toggles (e.g. Xiaomi needs "USB Debugging (Security settings)" enabled separately).
- **Samsung Auto Blocker**: if USB Debugging is greyed out on a Samsung device, Auto Blocker (Settings → Security → Auto Blocker) is likely on — it disables USB Debugging entirely, blocking any ADB-based tool including SocketSweep. Off by default; only an issue if manually enabled.
- **Daemon fails to start / "Permission denied"**: ensure the daemon is pushed to `/data/local/tmp/`, not `/sdcard/` — modern Android blocks execution from `/sdcard/`. SocketSweep handles this automatically.

## Relevance to this project

Directly relevant as a working precedent for the "Local Shell Loop" / ADB-based device-control patterns described in the Omni Claw blueprint — specifically the scrcpy-style "push a native binary via ADB, talk over a local TCP socket" pattern that Omni Claw's tool-execution map also relies on (see `omni-claw-blueprint.md` §6). Also a concrete demonstration that a small C++ daemon plus ADB port-forwarding can outperform standard Android-PC bridging for filesystem-heavy tasks — a pattern potentially reusable for any future need to stream large local data (e.g. wiki/JSONL sync) off-device fast.

<details><summary>chunk layer (JSONL) + source URLs</summary>

```jsonl
{"id": "socketsweep-001", "section": "problem", "heading": "The Problem", "content": "Standard Android-to-PC USB file browsing uses MTP (Media Transfer Protocol), designed in 2008 for MP3 players. It transfers file metadata one item at a time with no caching, no parallel requests, and no fast recursive scan, making it unusable for scanning 100GB-plus of modern phone storage. Result: Calculating size hangs for 4-plus minutes, painfully slow navigation, no practical way to find large files. SocketSweep bypasses MTP entirely.", "source_urls": [], "tags": []}
{"id": "socketsweep-002", "section": "speed", "heading": "Speed", "content": "Full /sdcard scan on a Samsung Galaxy S24 Ultra (256GB, about 47,000 files) completes in about 6-15 seconds with a full interactive treemap ready to explore, best case 6.9 seconds with a warm cache. Scan time varies with device load (background apps, media indexing, thermal state). These are single-device benchmark figures from the project's own README, not independently reproduced; the README itself notes proper side-by-side benchmarks against OpenMTP and other tools are coming soon.", "source_urls": [], "tags": []}
{"id": "socketsweep-003", "section": "how-it-works", "heading": "How It Works", "content": "SocketSweep pushes a tiny C++ program (about 1MB) to the phone via ADB. That program scans the filesystem directly on the phone using native POSIX calls, which is the source of the speed (no MTP bottleneck). It streams results back to the PC over a TCP socket through the USB cable via ADB port forwarding, and renders an interactive treemap in a React frontend. The architecture is explicitly inspired by scrcpy's push-a-native-binary-via-ADB, communicate-over-a-local-socket pattern.", "source_urls": ["https://github.com/Genymobile/scrcpy"], "tags": []}
{"id": "socketsweep-004", "section": "architecture", "heading": "Architecture (Three Layers)", "content": "Host Desktop: React plus Recharts interactive dashboard, with a Rust/Tauri backend as command orchestrator. ADB Protocol: ADB port forwarding, TCP:5050 to TCP:5050. Android Device: C++17 daemon, headless socket server, POSIX filesystem access to /sdcard.", "source_urls": [], "tags": []}
{"id": "socketsweep-005", "section": "interaction-lifecycle", "heading": "Interaction Lifecycle", "content": "Sequence: Rust (Tauri) invokes init_daemon; pkill daemon for cleanup of any stale process; push daemon to /data/local/tmp via ADB; appops set MANAGE_EXTERNAL_STORAGE allow; nohup ./daemon starts it headless; adb forward tcp:5050 tcp:5050; a ping-retry loop (150ms) runs until an ACK connection is received (Connected!). For scanning: invoke run_scan with path /sdcard sends TCP command SCAN /sdcard, the daemon does a recursive fast POSIX traversal and streams a large JSON tree back, which React parses and renders as a treemap. For deletion: invoke delete_item with a path sends TCP command DELETE /sdcard/..., the daemon runs std::filesystem::remove_all, returns status ok, and the UI updates/rescans.", "source_urls": [], "tags": []}
{"id": "socketsweep-006", "section": "installation-dev-setup", "heading": "Installation and Development Setup", "content": "End-user installation: download the platform build (Windows .exe/.msi, macOS Apple Silicon .dmg, Linux AppImage/.deb; macOS note: since the build is ad-hoc signed, run xattr -cr /Applications/SocketSweep.app once after install), enable USB Debugging on the phone (Settings, About Phone, tap Build Number 7 times, then Settings, Developer Options, enable USB Debugging), then plug in the phone, open SocketSweep, click Connect (auto-pushes the daemon and sets everything up), click Scan, click any treemap block to drill down, and delete directly from the app -- no apps needed on the phone, no Wi-Fi setup, no root required. Development setup prerequisites: Node.js v18+, Rust v1.70+ with Cargo, Android NDK v26d or newer, Android SDK/ADB on PATH. Build steps: export NDK=/path/to/android-ndk-r26d, cd engine, bash ./build.sh (cross-compiles the C++ daemon for aarch64-linux-android, generating the stripped daemon binary in engine/), then cd .., npm install, npm run tauri dev (phone must be plugged in with USB Debugging enabled).", "source_urls": [], "tags": []}
{"id": "socketsweep-007", "section": "troubleshooting", "heading": "Troubleshooting", "content": "0 Files or missing folders on Android 11-plus: Scoped Storage restricts file access; SocketSweep tries to bypass automatically via adb shell appops set com.android.shell MANAGE_EXTERNAL_STORAGE allow, but if scanning still shows nothing, check OEM-specific toggles (e.g. Xiaomi needs USB Debugging Security settings enabled separately). Samsung Auto Blocker: if USB Debugging is greyed out on a Samsung device, Auto Blocker (Settings, Security, Auto Blocker) is likely on, disabling USB Debugging entirely and blocking any ADB-based tool including SocketSweep; it is off by default. Daemon fails to start / Permission denied: ensure the daemon is pushed to /data/local/tmp/ rather than /sdcard/, since modern Android blocks execution from /sdcard/; SocketSweep handles this automatically.", "source_urls": [], "tags": []}
{"id": "socketsweep-008", "section": "relevance", "heading": "Relevance to This Project", "content": "SocketSweep is a working precedent for the Local Shell Loop / ADB-based device-control patterns described in the Omni Claw blueprint, specifically the scrcpy-style push-a-native-binary-via-ADB, talk-over-a-local-TCP-socket pattern also used in Omni Claw's tool-execution map. It also demonstrates that a small C++ daemon plus ADB port-forwarding can outperform standard Android-PC bridging for filesystem-heavy tasks, a pattern potentially reusable for streaming large local data such as wiki/JSONL sync off-device quickly. Author: Vishnu Srivatsava, license GPL 3.0.", "source_urls": ["https://github.com/sponsors/VishnuSrivatsava", "https://youtu.be/ttsc6Xf6Xb4", "https://github.com/VishnuSrivatsava/SocketSweep/releases/tag/v1.0.0", "https://paypal.me/mathcuber", "https://www.linkedin.com/in/vishnu-srivatsava-642222238/"], "tags": []}
```

</details>

---

# Fragmented QAT

<sub>source folder: `knowledge/fragmented-qat/`</sub>

## `fraqat-paper.md`
Source: Drive file ID `10uJWwhRSMI_nrgWKJTi9coOc-rIV7NRj` ("Copy of 2510.14823v1.pdf") — research paper (arXiv:2510.14823v1 [cs.CV], 16 Oct 2025).

# FraQAT: Quantization Aware Training with Fractional Bits

**Authors**: Luca Morreale, Alberto Gil C. P. Ramos, Malcolm Chadwick, Mehdi Noroozi, Ruchika Chavhan, Abhinav Mehrotra, Sourav Bhattacharya — Samsung AI Center, Cambridge, UK.

> Note: all benchmark numbers below (FID, CLIP-FID, ImageReward, latency figures, GPU-hour counts, etc.) are the paper's own reported results and have not been independently verified.

## Abstract

State-of-the-art (SOTA) generative models have demonstrated impressive capabilities in image synthesis or text generation, often with a large capacity model. However, these large models cannot be deployed on smartphones due to the limited availability of on-board memory and computations. Quantization methods lower the precision of the model parameters, allowing for efficient computations, e.g., in INT8. Although aggressive quantization addresses efficiency and memory constraints, preserving the quality of the model remains a challenge.

To retain quality in previous aggressive quantization, the authors propose a new fractional bits quantization (FraQAT) approach. The novelty is a simple yet effective idea: progressively reduce the model's precision from 32 to 4 bits per parameter, and exploit the fractional bits during optimization to maintain high generation quality. FraQAT yields improved quality on a variety of diffusion models, including SD3.5-Medium, Sana, PixArt-Σ, and FLUX.1-schnell, while achieving 4–7% lower FID than standard QAT. Finally, the authors deploy and run Sana on a Samsung S25U, which runs on the Qualcomm SM8750-AB Snapdragon 8 Elite Hexagon Tensor Processor (HTP).

**Figure 1 callout**: FraQAT is a Quantization Aware Training (QAT) technique that grants generative models high fidelity at a fraction of the training time required. Large text-to-image (T2I) models quantized with FraQAT (W4A8) achieve 16% lower FID score than the state of the art (shown across Sana 600M, SD3.5-M, and Flux-schnell).

## 1. Introduction

Over the past few years, generative models have made impressive progress in synthesizing high-quality images [1, 2, 3] and texts [4, 5]. Such a breakthrough is partly achieved by enlarging the model's size, e.g., Diffusion Transformer (DiT) models with over 10 billion (10B) parameters are increasingly common. However, larger models require significantly more resources, hence higher inference-time or latency, even for inference. This increase is particularly problematic for deploying these models on resource-limited devices, e.g., smartphones, thus limiting their wide-scale usage.

A well-established approach to mitigate these resource constraints is quantization: by shifting parameters from 32 bits to a lower precision, e.g., 4 bits, the model's weight-allocation footprint in its computational graph is significantly reduced. While past quantization research aimed mostly at decreasing model size, low-precision hardware support, such as NPUs on smartphones, drives researchers to further decrease inference latency. For example, latency gains from reduced data movement are boosted by native support for low-precision operations, e.g., using 4-bit weights and 8-bit activations (W4A8). Although initially few devices offered support for these operations, modern hardware manufacturers readily offer low-precision operations across devices: W4A8 in Qualcomm Snapdragon HTP [6], INT8/BF16/FP16 in Intel CPUs [7], Block FP16 in AMD CPUs [8], and FP8/FP4 in NVIDIA GPU H100/H200 [9], to name a few.

The advantages of deploying cloud-quality generative models on-device are multi-fold: it preserves users' privacy while offering a low-latency experience. For service providers, it reduces operating costs by pushing compute from expensive servers to users' personal devices as well as avoiding violating country-specific privacy regulations. This work targets mobile deployment, and restricts itself to W4A8 given its ubiquitous availability across devices.

Quantization approaches fall under two main categories: Post Training Quantization (PTQ) and Quantization-Aware-Training (QAT). PTQ creates a low-precision model from a high-precision pre-trained model using a small calibration dataset. Recent progress in PTQ research has resulted in W4A32 and W8A32 high-quality quantized models from pre-trained SANA [10], SANA 1.5 [2], and SANA-Sprint [11]. Mixed-precision W4A32 and W16A32 approaches like SVDQuant [12] have also yielded high-quality quantized models from pre-trained FLUX.1-schnell. In essence, PTQ is ideal for cases where access to a large training dataset or compute cluster is limited.

Despite its success, PTQ requires careful data selection [13]. For example, a poorly selected calibration dataset may manifest in poor prompt adherence or exhibit color shifts during deployment. Instead, Quantization-Aware-Training (QAT) optimizes weights in lower precision to boost the overall model's performance [14, 15, 16]. In general, QAT approaches yield better results, at lower precision, when a large training dataset or compute cluster is available. Nonetheless, quantized models suffer from a quality loss compared to the original FP32 model.

The authors propose fractional bits quantization (FraQAT) to bridge the quality gap between the original and the quantized model. Inspired by Curriculum Learning [17], their training process progressively increases the quantization complexity, i.e., gradually lowers parameter precision, while replicating the original model's output. FraQAT reduces outliers, stabilizes training and yields improved prompt adherence and image generation quality (Section 2.2). They apply FraQAT to the linear layers of SOTA generative models as they contain the majority of the parameters, and empirically demonstrate the advantages of the proposed techniques on diffusion models (Sections 4.1, 4.2). In terms of image quality, FraQAT achieves 16% lower FID than SOTA QAT. To address computational costs, they perform an outlier analysis (Section 4.3), and selectively train a subset of the model's layers. Finally, they quantize and deploy a model on a Samsung S25U, running on Qualcomm SM8750-AB Snapdragon 8 Elite Hexagon Tensor Processor (HTP) (Section 4.4).

## 2. Method

### 2.1 Quantization Preliminaries

The goal of quantization is to approximate — in dynamic or static finite precision — internal model operations, such as operations within linear layers `x × W` where `x ∈ R^(B×m)` and `W ∈ R^(m×n)`. Depending on hardware support, the quantization operation `W_b` on a matrix `W` to `b` bits can be expressed with narrower range as:

```
Q(W)_b := round( (2^(b-1) - 1) / max_{i,j}|[W]_{i,j}| · W )  ∈ {-2^(b-1), ..., 2^(b-1) - 1}
S(W)_b := max_{i,j}|[W]_{i,j}| / (2^(b-1) - 1)  ∈ R+
W_b := S(W)_b · Q(W)_b                                                    (1)
```

or with wider range as:

```
Q(W)_b := round( 2^b · (W - w_min) / (w_max - w_min) )  ∈ {0, ..., 2^b - 1}
S(W)_b := (w_max - w_min) / 2^b  ∈ R+
W_b := S(W)_b · Q(W)_b + w_min                                            (2)
```

where `w_min := min_{i,j}[W]_{i,j}` and `w_max := max_{i,j}[W]_{i,j}`. Most simply for (1), matrix multiplications can be rewritten as: `x_bx · W_bW = (S(x)_bx · S(W)_bW)(Q(x)_bx · Q(W)_bW)` where `bx` and `bW` may differ. Therefore, matrix multiplication `x_bx W_bW` can be reduced to the multiplication of two floats `S(x)_bx S(W)_bW` and matrix multiplication of two integer matrices `Q(x)_bx Q(W)_bW`.

Dynamic quantization refers to the case where `w_min`/`w_max` are computed at runtime, per sample, based on the input. In static quantization, `w_min`/`w_max` are pre-computed and shared across all samples. Dynamic quantization, especially when applied to activations, allows robust handling of outliers since each sample range is computed to maximize representability. Static quantization is more restrictive and generates more outliers, making the quantization problem strictly harder. Edge devices, such as smartphones, only support static quantization, while GPUs support both.

Activations are often quantized through a look-up table mapping from a `2^b`-sized partition of the input range into a fixed number of quantized output values, e.g., the previous layer output `x`. In general, weights and activations may be quantized to different precisions, upcasted to the same precision before computation and downcasted after computation. The paper makes the number of bits in weights and activations explicit with subscripts, e.g., `x32` refers to a 32-bit approximation of `x`.

Due to restricted address spaces in most mobile accelerators, it is critical to decrease weights precision aggressively, especially in large vision or language models (e.g., 12B-parameter models), otherwise these models cannot even be placed on the target devices. However, naively lowering the weight's precision from FP32 to INT4 causes severe degradation in generated results — exacerbated by lowering activation precision, as required by integer-only accelerators (most often to INT8 for reduced generation latency). At a high level, the quality-degradation phenomenon is attributed to outliers in both activations and weights due to training. The overall challenge of quantization is to approximate the original network's behavior while lowering the precision:

```
x·W ≈ x8·W4.                                                              (3)
```

### 2.2 Fractional Quantization-Aware-Training

Intuitively, QAT approaches — including the proposed FraQAT — handle outliers, both in weights and activations, by shifting parameters to quantization centroids within or towards adjacent bins, hence re-distributing weights in a more compact space. Consequently, the further apart bins are, the harder the optimization problem. The authors further speculate that it is slower to optimize for lower precisions (INT4 vs FP32) as the gap between two adjacent representable numbers is much larger. This can be observed in Figure 2, where the loss is higher for lower precisions — outliers appear gradually as precision decreases.

**Figure 2 (Bit vs Loss)**: as precision (number of bits) is reduced, the average knowledge-distillation loss increases — the gap between student and teacher widens. From a quantization perspective, this implies outliers incrementally affect the student model. (Curves shown for SD3.5-M, Sana 600M, PixArt-Σ, Flux-schnell.)

**Figure 3 (Classic QAT vs FraQAT)**: Classic QAT first computes the loss at the lower precision, then propagates it back to the original precision and optimizes the weights — resulting in coarse and noisy gradients. Fractional Quantization Aware Training relies on intermediate precisions (from INT8 to INT4 as training progresses) to incrementally adjust to weight distributions; parameters smoothly shift between bins thanks to the finer gradients available at intermediate precision.

To address this issue, the authors take inspiration from Curriculum Learning [17] literature: they progressively increase the complexity of the task during optimization by gradually lowering weights' precision while approximating the full-precision model's output. This is achieved by two key designs: first, FraQAT leverages weights from pre-trained models. Second, FraQAT continuously steps between discrete quantization ranges to exploit the fact that Eq. (1)–(2) are purely a software construct — hence it is possible to span any continuous (not just discrete) precision `b ∈ [32, 1] ∈ R`.

Coupled together, these concepts establish FraQAT as a faster and higher-quality QAT scheme: Fractional Quantization Aware Training. Given a model, FraQAT progressively lowers the precision — first coarsely between FP32 and INT8, then finely from INT8 to INT4 — stepping through intermediate fractional bits during training (Figure 3, Algorithm 1). As training progresses, outliers gradually appear (Figure 2) and are addressed. By optimizing at fractional-bit precision in a curriculum fashion, FraQAT allows weights to move to stable configurations, yielding higher-quality samples and reducing training time. Throughout the entire training process, FraQAT keeps all activation quantization constant (INT8).

As training progresses, this progressive lowering of precision smoothly moves the weights' distribution, facilitating quantization (cf. Figure 3). It is even possible to set `b = 5.5`. Although half-bit precisions have no direct meaning, in practice they bridge the gap in the range of representable numbers between two precisions: `INT6 ∈ [-32, 31]`, `INT5.5 ∈ [-22, 21]`, `INT5 ∈ [-16, 15]`. In other words, half-bit precisions reduce the distance between adjacent bins, speeding up convergence without ad-hoc hyper-parameters such as learning rate.

The proposed FraQAT approach is generally applicable to any model and quantization level. Given the widespread usage of DiT and MM-DiT blocks in SOTA T2I models, the presentation focuses on DiT models. Since model-size limits must first be met for any on-device placement, FraQAT quantizes linear layers as they contain the bulk of the parameters of DiT models (99.9%). FraQAT targets the most aggressive W4A8 quantization, as it allows for a wider range of models to fit edge accelerators with the lowest generation latency — though the technique itself is applicable to any precision.

#### Algorithm 1 — Fractional Quantization Aware Training

```
Input:  Pre-trained model M_{W32A32}, dataset D, loss function L,
        quantization schedule B (e.g., {8, 5, 4.5, 4}), optimizer O
Output: Quantized model M_{W4A8}

1:  M_{WbA8} ← M_{W32A32}
2:  for b in B do
3:      M_{WbA8} ← QUANTIZE_LINEAR_LAYER(M_{WbA8}, W_bA8)
4:      for batch in D do
5:          O_{WbA8}  ← FORWARD(M_{WbA8}, batch)
6:          O_{W32A32} ← FORWARD(M_{W32A32}, batch)
7:          l ← L( stop_grad(O_{W32A32}), O_{WbA8} )
8:          OPTIMIZE(O, l, M_{WbA8})
9:      end for
10: end for
11: return M_{WbA8}
```

## 3. Related Works

Large diffusion models are the de-facto framework for image generation [18, 19, 20, 21]. Large Language Models (LLMs) show human-like abilities with text [4, 22, 23]. However, quality and diversity come at a cost: these models have a huge number of parameters and cannot be hosted on an on-device NPU without some form of quantization.

**Tackling computational complexity.** Diffusion models' computational complexity has two major sources: the number of denoising passes and the conditioning mechanism. The former can be addressed by distilling the model to few or a single pass [24, 25]; the latter by modeling the latent-noise space [26] to decrease the number of function evaluations. Despite the success of these approaches, a major bottleneck remains: the memory required for inference. Quantization aims to preserve the original model's quality when moving to lower precision — thus saving memory and enabling deployment.

**Quantization-Aware-Training.** QAT methods optimize model weights at lower precision [15, 27], aiming to recover the original performance. Early approaches [15, 27] study QAT on ResNet for classification: starting from low-bit precision (`b = 2` or `4`), weight quantization is progressively reduced [27] or selected at random [15]. Although [15, 27] closely relate to FraQAT, they (i) focus on classification networks, (ii) ignore the gap with full-precision models and the hierarchical nature of different precisions by starting from a low-bit quantization, and (iii) aim to get models at different precisions. In parallel, Fracbits [28] introduces bit-width optimization relying on a non-standard quantization formula for fractional bits — bit-widths are regularized to achieve the desired precision, followed by a binary search and fine-tuning process to finalize weights. However, this focuses on average bit length across layers, obtaining lower bits in some layers at the cost of higher bits in others, which may not map to readily available hardware; Fracbits also focuses on classification rather than generative tasks.

More recently, MatryoshkaQAT [14] exploits the nested structure of a number's byte representation to encode LLM weights at different precisions (8, 4, and 2); joint training at the three precisions results in a multi-precision model. Parallel to this work, Liu et al. [29] extend [30] and discover that models quantized to lower than 4 bits develop a different representation from the original models. Finally, [31], based on the Teaching Assistant distillation framework [32], quantize LLM models to W1A1. Similar to this work, the authors use a progressive strategy, however limited to W1A4 → W1A2 → W1A1, where intermediate models (W1A2) are used as teachers. Combined with a series of techniques (gradient clipping, elastic binarization, etc.) to stabilize optimization, they achieve a binary quantized model — but since the quantization is binary, the model is not deployable to edge devices. This work shows progressive quantization is enough to quantize a model that *can* be deployed on edge devices.

Related to diffusion models, Bitfusion [16] combines different QAT techniques (distillation and fine-tuning) to convert SD1.5 [19] to 1.99 bits. Wang et al. [33] selectively fine-tune SD1.5 to handle activation distribution. BinaryDM [34] applies a multi-stage QAT approach to model quantization. None of these techniques showcase low-bit quantization of large-scale DiT models such as SD3.5-M [1] (2.2B) or FLUX.1-schnell [3] (12B) — this work is presented as the first QAT approach applied to such models.

**Post-Training Quantization.** SmoothQuant [35] proposes a PTQ approach injecting a smoothing factor in linear layers to reduce the impact of outliers in LLMs. AWQ [36] and MobileQuant [37] extend this approach to lower precision to W4A8, enabling an LLM to run on-device. These works have been extended to DiT models with specific focus on timesteps: PTQ4DiT [38] builds a calibration dataset by sampling timesteps before quantizing the diffusion model; DiTAS [39] proposes a temporal-aggregated smoothing technique combined with LoRA and grid search to reduce quantization errors of small DiT networks with W4A8 quantization; QuEST [33] achieves W4A4 quantization through layer-specific PTQ fine-tuning; Q-DiT [40], inspired by [36, 41, 37], combines fine-grained group quantization with a novel automatic allocation algorithm to account for weights' spatial variance. Most recently, SVDQuant [12] and FBQuant [42] have shown impressive preservation of image-generation quality when quantizing FLUX.1-schnell [3] to W4A16, relying on a low-rank approximation of the original weights and a residual branch to absorb outliers.

## 4. Experiments

**Models.** The evaluation focuses on recent text-to-image (T2I) models given the increasing interest in lowering their computational requirements. The authors assess the approach over 4 diffusion models: SD3.5-Medium [1], Sana [10], PixArt-Σ [43], and FLUX.1-schnell [3]. These models span a wide range of parameters (0.6B–12B) and architectural innovations (linear and non-linear attention, DiT, MM-DiT, etc.).

In all experiments, they start from a pre-trained W32A32 model and, through FraQAT, reduce it to W4A8. The student is bootstrapped at INT8 and optimized to replicate its FP32 counterpart — minimizing the initial gap between teacher (FP32) and student (INT8). After `T` epochs, the model's precision is lowered and optimization continues, repeating until precision reaches 4 bits. Since a fake-quantization process is used, arbitrary precisions with no hardware support (e.g., INT4.5) can be emulated. Unless stated otherwise, all experiments follow the same progression: `8 → 7 → 6 → 5.5 → 5 → 4.75 → 4.5 → 4.25 → 4`, targeting linear layers. In all cases a W4A8 model is distilled via knowledge-distillation loss, using dynamic quantization. The approach is applicable to any quantization precision (e.g., W2A8) and to static quantization.

**Baselines.** Compared against SOTA PTQ techniques: DiTAS [39] (W4A8) and SVDQuant [12] (W4A16), using publicly available code, calibrated on the training dataset. To further validate FraQAT, the authors implement a vanilla QAT (vQAT) — W4A8 quantization applied to all linear layers, optimized with the same loss as FraQAT — and an SVDQuant-like QAT (SVDQAT), which injects a LoRA-like layer in all linear layers (as in [12]) and optimizes both the low-rank and residual branch, almost doubling the parameter count. Naive quantization results (Dynamic Q.) are also reported via torchao.

**Datasets.** All models are trained/calibrated on the YE-POP dataset, split 97.5%/2.5% train/validation. Quantized models are evaluated on the PixArt-Σ Evaluation dataset and the MidJourney HQ (MJHQ) Evaluation dataset. Training and evaluation generate 512×512 images.

**Metrics.** Image quality is measured with ImageReward (IR) [45]; feature-distribution disparity between generated samples of the quantized vs. original model is measured with FID [46] and CLIP-FID [47] — lower FID/CLIP-FID indicates higher similarity to the original model.

### 4.1 Quantitative Evaluation

Table 1 compares FraQAT across the five models/methods, two SOTA QAT approaches, and three PTQ techniques, evaluated on two test datasets. Due to memory requirements, some techniques could not be applied to Flux-schnell [3] (12B model).

SVDQuant, developed and optimized for Sana, PixArt-Σ, and FLUX.1-schnell, achieves lower performance on SD3.5-Medium (worse FID and CLIP-FID on both test datasets). Results for Dynamic Quantization and DiTAS are mixed on both datasets: DiTAS outperforms Dynamic Quantization on PixArt-Σ but is overall worse for SD3.5-Medium and Sana — these models have different architectures (DiT, MM-DiT, linear attention), suggesting DiTAS is sensitive to model family. Among QAT approaches, SVDQAT consistently outperforms vanilla QAT (vQAT) — arguably the increased parameter count (LoRA + residual branch) better copes with lower precision. FraQAT outperforms even the strongest QAT baseline developed (SVDQAT), with overall higher gains for SD3.5-Medium and PixArt-Σ on both test datasets.

**Table 1 — Qualitative/quantitative evaluation (FID / CLIP-FID / ImageReward) on PixArt-Σ Evaluation dataset and MJHQ dataset:**

*PixArt-Σ Evaluation dataset*

| Method | Precision | SD3.5 Medium FID↓ | SD3.5 Medium CLIP-FID↓ | SD3.5 Medium IR↑ | Sana 600M FID↓ | Sana 600M CLIP-FID↓ | Sana 600M IR↑ | PixArt-Σ FID↓ | PixArt-Σ CLIP-FID↓ | PixArt-Σ IR↑ | Flux-schnell FID↓ | Flux-schnell CLIP-FID↓ | Flux-schnell IR↑ |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Dynamic Q. | W4A8 | 9.36 | 2.08 | 0.56 | 2.22 | 0.24 | 0.57 | 13.35 | 6.19 | 0.35 | 8.17 | 1.13 | -0.73 |
| DiTAS | W4A8 | 27.93 | 13.77 | 0.41 | 12.87 | 4.58 | 0.62 | 7.30 | 3.95 | 0.84 | - | - | - |
| SVDQuant | W4A16 | 14.42 | 3.14 | 0.66 | 2.43 | 0.24 | 0.60 | 6.80 | 2.02 | 0.79 | 2.26 | 0.36 | 0.84 |
| SVDQAT | W4A8 | 2.57 | 0.28 | 0.80 | 1.93 | 0.13 | 0.48 | 5.38 | 1.48 | 0.76 | - | - | - |
| vQAT | W4A8 | 2.67 | 0.31 | 0.78 | 2.13 | 0.16 | 0.45 | 7.00 | 2.52 | 0.79 | 3.40 | 0.66 | 0.87 |
| **FraQAT** | W4A8 | **2.54** | **0.27** | **0.82** | **2.17** | **0.19** | **0.48** | **4.48** | **1.07** | **0.79** | **2.55** | **0.30** | **0.86** |

*MJHQ dataset*

| Method | Precision | SD3.5 Medium FID↓ | SD3.5 Medium CLIP-FID↓ | SD3.5 Medium IR↑ | Sana 600M FID↓ | Sana 600M CLIP-FID↓ | Sana 600M IR↑ | PixArt-Σ FID↓ | PixArt-Σ CLIP-FID↓ | PixArt-Σ IR↑ | Flux-schnell FID↓ | Flux-schnell CLIP-FID↓ | Flux-schnell IR↑ |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Dynamic Q. | W4A8 | 10.29 | 2.11 | 0.65 | 2.40 | 0.28 | 0.63 | 15.04 | 5.55 | 0.44 | 8.66 | 1.24 | -0.90 |
| DiTAS | W4A8 | 32.04 | 14.06 | 0.41 | 12.91 | 5.59 | 0.68 | 8.63 | 4.07 | 1.04 | - | - | - |
| SVDQuant | W4A16 | 15.10 | 3.06 | 0.78 | 2.48 | 0.25 | 0.62 | 6.95 | 1.71 | 0.99 | 2.41 | 0.41 | 0.96 |
| SVDQAT | W4A8 | 2.85 | 0.32 | 0.91 | 2.04 | 0.16 | 0.53 | 5.83 | 1.44 | 0.96 | - | - | - |
| vQAT | W4A8 | 3.01 | 0.37 | 0.89 | 2.13 | 0.20 | 0.47 | 7.38 | 2.12 | 0.99 | 3.56 | 0.73 | 0.99 |
| **FraQAT** | W4A8 | **2.78** | **0.32** | **0.96** | **2.34** | **0.24** | **0.50** | **4.95** | **1.054** | **0.97** | **2.55** | **0.39** | **0.99** |

### 4.2 Qualitative Evaluation

Figure 4 depicts, for each of the four models: the original model, one PTQ representative (SVDQuant — chosen for its popularity over DiTAS/Dynamic Quantization), one QAT alternative (vQAT — chosen over SVDQAT for its overall popularity), and FraQAT. Images in each row share the same seed and prompt (sourced from MJHQ [44]).

As expected from a PTQ approach, SVDQuant under-performs when generating certain high-frequency image details — especially visible when multiple faces are present, as shown in the first row. QAT improves high-frequency image details but generates significantly different images than the original model for the same prompt/seed. FraQAT preserves both high-frequency details and generates images closest to the original model across all baselines.

### 4.3 Outlier Analysis

Activation outliers disrupt the quantization process by introducing artifacts or biases. By analyzing outliers across different models, the authors find that different models produce outliers in different layers — e.g., in SD3.5-M outliers emerge after Feed-Forward (FF) layers, while in PixArt-Σ outliers arise mostly from Attention (Attn) layers (Figure 5). Selectively training specific layers can reduce FraQAT's computational demand while still obtaining a deployable model. The authors analyze the impact of selective training — optimizing only certain layer types (Attn, FF, Transformer Blocks/TF) while the rest of the network is frozen and quantized (W4A8), compared with training the entire network (Full/All).

**Table 2 — Outlier analysis (FID / CLIP-FID on PixArt-Σ evaluation dataset, W4A8):**

| Model | Layer | FID↓ | CLIP-FID↓ |
|---|---|---|---|
| SD3.5-M | FF | 2.23 | 0.23 |
| SD3.5-M | Attn | 2.32 | 0.24 |
| SD3.5-M | TF | 2.49 | 0.28 |
| SD3.5-M | All | 2.54 | 0.22 |
| Sana 600M | FF | 2.18 | 0.17 |
| Sana 600M | Attn | 2.10 | 0.16 |
| Sana 600M | TF | 2.13 | 0.16 |
| Sana 600M | All | 2.17 | 0.19 |
| PixArt-Σ | FF | 5.34 | 1.55 |
| PixArt-Σ | Attn | 6.48 | 2.23 |
| PixArt-Σ | TF | 4.40 | 1.13 |
| PixArt-Σ | All | 4.48 | 1.07 |

Quantitative results show no clear winner — layer preference differs by model architecture — but the authors recommend starting from quantizing Transformer Blocks (TF), as it reduces memory requirements, lowers computational demands, and addresses all outliers.

### 4.4 On-Device Model Deployment

To demonstrate feasibility of on-device deployment, Sana 600M [10] was quantized to W4A8 and deployed on a Samsung S25U (Qualcomm SM8750-AB Snapdragon 8 Elite Hexagon Tensor Processor / HTP). Compared to CPUs/GPUs, integer accelerators support a limited range of precisions and exclusively support static quantization for both weights and activations.

FraQAT supports both dynamic and static quantization paradigms (unlike the baselines used in Section 4.1). To apply FraQAT to Sana 600M with static weight/activation quantization, scale and offset are pre-computed via statistical analysis of the features: 100 random samples are passed through the DiT, feature values per layer are recorded, and used to compute standard deviation and mean (following [48], a 3-σ range is used for inliers). These scale/offset values are then used during QAT; the overall training procedure is otherwise unchanged from Section 4.1.

All linear layers of the quantized model run at W4A8 except the last layer, which runs at W4A16 — a good compromise to preserve quality without impacting latency. The resulting model has a latency of 66ms per forward step, versus 95ms for the same model at W4A16 (the bit-width supported by SVDQuant) — a **30.5% latency improvement**. On-device generated samples were compared with the original model's GPU output (Figure 6); the quantized model produces high-quality pictures resembling both the original model and its GPU version.

## 5. Limitations and Future Work

FraQAT is a step forward compared to SOTA QAT, but — like most QAT techniques — is more computationally expensive than PTQ. Compared with multi-precision LLM SOTA QAT approaches such as MatryoshkaQAT, FraQAT's quantized model is tailored to a single bit precision; multi-precision support is left for future work. The intermediate precision levels used are hand-picked; a future goal is to design an algorithm to select the most impactful precisions automatically.

The training scheme may benefit from regularizers such as weight decay and data augmentation — preliminary regularization tests on Sana 0.6B [10] show weight decay boosts performance by ~10%, though a proper investigation is left to future work. Finally, FraQAT's networks are currently optimized using knowledge distillation only; different losses (e.g., feature and task loss) may further boost image-generation quality.

## 6. Conclusions

FraQAT is a novel Quantization-Aware-Training technique that exploits fractional bits while progressively reducing parameter precision during quantization. This curriculum-learning strategy addresses outliers as they arise at different precisions, achieving more stable and faster training. Evaluated over a variety of SOTA DiT and MM-DiT models, the quantized models achieve superior performance — both qualitatively and quantitatively — compared to SOTA QAT approaches. Such improved quality, if deployed on-device, may boost mobile users' productivity, preserve their privacy, and enable generation of personalized content.

## References

1. S. AI, "Stable diffusion 3.5." https://stability.ai/news/introducing-stable-diffusion-3-5, 2024.
2. E. Xie, J. Chen, Y. Zhao, J. Yu, L. Zhu, C. Wu, Y. Lin, Z. Zhang, M. Li, J. Chen, H. Cai, B. Liu, D. Zhou, and S. Han, "SANA 1.5: Efficient Scaling of Training-Time and Inference-Time Compute in Linear Diffusion Transformer," arXiv:2501.18427, 2025.
3. B. F. Labs, "Flux." https://github.com/black-forest-labs/flux, 2024.
4. A. Grattafiori et al., "The llama 3 herd of models," arXiv preprint arXiv:2407.21783, 2024.
5. G. Team et al., "Gemma 3 technical report," arXiv preprint arXiv:2503.19786, 2025.
6. Q. Snapdragon®, "Snapdragon 8 Elite Mobile Platform." https://docs.qualcomm.com/bundle/publicresource/87-83196-1_REV_D_Snapdragon_8_Elite_Mobile_Platform_Product_Brief.pdf.
7. X. Intel®, "Processors with Performance-Cores (P-Cores)." https://www.intel.com/content/www/us/en/products/details/processors/xeon/xeon6-p-cores.html.
8. A. Ryzen™, "AI 300 Series Processors." https://www.amd.com/content/dam/amd/en/documents/partner-hub/ryzen/amd-ryzen-ai-300-series-vs-qualcomm-snapdragon-x-elite-deck.pdf.
9. N. HGX™, "NVIDIA HGX Platform." https://www.nvidia.com/en-gb/data-center/hgx.
10. E. Xie et al., "SANA: Efficient High-Resolution Image Synthesis with Linear Diffusion Transformers," arXiv:2410.10629, 2024.
11. J. Chen et al., "SANA-Sprint: One-Step Diffusion with Continuous-Time Consistency Distillation," arXiv:2503.09641, 2025.
12. M. Li et al., "SVDQuant: Absorbing Outliers by Low-Rank Components for 4-Bit Diffusion Models," arXiv:2411.05007, 2024.
13. Z. Zhang, Y. Gao, J. Fan, Z. Zhao, Y. Yang, and S. Yan, "Selectq: Calibration data selection for post-training quantization," Machine Intelligence Research, pp. 1–12, 2025.
14. P. Nair, P. Datta, J. Dean, P. Jain, and A. Kusupati, "Matryoshka Quantization," arXiv:2502.06786, 2025.
15. A. Bulat and G. Tzimiropoulos, "Bit-Mixer: Mixed-precision networks with runtime bit-width selection," in ICCV, pp. 5188–5197, 2021.
16. Y. Sui et al., "Bitsfusion: 1.99 bits weight quantization of diffusion model," arXiv:2406.04333, 2024.
17. Y. Bengio, J. Louradour, R. Collobert, and J. Weston, "Curriculum learning," in Proceedings of the 26th annual international conference on machine learning, pp. 41–48, 2009.
18. P. Dhariwal and A. Nichol, "Diffusion models beat gans on image synthesis," NeurIPS vol. 34, pp. 8780–8794, 2021.
19. R. Rombach, A. Blattmann, D. Lorenz, P. Esser, and B. Ommer, "High-Resolution Image Synthesis with Latent Diffusion Models," in CVPR, pp. 10684–10695, 2022.
20. Y. Lipman, R. T. Q. Chen, H. Ben-Hamu, M. Nickel, and M. Le, "Flow Matching for Generative Modeling," arXiv:2210.02747, 2023.
21. I. Gat et al., "Discrete flow matching," NeurIPS vol. 37, pp. 133345–133385, 2024.
22. J. Achiam et al., "Gpt-4 technical report," arXiv preprint arXiv:2303.08774, 2023.
23. G. Team et al., "Gemini: a family of highly capable multimodal models," arXiv preprint arXiv:2312.11805, 2023.
24. T. Salimans and J. Ho, "Progressive distillation for fast sampling of diffusion models," arXiv preprint arXiv:2202.00512, 2022.
25. M. Noroozi, I. Hadji, B. Martinez, A. Bulat, and G. Tzimiropoulos, "You Only Need One Step: Fast Super-Resolution with Stable Diffusion via Scale Distillation," in ECCV, pp. 145–161, Springer, 2025.
26. M. Noroozi, A. G. Ramos, L. Morreale, R. Chavhan, M. Chadwick, A. Mehrotra, and S. Bhattacharya, "Guidance free image editing via explicit conditioning," arXiv preprint arXiv:2503.17593, 2025.
27. Q. Jin, L. Yang, and Z. Liao, "AdaBits: Neural Network Quantization with Adaptive Bit-Widths," in CVPR, pp. 2146–2156, 2020.
28. L. Yang and Q. Jin, "Fracbits: Mixed precision quantization via fractional bit-widths," in Proceedings of the AAAI Conference on Artificial Intelligence, vol. 35, pp. 10612–10620, 2021.
29. Z. Liu et al., "ParetoQ: Scaling Laws in Extremely Low-bit LLM Quantization," arXiv:2502.02631, 2025.
30. M. Nagel, M. Fournarakis, Y. Bondarenko, and T. Blankevoort, "Overcoming Oscillations in Quantization-Aware Training," in ICML, pp. 16318–16330, PMLR, 2022.
31. Z. Liu et al., "Bit: Robustly binarized multi-distilled transformer," NeurIPS vol. 35, pp. 14303–14316, 2022.
32. S. I. Mirzadeh, M. Farajtabar, A. Li, N. Levine, A. Matsukawa, and H. Ghasemzadeh, "Improved knowledge distillation via teacher assistant," in AAAI, vol. 34, pp. 5191–5198, 2020.
33. H. Wang, Y. Shang, Z. Yuan, J. Wu, J. Yan, and Y. Yan, "QuEST: Low-bit Diffusion Model Quantization via Efficient Selective Finetuning," arXiv:2402.03666, 2024.
34. X. Zheng et al., "BinaryDM: Accurate Weight Binarization for Efficient Diffusion Models," arXiv:2404.05662, 2024.
35. G. Xiao, J. Lin, M. Seznec, H. Wu, J. Demouth, and S. Han, "SmoothQuant: Accurate and Efficient Post-Training Quantization for Large Language Models," arXiv:2211.10438, 2024.
36. J. Lin et al., "AWQ: Activation-aware Weight Quantization for On-device LLM Compression and Acceleration," Proceedings of Machine Learning and Systems, vol. 6, pp. 87–100, 2024.
37. F. Tan et al., "MobileQuant: Mobile-friendly Quantization for On-device Language Models," arXiv:2408.13933, 2024.
38. J. Wu, H. Wang, Y. Shang, M. Shah, and Y. Yan, "PTQ4DiT: Post-training Quantization for Diffusion Transformers," arXiv:2405.16005, 2024.
39. Z. Dong and S. Q. Zhang, "DiTAS: Quantizing Diffusion Transformers via Enhanced Activation Smoothing," arXiv:2409.07756, 2024.
40. L. Chen et al., "Q-DiT: Accurate Post-Training Quantization for Diffusion Transformers," arXiv:2406.17343, 2024.
41. Y. Zhao et al., "Atom: Low-bit quantization for efficient and accurate llm serving," Proceedings of Machine Learning and Systems, vol. 6, pp. 196–209, 2024.
42. Y. Liu et al., "FBQuant: FeedBack Quantization for Large Language Models," arXiv:2501.16385, 2025.
43. J. Chen et al., "Pixart-σ: Weak-to-strong training of diffusion transformer for 4k text-to-image generation," in ECCV, pp. 74–91, Springer, 2024.
44. D. Li, A. Kamko, E. Akhgari, A. Sabet, L. Xu, and S. Doshi, "Playground v2.5: Three insights towards enhancing aesthetic quality in text-to-image generation," arXiv preprint arXiv:2402.17245, 2024.
45. J. Xu et al., "Imagereward: Learning and evaluating human preferences for text-to-image generation," NeurIPS vol. 36, pp. 15903–15935, 2023.
46. C. Szegedy, V. Vanhoucke, S. Ioffe, J. Shlens, and Z. Wojna, "Rethinking the inception architecture for computer vision," in CVPR, pp. 2818–2826, 2016.
47. T. Kynkäänniemi, T. Karras, M. Aittala, T. Aila, and J. Lehtinen, "The role of imagenet classes in fréchet inception distance," arXiv preprint arXiv:2203.06026, 2022.
48. R. Wang et al., "Optimizing large language model training using fp4 quantization," arXiv preprint arXiv:2501.17116, 2025.
49. Z. Lin et al., "Evaluating text-to-visual generation with image-to-text generation," arXiv preprint arXiv:2404.01291, 2024.
50. J. Wang, K. C. Chan, and C. C. Loy, "Exploring clip for assessing the look and feel of images," in AAAI, vol. 37, pp. 2555–2563, 2023.
51. G. Team, "Gemma," 2024.
52. C. Raffel et al., "Exploring the limits of transfer learning with a unified text-to-text transformer," Journal of machine learning research, vol. 21, no. 140, pp. 1–67, 2020.
53. C. Clark, K. Lee, M.-W. Chang, T. Kwiatkowski, M. Collins, and K. Toutanova, "Boolq: Exploring the surprising difficulty of natural yes/no questions," arXiv preprint arXiv:1905.10044, 2019.
54. A. Talmor, J. Herzig, N. Lourie, and J. Berant, "Commonsenseqa: A question answering challenge targeting commonsense knowledge," arXiv preprint arXiv:1811.00937, 2018.

## Appendix A — Experimental Evaluation

### A.1 Baselines

For state-of-the-art baselines, the authors rely on code released by the respective authors of SVDQuant (https://github.com/mit-han-lab/deepcompressor) and DiTAS (https://github.com/DZY122/DiTAS), using default parameters. For all approaches, pre-trained models with default resolution 512×512 are used, with baseline configurations changed where needed to match the same model.

### A.2 Hyper-Parameters for QAT

All QAT experiments use FuseAdam as optimizer, optimizing for 25 epochs. All experiments run on AMD MI300X, implemented using PyTorch (https://pytorch.org/), Lightning (https://lightning.ai/docs/pytorch/stable/), and torchao (https://github.com/pytorch/ao), with seed 1234.

**Table 3 — Hyper-parameters:**

| Method | Precision | SD3.5-M lr | SD3.5-M batch size | SD3.5-M low rank | Sana 600M lr | Sana 600M batch size | Sana 600M low rank | PixArt-Σ lr | PixArt-Σ batch size | PixArt-Σ low rank |
|---|---|---|---|---|---|---|---|---|---|---|
| SVDQAT | W4A8 | 10⁻⁵ | 128 | 32 | 10⁻⁶ | 128 | 16 | 10⁻⁶ | 256 | 16 |
| vQAT | W4A8 | 10⁻⁵ | 256 | - | 10⁻⁶ | 128 | - | 10⁻⁶ | 128 | - |
| FraQAT | W4A8 | 10⁻⁶ | 256 | - | 10⁻⁷ | 128 | - | 10⁻⁶ | 128 | - |

For all FraQAT experiments, the schedule in Table 4 is followed.

**Table 4 — Precision schedule:**

| Precision | 8 | 7 | 6 | 5.5 | 5 | 4.75 | 4.5 | 4.25 | 4 |
|---|---|---|---|---|---|---|---|---|---|
| # epochs | 1 | 1 | 1 | 1 | 1 | 2 | 2 | 2 | 14 |

Experiments with this configuration take on average 192 GPUh for Sana, 576 GPUh for PixArt-Σ, and 1008 GPUh for SD3.5-Medium.

### A.3 Qualitative Evaluation

Additional qualitative evaluation on the MJHQ dataset [44] is provided as HTML pages in an accompanying zip file (not included in this text extraction).

### A.4 Quantitative Evaluation

Additional evaluation uses a wider set of metrics: VQA [49] to measure adherence of generated samples to input prompts, and CLIP-IQA [50] to measure image quality.

**Table 5 — Extended qualitative/quantitative evaluation (FID / CLIP-FID / CLIP-IQA / ImageReward / VQA score) on PixArt-Σ Evaluation dataset and MJHQ dataset:**

*PixArt-Σ Evaluation dataset*

| Method | Precision | SD3.5-M FID↓ | CLIP-FID↓ | CLIP-IQA↑ | IR↑ | VQA↑ | Sana 600M FID↓ | CLIP-FID↓ | CLIP-IQA↑ | IR↑ | VQA↑ | PixArt-Σ FID↓ | CLIP-FID↓ | CLIP-IQA↑ | IR↑ | VQA↑ | Flux-schnell FID↓ | CLIP-FID↓ | CLIP-IQA↑ | IR↑ | VQA↑ |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Dynamic Q. | W4A8 | 9.36 | 2.08 | 0.44 | 0.56 | 0.84 | 2.22 | 0.24 | 0.46 | 0.57 | 0.82 | 13.35 | 6.19 | 0.44 | 0.35 | 0.82 | 8.17 | 1.13 | 0.43 | -0.73 | 0.77 |
| DiTAS | W4A8 | 27.93 | 13.77 | 0.47 | 0.41 | 0.82 | 12.87 | 4.58 | 0.45 | 0.62 | 0.82 | 7.30 | 3.95 | 0.46 | 0.84 | 0.86 | - | - | - | - | - |
| SVDQuant | W4A16 | 14.42 | 3.14 | 0.42 | 0.66 | 0.85 | 2.43 | 0.24 | 0.43 | 0.60 | 0.82 | 6.80 | 2.02 | 0.43 | 0.79 | 0.86 | 2.26 | 0.36 | 0.42 | 0.84 | 0.85 |
| SVDQAT | W4A8 | 2.57 | 0.28 | 0.45 | 0.80 | 0.85 | 1.93 | 0.13 | 0.43 | 0.48 | 0.82 | 5.38 | 1.48 | 0.43 | 0.76 | 0.86 | - | - | - | - | - |
| vQAT | W4A8 | 2.67 | 0.31 | 0.44 | 0.78 | 0.85 | 2.13 | 0.16 | 0.43 | 0.45 | 0.81 | 7.00 | 2.52 | 0.45 | 0.79 | 0.85 | 3.40 | 0.66 | 0.41 | 0.87 | 0.86 |
| **FraQAT** | W4A8 | **2.54** | **0.27** | **0.45** | **0.82** | **0.86** | **2.17** | **0.19** | **0.42** | **0.48** | **0.82** | **4.48** | **1.07** | **0.45** | **0.79** | **0.86** | **2.55** | **0.30** | **0.41** | **0.86** | **0.85** |

*MJHQ dataset*

| Method | Precision | SD3.5-M FID↓ | CLIP-FID↓ | CLIP-IQA↑ | IR↑ | VQA↑ | Sana 600M FID↓ | CLIP-FID↓ | CLIP-IQA↑ | IR↑ | VQA↑ | PixArt-Σ FID↓ | CLIP-FID↓ | CLIP-IQA↑ | IR↑ | VQA↑ | Flux-schnell FID↓ | CLIP-FID↓ | CLIP-IQA↑ | IR↑ | VQA↑ |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Dynamic Q. | W4A8 | 10.29 | 2.11 | 0.44 | 0.65 | 0.79 | 2.40 | 0.28 | 0.45 | 0.63 | 0.74 | 15.04 | 5.55 | 0.43 | 0.44 | 0.74 | 8.66 | 1.24 | 0.42 | -0.90 | 0.65 |
| DiTAS | W4A8 | 32.04 | 14.06 | 0.47 | 0.41 | 0.73 | 12.91 | 5.59 | 0.45 | 0.68 | 0.75 | 8.63 | 4.07 | 0.46 | 1.04 | 0.80 | - | - | - | - | - |
| SVDQuant | W4A16 | 15.10 | 3.06 | 0.42 | 0.78 | 0.78 | 2.48 | 0.25 | 0.42 | 0.62 | 0.75 | 6.95 | 1.71 | 0.43 | 0.99 | 0.80 | 2.41 | 0.41 | 0.42 | 0.96 | 0.79 |
| SVDQAT | W4A8 | 2.85 | 0.32 | 0.45 | 0.91 | 0.80 | 2.04 | 0.16 | 0.43 | 0.53 | 0.74 | 5.83 | 1.44 | 0.43 | 0.96 | 0.81 | - | - | - | - | - |
| vQAT | W4A8 | 3.01 | 0.37 | 0.44 | 0.89 | 0.80 | 2.13 | 0.20 | 0.43 | 0.47 | 0.74 | 7.38 | 2.12 | 0.44 | 0.99 | 0.80 | 3.56 | 0.73 | 0.41 | 0.99 | 0.80 |
| **FraQAT** | W4A8 | **2.78** | **0.32** | **0.45** | **0.96** | **0.81** | **2.34** | **0.24** | **0.42** | **0.50** | **0.74** | **4.95** | **1.05** | **0.44** | **0.97** | **0.80** | **2.55** | **0.39** | **0.41** | **0.99** | **0.80** |

Table 5 shows FraQAT outperforms even the strongest QAT baseline developed (SVDQAT), with overall higher gains for SD3.5-Medium and PixArt-Σ on both test datasets.

### A.5 Quantization Schedule

To validate the benefits of the fractional quantization schedule (Table 4), it is compared against an integer-only counterpart (`8 → 7 → 6 → 5 → 4`) and a simpler progressive schedule (`16 → 8 → 4`), all under the same computational budget. Validation loss is measured across training (Figure 7): the integer and simple schedules perform comparably to each other, while the fractional schedule consistently outperforms both, resulting in a sensibly lower validation loss (evaluated on SD3.5-M).

**Figure 7 (Fractional schedule)**: SD3.5-M trained with a simple progressive schedule (16→8→4), an integer schedule (8→7→6→5→4), and a fractional schedule (8→7→6→5.5→5→4.75→4.5→4.25→4). The fractional schedule achieves the lowest validation loss.

## Appendix B — Additional Evaluation

### B.1 Language Model

The method is architecture- and application-agnostic. FraQAT is applied to Gemma2 2B IT [51]: starting from the FP16 (original) model, it is quantized to 4 bits following the same schedule as Appendix A.2. The quantized model (W4A8) is compared against the original.

Training uses a subset of the C4 dataset [52]: 384K samples randomly selected for training, 38.4K for validation. Evaluation is zero-shot on two datasets: BoolQ [53] and CommonsenseQA [54]/Social IQA. Table 6 shows a minimal quality drop when FraQAT is applied to Gemma2 2B IT, demonstrating the technique generalizes beyond vision models to language models.

**Table 6 — Evaluation on Language Models:**

| Model | Precision | CommonSense QA↑ | BoolQ↑ | COQA↑ |
|---|---|---|---|---|
| Original | W16A32 | 0.70 ± 0.01 | 0.76 ± 0.01 | 0.66 ± 0.01 |
| FraQAT | W4A8 | 0.69 ± 0.01 | 0.72 ± 0.01 | 0.70 ± 0.01 |

## Relevance to this project

FraQAT is directly relevant to the Omni Claw / Novus Agenti on-device stack: it is a Samsung AI Center paper demonstrating a QAT technique that reduces generative model precision to W4A8 and deploys it on a Samsung S25U running the Qualcomm Snapdragon 8 Elite Hexagon Tensor Processor (HTP) — the same class of Qualcomm Hexagon NPU hardware referenced for the Rubik Pi 3 (Dragonwing QCS6490 / Hexagon 770) secondary hub described in the Edge AI Hub Integration dossier. The paper's core finding — that a curriculum-style progressive bit-precision schedule (fractional bits, e.g. INT5.5) yields more stable training and 4–7% lower FID than standard QAT at W4A8 — is a candidate technique for any future on-device fine-tuning or quantization work done on the Jetson Orin Nano hub (LoRA fine-tuning role) before deploying models to Hexagon-NPU-based edge devices.

<details><summary>chunk layer (JSONL) + source URLs</summary>

```jsonl
{"id": "fraqat-paper-001", "section": "abstract", "heading": "Abstract", "content": "State-of-the-art generative models demonstrate impressive image synthesis and text generation capabilities, typically at large model capacity, but such large models cannot be deployed on smartphones due to limited on-board memory and compute. Quantization lowers parameter precision (e.g. to INT8) for efficient computation, but aggressive quantization threatens model quality. The authors propose fractional bits quantization (FraQAT): progressively reducing a model's precision from 32 to 4 bits per parameter while exploiting fractional bit levels during optimization to preserve generation quality. FraQAT improves quality across diffusion models including SD3.5-Medium, Sana, PixArt-Sigma, and FLUX.1-schnell, achieving 4-7% lower FID than standard QAT, and is deployed and run on a Samsung S25U using the Qualcomm SM8750-AB Snapdragon 8 Elite Hexagon Tensor Processor (HTP).", "source_urls": [], "tags": []}
{"id": "fraqat-paper-002", "section": "introduction", "heading": "1. Introduction", "content": "Generative models have progressed rapidly in image and text synthesis partly by enlarging model size, with Diffusion Transformer (DiT) models now exceeding 10 billion parameters. Larger models require more resources and higher inference latency, which is especially problematic for resource-limited devices like smartphones. Quantization mitigates this by shifting parameters from 32-bit to lower precision (e.g. 4-bit), reducing weight footprint. Modern hardware increasingly supports low-precision operations natively: W4A8 on Qualcomm Snapdragon HTP, INT8/BF16/FP16 on Intel CPUs, Block FP16 on AMD CPUs, and FP8/FP4 on NVIDIA H100/H200 GPUs. On-device deployment of cloud-quality generative models preserves user privacy, lowers latency, reduces service-provider operating costs, and avoids regulatory issues from moving user data across borders. This work targets mobile deployment and focuses on W4A8 given its wide hardware availability. Quantization splits into Post-Training Quantization (PTQ), which calibrates a low-precision model from a pretrained high-precision model using a small dataset, and Quantization-Aware-Training (QAT), which optimizes weights directly at lower precision using a larger dataset or compute cluster, generally yielding better quality but requiring more resources. FraQAT is proposed to bridge the quality gap between original and quantized models by progressively lowering precision during training (inspired by Curriculum Learning) while replicating the original model's output, reducing outliers, stabilizing training, and improving prompt adherence and generation quality. FraQAT is applied to the linear layers of state-of-the-art generative models, achieving 16% lower FID than SOTA QAT, and is validated via outlier analysis and on-device deployment on a Samsung S25U with Qualcomm Snapdragon 8 Elite HTP.", "source_urls": [], "tags": []}
{"id": "fraqat-paper-003", "section": "method-quantization-preliminaries", "heading": "2.1 Quantization preliminaries", "content": "Quantization approximates internal model operations such as linear-layer matrix multiplication x times W in dynamic or static finite precision. Given a matrix W, the quantization operator to b bits can be defined with a narrower symmetric range (mapping into signed integers scaled by the maximum absolute value) or a wider range using the minimum and maximum values of W, each accompanied by a scale factor S(W)_b. Matrix multiplication of quantized tensors reduces to multiplying two scale floats and multiplying two integer matrices. Dynamic quantization computes the min/max range per sample at runtime, robustly handling outliers; static quantization pre-computes and shares the range across all samples, which is more restrictive and produces more outliers, making the quantization problem strictly harder. Edge devices such as smartphones only support static quantization, while GPUs support both. Activations are typically quantized via a look-up table mapping a 2^b sized partition of the input range to quantized output values. Weights and activations may be quantized to different precisions and are upcast/downcast around computation. Aggressive precision reduction (FP32 to INT4, with activations often down to INT8 for integer-only accelerators) causes severe degradation attributed to outliers arising during training; the overall quantization challenge is approximating the original network's behavior (x times W) with the lower-precision approximation (x8 times W4).", "source_urls": [], "tags": []}
{"id": "fraqat-paper-004", "section": "method-fractional-qat", "heading": "2.2 Fractional Quantization-Aware-Training", "content": "QAT approaches, including FraQAT, handle outliers by shifting parameters toward quantization centroids in adjacent bins; the further apart bins are, the harder the optimization problem, and lower precisions (e.g. INT4 vs FP32) are slower to optimize because the gap between adjacent representable numbers is larger, which is empirically shown by rising average knowledge-distillation loss as precision decreases (Figure 2). Classic QAT computes loss at the target low precision directly and propagates gradients back, yielding coarse and noisy gradients. FraQAT instead takes inspiration from Curriculum Learning: it progressively increases task complexity by gradually lowering weight precision while approximating the full-precision model's output, achieved via two design choices -- leveraging pretrained weights, and continuously stepping between discrete quantization ranges since the quantization formulas are a pure software construct that can span any continuous precision b in [32,1]. FraQAT progressively lowers precision, first coarsely from FP32 to INT8, then finely from INT8 to INT4, stepping through intermediate fractional bits (e.g. INT5.5) during training (see Algorithm 1). Activation quantization is held constant at INT8 throughout. Half-bit precisions have no direct hardware meaning but bridge the gap in representable-number range between adjacent integer precisions (e.g. INT6 in [-32,31], INT5.5 in [-22,21], INT5 in [-16,15]), speeding convergence without extra hyperparameters. FraQAT is applied to the linear layers of DiT/MM-DiT models since these contain 99.9% of parameters, targeting the aggressive W4A8 precision for the widest edge-accelerator compatibility and lowest latency, though the technique generalizes to any precision.", "source_urls": [], "tags": []}
{"id": "fraqat-paper-005", "section": "algorithm-1", "heading": "Algorithm 1: Fractional Quantization Aware Training", "content": "Given a pretrained full-precision model M at W32A32, a dataset D, a loss function L, a quantization schedule B such as {8, 5, 4.5, 4}, and an optimizer O, FraQAT initializes the quantized model from the pretrained weights, then for each precision level b in the schedule it quantizes the linear layers to WbA8 and, for each batch, computes the forward pass of both the current quantized model and the frozen full-precision teacher model, computes a loss between the teacher's stop-gradient output and the quantized student's output, and optimizes the quantized model's weights. This repeats across all precision levels in the schedule until the final W4A8 model is produced and returned.", "source_urls": [], "tags": []}
{"id": "fraqat-paper-006", "section": "related-works", "heading": "3. Related works", "content": "Large diffusion models dominate image generation while LLMs show human-like text abilities, but both require quantization for on-device NPU hosting given their parameter counts. Diffusion model computational complexity stems from denoising pass count (addressed via distillation to fewer steps) and conditioning mechanisms (addressed via latent-noise modeling), but the memory bottleneck for inference remains, motivating quantization. Prior QAT methods (Bit-Mixer, AdaBits) study classification networks like ResNet at low bit precision, progressively reduced or randomly selected, but ignore the gap to full-precision models and focus on classification rather than generation. Fracbits introduces bit-width optimization via a non-standard fractional-bit formula with binary search and fine-tuning, but averages bit length across layers in ways that may not map to available hardware, and also targets classification. MatryoshkaQAT exploits nested byte representations to jointly train LLM weights at 8/4/2-bit precision for a multi-precision model. Liu et al. discover that models quantized below 4 bits develop representations different from the original. A Teaching-Assistant-distillation-based approach quantizes LLMs progressively from W1A4 to W1A2 to W1A1 using intermediate models as teachers, but the resulting binary model is not edge-deployable, unlike FraQAT's W4A8 result. For diffusion models specifically, Bitfusion converts SD1.5 to 1.99 bits via combined QAT techniques, Wang et al. selectively fine-tune SD1.5 for activation distribution, and BinaryDM applies multi-stage QAT -- but none of these apply low-bit quantization to large-scale DiT models like SD3.5-Medium (2.2B) or FLUX.1-schnell (12B), which the authors claim as a first for QAT. On the PTQ side, SmoothQuant, AWQ, and MobileQuant reduce LLM precision to W4A8 for on-device use via smoothing factors; PTQ4DiT, DiTAS, QuEST, and Q-DiT extend PTQ techniques (calibration by timestep sampling, temporal-aggregated smoothing with LoRA, selective fine-tuning, fine-grained group quantization) to DiT models; SVDQuant and FBQuant achieve strong image quality when quantizing FLUX.1-schnell to W4A16 using low-rank approximation and residual branches to absorb outliers.", "source_urls": [], "tags": []}
{"id": "fraqat-paper-007", "section": "experiments-setup", "heading": "4. Experiments -- models, baselines, datasets, metrics", "content": "Evaluation covers four text-to-image diffusion models spanning 0.6B-12B parameters and multiple architectures: SD3.5-Medium, Sana, PixArt-Sigma, and FLUX.1-schnell. All experiments start from a pretrained W32A32 model, bootstrap the student at INT8, and progressively reduce precision through the schedule 8 to 7 to 6 to 5.5 to 5 to 4.75 to 4.5 to 4.25 to 4, targeting linear layers, using knowledge-distillation loss and dynamic quantization, ultimately producing a W4A8 model. Baselines include PTQ methods DiTAS (W4A8) and SVDQuant (W4A16) using publicly released code calibrated on the training set, plus two QAT baselines built by the authors: vanilla QAT (vQAT), applying W4A8 to all linear layers with the same loss as FraQAT, and SVDQuant-like QAT (SVDQAT), injecting LoRA-like layers into all linear layers and optimizing both low-rank and residual branches (nearly doubling parameters). A naive per-torchao dynamic quantization baseline is also reported. All models are trained/calibrated on the YE-POP dataset (97.5/2.5 train/validation split) and evaluated on the PixArt-Sigma Evaluation dataset and the MidJourney HQ (MJHQ) dataset at 512x512 resolution, using ImageReward for perceptual quality and FID/CLIP-FID for distributional similarity to the original model.", "source_urls": [], "tags": []}
{"id": "fraqat-paper-008", "section": "quantitative-evaluation", "heading": "4.1 Quantitative evaluation", "content": "Across both the PixArt-Sigma Evaluation dataset and MJHQ dataset, FraQAT (W4A8) achieves the best or near-best FID, CLIP-FID, and ImageReward scores among all compared methods, outperforming the strongest QAT baseline (SVDQAT) with particularly larger gains on SD3.5-Medium and PixArt-Sigma. SVDQuant, tuned originally for Sana/PixArt-Sigma/FLUX, underperforms on SD3.5-Medium. DiTAS and plain Dynamic Quantization show mixed, architecture-dependent results, with DiTAS beating Dynamic Quantization on PixArt-Sigma but losing on SD3.5-Medium and Sana, suggesting sensitivity to model family (DiT vs MM-DiT vs linear attention). SVDQAT consistently beats vanilla QAT (vQAT), likely due to its extra LoRA/residual parameters better absorbing lower-precision error. Full numeric results are reported in Table 1 (main text) and Table 5 (appendix, with added CLIP-IQA and VQA metrics), covering Dynamic Q., DiTAS, SVDQuant, SVDQAT, vQAT, and FraQAT at W4A8/W4A16 across all four models and two evaluation datasets; some techniques could not be applied to the 12B FLUX.1-schnell model due to memory constraints.", "source_urls": [], "tags": []}
{"id": "fraqat-paper-009", "section": "qualitative-evaluation", "heading": "4.2 Qualitative evaluation", "content": "Comparing generated images across the original model, SVDQuant (PTQ representative), vQAT (QAT alternative), and FraQAT under matched seeds and prompts, SVDQuant under-performs on high-frequency image details -- especially visible with multiple faces in a scene. vQAT improves high-frequency detail but produces images that diverge significantly from the original model for the same prompt/seed. FraQAT preserves both high-frequency detail and produces images closest to the original model's output among all compared baselines.", "source_urls": [], "tags": []}
{"id": "fraqat-paper-010", "section": "outlier-analysis", "heading": "4.3 Outlier analysis", "content": "Different model architectures exhibit activation outliers in different layer types: SD3.5-Medium's outliers emerge mainly after Feed-Forward (FF) layers, while PixArt-Sigma's outliers arise mainly from Attention (Attn) layers. The authors test selective training -- optimizing only Attn, FF, or full Transformer Block (TF) layers while freezing and quantizing the rest of the network at W4A8 -- and compare against training the whole network (All). Results (Table 2) show no universally best layer choice across architectures, but training Transformer Blocks (TF) is recommended as a good default since it reduces memory and compute requirements while addressing outliers across layer types reasonably well.", "source_urls": [], "tags": []}
{"id": "fraqat-paper-011", "section": "on-device-deployment", "heading": "4.4 On-device model deployment", "content": "To validate real-world feasibility, Sana 600M was quantized to W4A8 using FraQAT with static (rather than dynamic) weight and activation quantization -- required since integer NPU accelerators only support static quantization -- and deployed on a Samsung S25U running the Qualcomm SM8750-AB Snapdragon 8 Elite Hexagon Tensor Processor (HTP). Static quantization scale/offset values were pre-computed by passing 100 random samples through the DiT, recording per-layer feature statistics (mean and standard deviation), and using a 3-sigma inlier range. All linear layers ran at W4A8 except the final layer, which ran at W4A16 as a quality/latency compromise. The deployed model achieved 66ms latency per forward step versus 95ms for an equivalent W4A16 model (the precision SVDQuant requires) -- a 30.5% latency improvement -- while producing on-device generation quality visually comparable to both the original model and its GPU-quantized counterpart.", "source_urls": [], "tags": []}
{"id": "fraqat-paper-012", "section": "limitations-future-work", "heading": "5. Limitations and future work", "content": "FraQAT improves quality over standard QAT but remains more computationally expensive than PTQ, as is typical for QAT techniques. Unlike multi-precision LLM QAT approaches such as MatryoshkaQAT, FraQAT's quantized model targets a single bit precision; multi-precision support is left to future work. The intermediate precision schedule is currently hand-picked rather than algorithmically selected, which the authors identify as a future research direction. Preliminary tests suggest weight-decay regularization can boost performance on Sana 0.6B by roughly 10%, but a full investigation of regularizers and data augmentation is left to future work, as is exploring alternative losses (feature loss, task loss) beyond the current knowledge-distillation-only objective.", "source_urls": [], "tags": []}
{"id": "fraqat-paper-013", "section": "conclusions", "heading": "6. Conclusions", "content": "FraQAT is presented as a novel Quantization-Aware-Training technique exploiting fractional bits while progressively lowering parameter precision during training, following a curriculum-learning strategy that addresses outliers as they arise at each precision level, resulting in more stable and faster training. Evaluated across several state-of-the-art DiT and MM-DiT diffusion models, FraQAT achieves superior qualitative and quantitative performance versus existing state-of-the-art QAT approaches. The authors suggest that if deployed on-device, such quality improvements could boost mobile user productivity, preserve user privacy, and enable generation of personalized content without cloud dependency.", "source_urls": [], "tags": []}
{"id": "fraqat-paper-014", "section": "appendix-a-baselines-hyperparameters", "heading": "Appendix A: baselines and QAT hyper-parameters", "content": "State-of-the-art baseline comparisons rely on the original authors' released code for SVDQuant and DiTAS with default parameters, using pretrained models at 512x512 default resolution, adjusting baseline configurations where needed to match the same underlying model. All QAT experiments (SVDQAT, vQAT, FraQAT) use the FuseAdam optimizer for 25 epochs on AMD MI300X hardware, implemented in PyTorch, Lightning, and torchao, with a fixed seed of 1234. Reported hyper-parameters (learning rate, batch size, LoRA rank where applicable) differ per model and method: for example SVDQAT uses learning rates around 1e-5 to 1e-6 with LoRA ranks of 16-32, while vQAT and FraQAT use no low-rank adapters, with FraQAT generally using a lower learning rate (around 1e-6 to 1e-7) than vQAT. FraQAT's precision schedule spends one epoch at each of 8, 7, 6, 5.5, and 5 bits, two epochs at each of 4.75, 4.5, and 4.25 bits, and 14 epochs at the final 4-bit level, for a total compute cost of roughly 192 GPU-hours for Sana, 576 GPU-hours for PixArt-Sigma, and 1008 GPU-hours for SD3.5-Medium.", "source_urls": [], "tags": []}
{"id": "fraqat-paper-015", "section": "appendix-a5-quantization-schedule", "heading": "Appendix A.5: quantization schedule ablation", "content": "To justify the fractional quantization schedule design, the authors compare it against an integer-only schedule (8 to 7 to 6 to 5 to 4) and a simpler progressive schedule (16 to 8 to 4) under matched computational budgets, training SD3.5-Medium and tracking validation loss. The integer and simple schedules perform similarly to each other, while the fractional schedule consistently achieves a lower validation loss throughout training, supporting the choice of fine-grained fractional bit steps over coarser integer or two-stage schedules.", "source_urls": [], "tags": []}
{"id": "fraqat-paper-016", "section": "appendix-b-language-model", "heading": "Appendix B.1: application to a language model (Gemma2 2B IT)", "content": "To test generality beyond vision/diffusion models, FraQAT is applied to Gemma2 2B IT, quantizing from the original FP16 model to W4A8 using the same precision schedule as the diffusion experiments. Training uses a subset of the C4 dataset (384K training samples, 38.4K validation samples), with zero-shot evaluation on BoolQ and CommonsenseQA/Social IQA. The FraQAT-quantized model shows only minimal degradation relative to the original W16A32 model (CommonSense QA 0.69 vs 0.70, BoolQ 0.72 vs 0.76, with a COQA metric anomalously improving from 0.66 to 0.70), demonstrating that FraQAT's progressive fractional-bit approach generalizes from text-to-image diffusion transformers to autoregressive language models.", "source_urls": [], "tags": []}
{"id": "fraqat-paper-017", "section": "relevance", "heading": "Relevance to this project", "content": "FraQAT is directly relevant to the Omni Claw / Novus Agenti on-device AI stack because it is a Samsung AI Center paper that demonstrates practical W4A8 quantization-aware training deployed on a Samsung S25U phone running the Qualcomm Snapdragon 8 Elite Hexagon Tensor Processor, the same family of Qualcomm Hexagon NPU hardware referenced for the project's Rubik Pi 3 secondary edge hub (Dragonwing QCS6490 / Hexagon 770). The paper's progressive fractional-bit curriculum for quantization-aware training is a candidate technique for future on-device fine-tuning or model-compression work performed on the project's Jetson Orin Nano hub before deploying compressed models to Hexagon-NPU-based edge devices.", "source_urls": [], "tags": []}
```

</details>

---

# Google Dev Docs

<sub>source folder: `knowledge/google-dev-docs/`</sub>

## `automated-build-w-github/automated-build-android-github-actions.md`
Source: Drive file ID `1mR5NYnVkdCKCkwozyjPm4nsXQ_sEu_ZQ` — a Google Developer Library article/README page (dev-library tutorial) documenting the `amirisback/automated-build-android-app-with-github-action` GitHub repository.

# Automated Build Android Using GitHub Action

**Repository:** [amirisback/automated-build-android-app-with-github-action](https://devlibrary.withgoogle.com/products/android/repos/amirisback-automated-build-android-app-with-github-action) (Google Dev Library — Android)
**Author:** Faisal Amir (Muhammad Faisal Amir)

> Note on transcription: the source PDF is a printed capture of a GitHub README rendered on the Google Dev Library site. Several YAML code lines are cut off at the page margin in the original document (e.g. mid-expression on `awk`, `env.main_project_module`, etc.). Those lines are transcribed exactly as they appear in the source, including the truncation — nothing has been invented to complete them.

## Overview

This project demonstrates how to automate building Android APKs and App Bundles (AAB) using GitHub Actions workflows. It covers:

- Project GitHub Action Script (YAML)
- Using GitHub Workflows
- Automated Build of AAB (release)
- Automated Build of APK (release and debug)
- Using Bundletool
- Downloading artifacts
- Uploading artifacts
- Clearing (cleaning up) artifacts by naming convention

## Version / Release

Latest release: `version_release = 2.2.7`

**What's New:**

- Update Target SDK 36
- Update Action Script
- Update Android Studio Latest Version
- Update Gradle Latest Version
- Update Kotlin Latest Version
- Update GitHub Action Script
- Add Bash and Bat Script

**Article sources / guide sources (GitHub Action):**

- Sample artifact naming: `${date_today} - ${repository_name} - ${playstore_name} - APK(s)`
- Tested on a private repository (App Bundle(s) and APK generated successfully)
- Can also be run locally with a `.run` configuration in a local machine

Full code for the GitHub Actions workflow: [generate-apk-aab-debug-release.yml](https://github.com/amirisback/automated-build-android-app-with-github-action/blob/master/.github/workflows/generate-apk-aab-debug-release.yml)

Further reading referenced by the article:

- [How To Securely Build and Sign Your Android App With GitHub Actions](https://proandroiddev.com/how-to-securely-build-and-sign-your-android-app-with-github-actions-ad5323452ce)
- [How to Use GitHub Actions to Automate Android App Development](https://www.freecodecamp.org/news/use-github-actions-to-automate-android-development/)
- [Update Java Checkout Version CI (`actions/setup-java`)](https://github.com/actions/setup-java)

## How to Use Workflows (Push-Triggered Build)

**Step 1.** Upload your project to GitHub.

**Step 2.** Create the workflow file. The project must be an Android Studio project using Gradle. Create a file named `generate-apk-aab-debug-release.yml` inside the folder `.github/workflows/`, so the final path is:

```
.github/workflows/generate-apk-aab-debug-release.yml
```

**Step 3.** Populate the workflow with the following content (as captured from the source; some lines are truncated in the original document where noted):

```yaml
name: Generated APK AAB (Push Github - Create Artifact To Github Action)

env:
  # The name of the main module repository
  main_project_module: app
  # The name of the Play Store
  playstore_name: Frogobox ID
  # The output folder for build results
  build_output_path: buildActionResult

on:
  push:
    branches:
      - 'release/**'
  # Allows you to run this workflow manually from the Actions tab
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # Set Current Date As Env Variable
      - name: Set current date as env variable
        run: echo "date_today=$(date +'%Y-%m-%d')" >> $GITHUB_ENV

      # Set Repository Name As Env Variable
      - name: Set repository name as env variable
        run: echo "repository_name=$(echo '${{ github.repository }}' | awk   # [line truncated in source]

      - name: Set Up JDK
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'  # See 'Supported distributions' for availabl  # [line truncated in source]
          java-version: '17'
          cache: 'gradle'

      - name: Change wrapper permissions
        run: chmod +x ./gradlew

      # Run Tests Build
      - name: Run gradle tests
        run: ./gradlew test

      # Run Build Project
      - name: Build gradle project
        run: ./gradlew build

      # Create APK Debug
      - name: Build apk debug project (APK) - ${{ env.main_project_module }}   # [line truncated in source]
        run: ./gradlew assembleDebug

      # Create APK Release
      - name: Build apk release project (APK) - ${{ env.main_project_module }}  # [line truncated in source]
        run: ./gradlew assemble

      # Create Bundle AAB Release
      # Noted for main module build [main_project_module]:bundleRelease
      - name: Build app bundle release (AAB) - ${{ env.main_project_module }}   # [line truncated in source]
        run: ./gradlew ${{ env.main_project_module }}:bundleRelease

      # Upload Artifact Build
      # Noted For Output [main_project_module]/build/outputs/apk/debug/
      - name: Upload APK Debug - ${{ env.repository_name }}
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.date_today }} - ${{ env.playstore_name }} - ${{ env  # [line truncated in source]
          path: ${{ env.main_project_module }}/build/outputs/apk/debug/

      # Noted For Output [main_project_module]/build/outputs/apk/release/
      - name: Upload APK Release - ${{ env.repository_name }}
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.date_today }} - ${{ env.playstore_name }} - ${{ env  # [line truncated in source]
          path: ${{ env.main_project_module }}/build/outputs/apk/release/

      # Noted For Output [main_project_module]/build/outputs/bundle/release
      - name: Upload AAB (App Bundle) Release - ${{ env.repository_name }}
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.date_today }} - ${{ env.playstore_name }} - ${{ env  # [line truncated in source]
          path: ${{ env.main_project_module }}/build/outputs/bundle/release

      # ======================================================
      # Copy Build Outputs & Push to GitHub
      # ======================================================

      # Delete build/outputs directory if it already exists
      - name: Clean up build/outputs directory
        run: rm -rf ${{ env.build_output_path }}

      # Create build/outputs directory if it doesn't exist
      - name: Create build/outputs directory
        run: mkdir -p ${{ env.build_output_path }}

      # Copy APK Debug to build/outputs
      - name: Copy APK Debug to build/outputs
        run: |
          cp -r ${{ env.main_project_module }}/build/outputs/apk/debug/* ${  # [line truncated in source]

      # Copy APK Release to build/outputs
      - name: Copy APK Release to build/outputs
        run: |
          cp -r ${{ env.main_project_module }}/build/outputs/apk/release/*  # [line truncated in source]

      # Copy AAB Release to build/outputs
      - name: Copy AAB Release to build/outputs
        run: |
          cp -r ${{ env.main_project_module }}/build/outputs/bundle/release  # [line truncated in source]

      # List copied files for verification
      - name: List build/outputs contents
        run: ls -la ${{ env.build_output_path }}/

      # Commit and Push to GitHub
      - name: Commit & Push build outputs to GitHub
        run: |
          git config user.name '${{ github.actor }}'
          git config user.email '${{ github.actor }}@users.noreply.github.c  # [line truncated in source]
          git add ${{ env.build_output_path }}/ -f
          git diff --cached --quiet && echo "No changes to commit" || (git  # [line truncated in source]
```

This workflow was reported as tested successfully against a private repository (App Bundle(s) and APK generated successfully).

## How to Use Workflows (Manual / Upload-Triggered Build)

**Step 1.** Upload your project to GitHub.

**Step 2.** Create the workflow file at `.github/workflows/generate-apk-aab-debug-release.yml` (same location as above). The project must be an Android Studio project using Gradle.

**Step 3.** Use the following workflow content:

```yaml
name: Generated APK AAB (Upload - Create Artifact To Github Action)

env:
  # The name of the main module repository
  main_project_module: app
  # The name of the Play Store
  playstore_name: Frogobox ID

on:
  push:
    branches:
      - 'release/**'
  # Allows you to run this workflow manually from the Actions tab
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # Set Current Date As Env Variable
      - name: Set current date as env variable
        run: echo "date_today=$(date +'%Y-%m-%d')" >> $GITHUB_ENV

      # Set Repository Name As Env Variable
      - name: Set repository name as env variable
        run: echo "repository_name=$(echo '${{ github.repository }}' | awk   # [line truncated in source]

      - name: Set Up JDK
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'  # See 'Supported distributions' for availabl  # [line truncated in source]
          java-version: '17'
          cache: 'gradle'

      - name: Change wrapper permissions
        run: chmod +x ./gradlew

      # Run Tests Build
      - name: Run gradle tests
        run: ./gradlew test

      # Run Build Project
      - name: Build gradle project
        run: ./gradlew build

      # Create APK Debug
      - name: Build apk debug project (APK) - ${{ env.main_project_module }}   # [line truncated in source]
        run: ./gradlew assembleDebug

      # Create APK Release
      - name: Build apk release project (APK) - ${{ env.main_project_module }}  # [line truncated in source]
        run: ./gradlew assemble

      # Create Bundle AAB Release
      # Noted for main module build [main_project_module]:bundleRelease
      - name: Build app bundle release (AAB) - ${{ env.main_project_module }}   # [line truncated in source]
        run: ./gradlew ${{ env.main_project_module }}:bundleRelease

      # Upload Artifact Build
      # Noted For Output [main_project_module]/build/outputs/apk/debug/
      - name: Upload APK Debug - ${{ env.repository_name }}
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.date_today }} - ${{ env.playstore_name }} - ${{ env  # [line truncated in source]
          path: ${{ env.main_project_module }}/build/outputs/apk/debug/

      # Noted For Output [main_project_module]/build/outputs/apk/release/
      - name: Upload APK Release - ${{ env.repository_name }}
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.date_today }} - ${{ env.playstore_name }} - ${{ env  # [line truncated in source]
          path: ${{ env.main_project_module }}/build/outputs/apk/release/

      # Noted For Output [main_project_module]/build/outputs/bundle/release
      - name: Upload AAB (App Bundle) Release - ${{ env.repository_name }}
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.date_today }} - ${{ env.playstore_name }} - ${{ env  # [line truncated in source]
          path: ${{ env.main_project_module }}/build/outputs/bundle/release
```

**Step 4.** The build runs automatically on the Actions tab of the GitHub repository.

**Step 5.** Download the generated artifact.

Related tooling referenced:

- [Download Artifact From GitHub Action](https://github.com/actions/download-artifact)
- [Upload Artifact From GitHub Action](https://github.com/actions/upload-artifact)
- [Remove Artifact (`c-hive/gha-remove-artifacts`)](https://github.com/c-hive/gha-remove-artifacts)

### Results (Private Repository, Build Proven Successful)

The article includes screenshots showing:

- APK(s) debug generated
- APK(s) release generated
- App bundle(s) release generated

## Clean Up Artifact Workflow

A separate scheduled workflow removes old artifacts automatically:

```yaml
name: Generated APK AAB (Clean)

on:
  # Allows you to run this workflow manually from the Actions tab
  workflow_dispatch:
  schedule:
    # Every day at 1am
    - cron: '0 1 * * *'

jobs:
  remove-old-artifacts:
    runs-on: ubuntu-latest
    timeout-minutes: 10
    steps:
      - name: Clean all artifacts
        uses: c-hive/gha-remove-artifacts@v4
        with:
          age: '60 seconds'  # '<number> <unit>', e.g. 5 days, 2 years, 90 s  # [line truncated in source]
          # Optional inputs
          # skip-tags: true
          # skip-recent: 5
```

## Using Bundletool

**Step 1.** Prepare Bundletool. Check for the tool at `.github/lib/bundletool.jar` — [download the latest release here](https://github.com/google/bundletool/releases).

**Step 2.** Use the workflow code in [android-ci-generate-apk-aab-upload-3.yml](https://github.com/amirisback/automated-build-android-app-with-github-action/blob/master/.github/workflows/android-ci-generate-apk-aab-upload-3.yml):

```yaml
name: Generated APK AAB 2 Bundle Tool (Upload - Create Artifact To Github A  # [line truncated in source]

env:
  # The name of the main module repository
  main_project_module: app
  # The name of the Play Store
  playstore_name: Frogobox ID
  # Keystore Path
  ks_path: frogoboxdev.jks
  # Keystore Password
  ks_store_pass: cronoclez
  # Keystore Alias
  ks_alias: frogobox
  # Keystore Alias Password
  ks_alias_pass: xeonranger

on:
  push:
    branches:
      - 'release/**'
  # Allows you to run this workflow manually from the Actions tab
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # Set Current Date As Env Variable
      - name: Set current date as env variable
        run: echo "date_today=$(date +'%Y-%m-%d')" >> $GITHUB_ENV

      # Set Repository Name As Env Variable
      - name: Set repository name as env variable
        run: echo "repository_name=$(echo '${{ github.repository }}' | awk   # [line truncated in source]

      - name: Set Up JDK
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'  # See 'Supported distributions' for availabl  # [line truncated in source]
          java-version: '17'
          cache: 'gradle'

      - name: Change wrapper permissions
        run: chmod +x ./gradlew

      # Run Tests Build
      - name: Run gradle tests
        run: ./gradlew test

      # Run Build Project
      - name: Build gradle project
        run: ./gradlew build

      # Create APK Debug
      - name: Build apk debug project (APK) - ${{ env.main_project_module }}   # [line truncated in source]
        run: ./gradlew assembleDebug

      # Create APK Release
      - name: Build apk release project (APK) - ${{ env.main_project_module }}  # [line truncated in source]
        run: ./gradlew assemble

      # Create Bundle AAB Release
      # Noted for main module build [main_project_module]:bundleRelease
      - name: Build app bundle release (AAB) - ${{ env.main_project_module }}   # [line truncated in source]
        run: ./gradlew ${{ env.main_project_module }}:bundleRelease

      # - name: Build APK(s) Debug from bundle using bundletool
      # run: java -jar ".github/lib/bundletool.jar" build-apks --bundle=$   # [line truncated in source, commented-out step]

      - name: Set Env Artifact name from generated aab
        run: |
          cd ${{ env.main_project_module }}/build/outputs/bundle/release/
          files=(*)
          echo "generated_name_aab=${files[0]%.*}" >> $GITHUB_ENV

      # Build APK From Bundle Using Bundletool
      # Noted For Output [main_project_module]/build/outputs/bundle/release
      - name: Build APK(s) Release from bundle using bundletool (Path same   # [line truncated in source]
        run: java -jar ".github/lib/bundletool.jar" build-apks --bundle=${{  # [line truncated in source]

      # Duplicate APK(s) Release to zip file and extract
      - name: Duplicate APK(s) Release to zip file and extract
        run: |
          cd ${{ env.main_project_module }}/build/outputs/bundle/release/
          unzip -p ${{ env.generated_name_aab }}.apks universal.apk > ${{ e  # [line truncated in source]

      # Upload Artifact Build
      # Noted For Output [main_project_module]/build/outputs/apk/debug/
      - name: Upload APK Debug - ${{ env.repository_name }}
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.date_today }} - ${{ env.playstore_name }} - ${{ env  # [line truncated in source]
          path: ${{ env.main_project_module }}/build/outputs/apk/debug/

      # Noted For Output [main_project_module]/build/outputs/apk/release/
      - name: Upload APK Release - ${{ env.repository_name }}
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.date_today }} - ${{ env.playstore_name }} - ${{ env  # [line truncated in source]
          path: ${{ env.main_project_module }}/build/outputs/apk/release/

      # Noted For Output [main_project_module]/build/outputs/bundle/release
      - name: Upload AAB (App Bundle) Release - ${{ env.repository_name }}
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.date_today }} - ${{ env.playstore_name }} - ${{ env  # [line truncated in source]
          path: ${{ env.main_project_module }}/build/outputs/bundle/release
```

**Step 3.** Run the action.

**Step 4.** Wait for the action to run — check periodically in case of errors.

**Step 5.** Download the artifact (AAB artifact) and extract it.

## Run Using Gradle Configuration (`.run` Configuration)

An alternative for developers who don't have (or don't want to use) GitHub Actions: run builds locally through an IntelliJ/Android Studio `.run` configuration.

**Step 1.** Create a folder named `.run` at the root of the project directory. This lets you run builds on your local machine.

**Step 2.** Create a file named `[name-config].run.xml` inside it:

```xml
<component name="ProjectRunConfigurationManager">
  <!-- Add Name Configuration Here -->
  <configuration default="false" name="${your-config-name}" type="GradleRun...">
    <ExternalSystemSettings>
      <option name="executionName" />
      <option name="externalProjectPath" value="$PROJECT_DIR$" />
      <option name="externalSystemIdString" value="GRADLE" />
      <option name="scriptParameters" value="" />
      <option name="taskDescriptions">
        <list />
      </option>
      <option name="taskNames">
        <list>
          <!-- TODO : add your task here -->
          <option value=":app:assembleDebug" />
        </list>
      </option>
      <option name="vmOptions" />
    </ExternalSystemSettings>
    <ExternalSystemDebugServerProcess>true</ExternalSystemDebugServerProcess>
    <ExternalSystemReattachDebugProcess>true</ExternalSystemReattachDebugProcess>
    <DebugAllEnabled>false</DebugAllEnabled>
    <RunAsTest>false</RunAsTest>
    <method v="2" />
  </configuration>
</component>
```

**Step 3.** The configuration will appear in the run-configuration menu in the IDE.

**Step 4.** Running it executes the configured task(s), producing build output directly on the local machine.

### Sample Configuration: `signingreport`

```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="signingreport" type="GradleRunConfig...">
    <ExternalSystemSettings>
      <option name="executionName" />
      <option name="externalProjectPath" value="$PROJECT_DIR$" />
      <option name="externalSystemIdString" value="GRADLE" />
      <option name="scriptParameters" value="" />
      <option name="taskDescriptions">
        <list />
      </option>
      <option name="taskNames">
        <list>
          <option value="signingreport" />
        </list>
      </option>
      <option name="vmOptions" />
    </ExternalSystemSettings>
    <ExternalSystemDebugServerProcess>true</ExternalSystemDebugServerProcess>
    <ExternalSystemReattachDebugProcess>true</ExternalSystemReattachDebugProcess>
    <DebugAllEnabled>false</DebugAllEnabled>
    <RunAsTest>false</RunAsTest>
    <method v="2" />
  </configuration>
</component>
```

This configuration runs Gradle's `signingreport` task, which prints the SHA-1/SHA-256 signing fingerprints for each build variant — useful for registering a debug/release keystore fingerprint with services like Firebase or Google Play.

## Run Using Script File

### Batch script (Windows)

```bat
@echo off
setlocal

:: Navigate to the project root directory
cd /d "%~dp0.."

echo ======================================
echo Starting Android Build Process
echo ======================================

echo [1/6] Cleaning project...
call gradlew clean

echo [2/6] Running tests...
call gradlew test

echo [3/6] Building project...
call gradlew build

echo [4/6] Assembling Debug APK...
call gradlew assembleDebug

echo [5/6] Assembling Release APK...
call gradlew assemble

echo [6/6] Building Release App Bundle (AAB)...
call gradlew app:bundleRelease

echo ======================================
echo Build completed successfully!
echo ======================================
pause
```

### Shell script (macOS/Linux)

```bash
#!/bin/bash
echo "======================================"
echo " Starting Android Build Process "
echo "======================================"

echo "[1/6] Cleaning project..."
./gradlew clean

echo "[2/6] Running tests..."
./gradlew test

echo "[3/6] Building project..."
./gradlew build

echo "[4/6] Assembling Debug APK..."
./gradlew assembleDebug

echo "[5/6] Assembling Release APK..."
./gradlew assemble

echo "[6/6] Building Release App Bundle (AAB)..."
./gradlew app:bundleRelease

echo "======================================"
echo " Build completed successfully! "
echo "======================================"
```

## Collaborator / Contributing

The project is open to contributions. Contributors are credited in the README; contact is made by email.

- **Mail to:** `faisalamircs@gmail.com`
- **Subject format:** `Github_[Github-Username-Account]_[Language]_[Repository-Name]`
- **Example:** `Github_amirisback_kotlin_admob-helper-implementation`

Maintainer: Muhammad Faisal Amir.

A waiting list for contributors is present in the README (unpopulated at time of capture).

## AI Agent Skill

This project includes an AI Agent Skill designed to help AI coding assistants (such as Antigravity) understand and manage the GitHub Workflows in this repository.

**How to use:** if you are using an AI assistant, you can ask it to use the skill located at:

```
skills/github-workflows/SKILL.md
```

**Example commands:**

- "Explain how the APK upload workflow works using the github-workflows skill."
- "Use the github-workflows skill to add a new environment variable to all CI scripts."
- "Help me troubleshoot a failed build using the github-workflows skill."

## Related Content (Google Dev Library — Android)

The article page also surfaces related Android repositories on Google Dev Library:

- [Chucker](https://devlibrary.withgoogle.com/products/android/repos/ChuckerTeam-chucker) by [ChuckerTeam](https://devlibrary.withgoogle.com/authors/vbuberen) — an HTTP inspector for Android & OkHttp (like Charles Proxy but on-device).
- [detekt](https://devlibrary.withgoogle.com/products/android/repos/detekt-detekt) by [detekt](https://devlibrary.withgoogle.com/authors/cortinico) — a static code analysis tool for Kotlin, operating on the abstract syntax tree provided by the Kotlin compiler.
- [AboutLibraries](https://devlibrary.withgoogle.com/products/android/repos/mikepenz-AboutLibraries) by [mikepenz](https://devlibrary.withgoogle.com/authors/mikepenz) — automatically collects all dependencies and licenses of a Gradle project (including Kotlin Multiplatform) and provides easy-to-integrate UI components for Android.
- [PermissionX](https://devlibrary.withgoogle.com/products/android/repos/guolindev-PermissionX) by [guolindev](https://devlibrary.withgoogle.com/authors/guolindev) — an open-source Android library that makes handling runtime permissions extremely easy.
- [LitePal](https://devlibrary.withgoogle.com/products/android/repos/guolindev-LitePal) by [guolindev](https://devlibrary.withgoogle.com/authors/guolindev) — an Android library that makes using a SQLite database extremely easy.

Page footer: [About](https://devlibrary.withgoogle.com/about) · Terms · Privacy · Feedback

## Relevance to this project

This document is directly relevant to Novus Agenti / Omni Claw's eventual APK build pipeline. It provides a ready-made, field-tested GitHub Actions workflow template for building both debug/release APKs and release AABs from an Android Studio + Gradle project, plus a Bundletool-based path for extracting a universal APK from an AAB (useful for sideloading/testing builds without going through the Play Store). The scheduled artifact-cleanup workflow and the local `.run`-configuration / shell-script alternatives are useful fallback options for CI setup once the project needs repeatable, automated builds of the on-device assistant APK.

<details><summary>chunk layer (JSONL) + source URLs</summary>

```jsonl
{"id": "automated-build-android-github-actions-001", "section": "overview", "heading": "Overview", "content": "This Google Dev Library article documents the amirisback/automated-build-android-app-with-github-action GitHub repository by Faisal Amir (Muhammad Faisal Amir). The project demonstrates how to automate building Android APKs and App Bundles (AAB) using GitHub Actions workflows. It covers the project's GitHub Action script YAML, how to use GitHub Workflows, automated build of AAB in release mode, automated build of APK in both release and debug modes, using Bundletool to derive a universal APK from an AAB, downloading and uploading build artifacts through GitHub Actions, and clearing old artifacts by naming convention.", "source_urls": ["https://devlibrary.withgoogle.com/", "https://devlibrary.withgoogle.com/products/android/", "https://devlibrary.withgoogle.com/products/android/repos/amirisback-automated-build-android-app-with-github-action"], "tags": []}
{"id": "automated-build-android-github-actions-002", "section": "version-release", "heading": "Version / Release Notes", "content": "The latest documented release is version_release = 2.2.7. Its changelog updates the Target SDK to 36, refreshes the GitHub Action script, updates to the latest Android Studio version, updates to the latest Gradle version, updates to the latest Kotlin version, updates the GitHub Action script again, and adds both Bash and Batch build scripts. The article's sample artifact naming convention is ${date_today} - ${repository_name} - ${playstore_name} - APK(s) release generated. The workflow was tested against a private repository and successfully produced both App Bundle(s) and APK artifacts. It can also be run locally through a .run configuration on a developer's own machine. The article links to the full GitHub Actions workflow code, plus further reading on securely building and signing an Android app with GitHub Actions, using GitHub Actions to automate Android app development, and updating the Java setup action version used in CI.", "source_urls": ["https://github.com/amirisback/automated-build-android-app-with-github-action/blob/master/.github/workflows/generate-apk-aab-debug-release.yml", "https://proandroiddev.com/how-to-securely-build-and-sign-your-android-app-with-github-actions-ad5323452ce", "https://www.freecodecamp.org/news/use-github-actions-to-automate-android-development/", "https://github.com/actions/setup-java"], "tags": []}
{"id": "automated-build-android-github-actions-003", "section": "workflow-push-triggered", "heading": "How to Use Workflows (Push-Triggered Build)", "content": "To set this workflow up, first upload the project to GitHub. Then, because the project must be an Android Studio project using Gradle, create a workflow file named generate-apk-aab-debug-release.yml inside the .github/workflows/ folder. The workflow, named Generated APK AAB (Push Github - Create Artifact To Github Action), defines environment variables for the main project module (app), the Play Store display name (Frogobox ID), and a build output path (buildActionResult). It triggers on pushes to any release/** branch and can also be run manually from the Actions tab via workflow_dispatch. The job checks out the repository, records the current date and repository name as environment variables, sets up JDK 17 (Zulu distribution) with Gradle caching, makes gradlew executable, runs the Gradle test suite, builds the project, assembles the debug APK, assembles the release APK, and builds the release App Bundle (AAB) for the main module. It then uploads the debug APK, release APK, and release AAB as GitHub Actions artifacts named using the date, Play Store name, and repository name. Finally, it cleans and recreates a build_output_path directory, copies the debug APK, release APK, and release AAB into it, lists the copied files for verification, and commits and pushes those build outputs back to the GitHub repository using the workflow actor's git identity. This workflow was reported as tested successfully against a private repository.", "source_urls": ["https://github.com/amirisback/automated-build-android-app-with-github-action/blob/master/.github/workflows/generate-apk-aab-debug-release.yml"], "tags": []}
{"id": "automated-build-android-github-actions-004", "section": "workflow-manual-upload", "heading": "How to Use Workflows (Manual / Upload-Triggered Build)", "content": "A simpler variant of the workflow, named Generated APK AAB (Upload - Create Artifact To Github Action), lives at the same path (.github/workflows/generate-apk-aab-debug-release.yml) and omits the copy-and-push-to-git steps. It still triggers on pushes to release/** branches or manual dispatch, checks out the code, records the date and repository name, sets up JDK 17 with Gradle caching, makes gradlew executable, runs tests, builds the project, assembles debug and release APKs, builds the release AAB, and uploads all three as named artifacts. After pushing, the build runs automatically on the repository's Actions tab, and the resulting artifact can be downloaded from there. The article also links to the underlying GitHub Actions used for downloading artifacts, uploading artifacts, and removing old artifacts. Screenshots in the source document show successful generation of debug APK(s), release APK(s), and release App Bundle(s) against a private repository.", "source_urls": ["https://github.com/actions/download-artifact", "https://github.com/actions/upload-artifact", "https://github.com/c-hive/gha-remove-artifacts"], "tags": []}
{"id": "automated-build-android-github-actions-005", "section": "cleanup-artifact-workflow", "heading": "Clean Up Artifact Workflow", "content": "A separate scheduled workflow, Generated APK AAB (Clean), removes old GitHub Actions artifacts automatically. It can be triggered manually via workflow_dispatch or runs on a daily cron schedule at 1am (cron expression '0 1 * * *'). The job, remove-old-artifacts, runs on ubuntu-latest with a 10-minute timeout and uses the c-hive/gha-remove-artifacts@v4 action, configured with an age threshold (the example uses '60 seconds', with the comment noting the format is '<number> <unit>', e.g. 5 days or 2 years). Optional inputs shown but commented out include skip-tags and skip-recent, which would let the cleanup skip tagged releases or a number of the most recent artifacts.", "source_urls": ["https://github.com/c-hive/gha-remove-artifacts"], "tags": []}
{"id": "automated-build-android-github-actions-006", "section": "bundletool", "heading": "Using Bundletool to Extract a Universal APK", "content": "For teams that want an installable APK derived directly from the release AAB, the article documents a Bundletool-based workflow. The first step is preparing Bundletool itself: the workflow expects the jar at .github/lib/bundletool.jar, with the latest release downloadable from the bundletool GitHub releases page. The second step is using the workflow code in android-ci-generate-apk-aab-upload-3.yml, which extends the earlier workflow with additional environment variables for keystore handling (ks_path, ks_store_pass, ks_alias, ks_alias_pass — shown in the source with example values). After building the release AAB, the workflow captures the generated AAB's base filename into a generated_name_aab environment variable, then invokes bundletool build-apks against that AAB to produce a device-agnostic APK set, unzips the universal.apk from that APK set, and uploads the debug APK, release APK, and release AAB as artifacts exactly as in the simpler workflows. After running the action (step 3), the user waits for it to complete, checking periodically for errors (step 4), then downloads the AAB artifact and extracts it (step 5).", "source_urls": ["https://github.com/amirisback/automated-build-android-app-with-github-action/blob/master/.github/workflows/android-ci-generate-apk-aab-upload-3.yml", "https://github.com/google/bundletool/releases"], "tags": []}
{"id": "automated-build-android-github-actions-007", "section": "run-gradle-configuration", "heading": "Run Using Gradle Configuration (.run)", "content": "As an alternative for developers who do not have or do not want to use GitHub Actions, the article documents running builds locally through an IntelliJ/Android Studio .run configuration. The first step is creating a .run folder at the root of the project directory, which enables running builds on the local machine. The second step is creating a file named [name-config].run.xml inside that folder, containing an XML ProjectRunConfigurationManager component that wraps a GradleRunConfiguration with an ExternalSystemSettings block specifying the external project path, the GRADLE system id, and a list of Gradle task names — the example task is :app:assembleDebug. The third step is that the configuration then appears in the IDE's run-configuration menu, and the fourth step, running it, executes the configured Gradle task(s) locally. A second sample configuration named signingreport is also provided, which runs Gradle's signingreport task to print the signing certificate fingerprints (SHA-1/SHA-256) for each build variant — useful for registering a keystore fingerprint with services such as Firebase or the Play Console.", "source_urls": [], "tags": []}
{"id": "automated-build-android-github-actions-008", "section": "script-files", "heading": "Run Using Script Files (Batch and Shell)", "content": "The article also provides standalone build scripts for running the same build sequence outside of Gradle run configurations or CI. A Windows batch script (run from a subfolder, navigating up to the project root) sequentially runs gradlew clean, gradlew test, gradlew build, gradlew assembleDebug, gradlew assemble, and gradlew app:bundleRelease, printing numbered progress messages ([1/6] through [6/6]) and pausing at the end. An equivalent Bash shell script performs the same six steps — clean, test, build, assembleDebug, assemble, and app:bundleRelease — with matching progress echoes, for use on macOS or Linux.", "source_urls": [], "tags": []}
{"id": "automated-build-android-github-actions-009", "section": "collaborator-contributing", "heading": "Collaborator / Contributing", "content": "The project is open to contributions, and the maintainer, Muhammad Faisal Amir, states that contributor names will be added to the README upon request. Prospective contributors are asked to email faisalamircs@gmail.com with a subject line following the pattern Github_[Github-Username-Account]_[Language]_[Repository-Name], for example Github_amirisback_kotlin_admob-helper-implementation. At the time this document was captured, the contributor waiting list was empty.", "source_urls": [], "tags": []}
{"id": "automated-build-android-github-actions-010", "section": "ai-agent-skill", "heading": "AI Agent Skill", "content": "The project ships an AI Agent Skill intended to help AI coding assistants, such as Antigravity, understand and manage the GitHub Workflows defined in the repository. Developers using a compatible AI assistant can point it at the skill file located at skills/github-workflows/SKILL.md. Documented example prompts include asking the assistant to explain how the APK upload workflow works using the github-workflows skill, asking it to use the skill to add a new environment variable across all CI scripts, and asking it to help troubleshoot a failed build using the skill.", "source_urls": [], "tags": []}
{"id": "automated-build-android-github-actions-011", "section": "related-content", "heading": "Related Content on Google Dev Library", "content": "The Dev Library page surfaces several other related Android repositories. Chucker, by ChuckerTeam, is an HTTP inspector for Android and OkHttp, comparable to Charles Proxy but running on-device. detekt, by contributor cortinico, is a static code analysis tool for the Kotlin programming language that operates on the abstract syntax tree provided by the Kotlin compiler. AboutLibraries, by mikepenz, automatically collects all dependencies and licenses of a Gradle project (including Kotlin Multiplatform projects) and provides easy-to-integrate UI components for displaying them on Android. PermissionX, by guolindev, is an open-source Android library that makes handling Android runtime permissions extremely easy. LitePal, also by guolindev, is an Android library that makes using a SQLite database extremely easy. The page footer links to the Dev Library's About, Terms, Privacy, and Feedback pages.", "source_urls": ["https://devlibrary.withgoogle.com/products/android/repos/ChuckerTeam-chucker", "https://devlibrary.withgoogle.com/authors/vbuberen", "https://devlibrary.withgoogle.com/products/android/repos/detekt-detekt", "https://devlibrary.withgoogle.com/authors/cortinico", "https://devlibrary.withgoogle.com/products/android/repos/mikepenz-AboutLibraries", "https://devlibrary.withgoogle.com/authors/mikepenz", "https://devlibrary.withgoogle.com/products/android/repos/guolindev-PermissionX", "https://devlibrary.withgoogle.com/authors/guolindev", "https://devlibrary.withgoogle.com/products/android/repos/guolindev-LitePal", "https://devlibrary.withgoogle.com/about"], "tags": []}
```

```text
https://devlibrary.withgoogle.com/
https://devlibrary.withgoogle.com/products/android/
https://devlibrary.withgoogle.com/products/android/repos/amirisback-automated-build-android-app-with-github-action
https://github.com/amirisback/automated-build-android-app-with-github-action/blob/master/.github/workflows/generate-apk-aab-debug-release.yml
https://proandroiddev.com/how-to-securely-build-and-sign-your-android-app-with-github-actions-ad5323452ce
https://www.freecodecamp.org/news/use-github-actions-to-automate-android-development/
https://github.com/actions/setup-java
https://github.com/actions/download-artifact
https://github.com/actions/upload-artifact
https://github.com/c-hive/gha-remove-artifacts
https://github.com/amirisback/automated-build-android-app-with-github-action/blob/master/.github/workflows/android-ci-generate-apk-aab-upload-3.yml
https://github.com/google/bundletool/releases
https://devlibrary.withgoogle.com/products/android/repos/ChuckerTeam-chucker
https://devlibrary.withgoogle.com/authors/vbuberen
https://devlibrary.withgoogle.com/products/android/repos/detekt-detekt
https://devlibrary.withgoogle.com/authors/cortinico
https://devlibrary.withgoogle.com/products/android/repos/mikepenz-AboutLibraries
https://devlibrary.withgoogle.com/authors/mikepenz
https://devlibrary.withgoogle.com/products/android/repos/guolindev-PermissionX
https://devlibrary.withgoogle.com/authors/guolindev
https://devlibrary.withgoogle.com/products/android/repos/guolindev-LitePal
https://devlibrary.withgoogle.com/about
```

</details>

---

# Gemini Query Exports

<sub>source folder: `knowledge/gemini-query/`</sub>

## `july-3rd-horizons-apk.md`
To build a highly specialized Kotlin Android architecture that functions as a persistent background processing node using continuous integration, specific system APIs and lifecycle frameworks must be coupled together.  
Because this application relies on system-level capabilities, it must be signed with a matching platform signature or given deep elevated permissions to prevent Android's low-memory killer (LMK) from terminating the daemon context.  
\------------------------------  
\#\# 1\. The GitHub CI Workflow (.github/workflows/android-ci.yml) \[1\]   
This GitHub Actions blueprint automates your pipeline. It checks out your code, provisions JDK 21, ensures execution rights for Gradle, compiles your custom APK, and structures the artifact output. \[1, 2\] 

name: Build System Daemon APK  
on:  
  push:  
    branches: \[ "main", "dev" \]  
  pull\_request:  
    branches: \[ "main" \]  
jobs:  
  build:  
    runs-on: ubuntu-latest

    steps:  
    \- name: Checkout Code Base  
      uses: actions/checkout@v4

    \- name: Set up JDK 21  
      uses: actions/setup-java@v4  
      with:  
        distribution: 'temurin'  
        java-version: '21'  
        cache: 'gradle'

    \- name: Grant Execute Permission for Gradle Wrapper  
      run: chmod \+x gradlew

    \- name: Run Code Linters and Unit Tests  
      run: ./gradlew test

    \- name: Build Elevated Release APK  
      run: ./gradlew assembleRelease

    \- name: Upload Compiled Daemon Package  
      uses: actions/upload-artifact@v4  
      with:  
        name: Daemon-Engine-Release  
        path: app/build/outputs/apk/release/app-release.apk

\------------------------------  
\#\# 2\. Android Manifest Configuration (AndroidManifest.xml)  
To act as a Daemon, a Device Assistant API provider, and a registered Video Game, your manifest requires structural definitions that declare these operational hooks to the Android system.

\<manifest xmlns:android="

[http://android.com\](https://www.android.com](http://android.com]\(https://www.android.com) 

"  
    package="com.example.systemdaemon"\>

    \<\!-- Permissions required for continuous execution and system level oversight \--\>  
    \<uses-permission android:name="android.permission.FOREGROUND\_SERVICE" /\>  
    \<uses-permission android:name="android.permission.FOREGROUND\_SERVICE\_SPECIAL\_USE" /\>  
    \<uses-permission android:name="android.permission.RECEIVE\_BOOT\_COMPLETED" /\>  
    \<uses-permission android:name="android.permission.REQUEST\_IGNORE\_BATTERY\_OPTIMIZATIONS"/\>  
      
    \<\!-- Large heap minimizes sudden LMK kills during on-device model orchestration \--\>  
    \<application  
        android:allowBackup="false"  
        android:largeHeap="true"  
        android:label="@string/app\_name"\>

        \<\!-- 1\. Video Game Registration Hook \--\>  
        \<meta-data android:name="android.game.category" android:value="true" /\>

        \<\!-- 2\. Device Assistant API Configuration \--\>  
        \<service  
            android:name=".services.SystemAssistantVoiceService"  
            android:label="HTP Handoff Assistant"  
            android:permission="android.permission.BIND\_VOICE\_INTERACTION"  
            android:exported="true"\>  
            \<meta-data  
                android:name="android.voice\_interaction"  
                android:resource="@xml/assistant\_interaction\_info" /\>  
            \<intent-filter\>  
                \<action android:name="android.service.voice.VoiceInteractionService" /\>  
            \</intent-filter\>  
        \</service\>

        \<\!-- 3\. Low-Level Persistent Daemon Service \--\>  
        \<service  
            android:name=".services.DaemonProcessingEngine"  
            android:foregroundServiceType="specialUse"  
            android:exported="false" /\>

        \<\!-- Boot receiver to spin up daemon immediately on device activation \--\>  
        \<receiver android:name=".receivers.BootReceiver" android:exported="true"\>  
            \<intent-filter\>  
                \<action android:name="android.intent.action.BOOT\_COMPLETED" /\>  
            \</intent-filter\>  
        \</receiver\>

    \</application\>  
\</manifest\>

\[3\]   
Note: For the Assistant component to bind properly, you must create a resource file at res/xml/assistant\_interaction\_info.xml containing:

\<voice-interaction-service xmlns:android="

[http://android.com](http://android.com) 

"  
    android:sessionService="com.example.systemdaemon.services.AssistantSessionService"  
    android:recognitionService="com.example.systemdaemon.services.AssistantRecognitionService"  
    android:supportsAssist="true"  
    android:supportsLocalInteraction="true" /\>

\------------------------------  
\#\# 3\. The Kotlin Daemon implementation (DaemonProcessingEngine.kt)  
True system daemons in standard Linux do not exist within Android's sandbox application lifecycle. Instead, you must instantiate a Sticky Foreground Service bound to an unbreakable system notification channel to replicate daemon behavior.

package com.example.systemdaemon.services  
import android.app.Notificationimport android.app.NotificationChannelimport android.app.NotificationManagerimport android.app.Serviceimport android.content.Intentimport android.os.IBinderimport androidx.core.app.NotificationCompatimport kotlinx.coroutines.\*  
class DaemonProcessingEngine : Service() {

    private val serviceJob \= Job()  
    private val serviceScope \= CoroutineScope(Dispatchers.Default \+ serviceJob)  
    private val CHANNEL\_ID \= "system\_daemon\_channel"

    override fun onCreate() {  
        super.onCreate()  
        createNotificationChannel()  
        startForeground(1001, buildDaemonNotification(), Service.START\_STICKY)  
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {  
        // Spin up perpetual asynchronous processing loop   
        serviceScope.launch {  
            while (isActive) {  
                // Execute low level operations / monitor Hexagon HTP pipeline states  
                delay(5000)   
            }  
        }  
          
        // START\_STICKY instructs OS to recreate service if evicted under resource pressure  
        return START\_STICKY  
    }

    override fun onDestroy() {  
        super.onDestroy()  
        serviceJob.cancel() // Safeguard memory context leaks  
    }

    override fun onBind(intent: Intent?): IBinder? \= null

    private fun createNotificationChannel() {  
        val channel \= NotificationChannel(  
            CHANNEL\_ID, "System Daemon Engine",  
            NotificationManager.IMPORTANCE\_MIN  
        ).apply { description \= "Maintains persistent pipeline hardware states." }  
          
        val manager \= getSystemService(NotificationManager::class.java   
        manager?.createNotificationChannel(channel)  
    }

    private fun buildDaemonNotification(): Notification {  
        return NotificationCompat.Builder(this, CHANNEL\_ID)  
            .setContentTitle("Daemon Engine Active")  
            .setContentText("Monitoring dedicated HTP compute layers...")  
            .setPriority(NotificationCompat.PRIORITY\_MIN)  
            .build()  
    }  
}

\------------------------------  
\#\# 4\. Integrating the Device Assistant API API (SystemAssistantVoiceService.kt)  
To access screen structures, system contexts, or globally intercept user assist long-press triggers (like an alternative to Google Assistant or Bixby), your app implements VoiceInteractionService. \[4\] 

package com.example.systemdaemon.services  
import android.os.Bundleimport android.service.voice.VoiceInteractionService  
class SystemAssistantVoiceService : VoiceInteractionService() {  
      
    override fun onReady() {  
        super.onReady()  
        // Assistant architecture fully mapped into the system lifecycle  
    }

    override fun launchVoiceAssistFromKeyguard() {  
        super.launchVoiceAssistFromKeyguard()  
        // Custom processing routing handled when device is locked  
    }  
}

\------------------------------  
\#\# 🎛️ Elevated Permissions Validation  
Because this app is acting as a raw system resource agent, standard runtime user popups are insufficient. To elevate execution permissions, pass these configuration steps via an Android Debug Bridge (adb) terminal thread post-deployment:

   1\. Bypass Doze Engine / Battery Optimization Limits:  
     
   adb shell dumpsys deviceidle whitelist \+com.example.systemdaemon  
     
   2\. Manually Force-Bind Device Assistant Context Default Routing:  
     
   adb shell settings put secure assistant com.example.systemdaemon/.services.SystemAssistantVoiceService  
     
     
Would you like assistance setting up the boilerplate code for the associated AssistantSessionService and AssistantRecognitionService required to capture screen text data through this custom Assistant API layer?

\[1\] \[[https://www.youtube.com\](https://www.youtube.com/watch?v=9CYbB39PtSs](https://www.youtube.com]\(https://www.youtube.com/watch?v=9CYbB39PtSs) )  
\[2\] \[[https://github.com\](https://github.com/marketplace/actions/automated-build-android-app-with-github-action](https://github.com]\(https://github.com/marketplace/actions/automated-build-android-app-with-github-action) )  
\[3\] nu\[[https](https://x-team.com]\(https://x-team.com/magazine/android-runtime-permissions)\[4\] \[[https://www.samsung.com\](https://www.samsung.com/us/support/answer/ANS10001575/)](https://www.samsung.com]\(https://www.samsung.com/us/support/answer/ANS10001575/\))

This setup creates a robust, self-healing system pipeline. To achieve automated model swapping, remote updates, terminal orchestration, and cross-application persistence on Android, the system must bypass the typical restrictions of the application sandbox.  
\------------------------------  
\#\# 1\. Dual-App Watchdog System (The Ghost Reciprocal Recovery Pattern)  
Android will aggressively kill heavy background tasks like your 9B LLM processing node if system memory runs low. To prevent this, implement two distinct applications with opposing broadcast hooks that monitor and revive each other completely out of band.

       \[ App A: Main Engine \]                \[ App B: Watchdog Engine \]  
   (Package: com.sys.daemon)            (Package: com.sys.watchdog)

              |                                      |  
     CRASH / OOM EVICTION                           |  
              |===\[Broadcast: ACTION\_PACKAGE\_REPLACED\]====\>

              |                                      |  (Wakes up)  
              |                                 Launches Service

              |\<==\[Intent: startForegroundService()\]--|

\#\# App A Manifest Broadcast Registration (com.sys.daemon)

\<\!-- Listens for changes to the Watchdog app to protect the link \--\>  
\<receiver android:name=".receivers.WatchdogRecoveryReceiver" android:exported="true"\>  
    \<intent-filter\>  
        \<action android:name="android.intent.action.PACKAGE\_REPLACED" /\>  
        \<action android:name="android.intent.action.PACKAGE\_ADDED" /\>  
        \<data android:scheme="package" android:ssp="com.sys.watchdog" /\>  
    \</intent-filter\>  
\</receiver\>

\#\# App B Watchdog Recovery Logic (com.sys.watchdog)  
App B contains a minimal footprint and registers the inverse configuration, watching com.sys.daemon. When App B receives a notification that the main process has dropped or updated, it immediately uses an internal launch thread: \[1\] 

package com.sys.watchdog.receivers  
import android.content.BroadcastReceiverimport android.content.Contextimport android.content.Intent  
class EngineWatchdogReceiver : BroadcastReceiver() {  
    override fun onReceive(context: Context, intent: Intent) {  
        // Intercepts system events or direct signals if the main process crashes  
        val targetPackage \= "com.sys.daemon"   
        val launchIntent \= context.packageManager.getLaunchIntentForPackage(targetPackage)  
          
        launchIntent?.let {  
            it.addFlags(Intent.FLAG\_ACTIVITY\_NEW\_TASK)  
            context.startActivity(it)  
        }  
    }  
}

\------------------------------  
\#\# 2\. Frontend Execution Engine (HTTP Client & Dynamic APK Injector)  
To pull down updated comp  
ilation configurations, query cloud endpoints, or dynamically download and update associated secondary tool APKs from GitHub / Hugging Face, build an explicit OkHttp network pipeline coupled with Android's modern PackageInstaller.

package com.sys.daemon.network  
import android.app.PendingIntentimport android.content.Contextimport android.content.Intentimport android.content.pm.PackageInstallerimport okhttp3.OkHttpClientimport okhttp3.Requestimport java.io.File  
class NetworkPayloadManager(private val context: Context) {  
    private val client \= OkHttpClient()

    // 1\. Direct Cloud CLI / API Inference Hook  
    fun queryCloudInferenceEndpoint(apiUrl: String, jsonPayload: String): String {  
        val request \= Request.Builder()  
            .url(apiUrl)  
            .post(okhttp3.RequestBody.create(jsonPayload, okhttp3.MediaType.parse("application/json")))  
            .build()  
        client.newCall(request).execute().use { response \-\> return response.body()?.string() ?: "" }  
    }

    // 2\. Dynamic Local APK Sideload Injection  
    fun installDownloadedPackage(apkFile: File) {  
        val packageInstaller \= context.packageManager.packageInstaller  
        val sessionParams \= PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE\_FULL\_INSTALL)  
        val sessionId \= packageInstaller.createSession(sessionParams)  
        val session \= packageInstaller.openSession(sessionId)

        apkFile.inputStream().use { inputStream \-\>  
            session.openWrite("daemon\_payload", 0, \-1).use { outputStream \-\>  
                inputStream.copyTo(outputStream)  
                session.fsync(outputStream)  
            }  
        }

        // Create an intent callback to confirm installation states automatically  
        val intent \= Intent(context, context.javaClass)  
        val pendingIntent \= PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG\_MUTABLE)  
        session.commit(pendingIntent.intentSender)  
    }  
}

\------------------------------  
\#\# 3\. Termux Command-Line Interface Interop Pipeline  
You can trigger native on-device tool scripts, execute raw Linux binaries, or invoke compilation files inside the Termux userland from your Kotlin app using the Termux Run Command (TRC) intent system.  
\#\# Manifest Broadcast Requirements

\<\!-- Required to send task execution requests to Termux directly \--\>  
\<uses-permission android:name="com.termux.permission.RUN\_COMMAND" /\>

\#\# Executing Commands in the Termux Filesystem

fun executeLocalToolInTermux(context: Context, scriptPath: String, arguments: Array\<String\>) {  
    val intent \= Intent().apply {  
        className \= "com.termux",  
        action \= "com.termux.RUN\_COMMAND"  
        putExtra("com.termux.RUN\_COMMAND\_PATH", "/data/data/com.termux/files/usr/bin/bash")  
        putExtra("com.termux.RUN\_COMMAND\_ARGUMENTS", arrayOf(scriptPath) \+ arguments)  
        putExtra("com.termux.RUN\_COMMAND\_BACKGROUND", true) // Run in background silently  
    }  
    context.startService(intent)  
}

\------------------------------  
\#\# 4\. Dynamic Quantized Model Swapper  
To transition the Snapdragon 8 Elite NPU between Qwen 3.5 9B and Gemma 4 E4B, you cannot reload weights inline without fragmenting system RAM. You must systematically purge the active memory configuration, unbind the delegate, and remap the new binary file descriptor pointing to your local storage path.

package com.sys.daemon.runtime  
import android.content.Contextimport java.io.Fileimport java.io.FileInputStreamimport java.nio.channels.FileChannel  
class ModelSwapper(private val context: Context) {

    private var currentModelFileChannel: FileChannel? \= null

    @Synchronized  
    fun hotSwapNpuModel(modelName: String): FileChannel {  
        // 1\. Explicitly clear heap references and suggest garbage collection  
        currentModelFileChannel?.close()  
        System.gc()

        // 2\. Locate model on local disk (Downloaded from GitHub CI / Hugging Face)  
        val modelFile \= File(context.getExternalFilesDir(null), "models/$modelName.bin")  
        if (\!modelFile.exists()) throw IllegalArgumentException("Target binary not found.")

        // 3\. Map memory space natively to keep processing off the JVM heap  
        val fileInputStream \= FileInputStream(modelFile)  
        currentModelFileChannel \= fileInputStream.channel  
          
        // Return this channel descriptor directly to your LiteRT or QNN runtime hook  
        return currentModelFileChannel\!\!  
    }  
}

\------------------------------  
\#\# 🛠️ ADB Provisioning Matrix for Cloud Orchestration  
Because your architecture uses structural system interactions, run these overrides via ADB during testing to unlock the required security overrides:

\# Allow your app to request direct Package Install sessions without user popups  
adb shell appops set com.sys.daemon REQUEST\_INSTALL\_PACKAGES allow  
\# Grant Termux background execution privileges so intents do not block  
adb shell cmd power set-adaptive-power-saver-enabled false

Would you like to build the specific Ktor network engine blocks needed to authenticate and pull your release binaries directly using Hugging Face   GitHub API bearer tokens?

\[1\] \[[https://www.reddit.com\](https://www.reddit.com/r/androiddev/comments/813j9j/how\_do\_you\_use\_package\_added\_permission\_with\_the/)](https://www.reddit.com]\(https://www.reddit.com/r/androiddev/comments/813j9j/how_do_you_use_package_added_permission_with_the/\))

## `qwen-3.5-9b-compilation-and-quantization.md`
# Tab 1

Getting an LLM to run natively on the Snapdragon 8 Elite’s Hexagon HTP for Android requires cross-compiling with either the \[Qualcomm Neural Processing (QNN) SDK\]([https://www.qualcomm.com/developer/software/neural-processing-sdk-for-ai](https://www.qualcomm.com/developer/software/neural-processing-sdk-for-ai) ) or Google’s Edge LiteRT. Since open-source runtimes only support CPU execution natively, you must use proprietary toolchains to delegate the compute graph to the HTP. \[1, 2, 3, 4\]   
Three main developer paths exist to target the Hexagon NPU for Android:  
\#\# 1\. Google LiteRT with Qualcomm AOT Compilation (Highly Recommended) \[5\]   
This is the most straightforward method for running models like Gemma 2 natively on the Snapdragon 8 Elite NPU. \[3\] 

\* The Process: You must Ahead-of-Time (AOT) compile your model using the Google Edge LiteRT toolchain. This process transforms standard .tflite models into optimized NPU execution graphs. \[3\]   
\* Setup Requirements: The compilation must be done on a Linux or WSL host because the toolchain is not available for native Windows/macOS Python. \[3\]   
\* Actionable Tooling: Use the \[LiteRT Gemma NPU Notebook\]([https://github.com/google-ai-edge/litert-samples/blob/main/compiled\_model\_api/qualcomm/llm\_chatbot\_npu/NPU\_COMPILATION\_GUIDE.md](https://github.com/google-ai-edge/litert-samples/blob/main/compiled_model_api/qualcomm/llm_chatbot_npu/NPU_COMPILATION_GUIDE.md) ) as a guide to compile your model. \[3\] 

\#\# 2\. Qualcomm GenieX SDK / QNN Runtime \[4\]   
Qualcomm’s official path relies on their QNN (Qualcomm Neural Processing) SDK, providing the best hardware-level access and power efficiency.

\* The Process: GenieX allows you to run models from Hugging Face or Qualcomm AI Hub directly on the Hexagon NPU using their C, Python, or Kotlin APIs.  
\* Actionable Tooling: Check out the \[Qualcomm GenieX Repository\]([https://github.com/qualcomm/GenieX](https://github.com/qualcomm/GenieX) ) to use their open-source CLI and run LLMs on your target device. \[6, 7\] 

\#\# 3\. Community Research Projects (e.g., Llama.cpp NPU Forks)  
For open-source lovers trying to bend llama.cpp to run directly on the Hexagon HTP via Android Termux, the ecosystem relies on research branches. \[8, 9, 10, 11\] 

\* The Process: You must cross-compile the codebase using the Android NDK and the Hexagon SDK to generate custom .so files ( like libhtp\_ops\_skel.so ) that bridge the open-source ggml backend to Qualcomm's FastRPC. \[11, 12\]   
\* Actionable Tooling: Review the \[Llama.cpp NPU Research Repo\]([https://github.com/haozixu/llama.cpp-npu](https://github.com/haozixu/llama.cpp-npu) ) for an example of how to link custom HTP operator libraries to an LLM architecture. \[8, 13\] 

Could you tell me what specific model size and architecture you are planning to deploy, and if you are building a native Android app vs. running a terminal environment like Termux? That will help determine the exact SDK configuration you need.

\[1\] \[[https://github.com\](https://github.com/ggml-org/llama.cpp/blob/master/docs/backend/snapdragon/windows.md](https://github.com]\(https://github.com/ggml-org/llama.cpp/blob/master/docs/backend/snapdragon/windows.md) )  
\[2\] \[[https://mysupport.qualcomm.com\](https://mysupport.qualcomm.com/supportforums/s/question/0D5dK00000BcdbxSAB/can-snpe-sdk-run-llms-on-android-snapdragon-8-lite-if-not-which-sdk-can-be-used-please-provide-reference-materials](https://mysupport.qualcomm.com]\(https://mysupport.qualcomm.com/supportforums/s/question/0D5dK00000BcdbxSAB/can-snpe-sdk-run-llms-on-android-snapdragon-8-lite-if-not-which-sdk-can-be-used-please-provide-reference-materials) )  
\[3\] \[[https://github.com\](https://github.com/google-ai-edge/litert-samples/blob/main/compiled\_model\_api/qualcomm/llm\_chatbot\_npu/NPU\_COMPILATION\_GUIDE.md](https://github.com]\(https://github.com/google-ai-edge/litert-samples/blob/main/compiled_model_api/qualcomm/llm_chatbot_npu/NPU_COMPILATION_GUIDE.md) )  
\[4\] \[[https://github.com\](https://github.com/bpbonker/npurun](https://github.com]\(https://github.com/bpbonker/npurun) )  
\[5\] \[[https://github.com\](https://github.com/google-ai-edge/litert-samples/blob/main/compiled\_model\_api/qualcomm/llm\_chatbot\_npu/NPU\_COMPILATION\_GUIDE.md](https://github.com]\(https://github.com/google-ai-edge/litert-samples/blob/main/compiled_model_api/qualcomm/llm_chatbot_npu/NPU_COMPILATION_GUIDE.md) )  
\[6\] \[[https://github.com\](https://github.com/qualcomm/GenieX](https://github.com]\(https://github.com/qualcomm/GenieX) )  
\[7\] \[[https://github.com\](https://github.com/qualcomm/GenieX](https://github.com]\(https://github.com/qualcomm/GenieX) )  
\[8\] \[[https://github.com\](https://github.com/haozixu/llama.cpp-npu](https://github.com]\(https://github.com/haozixu/llama.cpp-npu) )  
\[9\] \[[https://www.reddit.com\](https://www.reddit.com/r/termux/comments/1qswe99/llama\_32\_3b\_on\_snapdragon\_8\_elite\_cpu\_performance/](https://www.reddit.com]\(https://www.reddit.com/r/termux/comments/1qswe99/llama_32_3b_on_snapdragon_8_elite_cpu_performance/) )  
\[10\] \[[https://medium.com\](https://medium.com/@mnp975/loading-models-on-gpu-534977993d68](https://medium.com]\(https://medium.com/@mnp975/loading-models-on-gpu-534977993d68) )  
\[11\] \[[https://github.com\](](https://github.com]\() [https://github.com/haozixu/llama.cpp-npu](https://github.com/haozixu/llama.cpp-npu) ,)  
\[12\] \[ [https://github.com\](https://github.com/chraac/llama-cpp-qnn-builder](https://github.com]\(https://github.com/chraac/llama-cpp-qnn-builder) )  
\[13\] \[[https://github.com\](https://github.com/haozixu/llama.cpp-npu)](https://github.com]\(https://github.com/haozixu/llama.cpp-npu\))

Running Qwen 3.5 9B and Gemma 4 E4B IT natively on the Snapdragon 8 Elite's Hexagon Tensor Processor (HTP) requires adapting your precision approach. While the Snapdragon 8 Elite natively supports FP16 arithmetic on the HTP, deploying a 9B or even a 4B model at a pure FP16 weight precision will severely bottleneck memory bandwidth or cause the system to crash. \[1, 2, 3, 4\]   
An exact breakdown outlines how to target the HTP for these architectures:  
\#\# ⚠️ Precision Strategy: Why Pure FP16 is Not Recommended

\*   
\* The Memory Math: A pure FP16 model requires \~2 GB of RAM per 1 billion parameters.  
\* Gemma 4 E4B (4B) in FP16 would consume \~8 GB.  
   \* Qwen 3.5 9B in FP16 would consume \~18 GB, exceeding typical Android system limitations for a single process. \[3, 5, 6\]   
\* The HTP Architecture: The Hexagon HTP achieves its hyper-efficient performance via Mixed Precision. The optimal approach is to use INT4 or INT8 quantized weights while keeping the key activation layers and mathematical operations in FP16. Pure unquantized floating-point inference is typically offloaded to the Adreno GPU. \[1, 4, 7, 8, 9\]   
\* 

\------------------------------  
\#\# Step-by-Step Toolchains per Model \[10\] \#\# Path A: Deploying Qwen 3.5 9B (Via Qualcomm QNN / GenieX)  
Because Qwen 3.5 utilizes standard transformer configurations with native rope/attention formats, it maps natively into Qualcomm's specialized LLM runtime. \[2, 11, 12\] 

   1\. Quantization Setup: Download the official \[Qualcomm AI Engine Direct (QNN) SDK\]([https://docs.qualcomm.com/bundle/publicresource/topics/80-62010-1/qnn-workflow.html](https://docs.qualcomm.com/bundle/publicresource/topics/80-62010-1/qnn-workflow.html) ) via the Qualcomm Developer Network.  
   2\. Graph Conversion: Run the QNN model converter on your Linux host to transform the Qwen Hugging Face PyTorch weights into a QNN graph topology:  
     
   qnn-pytorch-converter \-i /path/to/qwen3.5-9b \-d input\_ids 1x1 \--input\_type integer \-o qwen3.5\_htp.json  
     
   \[13\]   
   3\. Targeting HTP Mixed-Precision: Use the qnn-model-quantizer tool to enforce weight quantization while retaining FP16 precision for activations to maximize processing speed:  
     
   qnn-model-quantizer \--input\_network qwen3.5\_htp.json \--output\_precision htp\_mixed\_fp16\_int4  
     
   \[8, 13\]   
   4\. Integration: If you are building a terminal environment, utilize the open-source \[Qualcomm GenieX CLI\]([https://github.com/qualcomm/GenieX](https://github.com/qualcomm/GenieX) ) engine to load the resulting .bin and execute tokens directly on the device NPU. \[2, 13\] 

\#\# Path B: Deploying Gemma 4 E4B IT (Via Google LiteRT) \[14, 15\]   
Google's Gemma family has deep optimizations within Google’s own \[Edge LiteRT Toolchain\]([https://github.com/google-ai-edge/litert-samples/blob/main/compiled\_model\_api/qualcomm/llm\_chatbot\_npu/NPU\_COMPILATION\_GUIDE.md](https://github.com/google-ai-edge/litert-samples/blob/main/compiled_model_api/qualcomm/llm_chatbot_npu/NPU_COMPILATION_GUIDE.md) ) (formerly TFLite).

   1\. Obtain the Baseline: Pull the official .tflite model format optimized for mobile from the Google AI Edge repository.  
   2\. Ahead-of-Time (AOT) Compilation: To avoid fallback to the CPU, you must hard-bake the model operators for the HTP. Run the Google compiler on a Linux host, referencing the Snapdragon 8 Elite target system configuration:  
     
   \# Targets the kHtpFp16 / mixed precision execution on the NPU backend  
   litert\_compile \--model\_file=gemma4-e4b.tflite \--output\_file=gemma4\_compiled\_htp.bin \--delegate=QNN \--qnn\_backend=HTP  
     
   3\. Execution in Android: Load the compiled binary using the LiteRT C/C++ API in your application layer. \[16\] 

\------------------------------  
\#\# Critical Verification Settings for Snapdragon 8 Elite  
When running your execution layer inside an Android project or custom terminal pipeline, you must explicitly enable the underlying HTP precision flags in your backend options. If using a Java/Kotlin wrapper or C++ delegate, configure the execution provider options explicitly: \[17\] 

// Explicitly enabling HTP FP16 internal precision mode for delegated mathTfLiteQnnDelegateHtpBackendOptions options;  
options.precision \= TfLiteQnnDelegateHtpPrecision.kHtpFp16; 

This flag guarantees that while the model weights remain highly compressed (saving massive battery and system RAM), the mathematical engine on the Snapdragon chip treats internal tensor calculations with full 16-bit floating-point precision to protect against quantization decay or reasoning looping errors. \[3, 17, 18\]   
Are you planning to run these scripts on a Linux desktop host machine to compile the binaries, or are you trying to do the full compilation directly on the Android target device?

\[1\] \[[https://www.emergentmind.com\](https://www.emergentmind.com/topics/qualcomm-sm8750-ab-snapdragon-8-elite-hexagon-tensor-processor-htp](https://www.emergentmind.com]\(https://www.emergentmind.com/topics/qualcomm-sm8750-ab-snapdragon-8-elite-hexagon-tensor-processor-htp) )  
\[2\] \[[https://www.mindstudio.ai\](https://www.mindstudio.ai/blog/gemma-4-vs-qwen-3-5-open-weight-comparison](https://www.mindstudio.ai]\(https://www.mindstudio.ai/blog/gemma-4-vs-qwen-3-5-open-weight-comparison) )  
\[3\] \[[https://www.youtube.com\](https://www.youtube.com/watch?v=anhLHBi1pP4](https://www.youtube.com]\(https://www.youtube.com/watch?v=anhLHBi1pP4) )  
\[4\] \[[https://www.lantronix.com\](https://www.lantronix.com/products/snapdragon-8-elite-mobile-platform/](https://www.lantronix.com]\(https://www.lantronix.com/products/snapdragon-8-elite-mobile-platform/) )  
\[5\] \[[https://dev.to\](https://dev.to/purpledoubled/how-to-run-googles-gemma-4-locally-with-ollama-all-4-model-sizes-compared-2pbh](https://dev.to]\(https://dev.to/purpledoubled/how-to-run-googles-gemma-4-locally-with-ollama-all-4-model-sizes-compared-2pbh) )  
\[6\] \[[https://www.reddit.com\](https://www.reddit.com/r/LocalLLaMA/comments/1sfr6u4/m5\_max\_128gb\_17\_models\_23\_prompts\_qwen\_35\_122b\_is/](https://www.reddit.com]\(https://www.reddit.com/r/LocalLLaMA/comments/1sfr6u4/m5_max_128gb_17_models_23_prompts_qwen_35_122b_is/) )  
\[7\] \[[https://docs.qualcomm.com\](https://docs.qualcomm.com/doc/80-63195-1/topic/AI-hardware-cores-accelerators.html](https://docs.qualcomm.com]\(https://docs.qualcomm.com/doc/80-63195-1/topic/AI-hardware-cores-accelerators.html) )  
\[8\] \[[https://docs.qualcomm.com\](https://docs.qualcomm.com/doc/80-63442-10/topic/quantization.html](https://docs.qualcomm.com]\(https://docs.qualcomm.com/doc/80-63442-10/topic/quantization.html) )  
\[9\] \[[https://arxiv.org\](https://arxiv.org/html/2511.18674](https://arxiv.org]\(https://arxiv.org/html/2511.18674) )  
\[10\] \[[https://www.reddit.com\](https://www.reddit.com/r/LocalLLaMA/comments/1sg621w/benchmarked\_gemma\_4\_e2b\_vs\_qwen\_35\_2b\_on\_a/](https://www.reddit.com]\(https://www.reddit.com/r/LocalLLaMA/comments/1sg621w/benchmarked_gemma_4_e2b_vs_qwen_35_2b_on_a/) )  
\[11\] \[[https://pytorch.org\](https://pytorch.org/blog/up-to-580tps-new-speed-record-of-qwen3-5-397b-a17b-on-gpu-for-agentic-workloads-with-tokenspeed/](https://pytorch.org]\(https://pytorch.org/blog/up-to-580tps-new-speed-record-of-qwen3-5-397b-a17b-on-gpu-for-agentic-workloads-with-tokenspeed/) )  
\[12\] \[[https://venturebeat.com\](https://venturebeat.com/technology/alibabas-new-open-source-qwen3-5-medium-models-offer-sonnet-4-5-performance](https://venturebeat.com]\(https://venturebeat.com/technology/alibabas-new-open-source-qwen3-5-medium-models-offer-sonnet-4-5-performance) )  
\[13\] \[[https://docs.qualcomm.com\](https://docs.qualcomm.com/bundle/publicresource/topics/80-62010-1/qnn-workflow.html](https://docs.qualcomm.com]\(https://docs.qualcomm.com/bundle/publicresource/topics/80-62010-1/qnn-workflow.html) )  
\[14\] \[[https://huggingface.co\](https://huggingface.co/SeatownSin/gemma-4-E4B-mtp-drafter](https://huggingface.co]\(https://huggingface.co/SeatownSin/gemma-4-E4B-mtp-drafter) )  
\[15\] \[[https://www.reddit.com\](https://www.reddit.com/r/LocalLLaMA/comments/1tuygn6/using\_gemma\_4\_e4b\_with\_the\_litert\_engine\_24x/](https://www.reddit.com]\(https://www.reddit.com/r/LocalLLaMA/comments/1tuygn6/using_gemma_4_e4b_with_the_litert_engine_24x/) )  
\[16\] \[[https://www.reddit.com\](https://www.reddit.com/r/LocalLLaMA/comments/1sapl6k/my\_first\_impression\_after\_testing\_gemma\_4\_against/](https://www.reddit.com]\(https://www.reddit.com/r/LocalLLaMA/comments/1sapl6k/my_first_impression_after_testing_gemma_4_against/) )  
\[17\] \[[https://docs.qualcomm.com\](https://docs.qualcomm.com/doc/80-63442-10/topic/support.html](https://docs.qualcomm.com]\(https://docs.qualcomm.com/doc/80-63442-10/topic/support.html) )  
\[18\] \[[https://www.youtube.com\](https://www.youtube.com/watch?v=vcbuN\_d5qxc)](https://www.youtube.com]\(https://www.youtube.com/watch?v=vcbuN_d5qxc\))

Official documentation and technical guides support the architectural setup for running your cloud-compiled models on the Snapdragon 8 Elite:  
\#\# 📖 Official Ecosystem Documentation

\*   
\* Google AI Edge / LiteRT: Review the core \[Qualcomm NPU (AI Engine Direct) with LiteRT Integration Documentation\]([https://developers.google.com/edge/litert/next/qualcomm](https://developers.google.com/edge/litert/next/qualcomm) ). Google outlines how its new unified machine learning layout achieves a massive speedup over traditional mobile CPUs by linking directly to Qualcomm's silicon. \[1, 2\]   
\* Qualcomm AI Runtime (QAIRT) / QNN: The full deployment roadmap can be found in the \[Qualcomm AI Engine Direct SDK Documentation Overview\]([https://docs.qualcomm.com/bundle/publicresource/topics/80-63442-10/QNN\_general\_overview.html?product=1601111740009302](https://docs.qualcomm.com/bundle/publicresource/topics/80-63442-10/QNN_general_overview.html?product=1601111740009302) ). If you need specific environment configs for the runtime binaries, read the official \[Qualcomm AI Runtime SDK Linux Setup Guide\]([https://docs.qualcomm.com/bundle/publicresource/topics/80-63442-10/linux\_setup.html](https://docs.qualcomm.com/bundle/publicresource/topics/80-63442-10/linux_setup.html) ) or explore the broader portal via the \[Qualcomm Neural Processing SDK landing page\] [https://www.qualcomm.com/developer/software/neural-processing-sdk-for-ai](https://www.qualcomm.com/developer/software/neural-processing-sdk-for-ai)   
 \[3, 4, 5\]   
\* The Hardware Layer: To deeply optimize how your app interacts with the chip's physical compute sections, study the \[Qualcomm AI Engine Direct SDK HTP Backend Guides\]([https://docs.qualcomm.com/nav/home/htp\_htp.html?product=1601111740009302](https://docs.qualcomm.com/nav/home/htp_htp.html?product=1601111740009302) ), which explicitly covers operations targeting the Hexagon Tensor Processor. \[6, 7\]   
\* 

\------------------------------  
\#\# 🛠️ Execution & Deployment Blueprints

\*   
\* For Gemma 4 E4B IT (via LiteRT): Use the official \[Google LiteRT-Samples Gemma NPU Compilation and Push Blueprint\]([https://github.com/google-ai-edge/litert-samples/blob/main/compiled\_model\_api/qualcomm/llm\_chatbot\_npu/NPU\_COMPILATION\_GUIDE.md](https://github.com/google-ai-edge/litert-samples/blob/main/compiled_model_api/qualcomm/llm_chatbot_npu/NPU_COMPILATION_GUIDE.md) ). Step 3 explicitly highlights how to push the model file layout containing the TF\_LITE\_AUX compiled payload over ADB directly onto your target phone without forcing CPU fallback. \[8\]   
\* For Qwen 3.5 9B (via QNN): If you are packaging the native libraries manually to run on a Linux-based or terminal Android subsystem, reference the \[Qualcomm Innovation Development Kit (QIDK) GitHub Repository\]([https://github.com/qualcomm/qidk](https://github.com/qualcomm/qidk) ) for direct deployment scripts, and follow the device execution blocks outlined in the \[Qualcomm Model Run and Verification Guide\]([https://docs.qualcomm.com/doc/80-70014-15B/topic/qnn-run-model.html](https://docs.qualcomm.com/doc/80-70014-15B/topic/qnn-run-model.html) ). \[9, 10\]   
\* For Platform Runtimes: Read the official benchmark analysis and integration workflows on the \[Google Developers Blog on Qualcomm NPU LiteRT Acceleration\](https://developers.googleblog.com/unlocking-peak-performance-on-qualcomm-npu-with-litert/) to see how the Snapdragon 8 Elite processes models under 5ms. \[1\]   
\* 

Are you aiming to wrap these cloud-compiled execution steps inside a native Android Studio Project using Java/Kotlin, or are you planning to run the binaries directly through an on-device command-line interface?

\[1\] \[[https://developers.googleblog.com\](https://developers.googleblog.com/unlocking-peak-performance-on-qualcomm-npu-with-litert/](https://developers.googleblog.com]\(https://developers.googleblog.com/unlocking-peak-performance-on-qualcomm-npu-with-litert/) )  
\[2\] \[[https://developers.google.com\](https://developers.google.com/edge/litert/next/qualcomm](https://developers.google.com]\(https://developers.google.com/edge/litert/next/qualcomm) )  
\[3\] \[[https://docs.qualcomm.com\](https://docs.qualcomm.com/bundle/publicresource/topics/80-63442-10/QNN\_general\_overview.html?product=1601111740009302](https://docs.qualcomm.com]\(https://docs.qualcomm.com/bundle/publicresource/topics/80-63442-10/QNN_general_overview.html?product=1601111740009302) )  
\[4\] \[[https://docs.qualcomm.com\](https://docs.qualcomm.com/bundle/publicresource/topics/80-63442-10/linux\_setup.html](https://docs.qualcomm.com]\(https://docs.qualcomm.com/bundle/publicresource/topics/80-63442-10/linux_setup.html) )  
\[5\] \[[https://www.qualcomm.com\](https://www.qualcomm.com/developer/software/neural-processing-sdk-for-ai](https://www.qualcomm.com]\(https://www.qualcomm.com/developer/software/neural-processing-sdk-for-ai) )  
\[6\] \[[https://www.qualcomm.com\](https://www.qualcomm.com/developer/software/qualcomm-ai-engine-direct-sdk](https://www.qualcomm.com]\(https://www.qualcomm.com/developer/software/qualcomm-ai-engine-direct-sdk) )  
\[7\] \[[https://docs.qualcomm.com\](https://docs.qualcomm.com/nav/home/htp\_htp.html?product=1601111740009302](https://docs.qualcomm.com]\(https://docs.qualcomm.com/nav/home/htp_htp.html?product=1601111740009302) )  
\[8\] \[[https://github.com\](https://github.com/google-ai-edge/litert-samples/blob/main/compiled\_model\_api/qualcomm/llm\_chatbot\_npu/NPU\_COMPILATION\_GUIDE.md](https://github.com]\(https://github.com/google-ai-edge/litert-samples/blob/main/compiled_model_api/qualcomm/llm_chatbot_npu/NPU_COMPILATION_GUIDE.md) )  
\[9\] \[[https://github.com\](https://github.com/qualcomm/qidk](https://github.com]\(https://github.com/qualcomm/qidk) )  
\[10\] \[[https://docs.qualcomm.com\](https://docs.qualcomm.com/doc/80-70014-15B/topic/qnn-run-model.html)](https://docs.qualcomm.com]\(https://docs.qualcomm.com/doc/80-70014-15B/topic/qnn-run-model.html\))

# Tab 2

## `qwen-3.5-9b-q4_0.md`
[https://huggingface.co/Mer0vin8ian/My-Llama-3.2-FineTuned](https://huggingface.co/Mer0vin8ian/My-Llama-3.2-FineTuned)

That specific hardware version changes the game entirely. The v79 Hexagon HTP (Hardware Tensor Processor) is the exact engine built into your Snapdragon 8 Elite. \[1, 2, 3\]   
Because you are compiling for the v79 hardware layout via llama.cpp, you must switch back and use Q4\_0.  
The open-source communities and Qualcomm engineers specifically optimized the native Hexagon backend codebase (LLAMA\_DEVICE\_TYPE\_HEXAGON) around Q4\_0, Q8\_0, and MXFP4 matrix operations. \[2, 4\]   
\#\# Why Q4\_0 Wins on Hexagon v79 Hardware

   1\. Hardware-Bypassed Compilation: The native v79 assembly instruction compiler is mapped specifically to handle straight MUL\_MAT (matrix multiplication) calculations for linear data types. \[2, 3\]   
   2\. Missing Lookups for IQ/NL: While IQ4\_NL works beautifully on standard desktop GPUs, the raw custom C++/Assembly driver code for llama.cpp's Hexagon backend does not currently support the non-linear lookup layers (\_NL). Trying to pass a Q4\_NL model right now will throw an unhandled tensor layout error or cause the app to fallback entirely to your slow CPU cores. \[2, 4\]   
   3\. Thermal Benefits: On the v79 tensor tiles, running a native Q4\_0 file means the chip generates tokens at full processing speed with almost zero power draw. Your phone won't heat up, and it won't throttle. \[5\] 

\#\# Crucial Step for Your Kotlin Code  
Because you are handling a 9B model (\~5.5 GB) on the v79 architecture, a single Hexagon memory session block can sometimes choke if you attempt to load it onto one unified block.  
The standard way to bypass this in the Hexagon SDK/llama.cpp pipeline is to register multiple virtual HTP instances (HTP0, HTP1) in your C++ layer to slice the model execution across the v79's internal multi-threaded compute lanes: \[3, 6\] 

// Within your native CMake configuration layer:// Tell the layout to treat the v79 compute lanes like multiple distinct processing blocks  
NDEV=2   
D=HTP0,HTP1

Final Build Verdict: Stick purely to Qwen 3.5 9B Q4\_0. It matches the active capabilities of the Hexagon v79 framework perfectly. \[2, 4\]   
Do you have your CMake configuration flags mapped out to pull the explicit v79 compiler libraries, or do you need help setting up the dynamic linker path to prevent your Kotlin app from throwing a library not found crash at startup?

\[1\] \[https://www.qualcomm.com\](https://www.qualcomm.com/developer/blog/2026/02/build-faster-on-hexagon-npu-tritor-pytorch-with-hexagon-mlir-open-source)  
\[2\] \[https://www.reddit.com\](https://www.reddit.com/r/LocalLLaMA/comments/1odriw4/preliminary\_support\_in\_llamacpp\_for\_qualcomm/)  
\[3\] \[https://www.youtube.com\](https://www.youtube.com/watch?v=odnyMYSTxoU\&t=1)  
\[4\] \[https://www.reddit.com\](https://www.reddit.com/r/LocalLLaMA/comments/1odriw4/preliminary\_support\_in\_llamacpp\_for\_qualcomm/)  
\[5\] \[https://www.reddit.com\](https://www.reddit.com/r/LocalLLaMA/comments/1t0k6fj/running\_llamacpp\_on\_snapdragon\_hexagon\_npu\_seems/)  
\[6\] \[[https://huggingface.co\](https://huggingface.co/PanhaPa/My-Llama-3.2-FineTuned/blob/main/llama.cpp/docs/backend/hexagon/developer.md)](https://huggingface.co]\(https://huggingface.co/PanhaPa/My-Llama-3.2-FineTuned/blob/main/llama.cpp/docs/backend/hexagon/developer.md\))  
To run your Qwen 3.5 9B (Q4\_0) model on the Snapdragon 8 Elite's Hexagon NPU, you need an inference engine that explicitly utilizes Qualcomm's Snapdragon/Hexagon backend. \[1\]   
Standard Android LLM apps run purely on the phone's ARM CPU or Adreno GPU. To force the workload onto the NPU via Qualcomm's dedicated dspqueue channel, you have two main routes: the user-friendly App method or the Developer / Termux method. \[1, 2, 3, 4, 5\]   
\------------------------------  
\#\# Method 1: The App Route (Easiest)  
Several local Android LLM runners use native llama.cpp JNI bindings under the hood and have begun rolling out explicit Hexagon NPU acceleration. \[1, 5\] 

   1\. Download a Compatible Runner: Install an advanced open-source GGUF Android client like PocketPal AI or SmolChat (ensure you are on the latest update).  
   2\. Load your Model: Place your 5.5 GB qwen3.5-9b-instruct-q4\_0.gguf file into the app's designated model directory.  
   3\. Change the Hardware Backend: Go to the app's Settings \> Inference Engine / Hardware. Change the execution provider from CPU (Neon) or GPU (OpenCL/Vulkan) to Qualcomm Hexagon / NPU.  
   4\. Set Thread Count: Set your CPU threads to 4 (the number of performance cores on the chip) to handle the initial prompt ingestion smoothly while the NPU processes generation.

\------------------------------  
\#\# Method 2: The Termux Route (Max Performance)  
If you want raw speed without app layer overhead, the open-source community maintains a dedicated llama.cpp fork optimized for Qualcomm HTP (Hexagon Tensor Processor) engines. \[6, 7\] 

   1\. Set Up Termux: Install \[Termux\]([https://termux.dev/](https://termux.dev/) on your Android phone and install standard compilation dependencies (clang, cmake, git    
   2\. Acquire the Qualcomm NPU Libraries: You will need the native proprietary Qualcomm library files 

libhtp\_ops.so and libhtp\_ops\_skel.so 

 .  
 These are usually pulled from an existing Snapdragon 8 Elite device firmware dump or through the official \[Qualcomm AI Stack SDK [https://www.qualcomm.com/processors/ai-engine](https://www.qualcomm.com/processors/ai-engine)  \[4, 8, 9\]   
   3\. Compile llama.cpp with Hexagon Support: Clone the \[haozixu/llama.cpp-npu\](https://github.com/haozixu/llama.cpp-npu) repository (which features Qualcomm-contributed acceleration code) and compile it utilizing the Snapdragon flags:  
     
   mkdir build && cd build  
   cmake \-DLLAMA\_SNAPDRAGON=ON ..  
   make llama-cli  
     
   \[1, 6\]   
   4\. Link the Drivers: Move the model file and the Qualcomm .so binaries into your working directory, and point Android's dynamic linker to them:  
     
   export LD\_LIBRARY\_PATH=/data/local/tmp/llama.cpp  
   export DSP\_LIBRARY\_PATH=/data/local/tmp/llama.cpp  
     
   \[8\]   
   5\. Run Inference: Fire up the client, explicitly pointing to the NPU device:  
     
   ./llama-cli \-m qwen3.5-9b-q4\_0.gguf \--device hexagon \-p "Your prompt here"  
     
   \[8, 10\] 

\------------------------------  
\#\# Critical Settings for Qwen 3.5 9B on Mobile

To keep your 5.5 GB setup stable and avoid running out of RAM, use these exact parameters inside your app or CLI launcher:

\* Context Window (Context Size): Limit this to 4,096 tokens or 8,192 tokens max. While Qwen 3.5 natively supports massive context windows, letting it scale past 8k on a mobile device will cause the Key-Value (KV) cache to balloons in memory, which will cause your app to crash.  
\* Flash Attention: Always toggle Flash Attention \= ON in your app settings. This optimizes memory bandwidth significantly, which is critical since mobile LLM inference is highly bandwidth-bound. \[11\] 

Are you comfortable setting this up inside a graphic user interface (GUI) app, or do you want to try the Termux terminal compilation method to squeeze out every drop of token-generation speed?

\[1\] [https://www.reddit.com\](https://www.reddit.com/r/LocalLLaMA/comments/1t0k6fj/running\_llamacpp\_on\_snapdragon\_hexagon\_npu\_seems/](https://www.reddit.com]\(https://www.reddit.com/r/LocalLLaMA/comments/1t0k6fj/running_llamacpp_on_snapdragon_hexagon_npu_seems/)   
\[2\]   

[https://github.com\](https://github.com/jeffzhou2000/ggml-hexagon](https://github.com]\(https://github.com/jeffzhou2000/ggml-hexagon)   
\[3\]  

[https://www.qualcomm.com\](https://www.qualcomm.com/developer/blog/2025/02/how-to-run-deepseek-windows-snapdragon-tutorial-llama-cpp-mlc-llm](https://www.qualcomm.com]\(https://www.qualcomm.com/developer/blog/2025/02/how-to-run-deepseek-windows-snapdragon-tutorial-llama-cpp-mlc-llm)   
\[4\] [https://www.reddit.com\](https://www.reddit.com/r/LocalLLaMA/comments/1sdeok0/how\_to\_run\_ai\_on\_an\_android\_npu/](https://www.reddit.com]\(https://www.reddit.com/r/LocalLLaMA/comments/1sdeok0/how_to_run_ai_on_an_android_npu/)   
\[5\] [https://www.reddit.com\](https://www.reddit.com/r/LocalLLaMA/comments/1h5ll56/introducing\_smolchat\_running\_any\_gguf\_slmsllms/](https://www.reddit.com]\(https://www.reddit.com/r/LocalLLaMA/comments/1h5ll56/introducing_smolchat_running_any_gguf_slmsllms/)   
\[6\]  
[https://github.com\](https://github.com/haozixu/llama.cpp-npu](https://github.com]\(https://github.com/haozixu/llama.cpp-npu)   
\[7\] \[[https://www.qualcomm.com](https://www.qualcomm.com)     
 [https://www.qualcomm.com/processors/hexagon](https://www.qualcomm.com/processors/hexagon)   
\[8\] [https://github.com\](https://github.com/haozixu/llama.cpp-npu](https://github.com]\(https://github.com/haozixu/llama.cpp-npu)   
\[9\] \[[https://www.youtube.com](https://www.youtube.com)

[https://www.youtube.com/watch?v=wd57g2IM3C4\&t=51](https://www.youtube.com/watch?v=wd57g2IM3C4&t=51)   
\[10\] [https://www.reddit.com\](https://www.reddit.com/r/LocalLLaMA/comments/1t0k6fj/running\_llamacpp\_on\_snapdragon\_hexagon\_npu\_seems/](https://www.reddit.com]\(https://www.reddit.com/r/LocalLLaMA/comments/1t0k6fj/running_llamacpp_on_snapdragon_hexagon_npu_seems/)   
\[11\] \[[https://unsloth.ai\](https://unsloth.ai/docs/models/qwen3.5)](https://unsloth.ai]\(https://unsloth.ai/docs/models/qwen3.5\))

Building your own Kotlin app to tap into that Hexagon NPU is an excellent route, giving you full control over your project.  
To run your Qwen 3.5 9B (Q4\_0) model using Qualcomm's dedicated hardware acceleration in a native Android environment, your architecture needs a Kotlin/JNI bridge that talks directly to a specialized build of llama.cpp. Qualcomm explicitly maintains a native Hexagon/Snapdragon backend directly within the llama.cpp ecosystem. \[1, 2, 3\]   
This guide breaks down how to construct the engine, wire up your JNI bridge, and initialize it inside your Kotlin code.  
\------------------------------  
\#\# Phase 1: Compile the Native Libraries (.so)  
Because standard llama.cpp builds default to the CPU (NEON), you must cross-compile the library using the Android NDK and pass the specific flags that tell the compiler to build for the Qualcomm AI Stack. \[3, 4\] 

   1\. Get the proprietary drivers: You need Qualcomm's libhtp\_ops.so and libhtp\_ops\_skel.so libraries. These act as the communication bridge to the Hexagon NPU hardware. You can extract these binaries directly from a Snapdragon 8 Elite device firmware dump or download them via the official \[Qualcomm AI Stack SDK\](https://www.qualcomm.com/developer/software/neural-processing-sdk-for-ai). \[3, 5, 6\] 

   2\. Build with CMake: Set up your build toolchain on your machine using the following configuration, explicitly turning the Snapdragon backend engine on:  
     
   mkdir build-android && cd build-android  
   cmake \-DCMAKE\_TOOLCHAIN\_FILE=$ANDROID\_NDK/build/cmake/android.toolchain.cmake \\  
         \-DANDROID\_ABI=arm64-v8a \\  
         \-DANDROID\_PLATFORM=android-34 \\  
         \-DLLAMA\_SNAPDRAGON=ON \\  
         \-DLLAMA\_SNAPDRAGON\_HTP=ON ..  
   make \-j4  
     
   \[4\]   
   3\. Place your compiled libllama.so and the Qualcomm driver .so files into your app project's src/main/jniLibs/arm64-v8a/ directory so Android Studio bundles them natively.

\------------------------------  
\#\# Phase 2: Create the JNI Bridge (llama-jni.cpp)  
Your Kotlin code cannot talk to C++ directly; it needs a JNI mapping layer. Create a src/main/cpp/llama-jni.cpp file that translates Kotlin commands into the specific backend configuration commands for the NPU hardware: \[3, 7\] 

\#include \<jni.h\>\#include \<string\>\#include "llama.h"  
extern "C"JNIEXPORT jlong JNICALL  
Java\_com\_example\_qwenapp\_LlamaEngine\_loadModelNative(JNIEnv \*env, jobject thiz, jstring model\_path) {  
    const char \*path \= env-\>GetStringUTFChars(model\_path, nullptr);

    // 1\. Initialize llama standard parameters  
    llama\_model\_params model\_params \= llama\_model\_default\_params();  
      
    // 2\. Explicitly steer the model allocation to the Hexagon NPU  
    // This tells llama.cpp to pipe operations into the Qualcomm HTP pipeline  
    model\_params.devices\[0\] \= {  
        .type \= LLAMA\_DEVICE\_TYPE\_HEXAGON,  
        .index \= 0  
    };

    llama\_model \*model \= llama\_load\_model\_from\_file(path, model\_params);  
    env-\>ReleaseStringUTFChars(model\_path, path);

    return reinterpret\_cast\<jlong\>(model);  
}  
https://huggingface.co/Mer0vin8ian/My-Llama-3.2-FineTuned

\------------------------------  
\#\# Phase 3: Implement the Kotlin Layer  
On the Kotlin side, load the native libraries at runtime and create wrapper functions matching your JNI headers. \[3\]   
Ensure you pass Flash Attention parameters and explicitly set your parallel thread count. On the Snapdragon 8 Elite, assigning 4 threads handles the initial text scanning perfectly before the NPU takes over matrix multiplica)

package com.example.qwenapp  
import android.util.Log  
class LlamaEngine {

    companion object {  
        init {  
            // Load the proprietary Qualcomm NPU hardware communication layers  
            System.loadLibrary("htp\_ops")  
            System.loadLibrary("htp\_ops\_skel")  
            // Load your custom compiled llama engine bridge  
            System.loadLibrary("llama-jni")  
        }  
    }

    private var modelPointer: Long \= 0

    // External JNI hooks mapping to our native C++ code  
    private external fun loadModelNative(modelPath: String): Long  
    private external fun generateTokensNative(pointer: Long, prompt: String, maxTokens: Int): String

    fun initializeModel(absolutePath: String) {  
        try {  
            modelPointer \= loadModelNative(absolutePath)  
            if (modelPointer \== 0L) {  
                Log.error("LlamaEngine", "Failed to bind model to Hexagon NPU.")  
            } else {  
                Log.info("LlamaEngine", "Qwen 3.5 9B loaded onto NPU successfully\!")  
            }  
        } catch (e: Exception) {  
            Log.error("LlamaEngine", "Initialization error: ${e.message}")  
        }  
    }

    fun askQwen(prompt: String): String {  
        if (modelPointer \== 0L) return "Engine not initialized."  
        // Constrain settings: limit max output length to conserve mobile system buffers  
        return generateTokensNative(modelPointer, prompt, maxTokens \= 512\)  
    }  
}

\------------------------------  
\#\# Phase 4: Configure Android Permissions and Memory Bounds  
Because a 5.5 GB model file consumes significant memory, you must inform the Android OS that your app needs access to a larger memory pool to prevent the kernel from forcing an Out-Of-Memory (OOM) shutdown.  
Open your app's AndroidManifest.xml file and append the large heap flag inside the \<application\> tag:

\<application  
    android:name=".QwenApplication"  
    android:largeHeap="true"  
    android:hardwareAccelerated="true"\>  
    ...  
\</application\>

Would you like help writing the token generation logic loop for your llama-jni.cpp file, or do you need assistance configuring the CMakeLists.txt inside Android Studio to bundle these specific libraries correctly?

\[1\] \[[https://www.reddit.com](https://www.reddit.com) ([https://www.reddit.com/r/LocalLLaMA/comments/1t0k6fj/running\_llamacpp\_on\_snapdragon\_hexagon\_npu\_seems/](https://www.reddit.com/r/LocalLLaMA/comments/1t0k6fj/running_llamacpp_on_snapdragon_hexagon_npu_seems/)   
\[2\] \[[https://www.reddit.com](https://www.reddit.com)

[https://www.reddit.com/r/LocalLLaMA/comments/1h5ll56/introducing\_smolchat\_running\_any\_gguf\_slmsllms/](https://www.reddit.com/r/LocalLLaMA/comments/1h5ll56/introducing_smolchat_running_any_gguf_slmsllms/)   
\[3\] [https://github.com\](https://github.com/shubham0204/SmolChat-Android](https://github.com]\(https://github.com/shubham0204/SmolChat-Android)   
\[4\] [https://developers.google.com\](https://developers.google.com/edge/litert/android/npu/qualcomm](https://developers.google.com]\(https://developers.google.com/edge/litert/android/npu/qualcomm)   
\[5\] [https://cactuscompute.com](https://cactuscompute.com) [https://cactuscompute.com/compare/best-llama-cpp-alternative](https://cactuscompute.com/compare/best-llama-cpp-alternative)   
\[6\] [https://www.qualcomm.com\](https://www.qualcomm.com/developer/software/neural-processing-sdk-for-ai](https://www.qualcomm.com]\(https://www.qualcomm.com/developer/software/neural-processing-sdk-for-ai)   
\[7\] \[[https://meetprajapati.com\](https://meetprajapati.com/blogs/running-on-device-ai-models-android-mediapipe-llamacpp-executorch/)](https://meetprajapati.com]\(https://meetprajapati.com/blogs/running-on-device-ai-models-android-mediapipe-llamacpp-executorch/\))

\#\# What This Means for Your v79 Workbench Run  
Because the physical memory footprint is tighter and more efficient than I initially stated, compiling Qwen 3.5 9B with ai-edge-litert becomes a lot more attractive:

\* Zero-Copy Memory Layering: On the Hexagon v79, you can aggressively use ion/dma-buf memory allocations. Since the model weights are \~4.2 GB, you can map them directly into the HTP’s virtual address space, keeping your remaining overhead to less than 1.5 GB for execution.  
\* HMX Channel Saturation: At \~5.8 GB total runtime footprint, the model fits beautifully within the fast-access system cache segments of the Gen 4 silicon, meaning fewer trips back to the physical LPDDR5X RAM chips and significantly higher tokens-per-second.

\#\# Adjusting Your Workbench Quantization Flag  
Since you are aiming for that sleek 5.8 GB sweet spot, ensure your quantization config file (quantization\_config.json) passed to the compiler explicitly locks the weights to 4-bit while keeping activations at a higher precision for accuracy:

{  
    "quant\_mode": "WEIGHT\_ONLY\_INT4",  
    "activation\_precision": "INT8",  
    "kv\_cache\_precision": "INT8"  
}

This specific mix maintains Qwen's deep contextual reasoning without letting the file size balloon out of control.  
Now that the footprint is dialed into that \~5.8 GB target:

\* Are you going to use an asymmetric or symmetric INT4 quantization mapping?  
\* Do you need the LiteRT memory-mapping (mmap) code template to ensure it loads directly into the NPU cache without a CPU buffer middleman?

Let me know how you'd like to structure the next step\!

\# Let's double check the exact memory footprint of a 9B model in INT4 quantization.\# A 9B model has approx 9 billion parameters.\# In a strict INT4 weight-only quantization (4 bits per weight), memory \= 9,000,000,000 \* 0.5 bytes \= 4.5 GB.\# Plus the activations, system overhead, and KV cache.\# If it's a mixed precision or slightly under-9B model (like Qwen 2.5 7B/9B, Qwen models often have specific counts like 7.6B or 9B), let's calculate for a 9 billion parameter model.params \= 9e9bytes\_per\_param\_int4 \= 0.5weight\_size\_gb \= (params \* bytes\_per\_param\_int4) / (1024\*\*3)  
print(f"Weight size in GB: {weight\_size\_gb}")
