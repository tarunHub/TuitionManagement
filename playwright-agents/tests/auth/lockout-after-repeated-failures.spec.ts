// spec: specs/login.plan.md
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test'
import { LoginPage } from '../../pages/LoginPage'
import { throwawayUsername } from '../../support/env'

/** Matches the backend default LOGIN_MAX_FAILURES. */
const MAX_FAILURES = 5

test.describe('Invalid Credentials', () => {
  test('Repeated failures lock the username', async ({ page }) => {
    const login = new LoginPage(page)
    const username = throwawayUsername()

    // 1. Open /login and fail to sign in 5 times with the same throwaway username
    await login.goto()
    for (let attempt = 1; attempt <= MAX_FAILURES; attempt++) {
      const response = page.waitForResponse('**/api/v1/auth/login')
      await login.login(username, `wrong-${attempt}`)
      await response
      await expect(login.error).toHaveText('Invalid username or password')
    }

    // 2. Try a 6th time with the same username
    await login.login(username, 'wrong-again')
    await expect(login.error).toHaveText(/Too many failed login attempts\. Try again in \d+ minute\(s\)\./)
  })
})
