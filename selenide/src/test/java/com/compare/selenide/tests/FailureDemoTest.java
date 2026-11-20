package com.compare.selenide.tests;

import com.compare.selenide.pages.CatalogPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.CollectionCondition.size;

/**
 * Fails ON PURPOSE. Look at the error message: Selenide prints the screenshot + page source paths itself
 * (target/artifacts/selenide-reports). Excluded by default.
 * Run: mvnw -pl selenide -am test -Dgroups=demo-failure -DexcludedGroups= -Dsurefire.failIfNoSpecifiedTests=false
 */
@Tag("demo-failure")
@DisplayName("Failure demo (fails on purpose)")
class FailureDemoTest extends BaseTest {

    @Test
    void catalogHasThirteenProducts() {
        CatalogPage catalog = loginViaApi();

        catalog.names().shouldHave(size(13));
    }
}
