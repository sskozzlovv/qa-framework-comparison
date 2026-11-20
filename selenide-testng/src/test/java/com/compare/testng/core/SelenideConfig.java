package com.compare.testng.core;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.FileDownloadMode;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.CapabilityType;

/**
 * The same Selenide settings as in the JUnit module (SelenideSetup). Called once from &#64;BeforeSuite in BaseTest:
 * Configuration is a global static, so it must be written before the parallel test threads start.
 */
public final class SelenideConfig {

    private static boolean configured;

    private SelenideConfig() {
    }

    public static synchronized void configure() {
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
        Configuration.fileDownload = FileDownloadMode.FOLDER;
        if (!Config.remote().isBlank()) {
            // Selenium Grid: one line - Selenide creates a RemoteWebDriver and sends uploaded files to the node itself.
            Configuration.remote = Config.remote();
            // FOLDER watches the browser's download folder - on the Grid node, not here. HTTPGET downloads the
            // link's href with the browser's cookies from this machine instead.
            Configuration.fileDownload = FileDownloadMode.HTTPGET;
        }
        MutableCapabilities capabilities = new MutableCapabilities();
        capabilities.setCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR, "ignore");
        Configuration.browserCapabilities = capabilities;
        configured = true;
    }
}
