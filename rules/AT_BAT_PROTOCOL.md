# At-bat protocol — hard rules

An at-bat is one agent's turn at one milestone. End-to-end: claim → work →
commit → release. The rotation pattern keeps each at-bat clean (no
"polish own turd" loop) and parallelizable across disjoint deps.

1. **Read the pickup file first.** `CLAUDE.md`'s `## State of the Union`
   section — the single current-state source. There is no separate
   `SOTU.md` / `PROMPT_PREFIX.md` / `EXECUTION_BOARD.md` / per-session
   handoff file in this repo — those names are leftovers from an earlier
   project structure. If the SOTU date is older than the most recent
   commit date, flag it and ask before assuming it's current.
2. **Claim before touching code.** State what you're about to work on in
   your first message so a parallel agent doesn't duplicate the same
   milestone.
3. **One at-bat = one milestone = one fresh session.** Don't chain.
   The next milestone gets a fresh session so the agent isn't biased
   toward defending its prior choices.
4. **End-of-at-bat close-out (mandatory):**
   1. Tests / lint clean (or CI green — this repo has no local Android
      SDK, so CI is the real gate; see CLAUDE.md's `§Build / CI`).
   2. Update `CLAUDE.md`'s `## State of the Union` section in place for
      the next session — this is the only current-state doc, there is no
      separate handoff file to also write.
   3. If a blocker surfaced, append to `wiki/JOB_EXECUTION_LOG.md`'s
      strike/failure section (append-only ledger: date, milestone, what
      was tried, why it failed).
   4. Commit + push to the assigned feature branch — a local-only commit
      is invisible to the next session; it must reach the remote branch.
   5. `git status` clean.
5. **Working tree clean.** Untracked files either get committed or
   `.gitignore`'d before ending the session.
6. **No silent rule relaxation.** If a hard rule blocks you, raise it
   with the operator. Don't quietly bypass — the rule exists for a reason.
7. **Parallel at-bats are fine on disjoint deps.** State what you're
   claiming; if two agents pick the same milestone, the later one
   reroutes.
8. **Don't spawn sub-agents unless the task genuinely needs it.** See
   CLAUDE.md's `§Cache Prompting + Sub-Agent Rules` for when to spawn.

---

## Sub-agent orchestration — 2 repair attempts, then orchestrator, then gate

When the operator asks the orchestrator (foreground) to fan work out to
sub-agents: the sub-agent gets **2 repair attempts** after its initial
failure (strikes 1-2). If both fail, the orchestrator takes the at-bat
itself — no operator approval needed to make that swing, it just happens.
Only if the orchestrator's own attempt *also* fails (strike 3) does this
gate to a mandatory operator reconvene.

**A strike is a swing at a known failure, not the failure itself.** The
initial sub-agent launch is not a strike — if it succeeds, no strikes are
spent. A strike is only counted when a failure has been observed and
we're swinging at it.

### The loop

```
Sub-agent's initial attempt
       ↓ fails  (not a strike — just the discovered failure)
   [STRIKE 1]  SendMessage back to SAME sub-agent with concrete feedback.
               Polish in place — same context, no re-briefing tax.
               (Repair attempt 1 of 2.)
       ↓ fails
   [STRIKE 2]  SendMessage back AGAIN with sharper, more specific feedback.
               Same sub-agent. (Repair attempt 2 of 2 — the sub-agent's
               limit.)
       ↓ fails
   [STRIKE 3]  Orchestrator's swing — happens automatically, no operator
               approval needed first. Researches the failure (broken diff
               + wiki/JOB_EXECUTION_LOG.md + recent operator pushes), designs
               corrections, lands the change in-session, pushes. No more
               sub-agents on this milestone.
       ↓ fails
   [RECONVENE] NOW it gates. Orchestrator summarizes: every strike's
               attempt, what's still broken, suspected real blocker, AND
               a slate of alternative next-fix ideas — flags the
               operator. Operator approves/denies the next plan and may
               separately research outside resources in parallel.
       ↓ operator approves a plan
   [STRIKE 4]  Orchestrator's second swing — user-authorized at the
               reconvene.
       ↓ fails
   [Further escalation is per the operator's call, not autonomous.]
```

### Why this shape

- **Same sub-agent for strikes 1 and 2:** SendMessage continues the
  existing conversation context. A fresh sub-agent would re-read
  CLAUDE.md, re-read the affected files, re-parse the brief — expensive
  and rarely produces a better answer than the agent who already knows
  the territory. Capped at 2 so a sub-agent that's clearly not converging
  doesn't burn a third attempt on the same miss.
- **Orchestrator at strike 3, unrequested:** once the sub-agent's 2
  repair attempts are spent, the orchestrator moves on its own — the
  operator doesn't have to explicitly ask for this handoff each time.
- **Reconvene gates strike 4:** after the orchestrator's own swing
  (strike 3) fails, the next swing is NOT autonomous. Operator sees the
  full picture and decides — no further pushes without that sign-off.
- **Why a gate at all:** historical data — recent branches that ran 5-6
  sub-agent attempts in a row produced nothing usable. Past a certain
  point you're not fixing the bug, you're pattern-matching on noise.
  The gate forces a human read of the failure pattern before more
  compute is spent.

### Bookkeeping

- Track strike counts per milestone. When parallelizing, each milestone's
  strike count is independent.
- Every strike outcome gets an entry appended to `wiki/JOB_EXECUTION_LOG.md`
  per the append-only ledger format. The strike sequence + that log
  together are the audit trail.
- After strike 3, the orchestrator does NOT autonomously continue.
  Reconvene with the operator, surface the analysis, wait for the call.
