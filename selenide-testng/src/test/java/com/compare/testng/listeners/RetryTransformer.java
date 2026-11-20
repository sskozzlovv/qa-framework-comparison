package com.compare.testng.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Puts RetryAnalyzer on every &#64;Test. Pitfall: an IAnnotationTransformer works ONLY when registered in
 * testng.xml &lt;listeners&gt; (or via ServiceLoader / -listener); &#64;Listeners on a test class is too late -
 * the annotations are already read by then.
 */
public class RetryTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}
