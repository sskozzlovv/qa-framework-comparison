package com.compare.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class CheckoutPage extends BasePage {

    private final Locator firstName;
    private final Locator lastName;
    private final Locator zip;
    private final Locator placeOrder;
    private final Locator summary;
    private final Locator progress;
    private final Locator confirmation;
    private final Locator orderId;

    public CheckoutPage(Page page) {
        super(page);
        this.firstName = page.getByLabel("First name");
        this.lastName = page.getByLabel("Last name");
        this.zip = page.getByLabel("ZIP code");
        this.placeOrder = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Place order"));
        this.summary = page.locator("#summary-total");
        this.progress = page.locator("#progress");
        this.confirmation = page.locator("#confirmation");
        this.orderId = page.locator("#order-id");
    }

    public CheckoutPage open() {
        page.navigate("/checkout.html");
        return waitLoaded();
    }

    CheckoutPage waitLoaded() {
        waitUntilReady();
        assertThat(summary).not().hasText("…");
        return this;
    }

    /** fill() sets the value and fires "input"; blur() triggers the validation message. */
    public CheckoutPage fill(String first, String last, String zipCode) {
        firstName.fill(first);
        firstName.blur();
        lastName.fill(last);
        lastName.blur();
        zip.fill(zipCode);
        zip.blur();
        return this;
    }

    public Locator zipError() {
        return page.locator(".field-error[data-for='zip']");
    }

    public Locator placeOrderButton() {
        return placeOrder;
    }

    public CheckoutPage placeOrder() {
        placeOrder.click();
        return this;
    }

    public CheckoutPage waitProgressAtLeastHalf() {
        assertThat(progress).hasAttribute("aria-valuenow", Pattern.compile("[5-9]\\d|100"));
        return this;
    }

    public String waitForConfirmation() {
        assertThat(confirmation).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20_000));
        return orderId.textContent();
    }

    public OrdersPage viewOrders() {
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("View my orders")).click();
        return new OrdersPage(page).waitLoaded();
    }
}
