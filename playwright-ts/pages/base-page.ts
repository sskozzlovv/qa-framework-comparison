import { expect, type Locator, type Page } from '@playwright/test';
import { Header } from './header';

declare global {
  interface Window {
    __pending?: number; // app's testability hook: fetch() calls in flight
  }
}

export abstract class BasePage {
  readonly header: Header;
  readonly spinner: Locator;

  constructor(readonly page: Page) {
    this.header = new Header(page);
    this.spinner = page.locator('#spinner');
  }

  /** Page loaded, no fetch in flight, overlay gone. */
  protected async waitUntilReady(): Promise<void> {
    await this.page.waitForFunction(() => document.readyState === 'complete' && (window.__pending ?? 0) === 0);
    await expect(this.spinner).toBeHidden();
  }
}
