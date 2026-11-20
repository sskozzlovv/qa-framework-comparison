package com.compare.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Locators are lazy (like SelenideElement): they are resolved on every action, so they can be fields.
 * Every action auto-waits for "actionability": attached, visible, stable (no animation), enabled and
 * NOT COVERED by another element. That removes most explicit waits you need in Selenium.
 */
public abstract class BasePage {

    protected final Page page;
    protected final Locator spinner;

    protected BasePage(Page page) {
        this.page = page;
        this.spinner = page.locator("#spinner");
    }

    public Header header() {
        return new Header(page);
    }

    public Locator spinner() {
        return spinner;
    }

    /** waitForFunction polls a JS predicate in the page; assertThat(...).isHidden() is a retrying assertion. */
    protected void waitUntilReady() {
        page.waitForFunction("() => document.readyState === 'complete' && (window.__pending || 0) === 0");
        assertThat(spinner).isHidden();
    }
}
