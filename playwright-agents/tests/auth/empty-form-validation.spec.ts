// spec: specs/login.plan.md
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test'
import { LoginPage } from '../../pages/LoginPage'

test.describe('Form Validation', () => {
  test('Submitting an empty form shows required-field errors', async ({ page }) => {
    const login = new LoginPage(page)

    // 1. Open /login and click "Sign in" without entering anything
    await login.goto()
    await login.submit.click()
    await expect(page.getByText('Username is required')).toBeVisible()
    await expect(page.getByText('Password is required')).toBeVisible()
    await expect(page).toHaveURL(/\/login$/)
  })
})
