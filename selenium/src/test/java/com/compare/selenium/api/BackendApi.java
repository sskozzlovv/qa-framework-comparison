package com.compare.selenium.api;

import com.compare.selenium.core.Config;
import com.compare.selenium.core.Js;
import com.compare.selenium.data.TestUser;
import com.sandbox.app.DemoApp;
import org.openqa.selenium.WebDriver;

import java.util.HashMap;
import java.util.Map;

/**
 * Talks to the backend THROUGH THE BROWSER with executeAsyncScript + fetch().
 *
 * <p>Pros: no HTTP client, same origin/cookies as the UI. Cons (pitfalls!):
 * <ul>
 *   <li>the browser must already be on the app origin (about:blank has no origin -> relative fetch fails,
 *       and localStorage of about:blank is not the app's localStorage);</li>
 *   <li>an open alert blocks every script;</li>
 *   <li>if the browser crashed, cleanup cannot run - so cleanup failures are logged, not thrown.</li>
 * </ul>
 * Compare with Playwright, which has a separate APIRequestContext that does not need a page at all.
 */
public final class BackendApi {

    private static final Map<String, String> TEST_KEY = Map.of("X-Test-Key", DemoApp.TEST_KEY);

    private final WebDriver driver;
    private final Js js;
    private String token;

    public BackendApi(WebDriver driver) {
        this.driver = driver;
        this.js = new Js(driver);
    }

    /**
     * UI login takes seconds and is already covered by LoginTest. Every other test logs in via API and puts the
     * token where the front-end expects it - localStorage.
     */
    public void loginAndStartSession(TestUser user) {
        ensureOnOrigin();
        Map<String, Object> body = call("POST", "/api/login",
                Map.of("username", user.username(), "password", user.password()), Map.of(), 200);
        token = (String) body.get("token");
        js.setLocalStorage("token", token);
        js.setLocalStorage("username", user.username());
        js.setLocalStorage("promo.dismissed", "1"); // don't let the random promo modal break unrelated tests
    }

    public void addToCart(int productId) {
        if (token == null) {
            throw new IllegalStateException("Call loginAndStartSession() first");
        }
        call("POST", "/api/cart", Map.of("productId", productId), Map.of("X-Auth-Token", token), 200);
    }

    public void seedOrders(TestUser user, int count) {
        call("POST", "/api/test/seed-orders", Map.of("username", user.username(), "count", count), TEST_KEY, 200);
    }

    /** The "/cleanup" hook: deletes the cart and every order of the user. */
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
        Map<String, Object> response = js.fetch(method, path, body, new HashMap<>(headers));
        long status = ((Number) response.get("status")).longValue();
        if (status != expected) {
            throw new IllegalStateException(method + " " + path + " -> " + status + " " + response.get("body"));
        }
        return (Map<String, Object>) response.get("body");
    }

    private void ensureOnOrigin() {
        if (!driver.getCurrentUrl().startsWith(Config.baseUrl())) {
            driver.get(Config.baseUrl() + "/blank.html");
        }
    }
}
