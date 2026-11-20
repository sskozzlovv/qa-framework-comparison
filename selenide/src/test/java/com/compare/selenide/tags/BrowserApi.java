package com.compare.selenide.tags;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Composed annotation = @Tag("browser-api") without typos. Run with -Dgroups=browser-api. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag("browser-api")
public @interface BrowserApi {
}
