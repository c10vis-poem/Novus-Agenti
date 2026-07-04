#!/bin/bash
set -euo pipefail

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
