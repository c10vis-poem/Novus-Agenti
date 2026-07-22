# EXECUTIONS.md — Horizons Running Build Dock

> **What this is.** The single, code-anchored work list a build session is
> pointed at: "go here, do this." Read **after** CLAUDE.md + the canon bundle,
> **before** touching code. Each row: **Canon requirement → Current code
> (file:line) → Verdict → Exact action.** Verdicts: **KEEP** (matches canon,
> don't rebuild) · **CHANGE** · **CONTRADICTS** (actively fights canon) ·
> **MISSING**.
>
> **Canon this is measured against** (all mandatory-read):
> `knowledge/omni-claw-defined/` (what the app is) + the workbench docs (how it
> works: *"Daemons stay dumb, the user is the loader"* — Define→Validate→Execute,
> boots empty, nothing runs until a fuse is flipped). **The target architecture
> is `knowledge/omni-claw-defined/workbench/00-TILE-HUB-ARCHITECTURE.md`** — the
> explicit tile→hub definition. The runtime pipeline (Terminal→Monitor→Router)
> was never properly defined or implemented; **build to that doc, NOT to the old
> `RuntimeDefStore`/`RouterPane`/`CliffordService` code, which only assumed it.**
> Visual canon is `wiki/HOME-REDESIGN-SPEC.md` (read only when working the V-track).
>
> **Operating rule (operator):** every session drives its assigned items to
> **100% — mechanical AND visual, shoot for perfection.** No "get it close."
> No blind dumps. Every visual change ends with a real on-device screenshot vs.
> the reference (operator is the on-device check; cloud has no Android SDK).

---

## The central contradiction (read first)

The app currently ships **two conflicting runtime models**:

1. **Canon fuse-box** — Terminal *defines* a `RuntimeDef` → Monitor *green-lights*
   → Router *flip* runs it. Built as **UI + validation only**; the flip does
   **not** execute anything.
2. **Legacy Clifford auto-launcher** — `CliffordService` independently
   **auto-launches the daemon at boot** the moment a binary + any model file
   exist, ignoring the Router entirely.

Canon says #1 is the law and daemons never auto-start. So the spine of this
build is: **make the Router fuse the only thing that starts a runtime, and make
Clifford a dumb supervisor of what the fuse started — never an auto-starter.**

---

## P0 — Stable boot (nothing auto-loads). HIGHEST PRIORITY.

| # | Canon | Current (file:line) | Verdict | Action |
|---|---|---|---|---|
| 0.1 | Boots empty; a landed model is *acknowledged, never grabbed* until the user flips it. | `HorizonsApplication.resolveNpuModelPath()` :362–401 — after the user-pin check, **falls through to auto-detection**, scanning `models/`, `filesDir`, `/Download` for the newest LLM file and returning it. | **CONTRADICTS** | Delete the auto-detection fallback. Return a path **only** if `KEY_ACTIVE_MODEL` is pinned AND readable; otherwise `null`. Detection may inform the *Monitor UI* ("available to plug in") but must never feed the launcher. |
| 0.2 | Runtimes spin up only on an explicit fuse flip. | `CliffordService` CRS loop :116–203 `ensureDaemonRunning()` auto-launches whenever `resolveNpuModelPath()` ≠ null; Clifford is started at boot from `HorizonsApplication.onCreate()` :309. | **CONTRADICTS** | Clifford must launch **only** a runtime the Router marked RUNNING. Gate `ensureDaemonRunning()` behind "is there a RUNNING RouterConfig?" Once 0.1 lands (null until pinned) this is half-fixed, but make it explicit: no RUNNING config ⇒ Clifford supervises nothing. |
| 0.3 | The STT/TTS media layer is a **separate** persistent layer, not part of the model-runtime fuse pipeline (its own deal; see 00-TILE-HUB-ARCHITECTURE §"Separate layer"). | **DONE (session 19b):** voice layer is ONE in-process path — Moonshine STT + Kokoro TTS via sherpa-onnx + Silero VAD on both ends (endpoint + barge-in). The dead media-daemon contract (`DaemonSttClient`/`DaemonTtsClient`, :8091) is deleted; `KokoroModelManager` no longer downloads (user imports the archive from device storage). Boot: `ensureReady()` is now a disk check only; Moonshine loads on IO thread only if its files are imported. | **DONE** | Remaining: operator loads the Kokoro + Moonshine archives on device and verifies mic → VAD stop → transcript → reply → TTS, and barge-in mid-sentence. |
| 0.4 | — | `CliffordService.start()` at `onCreate()` :309 | **KEEP (as supervisor)** | Clifford-as-FGS is fine and needed for perf-lock + crash recovery. It's the *auto-launch behavior* (0.2) that's wrong, not Clifford's existence. |

**P0 exit test:** fresh install, GGUF present in Download, **no** config flipped →
app boots to a stable home, **no daemon process running** (`pidof ort_engine`
empty). This is the regression that took it from "ran fine" to "won't boot."

---

## P1 — Wire the fuse box to actually execute as defined.

| # | Canon | Current (file:line) | Verdict | Action |
|---|---|---|---|---|
| 1.1 | Flipping the Router fuse runs the plated runtime. | `RouterPane.switchOn()` :79–93 re-runs greenLight (✓) then only `setStatus(RUNNING)` + `llmRuntime.preWarm()`. | **KEEP validation / MISSING execution** | Keep the just-in-time greenLight re-check (canon-correct). On green, actually **start the runtime**: hand the `RuntimeDef` (binary, port, argsTemplate, model) to Clifford/DaemonLauncher. |
| 1.2 | `preWarm()` should start/warm the plated backend. | `LlmRuntime.preWarm()` :28 is an **empty stub** `fun preWarm() {}`. | **MISSING** | Implement, or replace the call with a real "start this config" path (see 1.1/1.3). |
| 1.3 | Launcher runs *the defined* runtime (any binary/port/args), not a hardcoded one. | `DaemonLauncher` hardcodes `ort_engine`, port 8080, `--model {path}`. `CliffordService.ensureDaemonRunning()` :196 launches `--model modelPath`, ignoring `RuntimeDef.argsTemplate`/`port`/`healthPath`. | **CONTRADICTS** | Drive `DaemonLauncher` from the flipped `RuntimeDef`: substitute `{model}`/`{port}` into `argsTemplate`, use the def's `port`/`healthPath` for health checks. One launcher, parameterized — not ort-engine-specific. |
| 1.4 | Health check hits the def's endpoint. | `CliffordService.daemonHealthCode()` :211 + `NpuClient` hardcode `:8080/health`; geniex def uses `:18181/v1/models`. | **CHANGE** | Read host/port/healthPath from the RUNNING `RuntimeDef`. |

**P1 exit test:** define a runtime in Terminal → green in Monitor → flip in Router
→ that exact binary launches with the templated args → Chat shows "backend ready"
only after the flip.

---

## P2 — Real runtime (GenieX) + model round-trip.

| # | Canon | Current | Verdict | Action |
|---|---|---|---|---|
| 2.1 | GenieX is the decided primary runtime (`geniex serve`, `:18181/v1`). | `RuntimeDef.builtIns()` has a `geniex` def; **no geniex binary** wired; `ort_engine` is the only real daemon. | **MISSING** | Fork `qualcomm/GenieX`, wire `geniex serve` behind the def. Device already has `geniex-bench` + GGUF + HTP libs (verify, don't re-download — see device-inventory). |
| 2.2 | Import → plug-in → run works end to end (the one gap: "couldn't upload the model"). | `ModelImportActivity` + Settings import exist; plug-in via `KEY_ACTIVE_MODEL`. | **VERIFY/CHANGE** | Reproduce the model-upload failure, fix it. This is the last thing that was broken when the app otherwise ran. |
| 2.3 | `http_server.cpp` reads to Content-Length (vision payloads). | Single 8 KB `recv()` — truncates images. | **CHANGE** | Read until Content-Length before vision can round-trip. |
| 2.4 | Clifford is a **failover supervisor**: on engaged-runtime failure → graceful reboot + fail over to a **tiny on-device model** (no hard crash). The failover model is a **pluggable "failover" config**, not baked in. | Current Clifford just relaunches ort_engine with backoff then gives up (`DaemonState.Failed`); no failover, no tiny-model floor. | **MISSING** | Rebuild Clifford per `00-TILE-HUB-ARCHITECTURE §CliffordService`: designate a failover fuse (points at a user-plugged tiny model, cold until triggered, CPU-capable last resort); on primary failure spin it up + preserve session. |
| 2.5 | **Runtime-agnostic:** nothing model/engine-specific hardcoded (arch invariant #8). | `DaemonLauncher` hardcodes `ort_engine`/8080 (P1.3); `resolveNpuModelPath()` hardcodes Qwen filenames + "NPU" naming; `NpuClient` hardcodes the port. | **CHANGE** | De-hardcode: every engine/port/model/"NPU-only" is a per-config parameter. Fold into P1.3/P1.4 — this is the same sin app-wide. |

---

## P3 — The "amperage" check (canon 4-param fuse, param 3).

| # | Canon | Current (file:line) | Verdict | Action |
|---|---|---|---|---|
| 3.1 | Fuse verifies **hardware/memory amperage**: arch compat + enough free RAM (the OOM guard). | `RuntimeDef.greenLight()` :106–152 checks binary/exec/assets/model — **no RAM, no arch check**. | **MISSING** | Add an amperage check to `greenLight`: device arch vs. binary arch, and free RAM vs. model footprint. This is the structural OOM-crash guard the canon fuse is supposed to provide. |

---

## KEEP — matches canon, do NOT rebuild

- `RuntimeDef` data model + `greenLight()` static-only checks (no network/side
  effects) — `RuntimeDefStore.kt`. Canon-correct; only *add* amperage (3.1).
- `RouterConfig` fluid slots + `READY/RUNNING/SLEEPING/ARCHIVED` states +
  Sleep/Archive actions — `RouterConfigStore.kt`, `RouterPane.kt`. Matches
  "fluid slots, no fixed count, Sleep/Archive."
- The Router **just-in-time greenLight re-check at flip** — `RouterPane.switchOn()`
  :81–89. Canon-correct; keep exactly.
- `ArchiveStore` file manager, `SettingsStore` vault split. (Verify against canon
  in a follow-up pass — not yet fully read.)
- Failure UX hooks: `⚡ FUSE BOX` banner (`RouterPane.kt` :188), the Goat / 404-cat
  states. Keep; visual polish is V-track.

---

## V — Visual track (home dock). Pursue to 100% alongside mechanical.

**The spec is `wiki/HOME-REDESIGN-SPEC.md` — read it in full before touching
`HomeGrid.kt`.** Reference images in `wiki/home-redesign-img/`. Every change
ends with an on-device screenshot vs. the references.

**The rule:** the prior build (![33](home-redesign-img/33-prior-build-full-home-target.png))
is the target for EVERYTHING — proportions, spacing, tile size, icon size/style,
status nodes, background darkness, aspect ratio. The **only** layout change:
chat bar above config nodes (already done). Do not redesign from scratch.

**What's broken (session 21).** The spec (§2–§8) has full details per element.
This is just the status checklist:

| # | What | Status | One-line |
|---|---|---|---|
| V.1 | Background | Fixed to `#080C10` (commit `f16ff33`), **needs re-verify** | Was gray wash. Must also show **stars** (like standby screen / ![26](home-redesign-img/26-v3-full-build.png)) + **telemetry circles** (faint rings around hub + 2–3 extra clusters elsewhere). |
| V.2 | Tile card size | **BROKEN** | 108×130dp is way too small — names truncate. Scale whole card up proportionally to match prior build; scale font UP to match, never shrink text. |
| V.3 | Tile icons | **BROKEN — full rebuild** | All icons are tiny (40dp), different sizes, flat wireframe. Must be large, uniform size, filled/shaded/detailed, with backlit glow. See per-tile refs in spec §4a–§4f. |
| V.4 | Tile text layout | **BROKEN** | Missing slug + subtitle lines. Each card: title → slug → subtitle → divider → bordered prompt box (`$_command` ⚙). See spec §4. |
| V.5 | Hub crystal | **BROKEN** | Renders as tilted wizard-hat prism. Must be a proper 3D hexagonal faceted gem, upright, on elliptical platform. See spec §5. |
| V.6 | Plasma cords | **BROKEN** | Don't match prior build. Must be curved glowing tubes with energy beads, each in its tile's color. See spec §6 + ![35](home-redesign-img/35-plasma-tube-shield-nodes.png). |
| V.7 | Status nodes | **BROKEN** | 42dp is too small, container is gray. Must be large vivid 3D glossy spheres on dark/near-black bg. See spec §8. |
| V.8 | Chat bar style | **BROKEN** | Doesn't match prior build's clean teal-bordered dark look. See spec §7. |
| V.9 | Aspect ratio | **BROKEN** | Everything compressed vertically, cramped. Match prior build's spacious proportions. See spec §3. |
| V.10 | Logo font | **BROKEN** | System monospace instead of the chunky blocky face from ![19](home-redesign-img/19-logo-font.webp). May need a bundled `.ttf`. See spec §1. |
| V.11 | Chat bar position | **DONE** | Above config nodes — correct, keep it. |

---

## Not-yet-read (confirm before acting on these areas)

`MonitorPane.kt` (green-light render + does it trigger anything?), `SettingsPane.kt`
(vault/import), `TerminalPanel.kt` (define/export flow), `AppStateStore` full,
`ArchiveStore.kt`, `NpuClient.kt` full, `LocalHomeActivity/Screen` (the additive
fork — reconcile with the seven-tile home or retire it). Add rows as read.

---

_Build dock replaces the earlier scaffolding EXECUTIONS.md. Update in place as
items land; SOTU points here as the running build state._
