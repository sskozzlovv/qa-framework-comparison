package com.compare.selenium.core;

import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Level;

/**
 * Creates a configured WebDriver. In plain Selenium EVERYTHING is your job: browser options, download folder,
 * alert behaviour, timeouts, window size. (Selenium Manager at least downloads the matching driver binary.)
 */
final class DriverFactory {

    private DriverFactory() {
    }

    static WebDriver create(Path downloadDir) {
        WebDriver driver = switch (Config.browser()) {
            case "firefox" -> new FirefoxDriver(firefox(downloadDir));
            case "edge" -> new EdgeDriver(edge(downloadDir));
            default -> new ChromeDriver(chrome(downloadDir));
        };
        // NEVER mix implicit and explicit waits: an implicit wait makes every findElement inside
        // ExpectedConditions block, so "wait until invisible" can take timeout x timeout.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        // executeAsyncScript fails after this time if the callback was never called
        driver.manage().timeouts().scriptTimeout(Config.timeout());
        return driver;
    }

    private static ChromeOptions chrome(Path downloadDir) {
        ChromeOptions options = new ChromeOptions();
        if (Config.headless()) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1366,900", "--disable-search-engine-choice-screen");
        // Default is "dismiss and notify": an unexpected alert would be closed and the NEXT command would fail.
        // IGNORE leaves it open so the test decides (and the failure screenshot helper can report it).
        options.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.IGNORE);
        options.setExperimentalOption("prefs", Map.of(
                "download.default_directory", downloadDir.toAbsolutePath().toString(),
                "download.prompt_for_download", false,
                // headed Chrome shows "Change your password" popups for weak passwords like "secret"
                "credentials_enable_service", false,
                "profile.password_manager_enabled", false,
                "profile.password_manager_leak_detection", false));
        LoggingPreferences logs = new LoggingPreferences();
        logs.enable(LogType.BROWSER, Level.ALL); // JS console -> collected on failure
        options.setCapability("goog:loggingPrefs", logs);
        return options;
    }

    private static EdgeOptions edge(Path downloadDir) {
        EdgeOptions options = new EdgeOptions();
        if (Config.headless()) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1366,900");
        options.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.IGNORE);
        options.setExperimentalOption("prefs", Map.of(
                "download.default_directory", downloadDir.toAbsolutePath().toString(),
                "download.prompt_for_download", false));
        return options;
    }

    private static FirefoxOptions firefox(Path downloadDir) {
        FirefoxOptions options = new FirefoxOptions();
        if (Config.headless()) {
            options.addArguments("-headless");
        }
        options.addArguments("-width=1366", "-height=900");
        options.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.IGNORE);
        options.addPreference("browser.download.folderList", 2);
        options.addPreference("browser.download.dir", downloadDir.toAbsolutePath().toString());
        options.addPreference("browser.helperApps.neverAsk.saveToDisk", "text/csv");
        return options;
    }
}
