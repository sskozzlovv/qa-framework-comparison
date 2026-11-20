package com.compare.selenium.tests;

import com.compare.selenium.api.BackendApi;
import com.compare.selenium.core.Browser;
import com.compare.selenium.core.BrowserExtension;
import com.compare.selenium.data.TestUser;
import com.compare.selenium.pages.CatalogPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;

/**
 * Lifecycle per test: [extension] new browser -> setUp -> test -> [extension] screenshot if failed
 * -> cleanUp (backend data via JS fetch) -> [extension] quit browser.
 */
@ExtendWith(BrowserExtension.class)
public abstract class BaseTest {

    protected WebDriver driver;
    protected BackendApi api;
    /** A new instance of the test class is created per test method -> a new user per test. */
    protected TestUser user = TestUser.unique();

    @BeforeEach
    void setUpBrowserAndApi() {
        driver = Browser.driver();
        api = new BackendApi(driver);
    }

    /** Runs even when the test failed - removes the cart and orders created by this test. */
    @AfterEach
    void cleanUpBackendData() {
        try {
            api.cleanup(user);
        } catch (RuntimeException e) {
            System.err.println("[cleanup] failed for " + user.username() + ": " + e.getMessage());
        }
    }

    protected CatalogPage loginViaApi() {
        api.loginAndStartSession(user);
        return new CatalogPage(driver).open();
    }
}
