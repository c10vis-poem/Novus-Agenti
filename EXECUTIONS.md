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

Full visual spec: **`wiki/HOME-REDESIGN-SPEC.md`** (read it THOROUGHLY before
touching `HomeGrid.kt`). Reference images: **`wiki/home-redesign-img/`**.
**Every pass ends with an on-device screenshot vs. the references before "done."**

### The rule (session 21, operator-confirmed)

The prior build is the target. **The ONLY layout change from the prior build is:
chat bar renders ABOVE config nodes (already done, V.11).** Everything else —
tiles, icons, crystal, cords, status nodes, background, aspect ratio — matches
the prior build exactly. Do NOT redesign from scratch; match what worked.

**Key reference images (all in `wiki/home-redesign-img/`):**
- ![prior build target](home-redesign-img/33-prior-build-full-home-target.png) — THE full-screen layout/aspect-ratio/background/crystal/status-nodes target. Match this for overall proportions.
- ![horizons tile](home-redesign-img/07-horizons-tile.webp) ![chat tile](home-redesign-img/08-chat-tile.webp) ![terminal tile](home-redesign-img/09-terminal-tile.webp) ![settings tile](home-redesign-img/10-settings-tile.webp) ![monitor tile](home-redesign-img/11-monitor-tile.webp) ![artifacts tile](home-redesign-img/12-artifacts-tile.webp) — prior build tile closeups. THE tile detail target: icon style, card size, text layout, prompt box, backlighting.
- ![agent platform hub](home-redesign-img/06-agent-platform-hub.jpg) — crystal geometry/style target (hexagonal faceted gem on elliptical platform).
- ![router crystal closeup](home-redesign-img/34-router-crystal-closeup.png) — crystal color/hue target (violet + white sun glow underneath).
- ![logo font](home-redesign-img/19-logo-font.webp) ![logo top crop](home-redesign-img/39-logo-font-top-crop.png) ![logo bottom crop](home-redesign-img/38-logo-font-bottom-crop.png) — logo font target.
- ![v3 build stars](home-redesign-img/26-v3-full-build.png) — star field reference (copy the STARS and telemetry circles from this, NOT the grid).

### On-device findings (session 21) — what's broken

| # | Element | What's wrong | Fix (with reference image) |
|---|---|---|---|
| V.1 | **Background** | Was a washed-out foggy gray gradient (`#1A222A`/`#222C34`) with obsidian facets adding haze. Stars and telemetry circles exist in the code but get lost in the wash. | Near-black base (`#080C10` range) — fixed in commit `f16ff33`, needs on-device re-verify. The home screen MUST have: (1) **stars** — subtle pinpoint star field like the standby screen and the V3 build ref ![v3 stars](home-redesign-img/26-v3-full-build.png), and (2) **telemetry circles** — faint concentric rings around the hub area + 2–3 extra clusters at varying positions around the screen (not all centered on hub). Both are in the code but invisible under the old gray wash. |
| V.2 | **Tile card size** | Cards too small (108×130dp) — tile names truncate on device: HORIZ, ARCHIV, SETTIN, TERMIN. | Match the prior build's tile card size and shape — scale the WHOLE card up proportionally (keep the same aspect ratio, don't just widen). The font size should match the prior build's tile font size too — scale it up to fit the bigger card. Do NOT shrink text to fit a small card; make the card the right size and use the right font size. Ref: prior build tiles in ![prior build](home-redesign-img/33-prior-build-full-home-target.png) and tile closeups (`07`–`12`). |
| V.3 | **Tile icons — COMPLETE REBUILD** | Every icon is way too small (40dp), they're all different sizes from each other, and none match the prior build's style. The colors are the only thing close to correct but even those aren't shaded right — flat/thin strokes instead of the prior build's filled/shaded/detailed rendering. This is a full icon rebuild, not a tweak. | **Every icon must be rebuilt from scratch to match the prior build's tile closeups (`07`–`12`).** All icons must be the SAME size as each other (large — dominating the top of the card, not tiny afterthoughts). Match the prior build's filled/shaded/detailed rendering style with vivid backlit glow, not thin wireframe strokes. **Horizons icon specifically:** same shape as prior build (arch + horizon line + sun) but three DISTINCT colors — pinkish-purple arch, blue horizon flatline, amber sun (three different colors, not monochrome). |
| V.4 | **Tile prompt box** | `$_` command text and gear icon float bare at the card bottom — no visual container. | Each tile needs a distinct outlined/bordered prompt box at the bottom containing the command hint + gear icon. Contents per tile: MONITOR `$_browser`, CHAT `$_model`, SETTINGS `$_utils`, TERMINAL `$_bash`, ARCHIVES `$_files`, HORIZONS `$_.home`. Gear icon right-aligned inside the box. Ref: prior build tiles show this box clearly. |
| V.5 | **Tile text layout** | Letter-spacing (2sp) eats horizontal space causing truncation. Slug and subtitle are crammed onto one line. | Reduce letter-spacing enough to fit full names. Text stack inside card: TITLE (bold) → /slug line → subtitle line → divider → prompt box. Properly spaced, not crammed. Ref: prior build tile closeups. |
| V.6 | **Hub crystal** | Renders as a tilted wizard-hat prism. Wrong shape, wrong proportions. | Proper 3D hexagonal faceted gem, centered and upright, sitting on an elliptical platform base. Ref: `06-agent-platform-hub.jpg` for geometry/structure, `34-router-crystal-closeup.png` for color (violet crystal + white sun glow permeating from underneath). Match the prior build's crystal size — big enough to be the centerpiece but not so big it crowds the tiles. |
| V.7 | **Status nodes** | 42dp spheres are too small. The `Surface` container behind them is washed-out gray. | MUCH bigger vivid glossy 3D spheres — match the prior build's status nodes which are large, vivid, with strong specular highlights and glow halos. Container background should be dark/near-black, not the light gray `Surface`. Ref: prior build config nodes closeup shows the target size and vibrancy. |
| V.8 | **Plasma cords** | Bezier curves are in the code but don't render like the prior build's glowing tubes on device. | Match the prior build's cord style: visible glowing colored tubes connecting each tile to the hub, with energy beads traveling along them. Each cord is the tile's color. Ref: `33-prior-build-full-home-target.png` shows the cords clearly. |
| V.9 | **Aspect ratio** | Everything is compressed vertically. Tiles are cramped together, not enough breathing room. | Match the prior build's overall proportions from `33-prior-build-full-home-target.png`: how big tiles are relative to the hub, how much space between the clock wheel and the bottom bar, how spread out the elements are across the full screen height. |
| V.10 | **Logo font** | Using system monospace instead of the chunky blocky terminal face shown in the reference. | Match the font from `19-logo-font.webp` / `39-logo-font-top-crop.png`. This is a specific chunky/blocky typeface, not generic monospace. May require bundling a custom font asset (e.g. a `.ttf` in `res/font/`). |
| V.11 | **Chat bar position** | Chat bar renders above config nodes. | **CORRECT — this is the one change from the prior build. Keep it.** |

---

## Not-yet-read (confirm before acting on these areas)

`MonitorPane.kt` (green-light render + does it trigger anything?), `SettingsPane.kt`
(vault/import), `TerminalPanel.kt` (define/export flow), `AppStateStore` full,
`ArchiveStore.kt`, `NpuClient.kt` full, `LocalHomeActivity/Screen` (the additive
fork — reconcile with the seven-tile home or retire it). Add rows as read.

---

_Build dock replaces the earlier scaffolding EXECUTIONS.md. Update in place as
items land; SOTU points here as the running build state._
