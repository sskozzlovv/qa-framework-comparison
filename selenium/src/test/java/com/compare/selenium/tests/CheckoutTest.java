package com.compare.selenium.tests;

import com.compare.selenium.data.TestUser;
import com.compare.selenium.pages.CatalogPage;
import com.compare.selenium.pages.CheckoutPage;
import com.compare.selenium.pages.OrdersPage;
import com.compare.selenium.tags.Critical;
import com.compare.selenium.tags.Regression;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static com.compare.selenium.data.Product.BACKPACK;
import static com.compare.selenium.data.Product.CAMP_MUG;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Checkout")
class CheckoutTest extends BaseTest {

    @Test
    @Critical
    void userBuysProductsAndOrderGetsCompleted() {
        CatalogPage catalog = loginViaApi().addToCart(BACKPACK).addToCart(CAMP_MUG);
        catalog.header().waitCartCount(2);
        CheckoutPage checkout = catalog.header().openCart().checkout();
        assertThat(checkout.isPlaceOrderEnabled()).as("button before the form is valid").isFalse();

        String orderId = checkout.fill("Ada", "Lovelace", "12345")
                .placeOrder()
                .waitProgressAtLeast(50)
                .waitForConfirmation();

        assertThat(orderId).matches("ORD-\\d+");
        OrdersPage orders = checkout.viewOrders();
        // PROCESSING -> COMPLETED happens on the server ~5 s after the order; the page needs "Refresh"
        orders.refreshUntilStatus(orderId, "COMPLETED", Duration.ofSeconds(20));
        orders.header().waitCartCount(0);
        assertThat(api.orders(user)).isEqualTo(1);
        assertThat(api.cartItems(user)).isZero();
    }

    @Regression
    @ParameterizedTest(name = "zip=''{0}''")
    @ValueSource(strings = {"123", "abcde", "123456"})
    void invalidZipBlocksTheOrder(String zip) {
        api.loginAndStartSession(user);
        api.addToCart(BACKPACK.id());

        CheckoutPage checkout = new CheckoutPage(driver).open().fill("Ada", "Lovelace", zip);

        assertThat(checkout.errorOf(CheckoutPage.ZIP)).isEqualTo("ZIP must be 5 digits");
        assertThat(checkout.isPlaceOrderEnabled()).isFalse();
    }

    @Test
    @Regression
    void slowUserCanStillCheckOut() {
        user = TestUser.slow(); // every API call is ~4x slower: the waits must not be tuned to "normal" speed
        api.loginAndStartSession(user);
        api.addToCart(BACKPACK.id());

        String orderId = new CheckoutPage(driver).open()
                .fill("Grace", "Hopper", "54321")
                .placeOrder()
                .waitForConfirmation();

        assertThat(orderId).startsWith("ORD-");
    }
}
