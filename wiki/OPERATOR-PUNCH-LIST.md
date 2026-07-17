# Operator Punch List — first real hands-on review (2026-07-17)

> Source: the operator's systematic walkthrough of the session-17 APK on the
> actual Razr Ultra. This is the ground-truth gap list between what the UI
> *shows* and what actually *works*. Work it top to bottom; nothing here is
> speculative — every line is something the operator personally hit.
> Status legend: FIXED (in branch) · ROOT-CAUSED (fix known, not yet built) ·
> OPEN (needs design/build) · ANSWERED (was a question, answered in-line).

## P0 — the backend never activating (FIXED on PR #19 branch)

1. **"Selected a model, still no backend."** Root cause found and it was
   foundational: `CliffordService` runs in the `:clifford` process; its
   `AppStateStore` is a stale per-process snapshot, so the Load selection
   written by the UI process was invisible to the daemon-launcher forever.
   AND the reverse direction was equally broken — `app.activateNpuRuntime()`
   /`activateGenieXRuntime()` called from clifford only mutated clifford's
   own dead copy of the app singleton, so the UI's chat could never learn a
   daemon was ready. Broken since the ORIGINAL ort_engine wiring — the
   daemon→chat handoff has never once worked. FIXED: selection travels via
   a plain cross-process file (`filesDir/active_geniex_model.txt`);
   activation now happens in the UI process itself (runtime watcher polls
   the daemon ports every 5s, both directions); clifford only manages
   processes; CrashReporter reads the GitHub token via a fresh store
   instance per crash.

## P1 — dead/deceptive UI (each needs real wiring or removal)

2. **Router: half the buttons do nothing; other half only copy to
   clipboard.** OPEN. Inventory every control; anything unwired gets wired
   or deleted — no decorative controls.
3. **Settings: same** — fields that can't be edited/pasted into. OPEN.
   Investigate TokenField's edit/save flow on-device (operator could not
   paste into Credentials).
4. **Copy/paste broken app-wide** — no copy affordance on chat reply
   bubbles, no paste into the Termux chat tile, Monitor tabs copy-only.
   PARTIAL FIX: copy button + long-press copy on chat bubbles (both
   ChatPane and LocalHomeScreen). Full clipboard support on remaining
   text surfaces still OPEN.
5. **Zoom works nowhere** (pinch-zoom shipped only for chat bubbles; the
   operator expects it across panels). OPEN: app-wide text-scale.
6. **STT/TTS layer decorative**: STT tab inert; NO TTS tab; voice pitch
   selector missing (only speed); chat mic records but nothing types; PARTIAL
   FIX: DaemonTtsClient wired into the speak path (sendChat + screenAsk),
   auto-speak toggle added to Router TTS section, STT/TTS sections now show
   actual media daemon connectivity status with probe buttons, Preview uses
   daemon TTS. Pitch note added (Kokoro uses fixed pitch per voice). Chat
   mic→input STT path was already wired. Still depends on media_daemon
   actually running on-device (needs moonshine/kokoro model dirs present).
7. **Live mode**: goes straight to single-app share (broken) instead of
   whole-screen capture. OPEN (MediaProjection config).
8. **Home-screen chat bar opens the full Chat tile** instead of a 1/3
   quick-chat overlay. OPEN.
9. **Monitor**: terminal row should expand to ~1/3 screen (currently one
   line); Models/Files/Network/System tabs are read-only copy boxes — no
   manual entry, no paste. PARTIAL FIX: console expanded from 80dp to
   200-360dp with scrollable output. Manual entry in other tabs still OPEN.
10. **Shortcut button does nothing.** FIXED — removed the decorative gear
    icon from tile cards (was non-functional, looked interactive).
11. **"Old tile still there"** on the launcher despite the manifest having
    exactly ONE launcher activity now. Likely a stale pinned/cached
    launcher shortcut from the pre-fix install surviving reinstall —
    verify on-device (long-press → remove), and check it's not the
    TerminalTile quick-settings tile being mistaken for a launcher icon.
    ROOT-CAUSED (probably), needs on-device confirmation.
12. **Cloud model selector tab does nothing** — distinct from Router (#2)
    and Settings (#3). The cloud model selector UI is non-functional; there
    is a spot to enter everything manually but nothing happens when you
    interact with it. OPEN.
13. **GenieX daemon loader stuck on "loading" in Router** — the status
    indicator perpetually shows loading state, never resolves. Likely the
    same cross-process wall as P0 (#1), but needs confirmation that the fix
    in PR #19 covers this specific indicator too. ROOT-CAUSED (probably
    fixed by #1's runtime watcher), needs on-device verification.
14. **No terminal route in Router** — the router panel has no way to reach
    a terminal session. The terminal exists in Monitor but not as a
    routable destination from Router. FIXED — added Quick Nav section to
    Router with Terminal, Monitor, and Settings links.
15. **No directions / vague confusing interface** — the overall navigation
    gives no guidance on what to do or where things are. No onboarding,
    no tooltips, no empty-state instructions. A new user has no idea what
    any panel does or how to get started. PARTIAL FIX: added Quick Start
    guide on the home screen ("Router → load a model · Chat → talk to it ·
    Monitor → system status"). Full onboarding / per-panel guidance still
    OPEN.

## P2 — architecture / UX redesign (operator direction, agreed)

16. **Model + Runtime Library** (replaces Monitor's flat model list AND
    Router's single picker): top level lists the RUNTIMES (GenieX,
    ort_engine, media daemon, cloud APIs, terminal), tapping one opens the
    models compatible with THAT runtime. Loaded models act like a library
    you switch between — saved, persistent, selectable — not a directory
    listing you re-discover each time. OPEN — this is the real fix for
    "gnarly flat list" + "why does it say NPU runtime".
17. **"Runtime" naming**: DONE for the Router section header — GenieX
    dispatches across NPU/GPU/CPU (hybrid default in its llama_cpp
    backend); hardware split is the runtime's business, not a UI category.
18. **Browser**: wants standard chromium-style chrome — its own window,
    toolbar, sidebar menu, desktop/mobile UA toggle, zoom, themes, tab
    select, scroll-to-hide, save/share/print-to-PDF, cookie controls.
    Cloud Console loads but Cloud Shell editor/terminal spins forever
    (likely UA or popup/WebView limitation). OPEN — big but contained.
19. **VS Code app + direct shell access** — operator floated; parked as
    future (their own call: "probably too much right now").

## Technical questions (ANSWERED — record so they don't get re-asked)

20. **"Model shows ORT runtime + QNN SDK — where's QAIRT/HTP?"** Correct
    observation, and it's intentional-for-now: the CURRENT GGUF path runs
    on GenieX's llama_cpp/GGML backend (which dispatches to the NPU via
    ggml-hexagon — that's what "npu" device means there). The QAIRT plugin
    (the AI-Engine-Direct/HTP-SDK backend) is a separate runtime inside
    GenieX that reads pre-compiled QAI Hub bundles, NOT GGUFs, so it can't
    serve the Q4_0 file. The max-performance QAIRT/HTP path becomes real
    either via (a) a 9B AI-Hub bundle (the dormant compile pipeline's
    purpose), or (b) QAIRT's `gguf_builder` (newly discovered — can take
    GGUF directly to HTP-optimized execution without the ONNX→QAI Hub
    round-trip; see `docs.qualcomm.com/doc/80-87189-2/topic/gguf_builder.html`,
    published 2026-07-08). The `gguf_builder` path may make the entire
    compile pipeline obsolete — investigate before triggering Job 8.
21. **"Is HTP SDK part of QAIRT?"** Yes — QAIRT is the umbrella runtime
    SDK; QNN/AI-Engine-Direct is its granular layer; HTP is the Hexagon
    backend within it. See knowledge/qairt-sdk/overview.md.
22. **`download.bin` (3.6 MB, in Downloads)** — a stray/unnamed download
    artifact from an earlier device session (it sat next to tokenizer.json
    in the July inventory, likely a renamed fetch that never got its real
    filename). Not used by anything; safe to delete on-device.
23. **`hybrid_llama_qnn.pte` (888 MB)** — an ExecuTorch (.pte) build of a
    LLaMA model with QNN delegation ("hybrid" = some ops on HTP, rest on
    CPU). From earlier experiments; NOT loadable by GenieX or ort_engine
    (would need the not-built executorch_engine daemon per
    NPU-RUNTIME-PATHS.md). Keep or delete — it's inert either way.
24. **`mtp-gemma-4-E2B-it-BF16.gguf`** — Gemma with MTP = Multi-Token
    Prediction (speculative-decoding-style head that drafts several tokens
    per step); E2B = the effective-2B-parameter MatFormer slice; BF16 =
    unquantized brain-float weights. It IS a GGUF, so GenieX's llama_cpp
    backend can try it — but BF16 means no Q4 memory savings.
25. **Key vault section "no idea how it works"** — it's AppStateStore
    (encrypted prefs) behind the Settings fields; the fields' edit UX being
    broken (#3) is why it feels opaque. Fixing #3 is the answer.

## Notes for whoever works this list

- The honesty rule from the status-badge fix generalizes: a control that
  does nothing is a bug, full stop. Prefer deleting scaffold UI over
  leaving it to be discovered dead.
- Items 2–4 are probably ONE root investigation: if Compose text fields /
  buttons are broken app-wide on-device but fine in code review, check
  the interaction between the edge-to-edge insets change and touch
  targets, and whether TokenField requires a Save tap that's off-screen.
- **QAIRT `gguf_builder` (new finding, session 18):** Qualcomm's QAIRT
  Python API now has a `gguf_builder` module that can take a GGUF file
  and produce an HTP-optimized artifact for the QAIRT runtime — no ONNX
  export, no QAI Hub compile job. This is a potential third path between
  "raw GGUF on ggml-hexagon" and "full ONNX→QAI Hub compile." If it works
  with the Qwen3.5-9B Q4_0 GGUF, the dormant compile pipeline (Job 8,
  `compile/compile_qwen3_5_9b.py`) may be entirely unnecessary. Investigate
  this before ever triggering Job 8. Source:
  `docs.qualcomm.com/doc/80-87189-2/topic/gguf_builder.html` (2026-07-08).
