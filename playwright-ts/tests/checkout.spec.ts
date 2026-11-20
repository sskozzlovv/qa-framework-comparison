import { test, expect } from '../support/fixtures';
import { products } from '../support/data';

const customer = { firstName: 'Ada', lastName: 'Lovelace', zip: '12345' };

test.describe('Checkout', () => {
  test('user buys products and the order gets completed', { tag: '@critical' },
    async ({ catalogPage, cartPage, checkoutPage, ordersPage, api, user }) => {
      // test.step = named blocks in the HTML report and in the trace
      await test.step('put two products into the cart', async () => {
        await catalogPage.goto();
        await catalogPage.addToCart(products.backpack, products.campMug);
        await expect(catalogPage.header.cartBadge).toHaveText('2');
      });

      const orderId = await test.step('check out', async () => {
        await catalogPage.header.cartLink.click();
        await cartPage.checkoutButton.click();
        await expect(checkoutPage.placeOrderButton).toBeDisabled();
        await checkoutPage.fill(customer);
        return checkoutPage.placeOrder();
      });
      expect(orderId).toMatch(/^ORD-\d+$/);

      await test.step('order becomes COMPLETED', async () => {
        await checkoutPage.viewOrdersLink.click();
        await ordersPage.waitForStatus(orderId, 'COMPLETED');
        await expect(ordersPage.header.cartBadge).toBeHidden();
      });

      // expect.poll retries a NON-UI check (here: backend state) until it matches
      await expect.poll(() => api.state(user)).toEqual({ cartItems: 0, orders: 1 });
    });

  for (const zip of ['123', 'abcde', '123456']) {
    test(`invalid zip "${zip}" blocks the order`, { tag: '@regression' }, async ({ checkoutPage, api }) => {
      await api.addToCart(products.backpack.id);
      await checkoutPage.goto();

      await checkoutPage.fill({ ...customer, zip });

      await expect(checkoutPage.zipError).toHaveText('ZIP must be 5 digits');
      await expect(checkoutPage.placeOrderButton).toBeDisabled();
    });
  }

  test.describe('slow user', () => {
    test.use({ userKind: 'slow' });

    test('can still check out', { tag: '@regression' }, async ({ checkoutPage, api }) => {
      await api.addToCart(products.backpack.id);
      await checkoutPage.goto();
      await checkoutPage.fill({ firstName: 'Grace', lastName: 'Hopper', zip: '54321' });

      expect(await checkoutPage.placeOrder()).toMatch(/^ORD-/);
    });
  });
});
