import { expect, test } from '../support/fixtures'

test('dashboard shows the seeded August 2026 figures', async ({ authedPage: page }) => {
  await page.goto('/dashboard?month=2026-08')
  await expect(page.getByTestId('stat-billed')).toHaveText('$930.00')
  await expect(page.getByTestId('stat-collected')).toHaveText('$960.00')
  await expect(page.getByTestId('stat-outstanding')).toHaveText('-$30.00')
  await expect(page.getByTestId('stat-collection-rate')).toHaveText('103%')
  await expect(page.getByTestId('stat-classes-held')).toHaveText('16')
  await expect(page.getByTestId('summary-row-2026-08')).toContainText('$930.00')
  await expect(page.getByTestId('summary-row-2026-08')).toContainText('-$30.00')
  await expect(page.getByTestId('year-total-row')).toBeVisible()
})

test('dashboard matches the fees page for the same month', async ({ authedPage: page }) => {
  await page.goto('/fees?month=2026-08')
  const due = await page.getByTestId('total-due').textContent()
  const paid = await page.getByTestId('total-paid').textContent()
  await page.goto('/dashboard?month=2026-08')
  await expect(page.getByTestId('stat-billed')).toHaveText(due!)
  await expect(page.getByTestId('stat-collected')).toHaveText(paid!)
})
