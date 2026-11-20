package com.compare.selenium.pages;

import com.compare.selenium.core.Config;
import com.compare.selenium.core.Js;
import com.compare.selenium.core.Waits;
import org.openqa.selenium.WebDriver;

/** Page objects hold locators (By, never cached WebElements!) and actions that wait for the page to settle. */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final Waits waits;
    protected final Js js;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.waits = new Waits(driver);
        this.js = new Js(driver);
    }

    protected void openPath(String path) {
        driver.get(Config.baseUrl() + path);
    }

    public Header header() {
        return new Header(driver);
    }

    public boolean isSpinnerVisible() {
        return driver.findElements(Waits.SPINNER).stream().anyMatch(e -> e.isDisplayed());
    }
}
