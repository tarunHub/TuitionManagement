import { env } from '../support/env'
import { expect, test, uid } from '../support/fixtures'

interface MailhogMessage {
  Raw: { To: string[] }
  Content: { Headers: Record<string, string[]>; Body: string }
}

test.describe('Notifications', () => {
  let studentId: number | undefined
  test.afterAll(async ({ api }) => {
    if (studentId) await api.retireStudent(studentId).catch(() => undefined)
  })

  test('composes, previews, and sends an email that arrives in MailHog', async ({ authedPage: page, api }) => {
    const s = await api.createStudent({ guardianName: 'Morgan Mailtest' })
    studentId = s.id
    const subject = `E2E hello ${uid()}`

    await page.goto('/notifications')
    await page.getByTestId('compose-notification').click()
    await page.getByTestId('compose-subject').fill(subject)
    await page.getByTestId('compose-body').fill('Dear {{guardianName}},\n\nThis is about {{studentName}}.')
    await page.getByTestId('mode-STUDENTS').check()
    await page.getByTestId(`pick-student-${s.studentCode}`).check()
    await page.getByTestId('compose-preview').click()
    await expect(page.getByTestId('preview-body')).toContainText('Dear Morgan Mailtest')
    await page.getByTestId('compose-send').click()

    await expect(page).toHaveURL(/\/notifications$/)
    const row = page.locator('[data-testid^="notification-row-"]', { hasText: subject })
    await expect(row).toContainText('Sent')

    await expect
      .poll(async () => {
        const res = await fetch(`${env.mailhogUrl}/api/v2/search?kind=containing&query=${encodeURIComponent(subject)}`)
        const data = (await res.json()) as { items: MailhogMessage[] }
        return data.items.map((m) => ({ to: m.Raw.To, body: m.Content.Body }))
      }, { message: 'Email never reached MailHog. Is the backend sending through real SMTP? Start it with docker-compose.e2e.yml (see RUNNING.md).' })
      .toEqual([expect.objectContaining({ to: [s.email], body: expect.stringContaining('Morgan Mailtest') })])
  })
})
