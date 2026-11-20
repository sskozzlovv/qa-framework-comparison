package com.compare.playwright.suites;

import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/** Fast feedback: is the app alive and can a user log in and shop? Run: mvnw -pl playwright -am test -Dtest=SmokeSuite -Dsurefire.failIfNoSpecifiedTests=false */
@Suite
@SuiteDisplayName("Playwright - SmokeSuite")
@SelectPackages("com.compare.playwright.tests")
@IncludeTags("smoke")
public class SmokeSuite {

    @BeforeSuite
    static void beforeSuite() {
        SuiteHooks.resetBackend("SmokeSuite");
    }

    @AfterSuite
    static void afterSuite() {
        System.out.println("[SmokeSuite] @AfterSuite: done");
    }
}
