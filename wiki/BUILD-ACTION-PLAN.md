# Build Action Plan — remaining app build + repo hygiene

> Step-by-step execution plan to finish the Horizons app and clean the repo.
> Grounded in CLAUDE.md's State of the Union section. Principle: **the app runs fully on
> cloud reasoning + CPU voice with NO on-device model** — HTP/GenieX is an
> optional backend added last, not a blocker.

## Hard constraints (shape the whole plan)

- **Parallel agents/sessions must touch DIFFERENT files.** Same-file concurrent
  edits collide. Streams below are partitioned by file ownership.
- **The APK builds in CI, not locally** (no JDK/Gradle/SDK on device). Every
  code change is verified by `build-apk.yml`, not a local build.
- **Native daemons + on-device run = device/Termux session**, not the repo
  session. The repo session writes Kotlin/docs; the device session builds/runs
  binaries (GenieX, media daemon) and installs Tailscale/SDKs.
- **One committer.** Only the repo session pushes to
  `claude/notice-agent-ui-local-xa14op` (the one active branch — the earlier
  sae7cy branch this plan originally named is merged). The device session
  stays read-only (inventory/verify) to avoid collisions.

## Session topology

| Session | Where | Owns | Notes |
|---|---|---|---|
| **A — Repo/App** (this one) | cloud env | Kotlin app code, cloud connectors, vision, chat-export, docs, CI, repo hygiene | THE committer. Spawns sub-agents for parallel file work. |
| **B — Device/Native** | Termux on the phone | extract QAIRT/Hexagon SDKs, run `geniex serve` (Q4_0 GGUF, HTP v79), build the **media daemon** (Moonshine STT + Silero VAD + Kokoro TTS, CPU), install Tailscale, on-device verify | operator-run; read-only on the repo. |
| **C — Compile** (optional, parallel) | env w/ huggingface.co egress | QAI Hub compile of Qwen3.5-9B (Job 8) → AI Hub bundle | independent; feeds the GenieX QAIRT backend later. Prompt already provided. |

## Work streams — Session A (parallel-safe, partitioned by file)

Run these as sub-agents where marked; they own disjoint files so they can run
concurrently. Shared files (`SettingsPane.kt`, `CloudLlmRuntime.kt`,
`AppStateStore.kt`) are owned by exactly ONE stream to avoid conflicts.

| # | Stream | Owns (files) | Sub-agent? |
|---|---|---|---|
| S1 | **Vision via cloud** — override `streamImage()` (base64 JPEG → OpenAI vision format); wire `ScreenshotCapture` MediaProjection consent UI launch path | `CloudLlmRuntime.kt`, `core/screen/ScreenshotCapture.kt`, `MainActivity.kt` | agent |
| S2 | **Connectors (new files only)** — `GcsConnector`, `GitHubConnector`, `OmniRouteProvider`, `QaiHubConnector` as new classes + agent tools; register in `AgentLoop` tool list | new files under `core/connect/` + `core/agent/tools/`, `AgentLoop.kt` | agent |
| S3 | **Chat-history export** — add export/sync (JSON/Markdown out, optional Drive/GCS push) | `core/state/ChatHistoryStore.kt` + new `ChatExporter.kt` | agent |
| S4 | **Settings UI + Tailscale hook** — Settings entries for the new connectors/keys + a Tailscale "home node" URL setting; owns the shared `SettingsPane.kt`/`AppStateStore.kt` so S2/S3 don't | `ui/panels/SettingsPane.kt`, `core/state/AppStateStore.kt` | inline (shared file — keep single-owner) |
| S5 | **NpuClient → GenieX** — swap `:8080 /api/v1/generate` → `:18181 /v1/chat/completions` (OpenAI), `/v1/models` readiness; media-daemon URL already in `DaemonSttClient` | `core/llm/NpuClient.kt` | inline |
| S6 | **Repo hygiene** — dedup + stale cleanup (audit agent running) | docs only (`wiki/`, `rules/`, stale dirs) | agent (audit) + inline (edits) |
| S7 | **De-Qwen-ify status strings** — runtime-agnostic UI text (kill hardcoded "Qwen3.5-9B / ort_engine / Hexagon" labels) | `RouterPane.kt`, `MonitorPane.kt`, `HorizonsPane.kt`, `HorizonsApplication.kt` doc comments | inline |

**Conflict rule:** S1 owns `CloudLlmRuntime.kt`; S2 must NOT edit it (add
providers via new files + an extension point). S4 owns `SettingsPane.kt` +
`AppStateStore.kt`; S2/S3 add keys via S4 or request them. If a stream needs a
shared file, it hands the edit to that file's owner.

## Sub-agent deployment (how many, what brief)

- **Now (running):** 1 Explore agent — read-only dedup/stale audit (S6). Returns
  a list; the repo session does the edits.
- **Next wave (up to 3 concurrent), each briefed per the CLAUDE.md sub-agent
  template** (repo + branch, read CLAUDE.md's State of the Union first, exact task,
  disjoint file set, do NOT commit/push — return a diff/patch for the repo
  session to apply and commit):
  - Agent-Vision (S1)
  - Agent-Connectors (S2)
  - Agent-ChatExport (S3)
- The repo session keeps S4/S5/S7 inline (shared files / small), applies each
  agent's returned diff, runs the mental compile check, and commits.
- **Cap agents at a 1h TTL.** If an agent hasn't returned, it's dead — do that
  stream inline. (This project has lost background agents before.)

## Execution sequence

1. **Repo hygiene first** (S6) — delete duplicates / fix stale so agents build on
   a clean tree. (Audit agent running; apply its findings.)
2. **Wave 1 (parallel):** S1 Vision, S2 Connectors, S3 Chat-export as agents;
   S4 Settings + S5 NpuClient + S7 status-strings inline.
3. **Integrate:** apply agent diffs, resolve the shared-file handoffs, push —
   CI verifies each.
4. **Hand to Session B:** device session runs `geniex serve` (Q4_0) + builds the
   media daemon (Moonshine/Silero/Kokoro) + Tailscale, verifies voice + LLM
   on-device against the new client code.
5. **TTS build fix:** ensure `horizons/libs/sherpa-onnx-1.13.2.aar` fetch stays
   CI-only (already is); no repo change needed.

## Definition of done

- App boots and is fully usable on **cloud reasoning + cloud vision**, no model.
- Connectors: OpenRouter ✅, + OmniRoute, GitHub, HuggingFace, QAI Hub, GCS wired.
- Chat history exportable. Tailscale home-node setting present.
- Voice loop reaches the media daemon (Moonshine STT / Kokoro TTS) — daemon built
  in Session B.
- GenieX (`:18181`) is a selectable local backend, not required to boot.
- Repo: **no duplicate files, nothing out of date** (S6 done).
- CI green on every push.
