package com.compare.testng.pages;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.DownloadOptions;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import java.io.File;
import java.io.FileNotFoundException;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.shadowCss;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.closeWindow;
import static com.codeborne.selenide.Selenide.switchTo;
import static com.codeborne.selenide.files.FileFilters.withExtension;

public class PlaygroundPage extends BasePage {

    private final SelenideElement confirmButton = $("#confirm-btn");
    private final SelenideElement promptButton = $("#prompt-btn");
    private final SelenideElement dialogResult = $("#dialog-result");
    private final SelenideElement supportFrame = $("#support-frame");
    private final SelenideElement promoHost = $("#promo");
    private final SelenideElement openReceipt = $("#open-receipt");
    private final SelenideElement fileInput = $("#file-input");
    private final SelenideElement uploadResult = $("#upload-result");
    private final SelenideElement download = $("#download-report");
    private final SelenideElement lazyBox = $("#lazy-box");

    public PlaygroundPage open() {
        Selenide.open("/playground.html");
        waitUntilReady();
        return this;
    }

    /** Selenide.confirm()/dismiss() WAIT for the alert (it opens 0.5 s after the click) and return its text. */
    public String answerConfirm(boolean accept) {
        confirmButton.click();
        return accept ? Selenide.confirm() : Selenide.dismiss();
    }

    public void answerPrompt(String answer) {
        promptButton.click();
        Selenide.prompt(answer);
    }

    public SelenideElement dialogResult() {
        return dialogResult;
    }

    /** switchTo().frame() waits for the frame; we still have to switch back ourselves. */
    public String sendSupportMessage(String message) {
        switchTo().frame(supportFrame);
        try {
            $("#message").setValue(message);
            $("#send").click();
            return $("#sent").shouldBe(visible).text();
        } finally {
            switchTo().defaultContent();
        }
    }

    /** shadowCss(target, host): a locator that pierces the shadow root - still lazy and auto-waiting. */
    public String applyPromoCode(String code) {
        $(shadowCss("#code", "#promo")).setValue(code);
        $(shadowCss("#apply", "#promo")).click();
        return $(shadowCss(".result", "#promo")).shouldNotBe(empty).text();
    }

    public SelenideElement promoWidget() {
        return promoHost;
    }

    /** switchTo().window(1) waits until the second window exists. */
    public String readCustomerFromReceiptTab() {
        openReceipt.click();
        switchTo().window(1);
        try {
            return $("#receipt-user").shouldBe(visible).text();
        } finally {
            closeWindow();
            switchTo().window(0);
        }
    }

    public SelenideElement upload(File file) {
        fileInput.uploadFile(file);
        return uploadResult;
    }

    public SelenideElement uploadResult() {
        return uploadResult;
    }

    /**
     * One line instead of browser prefs + folder polling. Mode from SelenideConfig: FOLDER (a real click + watching the
     * download folder) for a local browser, HTTPGET for Selenium Grid (the folder is on the node).
     */
    public File downloadReport() throws FileNotFoundException {
        return download.download(DownloadOptions.using(Configuration.fileDownload).withFilter(withExtension("csv")));
    }

    public SelenideElement lazyBox() {
        return lazyBox;
    }

    public SelenideElement scrollToLazyContent() {
        lazyBox.scrollIntoView("{block: 'center'}");
        return lazyBox.shouldHave(exactText("Lazy content loaded"));
    }
}
