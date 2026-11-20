package com.compare.selenium.suites;

import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/** Everything except the failure demo. Run: mvnw -pl selenium -am test -Dtest=FullRegressionSuite -Dsurefire.failIfNoSpecifiedTests=false */
@Suite
@SuiteDisplayName("Selenium - FullRegressionSuite")
@SelectPackages("com.compare.selenium.tests")
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
