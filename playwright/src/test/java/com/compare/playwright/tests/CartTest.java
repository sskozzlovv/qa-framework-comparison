package com.compare.playwright.tests;

import com.compare.playwright.pages.CartPage;
import com.compare.playwright.pages.CatalogPage;
import com.compare.playwright.tags.Critical;
import com.compare.playwright.tags.Regression;
import com.compare.playwright.tags.Smoke;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.compare.playwright.data.Product.BACKPACK;
import static com.compare.playwright.data.Product.CAMP_MUG;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@DisplayName("Cart")
class CartTest extends BaseTest {

    @Test
    @Smoke
    @Critical
    void addedProductsAppearInTheCartAndOnTheBackend() {
        CatalogPage catalog = loginViaApi().addToCart(BACKPACK).addToCart(CAMP_MUG);

        assertThat(catalog.toasts().last()).hasText(CAMP_MUG.name() + " added to cart");
        assertThat(catalog.header().cartBadge()).hasText("2");
        assertThat(catalog.toasts()).hasCount(0);
        org.assertj.core.api.Assertions.assertThat(api.cartItems(user)).isEqualTo(2);

        CartPage cart = catalog.header().openCart();
        assertThat(cart.names()).containsText(new String[]{BACKPACK.name(), CAMP_MUG.name()});
        assertThat(cart.total()).hasText("$36.98");
    }

    @Test
    @Regression
    void removedItemDisappearsAfterAnimationAndTotalIsUpdated() {
        api.loginAndStartSession(page.context(), user);
        api.addToCart(BACKPACK.id());
        api.addToCart(CAMP_MUG.id());

        CartPage cart = new CartPage(page).open().remove(BACKPACK);

        assertThat(cart.names()).hasText(new String[]{CAMP_MUG.name()});
        assertThat(cart.total()).hasText(CAMP_MUG.price());
        assertThat(cart.header().cartBadge()).hasText("1");
    }

    @Test
    @Regression
    void emptyCartCannotBeCheckedOut() {
        api.loginAndStartSession(page.context(), user);

        CartPage cart = new CartPage(page).open();

        assertThat(cart.emptyMessage()).isVisible();
        assertThat(cart.checkoutButton()).isDisabled();
    }
}
