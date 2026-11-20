package com.compare.selenide.suites;

import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/** Fast feedback: is the app alive and can a user log in and shop? Run: mvnw -pl selenide -am test -Dtest=SmokeSuite -Dsurefire.failIfNoSpecifiedTests=false */
@Suite
@SuiteDisplayName("Selenide - SmokeSuite")
@SelectPackages("com.compare.selenide.tests")
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
