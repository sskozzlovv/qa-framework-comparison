import { test, expect } from '../support/fixtures';

/**
 * Fails ON PURPOSE (excluded by default). Run `npm run test:failure-demo`, then `npm run report`:
 * the HTML report shows the error, screenshot, video and the trace (every step with DOM snapshot, network, console).
 */
test('catalog has thirteen products', { tag: '@demo-failure' }, async ({ catalogPage }) => {
  await catalogPage.goto();

  await expect(catalogPage.names).toHaveCount(13);
});
