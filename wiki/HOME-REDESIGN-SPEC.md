# HOME-REDESIGN-SPEC — Living Spec (hand session-to-session)

> **What this is.** The canonical, operator-approved specification for the
> Novus Agenti **home screen** (`HomeGrid.kt`) redesign. It exists so no
> session — including this one after a context reset — ever has to
> reconstruct the intent from scratch again. Update it **in place** as the
> operator refines direction. When a home-screen visual task starts, READ
> THIS FIRST.
>
> **Guiding principle.** The renders here are a **visual TARGET / mockup —
> REFERENCE ONLY.** They are NOT screenshots of a live or near-complete app;
> **nothing in the current UI is anywhere near complete.** Use the references to
> define what to build toward: match the target's colors, labels, sizes,
> proportions, and card format rather than reinventing. Past runs failed by
> rebuilding everything at once; this spec exists to prevent that.
>
> **Verification rule:** any change here MUST end with a real rendered
> screenshot (device/emulator) compared against the references below before
> it's called done. The cloud build container has **no Android SDK**, so the
> operator is the on-device visual check — ship in small, verifiable passes,
> never one blind dump.

---

## How to use the reference images

The actual reference screenshots are **committed** under
`wiki/home-redesign-img/` (recovered from the session transcript). Each `![]()`
link in this doc points at the real image, and each still carries a written
description so the spec is usable even from a plain-text read. If the operator
supplies a better/newer screenshot, replace the file in place (same name).

---

## Layout geometry

- **Seven elements arranged as a symmetrical wheel:** the center **Router hub**
  plus **6 tiles** on a clock face.
  - `12:00` Monitor · `2:00` Chat · `4:00` Settings · `6:00` Terminal ·
    `8:00` Archives · `10:00` Horizons.
- The Router is **centered within the ring** (all 7 symmetrical). It is **not**
  offset relative to the tiles.
- The **whole wheel is nudged slightly UP** (toward the upper half of the
  screen) as a unit. This is intentional — it leaves room at the bottom for the
  chat-bar expansion (see below).
- **Spacing fix:** the `2/4/8/10` tiles are currently **too crowded toward the
  top & bottom center** — spread them out.
- **Bottom row** of tiles can **expand downward a touch** — that frees vertical
  room for the **Router hub label**.

---

## Per-tile spec

Everything not called out as a change stays **identical** (color, label, size,
card style).

> **Color note — Chat green ≠ Terminal green (they are NOT the same).**
> **Terminal** has a **deeper, near-black card background** and a **brighter,
> more saturated matrix-green**. **Chat** uses its own softer green on a less-black
> background. Do not equalize the two.

> **Label format.** Each tile reads: **TITLE** · `/slug` · a short **subtitle** ·
> and a **bottom prompt line** (`$_…`). The corrected values are below.

### 12:00 — MONITOR  (teal/cyan)
- **Labels:** title `MONITOR` · slug `/cognito` · subtitle `library` · bottom
  prompt line `$_browser`.
- **Icon:** the **display/screen** glyph — rounded rect, 2 horizontal lines
  inside, a tail/legs at the bottom — carrying a small **`PC`** badge
  (top-right). *This replaces the "AI" badge seen in the current build. There
  is no "AI" label wanted anywhere.*
- Attach-slot: `![monitor icon](home-redesign-img/02-monitor-icon.webp)`
  — *green display/screen icon, 2 inner lines, tail, circular "AI" badge
  top-right (badge must become "PC").*

### 2:00 — CHAT  (green — its OWN softer green, NOT Terminal's; see color note)
- **Labels:** title `CHAT` · slug `/interface` · subtitle `tools` · bottom
  prompt line `$_model`.
- **Correct, keep:** size, overall style, its green color.
- **Change 1 — icon:** replace with a **simple, clean speech bubble** (folded
  tail at bottom-left, two short lines inside). NOT a hub-and-spoke node
  network, NOT the display/PC glyph.
- **Change 2 — overlap:** a neighboring tile's icon currently **overlaps** the
  Chat tile. Fix the layout collision so tiles/icons don't overlap.
- Attach-slots:
  - `![chat correct icon](home-redesign-img/04-chat-icon-correct.webp)`
    — *clean green speech bubble, "CHAT / interface".*
  - `![chat overlap bug](home-redesign-img/03-chat-tile-overlap.webp)`
    — *display+"AI" icon bleeding over the Chat tile.*

### 4:00 — SETTINGS  (pink/crimson)
- **Labels:** title `SETTINGS` · slug `/config` · subtitle `vault` · bottom
  prompt line `$_utils`.
- **Icon: no change** — pink **sun/flash inside a dashed ring**.
- Attach-slot: `![settings tile](home-redesign-img/10-settings-tile.webp)`

### 6:00 — TERMINAL  (green — DEEPER near-black bg + BRIGHTER matrix-green; see color note)
- **Labels:** title `TERMINAL` · slug `/shell` · subtitle `commands` · bottom
  prompt line `$_`.
- **Icon: no change** — green **terminal-window** icon (title-bar dots + `>_`).
- Attach-slot: `![terminal tile](home-redesign-img/09-terminal-tile.webp)`

### 8:00 — ARCHIVES  (amber)  *(currently mislabeled ARTIFACTS)*
- **Labels:** title `ARCHIVES` (was ARTIFACTS) · slug `/logs` · subtitle
  `artifacts` · bottom prompt line `$_files`.
- **Icon: no change** — amber, stacked-documents.
- Attach-slot: `![horizons + archives tiles](home-redesign-img/11-horizons-archives-tiles.webp)`

### 10:00 — HORIZONS  (blue)
- **Labels:** title `HORIZONS` · slug `/about` · subtitle `credits` · bottom
  prompt line `$_.home`.
- **Change — the sun turns AMBER:** in the icon (amber sun + rays over a blue
  horizon line + pale pinkish-purple arch), the **sun** element becomes
  **amber**; the rest of the icon stays blue.
- Attach-slot: shares `11-horizons-archives-tiles.webp` above.

---

## Center hub — ROUTER

**The whole Agent Platform reference image IS the target for the ENTIRE hub —
not just the crystal.** Build the hub after it, with three deliberate changes.

- **Take from the Agent Platform reference:** the **plasma tubes** (glowing
  radiating connectors), the **nodes** (surrounding node points), the **platform
  perimeter** (the elliptical platform base/ring), and the **protruding central
  faceted crystal**.
- **Change 1 — dome → white sun:** drop the reference's dome; use the **white
  sun / radial aura** instead. **No dome, no concentric-ring framing.**
- **Change 2 — crystal size:** the crystal is a hexagonal faceted gem **shaped
  like the Agent Platform gem, just slightly LARGER than that gem.** That is
  **much SMALLER** than the current build's crystal — the "wizard-hat"-looking
  purple thing from the past build is **grossly oversized; shrink it.**
- **Change 3 — violet hue + under-glow:** the gem is **violet/purple** (not the
  reference's blue), with the **white glow permeating from underneath** (bright
  core radiating up from beneath the crystal).
- **Label — stacked under the hub icon (NOT `/ route`):**
  - `// CORE_HUB` — slug, at the **top, right under the icon** (dim)
  - **`ROUTER`** — big, in the **off-white** used by other secondary on-screen
    text (**NOT violet**)
  - `$_Statio` — the **only** thing underneath ROUTER
- Attach-slots:
  - `![agent platform — whole-hub target](home-redesign-img/06-agent-platform-hub.jpg)`
    — *central hexagonal gem + nodes + platform perimeter + plasma connectors.
    THE reference for the entire hub (then: swap dome→sun, bigger violet crystal).*
  - `![ai orchestration hub](home-redesign-img/07-ai-orchestration-hub.webp)`
    — *glowing hub with plasma-cord connectors radiating out.*
  - `![router label + cords](home-redesign-img/08-router-label.webp)`
    — *"// CORE_HUB / ROUTER" over crossing plasma tubes. NOTE: label is now
    off-white `ROUTER` + `$_Statio`, no `/ route`.*
  - `![router circuit chip](home-redesign-img/05-router-circuit-blue-chip.jpg)`
    — *chipsets-as-nodes / 6-connector inspiration.*

---

## Connector cords

- **Plasma-tube styling:** glowing, layered tubes (soft outer glow → bright
  core), **each in its tile's color** — green→Terminal, amber→Archives,
  pink→Settings, teal/blue→Monitor/Chat/Horizons.
- **Beads/nodes** run along each tube.
- Six cords, one from each tile-node into the Router hub.

---

## Banner (top logo)

- **Placement is PERFECT — do not move it.**
- **Logo font — WRONG, fix it:** the `MØ[)u14R_11(` logo uses the wrong
  typeface. Color (green) is correct. Match the **chunky monospace/terminal
  font** in the font reference.
- **Motto format — WRONG, fix it:** `*Pioneer_Tech · (Next-Gen Certified)`
  must read as **one continuous, unbroken line** across the screen (fit on one
  line), with **`(NEXT-GEN CERTIFIED)` in parentheses**, and the
  **`HORIZONS // V4`** version string on the **bottom-right**. Color and font of
  the motto are correct — only the wrapping/format is wrong.
- Attach-slot: `![logo font](home-redesign-img/19-logo-font.webp)`
  — *"cat << 'EOF' / MØDU14R_11( / *Pioneer_Tech, / (Next-Gen Certified) /
  v1.0 / EOF" in a chunky blocky monospace font over wet slate.*

---

## Background (home)

- **Base is DONE** (already shipped): deep black with a faint blue tint,
  brighter/haloed stars, more-visible astral telemetry map. (`drawAstralBackground`.)
- **Operator note:** the color hue was *almost* perfect but the **background
  styling is still slightly off** — revisit after the forefront layers land.

---

## Chat bar (bottom input)

- **Placement + the bottom status nodes (ASR/LLM/TTS/MLLM/VAG) are PERFECT —
  do not move them.**
- **Behavior change:** holding the chat bar should open a **mini UI, not just a
  shortcut** to the Chat tile. **Hold → it pops up to ~⅓ screen for quick
  inference.** (This is why the wheel sits slightly high — to leave room for
  this expansion.)

---

## Colors — header / bold / outline

- **Header text, bold text, and outline colors** use the **purple/magenta from
  the nebula lock-screen** (the color of the `09:57` clock) — a hot
  violet-magenta.
- **Exception:** the **ROUTER** hub title is **off-white**, not this purple (see
  Router hub). So: nebula-purple for headers/bold/outlines generally; off-white
  for the ROUTER title specifically. ⚠️ *Confirm this split with the operator.*
- Attach-slot: `![nebula purple text](home-redesign-img/22-nebula-purple-text.webp)`
  — *the magenta-purple of the clock text is the header/bold/outline color.*

---

## Easter eggs & guardians

- **GOAT — crash-log easter egg.** On runtime crash/fail the **goat** pops up
  (`// GOAT_SAYS_NO`) with a synthesized bleat; a manual unlock (7 banner taps →
  `// GOAT_UNLOCKED`) also exists. Already wired (`HomeGrid.kt`
  `showGoat` / `playGoatBleat`).
  - `![goat easter egg](home-redesign-img/24-goat-easter-egg.webp)`
- **CHONK — screen-timeout guardian.** The idle / screen-timeout screensaver
  loads the **chonky orange cat** ("guardian chonk") from device storage.
  Partly wired (`Screensaver.kt`).
  - `![guardian chonk](home-redesign-img/23-guardian-chonk.png)`

---

## What is PERFECT — do not touch

- Top **logo placement**.
- **Bottom chat bar** placement.
- **Bottom status nodes** (ASR / LLM / TTS / MLLM / VAG) placement.
- Overall **spacing ratios, size ratios, color hues**.

---

## Reference: the target render — REFERENCE ONLY (not a live screenshot)

**This image is a REFERENCE MOCKUP of where the home screen should end up. It is
NOT a screenshot of the live app and does NOT reflect current state — nothing in
the real UI is near complete.** Use it strictly as the visual target.

- Attach-slot: `![target home](home-redesign-img/01-target-full-home.webp)`
  — *Black bg; green `MO)u14R_11(` banner; clock-face tiles; center purple
  crystal ROUTER/route with aura; plasma cords; bottom SYSTEM_STATUS dots; input
  bar "tap_or_hold ask //". Treat as target, not current state.*

---

## Panel backgrounds (separate track — mostly shipped)

Not part of the home wheel, but part of the same living design. Each of these 8
panels has a procedural background; **4 are also uploadable-wallpaper capable**
(Chat, Horizons, Archives, Settings — image fully replaces procedural, tiles go
semi-transparent). Home/Monitor/Terminal/Router stay procedural.

| Panel | Procedural bg | Wallpaper? | Reference image slot |
|---|---|---|---|
| Home | astral (deep black + stars + telemetry) ✅ | no | `01-target-full-home.webp` |
| Terminal | Matrix rain (`fakesteak` fork) | no | `18-matrix-rain.png` |
| Router | circuit-board / chip nodes | no | `05-router-circuit-blue-chip.jpg` |
| Monitor | sliding oscilloscope (animated) | no | `17-oscilloscope.webp` |
| Chat | wet blue-grey slate stone ✅ | yes | `14-stone-slab.webp` |
| Horizons | butterfly nebula (gold/blue-white)* ✅ | yes | `13-nebula-wallpaper.webp` |
| Archives | vintage film strip | yes | `16-film-strip.jpg` |
| Settings | brushed-steel vault door | yes | `15-vault-door.jpg` |

\* Palette note: written spec §4 said "purple/blue/gold", but the operator's
actual nebula reference is **gold + blue-white, minimal purple** — the image
wins. Nebula is also a candidate to use a **real image asset** rather than
procedural (operator leaned that way for photographic fidelity).

---

## Status ledger

- ✅ Home background layer (deep black + stars + telemetry) — shipped.
- ✅ Panel backgrounds: Chat slate, Horizons nebula, deeper Router/Monitor bases — shipped.
- ✅ Uploadable wallpapers on Chat/Horizons/Archives/Settings — shipped.
- ⛔ Home wallpaper — **reverted** (operator said no; home is procedural only).
- ⬜ **Home forefront redesign — PENDING (this spec).** Banner font/format,
  tile icon/label swaps, tile spacing, Router crystal/label/cords, background
  styling polish, chat-bar hold-to-⅓-screen mini UI.

---

## Open / to-confirm

- **Header purple vs ROUTER off-white:** confirm the split — nebula-purple for
  headers/bold/outlines, off-white for the ROUTER title specifically.
- **Horizons bottom prompt:** used `$_.home` (the cleaner of `$_version.s` /
  `$_.home`); confirm if wrong.
- **Exact tile card style:** operator says the tiles are "the wrong style" but
  the crisp target isn't fully pinned — resolve against the reference target
  render before restyling cards.
- **PENDING (big) — image-forefront / verbatim restructure:** operator wants
  the photos moved to the forefront (image-first, on top of everything) with
  exact word-for-word descriptions and comparisons quoting the operator's own
  prompts near-verbatim. Not yet executed.
- **Logo font asset:** matching the exact typeface likely needs a bundled
  `.ttf`; `FontFamily.Monospace` is the closest built-in stand-in until a font
  file is added.
