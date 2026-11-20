package com.compare.selenium.tests;

import com.compare.selenium.data.Product;
import com.compare.selenium.data.TestUser;
import com.compare.selenium.pages.CatalogPage;
import com.compare.selenium.pages.LoginPage;
import com.compare.selenium.tags.Regression;
import com.compare.selenium.tags.Smoke;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Login")
class LoginTest extends BaseTest {

    @Test
    @Smoke
    void userLogsInThroughTheForm() {
        CatalogPage catalog = new LoginPage(driver).open().loginAs(user);

        assertThat(catalog.header().username()).isEqualTo(user.username());
        assertThat(catalog.productNames()).hasSize(Product.CATALOG_SIZE);
    }

    @Test
    @Smoke
    void lockedUserSeesAnError() {
        TestUser locked = TestUser.locked();

        LoginPage page = new LoginPage(driver).open().submit(locked.username(), locked.password());

        assertThat(page.errorText()).isEqualTo("Sorry, this user has been locked out");
    }

    @Regression
    @ParameterizedTest(name = "user=''{0}'' password=''{1}''")
    @CsvSource({
            "'',        secret, Username and password are required",
            "someone,   wrong,  Username and password do not match any user",
            "ab,        secret, Username and password do not match any user"
    })
    void invalidCredentialsAreRejected(String username, String password, String expectedError) {
        LoginPage page = new LoginPage(driver).open().submit(username, password);

        assertThat(page.errorText()).isEqualTo(expectedError);
    }

    @Test
    @Regression
    void slowLoginBlocksThePageWithASpinner() {
        user = TestUser.slow();

        LoginPage page = new LoginPage(driver).open().submit(user.username(), user.password());

        assertThat(page.spinnerAppears()).isTrue();
        // Do NOT driver.get(catalog) here: navigating away aborts the pending login request and the token is
        // never saved. Wait for the app's own redirect instead.
        assertThat(new CatalogPage(driver).waitLoaded().header().username()).isEqualTo(user.username());
    }
}
