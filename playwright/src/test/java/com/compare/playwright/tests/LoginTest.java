package com.compare.playwright.tests;

import com.compare.playwright.data.Product;
import com.compare.playwright.data.TestUser;
import com.compare.playwright.pages.CatalogPage;
import com.compare.playwright.pages.LoginPage;
import com.compare.playwright.tags.Regression;
import com.compare.playwright.tags.Smoke;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@DisplayName("Login")
class LoginTest extends BaseTest {

    @Test
    @Smoke
    void userLogsInThroughTheForm() {
        CatalogPage catalog = new LoginPage(page).open().loginAs(user);

        assertThat(catalog.header().username()).hasText(user.username());
        assertThat(catalog.names()).hasCount(Product.CATALOG_SIZE);
    }

    @Test
    @Smoke
    void lockedUserSeesAnError() {
        TestUser locked = TestUser.locked();

        LoginPage login = new LoginPage(page).open().submit(locked.username(), locked.password());

        assertThat(login.error()).hasText("Sorry, this user has been locked out");
    }

    @Regression
    @ParameterizedTest(name = "user=''{0}'' password=''{1}''")
    @CsvSource({
            "'',        secret, Username and password are required",
            "someone,   wrong,  Username and password do not match any user",
            "ab,        secret, Username and password do not match any user"
    })
    void invalidCredentialsAreRejected(String username, String password, String expectedError) {
        LoginPage login = new LoginPage(page).open().submit(username, password);

        assertThat(login.error()).hasText(expectedError);
    }

    @Test
    @Regression
    void slowLoginBlocksThePageWithASpinner() {
        user = TestUser.slow();

        LoginPage login = new LoginPage(page).open().submit(user.username(), user.password());

        assertThat(login.spinner()).isVisible();
        assertThat(new CatalogPage(page).waitLoaded().header().username()).hasText(user.username());
    }
}
