package com.compare.testng.tests;

import com.compare.testng.pages.PlaygroundPage;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static org.assertj.core.api.Assertions.assertThat;

@Test(groups = Groups.BROWSER_API)
public class PlaygroundTest extends BaseTest {

    /**
     * Test-level &#64;BeforeMethod. Runs AFTER BaseTest.createTestUser() (TestNG: base class configuration first).
     * alwaysRun - otherwise a groups filter in testng.xml could skip it.
     */
    @BeforeMethod(alwaysRun = true)
    public void openPlayground() {
        api().loginAndStartSession(user());
        new PlaygroundPage().open();
    }

    private PlaygroundPage playground() {
        return new PlaygroundPage(); // stateless page object: a fresh one per call is safe with parallel="methods"
    }

    @Test
    public void confirmDialogCanBeAccepted() {
        assertThat(playground().answerConfirm(true)).isEqualTo("Delete all data?");
        playground().dialogResult().shouldHave(exactText("confirmed"));
    }

    @Test
    public void confirmDialogCanBeDismissed() {
        playground().answerConfirm(false);
        playground().dialogResult().shouldHave(exactText("cancelled"));
    }

    @Test
    public void promptReceivesTypedText() {
        playground().answerPrompt("Captain");
        playground().dialogResult().shouldHave(exactText("renamed to Captain"));
    }

    @Test
    public void messageCanBeSentFromIframe() {
        assertThat(playground().sendSupportMessage("Where is my order?")).isEqualTo("Thanks! We will reply soon.");
    }

    @Test
    public void promoCodeInsideShadowDomIsApplied() {
        assertThat(playground().applyPromoCode("save10")).isEqualTo("Code applied: -10%");
    }

    @Test
    public void receiptOpensInNewTabWithSharedLocalStorage() {
        assertThat(playground().readCustomerFromReceiptTab()).isEqualTo(user().username());
        assertThat(getWebDriver().getWindowHandles()).hasSize(1);
    }

    /** JUnit has @TempDir; TestNG has no built-in temp folder - create and delete it yourself. */
    @Test
    public void fileCanBeUploaded() throws IOException {
        Path file = Files.createTempFile("notes", ".txt");
        try {
            Files.writeString(file, "hello upload");

            playground().upload(file.toFile()).shouldHave(exactText(file.getFileName() + " (12 bytes)"));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void reportCanBeDownloaded() throws IOException {
        File report = playground().downloadReport();

        assertThat(report.getName()).isEqualTo("products-report.csv");
        assertThat(Files.readAllLines(report.toPath())).first().isEqualTo("id,name,category,price");
    }

    @Test
    public void lazyContentAppearsOnlyAfterScrolling() {
        playground().lazyBox().shouldHave(exactText("Waiting to be seen…"));

        playground().scrollToLazyContent();
    }
}
