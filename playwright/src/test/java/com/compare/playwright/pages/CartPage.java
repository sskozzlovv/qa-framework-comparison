package com.compare.playwright.pages;

import com.compare.playwright.data.Product;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

public class CartPage extends BasePage {

    private final Locator items;
    private final Locator names;
    private final Locator total;
    private final Locator checkout;
    private final Locator empty;

    public CartPage(Page page) {
        super(page);
        this.items = page.locator(".cart-item");
        this.names = items.locator(".name");
        this.total = page.locator("#cart-total");
        this.checkout = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Checkout"));
        this.empty = page.getByText("Your cart is empty.");
    }

    public CartPage open() {
        page.navigate("/cart.html");
        return waitLoaded();
    }

    CartPage waitLoaded() {
        waitUntilReady();
        return this;
    }

    public Locator names() {
        return names;
    }

    public Locator total() {
        return total;
    }

    public Locator checkoutButton() {
        return checkout;
    }

    public Locator emptyMessage() {
        return empty;
    }

    /** waitFor(DETACHED): the row must be REMOVED from the DOM, not just faded out. */
    public CartPage remove(Product product) {
        Locator row = items.filter(new Locator.FilterOptions().setHasText(product.name()));
        row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Remove")).click();
        row.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
        return this;
    }

    public CheckoutPage checkout() {
        checkout.click();
        return new CheckoutPage(page).waitLoaded();
    }
}
