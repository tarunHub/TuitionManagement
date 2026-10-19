import { test, expect } from '@playwright/test'

/**
 * Seed test for the Playwright agents: the planner and generator run this first to get a
 * page in a known state before exploring. Keep it minimal: open the login page.
 */
test.describe('Seed', () => {
  test('seed', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByTestId('login-form')).toBeVisible()
  })
})
