package com.compare.selenium.core;

import com.sandbox.app.DemoApp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

/** framework.properties, overridable with -Dkey=value (e.g. -Dheadless=false -Dbrowser=firefox). */
public final class Config {

    private static final Properties FILE = load();

    private Config() {
    }

    public static String get(String key, String defaultValue) {
        String fromSystem = System.getProperty(key);
        if (fromSystem != null && !fromSystem.isBlank()) {
            return fromSystem.trim();
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

    /** Default timeout of every explicit wait. */
    public static Duration timeout() {
        return Duration.ofSeconds(Long.parseLong(get("timeout.seconds", "10")));
    }

    /** false = a brand-new browser for every test (clean, slower); true = one browser per thread, state wiped between tests. */
    public static boolean reuseBrowser() {
        return Boolean.parseBoolean(get("browser.reuse", "false"));
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
