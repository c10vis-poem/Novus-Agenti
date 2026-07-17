# Operator Punch List — first real hands-on review (2026-07-17)

> Source: the operator's systematic walkthrough of the session-17 APK on the
> actual Razr Ultra. This is the ground-truth gap list between what the UI
> *shows* and what actually *works*. Work it top to bottom; nothing here is
> speculative — every line is something the operator personally hit.
> Status legend: FIXED (in branch) · ROOT-CAUSED (fix known, not yet built) ·
> OPEN (needs design/build) · ANSWERED (was a question, answered in-line).

## P0 — the backend never activating (FIXED in branch, this commit)

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
   OPEN: copy-on-bubble + full clipboard support on all text surfaces.
5. **Zoom works nowhere** (pinch-zoom shipped only for chat bubbles; the
   operator expects it across panels). OPEN: app-wide text-scale.
6. **STT/TTS layer decorative**: STT tab inert; NO TTS tab; voice pitch
   selector missing (only speed); chat mic records but nothing types; OPEN
   — depends on media_daemon actually running on-device (needs moonshine/
   kokoro model dirs present) + wiring DaemonTtsClient into the speak path
   (in-process TTS deliberately disabled after the launch crash).
7. **Live mode**: goes straight to single-app share (broken) instead of
   whole-screen capture. OPEN (MediaProjection config).
8. **Home-screen chat bar opens the full Chat tile** instead of a 1/3
   quick-chat overlay. OPEN.
9. **Monitor**: terminal row should expand to ~1/3 screen (currently one
   line); Models/Files/Network/System tabs are read-only copy boxes — no
   manual entry, no paste. OPEN.
10. **Shortcut button does nothing.** OPEN — wire or delete.
11. **"Old tile still there"** on the launcher despite the manifest having
    exactly ONE launcher activity now. Likely a stale pinned/cached
    launcher shortcut from the pre-fix install surviving reinstall —
    verify on-device (long-press → remove), and check it's not the
    TerminalTile quick-settings tile being mistaken for a launcher icon.
    ROOT-CAUSED (probably), needs on-device confirmation.

## P2 — architecture / UX redesign (operator direction, agreed)

12. **Model + Runtime Library** (replaces Monitor's flat model list AND
    Router's single picker): top level lists the RUNTIMES (GenieX,
    ort_engine, media daemon, cloud APIs, terminal), tapping one opens the
    models compatible with THAT runtime. Loaded models act like a library
    you switch between, not a directory listing. OPEN — this is the real
    fix for "gnarly flat list" + "why does it say NPU runtime".
13. **"Runtime" naming**: DONE for the Router section header — GenieX
    dispatches across NPU/GPU/CPU (hybrid default in its llama_cpp
    backend); hardware split is the runtime's business, not a UI category.
14. **Browser**: wants standard chromium-style chrome — its own window,
    toolbar, sidebar menu, desktop/mobile UA toggle, zoom, themes, tab
    select, scroll-to-hide, save/share/print-to-PDF, cookie controls.
    Cloud Console loads but Cloud Shell editor/terminal spins forever
    (likely UA or popup/WebView limitation). OPEN — big but contained.
15. **VS Code app + direct shell access** — operator floated; parked as
    future (their own call: "probably too much right now").

## Technical questions (ANSWERED — record so they don't get re-asked)

16. **"Model shows ORT runtime + QNN SDK — where's QAIRT/HTP?"** Correct
    observation, and it's intentional-for-now: the CURRENT GGUF path runs
    on GenieX's llama_cpp/GGML backend (which dispatches to the NPU via
    ggml's Hexagon support — that's what "npu" device means there). The
    QAIRT plugin (libgeniex_core/vlm — the AI-Engine-Direct/HTP-SDK
    backend the QAIRT manual documents) is deliberately OFF in our CI
    build; it loads Qualcomm AI-Hub bundles, NOT GGUFs, so it can't serve
    the Q4_0 file anyway. It IS present in the official
    geniex-bench-android bundle already on the device. The max-performance
    QAIRT/HTP path becomes real when a 9B AI-Hub bundle exists (that's the
    dormant compile pipeline's whole purpose). So: GGUF→GGML backend now;
    QAIRT backend later; both under the same GenieX daemon and wire.
17. **"Is HTP SDK part of QAIRT?"** Yes — QAIRT is the umbrella runtime
    SDK; QNN/AI-Engine-Direct is its granular layer; HTP is the Hexagon
    backend within it. See knowledge/qairt-sdk/overview.md.
18. **`download.bin` (3.6 MB, in Downloads)** — a stray/unnamed download
    artifact from an earlier device session (it sat next to tokenizer.json
    in the July inventory, likely a renamed fetch that never got its real
    filename). Not used by anything; safe to delete on-device.
19. **`hybrid_llama_qnn.pte` (888 MB)** — an ExecuTorch (.pte) build of a
    LLaMA model with QNN delegation ("hybrid" = some ops on HTP, rest on
    CPU). From earlier experiments; NOT loadable by GenieX or ort_engine
    (would need the not-built executorch_engine daemon per
    NPU-RUNTIME-PATHS.md). Keep or delete — it's inert either way.
20. **`mtp-gemma-4-E2B-it-BF16.gguf`** — Gemma with MTP = Multi-Token
    Prediction (speculative-decoding-style head that drafts several tokens
    per step); E2B = the effective-2B-parameter MatFormer slice; BF16 =
    unquantized brain-float weights. It IS a GGUF, so GenieX's llama_cpp
    backend can try it — but BF16 means no Q4 memory savings.
21. **Key vault section "no idea how it works"** — it's AppStateStore
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
