package com.compare.selenium.core;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.function.Function;

/**
 * Explicit waits. Plain Selenium does NOT wait for anything by itself: findElement fails immediately,
 * click() clicks whatever is on top (or throws ElementClickInterceptedException), getText() reads the current text.
 */
public final class Waits {

    /** The full-page loading overlay of the app. */
    public static final By SPINNER = By.id("spinner");

    private final WebDriver driver;

    public Waits(WebDriver driver) {
        this.driver = driver;
    }

    public WebDriverWait wait(Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout, Duration.ofMillis(100));
        wait.ignoring(StaleElementReferenceException.class); // the DOM was re-rendered between find and check -> retry
        return wait;
    }

    public <T> T until(Function<WebDriver, T> condition) {
        return wait(Config.timeout()).until(condition);
    }

    public <T> T until(ExpectedCondition<T> condition, Duration timeout) {
        return wait(timeout).until(condition);
    }

    public WebElement visible(By locator) {
        return until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** True when the element is absent OR hidden. */
    public void gone(By locator) {
        until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public void spinnerGone() {
        gone(SPINNER);
    }

    /** Custom condition through JS: page loaded and no fetch() in flight (the app counts them in window.__pending). */
    public void networkIdle() {
        until(d -> (Boolean) ((JavascriptExecutor) d).executeScript(
                "return document.readyState === 'complete' && (window.__pending || 0) === 0;"));
    }

    /** Network idle + no overlay = the page can be used. */
    public void pageReady() {
        networkIdle();
        spinnerGone();
    }

    /**
     * "elementToBeClickable" only checks displayed+enabled; it does NOT know that a transparent overlay or a modal
     * lies on top. So: find, check, click - and retry the whole thing while the click is intercepted or the
     * element went stale.
     */
    public void click(By locator) {
        wait(Config.timeout())
                .ignoring(ElementClickInterceptedException.class)
                .ignoring(ElementNotInteractableException.class)
                .until(d -> {
                    WebElement element = d.findElement(locator);
                    if (!element.isDisplayed() || !element.isEnabled()) {
                        return false;
                    }
                    element.click();
                    return true;
                });
    }

    public void type(By locator, String text) {
        WebElement element = visible(locator);
        element.clear();
        element.sendKeys(text);
    }

    public void textToBe(By locator, String text) {
        until(ExpectedConditions.textToBe(locator, text));
    }

    public void stale(WebElement element) {
        until(ExpectedConditions.stalenessOf(element));
    }
}
