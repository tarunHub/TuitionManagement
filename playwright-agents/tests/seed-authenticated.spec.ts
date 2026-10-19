import { test, expect } from '@playwright/test'
import { LoginPage } from '../pages/LoginPage'
import { env } from '../support/env'

/** Seed for pages behind login: signs in through the UI and lands on the dashboard. */
test.describe('Seed', () => {
  test('seed authenticated', async ({ page }) => {
    const login = new LoginPage(page)
    await login.goto()
    await login.login(env.username, env.password)
    await expect(page).toHaveURL(/\/dashboard$/)
  })
})
