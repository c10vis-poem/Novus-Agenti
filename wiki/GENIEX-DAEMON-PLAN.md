# GenieX Daemon Plan — Qwen3.5-9B runtime on HTP SDK

> Runtime decision (session 15, LOCKED): **backend = HTP SDK (QAIRT),
> runtime = `GenieX`, wired to a SEPARATE detached daemon.** This doc scopes
> how GenieX replaces `ort_engine` behind the existing socket without
> touching the watchdog or the crash-loop fix. See CLAUDE.md §Single-Path
> Architecture and the QAIT>ORT decision block.
>
> **Session 16 reconfirmation:** vision stays co-located with the LLM in this
> same daemon/socket (matches `libgeniex_vlm` being part of the same QAIRT
> backend below) — STT/TTS are the separate process, not vision. `ort_engine`'s
> wire contract now carries an optional `image_b64` field end-to-end
> (`NpuClient.kt` → `main.cpp` → `Engine::generate`) as a scaffold ahead of
> GenieX landing; see `NpuClient.kt`'s class doc and `engine.h`'s
> `GenerateRequest::image_b64` for the current (stubbed, not yet decoding)
> state, and `http_server.cpp`'s single-`recv()` 8KB-buffer limitation that
> must be fixed before real image payloads can round-trip.

## What stays exactly the same

The whole point of the daemon/socket design is that **only the thing behind
the socket changes.** These do NOT change:

- **The wire contract** — `NpuClient.kt` still `POST`s to
  `http://127.0.0.1:8080/api/v1/generate` and polls `/health`. Whatever runs
  behind :8080 must speak that contract (or a thin shim maps GenieX's HTTP/CLI
  to it).
- **`CliffordService` (CLIFFORD == Watchdog)** — FGS, `START_STICKY`,
  `specialUse`, exponential-backoff + 5-strike relaunch, `oom_score_adj`
  anchoring. Untouched.
- **The crash-loop fix** — serve-first: bind :8080 immediately, load the model
  on a background thread, serve `/health` 503 until ready, never suicide on a
  bad/missing model. GenieX's launcher must preserve this "alive ≠ ready"
  property so the watchdog never thrashes.
- **`DaemonLauncher.kt`** — `sh -T-` detach, reparent to init, pushed to
  `/data/local/tmp`. Same launch mechanism, new binary.
- The hard rule: **no in-process tensor runtime.** GenieX runs as its own
  detached process, not a LiteRT `CompiledModel` inside the UI process. (That
  in-process option was explicitly rejected.)

## What changes

- **`ort_engine` → legacy.** It stays in the repo as one of several uploadable
  runtime binaries (`RUNTIME_FILES`), but it is no longer the Qwen3.5-9B path.
- **New runtime binary: `geniex_server`** (working name) — a GenieX-based
  daemon that loads the QAIRT/HTP artifact and serves the :8080 contract.
- **Model artifact format** shifts from ORT's `qnn_context_binary` loaded via
  ONNX Runtime + QNN EP to whatever GenieX consumes on the HTP backend
  (QAIRT context binary / GenieX bundle — confirm against the GenieX repo +
  Drive `#QAIRT/` manual once the fork lands).

## GenieX facts (verified this session — README via web fetch)

- Repo: **`github.com/qualcomm/GenieX`** — official Qualcomm, ~8k★, **Rust**.
  Scope: "run frontier LLMs and VLMs locally on Qualcomm devices across
  **NPU, GPU, and CPU**." NOT QNN's `genie-t2t-run` ("Genie").
- **Ships an OpenAI-compatible HTTP server out of the box.** `geniex serve`
  → `http://127.0.0.1:18181/v1` (OpenAI-compatible; works with OpenAI client
  libs). Also `geniex infer` (one-shot) and `geniex pull` (model management).
- **Two runtimes under one interface:** (a) **llama.cpp / GGML** on NPU/GPU/CPU,
  and (b) **Qualcomm AI Engine Direct (QAIRT)** — **NPU-only, max performance**.
- **Model formats:** **GGUF** (any from Hugging Face) OR **Qualcomm AI Hub
  pre-compiled bundles**. No separate ONNX/QNN-context step required by GenieX.
- **Model acquisition:** `geniex pull`; HF GGUF (e.g. `unsloth/Qwen3.5-2B-GGUF`)
  or AI Hub bundles (e.g. `ai-hub-models/Qwen3-4B`).
- **Quantization:** **`Q4_0` is explicitly recommended — "best Hexagon NPU
  support."** (This VALIDATES the `gemini-query/qwen-3.5-9b-q4_0.md` doc a
  prior pass wrongly filed as a "third-runtime conflict"; llama.cpp-npu-style
  Q4_0 is literally GenieX's GGML runtime.)
- **Bindings:** Python (`from geniex import AutoModelForCausalLM`), **Kotlin/Java
  Android SDK** (`com.qualcomm.qti:geniex-android:0.3.1` via Gradle), C header
  (`sdk/include/geniex.h`), CLI, Docker. Build system: **Bazel**.
- **Platforms:** Snapdragon 8 Elite / 8 Elite Gen 5 (Android), Snapdragon X /
  X Elite (Windows ARM64), Dragonwing QCS9075 (Linux ARM64). **Snapdragon 8
  Elite = SM8750 = our target** (confirms the 8-Elite/v79 line, not v75).
- Linux CLI install: `curl -fsSL <qaihub s3>/qai-hub-geniex/install.sh | sh`.

## Answered → the daemon collapses to `geniex serve`

The scary parts are gone. GenieX already **is** an HTTP inference server, so
the "separate detached daemon" = **run `geniex serve` as the runtime binary**,
launched by `DaemonLauncher`, guarded by `CliffordService`. No hand-written
C++ server needed.

- **Q1 (HTTP surface)** → solved. `geniex serve` on `:18181/v1`, OpenAI-compat.
  Two options: (a) point `NpuClient.kt` at `:18181/v1/chat/completions` and
  adopt the OpenAI request/response shape, or (b) keep the current
  `:8080 /api/v1/generate` contract and run a tiny shim that translates to
  GenieX's `:18181/v1`. **(a) is cleaner** — standard OpenAI wire format, and
  the health check becomes a GET on `/v1/models`. Keep the serve-first / "alive
  ≠ ready" behavior so the watchdog doesn't thrash while the model loads.
- **Q2 (model path)** → **the 9B is NOT turnkey on GenieX yet** (operator
  correction). Two routes, at different readiness:
  - **Today: Q4_0 GGUF** of `Mer0vin8ian/Qwen3.5-9B` via GenieX's **GGML /
    llama.cpp backend** (NPU/GPU/CPU). This is the path that actually runs now.
  - **Max-perf QAIRT / AI-Engine-Direct (NPU-only) needs BYOM.** Qualcomm's
    prebuilt AI Hub library only ships **smaller** Qwen variants right now (a
    Qwen3.5 variant + a 0.8B text-only), **not** the 9B — so the 9B on the QAIRT
    backend requires **BYOM-compiling it via the QAI Hub workbench**, i.e. the
    existing `compile/compile_qwen3_5_9b.py` compile track / Job 8 producing the
    AI Hub bundle GenieX loads. This is where the two tracks connect: the
    compile pipeline is NOT wasted — it's the prerequisite for the fast path.
- **Q3 (quant)** → **Q4_0** for the GGUF route (GenieX's own recommendation);
  W4A16/mixed-precision still applies to the AI-Hub-bundle route. Both live in
  `knowledge/` (FraQAT, NPU scaling paper, the Q4_0 gemini doc).
- **Q4 (SoC)** → **Snapdragon 8 Elite / SM8750**, confirmed by GenieX's own
  platform list. The CLAUDE.md "v75" label is the thing to reconcile (8 Elite
  is v79). Flagged for operator confirm; not silently changed.
- **Q5 (CI / build)** → likely **no `ort_engine` cross-compile at all**: ship
  the **prebuilt GenieX aarch64-android CLI** as the runtime binary (or use the
  `geniex-android` Gradle SDK for an in-app path — but that's IN-PROCESS and is
  rejected here; the **`geniex serve` binary** keeps the separate-daemon rule).

## Grounded HTTP contract (from GenieX docs)

Confirmed verbatim from the GenieX README:

```bash
geniex pull ai-hub-models/Qwen3-4B-Instruct-2507   # fetch a model
geniex serve                                        # serves http://127.0.0.1:18181/v1
```
```bash
curl http://127.0.0.1:18181/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"model": "ai-hub-models/Qwen3-4B-Instruct-2507",
       "messages": [{"role": "user", "content": "Hello!"}]}'
```

So the wire seam is settled: **standard OpenAI `POST /v1/chat/completions`** on
`127.0.0.1:18181`, model selected by the `"model"` string in the body.

- **Readiness / health** — OpenAI-standard: **`GET /v1/models` → 200** once the
  server is up and the model is registered. That replaces the old `/health` 503
  gate. Keep the serve-first / "alive ≠ ready" behavior: the watchdog treats
  "port open but `/v1/models` not 200 yet" as *loading*, not *dead*.
- **`NpuClient.kt` change** — swap the `:8080 /api/v1/generate` call for a
  `:18181 /v1/chat/completions` OpenAI request (messages array in, streamed
  `choices[].delta.content` out), and point the readiness probe at
  `/v1/models`. This is a documented, stable contract — not a guess.

### Still needs the repo source (fork → `add_repo` to read)

The README does **not** document these; they require reading the GenieX repo:
- Exact `geniex serve` flags: **port override, bind/host, backend selector**
  (QAIRT/AI-Engine-Direct NPU-only vs GGML), and **which model** to serve when
  several are pulled.
- Whether a dedicated `/health` (or `/readyz`) exists beyond `GET /v1/models`.
- `geniex pull` syntax for a **specific HF GGUF** (vs the `ai-hub-models/...`
  bundle shown) and how **Q4_0** is chosen ("pick Q4_0 when prompted").
- **Android/aarch64 packaging as a detached binary.** The README only documents
  the in-process Gradle SDK (`com.qualcomm.qti:geniex-android:0.3.1`) — which is
  the rejected in-process path. Need to confirm a **standalone `geniex` CLI
  binary** for arm64-android (prebuilt release vs Bazel build) to run it as the
  detached daemon. Sample app lives in `qualcomm/ai-hub-apps`.

**Because of the above, `NpuClient`/`DaemonLauncher`/CI wiring is NOT written
yet** — the OpenAI wire format is safe to implement, but the serve flags, the
readiness endpoint, and the standalone-binary packaging must be confirmed
against source first (audit-before-feature, per the app-pathway mandate).

## Next steps (in order)

1. Operator forks `qualcomm/GenieX` → `c10vis-poem/GenieX` (agent session is
   scoped to `c10vis-poem` only; cross-owner fork/add is walled off). Once it's
   under `c10vis-poem`, `add_repo c10vis-poem/GenieX` can pull it into a session
   so the real source (exact `geniex serve` flags, health endpoint, GGUF vs
   bundle loading, Android packaging) can be read directly.
2. Ingest Drive `#QAIRT/` (Context/Backend/Api/Graph/Tensor/HTP/Overview) into
   `knowledge/qairt-sdk/` as the HTP/AI-Engine-Direct reference.
3. Decide the wire seam: adopt OpenAI format in `NpuClient.kt` (preferred) vs a
   `:8080→:18181` shim.
4. Get the Qwen3.5-9B **Q4_0 GGUF** (fits the ~5.5 GB envelope) and/or the AI
   Hub bundle; `geniex pull` / `geniex serve` on device.
5. Package `geniex serve` as the runtime binary in `build-apk.yml`; keep
   `ort_engine` as the legacy fallback until GenieX is verified on-device.
