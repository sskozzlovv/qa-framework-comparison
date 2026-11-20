import { test, expect } from '../support/fixtures';
import { CATALOG_SIZE, uniqueUser } from '../support/data';

// login tests start from a logged-out browser
test.use({ authenticated: false });

test.describe('Login', () => {
  test.beforeEach(async ({ loginPage }) => {
    await loginPage.goto();
  });

  test('user logs in through the form', { tag: '@smoke' }, async ({ loginPage, catalogPage, user, page }) => {
    await loginPage.login(user.username, user.password);

    await expect(page).toHaveURL(/catalog\.html/);
    await expect(catalogPage.header.username).toHaveText(user.username);
    await expect(catalogPage.names).toHaveCount(CATALOG_SIZE);
  });

  test('locked user sees an error', { tag: '@smoke' }, async ({ loginPage }) => {
    const locked = uniqueUser('locked');

    await loginPage.login(locked.username, locked.password);

    await expect(loginPage.error).toHaveText('Sorry, this user has been locked out');
  });

  // parameterized tests are just a loop: every case is a separate test with its own name
  const invalid = [
    { username: '', password: 'secret', error: 'Username and password are required' },
    { username: 'someone', password: 'wrong', error: 'Username and password do not match any user' },
    { username: 'ab', password: 'secret', error: 'Username and password do not match any user' },
  ];
  for (const { username, password, error } of invalid) {
    test(`rejects user="${username}" password="${password}"`, { tag: '@regression' }, async ({ loginPage }) => {
      await loginPage.login(username, password);

      await expect(loginPage.error).toHaveText(error);
    });
  }

  test.describe('slow user', () => {
    test.use({ userKind: 'slow' });

    test('login blocks the page with a spinner', { tag: '@regression' }, async ({ loginPage, catalogPage, user, page }) => {
      await loginPage.login(user.username, user.password);

      await expect(loginPage.spinner).toBeVisible();
      await expect(page).toHaveURL(/catalog\.html/, { timeout: 15_000 });
      await expect(catalogPage.header.username).toHaveText(user.username);
    });
  });
});
