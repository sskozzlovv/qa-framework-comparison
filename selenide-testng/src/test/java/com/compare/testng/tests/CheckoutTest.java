package com.compare.testng.tests;

import com.compare.testng.data.TestUser;
import com.compare.testng.pages.CatalogPage;
import com.compare.testng.pages.CheckoutPage;
import com.compare.testng.pages.OrdersPage;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Duration;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.hidden;
import static com.compare.testng.data.Product.BACKPACK;
import static com.compare.testng.data.Product.CAMP_MUG;
import static org.assertj.core.api.Assertions.assertThat;

public class CheckoutTest extends BaseTest {

    @Test(groups = Groups.CRITICAL, description = "User buys products and the order gets completed")
    public void userBuysProductsAndOrderGetsCompleted() {
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
        assertThat(api().orders(user())).isEqualTo(1);
        assertThat(api().cartItems(user())).isZero();
    }

    @DataProvider
    public Object[][] invalidZips() {
        return new Object[][] {{"123"}, {"abcde"}, {"123456"}};
    }

    @Test(groups = Groups.REGRESSION, dataProvider = "invalidZips")
    public void invalidZipBlocksTheOrder(String zip) {
        api().loginAndStartSession(user());
        api().addToCart(BACKPACK.id());

        CheckoutPage checkout = new CheckoutPage().open().fill("Ada", "Lovelace", zip);

        checkout.zipError().shouldHave(exactText("ZIP must be 5 digits"));
        checkout.placeOrderButton().shouldBe(disabled);
    }

    @Test(groups = Groups.REGRESSION)
    public void slowUserCanStillCheckOut() {
        useUser(TestUser.slow());
        api().loginAndStartSession(user());
        api().addToCart(BACKPACK.id());

        String orderId = new CheckoutPage().open()
                .fill("Grace", "Hopper", "54321")
                .placeOrder()
                .waitForConfirmation();

        assertThat(orderId).startsWith("ORD-");
    }
}
