package com.compare.selenium.pages;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;

public class PlaygroundPage extends BasePage {

    private static final By CONFIRM_BTN = By.id("confirm-btn");
    private static final By PROMPT_BTN = By.id("prompt-btn");
    private static final By DIALOG_RESULT = By.id("dialog-result");
    private static final By SUPPORT_FRAME = By.id("support-frame");
    private static final By PROMO_HOST = By.id("promo");
    private static final By OPEN_RECEIPT = By.id("open-receipt");
    private static final By FILE_INPUT = By.id("file-input");
    private static final By UPLOAD_RESULT = By.id("upload-result");
    private static final By DOWNLOAD = By.id("download-report");
    private static final By LAZY_BOX = By.id("lazy-box");

    public PlaygroundPage(WebDriver driver) {
        super(driver);
    }

    public PlaygroundPage open() {
        openPath("/playground.html");
        waits.pageReady();
        return this;
    }

    // ---------- native dialogs ----------

    /** The dialog opens 0.5 s after the click: switchTo().alert() right away would throw NoAlertPresentException. */
    public String answerConfirm(boolean accept) {
        waits.click(CONFIRM_BTN);
        Alert alert = waits.until(ExpectedConditions.alertIsPresent());
        String text = alert.getText();
        if (accept) {
            alert.accept();
        } else {
            alert.dismiss();
        }
        return text;
    }

    public void answerPrompt(String answer) {
        waits.click(PROMPT_BTN);
        Alert alert = waits.until(ExpectedConditions.alertIsPresent());
        alert.sendKeys(answer);
        alert.accept();
    }

    public String dialogResult() {
        return waits.until(d -> {
            String text = d.findElement(DIALOG_RESULT).getText();
            return text.isBlank() ? null : text;
        });
    }

    // ---------- iframe ----------

    /** Inside a frame the driver sees ONLY the frame's DOM. Always switch back - finally! */
    public String sendSupportMessage(String message) {
        waits.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(SUPPORT_FRAME));
        try {
            waits.type(By.id("message"), message);
            waits.click(By.id("send"));
            return waits.visible(By.id("sent")).getText();
        } finally {
            driver.switchTo().defaultContent();
        }
    }

    // ---------- shadow DOM ----------

    /** getShadowRoot() returns a SearchContext that supports CSS selectors only (no XPath!). */
    public String applyPromoCode(String code) {
        SearchContext shadow = waits.visible(PROMO_HOST).getShadowRoot();
        shadow.findElement(By.cssSelector("#code")).sendKeys(code);
        shadow.findElement(By.cssSelector("#apply")).click();
        return waits.until(d -> {
            String text = d.findElement(PROMO_HOST).getShadowRoot().findElement(By.cssSelector(".result")).getText();
            return text.isBlank() ? null : text;
        });
    }

    public WebElement promoWidget() {
        return driver.findElement(PROMO_HOST);
    }

    // ---------- new tab ----------

    /** target=_blank opens a new window handle; WebDriver does NOT switch to it automatically. */
    public String readCustomerFromReceiptTab() {
        String original = driver.getWindowHandle();
        waits.click(OPEN_RECEIPT);
        waits.until(ExpectedConditions.numberOfWindowsToBe(2));
        String receipt = driver.getWindowHandles().stream().filter(h -> !h.equals(original)).findFirst().orElseThrow();
        driver.switchTo().window(receipt);
        try {
            return waits.visible(By.id("receipt-user")).getText();
        } finally {
            driver.close();
            driver.switchTo().window(original);
        }
    }

    // ---------- upload / download ----------

    /** Upload = sendKeys(absolute path) into input[type=file]. (Remote grid: needs LocalFileDetector.) */
    public String upload(Path file) {
        driver.findElement(FILE_INPUT).sendKeys(file.toAbsolutePath().toString());
        return waits.until(d -> {
            String text = d.findElement(UPLOAD_RESULT).getText();
            return text.equals("nothing") ? null : text;
        });
    }

    /**
     * Selenium has no download API: configure the browser's download dir (DriverFactory), click, then poll the
     * folder until the file is complete (Chrome writes *.crdownload first).
     */
    public Path downloadReport(Path downloadDir) {
        waits.click(DOWNLOAD);
        return waits.until(d -> {
            try (Stream<Path> files = Files.list(downloadDir)) {
                return files.filter(f -> f.toString().endsWith(".csv")).findFirst().orElse(null);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    // ---------- lazy content ----------

    public String lazyText() {
        return driver.findElement(LAZY_BOX).getText();
    }

    public String scrollToLazyContent() {
        js.scrollIntoView(driver.findElement(LAZY_BOX));
        waits.until(ExpectedConditions.textToBe(LAZY_BOX, "Lazy content loaded"), Duration.ofSeconds(5));
        return lazyText();
    }
}
