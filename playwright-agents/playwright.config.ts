import { defineConfig, devices } from '@playwright/test'
import { env } from './support/env'

/**
 * Runs against the app started with `docker compose up` from the repo root
 * (frontend on :5173 proxies /api to the backend). Override the target with BASE_URL.
 */
export default defineConfig({
  testDir: './tests',
  // @pending = tests written from a requirement for a feature that is not built yet (npm run test:pending)
  grepInvert: process.env.PENDING ? undefined : /@pending/,
  grep: process.env.PENDING ? /@pending/ : undefined,
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  timeout: 30_000,
  expect: { timeout: 10_000 },
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: env.baseUrl,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } } }],
})
