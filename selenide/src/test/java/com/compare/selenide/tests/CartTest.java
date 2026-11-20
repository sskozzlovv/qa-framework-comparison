package com.compare.selenide.tests;

import com.compare.selenide.pages.CartPage;
import com.compare.selenide.pages.CatalogPage;
import com.compare.selenide.tags.Critical;
import com.compare.selenide.tags.Regression;
import com.compare.selenide.tags.Smoke;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static com.compare.selenide.data.Product.BACKPACK;
import static com.compare.selenide.data.Product.CAMP_MUG;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Cart")
class CartTest extends BaseTest {

    @Test
    @Smoke
    @Critical
    void addedProductsAppearInTheCartAndOnTheBackend() {
        CatalogPage catalog = loginViaApi().addToCart(BACKPACK).addToCart(CAMP_MUG);

        catalog.toasts().last().shouldHave(exactText(CAMP_MUG.name() + " added to cart"));
        catalog.header().cartBadge().shouldHave(exactText("2"));
        catalog.toasts().shouldHave(size(0)); // toasts vanish after ~2.4 s - the condition waits for it
        assertThat(api.cartItems(user)).isEqualTo(2);

        CartPage cart = catalog.header().openCart();
        cart.names().shouldHave(exactTextsCaseSensitiveInAnyOrder(BACKPACK.name(), CAMP_MUG.name()));
        cart.total().shouldHave(exactText("$36.98"));
    }

    @Test
    @Regression
    void removedItemDisappearsAfterAnimationAndTotalIsUpdated() {
        api.loginAndStartSession(user);
        api.addToCart(BACKPACK.id());
        api.addToCart(CAMP_MUG.id());

        CartPage cart = new CartPage().open().remove(BACKPACK);

        cart.names().shouldHave(exactTexts(CAMP_MUG.name()));
        cart.total().shouldHave(exactText(CAMP_MUG.price()));
        cart.header().cartBadge().shouldHave(exactText("1"));
    }

    @Test
    @Regression
    void emptyCartCannotBeCheckedOut() {
        api.loginAndStartSession(user);

        CartPage cart = new CartPage().open();

        cart.emptyMessage().shouldBe(visible);
        cart.checkoutButton().shouldBe(disabled);
    }
}
