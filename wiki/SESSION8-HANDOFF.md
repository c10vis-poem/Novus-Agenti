# SESSION 8 HANDOFF — Novus Agenti

**Date:** 2026-06-27
**Branch:** `claude/project-scope-review-lf615p` → PR #4
**Session goal:** Content remodel (agents/, rules/, skills/) + dead weight removal + GPT-OSS §7 fix

---

## Resume Prompt

```
Project: Novus Agenti (Omni Claw). Mission: compile Mer0vin8ian/Qwen3.5-9B
→ Hexagon HTP v75 (SM8750) qnn_context_binary via QAI Hub.
Canonical repo: c10vis-poem/Novus-Agenti, branch claude/project-scope-review-lf615p.

READ BEFORE ANY ACTION:
  1. CLAUDE.md (full read, all sections)
  2. wiki/GPT-OSS-Reference.md
  3. wiki/SESSION8-HANDOFF.md (this file)
  4. scripts/compile_qwen3_5_9b.py
  5. models/manifest.yaml
```

---

## Token Policy — STATIC, DO NOT ROTATE MID-SESSION

Tokens are stored as GitHub Actions secrets / HF secrets. They are NEVER
committed to any file. Pass via `--secrets HF_TOKEN --secrets QAI_HUB_API_TOKEN`
in the HF Jobs trigger command. Rotation happens AFTER the build ships, not
between sessions.

---

## What Was Accomplished This Session

### Session 7 (prior session, completed):
- Stateless prefill fix: `HtpDecodeWrapper` with `use_cache=False`
- M-RoPE two-pronged fix: folded RoPE forward + `apply_rotary_pos_emb` module-level patch
- `--max_dynamic_tensor_size_mib 64` (restored from false 32 MiB assumption)
- Full CLAUDE.md rewrite pushed to working branch + main

### Session 8 (this session):
- Merged PR #3 (`claude/migrate-horizons-app`) into main — brought full Android app
  framework: horizons/, agents/, rules/, skills/, watchdog/, .github/, gradle, release/
- Closed PR #2 (`claude/compile-qwen3-5-9b`) — superseded
- Resolved CLAUDE.md merge conflict: pushed authoritative CLAUDE.md + compile script
  directly to main (commit `307fde5`)
- Content remodel committed to working branch:
  - `agents/build-runner.yaml` → rewritten as `novus-compile-runner`
  - `agents/neuralmash-builder.system.md` → rewritten for Novus-Agenti stack
  - `agents/sub-agent.system.md` → rewritten for Novus-Agenti stack
  - `rules/AAR_DECOMPILE.md` → repurposed as QNN artifact inspection guide
  - `skills/horizons-wiki/SKILL.md` → rewritten as `novus-agenti-wiki`
  - `skills/project-memory/SKILL.md` → rewritten for Novus-Agenti files
  - `wiki/GPT-OSS-Reference.md` → §7 corrected (32 MiB → 64 MiB canonical;
    removed false "User confirmed 32 MiB" note; added §9 job failure table)
  - `wiki/SESSION8-HANDOFF.md` → this file created
- Dead weight deleted from working branch: `wiki/EDGE-MODEL-LISTS.md`
- Dead weight deleted from main: `scripts/compile_qwen3_vl.py`

---

## Job 8 Trigger Command

```bash
hf jobs uv run --flavor cpu-xl --timeout 2h \
  --with torch --with transformers --with onnx --with onnxruntime --with onnxscript \
  --with qai-hub --with datasets --with numpy --with huggingface_hub --with accelerate \
  --secrets HF_TOKEN --secrets QAI_HUB_API_TOKEN \
  -e MODEL_ID=Mer0vin8ian/Qwen3.5-9B -e PUBLISH_HF=1 -e OUTPUT_DIR=/tmp \
  https://raw.githubusercontent.com/c10vis-poem/Novus-Agenti/claude/project-scope-review-lf615p/scripts/compile_qwen3_5_9b.py
```

`SKIP_VISION` is NOT set — all three artifacts attempted.

---

## Architecture Decisions (locked — do not re-litigate)

| Decision | Value |
|---|---|
| Model | Qwen3.5-9B (`Mer0vin8ian/Qwen3.5-9B`) |
| Runtime | `qnn_context_binary`, Hexagon HTP v75 (SM8750) |
| Serving daemon | `ort_engine` (ONNX Runtime + QNN-EP, aarch64-android) |
| API | HTTP `http://127.0.0.1:8080/api/v1/generate` |
| NOT used | Genie SDK, LiteRT, Nexa SDK, TFLite |
| Quantization | W4A16 (`--quantize_full_type w4a16 --quantize_weight_bits 4`) |
| Scratch size | 16 MiB (`--scratch_size_mib 16`) |
| Dynamic tensor | 64 MiB (`--max_dynamic_tensor_size_mib 64`) — canonical, unverified 32 hypothesis |
| KV cache | Stateless prefill (`use_cache=False`) |
| Watchdog | `CliffordService` == CLIFFORD == Watchdog (same FGS, same service) |
| NPU lock | `NpuManager.acquirePerformanceLock(PERF_MODE_HIGH)` |
| Game SDK | `GameManager.setGameMode(GameMode.PERFORMANCE)` in `HorizonsApplication` |
| Vision | Deepstack injection at runtime — NOT a compile-time artifact |
| Serving path | ORT + QNN-EP confirmed (not Genie SDK) |

---

## Pending Tasks (priority order)

1. **Job 8** — user triggers the command above. This is the critical path.
2. **PR #4 merge** — merge working branch to main once content remodel is reviewed.
3. **NpuManager lock** — wire `acquirePerformanceLock(PERF_MODE_HIGH)` into
   `horizons/src/main/java/com/horizons/watchdog/CliffordService.kt`
4. **GameManager** — wire `setGameMode(GameMode.PERFORMANCE)` into
   `horizons/src/main/java/com/horizons/HorizonsApplication.kt`
5. **Manifest** — add `<uses-feature android:name="android.hardware.game"/>` +
   `HIGH_PERFORMANCE` permission to `horizons/src/main/AndroidManifest.xml`
6. **`watchdog/` module** — review `watchdog/src/main/kotlin/com/horizons/` and
   fold into `horizons/` CliffordService or delete the whole module.
7. **`ort_engine` C++ daemon** — scaffold aarch64-android binary
   (ONNX Runtime + QNN EP serving HTTP at 127.0.0.1:8080).
8. **`build-apk.yml`** — fix CI publish target from NeuroOmni to
   `${{ github.repository }}`.
9. **Termux log-capture CLI** — autonomous Termux monitoring for HF Jobs output.

---

## Repo State After This Session

```
c10vis-poem/Novus-Agenti
├── main (HEAD: ~307fde5)
│   ├── CLAUDE.md                          ← authoritative, single-path spec
│   ├── scripts/compile_qwen3_5_9b.py      ← 64 MiB, M-RoPE fixed, stateless prefill
│   ├── horizons/                          ← Android app from PR #3
│   ├── agents/                            ← OLD content on main; remodeled in PR #4
│   ├── rules/                             ← mostly generic; AAR_DECOMPILE remodeled in PR #4
│   ├── skills/                            ← OLD content on main; remodeled in PR #4
│   ├── watchdog/                          ← Kotlin source, needs review (task #6 above)
│   └── wiki/SESSION5,6-HANDOFF.md
│
└── claude/project-scope-review-lf615p (PR #4)
    ├── agents/ (build-runner.yaml, neuralmash-builder.system.md, sub-agent.system.md)
    │   → all rewritten for Novus-Agenti stack
    ├── rules/AAR_DECOMPILE.md             ← repurposed as QNN artifact inspection
    ├── skills/ (horizons-wiki/SKILL.md, project-memory/SKILL.md)
    │   → rewritten for Novus-Agenti
    ├── wiki/GPT-OSS-Reference.md          ← §7 corrected (64 MiB), §9 added (job failures)
    └── wiki/SESSION8-HANDOFF.md           ← this file
```

---

## M-RoPE Fix Reference

Prong 1 — `make_folded_rope_forward`: Returns `[B, S, D]` (3D) so
`apply_rotary_pos_emb`'s `unsqueeze(1)` produces `[B, 1, S, D]` — broadcasts
cleanly with `[B, H, S, D]` q/k. M-RoPE's `[3,B,S,D]` would unsqueeze to 5D
and break cat with 4D `q_pass`.

Prong 2 — module-level patch of `apply_rotary_pos_emb`: Patches
`transformers.models.qwen3_5.modeling_qwen3_5.apply_rotary_pos_emb` to use
precomputed FP16 cos/sin tables and return `[B, H, S, D]` shapes throughout.

See `scripts/compile_qwen3_5_9b.py` for full implementation.
