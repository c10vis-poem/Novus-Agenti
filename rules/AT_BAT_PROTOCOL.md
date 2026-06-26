# At-bat protocol — hard rules

An at-bat is one agent's turn at one milestone. End-to-end: claim → work →
commit → release. The rotation pattern keeps each at-bat clean (no
"polish own turd" loop) and parallelizable across disjoint deps.

1. **Read the three pickup files first.** `SOTU.md` → `PROMPT_PREFIX.md`
   → `EXECUTION_BOARD.md`. If SOTU is older than the most recent commit
   date, flag it and ask before assuming it's current.
2. **Claim before touching code.** Edit the dashboard row in
   `EXECUTION_BOARD.md` (`AVAILABLE → CLAIMED`), commit the 1-line diff,
   then start work.
3. **One at-bat = one milestone = one fresh session.** Don't chain.
   The next milestone gets a fresh session so the agent isn't biased
   toward defending its prior choices.
4. **End-of-at-bat close-out (mandatory):**
   1. Tests / lint clean.
   2. Commit + push to the assigned feature branch.
   3. Update the dashboard row (`IN_PROGRESS → DONE` or `→ BLOCKED`).
   4. Draft `SOTU.md` for the next session.
   5. If a blocker surfaced, append to `wiki/FAILURE_LOG.md`.
   6. `git status` clean.
5. **Working tree clean.** Stop-hook enforces. Untracked files either get
   committed or `.gitignore`'d.
6. **No silent rule relaxation.** If a hard rule blocks you, raise it
   with the operator. Don't quietly bypass — the rule exists for a reason.
7. **Parallel at-bats are fine on disjoint deps.** Check the dashboard
   before claiming; if two agents pick the same milestone, the later
   one reroutes.
8. **Don't spawn sub-agents unless the operator asks.** The current
   model is operator-orchestrated.

---

## Sub-agent orchestration — the 3-strike rule

When the operator asks the orchestrator (Opus, foreground) to fan work
out to sub-agents, each milestone follows a 3-strike escalation: two
sub-agent self-polish attempts, then the orchestrator takes the at-bat,
then operator reconvene.

**A strike is a swing at a known failure, not the failure itself.** The
initial sub-agent launch is not a strike — if it succeeds, no strikes
are spent. A strike is only counted when a failure has been observed
and we're swinging at it.

### The loop

```
Sub-agent's initial attempt
       ↓ fails  (not a strike — just the discovered failure)
   [STRIKE 1]  SendMessage back to SAME sub-agent with concrete feedback.
               Polish in place — same context, no re-briefing tax.
       ↓ fails
   [STRIKE 2]  SendMessage back AGAIN with sharper, more specific feedback.
               Same sub-agent. One more polish attempt.
       ↓ fails
   [STRIKE 3]  Orchestrator's swing. Researches the failure (broken diff +
               FAILURE_LOG + recent operator pushes), designs corrections,
               lands the change in-session, pushes. No more sub-agents on
               this milestone.
       ↓ fails
   [RECONVENE] Orchestrator surfaces analysis: every strike's attempt,
               what's still broken, suspected real blocker, AND a slate
               of alternative next-fix ideas (not just one) — operator
               picks. Operator researches their own angles in parallel.
               When operator picks an option:
   [STRIKE 4]  Orchestrator's second swing — user-authorized at the
               reconvene.
       ↓ fails
   [Further escalation is per the operator's call, not autonomous.]
```

### Why this shape

- **Same sub-agent for strikes 1 and 2:** SendMessage continues the
  existing conversation context. A fresh sub-agent would re-read
  RULES.md, re-read the affected files, re-parse the brief — expensive
  and rarely produces a better answer than the agent who already knows
  the territory.
- **Orchestrator at strike 3, not strike 2:** the operator's time is the
  scarce resource. Two cheap polish loops first; the orchestrator spends
  real attention only after the sub-agent has clearly run out of options.
- **Reconvene gates strike 4:** after the orchestrator's first swing
  (strike 3) fails, the next swing is NOT autonomous. Orchestrator
  surfaces a slate of alternative next-fix ideas (not just one); operator
  picks (or redirects) before strike 4 lands. Operator can research a
  parallel angle while strike 4 runs.
- **Why a gate at all:** historical data — recent branches that ran 5-6
  sub-agent attempts in a row produced nothing usable. Past a certain
  point you're not fixing the bug, you're pattern-matching on noise.
  The gate forces a human read of the failure pattern before more
  compute is spent.

### Bookkeeping

- Track strike counts per milestone. When parallelizing, each milestone's
  strike count is independent.
- Every strike outcome gets an entry appended to `wiki/FAILURE_LOG.md`
  per the existing append-only ledger format. The 3-strike rule + the
  failure log together are the audit trail.
- After strike 3, the orchestrator does NOT autonomously continue.
  Reconvene with the operator, surface the analysis, wait for the call.
