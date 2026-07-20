# Horizons Workbench — Tile → Hub Architecture (EXPLICIT DEFINITION)

> **Status: proposed explicit definition — PENDING OPERATOR CONFIRMATION.**
> The three workbench docs in this folder define each *room* and the
> "user-as-loader" philosophy, but the **exact function of each tile and how
> each wires into the center-hub Router was never written down.** This file is
> that missing definition. The old code
> (`RuntimeDefStore`/`RouterPane`/`MonitorPane`/`CliffordService`) is an *assumed*
> implementation that already contradicts this (see `EXECUTIONS.md`) — **build
> to THIS, do not reverse-engineer the target from the old code.**
>
> **Design stance: describe capabilities, not hard caps.** The tiles are rooms
> you *can* use, in any order or combination — not a mandatory pipeline. There
> is exactly one hard rule (the parameter check, below). Everything else is
> open-ended and multi-path. Do not encode "you always need X then Y."

## The one law

**Daemons stay dumb; the user is the loader.** The app boots **empty and
stable**. **Nothing RUNS until a config satisfies the parameter check and gets
engaged in the Router.** "The user is the loader" includes the user's *own
on-device agent acting on their behalf* (voice/agentic) — see **Chat**.

## The center hub + the one hard gate

The **Router is the center hub** — the information highway. **Every tile
connects to it, and there are many paths** to get a runnable config onto it.
There is no required tile sequence. There is exactly **one hard invariant**:

> **A config can only FULLY ENGAGE (run) once it satisfies the parameter check
> — the 4-base "fuse", green-lit — and the Router re-checks it at the instant of
> the flip.** *How* the pieces are assembled and *where* they come from is
> completely open.

**The parameter check — the 4 bases (the fuse):**
1. **Engine** — the binary/driver (`ort_engine`, `geniex`, a custom script, a cloud endpoint).
2. **Assets** — required companion files (weights, `.so`/QNN libs, plugins). May be **zero**.
3. **Amperage** — hardware/memory fit: device arch vs. binary arch, free RAM vs. footprint. The OOM guard.
4. **Communication** — args/syntax (where `{model}`/`{port}` inject) + health endpoint (the handshake).

Anything that satisfies these four — sourced **any** way — can run. No caps on
how you get there.

## Many paths (examples, NOT a required order)

- **Archives → Router.** A saved/compiled harness uploads straight to the Router. No Terminal needed.
- **Settings → Router.** A stored model + key pushes straight in.
- **Monitor(browser) → Settings → Router.** Download what you need in the browser, stash it, plate it.
- **Terminal → Router.** Forge a runtime, or ask the on-device CLI agent (OpenWiki CLI) what's needed to cover the 4 bases; assemble a runtime folder; export it.
- **Chat (voice) → everything.** The agent does any/all of the above for you — human-in-the-loop or autonomous.

## Each tile — function + how it feeds the hub

| Tile | Pos | Function | How it can feed the Router (open-ended) |
|---|---|---|---|
| **Terminal** — Mod Garage | 6:00 | **DEFINE / FORGE.** Author a fuse; ad-hoc bash; on-device CLI agent can auto-derive what's needed to cover the 4 bases and assemble a runtime folder. Never launches a daemon itself. | Export a definition/folder → Monitor and/or Router. Save harnesses → Archives. |
| **Settings** — Platform Armory / Vault | 4:30 | **STORE.** Vault for weights (`.gguf`), keys/tokens (TTL), SDKs/binaries (`.so`). Device-folder import. | Push/pull assets + credentials straight to the Router or into any config. |
| **Monitor** — Checkpoint / Console + Browser | 12:00 | **VALIDATE + ACQUIRE + ADVISE.** Runs the parameter check (green-light + amperage), no side effects. Reviews the vault + archives and tells you what's missing and where to get it. Chromium browser to download anything. | The check is the gate to *full engage*, however the config was built. Browser-sourced assets → Settings → Router. |
| **Router** — Fuse Box / Center Hub | Center | **ENGAGE.** Every tile feeds it. A config that satisfies the parameter check is plated and engaged; **re-checks at the flip**; **the only tile that starts a runtime**; honors the config's own binary/args/port/health. States: RUNNING / SLEEPING / ARCHIVED. User-activated fuse, **not** a hardened gatekeeper. | — (this is the hub) |
| **Chat** — Agentic Neural-Mesh Assistant | 2:00 | **DO.** Not just talk to a running model — the on-device agent that can, by voice or text, drive every operation here: web search/fetch, download from GitHub/HF, open the QAI workbench + BYOM-quantize, have the Terminal agent push results to Settings, define runtimes, plate and flip the Router. Autonomously or human-in-the-loop. (Grows into the full neural-mesh assistant from the blueprint.) | Can orchestrate any path above end to end. |
| **Archives** — Artifact Vault | 7:30 | **STORE / RECALL.** File manager (`ArchiveStore`) for saved fuses/harnesses/scripts, nested folders. | A stashed harness → straight to the Router. |
| **Horizons** — Front Desk | 10:00 | **INFO.** About/credits/legal/version. | None. |

## Separate layer: STT/TTS media (NOT part of the fuse pipeline)

The speech layer — STT (Silero/Moonshine) + TTS (Kokoro/Sherpa) — is its **own
deal**, separate from the model-runtime fuse box. It's consistent/persistent and
not defined per-run through the parameter check (unless manually changed). Do
**not** route it through the Router fuse or tie its lifecycle to the model
daemon. Its own boot/init behavior is a separate question from the "nothing runs
until a fuse is flipped" rule, which is about **model runtimes**.

## CliffordService — failover supervisor (no hard crashes), NOT a starter

Does **nothing at boot**; never auto-launches. Once the Router engages a
runtime, Clifford supervises it. Its real job: **turn an on-device failure into
a graceful reboot instead of a hard crash.** On failure of the engaged runtime
(daemon dead, OOM, health flatlines), it preserves the session (to disk) and
**fails over to a tiny on-device model** so the assistant keeps answering —
offline, no credits, no network dependency. When the primary is healthy again
(or the user re-engages), it hands back. Root-free.

The failover model is **pluggable, NOT hardcoded** — it's just a designated
"failover" fuse pointing at whatever tiny model file the user plugged in. Swap
the file → new failover, no code change; a better sub-1B model drops in as an
asset. It stays **cold** until a failure triggers it (keeps boot empty + memory
low), and runs on whatever its own config says — including CPU as the
last-resort floor when the NPU itself is wedged. (Cloud connectors are NOT the
failover — no internet / no credits = no floor; they can be an *optional* config
the user runs like any other, never the safety net.)

## Invariants (the build must enforce all)

1. Boots empty; no **model runtime** runs until a config satisfies the parameter
   check AND is engaged in the Router (by the user or the user's agent).
2. Every tile can feed the Router; **no mandatory sequence, no "you always need
   X," no hard caps** on how a config is assembled or sourced.
3. The parameter check (4 bases, green-lit) is the **one hard gate** to full
   engage; the Router re-checks at the flip.
4. The Router is the only thing that starts a runtime, and it honors the config's
   own binary/args/port/health — never a hardcoded engine.
5. No hardened gatekeeper — user-activated fuse.
6. A fuse/config is portable data; Monitor reads/reviews it, never owns it.
7. The STT/TTS media layer is separate from the model-runtime pipeline.
8. **Runtime-agnostic — nothing model/engine-specific is hardcoded** in the app
   or the daemon. "NPU-only," the chosen engine, CPU-vs-NPU, the model file, the
   failover model — all of it is per-config *parameters*, never app/daemon
   constants. A better model or runtime drops in as a plugged asset/config,
   never a recompile. (`DaemonLauncher` hardcoding `ort_engine`/port 8080, and
   `resolveNpuModelPath()` hardcoding Qwen filenames, are violations — see the
   build dock.)
