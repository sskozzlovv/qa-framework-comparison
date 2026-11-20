package com.compare.selenide.tests;

import com.compare.selenide.pages.CatalogPage;
import com.compare.selenide.tags.Regression;
import com.codeborne.selenide.ClickOptions;
import com.codeborne.selenide.SelenideElement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
import static com.compare.selenide.data.Product.BACKPACK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Regression
@DisplayName("Catalog")
class CatalogTest extends BaseTest {

    @Test
    void searchShowsOnlyMatchingProducts() {
        CatalogPage catalog = loginViaApi().search("shirt");

        catalog.names().shouldHave(exactTexts("Cotton T-Shirt")); // retries until the debounced re-render is done
    }

    @Test
    void searchWithoutMatchesShowsEmptyState() {
        CatalogPage catalog = loginViaApi().search("no-such-product");

        catalog.noResults().shouldBe(visible);
        catalog.names().shouldHave(size(0));
    }

    /**
     * "Is the list sorted?" is not a built-in collection condition. Pitfall: reading pricesInCents() right after
     * sortBy() returns the OLD list. We wait for the property with Selenide.Wait() and then assert.
     */
    @ParameterizedTest
    @ValueSource(strings = {"price-asc", "price-desc"})
    void productsAreSortedByPrice(String sort) {
        CatalogPage catalog = loginViaApi().sortBy(sort);
        Comparator<Integer> order = sort.equals("price-asc") ? Comparator.naturalOrder() : Comparator.reverseOrder();

        Wait().until(d -> isSorted(catalog.pricesInCents(), order));

        assertThat(catalog.pricesInCents()).hasSize(12).isSortedAccordingTo(order);
    }

    /** The Selenium version of this test expects StaleElementReferenceException. Selenide re-finds the element. */
    @Test
    void selenideElementSurvivesReRender() {
        CatalogPage catalog = loginViaApi();
        SelenideElement firstCard = catalog.cards().first();
        firstCard.shouldHave(text("Camp Mug"));

        catalog.search("bottle");

        firstCard.shouldHave(text("Steel Bottle")); // same object, new DOM node
    }

    /** Selenide RETRIES an intercepted click until the timeout - so the overlay only fails the test after 1 s here. */
    @Test
    void promoModalMustBeClosedBeforeShopping() {
        api.loginAndStartSession(user);
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
