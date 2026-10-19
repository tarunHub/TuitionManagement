// spec: specs/login.plan.md
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test'
import { LoginPage } from '../../pages/LoginPage'
import { throwawayUsername } from '../../support/env'

test.describe('Invalid Credentials', () => {
  test('Wrong credentials show an error', async ({ page }) => {
    const login = new LoginPage(page)

    // 1. Open /login, enter a throwaway username and a wrong password, click "Sign in"
    await login.goto()
    await login.login(throwawayUsername(), 'wrong-password')
    await expect(login.error).toHaveText('Invalid username or password')
    await expect(page).toHaveURL(/\/login$/)
  })
})
