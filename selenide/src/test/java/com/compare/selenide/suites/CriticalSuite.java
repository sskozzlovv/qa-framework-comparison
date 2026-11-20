package com.compare.selenide.suites;

import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/** Business-critical flows (money path). Run: mvnw -pl selenide -am test -Dtest=CriticalSuite -Dsurefire.failIfNoSpecifiedTests=false */
@Suite
@SuiteDisplayName("Selenide - CriticalSuite")
@SelectPackages("com.compare.selenide.tests")
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
