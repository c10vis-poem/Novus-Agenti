# Session 16 Handoff — 2026-07-13

Branch: `claude/notice-agent-ui-local-xa14op` (new — this is now the active
app/UI-fork track branch; `claude/on-device-inference-openwiki-sae7cy` merged
to main this session as PR #17).

## What landed

**Daemon split, confirmed and wired as contracts:**
- Vision stays in the same daemon/process as the LLM (one socket,
  `127.0.0.1:${DaemonLauncher.ENGINE_PORT}`). `NpuClient.kt` now overrides
  `streamImage()` for real (was falling through to the `LlmRuntime` default
  stub) and sends an optional `image_b64` field on `/api/v1/generate`.
  `ort_engine`'s `GenerateRequest` gained `image_b64`; `Engine::generate`
  acknowledges an image with an honest "VLM decode not yet wired" note instead
  of silently ignoring it.
- STT + TTS are confirmed as the separate media daemon (NOT vision — fixed a
  stale doc claim in `DaemonSttClient.kt` that used to list vision as one of
  the media daemon's models). Added `core/tts/DaemonTtsClient.kt`, the TTS
  sibling of `DaemonSttClient`, same `127.0.0.1:8091` base, new `/tts`
  endpoint. Contract-only — not wired as a caller yet; `SherpaOnnxTtsClient`
  still does TTS in-process. Migrating it is a follow-up, not done here.

**New local UI fork:**
- `com.horizons.uilocal.LocalHomeActivity` + `LocalHomeScreen.kt`. Additive —
  `MainActivity`/`HomeGrid` untouched. Boots immediately with no gate on the
  model daemon (mirrors the daemon-side serve-first fix at the UI layer).
  Two independently-polled status rows (model+vision daemon, media daemon),
  neither blocks the chat input. Chat is wired to the real
  `HorizonsApplication.llmRuntime`/`sendChat()`/`chatMessages`, not a mock.
  Registered in `AndroidManifest.xml` as a second launcher activity labeled
  "Novus Agenti (Local)" so it installs side by side with the existing app.

## Known gaps — flagged, not fixed this session

1. **`daemon/src/http_server.cpp` truncates large bodies.** Single `recv()`
   into an 8KB buffer — fine for text prompts, but `image_b64` payloads
   (100KB+) will be silently cut off. Needs a real read-until-Content-Length
   loop before vision can round-trip for real. Documented inline in the file
   and in `wiki/GENIEX-DAEMON-PLAN.md`.
2. **No real media-daemon binary exists yet.** `DaemonSttClient`/
   `DaemonTtsClient` both assume something is listening on `127.0.0.1:8091`;
   nothing in this repo currently binds that port. It needs its own daemon
   binary (Moonshine STT + Kokoro/Sherpa TTS), analogous to `daemon/` for the
   LLM+vision side, launched/guarded the same way `ort_engine` is by
   `DaemonLauncher`/`CliffordService`.
3. **GenieX itself is still not forked/integrated** — this session's changes
   are ort_engine-side contract scaffolding done ahead of that, per
   `GENIEX-DAEMON-PLAN.md`'s existing "next steps" list (unchanged: fork
   `qualcomm/GenieX` → `c10vis-poem/GenieX` first).
4. **Gradle/Android SDK not available in this session's container** — could
   not compile-check the Kotlin/Compose changes locally; relying on CI
   (`build-apk.yml`) to catch any build errors on push.

## Next session should

1. Watch CI on this branch's PR for build errors (Kotlin/Compose changes here
   were not locally compiled).
2. If the operator confirms GenieX has been forked, `add_repo
   c10vis-poem/GenieX` and resume `GENIEX-DAEMON-PLAN.md`'s "next steps."
3. Decide whether to build a real media-daemon binary now or continue
   scaffolding other contracts first — this session left it as an open gap,
   not a decision.
