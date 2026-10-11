import { expect, test } from '@playwright/test'
import { env } from '../support/env'

test.describe('Authentication', () => {
  test('redirects to login when the token is missing', async ({ page }) => {
    await page.goto('/students')
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByTestId('login-form')).toBeVisible()
  })

  test('shows an error for invalid credentials', async ({ page }) => {
    // A dedicated username so failed attempts never lock the real admin account.
    await page.goto('/login')
    await page.getByTestId('login-username').fill('e2e-nobody')
    await page.getByTestId('login-password').fill('wrong-password')
    await page.getByTestId('login-submit').click()
    await expect(page.getByTestId('login-error')).toContainText('Invalid username or password')
    await expect(page).toHaveURL(/\/login$/)
  })

  test('logs in, shows the dashboard, and logs out', async ({ page }) => {
    await page.goto('/login')
    await page.getByTestId('login-username').fill(env.username)
    await page.getByTestId('login-password').fill(env.password)
    await page.getByTestId('toggle-password').click()
    await expect(page.getByTestId('login-password')).toHaveAttribute('type', 'text')
    await page.getByTestId('login-submit').click()
    await expect(page).toHaveURL(/\/dashboard/)
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()

    await page.getByTestId('logout-button').click()
    await expect(page).toHaveURL(/\/login$/)
    await page.goto('/dashboard')
    await expect(page).toHaveURL(/\/login$/)
  })

  test('API rejects requests without a token', async ({ request }) => {
    const res = await request.get('/api/v1/students')
    expect(res.status()).toBe(401)
    expect((await res.json()).error).toBe('UNAUTHENTICATED')
  })
})
