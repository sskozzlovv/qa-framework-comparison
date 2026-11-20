package com.compare.selenium.tests;

import com.compare.selenium.core.Js;
import com.compare.selenium.pages.CatalogPage;
import com.compare.selenium.tags.Regression;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import java.util.Comparator;
import java.util.List;

import static com.compare.selenium.data.Product.BACKPACK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Regression
@DisplayName("Catalog")
class CatalogTest extends BaseTest {

    @Test
    void searchShowsOnlyMatchingProducts() {
        CatalogPage catalog = loginViaApi().search("shirt");

        assertThat(catalog.productNames()).containsExactly("Cotton T-Shirt");
    }

    @Test
    void searchWithoutMatchesShowsEmptyState() {
        CatalogPage catalog = loginViaApi().search("no-such-product");

        assertThat(catalog.productNames()).isEmpty();
        assertThat(catalog.isNoResultsVisible()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"price-asc", "price-desc"})
    void productsAreSortedByPrice(String sort) {
        List<Integer> prices = loginViaApi().sortBy(sort).pricesInCents();

        Comparator<Integer> order = sort.equals("price-asc") ? Comparator.naturalOrder() : Comparator.reverseOrder();
        assertThat(prices).hasSize(12).isSortedAccordingTo(order);
    }

    /** Selenium pitfall #1: a WebElement is a pointer to ONE DOM node. Re-render = the pointer is dead. */
    @Test
    void elementFoundBeforeReRenderBecomesStale() {
        CatalogPage catalog = loginViaApi();
        WebElement firstCard = catalog.firstCard();

        catalog.search("mug");

        assertThatThrownBy(firstCard::getText).isInstanceOf(StaleElementReferenceException.class);
    }

    /** Selenium pitfall #2: a modal on top of the page swallows clicks; Selenium throws instead of waiting. */
    @Test
    void promoModalMustBeClosedBeforeShopping() {
        api.loginAndStartSession(user);
        new Js(driver).exec("localStorage.removeItem('promo.dismissed');"); // we WANT the modal in this test
        CatalogPage catalog = new CatalogPage(driver).open().waitForPromo();

        assertThatThrownBy(() -> catalog.clickAddToCartWithoutWaiting(BACKPACK))
                .isInstanceOf(ElementClickInterceptedException.class);

        catalog.closePromo().addToCart(BACKPACK);
        catalog.header().waitCartCount(1);
    }
}
