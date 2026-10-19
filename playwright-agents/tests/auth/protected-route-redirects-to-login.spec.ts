// spec: specs/login.plan.md
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test'
import { LoginPage } from '../../pages/LoginPage'

test.describe('Access Control', () => {
  test('Protected pages redirect signed-out users to login', async ({ page }) => {
    const login = new LoginPage(page)

    // 1. While signed out, open /dashboard
    await page.goto('/dashboard')
    await expect(page).toHaveURL(/\/login$/)
    await expect(login.form).toBeVisible()
  })
})
