package com.compare.testng.tests;

import com.compare.testng.pages.CatalogPage;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fails ON PURPOSE (an AssertJ failure, not a Selenide one) to check that FailureScreenshotListener still gets a
 * screenshot while the browser is open. Only in testng/demo-failure.xml:  ./mvnw -pl selenide-testng -am test -Dsuite=demo-failure
 */
public class FailureDemoTest extends BaseTest {

    @Test(groups = Groups.DEMO_FAILURE)
    public void catalogHasThirteenProducts() {
        CatalogPage catalog = loginViaApi();

        assertThat(catalog.names().texts()).hasSize(13);
    }
}
