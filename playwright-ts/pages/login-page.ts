import type { Locator, Page } from '@playwright/test';
import { BasePage } from './base-page';

export class LoginPage extends BasePage {
  readonly username: Locator;
  readonly password: Locator;
  readonly submitButton: Locator;
  readonly error: Locator;

  constructor(page: Page) {
    super(page);
    this.username = page.getByLabel('Username');
    this.password = page.getByLabel('Password');
    this.submitButton = page.getByRole('button', { name: 'Log in' });
    this.error = page.getByTestId('login-error');
  }

  async goto(): Promise<void> {
    await this.page.goto('/index.html');
  }

  async login(username: string, password: string): Promise<void> {
    await this.username.fill(username);
    await this.password.fill(password);
    await this.submitButton.click();
  }
}
