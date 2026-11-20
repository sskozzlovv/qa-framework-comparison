package com.compare.selenium.pages;

import com.compare.selenium.data.Product;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;
import java.util.List;

public class CatalogPage extends BasePage {

    private static final By CARDS = By.cssSelector(".product-card");
    private static final By NAMES = By.cssSelector(".product-card .name");
    private static final By PRICES = By.cssSelector(".product-card .price");
    private static final By SEARCH = By.id("search");
    private static final By SORT = By.id("sort");
    private static final By RESULT_COUNT = By.id("result-count");
    private static final By NO_RESULTS = By.id("no-results");
    private static final By LOAD_ERROR = By.id("load-error");
    private static final By PROMO = By.id("promo-modal");
    private static final By PROMO_CLOSE = By.id("promo-close");
    private static final By TOASTS = By.cssSelector(".toast");

    public CatalogPage(WebDriver driver) {
        super(driver);
    }

    /** XPath because we match a card by the TEXT of a child element. */
    private static By addButton(Product product) {
        return By.xpath("//div[contains(@class,'product-card')][.//h3[normalize-space()='" + product.name() + "']]//button");
    }

    public CatalogPage open() {
        openPath("/catalog.html");
        return waitLoaded();
    }

    public CatalogPage waitLoaded() {
        waits.until(ExpectedConditions.urlContains("/catalog.html"), Duration.ofSeconds(15));
        waits.pageReady();
        waits.until(d -> !d.findElement(RESULT_COUNT).getText().isBlank());
        waitCardsVisible();
        return this;
    }

    /**
     * Pitfall found by a flaky run: new cards fade in (opacity 0 -> 1 in 0.35 s), and WebDriver treats opacity 0
     * as NOT displayed - getText() returns "" for them. Wait until every card is really visible.
     */
    private void waitCardsVisible() {
        waits.until(d -> d.findElements(CARDS).stream().allMatch(WebElement::isDisplayed));
    }

    /**
     * Pitfall: the search is debounced (400 ms) - right after typing the OLD list is still on the screen and the
     * spinner has not even appeared, so "wait for spinner to disappear" passes immediately and we read old data.
     * Fix: remember an element of the old list and wait until it is detached from the DOM (staleness),
     * then wait for the page to settle.
     */
    public CatalogPage search(String query) {
        WebElement oldFirstCard = driver.findElements(CARDS).stream().findFirst().orElseThrow();
        waits.type(SEARCH, query);
        waits.stale(oldFirstCard);
        waits.pageReady();
        waitCardsVisible();
        return this;
    }

    public CatalogPage sortBy(String value) {
        WebElement oldFirstCard = driver.findElements(CARDS).stream().findFirst().orElseThrow();
        new Select(driver.findElement(SORT)).selectByValue(value);
        waits.stale(oldFirstCard);
        waits.pageReady();
        waitCardsVisible();
        return this;
    }

    public List<String> productNames() {
        return driver.findElements(NAMES).stream().map(WebElement::getText).toList();
    }

    public List<Integer> pricesInCents() {
        return driver.findElements(PRICES).stream()
                .map(e -> Integer.parseInt(e.getText().replace("$", "").replace(".", "")))
                .toList();
    }

    public boolean isNoResultsVisible() {
        return driver.findElement(NO_RESULTS).isDisplayed();
    }

    public String loadError() {
        return waits.visible(LOAD_ERROR).getText();
    }

    /** Clicks and waits for the result of the click - the button turns into "Added". */
    public CatalogPage addToCart(Product product) {
        waits.click(addButton(product));
        waits.textToBe(addButton(product), "Added");
        return this;
    }

    /** Raw WebElement.click(): no waiting, no retry - used to SHOW what happens when a modal covers the page. */
    public void clickAddToCartWithoutWaiting(Product product) {
        driver.findElement(addButton(product)).click();
    }

    public WebElement firstCard() {
        return driver.findElements(CARDS).getFirst();
    }

    public WebElement addButtonOf(Product product) {
        return driver.findElement(addButton(product));
    }

    /** Several toasts may be stacked - we wait for the newest one to have the expected text. */
    public CatalogPage waitForToast(String text) {
        waits.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector(".toast:last-child"), text));
        return this;
    }

    public CatalogPage waitToastsGone() {
        waits.until(d -> d.findElements(TOASTS).isEmpty());
        return this;
    }

    public CatalogPage waitForPromo() {
        waits.until(ExpectedConditions.visibilityOfElementLocated(PROMO), Duration.ofSeconds(5));
        return this;
    }

    public CatalogPage closePromo() {
        waits.click(PROMO_CLOSE);
        waits.gone(PROMO);
        return this;
    }
}
