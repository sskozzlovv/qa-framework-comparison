package com.compare.testng.tests;

/** Group names as constants: @Test(groups = Groups.SMOKE) - no typos, one place to rename. */
public final class Groups {

    public static final String SMOKE = "smoke";
    public static final String CRITICAL = "critical";
    public static final String REGRESSION = "regression";
    public static final String BROWSER_API = "browser-api";
    public static final String DEMO_FAILURE = "demo-failure";

    private Groups() {
    }
}
