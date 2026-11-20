import { defineConfig, devices } from '@playwright/test';

/**
 * Everything the Java frameworks implement in code (DriverFactory, extensions, failure artifacts, retries,
 * parallelism, starting the app) is configuration here.
 */
const port = Number(process.env.APP_PORT ?? 4321);
const baseURL = process.env.BASE_URL ?? `http://127.0.0.1:${port}`;
const isCI = !!process.env.CI;

export default defineConfig({
  testDir: './tests',
  // the "before suite" of the whole run: reset backend data once
  globalSetup: require.resolve('./global-setup'),

  // every test - even tests of the same file - may run in parallel: each test has its own user (see fixtures)
  fullyParallel: true,
  workers: isCI ? 2 : 3,
  // test.only left in the code fails the CI run instead of silently skipping everything else
  forbidOnly: isCI,
  // retries are a band-aid; a test that passes on retry is reported as "flaky" in the HTML report
  retries: isCI ? 2 : 0,
  timeout: 60_000,

  expect: {
    timeout: 10_000, // web-first assertions (toHaveText, toBeVisible...) retry up to this
    toHaveScreenshot: { maxDiffPixelRatio: 0.01 },
  },
  // first run writes missing visual baselines instead of failing; changed ones still fail
  updateSnapshots: 'missing',

  // @demo-failure fails on purpose: excluded unless FAILURE_DEMO=1
  grepInvert: process.env.FAILURE_DEMO ? undefined : /@demo-failure/,

  reporter: [
    ['list'],
    // Sharded CI run (BLOB_REPORT=1, see .github/workflows/playwright.yml): every shard writes a "blob" -
    // raw results that `npx playwright merge-reports` later turns into ONE HTML report for all shards.
    process.env.BLOB_REPORT ? ['blob'] : ['html', { open: 'never' }],
    ['junit', { outputFile: 'test-results/junit.xml' }], // for CI dashboards, same format as surefire
    // Allure: the same report format as the Selenide modules; "epic" tells the frameworks apart in one report
    ['allure-playwright', { resultsDir: 'allure-results', globalLabels: { epic: 'Playwright TS' } }],
  ],

  use: {
    baseURL,
    testIdAttribute: 'data-test',
    actionTimeout: 10_000,
    navigationTimeout: 30_000,
    // failure artifacts - zero lines of code in the framework
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  projects: [
    {
      name: 'chrome',
      use: { ...devices['Desktop Chrome'], channel: 'chrome', viewport: { width: 1366, height: 900 } },
    },
    // Playwright's own browser builds: `npx playwright install firefox webkit` once, then ALL_BROWSERS=1
    ...(process.env.ALL_BROWSERS
      ? [
          { name: 'firefox', use: { ...devices['Desktop Firefox'], viewport: { width: 1366, height: 900 } } },
          { name: 'webkit', use: { ...devices['Desktop Safari'], viewport: { width: 1366, height: 900 } } },
        ]
      : []),
  ],

  // starts the system under test before the run and stops it afterwards
  webServer: process.env.BASE_URL
    ? undefined
    : {
        command: 'node scripts/start-demo-app.mjs',
        url: `${baseURL}/api/health`,
        env: { APP_PORT: String(port) },
        reuseExistingServer: !isCI, // locally `npm run app` in another terminal speeds up re-runs
        timeout: 180_000,           // first run builds demo-app with Maven
        stdout: 'ignore',
        stderr: 'pipe',
      },
});
