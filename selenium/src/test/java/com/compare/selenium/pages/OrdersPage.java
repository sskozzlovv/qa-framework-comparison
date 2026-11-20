package com.compare.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class OrdersPage extends BasePage {

    private static final By ROWS = By.cssSelector("#orders tbody tr");
    private static final By LOAD_MORE = By.id("load-more");
    private static final By REFRESH = By.id("refresh");
    private static final By COUNT = By.id("orders-count");
    private static final By NO_ORDERS = By.id("no-orders");

    public OrdersPage(WebDriver driver) {
        super(driver);
    }

    private static By status(String orderId) {
        return By.cssSelector("tr[data-order-id='" + orderId + "'] .status");
    }

    public OrdersPage open() {
        openPath("/orders.html");
        return waitLoaded();
    }

    OrdersPage waitLoaded() {
        waits.pageReady();
        waits.until(d -> !d.findElement(COUNT).getText().isBlank());
        return this;
    }

    public int rowCount() {
        return driver.findElements(ROWS).size();
    }

    public String countText() {
        return driver.findElement(COUNT).getText();
    }

    public boolean isLoadMoreVisible() {
        return driver.findElements(LOAD_MORE).stream().anyMatch(e -> e.isDisplayed());
    }

    public boolean isNoOrdersVisible() {
        return driver.findElement(NO_ORDERS).isDisplayed();
    }

    /** Waits for the row count to GROW - not for a fixed sleep. */
    public OrdersPage loadMore() {
        int before = rowCount();
        waits.click(LOAD_MORE);
        waits.until(ExpectedConditions.numberOfElementsToBeMoreThan(ROWS, before));
        waits.pageReady();
        return this;
    }

    public String statusOf(String orderId) {
        return waits.visible(status(orderId)).getText();
    }

    /**
     * The page does not poll the server: the status changes only after "Refresh". So the wait itself performs an
     * action on every attempt - read, and if not there yet, click Refresh and try again in 1 s.
     */
    public OrdersPage refreshUntilStatus(String orderId, String expected, Duration timeout) {
        new WebDriverWait(driver, timeout, Duration.ofSeconds(1))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> {
                    if (expected.equals(d.findElement(status(orderId)).getText())) {
                        return true;
                    }
                    waits.click(REFRESH);
                    waits.pageReady();
                    return false;
                });
        return this;
    }
}
