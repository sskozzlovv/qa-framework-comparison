import { test, expect } from '../support/fixtures';
import { products } from '../support/data';

const { backpack, campMug } = products;

test.describe('Cart', () => {
  test('added products appear in the cart and on the backend', { tag: ['@smoke', '@critical'] },
    async ({ catalogPage, cartPage, api, user }) => {
      await catalogPage.goto();

      await catalogPage.addToCart(backpack, campMug);

      await expect(catalogPage.toasts.last()).toHaveText(`${campMug.name} added to cart`);
      await expect(catalogPage.header.cartBadge).toHaveText('2');
      await expect(catalogPage.toasts).toHaveCount(0);
      expect(await api.state(user)).toMatchObject({ cartItems: 2 });

      await catalogPage.header.cartLink.click();
      await expect(cartPage.names).toHaveText([backpack.name, campMug.name]);
      await expect(cartPage.total).toHaveText('$36.98');
    });

  test('removed item disappears after the animation and the total is updated', { tag: '@regression' },
    async ({ cartPage, api }) => {
      // test data via API, not through the catalog UI
      await api.addToCart(backpack.id);
      await api.addToCart(campMug.id);
      await cartPage.goto();

      await cartPage.remove(backpack);

      await expect(cartPage.names).toHaveText([campMug.name]);
      await expect(cartPage.total).toHaveText(campMug.price);
      await expect(cartPage.header.cartBadge).toHaveText('1');
    });

  test('empty cart cannot be checked out', { tag: '@regression' }, async ({ cartPage }) => {
    await cartPage.goto();

    await expect(cartPage.emptyMessage).toBeVisible();
    await expect(cartPage.checkoutButton).toBeDisabled();
  });
});
