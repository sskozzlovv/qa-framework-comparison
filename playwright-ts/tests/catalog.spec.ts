import { test, expect } from '../support/fixtures';
import { products } from '../support/data';

test.describe('Catalog', { tag: '@regression' }, () => {
  test.beforeEach(async ({ catalogPage }) => {
    await catalogPage.goto();
  });

  test('search shows only matching products', async ({ catalogPage }) => {
    await catalogPage.searchFor('shirt');

    await expect(catalogPage.names).toHaveText(['Cotton T-Shirt']); // array = the whole list, in order
  });

  test('search without matches shows the empty state', async ({ catalogPage }) => {
    await catalogPage.searchFor('no-such-product');

    await expect(catalogPage.noResults).toBeVisible();
    await expect(catalogPage.names).toHaveCount(0);
  });

  for (const sort of ['price-asc', 'price-desc'] as const) {
    test(`products are sorted by ${sort}`, async ({ catalogPage }) => {
      await catalogPage.sortBy(sort);

      const prices = await catalogPage.pricesInCents();
      const expected = [...prices].sort((a, b) => (sort === 'price-asc' ? a - b : b - a));
      expect(prices).toHaveLength(12);
      expect(prices).toEqual(expected);
    });
  }

  test('a locator survives a re-render', async ({ catalogPage }) => {
    const firstCard = catalogPage.cards.first();
    await expect(firstCard).toContainText('Camp Mug');

    await catalogPage.searchFor('bottle');

    await expect(firstCard).toContainText('Steel Bottle');
  });
});

test.describe('Promo modal', { tag: '@regression' }, () => {
  test.use({ dismissPromo: false });

  test('must be closed before shopping', async ({ catalogPage }) => {
    await catalogPage.goto();
    await expect(catalogPage.promo).toBeVisible();

    // actionability: Playwright waits while the modal "intercepts pointer events", then times out
    await expect(catalogPage.addButton(products.backpack).click({ timeout: 1_000 }))
      .rejects.toThrow(/intercepts pointer events/);

    await catalogPage.closePromo();
    await catalogPage.addToCart(products.backpack);
    await expect(catalogPage.header.cartBadge).toHaveText('1');
  });
});

test.describe('Backend failure', { tag: '@regression' }, () => {
  test('error from the products API is shown to the user', async ({ page, catalogPage }) => {
    // network mocking: fake a state that is hard to produce with the real backend
    await page.route('**/api/products*', route =>
      route.fulfill({ status: 500, json: { error: 'Database is down' } }));

    await page.goto('/catalog.html');

    await expect(catalogPage.loadError).toHaveText('Could not load products: Database is down');
    await expect(catalogPage.names).toHaveCount(0);
  });
});
