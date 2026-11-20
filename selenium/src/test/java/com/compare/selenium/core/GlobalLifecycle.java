package com.compare.selenium.core;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * JUnit 5 has no TestNG-style &#64;BeforeSuite that runs for ANY selection of tests. The closest thing is a
 * LauncherSessionListener (registered in META-INF/services): it runs once per test run - Maven, IDE, single test.
 * <ul>
 *   <li>opened: start the system under test once (instead of in every test class)</li>
 *   <li>closed: kill every browser that may still be alive (crashed tests, browser.reuse=true)</li>
 * </ul>
 * &#64;BeforeSuite/&#64;AfterSuite of junit-platform-suite only run when you start a &#64;Suite class (see suites/).
 */
public class GlobalLifecycle implements LauncherSessionListener {

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        Config.baseUrl();
    }

    @Override
    public void launcherSessionClosed(LauncherSession session) {
        Browser.quitAll();
    }
}
