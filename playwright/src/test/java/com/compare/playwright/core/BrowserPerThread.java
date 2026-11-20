package com.compare.playwright.core;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Playwright objects are NOT thread-safe: one Playwright (+ its Browser) per thread.
 * Launching a browser is expensive (~1 s), so it lives for the whole run; isolation between tests comes from a
 * fresh BrowserContext per test (~10 ms) - a new "incognito profile" with its own cookies/storage/pages.
 */
final class BrowserPerThread {

    record Holder(Playwright playwright, Browser browser) { }

    private static final ThreadLocal<Holder> HOLDER = new ThreadLocal<>();
    private static final List<Holder> ALL = new CopyOnWriteArrayList<>();

    private BrowserPerThread() {
    }

    static Holder get() {
        Holder holder = HOLDER.get();
        if (holder == null || !holder.browser().isConnected()) {
            holder = launch();
            HOLDER.set(holder);
            ALL.add(holder);
        }
        return holder;
    }

    private static Holder launch() {
        Map<String, String> env = new HashMap<>(System.getenv());
        if (!Config.channel().isBlank()) {
            env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1"); // we use the installed Chrome/Edge
        }
        Playwright playwright = Playwright.create(new Playwright.CreateOptions().setEnv(env));
        playwright.selectors().setTestIdAttribute("data-test"); // getByTestId("login-error") -> [data-test=...]
        BrowserType type = switch (Config.browser()) {
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> playwright.chromium();
        };
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(Config.headless());
        if (type == playwright.chromium() && !Config.channel().isBlank()) {
            options.setChannel(Config.channel());
        }
        return new Holder(playwright, type.launch(options));
    }

    static void closeAll() {
        for (Holder holder : ALL) {
            try {
                holder.playwright().close(); // closes the browser as well
            } catch (RuntimeException ignored) {
                // already gone
            }
        }
        ALL.clear();
    }
}
