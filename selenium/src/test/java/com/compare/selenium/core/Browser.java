package com.compare.selenium.core;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-local WebDriver holder. Plain Selenium has no notion of "the current browser": with parallel tests every
 * thread needs its own driver, otherwise tests click in each other's windows.
 */
public final class Browser {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();
    private static final ThreadLocal<Path> DOWNLOADS = new ThreadLocal<>();
    private static final ThreadLocal<String> MAIN_WINDOW = new ThreadLocal<>();
    /** Every driver ever started - closed at the very end of the run (safety net for browser.reuse=true). */
    private static final List<WebDriver> ALL = new CopyOnWriteArrayList<>();

    private Browser() {
    }

    public static WebDriver driver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("No browser on thread " + Thread.currentThread().getName()
                    + ". Is the test annotated with @ExtendWith(BrowserExtension.class)?");
        }
        return driver;
    }

    public static Path downloadDir() {
        return DOWNLOADS.get();
    }

    static boolean isStarted() {
        return DRIVER.get() != null;
    }

    static void start() {
        Path downloads = Config.artifactsDir().resolve("downloads").resolve(UUID.randomUUID().toString());
        try {
            Files.createDirectories(downloads);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        WebDriver driver = DriverFactory.create(downloads);
        DRIVER.set(driver);
        DOWNLOADS.set(downloads);
        MAIN_WINDOW.set(driver.getWindowHandle());
        ALL.add(driver);
    }

    static void quit() {
        WebDriver driver = DRIVER.get();
        DRIVER.remove();
        DOWNLOADS.remove();
        MAIN_WINDOW.remove();
        if (driver != null) {
            ALL.remove(driver);
            driver.quit();
        }
    }

    /** browser.reuse=true: instead of a new browser, wipe what one test could leak into the next. */
    static void resetState() {
        WebDriver driver = driver();
        String main = MAIN_WINDOW.get();
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(main)) {
                driver.switchTo().window(handle).close();
            }
        }
        driver.switchTo().window(main);
        driver.get(Config.baseUrl() + "/blank.html"); // localStorage is per origin: we must be ON the origin
        ((JavascriptExecutor) driver).executeScript("localStorage.clear(); sessionStorage.clear();");
        driver.manage().deleteAllCookies();
    }

    static void quitAll() {
        for (WebDriver driver : ALL) {
            try {
                driver.quit();
            } catch (RuntimeException ignored) {
                // already dead
            }
        }
        ALL.clear();
    }
}
