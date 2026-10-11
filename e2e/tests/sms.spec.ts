import { env } from '../support/env'
import { expect, test, uid } from '../support/fixtures'

/** What the sms-sink container (docker-compose.e2e.yml) recorded, i.e. what the Android gateway would have texted. */
interface SinkMessage {
  path: string
  auth: string | null
  text: string
  phoneNumbers: string[]
}

const sinkMessages = async () => (await (await fetch(`${env.smsSinkUrl}/messages`)).json()) as SinkMessage[]

test.describe('SMS notifications', () => {
  const ids: number[] = []
  test.afterAll(async ({ api }) => {
    for (const id of ids) await api.retireStudent(id).catch(() => undefined)
  })

  test('a parent who opted in on the student form receives the text in E.164 format', async ({ authedPage: page, api }) => {
    const last4 = String(Math.floor(Math.random() * 1e4)).padStart(4, '0')
    const s = await api.createStudent({ guardianName: 'Casey Texter', primaryContact: `416 555-${last4}` })
    ids.push(s.id)

    // Opt in through the UI, as the admin would after the parent agrees.
    await page.goto(`/students/${s.id}/edit`)
    await expect(page.getByTestId('student-sms-opt-in')).not.toBeChecked()
    await page.getByTestId('student-sms-opt-in').check()
    await page.getByTestId('student-submit').click()
    await expect(page.getByText('Yes (opted in)')).toBeVisible()

    const title = `E2E sms ${uid()}`
    await page.goto('/notifications')
    await page.getByTestId('compose-notification').click()
    await page.getByTestId('channel-SMS').check()
    await expect(page.getByTestId('sms-not-configured')).toHaveCount(0)
    await page.getByTestId('compose-subject').fill(title)
    await page.getByTestId('compose-body').fill(`Hi {{guardianName}}, no class tomorrow. ${title}`)
    await expect(page.getByTestId('sms-length')).toContainText('about 1 SMS')
    await page.getByTestId('mode-STUDENTS').check()
    await page.getByTestId(`pick-student-${s.studentCode}`).check()
    await page.getByTestId('compose-preview').click()
    await expect(page.getByTestId('preview-body')).toContainText('Hi Casey Texter')
    await expect(page.getByTestId('preview-panel')).toContainText(`+1416555${last4}`)
    await page.getByTestId('compose-send').click()

    await expect(page).toHaveURL(/\/notifications$/)
    const row = page.locator('[data-testid^="notification-row-"]', { hasText: title })
    await expect(row).toContainText('SMS')
    await expect(row).toContainText('Sent')

    await expect
      .poll(async () => (await sinkMessages()).filter((m) => m.text.includes(title)), {
        message: 'SMS never reached the sms-sink. Is the backend started with docker-compose.e2e.yml (see RUNNING.md)?',
      })
      .toEqual([expect.objectContaining({ path: '/message', auth: 'e2e:e2e-secret', text: `Hi Casey Texter, no class tomorrow. ${title}`, phoneNumbers: [`+1416555${last4}`] })])
  })

  test('parents who have not opted in are never texted', async ({ authedPage: page, api }) => {
    const s = await api.createStudent({ guardianName: 'Quinn Quiet' })
    ids.push(s.id)

    await page.goto('/notifications/compose')
    await page.getByTestId('channel-SMS').check()
    await page.getByTestId('compose-subject').fill(`E2E no-consent ${uid()}`)
    await page.getByTestId('compose-body').fill('This must not be sent')
    await page.getByTestId('mode-STUDENTS').check()
    await page.getByTestId(`pick-student-${s.studentCode}`).check()
    await page.getByTestId('compose-send').click()

    await expect(page.getByRole('alert').filter({ hasText: 'opted in to SMS' }).first()).toBeVisible()
    await expect(page).toHaveURL(/\/compose$/)
    expect((await sinkMessages()).some((m) => m.text === 'This must not be sent')).toBe(false)
  })
})
