// spec: specs/login.plan.md
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test'
import { LoginPage } from '../../pages/LoginPage'

test.describe('Successful Login', () => {
  test('Password visibility can be toggled', async ({ page }) => {
    const login = new LoginPage(page)

    // 1. Open /login and type any password
    await login.goto()
    await login.password.fill('secret-value')
    await expect(login.password).toHaveAttribute('type', 'password')

    // 2. Click the "Show password" button
    await page.getByRole('button', { name: 'Show password' }).click()
    await expect(login.password).toHaveAttribute('type', 'text')
    await expect(login.togglePassword).toHaveAccessibleName('Hide password')

    // 3. Click the "Hide password" button
    await page.getByRole('button', { name: 'Hide password' }).click()
    await expect(login.password).toHaveAttribute('type', 'password')
  })
})
