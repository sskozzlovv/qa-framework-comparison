package com.compare.selenide.suites;

import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/** Everything except the failure demo. Run: mvnw -pl selenide -am test -Dtest=FullRegressionSuite -Dsurefire.failIfNoSpecifiedTests=false */
@Suite
@SuiteDisplayName("Selenide - FullRegressionSuite")
@SelectPackages("com.compare.selenide.tests")
@ExcludeTags("demo-failure")
public class FullRegressionSuite {

    @BeforeSuite
    static void beforeSuite() {
        SuiteHooks.resetBackend("FullRegressionSuite");
    }

    @AfterSuite
    static void afterSuite() {
        System.out.println("[FullRegressionSuite] @AfterSuite: done");
    }
}
