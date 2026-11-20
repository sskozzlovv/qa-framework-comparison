package com.compare.testng.listeners;

import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Arrays;

/** One line per test in the console: thread, status, groups, duration. Registered in testng.xml &lt;listeners&gt;. */
public class ConsoleLogListener implements ITestListener {

    @Override
    public void onTestSuccess(ITestResult result) {
        log("PASSED ", result);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        log("FAILED ", result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log("SKIPPED", result);
    }

    private static void log(String status, ITestResult result) {
        String params = result.getParameters().length == 0 ? "" : Arrays.toString(result.getParameters());
        System.out.printf("[%s] %s %s.%s%s %s (%d ms)%n", Thread.currentThread().getName(), status,
                result.getTestClass().getRealClass().getSimpleName(), result.getMethod().getMethodName(), params,
                Arrays.toString(result.getMethod().getGroups()), result.getEndMillis() - result.getStartMillis());
    }
}
