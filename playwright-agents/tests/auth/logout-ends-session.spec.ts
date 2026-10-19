// spec: specs/login.plan.md
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test'
import { LoginPage } from '../../pages/LoginPage'
import { env } from '../../support/env'

test.describe('Successful Login', () => {
  test('Logout ends the session', async ({ page }) => {
    const login = new LoginPage(page)

    // 1. Sign in with the admin credentials
    await login.goto()
    await login.login(env.username, env.password)
    await expect(page).toHaveURL(/\/dashboard$/)

    // 2. Click "Logout"
    await page.getByTestId('logout-button').click()
    await expect(page).toHaveURL(/\/login$/)

    // 3. Open /dashboard directly
    await page.goto('/dashboard')
    await expect(page).toHaveURL(/\/login$/)
  })
})
