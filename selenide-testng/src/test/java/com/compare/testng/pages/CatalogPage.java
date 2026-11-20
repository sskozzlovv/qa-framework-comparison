package com.compare.testng.pages;

import com.compare.testng.data.Product;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

import static com.codeborne.selenide.CollectionCondition.allMatch;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.webdriver;
import static com.codeborne.selenide.WebDriverConditions.urlContaining;

public class CatalogPage extends BasePage {

    private final SelenideElement search = $("#search");
    private final SelenideElement sort = $("#sort");
    private final SelenideElement resultCount = $("#result-count");
    private final SelenideElement noResults = $("#no-results");
    private final SelenideElement promo = $("#promo-modal");
    private final SelenideElement promoClose = $("#promo-close");
    private final ElementsCollection cards = $$(".product-card");
    private final ElementsCollection names = $$(".product-card .name");
    private final ElementsCollection prices = $$(".product-card .price");
    private final ElementsCollection toasts = $$(".toast");

    public CatalogPage open() {
        Selenide.open("/catalog.html");
        return waitLoaded();
    }

    public CatalogPage waitLoaded() {
        webdriver().shouldHave(urlContaining("/catalog.html"), Duration.ofSeconds(15));
        waitUntilReady();
        resultCount.shouldNotBe(empty);
        return this;
    }

    /**
     * No staleness tricks needed (compare with Selenium): the test asserts with names().shouldHave(...),
     * which simply retries until the debounced re-render produced the expected list.
     */
    public CatalogPage search(String query) {
        search.setValue(query);
        return this;
    }

    public CatalogPage sortBy(String value) {
        sort.selectOptionByValue(value);
        return this;
    }

    public ElementsCollection cards() {
        return cards;
    }

    public ElementsCollection names() {
        return names;
    }

    public SelenideElement noResults() {
        return noResults;
    }

    /**
     * texts() is a SNAPSHOT - read it only after a waiting condition passed. And it reads VISIBLE text: cards fade
     * in from opacity 0, during which WebDriver returns "" - so first wait until all prices are displayed.
     */
    public List<Integer> pricesInCents() {
        prices.shouldHave(allMatch("displayed (fade-in finished)", WebElement::isDisplayed));
        return prices.texts().stream().map(t -> Integer.parseInt(t.replace("$", "").replace(".", ""))).toList();
    }

    public SelenideElement addButton(Product product) {
        return cards.findBy(text(product.name())).$("button");
    }

    public CatalogPage addToCart(Product product) {
        SelenideElement button = addButton(product);
        button.click();
        button.shouldHave(exactText("Added"));
        return this;
    }

    public ElementsCollection toasts() {
        return toasts;
    }

    public SelenideElement promo() {
        return promo;
    }

    public CatalogPage closePromo() {
        promoClose.click();
        promo.should(disappear);
        return this;
    }
}
