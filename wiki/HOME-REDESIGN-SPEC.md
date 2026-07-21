# HOME-REDESIGN-SPEC — Visual Definition Document

> **What this is.** The canonical, operator-approved visual specification for
> the Novus Agenti **home screen** (`HomeGrid.kt`). Each section has reference
> screenshots with exact written descriptions of what's right, what's wrong,
> and what the finished version must look like. When a home-screen visual task
> starts, READ THIS FIRST — every detail here is operator-verified.
>
> **Verification rule.** Every change MUST end with a real on-device screenshot
> compared against the references before it's "done." The cloud build container
> has **no Android SDK** — the operator is the on-device visual check.

---

## 1 · Header / Logo

**Reference images:**
- `session20-current-build.png` — current build (session 20), what we have now
- `19-logo-font.webp` — the TARGET font (chunky blocky terminal face, `cat << 'EOF'` header, wet stone background)
- Prior build slogan closeup — dual-font `*Pioneer_Tech,` + `(Next-Gen Certified)` + `EOF` / `v1.0`

### Size

The logo text (`MØ[)u14R_11(`) should be approximately **1/3 larger** than it
is in the current build. The slogan line underneath (`*Pioneer_Tech,
(Next-Gen Certified)`) is **good size — don't change it.** The app label on
the bottom right (`HORIZONS // V4`) is **good size — don't change it.** The
faint purple divider line barely visible at the bottom of the header area is
**good** as long as it has **more contrast** once the background haze is
cleaned up.

### Location

- The faint purple divider line **stays where it is.**
- The app label (`HORIZONS // V4`) and slogan can both **drop down just a
  tiny bit.**
- The logo itself needs to **increase in size** as described above.
- **Logo and slogan both need to be centered on screen** (currently
  left-aligned).

### Color

- **Material teal** for the logo and slogan text — good, don't change.
- The faint purple (almost blacked-out purple) for the divider underline —
  **good, don't change.**

### Font

**The font is WRONG in the current build.** The logo font must match the
chunky blocky terminal typeface shown in `19-logo-font.webp` — the one from
the reference render with the `cat << 'EOF'` header and the wet stone
background. That is the target font.

The slogan is **dual font:**
- `*Pioneer_Tech,` — in monospace
- `(Next-Gen Certified)` — in the **same font as the logo** (the chunky
  blocky terminal face, NOT monospace)

### Text

Look at the reference font image — the `11(` portion is **smaller sized
font** and there is **no space between the underscore and `11(`**. That must
happen in our version: `MØ[)u14R_11(` with the `_11(` run together, no gap,
and `11(` rendered slightly smaller.

---

## 2 · Background Color and Style

**Reference images:**
- Current build (session 20) — **trash, not acceptable**
- Prior build (session 20, pic 2 — the one with the large crystal, vivid
  status nodes, plasma cords) — **this is the TARGET background quality**
- V3 Horizons build (the one with the grid overlay) — **copy the STARS ONLY
  from this image, NOT the grid**

### What's wrong (current build)

The background has a haze/wash to it that flattens everything. It looks
washed out and lifeless compared to the prior builds.

### What's right (prior build, pic 2)

- The **center hue of the white sun** radiating from behind the crystal —
  that exact warmth and glow must be replicated.
- The **purple contrast of the crystal** — the violet stands out against the
  dark background with real depth. That same purple/dark contrast must be
  identical in the finished version.
- The **telemetry circles** (faint concentric rings around the hub area) —
  keep these, and add **2-3 more layered telemetry circles in varying sizes**,
  **not all in the same spot** — spread them around at different positions.

### Stars (from V3 build, pic 3)

Copy the **star field only** from the V3 build — the mild, scattered pinpoint
stars across the dark background. **Do NOT copy the grid overlay** from that
image, just the stars. Add maybe a **couple more stars** than what's shown
there, but **not too many more** — keep it subtle, not a galaxy.

---

## 3 · Aspect Ratio / Overall Proportions

**Reference images:**
- Current build (session 20, pic 1) — what we have
- Prior build (session 20, pic 2 — vivid status nodes, large crystal) — the
  **TARGET aspect ratio**

### Definition

"Aspect ratio" here means the **ratio in size of every element in conjunction
with everything else on the screen** — how big the header is relative to the
tiles, how big the tiles are relative to the hub, how much space between the
wheel and the bottom bar, etc.

### Target

**Picture 2 (prior build) is the target** for overall proportions. Match it
for:
- **Header size** (logo + slogan area)
- **Tile size** and how they're **spread around the screen**
- **Spacing between the clock circle and the configuration nodes**
- **Size of the configuration nodes** (ASR/LLM/TTS/MLLM/VAG dots)
- **Chat bar size and style**

### What changes from picture 2

1. **Tile orientation** — the clock-face positions change per the tile
   sections below (don't copy pic 2's tile arrangement verbatim).
2. **Center hub size** — needs to be **a little smaller than pic 2** but
   **way bigger than pic 1** (current build). The current build's hub is
   tiny; pic 2's is slightly too large. Split the difference, leaning
   toward pic 2.
3. **Chat bar goes ABOVE the configuration nodes** — in pic 2 the chat bar
   is below the config nodes. Swap that: chat bar on top, config nodes on
   the bottom. Everything else about pic 2's chat bar and config nodes
   (size, style, color, design) stays identical.
4. **System bar padding** — keep the top padding (status bar) and bottom
   padding (gesture bar) that the current build has. Don't lose that.

---

## 4 · Tiles — General Style

**Reference image:** Prior build closeup (HORIZONS + ARTIFACTS tiles, session
20) — the best example of what ALL tiles should look like.

### Card style (applies to every tile)

- **Dark black interior** on the card body — near-black, not translucent,
  not hazy. The tile background should be a deep solid black that contrasts
  sharply against the colored border/glow.
- **Colored hue on the outside** — each tile's accent color forms a subtle
  border/outline around the card edge.
- **Backlight glow** — the icon's backlight radiates upward/outward from
  behind the icon, which protrudes above the card's top edge. The glow
  should be vibrant and directional (emanating from the icon position), not
  a flat wash.
- **No haze.** The current build has a washed-out haze over everything that
  flattens the contrast. Remove it. The dark-to-bright contrast between
  the black card interior and the colored accents/glow is critical.

### Label sizing and formatting (applies to every tile)

- **Title** (e.g. `HORIZONS`, `ARCHIVES`) — the font needs to be
  **increased in size** from what's in the current build. Bold,
  letter-spaced, monospace.
- **Subtitle lines** — slug + descriptors (e.g. `/home · System view`),
  rendered in the tile's accent color at reduced opacity.
- **Prompt line** — should be **brighter** than currently rendered. Must
  have an **underscore after the dollar sign**: `$_` not `$`. The gear
  icon (`⚙`) sits on the far right of the prompt line.

### Icon sizing

Icons should be **properly sized** — matching the proportions shown in the
prior build reference (HORIZONS and ARCHIVES closeup). They protrude above
the card top edge with the backlit glow behind them. The current build's
icons are **way too small**.

---

## 4a · HORIZONS tile (10:00 position, blue)

**Reference images:**
- Prior build closeup pic 1 — tile style/size reference (the arch-eye icon
  with rays, `HORIZONS` label, `Home · System view`, `$ home`)
- Prior build pic 2 (small thumbnail) — icon COLOR reference (amber sun,
  blue horizon plane, pinkish-purple atmosphere arch)

### Icon

The icon matches the **style and size of pic 1** (the arch/eye shape with
radiating rays above, half-circle horizon line below, dot in the center).
The **color scheme matches pic 2:**
- **Sun:** amber — can go **darker amber** than pic 2 shows, that's fine
- **Atmosphere arch** (the pinkish-purple curved line above): stays the
  same shade as pic 2
- **Horizon plane** (the blue straight line at the bottom): stays the same
  shade as pic 2, but the line in pic 1 is **a little too thin** — fatten
  it up slightly

### Composition (what the tile reads, top to bottom)

```
    [ICON protruding above card edge]


        H O R I Z O N S

       /home  ·  System
              view

    ─────────────────────────
    $_about                ⚙
```

Match the reference screenshot's spacing exactly: generous vertical
space between the title and subtitle, subtitle and divider, divider
and prompt line. Title is centered, subtitle is centered, prompt line
is left-aligned with gear right-aligned on the same baseline.

### Color

Blue accent (`TileHorizons` — `#40C4FF`). Card interior: near-black.

---

## 4b · ARCHIVES tile (8:00 position, amber)

**Reference image:** Prior build closeup pic 1 — the amber stacked-documents
icon. **Color and icon are perfect as shown.** Only the label font size
needs to increase.

### Icon

The stacked-documents / clipboard icon as shown in the reference — amber
outlined, two overlapping rectangles with inner lines. **Perfect as-is** in
style, size, and color. Do not change.

### Composition (what the tile reads, top to bottom)

```
         [ICON protruding above]

         ARCHIVES

      /logs  ·  Files
                ·  store
     ─────────────────────
     $_ls ./*.tar       ⚙
```

### Color

Amber accent (`TileArtifacts` — `#E8A838`). Card interior: near-black.

---

## 4c · TERMINAL tile (6:00 position, matrix green)

**Reference image:** Prior build closeup — terminal window icon with red/amber/
green dots, `>_` prompt, green border. **Icon and size are perfect as shown.**

### Icon

The terminal window icon exactly as shown in the reference: rounded rect
with a green border, three colored dots (red, amber, green) in the title
bar, divider line under the title bar, `>_` prompt inside the window body.
**Perfect as-is** — do not change the icon style or size. Same aspect ratio
as all other tile icons.

### Style changes from reference

- **Backlighting** needs to be **brighter** than shown in the reference.
- **Prompt box** (the bottom `$_` line area) needs to be **better defined
  and brighter** — more contrast against the card background.
- **Card background** is **deeper near-black** (`TerminalCardBg` `#060A07`)
  — darker than all other tiles. This is intentional: Terminal green is
  brighter matrix-green on a deeper black, distinct from Chat's softer
  green.

### Composition

```
    [TERMINAL WINDOW ICON protruding above]


          T E R M I N A L

        /shell  ·  commands

    ─────────────────────────
    $_bash                 ⚙
```

### Color

Matrix green accent (`TileTerminal` — `#00FF41`). Card interior: deeper
near-black (`#060A07`).

---

## 4d · MONITOR tile (12:00 position, material teal)

**Reference image:** Monitor icon closeup — display/screen glyph (rounded
rect, inner lines, V-stand/tail, badge in top-right). **Swap `AI` for `PC`
on the badge.**

### Icon

The display/screen glyph as shown in the reference: rounded rectangle
with 2-3 inner horizontal lines, V-shaped stand/tail underneath, circular
badge in the top-right corner reading **`PC`** (NOT `AI`). Same aspect
ratio / size proportions as the Terminal icon and all other tile icons.

### Color

**Material teal** (`TileMonitor` — `#2DD4D9`) — matches the logo teal.
Card interior: near-black.

### Composition

```
    [DISPLAY/PC ICON protruding above]


          M O N I T O R

       /cognito  ·  library

    ─────────────────────────
    $_browser              ⚙
```

Title font bigger than current build, same as all tiles.

---

## 4e · CHAT tile (2:00 position, soft green)

*(Coming — operator providing next)*

---

## 4f · SETTINGS tile (4:00 position, pink/crimson)

*(Coming — operator providing next)*

---

## 5 · Center Hub — ROUTER

*(Coming)*

---

## 6 · Connector Cords

*(Coming)*

---

## 7 · Chat Bar / Input

*(Coming)*

---

## 8 · Configuration Nodes (System Status)

*(Coming)*

---

## 9 · Easter Eggs and Guardians

![goat easter egg](home-redesign-img/24-goat-easter-egg.webp)
![guardian chonk](home-redesign-img/23-guardian-chonk.png)

- **GOAT — crash-log easter egg.** On runtime crash/fail the goat pops up
  (`// GOAT_SAYS_NO`) with a synthesized bleat; 7 banner taps →
  `// GOAT_UNLOCKED`. Already wired (`HomeGrid.kt` `showGoat` /
  `playGoatBleat`).
- **CHONK — screen-timeout guardian.** Idle/screen-timeout screensaver loads
  the **chonky orange cat** from device storage. Partly wired
  (`Screensaver.kt`).

---

## 10 · Panel Backgrounds (separate track — mostly shipped)

Each of the 8 panels has a procedural background; **4 are also
uploadable-wallpaper capable** (image fully replaces procedural, tiles go
semi-transparent).

| Panel | Procedural bg | Wallpaper? | Reference |
|---|---|---|---|
| Home | astral (deep black + stars + telemetry) | no | `01-target-full-home.webp` |
| Terminal | Matrix rain (`fakesteak` fork) | no | `18-matrix-rain.png` |
| Router | circuit-board / chip nodes | no | `05-router-circuit-blue-chip.jpg` |
| Monitor | sliding oscilloscope (animated) | no | `17-oscilloscope.webp` |
| Chat | wet blue-grey slate stone | yes | `14-stone-slab.webp` |
| Horizons | butterfly nebula (gold/blue-white) | yes | `13-nebula-wallpaper.webp` |
| Archives | vintage film strip | yes | `16-film-strip.jpg` |
| Settings | brushed-steel vault door | yes | `15-vault-door.jpg` |
