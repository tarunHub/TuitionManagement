// spec: specs/login.plan.md
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test'
import { LoginPage } from '../../pages/LoginPage'
import { env } from '../../support/env'

test.describe('Successful Login', () => {
  test('Login with valid credentials opens the dashboard', async ({ page }) => {
    const login = new LoginPage(page)

    // 1. Open /login
    await login.goto()
    await expect(login.username).toBeVisible()
    await expect(login.password).toBeVisible()
    await expect(login.submit).toHaveText('Sign in')

    // 2. Enter the admin username and password and click "Sign in"
    await login.login(env.username, env.password)
    await expect(page).toHaveURL(/\/dashboard$/)
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()
    await expect(page.getByTestId('logout-button')).toBeVisible()
  })
})
