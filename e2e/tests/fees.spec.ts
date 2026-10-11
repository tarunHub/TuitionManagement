import { expect, previousMonth, test, uid } from '../support/fixtures'

test.describe('Monthly fees', () => {
  let ids: number[] = []
  test.afterAll(async ({ api }) => {
    for (const id of ids) await api.retireStudent(id).catch(() => undefined)
  })

  test('generates fees, records full/partial/over payments, and totals correctly', async ({ authedPage: page, api }) => {
    const month = previousMonth()
    const group = await api.createGroup(`E2E Fees ${uid()}`)
    const students = []
    for (let i = 0; i < 3; i++) students.push(await api.createStudent({ classGroupId: group.id, grade: 'Grade 5' }))
    ids = students.map((s) => s.id)
    // Two held classes each (one absence is still billed): $20 × 2 = $40 due.
    await api.attendance(
      students.flatMap((s) => [
        { studentId: s.id, date: `${month}-02`, status: 'PRESENT' as const },
        { studentId: s.id, date: `${month}-03`, status: 'ABSENT' as const },
      ]),
    )

    await page.goto(`/fees?month=${month}`)
    await page.getByTestId('generate-fees').click()
    await expect(page.getByText(/created|refreshed/).first()).toBeVisible()
    await page.getByTestId('fees-filter-group').selectOption({ label: group.name })

    const [full, partial, over] = students
    for (const s of students) await expect(page.getByTestId(`status-${s.studentCode}`)).toContainText('Unpaid')

    const pay = async (code: string, amount: string) => {
      await page.getByTestId(`record-payment-${code}`).click()
      await page.getByTestId('payment-amount').fill(amount)
      await page.getByTestId('payment-method').selectOption('CASH')
      await page.getByTestId('payment-submit').click()
      await expect(page.getByTestId('payment-dialog')).toBeHidden()
    }
    await pay(full.studentCode, '40')
    await pay(partial.studentCode, '15')
    await pay(over.studentCode, '50')

    await expect(page.getByTestId(`status-${full.studentCode}`)).toContainText('Paid')
    await expect(page.getByTestId(`status-${partial.studentCode}`)).toContainText('Partial')
    await expect(page.getByTestId(`status-${over.studentCode}`)).toContainText('Credit')
    await expect(page.getByTestId(`balance-${partial.studentCode}`)).toHaveText('$25.00')
    await expect(page.getByTestId(`balance-${over.studentCode}`)).toHaveText('-$10.00')

    await expect(page.getByTestId('total-due')).toHaveText('$120.00')
    await expect(page.getByTestId('total-paid')).toHaveText('$105.00')
    await expect(page.getByTestId('total-balance')).toHaveText('$15.00')

    // Regenerating is idempotent and never removes payments.
    await page.getByTestId('generate-fees').click()
    await expect(page.getByTestId('total-paid')).toHaveText('$105.00')
    await expect(page.getByTestId('total-due')).toHaveText('$120.00')
  })
})
