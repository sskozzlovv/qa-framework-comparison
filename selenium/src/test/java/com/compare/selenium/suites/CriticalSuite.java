package com.compare.selenium.suites;

import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/** Business-critical flows (money path). Run: mvnw -pl selenium -am test -Dtest=CriticalSuite -Dsurefire.failIfNoSpecifiedTests=false */
@Suite
@SuiteDisplayName("Selenium - CriticalSuite")
@SelectPackages("com.compare.selenium.tests")
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
