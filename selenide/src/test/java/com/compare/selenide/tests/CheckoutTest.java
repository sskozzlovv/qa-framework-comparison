package com.compare.selenide.tests;

import com.compare.selenide.data.TestUser;
import com.compare.selenide.pages.CatalogPage;
import com.compare.selenide.pages.CheckoutPage;
import com.compare.selenide.pages.OrdersPage;
import com.compare.selenide.tags.Critical;
import com.compare.selenide.tags.Regression;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.hidden;
import static com.compare.selenide.data.Product.BACKPACK;
import static com.compare.selenide.data.Product.CAMP_MUG;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Checkout")
class CheckoutTest extends BaseTest {

    @Test
    @Critical
    void userBuysProductsAndOrderGetsCompleted() {
        CatalogPage catalog = loginViaApi().addToCart(BACKPACK).addToCart(CAMP_MUG);
        catalog.header().cartBadge().shouldHave(exactText("2"));
        CheckoutPage checkout = catalog.header().openCart().checkout();
        checkout.placeOrderButton().shouldBe(disabled);

        String orderId = checkout.fill("Ada", "Lovelace", "12345")
                .placeOrder()
                .waitProgressAtLeastHalf()
                .waitForConfirmation();

        assertThat(orderId).matches("ORD-\\d+");
        OrdersPage orders = checkout.viewOrders();
        orders.refreshUntilStatus(orderId, "COMPLETED", Duration.ofSeconds(20));
        orders.header().cartBadge().shouldBe(hidden);
        assertThat(api.orders(user)).isEqualTo(1);
        assertThat(api.cartItems(user)).isZero();
    }

    @Regression
    @ParameterizedTest(name = "zip=''{0}''")
    @ValueSource(strings = {"123", "abcde", "123456"})
    void invalidZipBlocksTheOrder(String zip) {
        api.loginAndStartSession(user);
        api.addToCart(BACKPACK.id());

        CheckoutPage checkout = new CheckoutPage().open().fill("Ada", "Lovelace", zip);

        checkout.zipError().shouldHave(exactText("ZIP must be 5 digits"));
        checkout.placeOrderButton().shouldBe(disabled);
    }

    @Test
    @Regression
    void slowUserCanStillCheckOut() {
        user = TestUser.slow();
        api.loginAndStartSession(user);
        api.addToCart(BACKPACK.id());

        String orderId = new CheckoutPage().open()
                .fill("Grace", "Hopper", "54321")
                .placeOrder()
                .waitForConfirmation();

        assertThat(orderId).startsWith("ORD-");
    }
}
