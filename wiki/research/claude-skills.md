# claude-skills — Reference

Source: `c10vis-poem/claude-skills` (fork of `alirezarezvani/claude-skills`), README.md, cloned and read directly from GitHub. Category: **skills library / agent plugin marketplace**.

## What it is

"246 production-ready Claude Code skills, plugins, and agent skills for 12 AI coding tools" — the largest single skills library in the user's fork list (5,200+ GitHub stars upstream). Works natively as Claude Code plugins, Codex agent skills, Gemini CLI skills, and converts to 8 more tools (Cursor, Aider, Kilo Code, Windsurf, OpenCode, Augment, Antigravity, Hermes Agent) via `scripts/convert.sh`.

Each skill = a folder with `SKILL.md` (frontmatter + instructions) + optional `scripts/` (Python, stdlib-only, no pip installs — 305 CLI tools total) + `references/` + `assets/`.

## Skills vs Agents vs Personas (the framework's own taxonomy)

| | Skills | Agents | Personas |
|---|---|---|---|
| Purpose | How to execute a task | What task to do | Who is thinking |
| Scope | Single domain | Single domain | Cross-domain |
| Voice | Neutral | Professional | Personality-driven |

## Domain breakdown (246 skills across 9 domains)

| Domain | Count | Highlights |
|---|---|---|
| Engineering — Core | 32 | Architecture, frontend, backend, fullstack, QA, DevOps, SecOps, AI/ML, data, Playwright, self-improving agent, security suite (6), a11y audit |
| Playwright Pro | 9+3 | Test generation, flaky fix, Cypress/Selenium migration, TestRail, BrowserStack, 55 templates |
| Self-Improving Agent | 5+2 | Auto-memory curation, pattern promotion, skill extraction, memory health |
| Engineering — POWERFUL | 40 | Agent designer, RAG architect, database designer, CI/CD builder, security auditor, MCP builder, AgentHub, Helm charts, Terraform, self-eval, **llm-wiki**, tc-tracker, reliability portfolio (feature-flags-architect, kubernetes-operator, chaos-engineering, slo-architect), ship-gate |
| Product | 13 | PM, agile PO, strategist, UX researcher, UI design, landing pages, SaaS scaffolder, analytics, experiment designer, discovery, roadmap communicator, code-to-prd, apple-hig-expert |
| Marketing | 44 | 7 pods: Content(8)/SEO(5)/CRO(6)/Channels(6)/Growth(4)/Intelligence(4)/Sales(2) + orchestration router, 32 Python tools |
| Project Management | 9 | Senior PM, scrum master, Jira, Confluence, Atlassian admin, bundled Atlassian Remote MCP |
| Regulatory & QM | 14 | ISO 13485, MDR 2017/745, FDA, ISO 27001, GDPR, SOC 2, CAPA, risk management |
| C-Level Advisory | 28 | Full C-suite (10 roles) + orchestration + board meetings + culture/collaboration |
| Business & Growth | 5 | Customer success, sales engineer, revenue ops, contracts & proposals, BizDev toolkit |
| Finance | 3 | Financial analyst (DCF/budgeting/forecasting), SaaS metrics coach, business investment advisor |

**Note**: there's a `llm-wiki` skill in the POWERFUL engineering tier — same concept name as the "LLM Wiki" research doc (Karpathy 3-layer framework) that kicked off this entire Claude_master_wiki project. <!-- UNVERIFIED: not confirmed whether this skill implements the same methodology or is coincidentally named — worth a direct diff if the wiki-building pipeline is ever automated. -->

## POWERFUL tier (25 advanced skills, deep production-grade capability)

agent-designer, agent-workflow-designer, rag-architect, database-designer, database-schema-designer, migration-architect, **skill-security-auditor** (scans skills for malicious code before install — command injection, code execution, data exfiltration, prompt injection, supply-chain risk, privilege escalation; PASS/WARN/FAIL), ci-cd-pipeline-builder, mcp-server-builder (OpenAPI→MCP scaffold), pr-review-expert, api-design-reviewer, api-test-suite-builder, dependency-auditor, release-manager, observability-designer, performance-profiler, monorepo-navigator, changelog-generator, codebase-onboarding, runbook-generator, git-worktree-manager, env-secrets-manager, incident-commander, tech-debt-tracker, interview-system-designer.

## Personas

Pre-configured agent identities with curated skill loadouts + distinct communication styles: **Startup CTO** (engineering+strategy — architecture decisions, tech stack, technical due diligence), **Growth Marketer** (content-led growth, launch strategy, channel optimization), **Solo Founder** (cross-domain, one-person startups, MVP building).

## Orchestration (4 patterns for combining personas/skills/agents)

- **Solo Sprint** — switch personas across project phases (side projects, MVPs)
- **Domain Deep-Dive** — one persona + multiple stacked skills (architecture reviews, compliance audits)
- **Multi-Agent Handoff** — personas review each other's output (high-stakes decisions)
- **Skill Chain** — sequential skills, no persona needed (content pipelines, repeatable checklists)

## Install patterns

Claude Code: `/plugin marketplace add alirezarezvani/claude-skills` then `/plugin install <domain>-skills@claude-code-skills` (per-domain bundles) or individual skills like `skill-security-auditor@claude-code-skills`. Also supports Gemini CLI (`./scripts/gemini-install.sh`), OpenAI Codex (`npx agent-skills-cli add ... --agent codex`), OpenClaw (curl installer), and manual copy to `~/.claude/skills/`.

## Relevance to this project

This is the single largest ready-made skills catalog available to fork into Omni Claw/Novus Agenti's skill system. Two entries matter most for this project specifically: **skill-security-auditor** (directly relevant if Omni Claw ever lets a background agent install skills autonomously — provides a PASS/WARN/FAIL gate before trusting a skill folder) and **mcp-server-builder** (OpenAPI→MCP scaffold — could accelerate wiring OB1's remote MCP or new Omni Claw tool endpoints). The 9-domain non-engineering catalog (marketing, finance, C-level, regulatory) is lower priority for an on-device coding assistant but available if the assistant's scope ever broadens beyond dev work.
