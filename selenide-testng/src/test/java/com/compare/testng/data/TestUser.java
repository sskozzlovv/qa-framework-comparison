package com.compare.testng.data;

import java.util.UUID;

/**
 * Every test gets its OWN user -> its own cart and orders. That is what makes parallel runs and
 * per-test cleanup safe: cleaning "my" user can never break a test running next to me.
 */
public record TestUser(String username, String password) {

    public static final String PASSWORD = "secret";

    public static TestUser unique() {
        return new TestUser("user_" + shortId(), PASSWORD);
    }

    /** The app adds 4x latency for slow_* users (think "performance_glitch_user"). */
    public static TestUser slow() {
        return new TestUser("slow_" + shortId(), PASSWORD);
    }

    public static TestUser locked() {
        return new TestUser("locked_" + shortId(), PASSWORD);
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
