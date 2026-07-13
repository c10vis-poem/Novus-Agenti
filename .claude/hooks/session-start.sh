#!/bin/bash
set -euo pipefail

# Session warm-up rules (session 16) — printed on every session start so they
# reach Claude's context before any work begins. Same two rules also live in
# CLAUDE.md's "Cache Prompting + Sub-Agent Rules" section and must be copied
# into every sub-agent brief this session spawns (see that section's template).
echo "WARM-UP: 1h prompt-cache TTL — pace work to it, don't schedule busywork just to keep it warm." >&2
echo "WARM-UP: lazy tool loading — do not preload tool schemas at session start; call ToolSearch for a tool only right before you use it, and again for any tool a spawned sub-agent needs." >&2

latest_handoff="$(ls "$CLAUDE_PROJECT_DIR"/wiki/SESSION*-HANDOFF.md 2>/dev/null | sort -V | tail -1)"
echo "WARM-UP: read CLAUDE.md IN FULL, then ${latest_handoff:-the latest wiki/SESSION*-HANDOFF.md} — before resuming or taking any action, not after." >&2

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
