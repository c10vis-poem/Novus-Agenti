#!/bin/bash
set -euo pipefail

# Session warm-up rules (session 16) — printed on every session start so they
# reach Claude's context before any work begins. Same rules also live in
# CLAUDE.md's "Cache Prompting + Sub-Agent Rules" section and must be copied
# into every sub-agent brief this session spawns (see that section's template).
echo "WARM-UP: this initial prompt is where the cacheable prefix (context + tools) gets established for the 1h TTL — attach the tools this task needs now, don't fragment the cache by resolving new ones mid-session for anticipated work." >&2
echo "WARM-UP: read CLAUDE.md IN FULL (including its current State of the Union — there is no separate handoff file, it's the single current-state doc) before resuming or taking any action, not after." >&2

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
