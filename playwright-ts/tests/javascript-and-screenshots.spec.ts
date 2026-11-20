import { test, expect } from '../support/fixtures';
import { products } from '../support/data';

test.describe('JavaScript execution', { tag: '@browser-api' }, () => {
  test.beforeEach(async ({ catalogPage }) => {
    await catalogPage.goto();
  });

  test('evaluate returns real JS values', async ({ page, user }) => {
    // the function body runs in the browser, the result is serialized back - fully typed in TS
    const values = await page.evaluate(() => [42, 1.5, 'text', true, null, { key: 'value' }]);
    const username = await page.evaluate(key => localStorage.getItem(key), 'username');

    expect(values).toEqual([42, 1.5, 'text', true, null, { key: 'value' }]);
    expect(username).toBe(user.username);
  });

  test('evaluate awaits promises', async ({ page }) => {
    const status = await page.evaluate(async path => (await fetch(path)).status, '/api/health');
    const delayed = await page.evaluate(() => new Promise(resolve => setTimeout(() => resolve('finished'), 500)));

    expect(status).toBe(200);
    expect(delayed).toBe('finished');
  });
});

test.describe('JS click', { tag: '@browser-api' }, () => {
  test.use({ dismissPromo: false });

  test('dispatchEvent ignores overlays, unlike a real click', async ({ catalogPage }) => {
    await catalogPage.goto();
    await expect(catalogPage.promo).toBeVisible();

    await catalogPage.addButton(products.backpack).dispatchEvent('click');

    await expect(catalogPage.header.cartBadge).toHaveText('1');
  });
});

test.describe('Screenshots', { tag: '@browser-api' }, () => {
  test.beforeEach(async ({ playgroundPage }) => {
    await playgroundPage.goto();
  });

  test('full page and element screenshots are attached to the report', async ({ page, playgroundPage }, testInfo) => {
    const fullPage = await page.screenshot({ fullPage: true });
    await testInfo.attach('full page', { body: fullPage, contentType: 'image/png' });
    await testInfo.attach('promo widget', { body: await playgroundPage.promoWidget.screenshot(), contentType: 'image/png' });

    expect(fullPage.readUInt32BE(20)).toBeGreaterThan(900); // PNG header: image height
  });

  /**
   * Visual regression: compared with a committed baseline in *-snapshots/. Missing baselines are written on the
   * first run (updateSnapshots: 'missing'); re-baseline with `npm run update-snapshots`.
   * Baselines are per browser AND per OS (fonts render differently) - generate them where CI runs.
   */
  test('promo widget looks as before', async ({ playgroundPage }) => {
    await expect(playgroundPage.promoWidget).toHaveScreenshot('promo-widget.png');
  });

  test('playground page looks as before, user name masked', async ({ page, playgroundPage }) => {
    await expect(page).toHaveScreenshot('playground.png', {
      fullPage: true,
      mask: [playgroundPage.header.username], // volatile data would break the comparison
    });
  });
});
