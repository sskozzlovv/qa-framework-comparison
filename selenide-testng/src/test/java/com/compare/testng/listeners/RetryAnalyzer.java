package com.compare.testng.listeners;

import com.compare.testng.core.Config;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * TestNG retries live in code, not in Maven (surefire's rerunFailingTestsCount works for JUnit only).
 * Can be put on one test: &#64;Test(retryAnalyzer = RetryAnalyzer.class); RetryTransformer puts it on all tests.
 * Number of retries: -Dtest.retries=2 (default 0).
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private int attempt;

    @Override
    public boolean retry(ITestResult result) {
        if (attempt < Config.retries()) {
            attempt++;
            System.out.println("[retry] " + result.getName() + " attempt " + (attempt + 1));
            return true;
        }
        return false;
    }
}
