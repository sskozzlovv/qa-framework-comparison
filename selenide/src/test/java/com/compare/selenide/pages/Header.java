package com.compare.selenide.pages;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

/** Page objects return elements; the TEST asserts on them with should*() - the assertion itself waits. */
public class Header {

    private final SelenideElement badge = $("#cart-badge");
    private final SelenideElement username = $("#current-user");
    private final SelenideElement cartLink = $("#nav-cart");
    private final SelenideElement ordersLink = $("#nav-orders");

    public SelenideElement cartBadge() {
        return badge;
    }

    public SelenideElement username() {
        return username;
    }

    public CartPage openCart() {
        cartLink.click();
        return new CartPage().waitLoaded();
    }

    public OrdersPage openOrders() {
        ordersLink.click();
        return new OrdersPage().waitLoaded();
    }
}
