package com.compare.selenium.pages;

import com.compare.selenium.core.Waits;
import com.compare.selenium.data.TestUser;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    private static final By USERNAME = By.id("username");
    private static final By PASSWORD = By.id("password");
    private static final By SUBMIT = By.id("login");
    private static final By ERROR = By.cssSelector("[data-test='login-error']");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open() {
        openPath("/index.html");
        return waitLoaded();
    }

    LoginPage waitLoaded() {
        waits.visible(USERNAME);
        return this;
    }

    public LoginPage submit(String username, String password) {
        waits.type(USERNAME, username);
        waits.type(PASSWORD, password);
        waits.click(SUBMIT);
        return this;
    }

    public CatalogPage loginAs(TestUser user) {
        submit(user.username(), user.password());
        return new CatalogPage(driver).waitLoaded();
    }

    public String errorText() {
        return waits.visible(ERROR).getText();
    }

    /** Waits until the overlay APPEARS (a slow login shows it long enough to be seen). */
    public boolean spinnerAppears() {
        waits.visible(Waits.SPINNER);
        return true;
    }
}
