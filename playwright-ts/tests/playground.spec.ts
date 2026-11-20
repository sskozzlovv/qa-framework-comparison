import { readFile } from 'node:fs/promises';
import { test, expect } from '../support/fixtures';

test.describe('Browser features', { tag: '@browser-api' }, () => {
  test.beforeEach(async ({ playgroundPage }) => {
    await playgroundPage.goto();
  });

  test('confirm dialog can be accepted', async ({ page, playgroundPage }) => {
    // register the handler BEFORE the click; without one Playwright auto-dismisses every dialog
    const dialog = page.waitForEvent('dialog');
    await playgroundPage.confirmButton.click();
    const confirm = await dialog;
    expect(confirm.message()).toBe('Delete all data?');
    await confirm.accept();

    await expect(playgroundPage.dialogResult).toHaveText('confirmed');
  });

  test('confirm dialog can be dismissed', async ({ page, playgroundPage }) => {
    page.once('dialog', dialog => dialog.dismiss());
    await playgroundPage.confirmButton.click();

    await expect(playgroundPage.dialogResult).toHaveText('cancelled');
  });

  test('prompt receives typed text', async ({ page, playgroundPage }) => {
    page.once('dialog', dialog => dialog.accept('Captain'));
    await playgroundPage.promptButton.click();

    await expect(playgroundPage.dialogResult).toHaveText('renamed to Captain');
  });

  test('message can be sent from an iframe', async ({ playgroundPage }) => {
    await playgroundPage.sendSupportMessage('Where is my order?');

    await expect(playgroundPage.supportFrame.getByText('Thanks! We will reply soon.')).toBeVisible();
  });

  test('promo code inside shadow DOM is applied', async ({ playgroundPage }) => {
    await playgroundPage.applyPromoCode('save10');

    await expect(playgroundPage.promoResult).toHaveText('Code applied: -10%');
  });

  test('receipt opens in a new tab with shared localStorage', async ({ page, context, playgroundPage, user }) => {
    const popup = page.waitForEvent('popup');
    await playgroundPage.receiptLink.click();
    const receipt = await popup;

    await expect(receipt.locator('#receipt-user')).toHaveText(user.username);
    await receipt.close();
    expect(context.pages()).toHaveLength(1);
  });

  test('file can be uploaded', async ({ playgroundPage }) => {
    // no file on disk needed: an in-memory buffer
    await playgroundPage.fileInput.setInputFiles({
      name: 'notes.txt', mimeType: 'text/plain', buffer: Buffer.from('hello upload'),
    });

    await expect(playgroundPage.uploadResult).toHaveText('notes.txt (12 bytes)');
  });

  test('report can be downloaded', async ({ page, playgroundPage }, testInfo) => {
    const downloadEvent = page.waitForEvent('download');
    await playgroundPage.downloadLink.click();
    const download = await downloadEvent;

    // testInfo.outputPath = a per-test folder in test-results/, nothing to clean up
    const file = testInfo.outputPath(download.suggestedFilename());
    await download.saveAs(file);

    expect(download.suggestedFilename()).toBe('products-report.csv');
    expect((await readFile(file, 'utf8')).split('\n')[0]).toBe('id,name,category,price');
  });

  test('lazy content appears only after scrolling', async ({ playgroundPage }) => {
    await expect(playgroundPage.lazyBox).toHaveText('Waiting to be seen…');

    await playgroundPage.scrollToLazyContent();
  });
});
