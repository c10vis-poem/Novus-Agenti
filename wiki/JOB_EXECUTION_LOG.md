# Job Execution Log

Single combined archive for both compile-job results and failure/strike
outcomes — one ledger, not two. Append-only; do not delete or rewrite past
entries. If a fix later resolves an old entry, add a new entry noting the
resolution and referencing the original, rather than editing history.

This replaces the former separate `wiki/FAILURE_LOG.md` and the
`## Job Execution Log` table that used to live inline in `CLAUDE.md`.

## Format (for a new failure/strike entry)

```
### YYYY-MM-DD — <milestone/task> — Strike N
**Tried:** what was attempted
**Result:** what happened (error, wrong output, etc.)
**Suspected cause:** best guess, if any
**Next:** what the next strike/session should try instead
```

For a compile-job row, just append to the table below instead.

---

## Compile pipeline jobs (Qwen3.5-9B → ONNX → QAI Hub)

| Job | Error | Fix | Result |
|---|---|---|---|
| 1–4 | Various load/submodule errors | Iterative | Done |
| 5 | `has_previous_state on LinearAttention` | `use_cache=False` | Done |
| 6–7 | `cat(): got 5 and 4` M-RoPE shape | Two-pronged fix `2af893b` | Done |
| 8 | — (not yet run) | — | Ready — see `wiki/COMPILE-PIPELINE.md`. This whole pipeline is on hold pending a hard failure on the GGUF/GenieX-GGML path; do not trigger pre-emptively. |

## Strike/failure entries

(No entries yet.)
