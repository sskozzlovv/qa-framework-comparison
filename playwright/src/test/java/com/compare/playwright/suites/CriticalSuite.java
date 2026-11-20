package com.compare.playwright.suites;

import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/** Business-critical flows (money path). Run: mvnw -pl playwright -am test -Dtest=CriticalSuite -Dsurefire.failIfNoSpecifiedTests=false */
@Suite
@SuiteDisplayName("Playwright - CriticalSuite")
@SelectPackages("com.compare.playwright.tests")
@IncludeTags("critical")
public class CriticalSuite {

    @BeforeSuite
    static void beforeSuite() {
        SuiteHooks.resetBackend("CriticalSuite");
    }

    @AfterSuite
    static void afterSuite() {
        System.out.println("[CriticalSuite] @AfterSuite: done");
    }
}
