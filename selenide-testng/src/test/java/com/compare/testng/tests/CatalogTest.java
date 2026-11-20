package com.compare.testng.tests;

import com.compare.testng.pages.CatalogPage;
import com.codeborne.selenide.ClickOptions;
import com.codeborne.selenide.SelenideElement;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.Wait;
import static com.codeborne.selenide.Selenide.localStorage;
import static com.compare.testng.data.Product.BACKPACK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** groups on the CLASS apply to every @Test method in it (like @Tag on a JUnit class). */
@Test(groups = Groups.REGRESSION)
public class CatalogTest extends BaseTest {

    @Test
    public void searchShowsOnlyMatchingProducts() {
        CatalogPage catalog = loginViaApi().search("shirt");

        catalog.names().shouldHave(exactTexts("Cotton T-Shirt"));
    }

    @Test
    public void searchWithoutMatchesShowsEmptyState() {
        CatalogPage catalog = loginViaApi().search("no-such-product");

        catalog.noResults().shouldBe(visible);
        catalog.names().shouldHave(size(0));
    }

    /** JUnit: @ValueSource(strings = {...}). TestNG: a DataProvider with one column. */
    @DataProvider
    public Object[][] priceSorts() {
        return new Object[][] {{"price-asc"}, {"price-desc"}};
    }

    @Test(dataProvider = "priceSorts")
    public void productsAreSortedByPrice(String sort) {
        CatalogPage catalog = loginViaApi().sortBy(sort);
        Comparator<Integer> order = sort.equals("price-asc") ? Comparator.naturalOrder() : Comparator.reverseOrder();

        Wait().until(d -> isSorted(catalog.pricesInCents(), order));

        assertThat(catalog.pricesInCents()).hasSize(12).isSortedAccordingTo(order);
    }

    @Test
    public void selenideElementSurvivesReRender() {
        CatalogPage catalog = loginViaApi();
        SelenideElement firstCard = catalog.cards().first();
        firstCard.shouldHave(text("Camp Mug"));

        catalog.search("bottle");

        firstCard.shouldHave(text("Steel Bottle"));
    }

    @Test
    public void promoModalMustBeClosedBeforeShopping() {
        api().loginAndStartSession(user());
        localStorage().removeItem("promo.dismissed");
        CatalogPage catalog = new CatalogPage().open();
        catalog.promo().shouldBe(visible, Duration.ofSeconds(5));

        assertThatThrownBy(() -> catalog.addButton(BACKPACK)
                .click(ClickOptions.usingDefaultMethod().timeout(Duration.ofSeconds(1))))
                .hasMessageContaining("click intercepted");

        catalog.closePromo().addToCart(BACKPACK);
        catalog.header().cartBadge().shouldHave(exactText("1"));
    }

    private static boolean isSorted(List<Integer> values, Comparator<Integer> order) {
        return values.size() == 12 && values.equals(values.stream().sorted(order).toList());
    }
}
