package com.compare.playwright.api;

import com.compare.playwright.data.TestUser;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import com.sandbox.app.DemoApp;

import java.util.Map;

/**
 * Playwright's built-in HTTP client (APIRequestContext). Unlike the Selenium/Selenide modules it needs NO page:
 * it works before the browser opened anything, after the page crashed, or when an alert blocks the page.
 */
public final class ApiClient implements AutoCloseable {

    private static final Gson GSON = new Gson();

    private final APIRequestContext request;
    private String token;

    public ApiClient(Playwright playwright, String baseUrl) {
        this.request = playwright.request().newContext(new APIRequest.NewContextOptions().setBaseURL(baseUrl));
    }

    public void loginAndStartSession(BrowserContext context, TestUser user) {
        loginAndStartSession(context, user, true);
    }

    /**
     * addInitScript runs BEFORE any page script on every navigation of the context - no need to open the origin
     * first (the Selenium pitfall). The sessionStorage guard makes it run once per tab, otherwise it would put the
     * token back after every navigation (e.g. right after a logout).
     */
    public void loginAndStartSession(BrowserContext context, TestUser user, boolean dismissPromo) {
        JsonObject body = send("POST", "/api/login",
                Map.of("username", user.username(), "password", user.password()), Map.of());
        token = body.get("token").getAsString();
        context.addInitScript("""
                (() => {
                  if (sessionStorage.getItem('__seeded')) return;
                  sessionStorage.setItem('__seeded', '1');
                  localStorage.setItem('token', %s);
                  localStorage.setItem('username', %s);
                  if (%s) localStorage.setItem('promo.dismissed', '1');
                })();
                """.formatted(GSON.toJson(token), GSON.toJson(user.username()), dismissPromo));
    }

    public void addToCart(int productId) {
        send("POST", "/api/cart", Map.of("productId", productId), Map.of("X-Auth-Token", token));
    }

    public void seedOrders(TestUser user, int count) {
        send("POST", "/api/test/seed-orders", Map.of("username", user.username(), "count", count), testKey());
    }

    public JsonObject cleanup(TestUser user) {
        return send("POST", "/api/test/cleanup", Map.of("username", user.username()), testKey());
    }

    public long cartItems(TestUser user) {
        return state(user).get("cartItems").getAsLong();
    }

    public long orders(TestUser user) {
        return state(user).get("orders").getAsLong();
    }

    private JsonObject state(TestUser user) {
        return send("GET", "/api/test/state?username=" + user.username(), null, testKey());
    }

    private static Map<String, String> testKey() {
        return Map.of("X-Test-Key", DemoApp.TEST_KEY);
    }

    private JsonObject send(String method, String path, Object data, Map<String, String> headers) {
        RequestOptions options = RequestOptions.create().setMethod(method);
        headers.forEach(options::setHeader);
        if (data != null) {
            options.setData(data); // serialized to JSON
        }
        APIResponse response = request.fetch(path, options);
        if (!response.ok()) {
            throw new IllegalStateException(method + " " + path + " -> " + response.status() + " " + response.text());
        }
        return GSON.fromJson(response.text(), JsonObject.class);
    }

    @Override
    public void close() {
        request.dispose();
    }
}
