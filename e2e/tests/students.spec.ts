import { expect, test, uid } from '../support/fixtures'

test.describe('Students', () => {
  const created: number[] = []
  test.afterAll(async ({ api }) => {
    for (const id of created) await api.retireStudent(id).catch(() => undefined)
  })

  test('validates required fields', async ({ authedPage: page }) => {
    await page.goto('/students/new')
    await page.getByTestId('student-submit').click()
    await expect(page.getByTestId('error-fullName')).toBeVisible()
    await expect(page.getByTestId('error-guardianName')).toBeVisible()
    // Email is optional, but must be valid when given.
    await expect(page.getByTestId('error-email')).toHaveCount(0)
    await page.getByTestId('student-email').fill('not-an-email')
    await page.getByTestId('student-submit').click()
    await expect(page.getByTestId('error-email')).toBeVisible()
  })

  test('creates, edits, and deletes a student without history', async ({ authedPage: page }) => {
    const name = `Pat Create ${uid()}`
    await page.goto('/students')
    await page.getByTestId('add-student').click()
    await page.getByTestId('student-fullName').fill(name)
    await page.getByTestId('student-dateOfBirth').fill('2016-02-03')
    await page.getByTestId('student-grade').selectOption('Grade 5')
    await expect(page.getByTestId('student-perClassRate')).toHaveValue('20')
    await page.getByTestId('student-guardianName').fill('Robin Create')
    await page.getByTestId('student-email').fill('robin.create@example.com')
    await page.getByTestId('student-primaryContact').fill('905-555-0123')
    await page.getByTestId('student-emergencyContactName').fill('Sam Create')
    await page.getByTestId('student-emergencyContactPhone').fill('905-555-0124')
    await page.getByTestId('student-submit').click()

    await expect(page.getByRole('heading', { name })).toBeVisible()
    await expect(page.getByTestId('student-code')).toHaveText(/^SS\d{3,}$/)
    const id = Number(page.url().split('/').pop())
    created.push(id)

    await page.getByTestId('edit-student').click()
    await page.getByTestId('student-school').fill('E2E Public School')
    await page.getByTestId('student-submit').click()
    await expect(page.getByText('E2E Public School')).toBeVisible()

    await page.getByTestId('delete-student').click()
    await expect(page.getByTestId('delete-explanation')).toContainText('permanently deleted')
    await page.getByTestId('confirm-button').click()
    await expect(page).toHaveURL(/\/students$/)
    created.pop()
  })

  test('a student with history cannot be deleted and is deactivated instead', async ({ authedPage: page, api }) => {
    const s = await api.createStudent()
    created.push(s.id)
    await api.attendance([{ studentId: s.id, date: '2026-09-01', status: 'PRESENT' }])

    await expect(api.call('DELETE', `/students/${s.id}`)).rejects.toThrow(/409/)

    await page.goto(`/students/${s.id}`)
    await expect(page.getByTestId('delete-student')).toHaveText(/Deactivate/)
    await page.getByTestId('delete-student').click()
    await expect(page.getByTestId('delete-explanation')).toContainText('cannot be deleted')
    await page.getByTestId('confirm-button').click()
    await expect(page.getByText('Inactive').first()).toBeVisible()
    await expect(page.getByTestId('activate-student')).toBeVisible()
  })

  test('searches the list', async ({ authedPage: page }) => {
    await page.goto('/students')
    await page.getByTestId('table-search').fill('Zara')
    await expect(page.getByTestId('student-row-SS004')).toBeVisible()
    await expect(page.getByTestId('student-row-SS001')).toHaveCount(0)
  })
})
