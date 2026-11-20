package com.compare.testng.tests;

import com.compare.testng.data.Product;
import com.compare.testng.data.TestUser;
import com.compare.testng.pages.CatalogPage;
import com.compare.testng.pages.LoginPage;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;

public class LoginTest extends BaseTest {

    @Test(groups = Groups.SMOKE, description = "User logs in through the form")
    public void userLogsInThroughTheForm() {
        CatalogPage catalog = new LoginPage().open().loginAs(user());

        catalog.header().username().shouldHave(exactText(user().username()));
        catalog.names().shouldHave(size(Product.CATALOG_SIZE));
    }

    @Test(groups = Groups.SMOKE)
    public void lockedUserSeesAnError() {
        TestUser locked = TestUser.locked();

        new LoginPage().open().submit(locked.username(), locked.password())
                .error().shouldHave(exactText("Sorry, this user has been locked out"));
    }

    /** JUnit: @ParameterizedTest + @CsvSource. TestNG: a method that returns the rows. */
    @DataProvider(name = "invalidCredentials")
    public Object[][] invalidCredentials() {
        return new Object[][] {
                {"", "secret", "Username and password are required"},
                {"someone", "wrong", "Username and password do not match any user"},
                {"ab", "secret", "Username and password do not match any user"},
        };
    }

    @Test(groups = Groups.REGRESSION, dataProvider = "invalidCredentials")
    public void invalidCredentialsAreRejected(String username, String password, String expectedError) {
        new LoginPage().open().submit(username, password).error().shouldHave(exactText(expectedError));
    }

    @Test(groups = Groups.REGRESSION)
    public void slowLoginBlocksThePageWithASpinner() {
        useUser(TestUser.slow());

        LoginPage page = new LoginPage().open().submit(user().username(), user().password());

        page.spinner().shouldBe(visible);
        new CatalogPage().waitLoaded().header().username().shouldHave(exactText(user().username()));
    }
}
