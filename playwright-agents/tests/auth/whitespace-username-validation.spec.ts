// spec: specs/login.plan.md
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test'
import { LoginPage } from '../../pages/LoginPage'

test.describe('Form Validation', () => {
  test('A whitespace-only username is rejected', async ({ page }) => {
    const login = new LoginPage(page)

    // 1. Open /login, enter three spaces as username and any password, click "Sign in"
    await login.goto()
    await login.login('   ', 'any-password')
    await expect(page.getByText('Username is required')).toBeVisible()
    await expect(page.getByText('Password is required')).toHaveCount(0)
    await expect(page).toHaveURL(/\/login$/)
  })
})
