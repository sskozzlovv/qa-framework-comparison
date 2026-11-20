package com.compare.selenium.pages;

import com.compare.selenium.data.Product;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class CartPage extends BasePage {

    private static final By NAMES = By.cssSelector(".cart-item .name");
    private static final By TOTAL = By.id("cart-total");
    private static final By CHECKOUT = By.id("checkout");
    private static final By EMPTY = By.id("empty-cart");

    public CartPage(WebDriver driver) {
        super(driver);
    }

    private static String rowXpath(Product product) {
        return "//div[contains(@class,'cart-item')][.//span[@class='name' and normalize-space()='" + product.name() + "']]";
    }

    private static By row(Product product) {
        return By.xpath(rowXpath(product));
    }

    public CartPage open() {
        openPath("/cart.html");
        return waitLoaded();
    }

    CartPage waitLoaded() {
        waits.pageReady();
        return this;
    }

    public List<String> itemNames() {
        return driver.findElements(NAMES).stream().map(WebElement::getText).toList();
    }

    public String total() {
        return driver.findElement(TOTAL).getText();
    }

    /**
     * The row fades out for 0.5 s and is still "displayed" during the animation (opacity is not visibility for
     * WebDriver). invisibilityOfElementLocated waits until it is really removed from the DOM.
     */
    public CartPage remove(Product product) {
        waits.click(By.xpath(rowXpath(product) + "//button[contains(@class,'remove')]"));
        waits.gone(row(product));
        return this;
    }

    public boolean isCheckoutEnabled() {
        return driver.findElement(CHECKOUT).isEnabled();
    }

    public boolean isEmptyMessageVisible() {
        return driver.findElement(EMPTY).isDisplayed();
    }

    public CheckoutPage checkout() {
        waits.click(CHECKOUT);
        return new CheckoutPage(driver).waitLoaded();
    }
}
