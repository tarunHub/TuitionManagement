// spec: specs/login.plan.md
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test'
import { LoginPage } from '../../pages/LoginPage'
import { env } from '../../support/env'

test.describe('Access Control', () => {
  test('Signed-in users visiting /login are sent to the dashboard', async ({ page }) => {
    const login = new LoginPage(page)

    // 1. Sign in with the admin credentials
    await login.goto()
    await login.login(env.username, env.password)
    await expect(page).toHaveURL(/\/dashboard$/)

    // 2. Open /login directly
    await page.goto('/login')
    await expect(page).toHaveURL(/\/dashboard$/)
  })
})
