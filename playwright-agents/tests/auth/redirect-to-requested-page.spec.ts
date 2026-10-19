// spec: specs/login.plan.md
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test'
import { LoginPage } from '../../pages/LoginPage'
import { env } from '../../support/env'

test.describe('Successful Login', () => {
  test('Login returns the user to the page they originally requested', async ({ page }) => {
    const login = new LoginPage(page)

    // 1. While signed out, open /students
    await page.goto('/students')
    await expect(page).toHaveURL(/\/login$/)

    // 2. Sign in with the admin credentials
    await login.login(env.username, env.password)
    await expect(page).toHaveURL(/\/students$/)
  })
})
