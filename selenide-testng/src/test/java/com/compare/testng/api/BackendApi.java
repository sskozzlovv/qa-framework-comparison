package com.compare.testng.api;

import com.compare.testng.core.Config;
import com.compare.testng.data.TestUser;
import com.codeborne.selenide.WebDriverRunner;
import com.sandbox.app.DemoApp;

import java.util.HashMap;
import java.util.Map;

import static com.codeborne.selenide.Selenide.executeAsyncJavaScript;
import static com.codeborne.selenide.Selenide.localStorage;
import static com.codeborne.selenide.Selenide.open;

/**
 * Same idea as in the Selenium module (fetch() inside the browser via executeAsyncJavaScript) - Selenide only
 * shortens the syntax: no casts to JavascriptExecutor, localStorage() API out of the box.
 */
public final class BackendApi {

    private static final Map<String, String> TEST_KEY = Map.of("X-Test-Key", DemoApp.TEST_KEY);

    private static final String FETCH = """
            const [method, path, body, headers, done] = arguments;
            fetch(path, {
              method,
              headers: Object.assign({'Content-Type': 'application/json'}, headers),
              body: body == null ? undefined : JSON.stringify(body)
            })
              .then(async r => done({status: r.status, body: await r.json().catch(() => null)}))
              .catch(e => done({status: -1, body: String(e)}));
            """;

    private String token;

    public void loginAndStartSession(TestUser user) {
        Map<String, Object> body = call("POST", "/api/login",
                Map.of("username", user.username(), "password", user.password()), Map.of(), 200);
        token = (String) body.get("token");
        localStorage().setItem("token", token);
        localStorage().setItem("username", user.username());
        localStorage().setItem("promo.dismissed", "1");
    }

    public void addToCart(int productId) {
        call("POST", "/api/cart", Map.of("productId", productId), Map.of("X-Auth-Token", token), 200);
    }

    public void seedOrders(TestUser user, int count) {
        call("POST", "/api/test/seed-orders", Map.of("username", user.username(), "count", count), TEST_KEY, 200);
    }

    public Map<String, Object> cleanup(TestUser user) {
        return call("POST", "/api/test/cleanup", Map.of("username", user.username()), TEST_KEY, 200);
    }

    public long cartItems(TestUser user) {
        return ((Number) state(user).get("cartItems")).longValue();
    }

    public long orders(TestUser user) {
        return ((Number) state(user).get("orders")).longValue();
    }

    private Map<String, Object> state(TestUser user) {
        return call("GET", "/api/test/state?username=" + user.username(), null, TEST_KEY, 200);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(String method, String path, Object body, Map<String, String> headers, int expected) {
        ensureOnOrigin();
        Map<String, Object> response = executeAsyncJavaScript(FETCH, method, path, body, new HashMap<>(headers));
        long status = ((Number) response.get("status")).longValue();
        if (status != expected) {
            throw new IllegalStateException(method + " " + path + " -> " + status + " " + response.get("body"));
        }
        return (Map<String, Object>) response.get("body");
    }

    /** The browser is started lazily by open() - and localStorage/fetch need the app origin, not about:blank. */
    private void ensureOnOrigin() {
        if (!WebDriverRunner.hasWebDriverStarted() || !WebDriverRunner.url().startsWith(Config.baseUrl())) {
            open("/blank.html");
        }
    }
}
