package com.compare.selenide.tests;

import com.compare.selenide.pages.OrdersPage;
import com.compare.selenide.tags.Regression;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;

@Regression
@DisplayName("Orders")
class OrdersTest extends BaseTest {

    @Test
    void ordersAreLoadedPageByPage() {
        api.loginAndStartSession(user);
        api.seedOrders(user, 12);

        OrdersPage orders = new OrdersPage().open();
        orders.count().shouldHave(exactText("12 orders"));
        orders.rows().shouldHave(size(5));

        orders.loadMore().rows().shouldHave(size(10));
        orders.loadMore().rows().shouldHave(size(12));
        orders.loadMoreButton().shouldBe(hidden);
    }

    @Test
    void newUserHasNoOrders() {
        api.loginAndStartSession(user);

        OrdersPage orders = new OrdersPage().open();

        orders.noOrders().shouldBe(visible);
        orders.rows().shouldHave(size(0));
    }
}
