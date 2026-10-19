# Smart Start — Playwright UI tests with test agents

TypeScript Playwright tests that use Playwright's AI test agents:

| Agent | What it does | Output |
|---|---|---|
| **planner** | Explores the running app in a browser and writes a test plan | `specs/<feature>.plan.md` |
| **generator** | Replays each plan scenario live in a browser and writes the test | `tests/<feature>/*.spec.ts` |
| **healer** | Runs the tests, debugs failures in the browser and patches them | edited specs |
| **requirements** (custom) | Writes a plan + tests from a written requirement for a feature that is **not built yet** — no browser | `specs/<feature>.plan.md`, `pages/`, `tests/<feature>/` tagged `@pending` |

Login is covered so far: `specs/login.plan.md` → 10 tests in `tests/auth/`.

## Setup

```bash
# 1. app running (from repo root)
docker compose up -d

# 2. this folder
cd playwright-agents
npm install
npx playwright install chromium
```

Credentials come from `ADMIN_USERNAME` / `ADMIN_PASSWORD` in the repo-root `.env`
(override with `TEST_USERNAME`, `TEST_PASSWORD`, `BASE_URL`).

## Running tests

```bash
npm test                 # headless
npm run test:headed      # watch the browser
npm run test:ui          # Playwright UI mode
npm run test:pending     # only @pending tests (features not built yet; skipped by npm test)
npm run report           # open the last HTML report
```

## Using the agents (Claude Code)

The agents live in `.claude/agents/` and talk to the browser through the `playwright-test` MCP server in `.mcp.json`.
**Start Claude Code from this folder** so it picks them up:

```bash
cd playwright-agents && claude
```

Approve the `playwright-test` MCP server when asked (check with `/mcp`; agents with `/agents`). Then, e.g.:

1. **Plan** — "Use the playwright-test-planner agent to create a test plan for the Students page (add, edit,
   search, deactivate). Seed: `tests/seed.spec.ts`. Save to `specs/students.plan.md`."
2. **Generate** — "Use the playwright-test-generator agent to generate tests for section 1 of `specs/students.plan.md`."
3. **Heal** — "Use the playwright-test-healer agent to run all tests and fix the failing ones."

### Feature not built yet (test-first from a requirement)

The planner and generator need a running page, so for an unbuilt feature use the custom
`playwright-test-requirements` agent instead:

1. Copy `requirements/_TEMPLATE.md` to `requirements/<feature>.md` and fill in the requirement.
2. "Use the playwright-test-requirements agent to write the plan and tests for `requirements/<feature>.md`."
   It writes `specs/<feature>.plan.md` (including a **UI Contract** of routes, `data-testid`s and messages, and
   **Open Questions**), a page object, and one test per scenario tagged `@pending`. `npm test` skips them.
3. Give the **UI Contract** to whoever builds the feature (or to Claude when building it) so the tests match it, and
   answer the Open Questions.
4. Once it's built: `npm run test:pending`, then "Use the playwright-test-healer agent to run the @pending tests and
   fix test problems. Report anything that looks like an app bug instead of changing the assertion."
5. When they pass, remove the `@pending` tags so they join the normal run.

Template prompts are in `.claude/prompts/`; `playwright-test-coverage.md` chains all three steps for one feature.

Seeds: `tests/seed.spec.ts` opens `/login`; `tests/seed-authenticated.spec.ts` signs in first. Use the second
for any page behind login.

## Test overlay (email, SMS, Ask assistant)

Tests that send email/SMS or use the Ask assistant must run with the backend in test mode, so nothing reaches real
parents. From the repo root:

```bash
docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d backend   # before
docker compose up -d backend                                                   # after
```

This routes email to MailHog (http://localhost:8025), SMS to `stubs/sms-sink` (http://localhost:8090/messages) and
the assistant to `stubs/fake-ollama`. Back up the database first if tests create data (see `../RUNNING.md`).

After upgrading `@playwright/test`, re-run `npx playwright init-agents --loop=claude` to refresh the agent
definitions (it overwrites Playwright's own agent and prompt files; the custom `playwright-test-requirements` files are kept).
