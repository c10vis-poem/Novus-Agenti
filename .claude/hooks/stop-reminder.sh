#!/bin/bash
set -euo pipefail

# Fires on Stop — reminder only, does not push anything itself. Per CLAUDE.md's
# "Order of Operations" step 4 (Document): before actually ending, the State
# of the Union in CLAUDE.md should be current and a wiki/SESSION{N+1}-HANDOFF.md
# should exist for real work done this session. This does not force a git push;
# that stays a decision made in-conversation (see CLAUDE.md Hard Rules).
#
# The critical failure mode this guards against: updating CLAUDE.md's SOTU
# and/or writing a handoff LOCALLY and then ending the session without
# committing + pushing. The next session reads CLAUDE.md from the remote
# branch (git clone/fetch), not from this container's filesystem — a
# local-only edit is invisible to it and the whole point of the SOTU
# (letting the next session pick up where this one left off) is defeated.
# Committing is not enough either; it must be PUSHED.
if ! git -C "$CLAUDE_PROJECT_DIR" diff --quiet 2>/dev/null || ! git -C "$CLAUDE_PROJECT_DIR" diff --cached --quiet 2>/dev/null; then
  echo "REMINDER: uncommitted changes exist. If this session changed real state, update CLAUDE.md's State of the Union and write the next wiki/SESSION{N+1}-HANDOFF.md, THEN commit AND PUSH to the remote branch — a local-only commit is invisible to the next session." >&2
else
  local_head="$(git -C "$CLAUDE_PROJECT_DIR" rev-parse HEAD 2>/dev/null || echo "")"
  remote_head="$(git -C "$CLAUDE_PROJECT_DIR" rev-parse '@{u}' 2>/dev/null || echo "")"
  if [ -n "$local_head" ] && [ -n "$remote_head" ] && [ "$local_head" != "$remote_head" ]; then
    echo "REMINDER: local branch is ahead of/diverged from its remote (unpushed commits). PUSH now — the next session only sees what's on the remote branch, including CLAUDE.md's SOTU." >&2
  else
    echo "REMINDER: if this session changed real state, confirm CLAUDE.md's State of the Union and the latest wiki/SESSION*-HANDOFF.md are both updated AND pushed — the next session only ever sees the remote branch." >&2
  fi
fi
exit 0
