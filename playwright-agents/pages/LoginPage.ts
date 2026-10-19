import { expect, type Locator, type Page } from '@playwright/test'

export class LoginPage {
  readonly form: Locator
  readonly username: Locator
  readonly password: Locator
  readonly togglePassword: Locator
  readonly submit: Locator
  readonly error: Locator

  constructor(readonly page: Page) {
    this.form = page.getByTestId('login-form')
    this.username = page.getByTestId('login-username')
    this.password = page.getByTestId('login-password')
    this.togglePassword = page.getByTestId('toggle-password')
    this.submit = page.getByTestId('login-submit')
    this.error = page.getByTestId('login-error')
  }

  async goto() {
    await this.page.goto('/login')
    await expect(this.form).toBeVisible()
  }

  async login(username: string, password: string) {
    await this.username.fill(username)
    await this.password.fill(password)
    await this.submit.click()
  }
}
