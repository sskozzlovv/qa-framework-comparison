package com.compare.selenium.core;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/** Saves screenshot, HTML, URL and JS console of a failed test to target/artifacts/&lt;Class&gt;/&lt;test&gt;-&lt;time&gt;/. */
final class FailureArtifacts {

    private FailureArtifacts() {
    }

    static void collect(WebDriver driver, ExtensionContext context, Throwable error) {
        Path dir = Config.artifactsDir()
                .resolve(context.getRequiredTestClass().getSimpleName())
                .resolve(context.getRequiredTestMethod().getName() + "-"
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss-SSS")));
        try {
            Files.createDirectories(dir);
            StringBuilder info = new StringBuilder("Test: ").append(context.getDisplayName())
                    .append("\nError: ").append(error).append('\n');
            // An open alert makes EVERY WebDriver command throw UnhandledAlertException - screenshot included.
            try {
                String alert = driver.switchTo().alert().getText();
                info.append("Open alert (dismissed to take the screenshot): ").append(alert).append('\n');
                driver.switchTo().alert().dismiss();
            } catch (NoAlertPresentException ignored) {
                // normal case
            }
            info.append("URL: ").append(driver.getCurrentUrl()).append('\n');
            Files.write(dir.resolve("screenshot.png"), ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
            Files.writeString(dir.resolve("page.html"), driver.getPageSource(), StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("info.txt"), info, StandardCharsets.UTF_8);
            try {
                String console = driver.manage().logs().get(LogType.BROWSER).getAll().stream()
                        .map(LogEntry::toString).collect(Collectors.joining("\n"));
                Files.writeString(dir.resolve("console.log"), console, StandardCharsets.UTF_8);
            } catch (RuntimeException notSupported) {
                // Firefox does not support the legacy log API
            }
            System.out.println("[artifacts] " + dir.toAbsolutePath());
        } catch (IOException | RuntimeException e) {
            // never hide the real test failure behind a reporting problem
            System.err.println("[artifacts] could not save failure artifacts: " + e);
        }
    }
}
