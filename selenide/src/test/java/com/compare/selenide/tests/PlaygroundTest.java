package com.compare.selenide.tests;

import com.compare.selenide.pages.PlaygroundPage;
import com.compare.selenide.tags.BrowserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static org.assertj.core.api.Assertions.assertThat;

@BrowserApi
@DisplayName("Browser features: dialogs, frames, shadow DOM, tabs, files")
class PlaygroundTest extends BaseTest {

    private PlaygroundPage playground;

    @BeforeEach
    void openPlayground() {
        api.loginAndStartSession(user);
        playground = new PlaygroundPage().open();
    }

    @Test
    void confirmDialogCanBeAccepted() {
        assertThat(playground.answerConfirm(true)).isEqualTo("Delete all data?");
        playground.dialogResult().shouldHave(exactText("confirmed"));
    }

    @Test
    void confirmDialogCanBeDismissed() {
        playground.answerConfirm(false);
        playground.dialogResult().shouldHave(exactText("cancelled"));
    }

    @Test
    void promptReceivesTypedText() {
        playground.answerPrompt("Captain");
        playground.dialogResult().shouldHave(exactText("renamed to Captain"));
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
        assertThat(getWebDriver().getWindowHandles()).hasSize(1);
    }

    @Test
    void fileCanBeUploaded(@TempDir Path tmp) throws IOException {
        Path file = Files.writeString(tmp.resolve("notes.txt"), "hello upload");

        playground.upload(file.toFile()).shouldHave(exactText("notes.txt (12 bytes)"));
    }

    @Test
    void reportCanBeDownloaded() throws IOException {
        File report = playground.downloadReport();

        assertThat(report.getName()).isEqualTo("products-report.csv");
        assertThat(Files.readAllLines(report.toPath())).first().isEqualTo("id,name,category,price");
    }

    @Test
    void lazyContentAppearsOnlyAfterScrolling() {
        playground.lazyBox().shouldHave(exactText("Waiting to be seen…"));

        playground.scrollToLazyContent();
    }
}
