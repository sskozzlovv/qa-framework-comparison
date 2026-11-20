package com.compare.selenide.pages;

import com.compare.selenide.data.Product;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class CartPage extends BasePage {

    private final ElementsCollection items = $$(".cart-item");
    private final ElementsCollection names = $$(".cart-item .name");
    private final SelenideElement total = $("#cart-total");
    private final SelenideElement checkout = $("#checkout");
    private final SelenideElement empty = $("#empty-cart");

    public CartPage open() {
        Selenide.open("/cart.html");
        return waitLoaded();
    }

    CartPage waitLoaded() {
        waitUntilReady();
        return this;
    }

    public ElementsCollection names() {
        return names;
    }

    public SelenideElement total() {
        return total;
    }

    public SelenideElement checkoutButton() {
        return checkout;
    }

    public SelenideElement emptyMessage() {
        return empty;
    }

    /** disappear = hidden OR removed. During the fade-out the row is still visible, so this waits for removal. */
    public CartPage remove(Product product) {
        SelenideElement row = items.findBy(text(product.name()));
        row.$(".remove").click();
        row.should(disappear);
        return this;
    }

    public CheckoutPage checkout() {
        checkout.click();
        return new CheckoutPage().waitLoaded();
    }
}
