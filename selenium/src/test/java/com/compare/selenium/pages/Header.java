package com.compare.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Top navigation bar, present on every page after login. */
public class Header extends BasePage {

    private static final By BADGE = By.id("cart-badge");
    private static final By USER = By.id("current-user");
    private static final By CART_LINK = By.id("nav-cart");
    private static final By ORDERS_LINK = By.id("nav-orders");
    private static final By LOGOUT = By.id("logout");

    Header(WebDriver driver) {
        super(driver);
    }

    @Override
    public Header header() {
        return this;
    }

    /** The badge is refreshed by a separate request AFTER the button already says "Added" - so wait for it. */
    public Header waitCartCount(int count) {
        if (count == 0) {
            waits.gone(BADGE);
        } else {
            waits.textToBe(BADGE, String.valueOf(count));
        }
        return this;
    }

    public String username() {
        return waits.visible(USER).getText();
    }

    public CartPage openCart() {
        waits.click(CART_LINK);
        return new CartPage(driver).waitLoaded();
    }

    public OrdersPage openOrders() {
        waits.click(ORDERS_LINK);
        return new OrdersPage(driver).waitLoaded();
    }

    public LoginPage logout() {
        waits.click(LOGOUT);
        return new LoginPage(driver).waitLoaded();
    }
}
