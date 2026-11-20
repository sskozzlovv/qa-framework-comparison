package com.compare.testng.tests;

import com.compare.testng.pages.CartPage;
import com.compare.testng.pages.CatalogPage;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.exactTextsCaseSensitiveInAnyOrder;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static com.compare.testng.data.Product.BACKPACK;
import static com.compare.testng.data.Product.CAMP_MUG;
import static org.assertj.core.api.Assertions.assertThat;

public class CartTest extends BaseTest {

    /** A test in several groups: runs for -Dsuite=smoke AND for -Dsuite=critical. */
    @Test(groups = {Groups.SMOKE, Groups.CRITICAL})
    public void addedProductsAppearInTheCartAndOnTheBackend() {
        CatalogPage catalog = loginViaApi().addToCart(BACKPACK).addToCart(CAMP_MUG);

        catalog.toasts().last().shouldHave(exactText(CAMP_MUG.name() + " added to cart"));
        catalog.header().cartBadge().shouldHave(exactText("2"));
        catalog.toasts().shouldHave(size(0));
        assertThat(api().cartItems(user())).isEqualTo(2);

        CartPage cart = catalog.header().openCart();
        cart.names().shouldHave(exactTextsCaseSensitiveInAnyOrder(BACKPACK.name(), CAMP_MUG.name()));
        cart.total().shouldHave(exactText("$36.98"));
    }

    @Test(groups = Groups.REGRESSION)
    public void removedItemDisappearsAfterAnimationAndTotalIsUpdated() {
        api().loginAndStartSession(user());
        api().addToCart(BACKPACK.id());
        api().addToCart(CAMP_MUG.id());

        CartPage cart = new CartPage().open().remove(BACKPACK);

        cart.names().shouldHave(exactTexts(CAMP_MUG.name()));
        cart.total().shouldHave(exactText(CAMP_MUG.price()));
        cart.header().cartBadge().shouldHave(exactText("1"));
    }

    @Test(groups = Groups.REGRESSION)
    public void emptyCartCannotBeCheckedOut() {
        api().loginAndStartSession(user());

        CartPage cart = new CartPage().open();

        cart.emptyMessage().shouldBe(visible);
        cart.checkoutButton().shouldBe(disabled);
    }
}
