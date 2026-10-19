# Smart Start UI tests (Playwright + test agents)

Playwright tests for the Smart Start Student Portal, written with Playwright's planner / generator / healer agents
(`.claude/agents/`). The app must already be running (`docker compose up -d` in the repo root); tests target
`BASE_URL` (default http://localhost:5173).

## Layout
- `specs/*.plan.md` — test plans written by the planner
- `tests/<feature>/*.spec.ts` — one scenario per file, written by the generator
- `tests/seed.spec.ts` — seed test the agents run first (opens `/login`); `tests/seed-authenticated.spec.ts` signs in first
- `requirements/*.md` — written requirements for features not built yet (input for `playwright-test-requirements`)
- `stubs/` — fake SMS gateway and fake Ollama used by `../docker-compose.e2e.yml`
- `pages/` — page objects (reuse and extend them; don't duplicate locators in tests)
- `support/env.ts` — credentials (`env.username` / `env.password`, read from `../.env` ADMIN_*) and `throwawayUsername()`

## Rules for agents
- **Never fail a login with the real admin username.** 5 failures lock a username for 15 minutes. Every negative
  login test must use `throwawayUsername()`.
- Never hard-code the admin password; import `env` from `support/env.ts`.
- Prefer `getByTestId(...)` (the app uses `data-testid` everywhere), then roles/labels. No CSS/XPath, no `waitForTimeout`.
- Tests must be independent and runnable in parallel. Don't delete or edit data you didn't create — the database holds
  the centre's real students.
- `@pending` tag = test written before the feature exists; skipped by `npm test`, run with `npm run test:pending`.
  When healing a `@pending` test, fix selectors/timing to match the built feature but never weaken an assertion that
  encodes the requirement — report the mismatch instead.
- Run `npx playwright test` and `npx tsc --noEmit` after changes.
