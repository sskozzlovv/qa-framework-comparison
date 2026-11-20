package com.compare.selenide.tests;

import com.compare.selenide.data.Product;
import com.compare.selenide.data.TestUser;
import com.compare.selenide.pages.CatalogPage;
import com.compare.selenide.pages.LoginPage;
import com.compare.selenide.tags.Regression;
import com.compare.selenide.tags.Smoke;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;

@DisplayName("Login")
class LoginTest extends BaseTest {

    @Test
    @Smoke
    void userLogsInThroughTheForm() {
        CatalogPage catalog = new LoginPage().open().loginAs(user);

        catalog.header().username().shouldHave(exactText(user.username()));
        catalog.names().shouldHave(size(Product.CATALOG_SIZE));
    }

    @Test
    @Smoke
    void lockedUserSeesAnError() {
        TestUser locked = TestUser.locked();

        new LoginPage().open().submit(locked.username(), locked.password())
                .error().shouldHave(exactText("Sorry, this user has been locked out"));
    }

    @Regression
    @ParameterizedTest(name = "user=''{0}'' password=''{1}''")
    @CsvSource({
            "'',        secret, Username and password are required",
            "someone,   wrong,  Username and password do not match any user",
            "ab,        secret, Username and password do not match any user"
    })
    void invalidCredentialsAreRejected(String username, String password, String expectedError) {
        new LoginPage().open().submit(username, password).error().shouldHave(exactText(expectedError));
    }

    @Test
    @Regression
    void slowLoginBlocksThePageWithASpinner() {
        user = TestUser.slow();

        LoginPage page = new LoginPage().open().submit(user.username(), user.password());

        page.spinner().shouldBe(visible);
        new CatalogPage().waitLoaded().header().username().shouldHave(exactText(user.username()));
    }
}
