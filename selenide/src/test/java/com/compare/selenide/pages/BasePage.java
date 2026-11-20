package com.compare.selenide.pages;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.Wait;
import static com.codeborne.selenide.Selenide.executeJavaScript;

/**
 * Selenide page objects keep SelenideElement FIELDS: unlike a WebElement, a SelenideElement is only a lazy locator
 * that is re-found on every call -> no StaleElementReferenceException, and fields are safe.
 */
public abstract class BasePage {

    protected static final SelenideElement SPINNER = $("#spinner");

    public Header header() {
        return new Header();
    }

    public SelenideElement spinner() {
        return SPINNER;
    }

    /**
     * Selenide has no built-in "network idle". Selenide.Wait() gives the underlying WebDriverWait for any custom
     * condition; should(disappear) is the Selenide way to wait for the overlay.
     */
    protected void waitUntilReady() {
        Wait().until(d -> Boolean.TRUE.equals(executeJavaScript(
                "return document.readyState === 'complete' && (window.__pending || 0) === 0;")));
        SPINNER.should(disappear);
    }
}
