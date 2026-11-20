package com.compare.selenide.core;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Runs once per test run (Maven, IDE, single test) - the JUnit 5 replacement for TestNG &#64;BeforeSuite.
 * Starts the app and configures Selenide BEFORE any test thread exists: Configuration is a global static,
 * so writing it from parallel test threads would be a race.
 * <p>No "quit all browsers" needed at the end: Selenide registers a shutdown hook that closes its browsers.
 */
public class GlobalLifecycle implements LauncherSessionListener {

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        AllureSupport.cleanResults();
        SelenideSetup.configure();
    }
}
