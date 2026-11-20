package com.compare.selenide.pages;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import java.time.Duration;

import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.Wait;

public class OrdersPage extends BasePage {

    private final ElementsCollection rows = $$("#orders tbody tr");
    private final SelenideElement loadMore = $("#load-more");
    private final SelenideElement refresh = $("#refresh");
    private final SelenideElement count = $("#orders-count");
    private final SelenideElement noOrders = $("#no-orders");

    public OrdersPage open() {
        Selenide.open("/orders.html");
        return waitLoaded();
    }

    OrdersPage waitLoaded() {
        waitUntilReady();
        count.shouldNotBe(empty);
        return this;
    }

    public ElementsCollection rows() {
        return rows;
    }

    public SelenideElement count() {
        return count;
    }

    public SelenideElement loadMoreButton() {
        return loadMore;
    }

    public SelenideElement noOrders() {
        return noOrders;
    }

    /** The test then asserts rows().shouldHave(size(10)) - that assertion is the wait. */
    public OrdersPage loadMore() {
        loadMore.click();
        return this;
    }

    public SelenideElement status(String orderId) {
        return $("tr[data-order-id='" + orderId + "'] .status");
    }

    /**
     * Selenide has no "retry an action until" helper -> fall back to Selenide.Wait() (a WebDriverWait).
     * has() checks ONCE without waiting - right tool inside a polling loop.
     */
    public OrdersPage refreshUntilStatus(String orderId, String expected, Duration timeout) {
        Wait().withTimeout(timeout).pollingEvery(Duration.ofSeconds(1)).until(d -> {
            if (status(orderId).has(exactText(expected))) {
                return true;
            }
            refresh.click();
            waitUntilReady();
            return false;
        });
        return this;
    }
}
