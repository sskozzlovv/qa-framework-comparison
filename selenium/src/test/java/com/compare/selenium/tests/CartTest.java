package com.compare.selenium.tests;

import com.compare.selenium.pages.CartPage;
import com.compare.selenium.pages.CatalogPage;
import com.compare.selenium.tags.Critical;
import com.compare.selenium.tags.Regression;
import com.compare.selenium.tags.Smoke;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.compare.selenium.data.Product.BACKPACK;
import static com.compare.selenium.data.Product.CAMP_MUG;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Cart")
class CartTest extends BaseTest {

    @Test
    @Smoke
    @Critical
    void addedProductsAppearInTheCartAndOnTheBackend() {
        CatalogPage catalog = loginViaApi()
                .addToCart(BACKPACK)
                .addToCart(CAMP_MUG)
                .waitForToast(CAMP_MUG.name() + " added to cart");

        catalog.header().waitCartCount(2);
        catalog.waitToastsGone();
        assertThat(api.cartItems(user)).isEqualTo(2);

        CartPage cart = catalog.header().openCart();
        assertThat(cart.itemNames()).containsExactlyInAnyOrder(BACKPACK.name(), CAMP_MUG.name());
        assertThat(cart.total()).isEqualTo("$36.98");
    }

    @Test
    @Regression
    void removedItemDisappearsAfterAnimationAndTotalIsUpdated() {
        api.loginAndStartSession(user);
        api.addToCart(BACKPACK.id()); // test data via API: fast and independent of the catalog UI
        api.addToCart(CAMP_MUG.id());

        CartPage cart = new CartPage(driver).open().remove(BACKPACK);

        assertThat(cart.itemNames()).containsExactly(CAMP_MUG.name());
        assertThat(cart.total()).isEqualTo(CAMP_MUG.price());
        cart.header().waitCartCount(1);
    }

    @Test
    @Regression
    void emptyCartCannotBeCheckedOut() {
        api.loginAndStartSession(user);

        CartPage cart = new CartPage(driver).open();

        assertThat(cart.isEmptyMessageVisible()).isTrue();
        assertThat(cart.isCheckoutEnabled()).isFalse();
    }
}
