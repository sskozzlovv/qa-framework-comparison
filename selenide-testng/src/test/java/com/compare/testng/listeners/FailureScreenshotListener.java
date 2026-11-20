package com.compare.testng.listeners;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.compare.testng.core.AllureSupport;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

/**
 * Screenshot of a failed test (any failure - also AssertJ, not only Selenide's should*()).
 *
 * <p>The browser is closed in BaseTest's &#64;AfterMethod, so the screenshot must be taken before it. Order in
 * TestNG 7.12 (checked with the failure demo): test method -&gt; IInvokedMethodListener.afterInvocation -&gt;
 * retry decision -&gt; ITestListener.onTestFailure / onTestSkipped -&gt; &#64;AfterMethod. Both listener types run
 * while the browser is still open; afterInvocation is the earliest point, right after the test body.
 * (JUnit 5 has the same concern: AfterTestExecutionCallback is in time, TestWatcher is too late.)
 */
public class FailureScreenshotListener implements IInvokedMethodListener {

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        if (!method.isTestMethod() || result.isSuccess() || !WebDriverRunner.hasWebDriverStarted()) {
            return;
        }
        String name = result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName();
        String path = Selenide.screenshot(name); // png + html into Configuration.reportsFolder
        System.out.println("[artifacts] " + name + " -> " + path);
        AllureSupport.attachFailure(result.getThrowable()); // the Allure test is still open here (onTestFailure comes later)
    }
}
