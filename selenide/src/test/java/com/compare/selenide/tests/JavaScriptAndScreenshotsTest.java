package com.compare.selenide.tests;

import com.compare.selenide.core.Config;
import com.compare.selenide.data.Product;
import com.compare.selenide.pages.CatalogPage;
import com.compare.selenide.pages.PlaygroundPage;
import com.compare.selenide.tags.BrowserApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.chromium.HasCdp;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static com.codeborne.selenide.ClickOptions.usingJavaScript;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.executeAsyncJavaScript;
import static com.codeborne.selenide.Selenide.executeJavaScript;
import static com.codeborne.selenide.Selenide.localStorage;
import static com.codeborne.selenide.Selenide.screenshot;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@BrowserApi
@DisplayName("JavaScript execution and screenshots")
class JavaScriptAndScreenshotsTest extends BaseTest {

    @Test
    void executeJavaScriptConvertsJsValuesToJava() {
        api.loginAndStartSession(user);

        List<Object> values = executeJavaScript("return [42, 1.5, 'text', true, null, {key: 'value'}];");

        assertThat(values).containsExactly(42L, 1.5, "text", true, null, Map.of("key", "value"));
        assertThat(localStorage().getItem("username")).isEqualTo(user.username());
    }

    @Test
    void executeAsyncJavaScriptWaitsForTheCallback() {
        api.loginAndStartSession(user);

        String result = executeAsyncJavaScript(
                "const done = arguments[arguments.length - 1]; setTimeout(() => done('finished'), 500);");

        assertThat(result).isEqualTo("finished");
    }

    /** Selenide has a built-in JS click: click(usingJavaScript()). Same warning as in Selenium: it ignores overlays. */
    @Test
    void jsClickIgnoresOverlaysUnlikeARealClick() {
        api.loginAndStartSession(user);
        localStorage().removeItem("promo.dismissed");
        CatalogPage catalog = new CatalogPage().open();
        catalog.promo().shouldBe(visible, Duration.ofSeconds(5));

        catalog.addButton(Product.BACKPACK).click(usingJavaScript());

        catalog.header().cartBadge().shouldHave(exactText("1"));
    }

    @Test
    void pageElementAndFullPageScreenshots() throws IOException {
        api.loginAndStartSession(user);
        PlaygroundPage playground = new PlaygroundPage().open();

        String pagePng = screenshot("selenide-page");             // png + html into Configuration.reportsFolder
        File elementPng = playground.promoWidget().screenshot();   // element only
        byte[] viewport = screenshot(OutputType.BYTES);

        assertThat(pagePng).endsWith(".png");
        assertThat(elementPng).exists();
        assertThat(image(Files.readAllBytes(elementPng.toPath())).getWidth()).isLessThan(image(viewport).getWidth());

        // Full page: not in Selenide core (there is a "selenide-full-screenshot" plugin) -> raw WebDriver + CDP,
        // exactly like in plain Selenium. getWebDriver() is always the escape hatch.
        assumeTrue(getWebDriver() instanceof HasCdp, "full-page screenshot via CDP needs a Chromium browser");
        HasCdp cdp = (HasCdp) getWebDriver();
        @SuppressWarnings("unchecked")
        Map<String, Number> size = (Map<String, Number>) cdp.executeCdpCommand("Page.getLayoutMetrics", Map.of())
                .get("cssContentSize");
        Map<String, Object> shot = cdp.executeCdpCommand("Page.captureScreenshot", Map.of(
                "format", "png", "captureBeyondViewport", true,
                "clip", Map.of("x", 0, "y", 0, "width", size.get("width"), "height", size.get("height"), "scale", 1)));
        byte[] fullPage = Base64.getDecoder().decode((String) shot.get("data"));
        Path dir = Files.createDirectories(Config.artifactsDir().resolve("screenshots"));
        Files.write(dir.resolve("selenide-full-page.png"), fullPage);

        assertThat(image(fullPage).getHeight()).isGreaterThan(image(viewport).getHeight());
    }

    private static BufferedImage image(byte[] png) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(png));
    }
}
