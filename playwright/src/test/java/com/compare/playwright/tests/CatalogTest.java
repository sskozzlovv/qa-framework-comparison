package com.compare.playwright.tests;

import com.compare.playwright.pages.CatalogPage;
import com.compare.playwright.tags.Regression;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.TimeoutError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Comparator;
import java.util.List;

import static com.compare.playwright.data.Product.BACKPACK;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Regression
@DisplayName("Catalog")
class CatalogTest extends BaseTest {

    @Test
    void searchShowsOnlyMatchingProducts() {
        CatalogPage catalog = loginViaApi().search("shirt");

        assertThat(catalog.names()).hasText(new String[]{"Cotton T-Shirt"}); // array = whole list, in order
    }

    @Test
    void searchWithoutMatchesShowsEmptyState() {
        CatalogPage catalog = loginViaApi().search("no-such-product");

        assertThat(catalog.noResults()).isVisible();
        assertThat(catalog.names()).hasCount(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"price-asc", "price-desc"})
    void productsAreSortedByPrice(String sort) {
        List<Integer> prices = loginViaApi().sortBy(sort).pricesInCents(); // sortBy waited for the response

        Comparator<Integer> order = sort.equals("price-asc") ? Comparator.naturalOrder() : Comparator.reverseOrder();
        org.assertj.core.api.Assertions.assertThat(prices).hasSize(12).isSortedAccordingTo(order);
    }

    /** Same as Selenide: a Locator is re-resolved on every use, so a re-render cannot make it stale. */
    @Test
    void locatorSurvivesReRender() {
        CatalogPage catalog = loginViaApi();
        Locator firstCard = catalog.cards().first();
        assertThat(firstCard).containsText("Camp Mug");

        catalog.search("bottle");

        assertThat(firstCard).containsText("Steel Bottle");
    }

    /**
     * Actionability check "receives events": Playwright sees the modal on top and keeps waiting/retrying until the
     * timeout - the error message even names the element that intercepts the pointer.
     */
    @Test
    void promoModalMustBeClosedBeforeShopping() {
        api.loginAndStartSession(page.context(), user, false); // keep the promo modal
        CatalogPage catalog = new CatalogPage(page).open();
        assertThat(catalog.promo()).isVisible();

        assertThatThrownBy(() -> catalog.addButton(BACKPACK).click(new Locator.ClickOptions().setTimeout(1000)))
                .isInstanceOf(TimeoutError.class)
                .hasMessageContaining("intercepts pointer events");

        catalog.closePromo().addToCart(BACKPACK);
        assertThat(catalog.header().cartBadge()).hasText("1");
    }

    /**
     * Playwright-only (out of the box): intercept the network and fake the backend to test an error state that
     * is hard to produce for real. Selenium needs CDP/BiDi or a proxy for this.
     */
    @Test
    void backendErrorIsShownToTheUser() {
        page.route("**/api/products*", route -> route.fulfill(new Route.FulfillOptions()
                .setStatus(500)
                .setContentType("application/json")
                .setBody("{\"error\": \"Database is down\"}")));

        api.loginAndStartSession(page.context(), user);
        CatalogPage catalog = new CatalogPage(page).navigate();

        assertThat(catalog.loadError()).hasText("Could not load products: Database is down");
        assertThat(catalog.names()).hasCount(0);
    }
}
