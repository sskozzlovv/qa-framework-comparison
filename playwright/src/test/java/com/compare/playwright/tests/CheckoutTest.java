package com.compare.playwright.tests;

import com.compare.playwright.data.TestUser;
import com.compare.playwright.pages.CatalogPage;
import com.compare.playwright.pages.CheckoutPage;
import com.compare.playwright.pages.OrdersPage;
import com.compare.playwright.tags.Critical;
import com.compare.playwright.tags.Regression;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static com.compare.playwright.data.Product.BACKPACK;
import static com.compare.playwright.data.Product.CAMP_MUG;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@DisplayName("Checkout")
class CheckoutTest extends BaseTest {

    @Test
    @Critical
    void userBuysProductsAndOrderGetsCompleted() {
        CatalogPage catalog = loginViaApi().addToCart(BACKPACK).addToCart(CAMP_MUG);
        assertThat(catalog.header().cartBadge()).hasText("2");
        CheckoutPage checkout = catalog.header().openCart().checkout();
        assertThat(checkout.placeOrderButton()).isDisabled();

        String orderId = checkout.fill("Ada", "Lovelace", "12345")
                .placeOrder()
                .waitProgressAtLeastHalf()
                .waitForConfirmation();

        org.assertj.core.api.Assertions.assertThat(orderId).matches("ORD-\\d+");
        OrdersPage orders = checkout.viewOrders();
        orders.refreshUntilStatus(orderId, "COMPLETED", Duration.ofSeconds(20));
        assertThat(orders.header().cartBadge()).isHidden();
        org.assertj.core.api.Assertions.assertThat(api.orders(user)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(api.cartItems(user)).isZero();
    }

    @Regression
    @ParameterizedTest(name = "zip=''{0}''")
    @ValueSource(strings = {"123", "abcde", "123456"})
    void invalidZipBlocksTheOrder(String zip) {
        api.loginAndStartSession(page.context(), user);
        api.addToCart(BACKPACK.id());

        CheckoutPage checkout = new CheckoutPage(page).open().fill("Ada", "Lovelace", zip);

        assertThat(checkout.zipError()).hasText("ZIP must be 5 digits");
        assertThat(checkout.placeOrderButton()).isDisabled();
    }

    @Test
    @Regression
    void slowUserCanStillCheckOut() {
        user = TestUser.slow();
        api.loginAndStartSession(page.context(), user);
        api.addToCart(BACKPACK.id());

        String orderId = new CheckoutPage(page).open()
                .fill("Grace", "Hopper", "54321")
                .placeOrder()
                .waitForConfirmation();

        org.assertj.core.api.Assertions.assertThat(orderId).startsWith("ORD-");
    }
}
