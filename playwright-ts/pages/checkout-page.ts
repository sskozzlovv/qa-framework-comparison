import { expect, type Locator, type Page } from '@playwright/test';
import { BasePage } from './base-page';

export interface Customer {
  firstName: string;
  lastName: string;
  zip: string;
}

export class CheckoutPage extends BasePage {
  readonly firstName: Locator;
  readonly lastName: Locator;
  readonly zip: Locator;
  readonly zipError: Locator;
  readonly placeOrderButton: Locator;
  readonly summary: Locator;
  readonly progress: Locator;
  readonly confirmation: Locator;
  readonly orderId: Locator;
  readonly viewOrdersLink: Locator;

  constructor(page: Page) {
    super(page);
    this.firstName = page.getByLabel('First name');
    this.lastName = page.getByLabel('Last name');
    this.zip = page.getByLabel('ZIP code');
    this.zipError = page.locator('.field-error[data-for="zip"]');
    this.placeOrderButton = page.getByRole('button', { name: 'Place order' });
    this.summary = page.locator('#summary-total');
    this.progress = page.locator('#progress');
    this.confirmation = page.locator('#confirmation');
    this.orderId = page.locator('#order-id');
    this.viewOrdersLink = page.getByRole('link', { name: 'View my orders' });
  }

  async goto(): Promise<void> {
    await this.page.goto('/checkout.html');
    await this.waitUntilReady();
    await expect(this.summary).not.toHaveText('…');
  }

  /** blur() after each field: the app shows validation messages for fields the user has left. */
  async fill(customer: Customer): Promise<void> {
    for (const [field, value] of [
      [this.firstName, customer.firstName],
      [this.lastName, customer.lastName],
      [this.zip, customer.zip],
    ] as const) {
      await field.fill(value);
      await field.blur();
    }
  }

  /** Clicks "Place order" and returns the order id once the confirmation is shown (~2-3 s, more for slow users). */
  async placeOrder(): Promise<string> {
    await this.placeOrderButton.click();
    await expect(this.progress).toHaveAttribute('aria-valuenow', /^([5-9]\d|100)$/);
    await expect(this.confirmation).toBeVisible({ timeout: 20_000 });
    return (await this.orderId.textContent()) ?? '';
  }
}
