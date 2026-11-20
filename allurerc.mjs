// Allure Report 3 configuration: ONE report for the frameworks that write Allure results.
// Each framework writes its own allure-results folder and labels its tests with epic = framework name
// (Selenide modules: allure.label.epic in pom.xml, Playwright TS: globalLabels in playwright.config.ts).
// Build it with:  npm run allure3:generate   (or  npm run allure3:open  to build and open it).
// Allure 2 (the classic report) does not read this file: npm run allure2:open
import { defineConfig } from 'allure';

export default defineConfig({
  name: 'Sandbox Shop: Selenide (JUnit 5 / TestNG) vs Playwright TS',
  output: './allure-report-3',
  plugins: {
    awesome: {
      options: {
        reportName: 'Sandbox Shop: Selenide (JUnit 5 / TestNG) vs Playwright TS',
        // tree: framework -> the suite levels each adapter fills in (missing levels are skipped):
        //   JUnit 5:  parentSuite = "Selenide + JUnit 5" (selenide/pom.xml), suite = @DisplayName of the class
        //   TestNG:   parentSuite = <suite name>, suite = <test name>, subSuite = class
        //   Playwright TS: parentSuite = project, suite = spec file, subSuite = describe
        groupBy: ['epic', 'parentSuite', 'suite', 'subSuite'],
        // one self-contained index.html (data and attachments inside): opens with a double click, no web server.
        // A multi-file report loads every test with fetch() and shows "Failed to fetch" when opened as file://.
        singleFile: true,
      },
    },
  },
});
