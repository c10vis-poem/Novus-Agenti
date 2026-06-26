# Session 6 Handoff — 2026-06-26 evening

> Load AFTER `/memory`, `CLAUDE.md`, and `wiki/SESSION5-HANDOFF.md`.

---

## SECURITY — DO FIRST WHEN RESUMING

1. **Rotate HF token** — https://huggingface.co/settings/tokens → ⋮ on `HF.token` → Refresh
2. **Rotate QAI Hub token** — https://app.aihub.qualcomm.com/account/ → Generate new

Both tokens were pasted in the session 5/6 chat transcript. GitHub's secret scanner even blocked one push containing the HF token value — confirming it matches a valid HF token pattern. Rotate before doing anything else.

3. **Flip `c10vis-poem/Novus-Agenti` back to private when done compiling** — https://github.com/c10vis-poem/Novus-Agenti/settings → Danger Zone → Change visibility. It was made public for HF Jobs anonymous script fetch and is currently still public.

---

## Where we are

### Branches and PRs

| PR | State | Branch | What |
|---|---|---|---|
| #1 | merged | claude/epic-wozniak-7b0ban | Session 5 handoff, manifest, VL script fix |
| #2 | draft open | claude/compile-qwen3-5-9b | `scripts/compile_qwen3_5_9b.py` + Colab notebook. 3 commits: initial → multimodal-loader fix → qai_hub.configure removal |
| #3 | draft open | claude/migrate-horizons-app | Full Android app migration from NeuroOmni reference repo (53 .kt files, watchdog process, CI, signing keystore, gradle) |

### Sub-agents launched (background, output not retrieved at handoff)

Two agents were spawned earlier in the session. Their output files are in `tasks/`. Read or re-launch when resuming:

- **Startup-crash diagnostic** — ranked likely throw sites in `HorizonsApplication.kt` / `AppStateStore.kt` / Sherpa-ONNX JNI for the pre-UI crash, with file:line refs and defensive fix sketches.
- **genie_engine C++ scaffolding** — drafting `daemon/genie_engine/main.cpp` + `CMakeLists.txt` + `README.md` against the Qualcomm Genie SDK API. Will have `// VERIFY:` flags on unverified symbol names.

---

## LIVE STATE — HF Jobs compile in flight

Last attempt got past:
- dep install (`Installed 88 packages`)
- model identifier printed (`Qwen/Qwen3.5-9B → Snapdragon 8 Elite`)
- HF auth (no "Invalid token" error this time)

Failed on transient network drop (`httpx.ConnectError [Errno 104] Connection reset`) while streaming logs. The remote job may still be running — use `hf jobs ps` or `hf jobs logs <id>` to reconnect.

### Compile command template (paste fresh tokens, single line)

```
hf jobs uv run --flavor cpu-xl --timeout 2h --with torch --with transformers --with onnx --with onnxruntime --with qai-hub --with datasets --with numpy --with huggingface_hub --secrets HF_TOKEN --secrets QAI_HUB_API_TOKEN=NEW_QAI_TOKEN -e MODEL_ID=Qwen/Qwen3.5-9B -e MAX_SEQ_LEN=4096 -e PUBLISH_HF=1 -e OUTPUT_DIR=/tmp https://raw.githubusercontent.com/c10vis-poem/Novus-Agenti/claude/compile-qwen3-5-9b/scripts/compile_qwen3_5_9b.py
```

Notes:
- `--secrets HF_TOKEN` (no value) reads from local `hf auth login` config
- `--flavor cpu-xl` ($1/hr, 124GB RAM) — GPU not needed; compile runs server-side on QAI Hub
- Replace `NEW_QAI_TOKEN` with the freshly-rotated QAI token
- Novus-Agenti must be public when this runs (anonymous fetch)

### Failure modes already triaged this session (don't re-debug)

| Symptom | Root cause | Fix applied |
|---|---|---|
| `404: Not Found` parsed as Python | Private repo, anonymous fetch | Make Novus-Agenti public for the run |
| `Insufficient credits` on a100-large | $3/hr exceeds HF Pro monthly credit | Use `cpu-xl` ($1/hr) — GPU not needed |
| `Failed to spawn: <token>-e` | `-s KEY=VALUE` short flag mis-parsed by uv | Use long-form `--secrets KEY=VALUE` |
| `Invalid user token from HF_TOKEN` env var | Token paste artifact (doubled `hf_hf_` prefix) or revoked | Use single-prefix value or rotate |
| `AttributeError: module 'qai_hub' has no attribute 'configure'` | API removed in newer qai-hub | hasattr check, fall back to env var (already pushed) |
| `httpx.ConnectError [Errno 104]` | Phone network drop during log stream | Reconnect with `hf jobs logs <id>` — server job keeps running |

---

## After compile succeeds

1. Script publishes `.bin` to `Mer0vin8ian/qwen3-5-9b-npu-sm8750` (PUBLISH_HF=1 enabled in command)
2. Download to phone:
   ```
   hf download Mer0vin8ian/qwen3-5-9b-npu-sm8750 qwen3_5_9b_htp.bin --local-dir ~/Downloads
   adb push ~/Downloads/qwen3_5_9b_htp.bin /storage/emulated/0/Download/
   ```
3. **Next blocker:** `genie_engine` C++ daemon doesn't exist. The `.bin` cannot be served on-device without it. Sub-agent B was scaffolding this; pick up that report.

---

## Cost so far

Multiple failed runs at `cpu-xl` flavor. Each failed run costs ~1–2 min × $1/hr = ~$0.02–0.04. Total burn through this session → well under $1. A successful run is ~$0.75. Confirm HF Pro balance at https://huggingface.co/settings/billing before resuming.

---

## What I would do first when resuming

1. Rotate both tokens (security step)
2. `hf auth login` with new HF token in Termux
3. Make Novus-Agenti public temporarily
4. Run the compile command with fresh QAI token inline
5. Job runs ~30–40 min — use that time to read the two sub-agent outputs
6. Verify `.bin` appears at `Mer0vin8ian/qwen3-5-9b-npu-sm8750`
7. `adb push` to phone
8. Pick up genie_engine scaffolding from sub-agent output
9. Flip Novus-Agenti back to private

---

## Key URLs

- Compile PR: https://github.com/c10vis-poem/Novus-Agenti/pull/2
- App migration PR: https://github.com/c10vis-poem/Novus-Agenti/pull/3
- HF Jobs pricing: https://huggingface.co/docs/hub/jobs-pricing
- HF Pro billing: https://huggingface.co/settings/billing
- QAI Hub account: https://app.aihub.qualcomm.com/account/
- Target output repo (created on first successful publish): https://huggingface.co/Mer0vin8ian/qwen3-5-9b-npu-sm8750
