package com.compare.selenide.tests;

import com.compare.selenide.api.BackendApi;
import com.compare.selenide.core.SelenideSetup;
import com.compare.selenide.data.TestUser;
import com.compare.selenide.pages.CatalogPage;
import com.codeborne.selenide.junit5.ScreenShooterExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * SelenideSetup closes the browser in afterEach (= after this class's &#64;AfterEach, so cleanup still has a browser).
 * ScreenShooterExtension (built into Selenide) takes the screenshot in afterTestExecution - also for failures
 * that are not Selenide's own (AssertJ, exceptions). Failed should*() calls screenshot themselves anyway.
 */
@ExtendWith({SelenideSetup.class, ScreenShooterExtension.class})
public abstract class BaseTest {

    protected final BackendApi api = new BackendApi();
    protected TestUser user = TestUser.unique();

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
        return new CatalogPage().open();
    }
}
