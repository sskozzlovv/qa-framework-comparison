package com.compare.selenide.pages;

import com.compare.selenide.data.TestUser;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;
import com.codeborne.selenide.Selenide;

import static com.codeborne.selenide.Selenide.$;

public class LoginPage extends BasePage {

    private final SelenideElement username = $("#username");
    private final SelenideElement password = $("#password");
    private final SelenideElement submit = $("#login");
    private final SelenideElement error = $("[data-test='login-error']");

    public LoginPage open() {
        Selenide.open("/index.html"); // relative to Configuration.baseUrl
        username.shouldBe(visible);
        return this;
    }

    /** setValue = clear + type; click() waits until the button is visible and retries if it is covered. */
    public LoginPage submit(String user, String pass) {
        username.setValue(user);
        password.setValue(pass);
        submit.click();
        return this;
    }

    public CatalogPage loginAs(TestUser user) {
        submit(user.username(), user.password());
        return new CatalogPage().waitLoaded();
    }

    public SelenideElement error() {
        return error;
    }
}
