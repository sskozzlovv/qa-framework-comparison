import { test, expect } from '../support/fixtures';

test.describe('Orders', { tag: '@regression' }, () => {
  test('orders are loaded page by page', async ({ ordersPage, api, user }) => {
    await api.seedOrders(user, 12);
    await ordersPage.goto();
    await expect(ordersPage.count).toHaveText('12 orders');
    await expect(ordersPage.rows).toHaveCount(5);

    await ordersPage.loadMoreButton.click();
    await expect(ordersPage.rows).toHaveCount(10);

    await ordersPage.loadMoreButton.click();
    await expect(ordersPage.rows).toHaveCount(12);
    await expect(ordersPage.loadMoreButton).toBeHidden();
  });

  test('a new user has no orders', async ({ ordersPage }) => {
    await ordersPage.goto();

    await expect(ordersPage.noOrders).toBeVisible();
    await expect(ordersPage.rows).toHaveCount(0);
  });
});
