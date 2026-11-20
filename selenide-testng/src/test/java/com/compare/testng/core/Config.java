package com.compare.testng.core;

import com.sandbox.app.DemoApp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Settings, highest priority first: -Dkey=value, testng.xml &lt;parameter&gt;, framework.properties, default.
 * The testng.xml layer is the TestNG-specific part: a suite file can say "this run uses firefox".
 */
public final class Config {

    private static final Properties FILE = load();
    /** Filled once from &#64;BeforeSuite (&#64;Parameters of the suite XML). */
    private static final Map<String, String> SUITE_PARAMETERS = new ConcurrentHashMap<>();

    private Config() {
    }

    /** Empty / missing XML parameters (&#64;Optional("")) are ignored. */
    public static void setSuiteParameter(String key, String value) {
        if (value != null && !value.isBlank()) {
            SUITE_PARAMETERS.put(key, value.trim());
        }
    }

    public static String get(String key, String defaultValue) {
        String fromSystem = System.getProperty(key);
        if (fromSystem != null && !fromSystem.isBlank()) {
            return fromSystem.trim();
        }
        String fromSuite = SUITE_PARAMETERS.get(key);
        if (fromSuite != null) {
            return fromSuite;
        }
        return FILE.getProperty(key, defaultValue).trim();
    }

    /** -DbaseUrl=... or the in-process demo app (started once per JVM). */
    public static String baseUrl() {
        return DemoApp.startShared();
    }

    public static String browser() {
        return get("browser", "chrome").toLowerCase();
    }

    public static boolean headless() {
        return Boolean.parseBoolean(get("headless", "true"));
    }

    /** Default timeout of every should*() / click(). */
    public static Duration timeout() {
        return Duration.ofSeconds(Long.parseLong(get("timeout.seconds", "10")));
    }

    /** false = a brand-new browser for every test; true = one browser per thread, state wiped between tests. */
    /**
     * Selenium Grid URL, e.g. -Dremote=http://selenium-hub:4444 (Jenkins stand). Empty = a local browser on this machine.
     * With a Grid the browser runs elsewhere: baseUrl must be reachable FROM THE GRID NODE, not only from here.
     */
    public static String remote() {
        return get("remote", "");
    }

    public static boolean reuseBrowser() {
        return Boolean.parseBoolean(get("browser.reuse", "false"));
    }

    /** How many times a failed test is retried (RetryAnalyzer). */
    public static int retries() {
        return Integer.parseInt(get("test.retries", "0"));
    }

    public static Path artifactsDir() {
        return Path.of(get("artifacts.dir", "target/artifacts"));
    }

    private static Properties load() {
        Properties p = new Properties();
        try (InputStream in = Config.class.getResourceAsStream("/framework.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read framework.properties", e);
        }
        return p;
    }
}
