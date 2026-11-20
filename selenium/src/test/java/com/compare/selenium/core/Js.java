package com.compare.selenium.core;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

/**
 * JavaScript helpers.
 * <ul>
 *   <li>executeScript: synchronous; "return x" returns a value. JS number -> Long/Double, object -> Map,
 *       array -> List, DOM element -> WebElement.</li>
 *   <li>executeAsyncScript: the LAST argument is a callback; the script must call it, otherwise the command fails
 *       after the script timeout. That is how you wait for a Promise (e.g. fetch) inside the browser.</li>
 * </ul>
 */
public final class Js {

    private final JavascriptExecutor js;

    public Js(WebDriver driver) {
        this.js = (JavascriptExecutor) driver;
    }

    @SuppressWarnings("unchecked")
    public <T> T exec(String script, Object... args) {
        return (T) js.executeScript(script, args);
    }

    public void setLocalStorage(String key, String value) {
        exec("localStorage.setItem(arguments[0], arguments[1]);", key, value);
    }

    public String getLocalStorage(String key) {
        return exec("return localStorage.getItem(arguments[0]);", key);
    }

    public void scrollIntoView(WebElement element) {
        exec("arguments[0].scrollIntoView({block: 'center'});", element);
    }

    /**
     * fetch() executed BY THE BROWSER: same origin, same cookies, no CORS, no HTTP client in the test code.
     * Returns {status: Long, body: parsed JSON or null}. Requires the browser to be on the app origin.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> fetch(String method, String path, Object body, Map<String, String> headers) {
        return (Map<String, Object>) js.executeAsyncScript("""
                const [method, path, body, headers, done] = arguments;
                fetch(path, {
                  method,
                  headers: Object.assign({'Content-Type': 'application/json'}, headers),
                  body: body == null ? undefined : JSON.stringify(body)
                })
                  .then(async r => done({status: r.status, body: await r.json().catch(() => null)}))
                  .catch(e => done({status: -1, body: String(e)}));
                """, method, path, body, headers);
    }
}
