package com.compare.playwright.pages;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class PlaygroundPage extends BasePage {

    private final Locator dialogResult;
    private final Locator uploadResult;
    private final Locator lazyBox;

    public PlaygroundPage(Page page) {
        super(page);
        this.dialogResult = page.locator("#dialog-result");
        this.uploadResult = page.locator("#upload-result");
        this.lazyBox = page.locator("#lazy-box");
    }

    public PlaygroundPage open() {
        page.navigate("/playground.html");
        waitUntilReady();
        return this;
    }

    private Locator button(String name) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name));
    }

    // ---------- native dialogs ----------

    /**
     * Event-based: register the handler BEFORE the action. Without a handler Playwright auto-DISMISSES every
     * dialog - confirm() silently returns false and the test checks the wrong branch.
     */
    public String answerConfirm(boolean accept) {
        AtomicReference<String> message = new AtomicReference<>();
        page.onceDialog(dialog -> {
            message.set(dialog.message());
            if (accept) {
                dialog.accept();
            } else {
                dialog.dismiss();
            }
        });
        button("Delete all data").click();
        assertThat(dialogResult).not().isEmpty(); // the dialog opens 0.5 s later; wait for its effect
        return message.get();
    }

    public void answerPrompt(String answer) {
        page.onceDialog(dialog -> dialog.accept(answer));
        button("Rename profile").click();
    }

    public Locator dialogResult() {
        return dialogResult;
    }

    // ---------- iframe ----------

    /** No switching: a FrameLocator scopes locators into the frame; the page itself stays usable. */
    public Locator sendSupportMessage(String message) {
        FrameLocator frame = page.frameLocator("#support-frame");
        frame.getByLabel("Message to support").fill(message);
        frame.getByRole(AriaRole.BUTTON, new FrameLocator.GetByRoleOptions().setName("Send")).click();
        return frame.locator("#sent");
    }

    // ---------- shadow DOM ----------

    /** CSS and role locators pierce OPEN shadow roots automatically - the widget looks like normal DOM. */
    public Locator applyPromoCode(String code) {
        page.getByPlaceholder("Promo code").fill(code);
        button("Apply").click();
        return page.locator("#promo .result");
    }

    public Locator promoWidget() {
        return page.locator("#promo");
    }

    // ---------- new tab ----------

    /** waitForPage wraps the action that opens the tab - the new Page object comes back ready to use. */
    public String readCustomerFromReceiptTab() {
        Page receipt = page.context().waitForPage(() -> page.getByText("Open receipt in a new tab").click());
        try {
            return receipt.locator("#receipt-user").textContent();
        } finally {
            receipt.close();
        }
    }

    // ---------- upload / download ----------

    public Locator upload(Path file) {
        page.locator("#file-input").setInputFiles(file);
        return uploadResult;
    }

    public Locator uploadResult() {
        return uploadResult;
    }

    /** First-class download API: wait for the Download event caused by the click, then save it where we want. */
    public Path downloadReport(Path targetDir) {
        Download download = page.waitForDownload(() -> page.getByText("Download products report (CSV)").click());
        Path target = targetDir.resolve(download.suggestedFilename());
        download.saveAs(target);
        return target;
    }

    // ---------- lazy content ----------

    public Locator lazyBox() {
        return lazyBox;
    }

    public Locator scrollToLazyContent() {
        lazyBox.scrollIntoViewIfNeeded();
        assertThat(lazyBox).hasText("Lazy content loaded");
        return lazyBox;
    }
}
