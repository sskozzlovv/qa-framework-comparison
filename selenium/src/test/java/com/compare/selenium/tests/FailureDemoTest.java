package com.compare.selenium.tests;

import com.compare.selenium.pages.CatalogPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fails ON PURPOSE to show what a failure leaves behind (screenshot, HTML, console log in target/artifacts).
 * Excluded by default. Run: mvnw -pl selenium -am test -Dgroups=demo-failure -DexcludedGroups= -Dsurefire.failIfNoSpecifiedTests=false
 */
@Tag("demo-failure")
@DisplayName("Failure demo (fails on purpose)")
class FailureDemoTest extends BaseTest {

    @Test
    void catalogHasThirteenProducts() {
        CatalogPage catalog = loginViaApi();

        assertThat(catalog.productNames()).hasSize(13);
    }
}
