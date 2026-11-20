package com.compare.selenium.tests;

import com.compare.selenium.core.Browser;
import com.compare.selenium.pages.PlaygroundPage;
import com.compare.selenium.tags.BrowserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@BrowserApi
@DisplayName("Browser features: dialogs, frames, shadow DOM, tabs, files")
class PlaygroundTest extends BaseTest {

    private PlaygroundPage playground;

    @BeforeEach
    void openPlayground() {
        api.loginAndStartSession(user);
        playground = new PlaygroundPage(driver).open();
    }

    @Test
    void confirmDialogCanBeAccepted() {
        assertThat(playground.answerConfirm(true)).isEqualTo("Delete all data?");
        assertThat(playground.dialogResult()).isEqualTo("confirmed");
    }

    @Test
    void confirmDialogCanBeDismissed() {
        playground.answerConfirm(false);
        assertThat(playground.dialogResult()).isEqualTo("cancelled");
    }

    @Test
    void promptReceivesTypedText() {
        playground.answerPrompt("Captain");
        assertThat(playground.dialogResult()).isEqualTo("renamed to Captain");
    }

    @Test
    void messageCanBeSentFromIframe() {
        assertThat(playground.sendSupportMessage("Where is my order?")).isEqualTo("Thanks! We will reply soon.");
    }

    @Test
    void promoCodeInsideShadowDomIsApplied() {
        assertThat(playground.applyPromoCode("save10")).isEqualTo("Code applied: -10%");
    }

    @Test
    void receiptOpensInNewTabWithSharedLocalStorage() {
        assertThat(playground.readCustomerFromReceiptTab()).isEqualTo(user.username());
        assertThat(driver.getWindowHandles()).hasSize(1);
    }

    @Test
    void fileCanBeUploaded(@TempDir Path tmp) throws IOException {
        Path file = Files.writeString(tmp.resolve("notes.txt"), "hello upload");

        assertThat(playground.upload(file)).isEqualTo("notes.txt (12 bytes)");
    }

    @Test
    void reportCanBeDownloaded() throws IOException {
        Path report = playground.downloadReport(Browser.downloadDir());

        assertThat(report.getFileName().toString()).isEqualTo("products-report.csv");
        assertThat(Files.readAllLines(report)).first().isEqualTo("id,name,category,price");
    }

    @Test
    void lazyContentAppearsOnlyAfterScrolling() {
        assertThat(playground.lazyText()).isEqualTo("Waiting to be seen…");

        assertThat(playground.scrollToLazyContent()).isEqualTo("Lazy content loaded");
    }
}
