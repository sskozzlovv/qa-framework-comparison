package com.compare.selenide.core;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.FileDownloadMode;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.CapabilityType;

import static com.codeborne.selenide.Selenide.clearBrowserCookies;
import static com.codeborne.selenide.Selenide.clearBrowserLocalStorage;
import static com.codeborne.selenide.Selenide.closeWebDriver;

/**
 * Compare with the Selenium module: no DriverFactory, no ThreadLocal, no download prefs, no screenshot code.
 * Selenide creates the browser lazily on the first open(), keeps one browser PER THREAD, and on a failed
 * should*() saves a screenshot + page source by itself.
 */
public class SelenideSetup implements BeforeAllCallback, BeforeEachCallback, AfterTestExecutionCallback, AfterEachCallback {

    private static boolean configured;

    static synchronized void configure() {
        if (configured) {
            return;
        }
        Configuration.baseUrl = Config.baseUrl();           // open("/catalog.html") is relative to it
        Configuration.browser = Config.browser();
        Configuration.headless = Config.headless();
        Configuration.browserSize = "1366x900";
        Configuration.timeout = Config.timeout().toMillis(); // default for every should*() / click()
        Configuration.pageLoadTimeout = 30_000;
        Configuration.reportsFolder = Config.artifactsDir().resolve("selenide-reports").toString();
        Configuration.screenshots = true;
        Configuration.savePageSource = true;
        Configuration.downloadsFolder = Config.artifactsDir().resolve("downloads").toString();
        // HTTPGET (default) re-downloads the href with its own HTTP client; FOLDER really clicks and watches the
        // browser download folder - closer to what a user does, works for JS-triggered downloads too.
        Configuration.fileDownload = FileDownloadMode.FOLDER;
        MutableCapabilities capabilities = new MutableCapabilities();
        capabilities.setCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR, "ignore");
        Configuration.browserCapabilities = capabilities;
        configured = true;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        configure(); // safety net when the LauncherSessionListener is not on the classpath
    }

    /** Runs in the test's own thread, before @BeforeEach of the test class. */
    @Override
    public void beforeEach(ExtensionContext context) {
        AllureSupport.attachSelenideSteps();
    }

    /** Right after the test body, browser still open: failure screenshot into Allure (non-Selenide failures). */
    @Override
    public void afterTestExecution(ExtensionContext context) {
        context.getExecutionException().ifPresent(AllureSupport::attachFailure);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (!Config.reuseBrowser()) {
            closeWebDriver(); // the next test of this thread gets a brand-new browser on its first open()
        } else if (WebDriverRunner.hasWebDriverStarted()) {
            clearBrowserCookies();
            clearBrowserLocalStorage();
        }
    }
}
