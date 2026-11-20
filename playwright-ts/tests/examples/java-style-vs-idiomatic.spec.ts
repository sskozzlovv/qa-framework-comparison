/**
 * ONE scenario - "buy two products and wait until the order is COMPLETED" - written twice.
 *
 * 1. "Selenium habits in Playwright": what a team coming from WebDriver usually writes first.
 *    It passes, but every marked line is a known anti-pattern (slow, flaky or blind on failure).
 * 2. Idiomatic Playwright Test: fixtures, user-facing locators, web-first assertions.
 *
 * Run only this file:  npx playwright test examples
 */
import { test as plainTest, expect as plainExpect } from '@playwright/test';
import { test, expect } from '../../support/fixtures';
import { products, uniqueUser, type TestUser } from '../../support/data';

// ------------------------------------------------------------------------------------------------------------
// 1. Selenium habits
// ------------------------------------------------------------------------------------------------------------
plainTest.describe('Purchase - Selenium habits', { tag: '@example' }, () => {
  let user: TestUser; // shared mutable state between hooks and test (BaseTest field)

  plainTest.beforeEach(async ({ page }) => {
    user = uniqueUser();
    // UI login before EVERY test: seconds per test, and the login page becomes a dependency of every test
    await page.goto('/index.html');
    await page.fill('#username', user.username);            // page.fill(selector): legacy API, CSS ids everywhere
    await page.fill('#password', user.password);
    await page.click('#login');
    await page.waitForURL('**/catalog.html');
    await page.evaluate(() => localStorage.setItem('promo.dismissed', '1'));
    await page.reload();                                    // second page load just to get rid of the modal
    await page.waitForLoadState('networkidle');             // discouraged: "500 ms of silence" is not a state
  });

  plainTest.afterEach(async ({ page }) => {
    // cleanup THROUGH THE PAGE, like executeAsyncScript: breaks if the page is gone or blocked by a dialog
    await page.evaluate(async username => {
      await fetch('/api/test/cleanup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Test-Key': 'let-me-in' },
        body: JSON.stringify({ username }),
      });
    }, user.username);
  });

  plainTest('user buys products', async ({ page }) => {
    await page.click('.product-card:has-text("Trail Backpack") button');
    await page.waitForTimeout(1500);                        // Thread.sleep: too long usually, too short sometimes
    await page.click('.product-card:has-text("Camp Mug") button');
    await page.waitForTimeout(1500);

    const badge = await page.textContent('#cart-badge');    // one-shot read...
    plainExpect(badge).toBe('2');                           // ...+ plain assertion = no retry, flaky

    await page.click('#nav-cart');
    await page.waitForSelector('.cart-item');               // explicit wait that auto-waiting already does
    await page.click('#checkout');
    await page.waitForSelector('#first-name');
    await page.fill('#first-name', 'Ada');
    await page.press('#first-name', 'Tab');
    await page.fill('#last-name', 'Lovelace');
    await page.press('#last-name', 'Tab');
    await page.fill('#zip', '12345');
    await page.press('#zip', 'Tab');
    await page.click('#place-order');
    await page.waitForSelector('#confirmation', { state: 'visible', timeout: 20_000 });
    const orderId = await page.textContent('#order-id');
    plainExpect(orderId).toMatch(/^ORD-\d+$/);

    await page.click('#view-orders');
    for (let attempt = 0; attempt < 20; attempt++) {          // hand-made polling loop
      const status = await page.textContent(`tr[data-order-id="${orderId}"] .status`);
      if (status === 'COMPLETED') break;
      await page.waitForTimeout(1000);
      await page.click('#refresh');
      await page.waitForLoadState('networkidle');
    }
    plainExpect(await page.textContent(`tr[data-order-id="${orderId}"] .status`)).toBe('COMPLETED');
  });
});

// ------------------------------------------------------------------------------------------------------------
// 2. Idiomatic Playwright Test
// ------------------------------------------------------------------------------------------------------------
// - logged in via the storageState fixture (API login, no UI), promo already dismissed
// - cleanup happens in the `api` fixture teardown, through the API, even if the test failed
// - every expect() retries until it passes, so no sleeps and no manual polling
test('Purchase - idiomatic', { tag: '@example' }, async ({ catalogPage, cartPage, checkoutPage, ordersPage }) => {
  await catalogPage.goto();
  await catalogPage.addToCart(products.backpack, products.campMug);
  await expect(catalogPage.header.cartBadge).toHaveText('2');

  await catalogPage.header.cartLink.click();
  await cartPage.checkoutButton.click();
  await checkoutPage.fill({ firstName: 'Ada', lastName: 'Lovelace', zip: '12345' });
  const orderId = await checkoutPage.placeOrder();
  expect(orderId).toMatch(/^ORD-\d+$/);

  await checkoutPage.viewOrdersLink.click();
  await ordersPage.waitForStatus(orderId, 'COMPLETED'); // expect(...).toPass() inside
});
