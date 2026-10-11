import { expect, previousMonth, test, uid } from '../support/fixtures'

test.describe('Attendance grid', () => {
  let ids: number[] = []
  test.afterAll(async ({ api }) => {
    for (const id of ids) await api.retireStudent(id).catch(() => undefined)
  })

  test('marks P/A, saves in bulk, and totals survive a reload', async ({ authedPage: page, api }) => {
    const group = await api.createGroup(`E2E Att ${uid()}`)
    const a = await api.createStudent({ classGroupId: group.id })
    const b = await api.createStudent({ classGroupId: group.id })
    ids = [a.id, b.id]
    const month = previousMonth()

    await page.goto(`/attendance?month=${month}`)
    await page.getByTestId('attendance-group').selectOption({ label: group.name })
    await expect(page.getByTestId(`grid-row-${a.studentCode}`)).toBeVisible()

    const cell = (code: string, day: number) => page.getByTestId(`cell-${code}-${day}`)
    await cell(a.studentCode, 3).click() // blank -> P
    await cell(a.studentCode, 4).click() // blank -> P
    await cell(a.studentCode, 4).click() // P -> A
    await cell(b.studentCode, 3).click()
    await cell(b.studentCode, 4).focus()
    await page.keyboard.press('a') // keyboard support
    await page.keyboard.press('ArrowRight')
    await page.keyboard.press('p')
    await page.keyboard.press('Delete') // and clear it again

    await expect(cell(a.studentCode, 3)).toHaveAttribute('data-value', 'P')
    await expect(cell(a.studentCode, 4)).toHaveAttribute('data-value', 'A')
    await expect(page.getByTestId('unsaved-count')).toHaveText('4 unsaved changes')
    await page.getByTestId('save-attendance').click()
    await expect(page.getByTestId('unsaved-count')).toHaveCount(0)

    await page.reload()
    await page.getByTestId('attendance-group').selectOption({ label: group.name })
    await expect(cell(b.studentCode, 4)).toHaveAttribute('data-value', 'A')
    await expect(cell(b.studentCode, 5)).toHaveAttribute('data-value', '')
    for (const s of [a, b]) {
      await expect(page.getByTestId(`total-present-${s.studentCode}`)).toHaveText('1')
      await expect(page.getByTestId(`total-absent-${s.studentCode}`)).toHaveText('1')
      await expect(page.getByTestId(`total-held-${s.studentCode}`)).toHaveText('2')
      await expect(page.getByTestId(`total-pct-${s.studentCode}`)).toHaveText('50%')
    }
    await expect(page.getByTestId('present-day-3')).toHaveText('2')
    await expect(page.getByTestId('grid-total-held')).toHaveText('4')
  })
})
