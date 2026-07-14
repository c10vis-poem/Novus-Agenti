# knowledge/ — Project Knowledge Corpus

Two kinds of content live here, clearly separated:

1. **Drive-mirrored folders** (`omni-claw-defined/`, `research-npu/`,
   `proofs/`, `fragmented-qat/`, `google-dev-docs/`, `gemini-query/`,
   `qairt-sdk/`) — **byte-faithful copies** of the operator's finished
   `Claude_master_wiki` Google Drive folder, the portable knowledge layer
   distilled by hand from raw source captures. **These files are the
   deliverable as-authored.** Copied verbatim (base64 → exact bytes, sizes
   verified against Drive `fileSize`), NOT re-processed, re-summarized, or
   re-compiled by any model. Do not "improve," reflow, or re-chunk them —
   if a distillation needs changing, that happens in Drive first, then
   re-copied here. (Exception: a topic that has a Drive source but was
   missing its `.jsonl` companion — e.g. `qairt-sdk/htp.jsonl` — had that
   companion generated in-repo from the existing `.md`, since that's
   completing the triplet, not re-processing a finished one.)
2. **`daemon-reference/`** and **`claude-code-reference/`** — repo-native
   reference material (not Drive content). Written and maintained directly
   in this repo, or condensed from external docs. Kept alongside the
   Drive-mirrored folders because it's the same kind of "stable reference,
   not day-to-day state" material, not because either pretends to be a
   Drive mirror.

Each topic ships as a triplet where applicable:
- `*.md` — clean Markdown reconstruction (human-readable reference)
- `*.jsonl` — one object per logical section (`id`, `section`, `heading`,
  `content`, `source_urls`, `tags`) — the machine-consumable retrieval layer
- `*-urls.txt` — deduped external reference URLs (where applicable)

## Reading tier — what gets loaded vs. retrieved

- **Always read, every session:** `omni-claw-defined/` — this is the
  project's own definition (vision, blueprint, architecture). Any new
  session needs "what are we building" up front, not on demand.
- **Retrieve on demand:** everything else below. Grep/Glob the relevant
  topic's `.jsonl` for what the current task actually touches — don't load
  whole files speculatively. See `skills/project-memory/SKILL.md` for how
  this is meant to be used.

## Folder map (repo ↔ Drive)

| repo folder | Drive source folder | contents |
|---|---|---|
| `omni-claw-defined/` | `OMNI.CLAW_DEFINED` | project vision, blueprint, knowledge-synthesis architecture, repo-fork asset list, welcome doc — **always-read tier** |
| `research-npu/` | `Research.NPU` | Scaling LLM Test-Time Compute on Mobile NPU (EuroSys '26) distillation |
| `proofs/` | `PROOFS` | on-device PoC precedents: Overlayd-AI, SocketSweep |
| `fragmented-qat/` | `Fragmented QAT` | FraQAT (fractional-bit W4A8 QAT, deployed on S25U / SM8750 HTP) |
| `google-dev-docs/automated-build-w-github/` | `GOOGLE.DEV_DOCS/Automated build w GitHub` | GitHub Actions Android APK/AAB build workflow reference |
| `gemini-query/` | `GEMINI.QUERY` | three Gemini research-query exports (Qwen3.5-9B compile/quant, Q4_0 on Hexagon v79, July-3rd Horizons APK) — Google Docs exported to Markdown |
| `qairt-sdk/` | `#QAIRT/` | Qualcomm AI Engine Direct (QNN) HTP backend reference manual (`htp.md`, 6100+ lines: API specializations, backend extensions, profiling, op-writing guidelines, multi-graph switching) |
| `daemon-reference/` | *(none — repo-native)* | `GPT-DAEMON-REFERENCE.md` (distilled daemon/architecture patterns, what to keep vs. discard from early GPT-generated guides), `NPU-RUNTIME-PATHS.md` (runtime formats + SDK distribution model) — moved here from `wiki/` since they're stable reference material, not session-to-session state |
| `claude-code-reference/` | *(none — condensed from Anthropic's public docs)* | `PROMPT-CACHING.md` — how Claude Code's own session-level caching works; general Claude Code knowledge, not project-specific, moved here from `wiki/` (session 16). The hard-rules contract for `cache_control` mechanics lives in `CLAUDE.md` itself, not here — this file is reference/explanation only. |

`repo-fork-asset-list-source.md` is the original operator Google Doc
("REPO FORK ASSET LIST DO NOT SKIP THIS") that `repo-fork-asset-list.md`
was distilled from — kept alongside its distillation per the doc's own
instruction not to skip it.

## Not copied into the repo (binary source captures — remain in Drive)

The distilled artifacts above ARE the knowledge layer. The raw multi-MB
binary captures they were made from are intentionally left in Drive (they
are sources, not the portable layer, and don't belong in git):

- PDFs: `Snapdragon NPU LLM`, `Scaling.LLM.Test-Time_Compute_Mobile_NPU.pdf`,
  `Off Grid AI`, `Auto.Build_Android...pdf`, `SOCKETSWEEP_README(Markor).pdf`
- `.mht` web captures: QAIRT SDK, LiteRT/Qualcomm NPU, Convert PyTorch GenAI,
  Prebuilt C++ LiteRT Maven, Google Dev Library, automated-build marketplace
- `SocketSweep_1.1.0_amd64.deb`

If any raw source is later needed in-repo, pull it from the corresponding
Drive folder above.
