package com.compare.playwright.tests;

import com.compare.playwright.api.ApiClient;
import com.compare.playwright.core.PlaywrightExtension;
import com.compare.playwright.data.TestUser;
import com.compare.playwright.pages.CatalogPage;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

/** Page and ApiClient are injected by the extension (fixture style) - a fresh BrowserContext per test. */
@ExtendWith(PlaywrightExtension.class)
public abstract class BaseTest {

    protected Page page;
    protected ApiClient api;
    protected TestUser user = TestUser.unique();

    @BeforeEach
    void injectFixtures(Page page, ApiClient api) {
        this.page = page;
        this.api = api;
    }

    /** Uses the API context, not the page: works even if the page is broken or blocked by a dialog. */
    @AfterEach
    void cleanUpBackendData() {
        try {
            api.cleanup(user);
        } catch (RuntimeException e) {
            System.err.println("[cleanup] failed for " + user.username() + ": " + e.getMessage());
        }
    }

    protected CatalogPage loginViaApi() {
        api.loginAndStartSession(page.context(), user);
        return new CatalogPage(page).open();
    }
}
