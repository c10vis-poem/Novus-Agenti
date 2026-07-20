#!/bin/bash
set -euo pipefail

# Session warm-up rules (session 16) — printed on every session start so they
# reach Claude's context before any work begins. Same rules also live in
# CLAUDE.md's "Cache Prompting + Sub-Agent Rules" section and must be copied
# into every sub-agent brief this session spawns (see that section's template).
echo "WARM-UP: read the MANDATORY FIRST-READ CANON IN FULL before ANY action — do not read the top of CLAUDE.md and start guessing. Ingest, then state it back:" >&2
echo "  1. CLAUDE.md (full, incl. State of the Union)" >&2
echo "  2. knowledge/omni-claw-defined/ — what the app IS + how it works, INCLUDING workbench/00-TILE-HUB-ARCHITECTURE.md and workbench/*.md (the fuse-box / seven-tile → center-hub Router definition)" >&2
echo "  3. knowledge/daemon-reference/GPT-DAEMON-REFERENCE.md + NPU-RUNTIME-PATHS.md" >&2
echo "  4. compile/manifest.yaml" >&2
echo "  5. EXECUTIONS.md — the build dock (prioritized, code-anchored work list: go here, do this)" >&2
echo "WARM-UP: JSONL is GREP/RETRIEVAL ONLY — never a first-read. The .md files above are the first read. Do NOT assume the existing RuntimeDefStore/RouterPane/CliffordService code implements canon — it contradicts it (see the build dock). Build to the canon, not the old code." >&2
echo "VISUAL: wiki/HOME-REDESIGN-SPEC.md + wiki/home-redesign-img/ is the home-dock visual canon — open ONLY for V-track work, not first-read. Every visual change ends with a real on-device screenshot vs. the reference (operator is the on-device check; cloud has no Android SDK)." >&2
echo "HYGIENE: before/after every push, check your own changes for dangling references — not a full repo audit. If you find anything stale, redundant, or contradictory at any point, STOP and flag it to the operator before taking further action." >&2

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

missing=()
[ -z "${HF_TOKEN:-}" ] && missing+=("HF_TOKEN")
[ -z "${QAI_HUB_API_TOKEN:-}" ] && missing+=("QAI_HUB_API_TOKEN")

if [ ${#missing[@]} -gt 0 ]; then
  echo "WARNING: missing env vars: ${missing[*]} — add them in this environment's settings (web UI -> environment -> Environment variables)." >&2
else
  echo "HF_TOKEN and QAI_HUB_API_TOKEN present, ready." >&2
fi

if curl -sS -o /dev/null -w "%{http_code}" --max-time 5 "https://huggingface.co" 2>/dev/null | grep -q "^200$\|^30"; then
  echo "huggingface.co reachable from this session." >&2
else
  echo "NOTE: huggingface.co not reachable from this session (proxy egress policy). HF Jobs must be triggered elsewhere (Termux, or a differently-configured environment)." >&2
fi

exit 0
