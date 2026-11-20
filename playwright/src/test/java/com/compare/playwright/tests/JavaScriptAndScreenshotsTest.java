package com.compare.playwright.tests;

import com.compare.playwright.core.Config;
import com.compare.playwright.data.Product;
import com.compare.playwright.pages.CatalogPage;
import com.compare.playwright.pages.PlaygroundPage;
import com.compare.playwright.tags.BrowserApi;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@BrowserApi
@DisplayName("JavaScript execution and screenshots")
class JavaScriptAndScreenshotsTest extends BaseTest {

    /** evaluate() takes a FUNCTION expression; the arg is passed explicitly (no arguments[0]). */
    @Test
    void evaluateConvertsJsValuesToJava() {
        loginViaApi();

        Object values = page.evaluate("() => [42, 1.5, 'text', true, null, {key: 'value'}]");
        Object username = page.evaluate("key => localStorage.getItem(key)", "username");

        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) values;
        // note: 42 comes back as Integer here, as Long in Selenium
        org.assertj.core.api.Assertions.assertThat(list)
                .containsExactly(42, 1.5, "text", true, null, Map.of("key", "value"));
        org.assertj.core.api.Assertions.assertThat(username).isEqualTo(user.username());
    }

    /** A returned Promise is awaited automatically - no callback juggling like executeAsyncScript. */
    @Test
    void evaluateAwaitsPromises() {
        loginViaApi();

        Object status = page.evaluate("async path => (await fetch(path)).status", "/api/health");
        Object delayed = page.evaluate("() => new Promise(r => setTimeout(() => r('finished'), 500))");

        org.assertj.core.api.Assertions.assertThat(status).isEqualTo(200);
        org.assertj.core.api.Assertions.assertThat(delayed).isEqualTo("finished");
    }

    /** dispatchEvent / click(force) skip the actionability checks - same anti-pattern as a JS click. */
    @Test
    void dispatchedClickIgnoresOverlaysUnlikeARealClick() {
        api.loginAndStartSession(page.context(), user, false);
        CatalogPage catalog = new CatalogPage(page).open();
        assertThat(catalog.promo()).isVisible();

        catalog.addButton(Product.BACKPACK).dispatchEvent("click");

        assertThat(catalog.header().cartBadge()).hasText("1");
    }

    @Test
    void viewportElementFullPageAndMaskedScreenshots() throws IOException {
        loginViaApi();
        PlaygroundPage playground = new PlaygroundPage(page).open();
        Path dir = Files.createDirectories(Config.artifactsDir().resolve("screenshots"));

        byte[] viewport = page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve("playwright-viewport.png")));
        byte[] fullPage = page.screenshot(new Page.ScreenshotOptions()
                .setPath(dir.resolve("playwright-full-page.png")).setFullPage(true));
        byte[] element = playground.promoWidget().screenshot();
        // mask = paint over volatile data (user name, dates) - the basis of stable visual comparisons
        page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve("playwright-masked.png"))
                .setMask(List.of(playground.header().username())));

        org.assertj.core.api.Assertions.assertThat(image(viewport).getHeight()).isEqualTo(900);
        org.assertj.core.api.Assertions.assertThat(image(fullPage).getHeight()).isGreaterThan(900);
        org.assertj.core.api.Assertions.assertThat(image(element).getWidth()).isLessThan(1366);
    }

    private static BufferedImage image(byte[] png) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(png));
    }
}
