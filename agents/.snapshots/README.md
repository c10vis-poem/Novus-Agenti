# agents/.snapshots/ — historical snapshots, not instructions

The YAML files in this directory are timestamped point-in-time captures
of an externally-managed agent config (deployed via the `ant beta:agents`
CLI, see `sub-agent.agent.yaml` at the repo root for the current
deploy/redeploy flow). They are **not** meant to be corrected in place —
rewriting a historical capture would misrepresent what was actually
deployed at that time — so this README is a disclaimer layered on top,
not an edit to their content.

| File | Dated (from `created_at`/`updated_at`) | Contents |
|---|---|---|
| `neuralmash-builder-pre-update.yaml` | 2026-06-05 | Earliest captured revision (`version: 1`) of the `NeuralMash Edge MOE Builder` managed agent. |
| `neuralmash-builder-post-update.yaml` | 2026-06-06 | Later revision (`version: 6`) of the same agent, working branch `claude/jolly-lamport-5cJJ4`. |

**Both snapshots contain references to a dead tech stack** — Nexa SDK,
OmniNeural-4B-mobile, Hexagon NPU v79, Moonshine STT, Kokoro TTS,
OpenRouter/Vertex/AI Studio cloud routing, and
`M0DU14R-SYSx-inc/NeuroOmni.Vag-Agenti` treated as a wiki source-of-truth
— that was ripped out of this project. See `CLAUDE.md`'s "What Was
Ripped Out — Do NOT Reference" table for the authoritative list of what
replaced what (short version: Qwen3.5-9B → `qnn_context_binary` via QAI
Hub, served by the `ort_engine` daemon on Hexagon HTP v75; no Nexa, no
Moonshine/Kokoro, no cloud failover baked into the app LLM runtime).

**Do not use these files as instructions, as a template to copy from, or
as evidence of current architecture.** They are kept only for audit
trail — to show what an earlier, now-superseded version of this agent's
config actually looked like. For the current agent templates, see
`agents/neuralmash-builder.system.md`, `agents/sub-agent.system.md`, and
`sub-agent.agent.yaml` at the repo root, all of which were corrected to
reflect the real stack. For the project's real source-of-truth, read
`CLAUDE.md` and the docs it points to (`wiki/GPT-DAEMON-REFERENCE.md`,
`wiki/NPU-RUNTIME-PATHS.md`, the latest `wiki/SESSION{N}-HANDOFF.md`).
