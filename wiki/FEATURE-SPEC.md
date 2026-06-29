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

| Spec Tile | Current Panel Enum | Current Pane Class | Status |
|---|---|---|---|
| HORIZONS | `Panel.Diagnostics` | `RouterPane.kt` | Name mismatch — needs rename |
| MONITOR | `Panel.Library` | `LibraryPane.kt` | Name mismatch — needs rename + major expansion |
| CHAT | `Panel.Chat` | `ChatPane.kt` | Closest match — needs live mode, history side panel |
| ROUTER | (no dedicated panel) | — | Currently mapped to `Panel.Router` → `ModelsPane.kt` — needs rewrite |
| ARTIFACTS | (no dedicated panel) | — | Not yet built |
| TERMINAL | `Panel.Terminal` | `TerminalPanel.kt` | Exists — needs Matrix theme, prompt tiles |
| SETTINGS | `Panel.Settings` | `SettingsPane.kt` | Exists — needs voice options, themes, registrations |

### Panel Enum Remap Needed
```kotlin
// Current:
enum class Panel { Chat, Router, Library, Diagnostics, Settings, Terminal }

// Target:
enum class Panel { Horizons, Monitor, Chat, Router, Artifacts, Terminal, Settings }
```

---

## Design Direction Notes

**Closest reference:** Screenshot 5 (final mockup with rectangular tile cards, `MO)u14R_11(` banner, status bar at bottom, input bar below). This is the target layout geometry.

**What changes from screenshot 5:**
- CORE_HUB crystal graphic needs redesign — the tilted wizard-hat shape is not final. Target is a clean hexagonal crystal with glow, not tilted/pointed.
- Tile graphics throughout need rework — icons, line colors, glow effects are placeholder. Final art direction TBD.
- Overall graphics polish pass needed across all tiles.

**What carries forward from all mockups:**
- Tile card layout (not circles) with icon breaking the top border
- Energy/connection lines from tiles to center hub
- Dark background (`#222C34`) with colored accents per tile
- Monospace subtitle typography
- Bottom status indicator dots with labels
- Persistent input bar at very bottom

**Color assignments per tile (from mockups):**
- HORIZONS: teal/cyan/ice-blue
- MONITOR: teal/green
- CHAT: teal/green
- ROUTER: purple
- ARTIFACTS: orange/amber
- TERMINAL: green (Matrix green `#00FF41`)
- SETTINGS: pink/red
