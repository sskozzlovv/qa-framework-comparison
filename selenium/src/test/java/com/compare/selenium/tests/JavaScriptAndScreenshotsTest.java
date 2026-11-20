package com.compare.selenium.tests;

import com.compare.selenium.core.Config;
import com.compare.selenium.core.Js;
import com.compare.selenium.data.Product;
import com.compare.selenium.pages.CatalogPage;
import com.compare.selenium.pages.PlaygroundPage;
import com.compare.selenium.tags.BrowserApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chromium.HasCdp;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@BrowserApi
@DisplayName("JavaScript execution and screenshots")
class JavaScriptAndScreenshotsTest extends BaseTest {

    @Test
    void executeScriptConvertsJsValuesToJava() {
        api.loginAndStartSession(user);
        Js js = new Js(driver);

        List<Object> values = js.exec("return [42, 1.5, 'text', true, null, {key: 'value'}];");

        assertThat(values).containsExactly(42L, 1.5, "text", true, null, Map.of("key", "value"));
        assertThat(js.getLocalStorage("username")).isEqualTo(user.username());
    }

    @Test
    void executeAsyncScriptWaitsForTheCallback() {
        api.loginAndStartSession(user);

        Object result = ((org.openqa.selenium.JavascriptExecutor) driver).executeAsyncScript(
                "const done = arguments[arguments.length - 1]; setTimeout(() => done('finished'), 500);");

        assertThat(result).isEqualTo("finished");
    }

    /** JS click bypasses the overlay - it "works" where a real user could not click. Use only as a last resort. */
    @Test
    void jsClickIgnoresOverlaysUnlikeARealClick() {
        api.loginAndStartSession(user);
        Js js = new Js(driver);
        js.exec("localStorage.removeItem('promo.dismissed');");
        CatalogPage catalog = new CatalogPage(driver).open().waitForPromo();

        js.exec("arguments[0].click();", catalog.addButtonOf(Product.BACKPACK));

        catalog.header().waitCartCount(1); // added although the modal is still covering the page
    }

    @Test
    void viewportElementAndFullPageScreenshots() throws IOException {
        api.loginAndStartSession(user);
        PlaygroundPage playground = new PlaygroundPage(driver).open();
        Path dir = Files.createDirectories(Config.artifactsDir().resolve("screenshots"));

        byte[] viewport = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        WebElement widget = playground.promoWidget();
        byte[] element = widget.getScreenshotAs(OutputType.BYTES); // Selenium 4: element screenshot
        Files.write(dir.resolve("selenium-viewport.png"), viewport);
        Files.write(dir.resolve("selenium-element.png"), element);

        // --window-size is the OUTER window; the screenshot is only the visible viewport
        assertThat(image(viewport).getHeight()).isLessThanOrEqualTo(900);
        assertThat(image(element).getWidth()).isLessThan(image(viewport).getWidth());

        // Full page: Selenium has it only for Firefox (getFullPageScreenshotAs); for Chrome -> CDP command.
        assumeTrue(driver instanceof HasCdp, "full-page screenshot via CDP needs a Chromium browser");
        HasCdp cdp = (HasCdp) driver;
        Map<String, Object> metrics = cdp.executeCdpCommand("Page.getLayoutMetrics", Map.of());
        @SuppressWarnings("unchecked")
        Map<String, Number> size = (Map<String, Number>) metrics.get("cssContentSize");
        Map<String, Object> shot = cdp.executeCdpCommand("Page.captureScreenshot", Map.of(
                "format", "png", "captureBeyondViewport", true,
                "clip", Map.of("x", 0, "y", 0, "width", size.get("width"), "height", size.get("height"), "scale", 1)));
        byte[] fullPage = Base64.getDecoder().decode((String) shot.get("data"));
        Files.write(dir.resolve("selenium-full-page.png"), fullPage);

        assertThat(image(fullPage).getHeight()).isGreaterThan(image(viewport).getHeight());
    }

    private static BufferedImage image(byte[] png) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(png));
    }
}
