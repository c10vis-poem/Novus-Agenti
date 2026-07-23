# HomeGrid.kt — Full Rebuild Spec (from-scratch)

**Source of truth**: `wiki/HomeGridSim.tsx` (51,863 bytes) — the React/TypeScript
simulator the operator perfected in Google AI Studio. The next session MUST
write HomeGrid.kt from zero using this file as the visual reference. Do NOT
patch or layer on top of the existing `HomeGrid.kt` — delete it and rewrite.

**Why**: The current `HomeGrid.kt` is an intermediate AI Studio export that was
~75% of the way to the finished visual. The operator refined the simulator
further after that export. The Kotlin was never updated to match. On-device it
renders "the same slop as previous builds" — tile names truncated, crystal
small/dim, plasma cords thin, icons wrong.

---

## Visual Differences: Simulator (CORRECT) vs Current Kotlin (WRONG)

### 1. TILE COLORS — two tiles have WRONG colors in Kotlin

| Tile | Simulator (correct) | Current Kotlin (wrong) | Fix |
|------|---------------------|----------------------|-----|
| CHAT | `#4FE9A6` (mint green) | `HorizonsColors.TileChat = #4FE7EC` (teal) | Change TileChat in HorizonsTheme.kt |
| HORIZONS | `#40C4FF` (blue) | `HorizonsColors.TileHorizons = #2DD4D9` (teal) | Change TileHorizons in HorizonsTheme.kt |
| MONITOR | `#2DD4D9` (teal) | Correct | — |
| SETTINGS | `#FF5577` (pink/red) | Correct | — |
| TERMINAL | `#00FF41` (matrix green) | Correct | — |
| ARCHIVES | `#E8A838` (amber) | Correct | — |

### 2. STATUS NODE COLORS — two nodes have different colors

| Node | Simulator (correct) | Current Kotlin (wrong) |
|------|---------------------|----------------------|
| ASR | `#00FF41` (matrix green) | `#00E676` (green) — close but different shade |
| LLM | `#40C4FF` (blue) | `#40C4FF` — Correct |
| TTS | `#E8A838` (amber) | `#FFB74D` (orange) — different |
| MLLM | `#AA77FF` (purple) | `#CE93D8` (lighter purple) — different |
| VAG | `#FF5577` (pink) | `#FF4081` (different pink) — different |

### 3. CRYSTAL — completely different implementation

**Simulator (correct)**:
- 3D hexagonal pointed gem with **6 distinct facets** (front-left, front-right,
  right-side, top-cap-left, top-cap-right, back shadow)
- Uses **gradient fills** per facet (not flat transparent fills):
  - Front-left: `#9333EA → #581C87`
  - Front-right: `#A855F7 → #6B21A8`
  - Cap-left: `#C084FC → #7E22CE`
  - Cap-right: `#E9D5FF → #9333EA`
- Has a **3D glowing platform/pedestal base** underneath:
  - Elliptical disc with gradient fill (`#2DD4D9 → #7E22CE → #1E1035`)
  - 3D cylinder wall below disc
  - Rim gradient (`#2DD4D9 → #A855F7 → #2DD4D9`)
  - Inner dashed ring on platform
  - **6 socket nodes** on the platform perimeter (each with 3-layer circles:
    outer glow + core + white center)
- **Sun core glow** inside: radial gradient white→purple, `r=22` relative to
  100-unit viewbox (large), plus a bright white center dot `r=4.5`
- SVG size: `110 * crystalScale(1.20) = 132px`
- **Hub label** has a **background card** with purple border + blur backdrop:
  `bg-[#0A0518]/90 backdrop-blur-md rounded-lg border border-purple-500/30
  shadow-[0_0_12px_rgba(168,85,247,0.35)]`
- Drop shadow on entire crystal: `drop-shadow-[0_0_16px_rgba(168,85,247,0.7)]`

**Current Kotlin (wrong)**:
- Crystal is tiny — `W = minDimension * 0.105f` on a 150dp canvas = ~8dp half-width
- Flat transparent fills (`#8855CC @ 0.24 alpha`) — no gradients per facet
- No platform/pedestal at all
- No socket nodes
- Sun aura is there but weak (`alpha 0.13 - 0.024*layer`)
- Hub label has no background card, just floating text

### 4. PLASMA CORDS — different structure

**Simulator (correct)**:
- Plasma cords go from **tile corners to crystal platform socket nodes** (not to
  hub center)
- 3-layer rendering per cord:
  1. Outer neon glow (`strokeWidth: max(3.2, thickness*2)`, `opacity: 0.25`)
  2. Main plasma core tube (`strokeWidth: max(1.5, thickness)`, `opacity: 0.9`)
  3. Inner sharp laser core line (`strokeWidth: 0.8`, `color: #FFFFFF`, `opacity: 0.95`)
- Uses **quadratic** bezier (Q), not cubic
- Tile attachment point has a soft glow circle (`r=8`, `opacity: 0.4`, blurred)
- Endpoint dots: tile end = 2.5r colored + 1.2r white; node end = 3r colored + 1.5r white
- `plasmaTubeThickness: 3.5` (configurable)
- NO beads along the path (current Kotlin has beads — remove them)

**Current Kotlin (wrong)**:
- Cords go to hub center, not socket nodes
- 4-layer rendering with different alphas (0.05, 0.10, 0.18, 0.35) — too dim
- Uses cubic bezier
- Has "beads" along path — NOT in the final simulator
- Much thinner appearance overall

### 5. CHAT ICON — different design

**Simulator**: Speech bubble with **2 horizontal lines** inside (not dots):
```
rect(4,5 28x20 rx5) stroke #4FE9A6
path tail: M10,25 L8,31 L16,25
line(10,11 → 24,11) — first text line
line(10,16 → 19,16) — second text line (shorter)
```

**Current Kotlin**: Speech bubble with **3 dots** (wrong)

### 6. HORIZONS ICON — different details

**Simulator**: Has violet arch dome (`#BB88FF`), amber sun with **arc + rays
pointing upward + core circle**, and blue horizon line. Sun has an iris arc
(`path d="M 12,23 A 6,6..."`) that current Kotlin doesn't have.

### 7. TERMINAL ICON — different prompt cursor

**Simulator**: Uses a **chevron prompt** (`> `) with polyline + underscore line:
```
polyline points="8,18 13,21 8,24" (chevron >)
line 15,24 → 22,24 (underscore cursor)
```

**Current Kotlin**: Uses `>_` as two separate drawLine calls (close but subtly different)

### 8. SETTINGS ICON — different implementation

**Simulator**: 8 outer notches/ticks (not 12), dashed ring, inner filled circle
with **filled polygon lightning bolt** (not stroked path).

**Current Kotlin**: 12 rays with dashed arc segments, stroked bolt path (not filled)

### 9. ARCHIVES ICON — different layout

**Simulator**: Two overlapping documents — back document has 3 text lines,
front overlapping document has large bold **"A"** letter badge.

**Current Kotlin**: Two stacked documents with just lines on the front — no "A" badge.

### 10. TILE CARD — prompt box styling

**Simulator**: The `$_command + ⚙` row at the bottom is inside a **bordered box**
with colored tint background:
```
border: ${tile.color}33
background: ${tile.color}10
rounded, padded
```

**Current Kotlin**: Just a plain Row with text, no border/background box.

### 11. TELEMETRY RINGS — different style

**Simulator**: Uses **dashed** strokes with `strokeDasharray`:
- Ring 1: `r=65`, `strokeWidth=0.6`, `dasharray="3 3"`, `opacity=0.18`
- Ring 2: `r=105`, `strokeWidth=0.5`, `dasharray="6 4"`, `opacity=0.12`
- Ring 3: `r=145`, `strokeWidth=0.5`, no dash, `opacity=0.08`

Extra clusters also use dashed strokes.

**Current Kotlin**: Solid strokes, `opacity=0.03`, much less visible.

### 12. TILE NAME TRUNCATION — layout issue

The 114dp tile width with 13sp monospace + 2sp letter spacing causes "MONITOR",
"HORIZONS", "ARCHIVES", "SETTINGS", "TERMINAL" to truncate on device. The
simulator avoids this by rendering at `0.82x` scale factor with CSS text that
naturally wraps. Options:
- Reduce `letterSpacing` from `2.sp` to `1.sp` or `0.5.sp`
- Reduce `fontSize` from `13.sp` to `11.sp` or `12.sp`
- Or use `autoSizeText` approach
- The simulator uses `titleFontSizeSp: 14` but renders at `14 * 0.75 = 10.5px`
  effective — so the Kotlin equivalent should be approximately **11sp**

---

## Build Instructions for Next Session

1. **Delete** `horizons/src/main/java/com/horizons/ui/HomeGrid.kt` entirely
2. **Update** `HorizonsTheme.kt` colors per the table above (Chat, Horizons tile
   colors; all 5 status node colors)
3. **Write** `HomeGrid.kt` from scratch, using `wiki/HomeGridSim.tsx` as the
   pixel-perfect visual reference, translating each SVG/CSS construct into
   Compose Canvas equivalents
4. **Preserve** the non-visual wiring: `onTileClick(Panel)`, `Panel` enum
   references, `HorizonsApplication` backend status collection, goat easter egg
5. **Test** by building APK via CI and having user screenshot on device

## Files to Read

- `wiki/HomeGridSim.tsx` — the ONLY visual authority (already in repo)
- `wiki/HOME-REDESIGN-SPEC.md` — the broader visual spec (if it exists)
- `horizons/src/main/java/com/horizons/ui/theme/HorizonsTheme.kt` — color defs
- `horizons/src/main/java/com/horizons/HorizonsApplication.kt` — for wiring
- `horizons/src/main/java/com/horizons/Panel.kt` or wherever Panel enum lives
