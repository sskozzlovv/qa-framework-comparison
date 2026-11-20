import { expect, type Locator, type Page } from '@playwright/test';
import { BasePage } from './base-page';

export class OrdersPage extends BasePage {
  readonly rows: Locator;
  readonly loadMoreButton: Locator;
  readonly refreshButton: Locator;
  readonly count: Locator;
  readonly noOrders: Locator;

  constructor(page: Page) {
    super(page);
    this.rows = page.locator('#orders tbody tr');
    this.loadMoreButton = page.getByRole('button', { name: 'Load more' });
    this.refreshButton = page.getByRole('button', { name: 'Refresh' });
    this.count = page.locator('#orders-count');
    this.noOrders = page.getByText('You have no orders yet.');
  }

  async goto(): Promise<void> {
    await this.page.goto('/orders.html');
    await this.waitUntilReady();
    await expect(this.count).not.toBeEmpty();
  }

  status(orderId: string): Locator {
    return this.page.locator(`tr[data-order-id="${orderId}"] .status`);
  }

  /**
   * The page does not poll - the status changes only after "Refresh".
   * expect(...).toPass() retries the whole block (action + assertion) until it passes.
   */
  async waitForStatus(orderId: string, status: string): Promise<void> {
    await expect(async () => {
      await this.refreshButton.click();
      await expect(this.status(orderId)).toHaveText(status, { timeout: 1_000 });
    }).toPass({ intervals: [1_000], timeout: 20_000 });
  }
}
