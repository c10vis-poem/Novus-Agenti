#!/bin/bash
set -euo pipefail

# Fires on Stop — reminder only, does not push anything itself. Per CLAUDE.md's
# "Order of Operations" step 4 (Document): before actually ending, the State
# of the Union in CLAUDE.md should be current and a wiki/SESSION{N+1}-HANDOFF.md
# should exist for real work done this session. This does not force a git push;
# that stays a decision made in-conversation (see CLAUDE.md Hard Rules).
echo "REMINDER: if this session changed real state, update CLAUDE.md's State of the Union and write the next wiki/SESSION{N+1}-HANDOFF.md before treating the work as done — then push/PR per the usual flow." >&2
exit 0
