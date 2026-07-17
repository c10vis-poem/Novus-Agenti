# Boot / Loading-Phase Sequence — Horizons App

> The authoritative init-order contract (Pending item 4, session 17).
> Derived directly from the code as it exists on this branch —
> `HorizonsApplication`, `CliffordService`, `DaemonLauncher`,
> `CliffordBootReceiver`, `MainActivity`, `LocalHomeActivity`,
> `LocalHomeScreen`. If code and this doc disagree, fix one of them —
> per CLAUDE.md's hygiene protocol, flag it, don't silently pick.

## Process topology (Phase 0 — before anything "boots")

Two Android processes, deliberately isolated so LMKD scores them separately:

| Process | Contains | Why separate |
|---|---|---|
| `com.horizons` (main) | UI activities, `HorizonsApplication` singletons, assist services | crashes here must not take the daemon guardian down |
| `com.horizons:clifford` | `CliffordService` (FGS watchdog) | FGS oom_score_adj (~ -200 to -400) is inherited by the native daemon it spawns |

The **native daemons are not Android processes at all**: the model+vision
daemon (ort_engine today, `geniex serve` later) is spawned via `sh -T-` and
reparented to init, so after launch AMS never touches it again.
`HorizonsApplication.onCreate()` runs in BOTH app processes — the
`isMainProcess()` check short-circuits the `:clifford` copy after
Breadcrumb + AppStateStore init only.

## Entry points (three ways the system starts us)

1. **User launches an activity** → main process spins up → Phase 1 → Phase 3.
2. **Device boot** → `CliffordBootReceiver` (BOOT_COMPLETED / QUICKBOOT) →
   30s deferred exact alarm (Android 15+ forbids direct FGS start from boot)
   → `CliffordService` starts with NO main process → Phase 2 runs alone.
   The main process joins later when the user opens the app.
3. **System binds an assist/voice service** (user set us as default
   assistant) → main process spins up → Phase 1 runs, then the service
   binds. Assist services touch only lazy singletons, so binding at any
   point is safe.

There is no ordering requirement between the main process and `:clifford` —
each side must (and does) tolerate the other not existing yet.

## Phase 1 — Main-process application init (`HorizonsApplication.onCreate`)

Synchronous, in this order, every step individually try/caught with a
Breadcrumb so a failure is visible in CLIFFORD's notification:

1. `Breadcrumb.install` — crash-visibility first, before anything can fail
2. `isMainProcess()` gate (non-main: AppStateStore only, return)
3. `CrashRecorder.install`
4. `AppStateStore` — the only synchronous disk read allowed here
5. `CliffordService.start()` — fire-and-forget into the `:clifford`
   process; failure breadcrumbed, never fatal to main
6. `cloudRuntime.refreshStatus()` — so `llmRuntime` can resolve to cloud
   before the NPU daemon is up
7. `kokoroManager.ensureReady()` — kicks off TTS model download/extract
   (async); a collector later calls `tts.init()` when it reports Ready
8. Async fire-offs on `scope`: kokoro-state → `tts.init()`, `stt.probe()`
   (media-daemon connectivity flag only — nothing launches that daemon
   yet, see Pending 2), voice-settings persistence collectors

**Rule: nothing in Phase 1 blocks on a daemon, a network call, or a model
file.** Everything heavyweight is lazy (`by lazy`) or launched async.

## Phase 2 — Daemon bring-up (`:clifford`, CRS loop)

`onStartCommand` → Breadcrumb → `startForeground` (notification = live
state readout) → CRS coroutine:

**2a. Launch attempt (`ensureDaemonRunning`)** — gates in order:
`NativeBinaryInstaller.install` → binary present? (else `BinaryMissing`,
re-checked each tick, not a failure) → model resolvable via
`resolveNpuModelPath()`? (else `ModelMissing`, same) →
`DaemonLauncher.launch("--model", path)` — `sh -T-` detach from THIS FGS
context so the daemon inherits FGS-level oom protection at birth
(root `-1000` upgrade attempted, silent no-op without root).

**2b. Heartbeat (every 15s), the load-phase state machine:**

```
process dead?  ──yes──▶ relaunch w/ backoff 15s→240s; after 5 consecutive
     │                  failures → Failed (idle until model file signature
     │no                changes, then auto re-arm)
     ▼
/health code:  200 → Healthy → app.activateNpuRuntime() + acquireNpuPerfLock()
               503 → Loading (model loading OR load-failed; daemon is alive
                     and serving — NEVER relaunched)
               -1  → Unhealthy (port not answering yet)
```

**The two key separations (LAW, they broke crash-loops before):**
- **Liveness decides relaunch; readiness decides activation.** A live
  process is never relaunched no matter what `/health` says.
- **Perf lock only after Healthy**, acquired from the `:clifford` process
  (reflection on the `@hide` NpuManager), released in `onDestroy`.

`activateNpuRuntime()` is the single cross-process handoff: it swaps the
main app's `llmRuntime` resolution to `NpuClient`. Until then `llmRuntime`
resolves NpuClient → configured cloud → honest fallback stub, in that order.

## Phase 3 — UI activation (both activities)

**Serve-first is the contract:** neither `MainActivity` nor
`LocalHomeActivity` gates composition on any daemon. `MainActivity`
additionally does: `setDecorFitsSystemWindows(false)` → runtime-permission
request (RECORD_AUDIO, POST_NOTIFICATIONS 33+) → optional deep-link tab →
`setContent`. No permission is a boot gate; denial degrades features, not
boot. (The old auto-launch of MANAGE_ALL_FILES on cold start was removed —
do not reintroduce.)

`LocalHomeScreen` then polls both daemons' `/health` every 3s purely for
the status chips (READY/LOADING/OFFLINE), mirroring but never replacing
CRS's authoritative loop. Chat input is never disabled by daemon state —
sending early surfaces the runtimes' own "[not ready]" messages.

## Phase 4 — Voice/assist services (system-driven, not boot steps)

`HorizonsVoiceInteractionService` / `...SessionService` /
`HorizonsRecognitionService` are manifest-registered; **the system binds
them on demand** when we're the selected assistant. They are not started
during Phases 1–3 and must keep working whenever bound, which they do by
only touching `HorizonsApplication`'s lazy singletons
(`transcribeAudio`, `llmRuntime`, `tts`).

## Invariants (checklist for any future boot-path change)

- [ ] I1 **Serve-first** — no activity, ever, gates on daemon readiness
- [ ] I2 **Liveness ≠ readiness** — relaunch only on dead process; 503 never relaunches
- [ ] I3 **Perf lock follows Healthy** — never acquired speculatively; released on service destroy
- [ ] I4 **Daemon spawns from the FGS context** — oom_score_adj inheritance is the whole point of `:clifford`
- [ ] I5 **Phase 1 does no blocking I/O** beyond AppStateStore; heavyweight = lazy/async
- [ ] I6 **Every Phase-1/2 step breadcrumbed + isolated** — one failed step degrades, never crashes boot
- [ ] I7 **Either process may start first** — no cross-process ordering assumptions

## Known non-wired pieces (tracked in CLAUDE.md Pending, not bugs here)

- **Media daemon (`:8091`)** — probed and displayed, but nothing launches
  it; `ensureDaemonRunning` covers only the model+vision daemon (Pending 2).
- **GenieX** — when it replaces ort_engine behind the socket, Phase 2 is
  unchanged conceptually; only `ENGINE_PORT`/binary name/launch args move
  (see `wiki/GENIEX-DAEMON-PLAN.md`).
