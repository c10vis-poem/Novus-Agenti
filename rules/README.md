# Rules — hard contract

Rules are the contract. **Rules > wiki guidance** when in conflict.

| Rule file | Scope |
|---|---|
| `CACHE_PROMPT_RULES.md` | Anthropic prompt caching (TTL, breakpoint budget, edit cadence) |
| `GIT_HYGIENE.md` | Branch policy, commit safety, branch preservation |
| `AT_BAT_PROTOCOL.md` | How an agent claims, works, and hands off a milestone |
| `AAR_DECOMPILE.md` | **ARCHIVED** — Nexa-specific AAR decompile procedure, not applicable to the current QNN/Hexagon HTP stack. Kept for the reusable javap technique only. Not an active rule. |

## Precedence

1. Hard rules in this folder.
2. Pickup-file directives: CLAUDE.md's `## State of the Union` section,
   plus the latest `wiki/SESSION{N}-HANDOFF.md`. There is no separate
   `SOTU.md` / `PROMPT_PREFIX.md` / `EXECUTION_BOARD.md` in this repo —
   those names are leftovers from an earlier project structure.
3. Wiki guidance (`wiki/*`, including `wiki/GPT-DAEMON-REFERENCE.md` and
   `wiki/NPU-RUNTIME-PATHS.md`). There is no separate `CLAUDE_AT_HORIZONS.md`
   in this repo.
4. Inline comments, ad-hoc convention.

If a rule needs to change, the change goes through the operator. Don't
quietly relax a rule mid-session.
