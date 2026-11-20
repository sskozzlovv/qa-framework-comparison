package com.compare.playwright.core;

import com.compare.playwright.api.ApiClient;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Tracing;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Fixtures for JUnit, in the spirit of Playwright Test: every test gets a NEW BrowserContext + Page (+ API client),
 * injected as parameters. Tracing records every test; the trace is SAVED only when the test failed.
 * (Playwright for Java also ships its own &#64;UsePlaywright extension; this one adds trace/screenshot on failure.)
 *
 * <p>Order: beforeEach (create) -> &#64;BeforeEach -> test -> afterTestExecution (artifacts) -> &#64;AfterEach (cleanup)
 * -> afterEach (close context).
 */
public class PlaywrightExtension implements BeforeEachCallback, AfterTestExecutionCallback, AfterEachCallback,
        ParameterResolver {

    private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(PlaywrightExtension.class);

    @Override
    public void beforeEach(ExtensionContext extension) {
        BrowserPerThread.Holder holder = BrowserPerThread.get();
        Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setBaseURL(Config.baseUrl())           // page.navigate("/catalog.html")
                .setViewportSize(1366, 900)
                .setAcceptDownloads(true);
        if (Config.video()) {
            options.setRecordVideoDir(Config.artifactsDir().resolve("videos"));
        }
        BrowserContext context = holder.browser().newContext(options);
        context.setDefaultTimeout(Config.timeout().toMillis());    // actions: click, fill, waitFor...
        context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        Page page = context.newPage();

        ExtensionContext.Store store = extension.getStore(NS);
        store.put(BrowserContext.class, context);
        store.put(Page.class, page);
        store.put(ApiClient.class, new ApiClient(holder.playwright(), Config.baseUrl()));
    }

    @Override
    public void afterTestExecution(ExtensionContext extension) {
        BrowserContext context = extension.getStore(NS).get(BrowserContext.class, BrowserContext.class);
        Page page = extension.getStore(NS).get(Page.class, Page.class);
        if (extension.getExecutionException().isEmpty()) {
            context.tracing().stop(); // discard
            return;
        }
        Path dir = Config.artifactsDir()
                .resolve(extension.getRequiredTestClass().getSimpleName())
                .resolve(extension.getRequiredTestMethod().getName() + "-"
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss-SSS")));
        try {
            Files.createDirectories(dir);
            page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve("screenshot.png")).setFullPage(true));
            Files.writeString(dir.resolve("page.html"), page.content(), StandardCharsets.UTF_8);
            // Open with: mvnw -pl playwright exec:java -Dexec.mainClass=com.microsoft.playwright.CLI
            //            -Dexec.args="show-trace <path>/trace.zip"   (or drag it onto https://trace.playwright.dev)
            context.tracing().stop(new Tracing.StopOptions().setPath(dir.resolve("trace.zip")));
            System.out.println("[artifacts] " + dir.toAbsolutePath());
        } catch (IOException | RuntimeException e) {
            System.err.println("[artifacts] could not save failure artifacts: " + e);
        }
    }

    @Override
    public void afterEach(ExtensionContext extension) {
        ExtensionContext.Store store = extension.getStore(NS);
        BrowserContext context = store.remove(BrowserContext.class, BrowserContext.class);
        ApiClient api = store.remove(ApiClient.class, ApiClient.class);
        store.remove(Page.class);
        if (context != null) {
            context.close(); // closes all pages of the test; the video file is finalized here
        }
        if (api != null) {
            api.close();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext extension) {
        Class<?> type = parameter.getParameter().getType();
        return type == Page.class || type == BrowserContext.class || type == ApiClient.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameter, ExtensionContext extension) {
        Class<?> type = parameter.getParameter().getType();
        return extension.getStore(NS).get(type, type);
    }
}
