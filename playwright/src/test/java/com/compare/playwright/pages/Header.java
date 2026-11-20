package com.compare.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class Header {

    private final Page page;
    private final Locator badge;
    private final Locator username;

    Header(Page page) {
        this.page = page;
        this.badge = page.locator("#cart-badge");
        this.username = page.locator("#current-user");
    }

    public Locator cartBadge() {
        return badge;
    }

    public Locator username() {
        return username;
    }

    public CartPage openCart() {
        page.locator("#nav-cart").click();
        return new CartPage(page).waitLoaded();
    }

    public OrdersPage openOrders() {
        page.locator("#nav-orders").click();
        return new OrdersPage(page).waitLoaded();
    }
}
