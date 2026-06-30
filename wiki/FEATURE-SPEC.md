# FEATURE-SPEC.md — Horizons UI Tile Specification

> Canonical feature spec for the Horizons app UI. Referenced by CLAUDE.md.
> Source: user-provided mockups + verbal spec (session 8 closeout).

---

## Layout

Six tiles arranged around a central CORE_HUB crystal/hexagon, connected by colored energy lines. The crystal graphic breaks the top plane of the tile grid. Tiles are cards (not circles) with the icon graphic protruding above each card's border.

```
    [HORIZONS]     [MONITOR]      [CHAT]
         \            |            /
          \           |           /
           ====  CORE_HUB  ====        ← ROUTER (tap to open)
          /           |           \
         /            |            \
    [ARTIFACTS]    [TERMINAL]    [SETTINGS]
```

**Top bar:** App title `MO)u14R_11(` in one unbroken line, subtitle `*PIONEER_TECH · (NEXT-GEN CERTIFIED) · HORIZONS // V{n}` below it.

**Bottom status bar:** Five status indicator dots with labels:
- **ASR** (green) — speech recognition engine status
- **LLM** (blue) — language model / NPU daemon status
- **TTS** (orange) — text-to-speech engine status
- **MLLM** (purple) — multimodal LLM status
- **VAG** (pink) — vision-agent / screen-share status

**Input bar:** Below status bar — `tap_or_hold ask //` with send arrow.
- **Tap:** opens quick-chat text input (stays compact, ~1 line)
- **Hold:** expands to a floating chat panel taking bottom ~40% of screen. Full chat interface with scroll thread. Conversations from this floating panel are saved in the Chat tile's side-panel history.
- Send arrow on right edge.

---

## Tile Labels

Each tile has a two-line label: **NAME** on top, **/ slug** below. These are canonical:

| Tile | Label | Slug |
|---|---|---|
| HORIZONS | `HORIZONS` | `/ home` |
| MONITOR | `MONITOR` | `/ cognito` |
| CHAT | `CHAT` | `/ interface` |
| ROUTER | `ROUTER` | `/ route` |
| ARTIFACTS | `ARTIFACTS` | `/ logs_skills` |
| TERMINAL | `TERMINAL` | `/ shell` |
| SETTINGS | `SETTINGS` | `/ config` |

---

## Tile 1: HORIZONS — `/home` (top-left)

**Color:** Teal/cyan, ice-blue sphere icon (antenna/horizon symbol)

**Purpose:** System identity and device overview — the "about" screen.

**Contents:**
- Model number / device identifier
- Build number + version (`build {GIT_SHA} · v{VERSION}`)
- Open-source credits link
- README.md / whitepaper viewer (scrollable)
- System update check / condition status
- Link to GitHub repo

---

## Tile 2: MONITOR / COGNITO — `/cognito` (top-center)

**Color:** Teal/green, compass/sun icon

**Purpose:** Model library, prompt management, and credential vault. The "brain" of the system — see what's loaded, manage scripts, store keys.

**Contents:**
- **Model library** — visual list of every model loaded on device (.bin, .onnx, .gguf). Shows file name, size, format, load status (active/idle/error).
- **Prompt cache / scripts** — saved system prompts, load prompts, agent scripts. Tap to load into chat or terminal.
- **Script library** — reusable prompt templates and automation scripts.
- **API key / token vault** — encrypted storage for API keys (SambaNova, OpenRouter, HuggingFace, etc.) and any coin/credit balances. Managed via `AppStateStore` encrypted slots.
- **Console interface** — small terminal/console at the bottom of the tile for direct model loading commands or quick access operations (e.g., `load qwen3_5_9b_unified.bin`, `status`, `reload`).

---

## Tile 3: CHAT — `/interface` (top-right)

**Color:** Teal/green, chat bubble icon

**Purpose:** Primary AI conversation interface. Two modes: standard chat and live (screen-share) chat.

### Standard Chat Mode
- Frontier-style chat UI (similar to Claude/ChatGPT/Gemini web apps)
- **Top:** Mode toggle (standard / live / think)
- **Center:** Scrolling chat thread with message blocks (user on bottom, assistant on top)
- **Bottom input bar:**
  - `+` button (left) — upload files, images, documents
  - Text input field
  - Speaker/mic icon — tap for voice input
  - Think tab — toggle extended thinking / reasoning mode
  - Send button

### Live Chat Mode (Screen Vision)
- No dedicated interface opens inside the app
- Overlays onto whatever screen is active (uses MediaProjection + accessibility)
- Screen vision: assistant can see and analyze what's on screen in real-time
- Audio loop: continuous mic capture for voice conversation
- All conversation from live mode is **recorded into the Chat tile's history** — transcripts, referenced links, assistant responses are all accessible later in the app

### Chat History (Side Panel)
- Slide-out side panel within the Chat tile
- Chronological list of past conversations
- Tap any conversation to view full visual transcript
- Copy/paste any message, link, or code block
- Search across history

### Condensed Chat (Bottom Overlay)
- When Chat tile is selected from the home grid, a condensed chat interface pops up at the bottom ~1/3 of the screen
- Quick-access for short queries without leaving the tile grid view
- Expands to full-screen chat on pull-up

---

## Tile 4: ROUTER — `CORE_HUB` / `/route` (center)

**Color:** Purple, hexagonal crystal icon

**Purpose:** Live routing dashboard. The nerve center — see what's connected, what's active, where inference is flowing. This is where you plug everything in.

**Contents:**
- **Live routing status** — visual diagram showing active inference path:
  - NPU daemon (ort_engine) status + PID
  - Cloud API connections (SambaNova, OpenRouter, etc.) — which are active
  - Local model file currently loaded
- **Model selector** — switch between on-device model, cloud API, or CLI backend
  - On-device: select from models detected in `/files/models/` or `/Download/`
  - Cloud API: pick provider, enter endpoint, test connection
  - CLI: Termux-based runtime path
- **Connection manager** — add/remove/test API endpoints
- **Routing rules** — configure when to use local vs cloud (e.g., "use cloud when NPU unavailable", "always local for voice", "cloud for long-context")
- **Performance metrics** — tokens/sec, latency, memory usage from active backend

---

## Tile 5: ARTIFACTS — `/logs_skills` (bottom-left)

**Color:** Orange/amber, document/clipboard icon

**Purpose:** Archive and storage for all generated content.

**Contents:**
- **Past chats** — archived conversation transcripts (moved from Chat history or auto-archived)
- **Scripts** — saved agent scripts, automation sequences, prompt chains
- **Created artifacts** — any files, images, code, documents generated by the assistant during conversations
- **Logs** — agent execution logs, tool call history, error logs
- **Export** — share/export any artifact via Android share sheet

---

## Tile 6: TERMINAL — `/shell` (bottom-center)

**Color:** Green, terminal/prompt icon

**Purpose:** Direct shell access with Matrix aesthetic.

**Layout:**
- **Top 40%:** Terminal display — solid black background with Matrix-style green waterfall text animation behind the active terminal. Monospace font (`#00FF41` green on black).
- **Bottom 60%:** Keyboard + controls:
  - Standard keyboard
  - Shortcut key row (ctrl, tab, esc, arrows, pipe, etc.)
  - Prompt tile window — expandable grid of preloaded command prompts/shortcuts. Tap to execute. Scroll through loaded prompts.

**Integrations:**
- Termux command execution (via `com.termux.permission.RUN_COMMAND`)
- Tasker task triggers
- MacroDroid macro links
- Uploads library — scrollable list of preloaded prompts/scripts, tap to insert into terminal

---

## Tile 7: SETTINGS — `/config` (bottom-right)

**Color:** Pink/red, gear/sun icon with lightning

**Purpose:** App configuration and personalization.

**Contents:**
- **Voice options:**
  - TTS voice selector (28 Kokoro voices)
  - Speed slider
  - Tone/pitch adjustment
  - Preview/test button
- **Themes:**
  - Color scheme picker (default dark, Matrix green, custom)
  - Font selector (monospace variants)
  - Animation intensity
- **Verbosity:** Control assistant response length/detail level
- **Memory:** View/edit persistent memory the assistant retains across conversations
- **Failure logs:** Crash reports, daemon restart history, inference errors
- **Script access:** Quick links to edit/view loaded scripts
- **Permissions:** Status of all Android permissions (accessibility, notification access, TTS, assistant, etc.)
- **System registrations:** Toggle/status for:
  - System TTS engine (Kokoro)
  - System STT engine (Qwen3.5-9B)
  - Default assistant (VoiceInteractionService)
  - Accessibility service
  - Notification listener

---

## System Status Bar Indicators

| Indicator | Color | What it tracks |
|---|---|---|
| **ASR** | Green | `HorizonsRecognitionService` — system STT loaded and responsive |
| **LLM** | Blue | `ort_engine` daemon — NPU inference runtime PID alive, model loaded |
| **TTS** | Orange | `HorizonsTtsService` / Kokoro — TTS engine initialized, voice loaded |
| **MLLM** | Purple | Multimodal capability — vision encoder loaded, image/screen analysis ready |
| **VAG** | Pink | Vision-Agent / screen-share — `ScreenShareService` FGS active, MediaProjection granted |

Status dot states:
- **Solid bright** = active and ready
- **Dim/dark** = loaded but idle
- **Pulsing** = initializing / warming up
- **Gray/off** = not available / not configured

The status bar doubles as a **highlights tab** — tapping an indicator shows a quick tooltip or expands to show detail about that subsystem's current state.

---

## Navigation Model

- **Home grid** is the default view — all 6 tiles + center crystal visible
- Tapping a tile opens it full-screen with a back gesture/button to return to grid
- The CORE_HUB crystal (Router) is always tappable from any tile via a floating mini-crystal or status bar
- Chat condensed overlay (bottom 1/3) can be summoned from any tile by pulling up from the input bar
- Live chat mode is accessible from Chat tile toggle or long-press on the input bar mic icon

---

## Current Code ↔ Spec Mapping

Panel enum remapped (commit `36f5d54`):
```kotlin
enum class Panel { Horizons, Monitor, Chat, Router, Artifacts, Terminal, Settings }
```

| Spec Tile | Pane Class | Status |
|---|---|---|
| HORIZONS | `HorizonsPane.kt` | Built — system info, credits, condition |
| MONITOR | `MonitorPane.kt` | Built — model library, key vault, console |
| CHAT | `ChatPane.kt` | Built — modes, voice, vision, history TBD |
| ROUTER | `RouterPane.kt` | Built — NPU/TTS/STT status, cloud APIs |
| ARTIFACTS | `ArtifactsPane.kt` | Built — placeholder sections |
| TERMINAL | `TerminalPanel.kt` | Built — needs Matrix waterfall theme |
| SETTINGS | `SettingsPane.kt` | Built — needs voice picker, themes |

---

## Visual Design Spec (from user reference screenshots)

### Header

- Line 1: `MO)u14R_11(` — large, centered, blocky angular stencil-style monospace font (thick strokes, glitch/fragmented aesthetic — NOT standard monospace)
- Line 2: `*Pioneer_Tech, (Next-Gen Certified)` — ALL on one unbroken line, smaller
- Line 3: `HORIZONS // v4` — bottom line
- Thin divider line underneath
- All text in primary teal (`#2DD4D9`)

### CORE_HUB Crystal

- **Shape:** hexagonal (6-sided) crystal, viewed from ~45° angle looking down at it
- **Top facets:** ~30° pitch, NOT sharp wizard-hat point. Broad face on top faces outward, peak/ridge offset toward back
- **Base:** concentric elliptical rings (circles viewed at angle = ellipses) creating a 3D platform — like an orbital ring base
- **Glow:** intense radial purple glow emanating from center, multiple layers
- **Border:** faint circular border around the crystal area
- **Labels:** `// CORE_HUB` above, `ROUTER / route` below

### Conduits (connection lines from tiles to hub)

- NOT solid colored cords/lines
- **Hollow plasma tubes** with radiant glow — translucent tube shape with bright core
- Each conduit in its tile's accent color
- Can incorporate particle/photon effects (small dots traveling along the tubes)

### Tile Cards

- Rectangular cards with rounded corners
- **Icons break out of the top border** — graphic protrudes above the tile edge
- Pronounced glow around edges in tile's accent color
- Inside: icon, tile NAME (bold monospace), subtitle description, divider, `$ command` hint + gear icon
- Background: mostly transparent with radiant glow around edges

### Tile Icons (Canvas-drawn)

| Tile | Icon Description | Color |
|---|---|---|
| HORIZONS | Horizon line (horizontal blue line) + green arc above (sunrise curve) + amber dot (sun). Arc is green, sun dot is amber/gold, line is blue. | teal frame |
| MONITOR | Chat-bubble style icon (from landscape mockup — the one labeled CHAT there) | teal |
| CHAT | Speech bubble with tail (from spacing reference screenshot) | highlight teal |
| ROUTER | Hexagonal crystal / cube (purple, part of the center hub) | purple |
| ARTIFACTS | Stacked documents / clipboard pages (from landscape mockup) | orange/amber |
| TERMINAL | Terminal prompt `>_` with window dots (from landscape mockup) | matrix green |
| SETTINGS | Gear/sun with lightning bolt (from landscape mockup) | pink/red |

### Backgrounds

**Home grid background:**
- Dark base (`#222C34`)
- Stars scattered throughout (small white/teal dots, varying brightness)
- Faint orbital rings/circles around center hub (translucent, glowing)
- Telemetry lines and chart circles — like an astral/star chart map
- The whole UI overlay sits on top of this astral chart

**Tile pane backgrounds (Monitor, Router, Artifacts, Settings):**
- Dark slate/stone texture with rain droplets
- Deep blue-gray stone surface, water beading on surface
- Cracks/veins in the stone visible

**Terminal pane background:**
- Matrix green `^` character waterfall rain on solid black
- Characters fall at varying speeds, varying brightness
- Top 40% is the terminal display, bottom is keyboard

**Chat pane background:**
- Dark teal water-droplet texture (from phone home screen wallpaper)

**Horizons pane background:**
- Astral/space feel carries through from home grid

### Easter Eggs

- **Goat popup:** appears as 404/error state message. Photo of a goat making a face. Random chance popup or triggered by specific error conditions.

### Status Bar Indicators

Updated labels from mockup reference:
- **NPU** (green) — `ready` / `idle` / `offline`
- **STT** (blue) — `loaded` / `idle`
- **TTS** (orange) — `warming` / `ready`
- **CLD** (purple) — `linked` / `offline` (cloud connection)

Note: some mockups show ASR/LLM/TTS/MLLM/VAG, others show NPU/STT/TTS/CLD. Final set TBD — user prefers the 5-indicator version (ASR/LLM/TTS/MLLM/VAG).

### Color Assignments

| Tile | Color | Hex |
|---|---|---|
| HORIZONS | teal/cyan | `#2DD4D9` |
| MONITOR | teal/green | `#2DD4D9` |
| CHAT | highlight teal | `#4FE7EC` |
| ROUTER | purple | `#AA77FF` |
| ARTIFACTS | orange/amber | `#E8A838` |
| TERMINAL | matrix green | `#00FF41` |
| SETTINGS | pink/red | `#FF5577` |
