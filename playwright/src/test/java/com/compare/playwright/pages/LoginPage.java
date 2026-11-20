package com.compare.playwright.pages;

import com.compare.playwright.data.TestUser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** User-facing locators: by label, role and test id - the way Playwright recommends (resilient to markup changes). */
public class LoginPage extends BasePage {

    private final Locator username;
    private final Locator password;
    private final Locator submit;
    private final Locator error;

    public LoginPage(Page page) {
        super(page);
        this.username = page.getByLabel("Username");
        this.password = page.getByLabel("Password");
        this.submit = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in"));
        this.error = page.getByTestId("login-error");
    }

    public LoginPage open() {
        page.navigate("/index.html"); // relative to the context baseURL
        return this;
    }

    public LoginPage submit(String user, String pass) {
        username.fill(user);
        password.fill(pass);
        submit.click();
        return this;
    }

    public CatalogPage loginAs(TestUser user) {
        submit(user.username(), user.password());
        return new CatalogPage(page).waitLoaded();
    }

    public Locator error() {
        return error;
    }
}
