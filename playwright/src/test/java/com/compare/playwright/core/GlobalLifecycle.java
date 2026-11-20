package com.compare.playwright.core;

import com.microsoft.playwright.assertions.PlaywrightAssertions;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Once per test run (the JUnit 5 replacement for TestNG &#64;BeforeSuite/&#64;AfterSuite):
 * start the app, set the global assertion timeout, and close every Playwright/browser at the end.
 */
public class GlobalLifecycle implements LauncherSessionListener {

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        Config.baseUrl();
        // assertThat(locator).hasText(...) retries up to this timeout (default 5 s); actions use context timeout
        PlaywrightAssertions.setDefaultAssertionTimeout(Config.timeout().toMillis());
    }

    @Override
    public void launcherSessionClosed(LauncherSession session) {
        BrowserPerThread.closeAll();
    }
}
