#!/usr/bin/env bash
# failures.sh — one-stop failure inspection for any CLI sandbox in the GitHub
# workbench. No arguments = show everything reachable from here.
#
#   tools/failures.sh            # ledger + latest CI outcome + latest report
#   tools/failures.sh ci         # latest build-apk conclusion only
#   tools/failures.sh report     # download the newest failure-report artifact
#   tools/failures.sh device     # adb-pull on-device failure report (needs adb)
#
# CI paths use `gh` (pre-authenticated in the workbench). Device path uses adb.
set -euo pipefail

REPO="${REPO:-c10vis-poem/Novus-Agenti}"
PKG="com.horizons"
cmd="${1:-all}"

have() { command -v "$1" >/dev/null 2>&1; }

ci() {
  have gh || { echo "gh not available"; return 1; }
  echo "== latest build-apk runs ($REPO) =="
  gh run list --repo "$REPO" --workflow build-apk.yml --limit 5 \
    --json headBranch,headSha,status,conclusion,createdAt \
    -q '.[] | "\(.createdAt)  \(.headBranch)  \(.status)/\(.conclusion // "-")  \(.headSha[0:7])"'
}

report() {
  have gh || { echo "gh not available"; return 1; }
  echo "== newest failure-report artifact =="
  local run_id
  run_id=$(gh run list --repo "$REPO" --workflow failure-monitor.yml --limit 1 \
    --json databaseId -q '.[0].databaseId')
  [ -n "$run_id" ] || { echo "no failure-monitor runs yet"; return 0; }
  local out; out="$(mktemp -d)"
  gh run download "$run_id" --repo "$REPO" --name failure-report --dir "$out" 2>/dev/null || {
    echo "no failure-report artifact on run $run_id"; return 0; }
  echo "downloaded to $out"
  [ -f "$out/summary.txt" ] && { echo "--- summary ---"; cat "$out/summary.txt"; }
  [ -f "$out/highlights.txt" ] && { echo "--- highlights ---"; cat "$out/highlights.txt"; }
}

device() {
  have adb || { echo "adb not available"; return 1; }
  local dst="./failures-device"
  adb pull "/sdcard/Android/data/$PKG/files/failures" "$dst" 2>/dev/null \
    && { echo "pulled to $dst"; cat "$dst/failures/REPORT.md" 2>/dev/null || true; } \
    || echo "no on-device failures dir (app may not have run yet)"
}

ledger() {
  [ -f FAILURES.md ] && { echo "== FAILURES.md (ledger) =="; head -40 FAILURES.md; }
}

case "$cmd" in
  ci)     ci ;;
  report) report ;;
  device) device ;;
  all)    ledger; echo; ci; echo; report ;;
  *)      echo "usage: $0 [ci|report|device|all]"; exit 1 ;;
esac
