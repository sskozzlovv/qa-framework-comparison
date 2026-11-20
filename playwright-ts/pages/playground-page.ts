import { expect, type FrameLocator, type Locator, type Page } from '@playwright/test';
import { BasePage } from './base-page';

export class PlaygroundPage extends BasePage {
  readonly confirmButton: Locator;
  readonly promptButton: Locator;
  readonly dialogResult: Locator;
  readonly supportFrame: FrameLocator;
  readonly promoCode: Locator;
  readonly applyPromo: Locator;
  readonly promoResult: Locator;
  readonly promoWidget: Locator;
  readonly receiptLink: Locator;
  readonly fileInput: Locator;
  readonly uploadResult: Locator;
  readonly downloadLink: Locator;
  readonly lazyBox: Locator;

  constructor(page: Page) {
    super(page);
    this.confirmButton = page.getByRole('button', { name: 'Delete all data' });
    this.promptButton = page.getByRole('button', { name: 'Rename profile' });
    this.dialogResult = page.locator('#dialog-result');
    // no switchTo(): a FrameLocator scopes locators into the iframe
    this.supportFrame = page.frameLocator('#support-frame');
    // the promo widget lives in an open shadow root - locators pierce it without any special syntax
    this.promoCode = page.getByPlaceholder('Promo code');
    this.applyPromo = page.getByRole('button', { name: 'Apply' });
    this.promoResult = page.locator('promo-code .result');
    this.promoWidget = page.locator('#promo');
    this.receiptLink = page.getByRole('link', { name: 'Open receipt in a new tab' });
    this.fileInput = page.locator('#file-input');
    this.uploadResult = page.locator('#upload-result');
    this.downloadLink = page.getByRole('link', { name: 'Download products report (CSV)' });
    this.lazyBox = page.locator('#lazy-box');
  }

  async goto(): Promise<void> {
    await this.page.goto('/playground.html');
    await this.waitUntilReady();
  }

  async sendSupportMessage(text: string): Promise<void> {
    await this.supportFrame.getByLabel('Message to support').fill(text);
    await this.supportFrame.getByRole('button', { name: 'Send' }).click();
  }

  async applyPromoCode(code: string): Promise<void> {
    await this.promoCode.fill(code);
    await this.applyPromo.click();
  }

  async scrollToLazyContent(): Promise<void> {
    await this.lazyBox.scrollIntoViewIfNeeded();
    await expect(this.lazyBox).toHaveText('Lazy content loaded');
  }
}
