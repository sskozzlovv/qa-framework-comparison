package com.compare.testng.tests;

import com.compare.testng.api.BackendApi;
import com.compare.testng.core.AllureSupport;
import com.compare.testng.core.Config;
import com.compare.testng.core.SelenideConfig;
import com.compare.testng.data.TestUser;
import com.compare.testng.listeners.FailureScreenshotListener;
import com.compare.testng.pages.CatalogPage;
import com.codeborne.selenide.WebDriverRunner;
import com.sandbox.app.DemoApp;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.codeborne.selenide.Selenide.clearBrowserCookies;
import static com.codeborne.selenide.Selenide.clearBrowserLocalStorage;
import static com.codeborne.selenide.Selenide.closeWebDriver;

/**
 * Base class of all TestNG tests. Three TestNG rules shape it:
 * <ol>
 *   <li><b>One instance of a test class for ALL its methods</b> (JUnit: a new instance per method). A field like
 *       {@code TestUser user = TestUser.unique()} would be shared by every test of the class - so the per-test
 *       state is created in &#64;BeforeMethod.</li>
 *   <li><b>parallel="methods"</b> runs methods of the SAME instance in different threads at once - plain fields
 *       would be overwritten by a neighbour thread, hence ThreadLocal.</li>
 *   <li><b>alwaysRun = true</b> on configuration methods: when testng.xml filters by groups, &#64;BeforeMethod /
 *       &#64;AfterMethod without a group are SKIPPED otherwise - the tests would run without user and cleanup.</li>
 * </ol>
 */
@Listeners(FailureScreenshotListener.class)
public abstract class BaseTest {

    private static final ThreadLocal<TestUser> USER = new ThreadLocal<>();
    private static final ThreadLocal<BackendApi> API = new ThreadLocal<>();

    /**
     * Once per &lt;suite&gt; (although every test class inherits it). Parameters come from testng.xml;
     * &#64;Optional("") makes them optional, e.g. when one class is run with -Dtest=CartTest.
     */
    @Parameters({"browser", "headless"})
    @BeforeSuite(alwaysRun = true)
    public void beforeSuite(@Optional("") String browser, @Optional("") String headless) {
        Config.setSuiteParameter("browser", browser);
        Config.setSuiteParameter("headless", headless);
        AllureSupport.cleanResults();
        SelenideConfig.configure(); // also starts the demo app (Config.baseUrl())
        resetBackend();
    }

    @BeforeMethod(alwaysRun = true)
    public void createTestUser() {
        AllureSupport.attachSelenideSteps(); // in the test's own thread: SelenideLogger listeners are ThreadLocal
        USER.set(TestUser.unique());
        API.set(new BackendApi());
    }

    /** Runs even when the test failed: delete this test's data, then a fresh browser for the next test. */
    @AfterMethod(alwaysRun = true)
    public void cleanUp() {
        try {
            api().cleanup(user());
        } catch (RuntimeException e) {
            System.err.println("[cleanup] failed for " + user().username() + ": " + e.getMessage());
        } finally {
            if (!Config.reuseBrowser()) {
                closeWebDriver();
            } else if (WebDriverRunner.hasWebDriverStarted()) {
                clearBrowserCookies();
                clearBrowserLocalStorage();
            }
            USER.remove();
            API.remove();
        }
    }

    protected TestUser user() {
        return USER.get();
    }

    /** E.g. useUser(TestUser.slow()) at the start of a test. */
    protected void useUser(TestUser user) {
        USER.set(user);
    }

    protected BackendApi api() {
        return API.get();
    }

    protected CatalogPage loginViaApi() {
        api().loginAndStartSession(user());
        return new CatalogPage().open();
    }

    private static void resetBackend() {
        try (HttpClient http = HttpClient.newHttpClient()) {
            HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(Config.baseUrl() + "/api/test/reset"))
                            .header("X-Test-Key", DemoApp.TEST_KEY)
                            .POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.ofString());
            System.out.println("[suite] @BeforeSuite: backend reset -> " + response.statusCode());
        } catch (IOException e) {
            throw new IllegalStateException("Backend is not reachable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
