package com.compare.playwright.tests;

import com.compare.playwright.pages.PlaygroundPage;
import com.compare.playwright.tags.BrowserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@BrowserApi
@DisplayName("Browser features: dialogs, frames, shadow DOM, tabs, files")
class PlaygroundTest extends BaseTest {

    private PlaygroundPage playground;

    @BeforeEach
    void openPlayground() {
        api.loginAndStartSession(page.context(), user);
        playground = new PlaygroundPage(page).open();
    }

    @Test
    void confirmDialogCanBeAccepted() {
        org.assertj.core.api.Assertions.assertThat(playground.answerConfirm(true)).isEqualTo("Delete all data?");
        assertThat(playground.dialogResult()).hasText("confirmed");
    }

    @Test
    void confirmDialogCanBeDismissed() {
        playground.answerConfirm(false);
        assertThat(playground.dialogResult()).hasText("cancelled");
    }

    @Test
    void promptReceivesTypedText() {
        playground.answerPrompt("Captain");
        assertThat(playground.dialogResult()).hasText("renamed to Captain");
    }

    @Test
    void messageCanBeSentFromIframe() {
        assertThat(playground.sendSupportMessage("Where is my order?")).hasText("Thanks! We will reply soon.");
    }

    @Test
    void promoCodeInsideShadowDomIsApplied() {
        assertThat(playground.applyPromoCode("save10")).hasText("Code applied: -10%");
    }

    @Test
    void receiptOpensInNewTabWithSharedLocalStorage() {
        org.assertj.core.api.Assertions.assertThat(playground.readCustomerFromReceiptTab()).isEqualTo(user.username());
        org.assertj.core.api.Assertions.assertThat(page.context().pages()).hasSize(1);
    }

    @Test
    void fileCanBeUploaded(@TempDir Path tmp) throws IOException {
        Path file = Files.writeString(tmp.resolve("notes.txt"), "hello upload");

        assertThat(playground.upload(file)).hasText("notes.txt (12 bytes)");
    }

    @Test
    void reportCanBeDownloaded(@TempDir Path tmp) throws IOException {
        Path report = playground.downloadReport(tmp);

        org.assertj.core.api.Assertions.assertThat(report.getFileName().toString()).isEqualTo("products-report.csv");
        org.assertj.core.api.Assertions.assertThat(Files.readAllLines(report)).first().isEqualTo("id,name,category,price");
    }

    @Test
    void lazyContentAppearsOnlyAfterScrolling() {
        assertThat(playground.lazyBox()).hasText("Waiting to be seen…");

        playground.scrollToLazyContent();
    }
}
