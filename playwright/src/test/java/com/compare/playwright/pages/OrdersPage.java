package com.compare.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.time.Duration;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class OrdersPage extends BasePage {

    private final Locator rows;
    private final Locator loadMore;
    private final Locator refresh;
    private final Locator count;
    private final Locator noOrders;

    public OrdersPage(Page page) {
        super(page);
        this.rows = page.locator("#orders tbody tr");
        this.loadMore = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Load more"));
        this.refresh = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Refresh"));
        this.count = page.locator("#orders-count");
        this.noOrders = page.getByText("You have no orders yet.");
    }

    public OrdersPage open() {
        page.navigate("/orders.html");
        return waitLoaded();
    }

    OrdersPage waitLoaded() {
        waitUntilReady();
        assertThat(count).not().isEmpty();
        return this;
    }

    public Locator rows() {
        return rows;
    }

    public Locator count() {
        return count;
    }

    public Locator loadMoreButton() {
        return loadMore;
    }

    public Locator noOrders() {
        return noOrders;
    }

    public OrdersPage loadMore() {
        loadMore.click();
        return this;
    }

    public Locator status(String orderId) {
        return page.locator("tr[data-order-id='" + orderId + "'] .status");
    }

    /**
     * Playwright Test (TS) has expect(...).toPass() for "retry this block"; Java has no such helper,
     * so a plain loop: read, otherwise click Refresh and wait for the orders response.
     */
    public OrdersPage refreshUntilStatus(String orderId, String expected, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!expected.equals(status(orderId).textContent())) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Order " + orderId + " did not reach status " + expected + " in " + timeout);
            }
            page.waitForTimeout(1000); // polling interval, not a synchronization point
            page.waitForResponse(r -> r.url().contains("/api/orders"), refresh::click);
            assertThat(spinner).isHidden();
        }
        return this;
    }
}
