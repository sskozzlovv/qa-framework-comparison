package com.compare.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

public class CheckoutPage extends BasePage {

    public static final By FIRST_NAME = By.id("first-name");
    public static final By LAST_NAME = By.id("last-name");
    public static final By ZIP = By.id("zip");
    private static final By PLACE_ORDER = By.id("place-order");
    private static final By SUMMARY = By.id("summary-total");
    private static final By PROGRESS = By.id("progress");
    private static final By CONFIRMATION = By.id("confirmation");
    private static final By ORDER_ID = By.id("order-id");
    private static final By VIEW_ORDERS = By.id("view-orders");

    public CheckoutPage(WebDriver driver) {
        super(driver);
    }

    public CheckoutPage open() {
        openPath("/checkout.html");
        return waitLoaded();
    }

    CheckoutPage waitLoaded() {
        waits.pageReady();
        waits.until(d -> !d.findElement(SUMMARY).getText().equals("…"));
        return this;
    }

    public CheckoutPage fill(String firstName, String lastName, String zip) {
        return fillField(FIRST_NAME, firstName).fillField(LAST_NAME, lastName).fillField(ZIP, zip);
    }

    /** TAB = blur: the app shows a validation message only for fields the user has left. */
    public CheckoutPage fillField(By field, String value) {
        waits.type(field, value);
        driver.findElement(field).sendKeys(Keys.TAB);
        return this;
    }

    public String errorOf(By field) {
        String id = driver.findElement(field).getDomAttribute("id");
        return driver.findElement(By.cssSelector(".field-error[data-for='" + id + "']")).getText();
    }

    public boolean isPlaceOrderEnabled() {
        return driver.findElement(PLACE_ORDER).isEnabled();
    }

    public CheckoutPage placeOrder() {
        waits.click(PLACE_ORDER);
        return this;
    }

    /** Custom wait on an ATTRIBUTE that changes over time: aria-valuenow of the progress bar. */
    public CheckoutPage waitProgressAtLeast(int percent) {
        waits.until(d -> {
            String value = d.findElement(PROGRESS).getDomAttribute("aria-valuenow");
            return value != null && Integer.parseInt(value) >= percent;
        });
        return this;
    }

    /** Placing an order takes 2-3 s (more for slow users) -> a longer timeout just for this step. */
    public String waitForConfirmation() {
        waits.until(ExpectedConditions.visibilityOfElementLocated(CONFIRMATION), Duration.ofSeconds(20));
        return driver.findElement(ORDER_ID).getText();
    }

    public OrdersPage viewOrders() {
        waits.click(VIEW_ORDERS);
        return new OrdersPage(driver).waitLoaded();
    }
}
