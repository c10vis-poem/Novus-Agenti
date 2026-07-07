# Reference Implementations — Dissection & Applicable Patterns

> Companion to `HORIZONS-BLUEPRINTS.md`. Raw findings from binary analysis,
> source inspection, and architecture review of every reference project.
> Each section ends with **APPLICABLE TO HORIZONS** — the specific pattern
> we should steal, adapted to our constraints.

---

## 1. SocketSweep

**What it is:** Tauri (Rust) desktop app that bundles its own ADB binary +
ARM64 daemon. Scans a phone's filesystem over a TCP tunnel without MTP.

### Deployment Flow (reconstructed from binary strings)

```
1. check_adb        — verify USB debugging is on, device authorized
2. pkill -f socketsweep_daemon || true   — kill any prior instance
3. adb push daemon /data/local/tmp/socketsweep_daemon
4. appops set com.android.shell MANAGE_EXTERNAL_STORAGE allow
5. nohup ./socketsweep_daemon &          — daemon starts, detached
6. adb forward tcp:PORT tcp:PORT         — tunnel to host
7. PING health check over TCP            — confirm daemon responds
```

### MTP Bypass

SocketSweep never touches MTP. It uses ADB (the USB debugging protocol) to
push the daemon binary to `/data/local/tmp/`. The daemon then accesses
`/sdcard` via direct filesystem calls, not via MTP. The `appops set
com.android.shell MANAGE_EXTERNAL_STORAGE allow` command grants the shell
user (UID 2000) full storage access without any UI permission dialog.

### Kill-Switch Bypass

The daemon runs under the **shell user** (UID 2000) via ADB — NOT under any
app UID. Android's battery optimization, doze mode, app standby, and force
stop only target app processes (those running under app UIDs). Shell user
processes are treated like system processes. `nohup` ensures the daemon
survives the ADB session ending.

### Daemon Architecture (from `daemon.cpp` strings)

- Single-threaded TCP server on `127.0.0.1:PORT`
- Signal handler for clean shutdown (`on_signal`)
- `scan()` function: recursive filesystem traversal, sorts results
- JSON response: `{"status":"ok","scan_time_ms":...}`
- Log format: `[engine] Listening on %s:%u (pid %d)`
- Clean exit: `[engine] Shutdown complete.`
- Built with Android NDK (linker64, libc.so)

### APPLICABLE TO HORIZONS

1. **Daemon-as-TCP-server on localhost** — identical to our `ort_engine` /
   `llama-server` architecture. Validated pattern.
2. **`/data/local/tmp` deploy path** — only works with ADB access (desktop
   companion or Shizuku). Horizons uses jniLibs packaging instead (exec from
   nativeLibraryDir), which works without ADB but IS subject to Android
   lifecycle. Trade-off is correct for a standalone app.
3. **`MANAGE_EXTERNAL_STORAGE` via appops** — we could use this for model
   file access from Download/ if `requestLegacyExternalStorage` proves
   flaky. Requires Shizuku or ADB.
4. **Health-check pattern** — PING over TCP before considering daemon ready.
   CliffordService should implement this instead of assuming the daemon is
   alive after Process.start().

---

## 2. OverlayD-AI

**What it is:** Fully offline AI assistant running in Termux. Uses Shizuku
for privilege escalation, llama.cpp as the inference server, Node.js as
the orchestration bridge, Telegram as the UI.

### Architecture Stack (from the 13-page technical guide)

```
Layer 1: UI              — Telegram Bot (natural language I/O)
Layer 2: Orchestration   — Node.js bridge (prompt engineering, cmd parsing)
                         — OpenClaw Local (vision capture, UI analysis)
Layer 3: AI Inference    — llama.cpp + Qwen vision model
Layer 4: Privilege       — Shizuku + rish (ADB privileges without root)
Layer 5: System          — Android APIs (hardware, apps, settings)
```

### Sandbox Escape — Shizuku + rish

The core innovation: uses Android's Wireless Debugging feature to acquire
ADB permissions without root. Shizuku exports a `rish` command into Termux
that executes with system-level privileges:

```bash
rish -c "input keyevent 3"          # force home
rish -c "monkey -p com.youtube 1"   # launch app
rish -c "svc wifi disable"          # toggle WiFi
```

This creates the bridge: the AI (sandboxed in Termux) generates ADB
commands, Node.js wraps them in `rish`, and the system executes them.

### OpenClaw-Local Vision Interception

OpenClaw is designed for ChatGPT over the internet. OverlayD-AI intercepts
it with a custom `openclaw-local` executable that redirects all API calls
to `http://127.0.0.1:8080/v1` with a dummy API key (`sk-local-offline`).
The closed loop: screenshot → OpenClaw UI analysis → LLM inference →
coordinate extraction → `rish` tap command.

### Inference Server Config

```bash
./server -m model.gguf \
         --host 127.0.0.1 \
         --port 8080 \
         -c 4096 \
         --timeout 300
```

Identical to our llama-server configuration.

### APPLICABLE TO HORIZONS

1. **Shizuku/rish as optional privilege escalation** — Horizons' Agent tools
   (`ScreenshotCapture`, `SecureResourceRelay`) need system-level access
   that the app sandbox doesn't provide. Shizuku integration (optional,
   for power users) would unlock: screen capture without MediaProjection
   dialog, app launch/control, system setting toggles. Add as an optional
   capability, not a requirement.
2. **OpenAI-compatible localhost endpoint** — proven pattern. Our
   `llama-server` already exposes `/v1/chat/completions`. Any tool that
   speaks OpenAI format (OpenClaw, LangChain, etc.) works out of the box.
3. **Few-shot prompt → structured command extraction** — the `CMD:` prefix
   pattern for getting the LLM to emit executable commands. AgentLoop
   already does this with tool calls; the pattern validates it.
4. **Vision-to-action closed loop** — screenshot → LLM → tap coordinates.
   This is the Vision-Agent tile in FEATURE-SPEC.md. The OpenClaw approach
   (intercept a cloud tool and redirect to localhost) is how we'd integrate
   third-party vision frameworks.

---

## 3. scrcpy (Genymobile)

**What it is:** 145k-star tool that mirrors and controls an Android device's
screen from a desktop. The server-side Java process runs on the phone.

### Server Deployment Model

1. Desktop client pushes `scrcpy-server.jar` to the device via ADB
2. Server started via `adb shell app_process` (runs as a Java process, not
   an installed APK — so it has ADB shell privileges)
3. **Nothing is left installed on the device** — the server jar is temporary

### Process Lifecycle (`Server.java`)

- Entry: `main()` → `internalMain()` → `scrcpy()`
- Sets uncaught exception handler
- **`dropRootPrivileges()`** — if running as root (UID 0), drops to UID
  2000 (shell user). Comment: "Copy-paste does not work with root user."
- **`Looper.loop()`** — blocks the main thread, keeping the process alive.
  Only exits when `quitSafely()` is called by the Completion handler.
- Cleanup (finally block): stops all AsyncProcessors (audio, video,
  control), shuts down connection, joins threads, closes OpenGL
- **`System.exit(status)`** — explicit exit to kill non-daemon threads
  started by the Android SDK

### Kill-Switch Bypass

Same mechanism as SocketSweep: runs under the **shell user** (UID 2000),
not an app UID. Android's lifecycle management doesn't apply. The process
survives doze, battery optimization, and app standby because it's not an
app process.

When ADB disconnects: the Looper keeps the process alive briefly, but the
connection handler detects the socket closure and triggers the Completion
shutdown. Clean exit, no zombie.

### APPLICABLE TO HORIZONS

1. **`app_process` as a deployment vehicle** — scrcpy proves you can run
   arbitrary Java code on Android as a shell process via `app_process`.
   We could use this for a diagnostic/debugging server that runs alongside
   the main app but outside its lifecycle (useful for monitoring CliffordService
   from outside the app's process group).
2. **`dropRootPrivileges()` pattern** — always drop to lowest needed UID.
   Our daemon doesn't run as root, but if it ever does (e.g. via Shizuku),
   it should drop immediately.
3. **Looper.loop() for process persistence** — the Android way to keep a
   non-UI process alive. CliffordService already uses a foreground service
   (the Android-approved approach), which is correct for an app process.
4. **Explicit System.exit()** — prevents zombie processes from SDK threads.
   CliffordService should call this in its crash recovery path.

---

## 4. Genymobile GitHub Ecosystem

14 repositories. Key ones for Horizons:

| Repo | Stars | Language | Relevance |
|---|---|---|---|
| **scrcpy** | 145k | C | Screen mirroring. See section 3. |
| **gnirehtet** | 7.8k | Java | **Reverse tethering** — provides internet to a phone via USB from a PC. Relevant for development/testing when the phone has no WiFi. |
| **genymotion-device-web-player** | 147 | JS | Web-based device player. Interesting for remote Horizons testing — could embed a web view of the device for debugging. |
| **Magisk** | 1 | Rust | Fork of topjohnwu/Magisk. Root manager. NOT needed for Horizons (we use Shizuku, not root). But confirms Genymobile's deep Android internals expertise. |
| **genymotion-kernel** | 92 | C | Custom Android kernel. No direct Horizons applicability unless we need kernel-level NPU tuning. |
| **genymotion-saas-github-action** | 22 | JS | CI integration for Genymotion Cloud devices. Could use for Horizons CI testing on real Snapdragon hardware if Genymotion supports SM8750. |
| **genymotion-gradle-plugin** | 171 | Groovy | Gradle device control. Could integrate into our build pipeline for automated APK testing. |
| **gm_pr** | 47 | Python | Multi-project PR viewer. Not directly relevant. |

### SaaS Offering

Genymotion Cloud SaaS provides virtual Android devices in the cloud for CI
testing. Available as: GitHub Action (`genymotion-saas-github-action`),
CircleCI Orb (`genymotion-saas-orb`), and Bitrise Step. This is potentially
useful for Horizons CI — running our APK on a virtual Snapdragon device to
verify the daemon lifecycle without needing physical hardware.

### APPLICABLE TO HORIZONS

1. **gnirehtet** — reverse tethering could solve development connectivity
   (user's "phone only, no laptop" constraint means the phone sometimes
   needs internet from another device).
2. **Genymotion Cloud for CI** — if they support SM8750 or similar
   Snapdragon devices, we could run end-to-end daemon tests in CI.
3. **Device web player** — for remote debugging sessions, embed a web
   view of the device's screen.

---

## 5. off-grid-ai-mobile

**What it is:** React Native AI assistant app. The closest architectural
parallel to Horizons — same problem space, production-quality codebase.

### Key Architecture Patterns

#### Provider Abstraction (`src/services/providers/types.ts`)

Single `LLMProvider` interface that ALL backends implement:

```typescript
interface LLMProvider {
  id: string;
  type: ProviderType;        // 'local' | 'openai-compatible' | 'anthropic'
  capabilities: ProviderCapabilities;
  loadModel(modelId: string): Promise<void>;
  unloadModel(): Promise<void>;
  isModelLoaded(): boolean;
  generate(messages, options, callbacks): Promise<void>;
  stopGeneration(): Promise<void>;
  getTokenCount(text: string): Promise<number>;
  isReady(): Promise<boolean>;
  dispose?(): Promise<void>;
}
```

`ProviderCapabilities` declares what each backend CAN do (vision, tool
calling, thinking/reasoning, max context length) — the UI reads capabilities,
never checks which provider is active.

#### ActiveModelService — Single Loading Authority

One class owns ALL model load/unload decisions. No other code path may
load a model directly. This prevents the race conditions and double-loads
that plagued earlier designs:

```
ActiveModelService
  ├── loadedTextModelId    (single source of truth)
  ├── loadedImageModelId
  ├── textLoadPromise      (coalesces concurrent load requests)
  ├── imageLoadPromise
  └── listeners            (change notification)
```

Engine-aware: checks both `llmService.isModelLoaded()` AND
`liteRTService.isModelLoaded()` — the service knows about multiple
engines, but callers don't.

#### Model Residency Manager

Tracks whether model memory is "clean" (loaded, ready) or "dirty" (partially
evicted, needs reload). Handles the Android case where the OS reclaims memory
from backgrounded models — the residency manager detects this and triggers a
reload transparently.

#### Memory Budget Service

Hardware-aware context sizing that steps down gracefully:
`4096 → 2048 → 1024` based on available RAM. Prevents OOM kills by
proactively reducing context window rather than crashing.

#### Zustand as Read-Only Projection

Reactive stores are ONLY for UI rendering. All imperative coordination
(model loads, audio sessions, playback control) lives in services.
The store is a thin projection of service state, never the source of truth.

### APPLICABLE TO HORIZONS

1. **LLMProvider interface** — Horizons should implement this EXACT pattern.
   Each runtime (ort_engine, llama-server, tflite_engine, etc.) implements
   a common interface. NpuClient dispatches to the active provider.
2. **ActiveModelService** — CliffordService + NpuClient should have a single
   authority that owns model load state. Current architecture has this
   roughly right but not formalized.
3. **Memory budget → context step-down** — critical for SM8750 (16GB shared
   with GPU/display). Our `max_seq_len` in manifest.yaml (2048 for target,
   4096 for ideal) already reflects this thinking.
4. **Capability-based routing** — RouterPane should query provider
   capabilities, not provider identity. "Does the current provider support
   vision?" not "Is the current provider ort_engine?"
5. **Zustand discipline** — applies directly to Horizons' Compose state
   management. ViewModel projections, not authoritative state.

---

## 6. kernel and snagboot Forks

**NOT FOUND** under `c10vis-poem`. GitHub search returned zero results for
both `kernel user:c10vis-poem` and `snagboot user:c10vis-poem`. They may
exist under a different account, have been renamed, or were deleted.

`snagboot` (from Bootlin) is a USB boot/flash recovery tool for embedded
Linux — useful for reflashing bricked devices but not directly applicable
to Horizons' runtime architecture. If the forks exist elsewhere, the
relevant pattern would be: low-level USB device communication for recovery
scenarios (factory reset, bootloader unlock).

---

## Summary — Pattern Priority for Horizons

| Priority | Pattern | Source | Where in Horizons |
|---|---|---|---|
| 1 | LLMProvider interface + single loading authority | off-grid-ai-mobile | NpuClient / CliffordService |
| 2 | TCP health check before declaring daemon ready | SocketSweep | CliffordService |
| 3 | OpenAI-compatible localhost endpoint | OverlayD-AI | llama-server (already done) |
| 4 | Capability-based routing (not identity-based) | off-grid-ai-mobile | RouterPane |
| 5 | Memory budget → context step-down | off-grid-ai-mobile | manifest.yaml / daemon config |
| 6 | Shizuku/rish optional privilege escalation | OverlayD-AI | Agent tools (future) |
| 7 | Vision-to-action closed loop | OverlayD-AI | Vision-Agent tile (future) |
| 8 | Shell-user daemon for diagnostics | scrcpy | Debugging tool (future) |
| 9 | Genymotion Cloud CI | Genymobile | CI pipeline (future) |
