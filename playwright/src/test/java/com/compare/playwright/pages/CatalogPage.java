package com.compare.playwright.pages;

import com.compare.playwright.data.Product;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class CatalogPage extends BasePage {

    private final Locator search;
    private final Locator sort;
    private final Locator resultCount;
    private final Locator noResults;
    private final Locator loadError;
    private final Locator cards;
    private final Locator names;
    private final Locator prices;
    private final Locator toasts;
    private final Locator promo;

    public CatalogPage(Page page) {
        super(page);
        this.search = page.getByPlaceholder("Search products");
        this.sort = page.getByLabel("Sort");
        this.resultCount = page.locator("#result-count");
        this.noResults = page.getByText("No products found.");
        this.loadError = page.locator("#load-error");
        this.cards = page.locator(".product-card");
        this.names = cards.locator(".name");
        this.prices = cards.locator(".price");
        this.toasts = page.locator(".toast");
        this.promo = page.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName("Promo"));
    }

    public CatalogPage open() {
        page.navigate("/catalog.html");
        return waitLoaded();
    }

    /** Opens the page without expecting a product list (error-state tests). */
    public CatalogPage navigate() {
        page.navigate("/catalog.html");
        return this;
    }

    public CatalogPage waitLoaded() {
        page.waitForURL("**/catalog.html");
        waitUntilReady();
        assertThat(resultCount).not().isEmpty();
        return this;
    }

    /**
     * Network-level synchronization: run the action and wait for the request it triggers (it starts 400 ms later
     * because of the debounce). The overlay check then guarantees the list was re-rendered.
     */
    public CatalogPage search(String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        page.waitForResponse(r -> r.url().contains("/api/products?q=" + encoded), () -> search.fill(query));
        assertThat(spinner).isHidden();
        return this;
    }

    public CatalogPage sortBy(String value) {
        page.waitForResponse(r -> r.url().contains("sort=" + value), () -> sort.selectOption(value));
        assertThat(spinner).isHidden();
        return this;
    }

    public Locator cards() {
        return cards;
    }

    public Locator names() {
        return names;
    }

    public Locator noResults() {
        return noResults;
    }

    public Locator loadError() {
        return loadError;
    }

    /** allTextContents() does NOT wait - it is a snapshot, call it after a synchronization point. */
    public List<Integer> pricesInCents() {
        return prices.allTextContents().stream()
                .map(t -> Integer.parseInt(t.replace("$", "").replace(".", "")))
                .toList();
    }

    /** filter(hasText) + role inside the card: no XPath needed. */
    public Locator addButton(Product product) {
        return cards.filter(new Locator.FilterOptions().setHasText(product.name()))
                .getByRole(AriaRole.BUTTON);
    }

    public CatalogPage addToCart(Product product) {
        Locator button = addButton(product);
        button.click();
        assertThat(button).hasText("Added");
        return this;
    }

    public Locator toasts() {
        return toasts;
    }

    public Locator promo() {
        return promo;
    }

    public CatalogPage closePromo() {
        promo.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close")).click();
        assertThat(promo).isHidden();
        return this;
    }
}
