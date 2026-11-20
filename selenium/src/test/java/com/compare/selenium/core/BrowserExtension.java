package com.compare.selenium.core;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Browser lifecycle for plain Selenium.
 *
 * <p>JUnit 5 order for one test (this is THE thing to understand for screenshots):
 * <pre>
 *   BeforeEachCallback.beforeEach        <- start browser (here)
 *   &#64;BeforeEach methods                  <- test setup can already use the browser
 *   test method
 *   AfterTestExecutionCallback           <- screenshot on failure (here) - browser still alive
 *   &#64;AfterEach methods                   <- test cleanup (e.g. /api/test/cleanup) can still use the browser
 *   AfterEachCallback.afterEach          <- quit browser (here)
 *   TestWatcher.testFailed               <- TOO LATE for a screenshot, the browser is already gone
 * </pre>
 */
public class BrowserExtension implements BeforeEachCallback, AfterTestExecutionCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        if (!Browser.isStarted()) {
            Browser.start();
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        context.getExecutionException().ifPresent(error -> FailureArtifacts.collect(Browser.driver(), context, error));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (Config.reuseBrowser()) {
            try {
                Browser.resetState();
            } catch (RuntimeException e) {
                Browser.quit(); // the browser is broken - next test gets a new one
            }
        } else {
            Browser.quit(); // a clean browser for every test: no cookies, storage, open tabs or dialogs leak
        }
    }
}
