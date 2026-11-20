import type { Locator, Page } from '@playwright/test';
import type { Product } from '../support/data';
import { BasePage } from './base-page';

export class CartPage extends BasePage {
  readonly items: Locator;
  readonly names: Locator;
  readonly total: Locator;
  readonly checkoutButton: Locator;
  readonly emptyMessage: Locator;

  constructor(page: Page) {
    super(page);
    this.items = page.locator('.cart-item');
    this.names = this.items.locator('.name');
    this.total = page.locator('#cart-total');
    this.checkoutButton = page.getByRole('button', { name: 'Checkout' });
    this.emptyMessage = page.getByText('Your cart is empty.');
  }

  async goto(): Promise<void> {
    await this.page.goto('/cart.html');
    await this.waitUntilReady();
  }

  /** The row fades out for 0.5 s before it is removed - wait for removal, not for opacity. */
  async remove(product: Product): Promise<void> {
    const row = this.items.filter({ hasText: product.name });
    await row.getByRole('button', { name: 'Remove' }).click();
    await row.waitFor({ state: 'detached' });
  }
}
