# knowledge/ — Master-Wiki Corpus (byte-faithful copy)

This directory is a **byte-faithful copy** of the operator's finished
`Claude_master_wiki` Google Drive folder — the portable `.md` / `.jsonl` /
`.txt` knowledge layer distilled by hand (~18 hours of work) from the raw
source captures. **These files are the deliverable as-authored.** They were
copied verbatim (base64 → exact bytes, sizes verified against Drive
`fileSize`), NOT re-processed, re-summarized, or re-compiled by any model.
Do not "improve," reflow, or re-chunk them — if a distillation needs
changing, that happens in Drive first, then re-copied here.

Each topic ships as a triplet by design:
- `*.md` — clean Markdown reconstruction (human-readable reference)
- `*.jsonl` — one object per logical section (`id`, `section`, `heading`,
  `content`, `source_urls`, `tags`) — the machine-consumable retrieval layer
- `*-urls.txt` — deduped external reference URLs (where applicable)

## Folder map (repo ↔ Drive)

| repo folder | Drive source folder | contents |
|---|---|---|
| `omni-claw-defined/` | `OMNI.CLAW_DEFINED` | project vision, blueprint, knowledge-synthesis architecture, repo-fork asset list, welcome doc |
| `research-npu/` | `Research.NPU` | Scaling LLM Test-Time Compute on Mobile NPU (EuroSys '26) distillation |
| `proofs/` | `PROOFS` | on-device PoC precedents: Overlayd-AI, SocketSweep |
| `fragmented-qat/` | `Fragmented QAT` | FraQAT (fractional-bit W4A8 QAT, deployed on S25U / SM8750 HTP) |
| `google-dev-docs/automated-build-w-github/` | `GOOGLE.DEV_DOCS/Automated build w GitHub` | GitHub Actions Android APK/AAB build workflow reference |
| `gemini-query/` | `GEMINI.QUERY` | three Gemini research-query exports (Qwen3.5-9B compile/quant, Q4_0 on Hexagon v79, July-3rd Horizons APK) — Google Docs exported to Markdown |

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
