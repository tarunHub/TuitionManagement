import { expect, test } from '../support/fixtures'

/**
 * Runs against e2e/fake-ollama (docker-compose.e2e.yml): the fake asks the backend for real lookups and answers
 * with the numbers they returned, so these tests prove the question -> tool -> database -> answer path end to end.
 */
test.describe('Ask (assistant)', () => {
  test('answers from a real lookup and links to the screen the numbers came from', async ({ authedPage: page, api }) => {
    const active = await api.call<{ totalElements: number }>('GET', '/students', undefined, { status: 'ACTIVE', size: 1 })

    await page.goto('/ask')
    await expect(page.getByTestId('chat-unavailable')).toHaveCount(0)
    await page.getByTestId('chat-input').fill('How many students are active?')
    await page.getByTestId('chat-input').press('Enter')

    const answer = page.getByTestId('chat-answer').last()
    await expect(answer).toContainText(`There are ${active.totalElements} active students.`)
    await expect(answer.locator('strong')).toHaveText(String(active.totalElements))
    await answer.getByTestId('chat-source').filter({ hasText: 'Students' }).click()
    await expect(page).toHaveURL(/\/students$/)
  })

  test('follow-up fee question uses the current month, and unlooked-up answers are flagged', async ({ authedPage: page, api }) => {
    const month = new Date().toISOString().slice(0, 7)
    const fees = await api.call<{ totals: { amountDue: number } }>('GET', '/fees', undefined, { month })

    await page.goto('/ask')
    await page.getByTestId('chat-input').fill('Who still owes fees?')
    await page.getByTestId('chat-send').click()
    const feeAnswer = page.getByTestId('chat-answer').last()
    await expect(feeAnswer).toContainText(`total due is $${fees.totals.amountDue.toFixed(2)}`)
    await expect(feeAnswer.getByTestId('chat-source')).toHaveAttribute('href', `/fees?month=${month}`)

    await page.getByTestId('chat-input').fill("What's the weather?")
    await page.getByTestId('chat-send').click()
    await expect(page.getByTestId('chat-answer')).toHaveCount(2)
    await expect(page.getByTestId('chat-answer').last().getByTestId('chat-ungrounded')).toBeVisible()

    await page.getByTestId('chat-new').click()
    await expect(page.getByTestId('chat-log')).toHaveCount(0)
  })

  test('bottom-right popup answers on any screen and carries the conversation to the Ask page', async ({ authedPage: page, api }) => {
    const active = await api.call<{ totalElements: number }>('GET', '/students', undefined, { status: 'ACTIVE', size: 1 })

    await page.goto('/dashboard')
    await expect(page.getByTestId('chat-widget')).toHaveCount(0)
    await page.getByTestId('chat-widget-toggle').click()
    const widget = page.getByTestId('chat-widget')
    await widget.getByTestId('chat-input').fill('How many students are active?')
    await widget.getByTestId('chat-input').press('Enter')
    await expect(widget.getByTestId('chat-answer')).toContainText(`There are ${active.totalElements} active students.`)

    await widget.getByRole('link', { name: 'Open full page' }).click()
    await expect(page).toHaveURL(/\/ask$/)
    await expect(page.getByTestId('chat-widget-toggle')).toHaveCount(0)
    await expect(page.getByTestId('chat-answer')).toContainText(`There are ${active.totalElements} active students.`)
  })
})

test.describe('Feedback (AI draft)', () => {
  test('drafts feedback from rough notes, can undo, and saves only when the admin saves', async ({ authedPage: page, api }) => {
    const month = new Date().toISOString().slice(0, 7)
    const student = await api.createStudent()
    try {
      await page.goto(`/feedback?month=${month}`)
      await page.getByTestId(`edit-feedback-${student.studentCode}`).click()
      const drawer = page.getByTestId('feedback-drawer')
      await drawer.getByTestId('feedback-ai-notes').fill('fractions and word problems')
      await drawer.getByTestId('feedback-ai-draft').click()

      await expect(drawer.getByTestId('feedback-topics')).toHaveValue('Drafted: fractions and word problems')
      await expect(drawer.getByTestId('feedback-achievements')).toHaveValue(`${student.fullName} worked hard this month.`)
      await drawer.getByTestId('feedback-ai-undo').click()
      await expect(drawer.getByTestId('feedback-topics')).toHaveValue('')

      // Drafting alone saves nothing.
      let rows = await api.call<{ studentId: number; id: number | null }[]>('GET', '/feedback', undefined, { month })
      expect(rows.find((r) => r.studentId === student.id)?.id).toBeNull()

      await drawer.getByTestId('feedback-ai-draft').click()
      await expect(drawer.getByTestId('feedback-topics')).toHaveValue('Drafted: fractions and word problems')
      await drawer.getByTestId('feedback-save').click()
      await expect(drawer).toBeHidden()
      rows = await api.call('GET', '/feedback', undefined, { month })
      expect(rows.find((r) => r.studentId === student.id)).toMatchObject({
        topicsTaught: 'Drafted: fractions and word problems',
        improvementAreas: `${student.fullName} should keep practising.`,
      })
    } finally {
      await api.retireStudent(student.id)
    }
  })
})
