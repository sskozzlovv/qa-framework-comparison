import { test as base, expect } from '@playwright/test';
import { CartPage } from '../pages/cart-page';
import { CatalogPage } from '../pages/catalog-page';
import { CheckoutPage } from '../pages/checkout-page';
import { LoginPage } from '../pages/login-page';
import { OrdersPage } from '../pages/orders-page';
import { PlaygroundPage } from '../pages/playground-page';
import { ApiClient } from './api-client';
import { uniqueUser, type TestUser, type UserKind } from './data';

/** Options: switched per file/describe with test.use({...}). */
type Options = {
  userKind: UserKind;
  /** false = the browser starts logged out (login tests) */
  authenticated: boolean;
  /** false = let the random promo modal appear */
  dismissPromo: boolean;
};

type Fixtures = {
  user: TestUser;
  api: ApiClient;
  loginPage: LoginPage;
  catalogPage: CatalogPage;
  cartPage: CartPage;
  checkoutPage: CheckoutPage;
  ordersPage: OrdersPage;
  playgroundPage: PlaygroundPage;
};

/**
 * Fixtures replace BaseTest + @BeforeEach/@AfterEach + JUnit extensions:
 * a test asks for what it needs by name, Playwright builds the dependency graph,
 * and code after `await use(...)` is the teardown - it runs even when the test failed.
 */
export const test = base.extend<Options & Fixtures>({
  userKind: ['normal', { option: true }],
  authenticated: [true, { option: true }],
  dismissPromo: [true, { option: true }],

  user: async ({ userKind }, use) => {
    await use(uniqueUser(userKind));
  },

  // own APIRequestContext (not the built-in `request`: that one picks up the browser-context options, and our
  // storageState below depends on this fixture). No browser page involved.
  api: async ({ playwright, baseURL, user }, use) => {
    const request = await playwright.request.newContext({ baseURL });
    const api = new ApiClient(request);
    await use(api);
    await api.cleanup(user); // teardown: delete this test's cart and orders - runs even if the test failed
    await request.dispose();
  },

  /**
   * Override of the built-in `storageState` option: the BrowserContext is CREATED already logged in.
   * No UI login, no "open the origin first, then write localStorage" dance.
   */
  storageState: async ({ authenticated, dismissPromo, api, user, baseURL }, use) => {
    if (!authenticated) {
      await use({ cookies: [], origins: [] });
      return;
    }
    const token = await api.login(user);
    const localStorage = [
      { name: 'token', value: token },
      { name: 'username', value: user.username },
    ];
    if (dismissPromo) localStorage.push({ name: 'promo.dismissed', value: '1' });
    await use({ cookies: [], origins: [{ origin: baseURL!, localStorage }] });
  },

  loginPage: async ({ page }, use) => use(new LoginPage(page)),
  catalogPage: async ({ page }, use) => use(new CatalogPage(page)),
  cartPage: async ({ page }, use) => use(new CartPage(page)),
  checkoutPage: async ({ page }, use) => use(new CheckoutPage(page)),
  ordersPage: async ({ page }, use) => use(new OrdersPage(page)),
  playgroundPage: async ({ page }, use) => use(new PlaygroundPage(page)),
});

export { expect };
