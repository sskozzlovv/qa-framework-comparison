package com.compare.playwright.tests;

import com.compare.playwright.pages.CatalogPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Fails ON PURPOSE. Leaves screenshot.png, page.html and trace.zip (DOM snapshots of every step, network,
 * console - open it in the trace viewer) in target/artifacts/FailureDemoTest/. Excluded by default.
 * Run: mvnw -pl playwright -am test -Dgroups=demo-failure -DexcludedGroups= -Dsurefire.failIfNoSpecifiedTests=false
 */
@Tag("demo-failure")
@DisplayName("Failure demo (fails on purpose)")
class FailureDemoTest extends BaseTest {

    @Test
    void catalogHasThirteenProducts() {
        CatalogPage catalog = loginViaApi();

        assertThat(catalog.names()).hasCount(13);
    }
}
