import { expect, type Locator, type Page } from '@playwright/test';
import type { Product } from '../support/data';
import { BasePage } from './base-page';

export class CatalogPage extends BasePage {
  readonly search: Locator;
  readonly sort: Locator;
  readonly resultCount: Locator;
  readonly noResults: Locator;
  readonly loadError: Locator;
  readonly cards: Locator;
  readonly names: Locator;
  readonly prices: Locator;
  readonly toasts: Locator;
  readonly promo: Locator;

  constructor(page: Page) {
    super(page);
    this.search = page.getByPlaceholder('Search products');
    this.sort = page.getByLabel('Sort');
    this.resultCount = page.locator('#result-count');
    this.noResults = page.getByText('No products found.');
    this.loadError = page.locator('#load-error');
    this.cards = page.locator('.product-card');
    this.names = this.cards.locator('.name');
    this.prices = this.cards.locator('.price');
    this.toasts = page.locator('.toast');
    this.promo = page.getByRole('dialog', { name: 'Promo' });
  }

  async goto(): Promise<void> {
    await this.page.goto('/catalog.html');
    await this.waitUntilReady();
    await expect(this.resultCount).not.toBeEmpty();
  }

  /** Start waiting for the response BEFORE the action that triggers it (debounced 400 ms). */
  async searchFor(query: string): Promise<void> {
    const response = this.page.waitForResponse(r => r.url().includes(`/api/products?q=${encodeURIComponent(query)}`));
    await this.search.fill(query);
    await response;
    await expect(this.spinner).toBeHidden();
  }

  async sortBy(value: 'name-asc' | 'name-desc' | 'price-asc' | 'price-desc'): Promise<void> {
    const response = this.page.waitForResponse(r => r.url().includes(`sort=${value}`));
    await this.sort.selectOption(value);
    await response;
    await expect(this.spinner).toBeHidden();
  }

  addButton(product: Product): Locator {
    return this.cards.filter({ hasText: product.name }).getByRole('button');
  }

  async addToCart(...items: Product[]): Promise<void> {
    for (const product of items) {
      await this.addButton(product).click();
      await expect(this.addButton(product)).toHaveText('Added');
    }
  }

  async closePromo(): Promise<void> {
    await this.promo.getByRole('button', { name: 'Close' }).click();
    await expect(this.promo).toBeHidden();
  }

  async pricesInCents(): Promise<number[]> {
    const texts = await this.prices.allTextContents(); // snapshot, no auto-wait
    return texts.map(t => Number(t.replace(/[$.]/g, '')));
  }
}
