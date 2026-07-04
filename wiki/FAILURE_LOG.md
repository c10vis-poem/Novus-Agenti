# Failure Log

Append-only ledger. One entry per strike outcome per
`rules/AT_BAT_PROTOCOL.md`'s 2-strike rule — every failed sub-agent or
orchestrator attempt gets logged here before escalating, so the next
attempt (or the next session) isn't repeating a diagnosis from scratch.

Do not delete or rewrite past entries — append only. If a fix later
resolves an old entry, add a new entry noting the resolution and
referencing the original by date, rather than editing history.

## Format

```
### YYYY-MM-DD — <milestone/task> — Strike N
**Tried:** what was attempted
**Result:** what happened (error, wrong output, etc.)
**Suspected cause:** best guess, if any
**Next:** what the next strike/session should try instead
```

---

(No entries yet.)
