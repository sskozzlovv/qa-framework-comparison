package com.compare.playwright.tests;

import com.compare.playwright.pages.OrdersPage;
import com.compare.playwright.tags.Regression;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Regression
@DisplayName("Orders")
class OrdersTest extends BaseTest {

    @Test
    void ordersAreLoadedPageByPage() {
        api.loginAndStartSession(page.context(), user);
        api.seedOrders(user, 12);

        OrdersPage orders = new OrdersPage(page).open();
        assertThat(orders.count()).hasText("12 orders");
        assertThat(orders.rows()).hasCount(5);

        assertThat(orders.loadMore().rows()).hasCount(10);
        assertThat(orders.loadMore().rows()).hasCount(12);
        assertThat(orders.loadMoreButton()).isHidden();
    }

    @Test
    void newUserHasNoOrders() {
        api.loginAndStartSession(page.context(), user);

        OrdersPage orders = new OrdersPage(page).open();

        assertThat(orders.noOrders()).isVisible();
        assertThat(orders.rows()).hasCount(0);
    }
}
