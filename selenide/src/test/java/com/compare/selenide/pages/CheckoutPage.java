package com.compare.selenide.pages;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import java.time.Duration;

import static com.codeborne.selenide.Condition.attributeMatching;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class CheckoutPage extends BasePage {

    private final SelenideElement firstName = $("#first-name");
    private final SelenideElement lastName = $("#last-name");
    private final SelenideElement zip = $("#zip");
    private final SelenideElement placeOrder = $("#place-order");
    private final SelenideElement summary = $("#summary-total");
    private final SelenideElement progress = $("#progress");
    private final SelenideElement confirmation = $("#confirmation");
    private final SelenideElement orderId = $("#order-id");
    private final SelenideElement viewOrders = $("#view-orders");

    public CheckoutPage open() {
        Selenide.open("/checkout.html");
        return waitLoaded();
    }

    CheckoutPage waitLoaded() {
        waitUntilReady();
        summary.shouldNotHave(exactText("…"));
        return this;
    }

    /** pressTab() = blur, which triggers the validation message. */
    public CheckoutPage fill(String first, String last, String zipCode) {
        firstName.setValue(first).pressTab();
        lastName.setValue(last).pressTab();
        zip.setValue(zipCode).pressTab();
        return this;
    }

    public SelenideElement zipError() {
        return $(".field-error[data-for='zip']");
    }

    public SelenideElement placeOrderButton() {
        return placeOrder;
    }

    public CheckoutPage placeOrder() {
        placeOrder.click();
        return this;
    }

    /** A condition on an attribute that changes over time, expressed as a regex: 50..100 %. */
    public CheckoutPage waitProgressAtLeastHalf() {
        progress.shouldHave(attributeMatching("aria-valuenow", "[5-9]\\d|100"));
        return this;
    }

    /** Per-call timeout override instead of raising the global Configuration.timeout. */
    public String waitForConfirmation() {
        confirmation.shouldBe(visible, Duration.ofSeconds(20));
        return orderId.text();
    }

    public OrdersPage viewOrders() {
        viewOrders.click();
        return new OrdersPage().waitLoaded();
    }
}
