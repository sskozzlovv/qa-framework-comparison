package com.compare.selenium.tests;

import com.compare.selenium.pages.OrdersPage;
import com.compare.selenium.tags.Regression;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Regression
@DisplayName("Orders")
class OrdersTest extends BaseTest {

    @Test
    void ordersAreLoadedPageByPage() {
        api.loginAndStartSession(user);
        api.seedOrders(user, 12); // test data through the test API, removed by cleanup in @AfterEach

        OrdersPage orders = new OrdersPage(driver).open();
        assertThat(orders.countText()).isEqualTo("12 orders");
        assertThat(orders.rowCount()).isEqualTo(5);

        orders.loadMore();
        assertThat(orders.rowCount()).isEqualTo(10);

        orders.loadMore();
        assertThat(orders.rowCount()).isEqualTo(12);
        assertThat(orders.isLoadMoreVisible()).isFalse();
    }

    @Test
    void newUserHasNoOrders() {
        api.loginAndStartSession(user);

        OrdersPage orders = new OrdersPage(driver).open();

        assertThat(orders.isNoOrdersVisible()).isTrue();
        assertThat(orders.rowCount()).isZero();
    }
}
