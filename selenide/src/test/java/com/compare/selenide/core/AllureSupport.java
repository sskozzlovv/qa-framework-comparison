package com.compare.selenide.core;

import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.ex.UIAssertionError;
import com.codeborne.selenide.logevents.SelenideLogger;
import io.qameta.allure.Allure;
import io.qameta.allure.selenide.AllureSelenide;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Allure glue. The test results themselves are written by allure-junit5, which JUnit finds on the classpath by
 * itself (ServiceLoader) - no code for that.
 */
public final class AllureSupport {

    private static final String LISTENER = "AllureSelenide";

    private AllureSupport() {
    }

    /**
     * Every Selenide action ($.click(), shouldHave(), open()...) becomes a report step; a failed step gets a
     * screenshot and the page HTML. SelenideLogger keeps listeners in a plain ThreadLocal, so this must run in the
     * thread of EVERY test (parallel threads would get no steps if it ran once at start-up).
     */
    public static void attachSelenideSteps() {
        if (!SelenideLogger.hasListener(LISTENER)) {
            SelenideLogger.addListener(LISTENER, new AllureSelenide().screenshots(true).savePageSource(true));
        }
    }

    /**
     * Screenshot + page HTML of a failed test into the report. AllureSelenide already attaches them to a failed
     * Selenide step (should*(), click()...), but NOT when the test fails elsewhere - e.g. an AssertJ assertion.
     * Must be called while the browser is still open, i.e. before the browser is closed after the test.
     */
    public static void attachFailure(Throwable error) {
        if (error instanceof UIAssertionError || !WebDriverRunner.hasWebDriverStarted()) {
            return;
        }
        byte[] png = ((TakesScreenshot) WebDriverRunner.getWebDriver()).getScreenshotAs(OutputType.BYTES);
        Allure.addAttachment("Screenshot on failure", "image/png", new ByteArrayInputStream(png), "png");
        byte[] html = WebDriverRunner.source().getBytes(StandardCharsets.UTF_8);
        Allure.addAttachment("Page source on failure", "text/html", new ByteArrayInputStream(html), "html");
    }

    /**
     * Results are plain files that survive between runs (mvn test without clean): old ones would show up in the
     * report as retries of the new tests. Called once, before any test.
     */
    public static void cleanResults() {
        String dir = System.getProperty("allure.results.directory");
        if (dir == null || dir.isBlank() || !Files.isDirectory(Path.of(dir))) {
            return;
        }
        try (Stream<Path> files = Files.walk(Path.of(dir))) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
