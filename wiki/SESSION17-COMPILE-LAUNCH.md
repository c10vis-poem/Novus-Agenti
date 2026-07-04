# SESSION 17 LAUNCH PROMPT — COMPILE TRACK ONLY. READ BEFORE ANY ACTION.

> Task split (operator, 2026-07-04): **this session owns the Qwen3.5
> compile job. The app build is a separate session** (see
> `wiki/SESSION17-LAUNCH.md`). Do not touch app code, PR #13, or the
> emulator work. Compile-track branch lineage: PR #4
> (`claude/project-scope-review-lf615p`), PR #10 (`--quantize_io` fix),
> PR #11 (v79 fix + RESEARCH-CROSSREF).

## OPERATOR DECISIONS — the mission, not up for relitigation
- **We ARE compiling Qwen3.5.** Model family is fixed. Downsize option if
  ever needed is another Qwen3.5 variant (2B/4B exist in operator's
  inventory), never an older generation.
- **Two sanctioned routes, both approved:**
  1. **BYOM via Qualcomm AI Hub Workbench** (workbench.aihub.qualcomm.com)
     — QNN context binaries for Hexagon **v79** (SM8750, 8 Elite).
  2. **llama-quantize / GGUF route** — Q4_0 / IQ4_NL (+Q8_0) for the
     ggml-hexagon NPU path; K-quants (Q4_K_M) are CPU-only on Hexagon.
     `Mer0vin8ian/Qwen3.5-9B-VLM-Q4_K_M-GGUF` already exists (CPU tier);
     an NPU-quant GGUF (Q4_0/IQ4_NL from the F16 source) is wanted.
- 9B @ ~4.5GB INT4 weights / ~5.5-5.8GB live is inside the envelope and
  fine per operator. The 4-chunk decoder split already in
  `scripts/compile_qwen3_5_9b.py` is the right shape for multi-context.

## STATE OF JOB 8 (verified 2026-07-04, hf jobs logs)
- Job `6a488c52d235f6e43ae5a5a3` (attempt 8): **ONNX export fully
  succeeded** — the grid_thw fix + commit-SHA-pinned raw URL work.
  Vision 793MB, projection 76MB, 4 decoder chunks all exported.
- **Failure: QAI Hub vision-encoder compile `jpeler3og` — exit code 14,
  "Conversion to context binary failed"** — even with `--quantize_io`.
  Get the actual QNN log: `scripts/fetch_qai_logs.py` on branch
  `claude/proxy-hf-auth-status-3pcwbu` (run as an HF Job; this container
  blocks app.aihub.qualcomm.com, HF egress was OPEN in the 07-04
  container — verify fresh with `curl -sS "$HTTPS_PROXY/__agentproxy/status"`).
  Log hint printed by the job: `SKIP_EXPORT=1 JOB_ID_*=jpeler3og`.
- Secondary bug: calibration dataset URI invalid
  (`hf://datasets/wikitext@…` → "Repository id must be 'namespace/name'")
  → silent fallback to synthetic random tokens. Fix the URI (e.g.
  `wikitext-2-raw-v1` via `datasets.load_dataset("Salesforce/wikitext",
  "wikitext-2-raw-v1")`) — synthetic-token calibration degrades W4A16.

## KNOWN TECHNICAL CONSTRAINTS (all first-source, see RESEARCH-CROSSREF)
- SM8750 = Hexagon **v79** (4 independent sources). PR #11 fixed docs.
- Hexagon cDSP = 32-bit VA (4GB) per session; single-session caps ~4B.
  QNN's answer is **multiple NPU sessions / multi-part linked context
  binaries** — Qualcomm ships llama_v3_1_8b that way. If keeping
  `QNN_GRAPH_CONFIG_MAX_CONTEXTS=1`, that contradicts a 5.5GB 9B; the
  4-chunk split wants per-chunk contexts + linked shared weights.
- Official pipeline reference: `ai-hub-models/tutorials/llm/onboarding.md`
  (cloned at /home/user/ai-hub-models): AR-128 + AR-1 dual graphs,
  transposed KV cache I/O, LinkJobs → shared-weight context binaries,
  Genie runtime. Current script exports `use_cache=False` (no KV cache)
  — fine for a first compile artifact, but decode perf will need the
  AR-1/KV-cache shape eventually.
- qwen3_5 arch = hybrid with **linear-attention (GDN) layers** — this is
  confirmed by Job 5's own error (`has_previous_state on LinearAttention`).
  These layers are the most likely exit-14 culprits after Reshape I/O.
  Also `deepstack` vision injection — no vendor recipe exists anywhere
  for this arch on HTP; expect op-support gaps in the vision encoder.
  Consider `SKIP_VISION=1` to land the language artifacts first while
  debugging the vision compile separately (operator authorized
  text-first iterations implicitly by splitting tracks — confirm before
  changing the default trigger though; CLAUDE.md hard rule says don't
  set SKIP_VISION in the DEFAULT command).
- GGUF route arch support: qwen3_5 is new — check upstream llama.cpp
  `convert_hf_to_gguf.py` supports it before quantizing; the June-30
  llama-server binaries on `latest-debug` may predate the arch.

## TOOLING FACTS
- `hf` CLI works when the container has HF egress (verify fresh, per
  session). HF_TOKEN + QAI_HUB_API_TOKEN pre-exported from env config.
- Job trigger: pin raw.githubusercontent URLs to the COMMIT SHA (branch
  URLs cache stale through an internal redirect — session 15 lesson).
- GitHub App token lacks actions:write → trigger CI by push.
- Workbench BYOM is browser-side for the operator; API-side is qai-hub
  SDK (`submit_compile_job`, `submit_link_job` — see
  /home/user/ai-hub-models `.claude/` docs and the local qai_hub client).

## FIRST ACTIONS
1. Fetch jpeler3og's real QNN failure log (HF Job route above).
2. Diagnose exit-14 against the constraints list; decide vision-encoder
   strategy (op fixes vs SKIP_VISION-first iteration — ask operator).
3. Fix the calibration dataset URI in the same pass as the next trigger.
4. In parallel: verify qwen3_5 support in upstream llama.cpp; if present,
   produce the Q4_0/IQ4_NL GGUF from the F16 source via llama-quantize
   (HF Job or CI), publish to HF + latest-debug for the app track.
