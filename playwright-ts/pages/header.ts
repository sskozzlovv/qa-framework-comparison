import type { Locator, Page } from '@playwright/test';

export class Header {
  readonly cartBadge: Locator;
  readonly username: Locator;
  readonly cartLink: Locator;
  readonly ordersLink: Locator;

  constructor(page: Page) {
    this.cartBadge = page.locator('#cart-badge');
    this.username = page.locator('#current-user');
    // ids, not roles: "Orders" would also match "View my orders" (name match is a substring by default)
    this.cartLink = page.locator('#nav-cart');
    this.ordersLink = page.locator('#nav-orders');
  }
}
