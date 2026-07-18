# HOME-REDESIGN-SPEC — Living Spec (hand session-to-session)

> **What this is.** The canonical, operator-approved specification for the
> Novus Agenti **home screen** (`HomeGrid.kt`) redesign. It exists so no
> session — including this one after a context reset — ever has to
> reconstruct the intent from scratch again. Update it **in place** as the
> operator refines direction. When a home-screen visual task starts, READ
> THIS FIRST.
>
> **Guiding principle (operator's words):** the current *best* version of the
> home screen is already **mostly right**. Keep the colors, labels, sizes,
> proportions, and card format. Make **only the specific edits** listed here —
> nothing else. Past runs failed by rebuilding everything at once; this spec
> exists to prevent that.
>
> **Verification rule:** any change here MUST end with a real rendered
> screenshot (device/emulator) compared against the references below before
> it's called done. The cloud build container has **no Android SDK**, so the
> operator is the on-device visual check — ship in small, verifiable passes,
> never one blind dump.

---

## How to use the reference images

The image binaries are **not embedded** (they were pasted inline in chat, not
saved as files). Each reference below has:
- a **written description** detailed enough to work from without the picture, and
- an **attach-slot**: a path under `wiki/home-redesign-img/`. Drop the actual
  screenshot at that path with that filename and the `![]()` link renders.

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

### 12:00 — MONITOR  (teal/cyan)
- **Labels:** `MONITOR` / `/ cognito` / third line `$_browser`.
- **Icon:** the **display/screen** glyph — rounded rect, 2 horizontal lines
  inside, a tail/legs at the bottom — carrying a small **`PC`** badge
  (top-right). *This replaces the "AI" badge seen in the current build. There
  is no "AI" label wanted anywhere.*
- Attach-slot: `![monitor icon](home-redesign-img/02-monitor-icon.png)`
  — *green display/screen icon, 2 inner lines, tail, circular "AI" badge
  top-right (badge must become "PC").*

### 2:00 — CHAT  (green)
- **Correct, keep:** size, labeling (`CHAT` / `/ interface`), overall style,
  green color.
- **Change 1 — icon:** replace with a **simple, clean speech bubble** (folded
  tail at bottom-left, two short lines inside). NOT a hub-and-spoke node
  network, NOT the display/PC glyph.
- **Change 2 — overlap:** a neighboring tile's icon currently **overlaps** the
  Chat tile. Fix the layout collision so tiles/icons don't overlap.
- Attach-slots:
  - `![chat correct icon](home-redesign-img/04-chat-icon-correct.png)`
    — *clean green speech bubble, "CHAT / interface".*
  - `![chat overlap bug](home-redesign-img/03-chat-tile-overlap.png)`
    — *display+"AI" icon bleeding over the Chat tile.*

### 4:00 — SETTINGS  (pink/crimson)
- **No change.** Pink **sun/flash inside a dashed ring** icon; labels
  `SETTINGS` / `/ config` (full card also shows `Config · Matrix UI` / `$ config`).
- Attach-slot: `![settings tile](home-redesign-img/10-settings-tile.png)`

### 6:00 — TERMINAL  (green)
- **No change.** Green **terminal-window** icon (title-bar dots + `>_`); labels
  `TERMINAL` / `/ shell` (full card: `Shell · Matrix mode` / `$ _`).
- Attach-slot: `![terminal tile](home-redesign-img/09-terminal-tile.png)`

### 8:00 — ARCHIVES  (amber)  *(currently mislabeled ARTIFACTS)*
- **Change — label:** `ARTIFACTS` → **`ARCHIVES`**. Nothing else.
- Everything else identical: amber, stacked-documents icon, subtitle
  (`Logs · Prompts · Store`), `$ ls ./`.
- Attach-slot: `![horizons + archives tiles](home-redesign-img/11-horizons-archives-tiles.png)`

### 10:00 — HORIZONS  (blue)
- **Change — the sun turns AMBER:** in the icon (amber sun + rays over a blue
  horizon line + pale pinkish-purple arch), the **sun** element becomes
  **amber**; the rest of the icon stays blue.
- Labels unchanged: `HORIZONS` / `/ home` (full card: `Home · System view` / `$ home`).
- Attach-slot: shares `11-horizons-archives-tiles.png` above.

---

## Center hub — ROUTER

- **Form:** a **purple faceted crystal** styled as an **agentic hub** — the
  crystal is the central gem; the 6 tiles are its connected nodes
  (hub-and-spoke).
- **Size:** the crystal should **protrude a little MORE** than it does now —
  slightly more prominent. **But not** the stupid-big, slanted-over crystal from
  the color-reference image. Keep it reasonably sized and upright, not oversized
  or heavily tilted.
- **Glow:** keep the **glowing sun AURA** (radial glow around it). **No dome /
  concentric-ring framing.**
- **Color + under-glow:** not *just* purple — it also has a **white sun/glow
  permeating from underneath** (bright core radiating up from beneath the
  crystal). Keep both: violet body + white under-glow + purple aura.
- **Label (3 lines, violet):** `// CORE_HUB` (top, dim) · **`ROUTER`** (large,
  violet) · `/ route` (bottom, dim).
- Attach-slots:
  - `![router label + cords](home-redesign-img/08-router-label.png)`
    — *"// CORE_HUB / ROUTER / route" in violet over crossing plasma tubes.*
  - `![agent platform hub](home-redesign-img/06-agent-platform-hub.png)`
    — *central hexagonal gem, nodes around it, hub-and-spoke dotted lines.*
  - `![ai orchestration hub](home-redesign-img/07-ai-orchestration-hub.png)`
    — *glowing circular hub with plasma-cord connectors radiating out.*
  - `![router circuit chip](home-redesign-img/05-router-circuit-blue-chip.png)`
    — *blue PCB, glowing central chip/die, amber solder-point bokeh
    (chipsets-as-nodes / 6-connector inspiration).*

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
- Attach-slot: `![logo font](home-redesign-img/19-logo-font.png)`
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

## What is PERFECT — do not touch

- Top **logo placement**.
- **Bottom chat bar** placement.
- **Bottom status nodes** (ASR / LLM / TTS / MLLM / VAG) placement.
- Overall **spacing ratios, size ratios, color hues**.

---

## Reference: the baseline "best" render

- Attach-slot: `![baseline home](home-redesign-img/01-current-full-home.png)`
  — *THE target. Black bg; green `MO)u14R_11(` banner; clock-face tiles
  (Monitor/cognito, Chat/interface, Settings/config, Terminal/shell,
  Artifacts→Archives/logs_skills, Horizons/home); center purple crystal
  ROUTER/route with aura; plasma cords; bottom SYSTEM_STATUS dots; input bar
  "tap_or_hold ask //". Operator: "one of the best and one of the first
  versions — everything has gotten worse since then."*

---

## Panel backgrounds (separate track — mostly shipped)

Not part of the home wheel, but part of the same living design. Each of these 8
panels has a procedural background; **4 are also uploadable-wallpaper capable**
(Chat, Horizons, Archives, Settings — image fully replaces procedural, tiles go
semi-transparent). Home/Monitor/Terminal/Router stay procedural.

| Panel | Procedural bg | Wallpaper? | Reference image slot |
|---|---|---|---|
| Home | astral (deep black + stars + telemetry) ✅ | no | `01-current-full-home.png` |
| Terminal | Matrix rain (`fakesteak` fork) | no | `18-matrix-rain.png` |
| Router | circuit-board / chip nodes | no | `05-router-circuit-blue-chip.png` |
| Monitor | sliding oscilloscope (animated) | no | `17-oscilloscope.png` |
| Chat | wet blue-grey slate stone ✅ | yes | `14-stone-slab.png` |
| Horizons | butterfly nebula (gold/blue-white)* ✅ | yes | `13-nebula-wallpaper.png` |
| Archives | vintage film strip | yes | `16-film-strip.png` |
| Settings | brushed-steel vault door | yes | `15-vault-door.png` |

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

- **Exact tile card style:** operator says the current tiles are "the wrong
  style" but the crisp target isn't fully pinned — resolve against the baseline
  render before restyling cards.
- **Logo font asset:** matching the exact typeface likely needs a bundled
  `.ttf`; `FontFamily.Monospace` is the closest built-in stand-in until a font
  file is added.
