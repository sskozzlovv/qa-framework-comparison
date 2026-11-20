# Selenide (JUnit 5 / TestNG) ↔ Playwright Test (TypeScript): построчное сравнение

Сравниваются модули этого репозитория: [`selenide/`](selenide) (Selenide + JUnit 5),
[`selenide-testng/`](selenide-testng) (тот же Selenide, раннер TestNG) и [`playwright-ts/`](playwright-ts).
Сценарии у них одинаковые, поэтому почти каждая строка слева имеет пару справа.

Разделы 1–5 и 11–13 — в три колонки: **Selenide + JUnit 5 | Selenide + TestNG | Playwright TS** (структура, хуки,
BaseTest, запуски, конфиги, теги, отчёты). Разделы 6–10 — про библиотеку (локаторы, действия, ожидания, браузер,
API): они одинаковы для обоих Selenide-модулей, поэтому там две колонки — Selenide ↔ Playwright TS.
Раздел 14 — три раннера рядом: **TestNG ↔ JUnit 5 ↔ Playwright Test**. Раздел 15 — что есть только в TestNG.

Обозначения:
- ✅ — код есть в репозитории (указан файл);
- 💡 — в репозитории так не сделано, это пример «как это пишется»;
- — — аналога нет.

Содержание:

1. [Структура проекта: какой файл чему соответствует](#1-структура-проекта-какой-файл-чему-соответствует)
2. [Жизненный цикл: before/after на всех уровнях](#2-жизненный-цикл-beforeafter-на-всех-уровнях)
3. [BaseTest ↔ фикстуры](#3-basetest--фикстуры)
4. [Запуски из командной строки](#4-запуски-из-командной-строки)
5. [Конфигурация построчно](#5-конфигурация-построчно)
6. [Page Object и локаторы](#6-page-object-и-локаторы)
7. [Действия: клики, ввод, выбор, файлы](#7-действия-клики-ввод-выбор-файлы)
8. [Ожидания и проверки](#8-ожидания-и-проверки)
9. [Браузерные возможности](#9-браузерные-возможности)
10. [Тестовые данные, API и логин без UI](#10-тестовые-данные-api-и-логин-без-ui)
11. [Теги, сьюты, параметризация, шаги](#11-теги-сьюты-параметризация-шаги)
12. [Артефакты падения и отчёты](#12-артефакты-падения-и-отчёты)
13. [Короткий словарь «перевода»](#13-короткий-словарь-перевода)
14. [TestNG ↔ JUnit 5 ↔ Playwright Test](#14-testng--junit-5--playwright-test)
15. [Что есть только в TestNG](#15-что-есть-только-в-testng)

---

## 1. Структура проекта: какой файл чему соответствует

Пути Selenide-модулей даны от `src/test/java/com/compare/selenide/` (JUnit 5) и `src/test/java/com/compare/testng/` (TestNG).

| Роль | Selenide + JUnit 5 (`selenide/`) | Selenide + TestNG (`selenide-testng/`) | Playwright TS (`playwright-ts/`) |
|---|---|---|---|
| Сборка и зависимости | родительский `pom.xml` + `selenide/pom.xml` | родительский `pom.xml` + `selenide-testng/pom.xml` (TestNG 7.12, `selenide-testng`) | `package.json` |
| Настройки раннера | surefire в `pom.xml` + `src/test/resources/junit-platform.properties` | surefire **3.5.6** в `selenide-testng/pom.xml` + **XML-сьюты** `src/test/resources/testng/*.xml` | `playwright.config.ts` |
| Настройки фреймворка | `src/test/resources/framework.properties` + `core/Config.java` | те же `framework.properties` + `core/Config.java` (ещё слой — `<parameter>` из XML) | `playwright.config.ts` (тот же файл) |
| Настройка браузера | `core/SelenideSetup.java` (`Configuration.*`) | `core/SelenideConfig.java` (те же `Configuration.*`) | `playwright.config.ts` → `use`, `projects` |
| «Один раз за прогон» | `core/GlobalLifecycle.java` + `META-INF/services/...LauncherSessionListener` | `@BeforeSuite` в `tests/BaseTest.java` | `global-setup.ts` + `webServer` в конфиге |
| Базовый класс тестов | `tests/BaseTest.java` (поля + extensions) | `tests/BaseTest.java` (`@BeforeMethod`/`@AfterMethod`, `ThreadLocal`) | `support/fixtures.ts` (фикстуры вместо базового класса) |
| Listener'ы / расширения | `SelenideSetup` (extension), `ScreenShooterExtension` | `listeners/FailureScreenshotListener`, `RetryAnalyzer`, `RetryTransformer`, `ConsoleLogListener` | конфиг: `reporter`, `use.screenshot/trace/video` |
| Клиент API для тестовых данных | `api/BackendApi.java` (через браузер) | `api/BackendApi.java` (тот же) | `support/api-client.ts` (свой HTTP-клиент) |
| Тестовые данные | `data/TestUser.java`, `data/Product.java` | те же | `support/data.ts` |
| Page objects | `pages/*.java` | `pages/*.java` (те же, другой пакет) | `pages/*.ts` |
| Тесты | `tests/*Test.java` | `tests/*Test.java` | `tests/*.spec.ts` |
| Метки тестов | теги: `tags/Smoke.java`, `Regression.java`, … (`@Tag`) | группы: `tests/Groups.java` + `@Test(groups = ...)` | `{ tag: '@smoke' }` прямо в тесте |
| Наборы на запуск | `suites/SmokeSuite.java`, `CriticalSuite.java`, `FullRegressionSuite.java` | `testng/all.xml`, `smoke.xml`, `critical.xml`, `regression.xml`, `parallel-methods.xml`, `demo-failure.xml` | npm-скрипты (`test:smoke`) / `--grep` / `projects` |

---

## 2. Жизненный цикл: before/after на всех уровнях

### 2.1. Сводная таблица уровней

| Уровень | Selenide + JUnit 5 | Selenide + TestNG | Playwright TS |
|---|---|---|---|
| **Один раз за весь прогон, до всего** | ✅ `LauncherSessionListener.launcherSessionOpened` — `core/GlobalLifecycle.java` | ✅ `@BeforeSuite(alwaysRun = true)` — `BaseTest.beforeSuite()` (один раз, хотя её наследуют все классы) | ✅ `globalSetup` — `global-setup.ts`; ✅ запуск приложения — `webServer` |
| **Перед конкретным набором** | ✅ `@BeforeSuite` в `suites/SmokeSuite.java` (только при запуске этого сьюта) | `@BeforeSuite` срабатывает для любого XML; на часть прогона — `@BeforeTest` 💡 (на каждый блок `<test>` XML-файла) | — (набор = выборка через `--grep`; `globalSetup` выполняется всегда) |
| **Перед группой** | — | `@BeforeGroups(groups = "smoke")` 💡 | — |
| **Перед классом / файлом** | ✅ extension `BeforeAllCallback` — `SelenideSetup.beforeAll`; 💡 `@BeforeAll static void ...` | `@BeforeClass` 💡 (метод **не** static: экземпляр класса один на все методы) | 💡 `test.beforeAll(...)`; 💡 фикстура со `scope: 'worker'` |
| **Перед каждым тестом, уровень базы** | ✅ поля `BaseTest` (новый экземпляр класса на каждый тест): `user = TestUser.unique()`, `api = new BackendApi()` | ✅ `@BeforeMethod(alwaysRun = true) createTestUser()` — кладёт `user` и `api` в `ThreadLocal` | ✅ фикстуры `user`, `api`, `storageState` — для каждого теста, который их попросил |
| **Перед каждым тестом, уровень класса / файла** | ✅ `@BeforeEach void openPlayground()` — `PlaygroundTest` | ✅ `@BeforeMethod(alwaysRun = true) openPlayground()` — `PlaygroundTest` | ✅ `test.beforeEach(...)` — `playground.spec.ts`, `catalog.spec.ts`, `login.spec.ts` |
| **Сразу после теста, до очистки** (скриншот упавшего) | ✅ `ScreenShooterExtension` (`afterTestExecution`) | ✅ `FailureScreenshotListener` (`IInvokedMethodListener.afterInvocation`) | ✅ `screenshot`, `trace`, `video` в `use` конфига — без кода |
| **После каждого теста, уровень класса / файла** | 💡 `@AfterEach` в тестовом классе | 💡 `@AfterMethod(alwaysRun = true)` в тестовом классе | 💡 `test.afterEach(...)` |
| **После каждого теста, уровень базы** | ✅ `@AfterEach cleanUpBackendData()`; ✅ `SelenideSetup.afterEach` → `closeWebDriver()` | ✅ `@AfterMethod(alwaysRun = true) cleanUp()`: cleanup данных + `closeWebDriver()` | ✅ код после `await use(api)` в фикстуре `api`; ✅ `context` и `page` закрываются сами |
| **После класса / файла** | 💡 `@AfterAll static void ...` | `@AfterClass` 💡 | 💡 `test.afterAll(...)`; 💡 teardown worker-фикстуры |
| **После набора** | ✅ `@AfterSuite` в `suites/SmokeSuite.java` | `@AfterTest` 💡 (блок `<test>`), `@AfterGroups` 💡 | — |
| **Один раз за прогон, после всего** | 💡 `launcherSessionClosed`; браузеры Selenide закрывает сам (shutdown hook) | `@AfterSuite(alwaysRun = true)` 💡; браузеры — так же shutdown hook Selenide | 💡 `globalTeardown`; приложение из `webServer` Playwright останавливает сам |

### 2.2. Порядок выполнения одного теста

Selenide + JUnit 5 (`CartTest` → `BaseTest`):

```
LauncherSessionListener.launcherSessionOpened   GlobalLifecycle: SelenideSetup.configure() + старт приложения
  @BeforeSuite                                   только если запущен SmokeSuite: POST /api/test/reset
    BeforeAllCallback.beforeAll                  SelenideSetup.beforeAll  (один раз на класс)
    @BeforeAll                                   (в репо нет)
      new CartTest()                             поля BaseTest: user = TestUser.unique(), api = new BackendApi()
      BeforeEachCallback / @BeforeEach           (BaseTest: нет; PlaygroundTest: openPlayground())
      @Test                                      браузер стартует лениво на первом open()
      AfterTestExecutionCallback                 ScreenShooterExtension: скриншот, если упал
      @AfterEach                                 BaseTest.cleanUpBackendData()  — браузер ещё жив
      AfterEachCallback                          SelenideSetup.afterEach: closeWebDriver()
    @AfterAll                                    (в репо нет)
  @AfterSuite
```

Selenide + TestNG (`CartTest` → `BaseTest`; порядок проверен по выводу `FailureDemoTest`):

```
@BeforeSuite                     BaseTest.beforeSuite(): <parameter> из XML, SelenideConfig.configure(), старт приложения, reset
  @BeforeTest                    (в репо нет) - на каждый блок <test> XML-файла
    @BeforeClass                 (в репо нет) - экземпляр CartTest уже создан, ОДИН на все методы класса
      @BeforeMethod (база)       BaseTest.createTestUser(): user и api в ThreadLocal
      @BeforeMethod (класс)      (PlaygroundTest: openPlayground())
      @Test                      браузер стартует лениво на первом open()
      afterInvocation            FailureScreenshotListener: скриншот, если упал
      IRetryAnalyzer.retry       RetryAnalyzer: повторить? (при -Dtest.retries > 0)
      onTestSuccess / Failure    ConsoleLogListener
      @AfterMethod (класс, база) BaseTest.cleanUp(): cleanup данных, closeWebDriver()
    @AfterClass
  @AfterTest
@AfterSuite
```

Playwright TS (`cart.spec.ts`):

```
webServer                     node scripts/start-demo-app.mjs, ждёт /api/health
globalSetup                   global-setup.ts: POST /api/test/reset
  worker (процесс)            запускает браузер (один на worker)
    test.beforeAll            (в репо нет; выполняется в КАЖДОМ worker, который берёт тесты этого файла)
      фикстуры теста          user → api (APIRequestContext) → storageState (логин через API) → context → page → catalogPage
      test.beforeEach         (в playground.spec.ts: playgroundPage.goto())
      test(...)
      test.afterEach          (в репо нет)
      teardown фикстур        в обратном порядке: page, context закрываются; api: cleanup(user), dispose()
      артефакты               screenshot / trace / video упавшего теста — из конфига
    test.afterAll             (в репо нет)
globalTeardown                (в репо нет); webServer останавливается
```

### 2.3. Примеры по уровням

**«Before suite» — один раз за прогон**

```java
// Selenide + JUnit 5 ✅ core/GlobalLifecycle.java
// регистрация: файл META-INF/services/org.junit.platform.launcher.LauncherSessionListener
public class GlobalLifecycle implements LauncherSessionListener {
    @Override
    public void launcherSessionOpened(LauncherSession session) {
        SelenideSetup.configure(); // + старт приложения; сброс данных - в @BeforeSuite сьюта (suites/SuiteHooks.java)
    }
}
```

```java
// Selenide + TestNG ✅ tests/BaseTest.java — регистрация не нужна, метод наследуют все тесты
@Parameters({"browser", "headless"})
@BeforeSuite(alwaysRun = true)
public void beforeSuite(@Optional("") String browser, @Optional("") String headless) {
    Config.setSuiteParameter("browser", browser);   // значения из <parameter> XML-файла
    Config.setSuiteParameter("headless", headless);
    SelenideConfig.configure();                      // + старт приложения
    resetBackend();                                  // POST /api/test/reset
}
```

```ts
// Playwright TS ✅ global-setup.ts — регистрация: globalSetup: require.resolve('./global-setup')
export default async function globalSetup(config: FullConfig): Promise<void> {
  const api = await request.newContext({ baseURL: config.projects[0].use.baseURL });
  const response = await api.post('/api/test/reset', { headers: { 'X-Test-Key': TEST_KEY } });
  if (!response.ok()) throw new Error(`Backend reset failed: ${response.status()}`);
  await api.dispose();
}
```

Запуск приложения: в обоих Selenide-модулях — `Config.baseUrl()` → `DemoApp.startShared()` (в той же JVM);
в TS — `webServer: { command: 'node scripts/start-demo-app.mjs', url: '.../api/health' }`.

**Перед набором тестов**

```java
// Selenide + JUnit 5 ✅ suites/SmokeSuite.java — только при запуске этого сьюта
@Suite
@SelectPackages("com.compare.selenide.tests")
@IncludeTags("smoke")
public class SmokeSuite {
    @BeforeSuite static void beforeSuite() { SuiteHooks.resetBackend("SmokeSuite"); }
    @AfterSuite  static void afterSuite()  { System.out.println("[SmokeSuite] @AfterSuite: done"); }
}
```

```java
// Selenide + TestNG 💡 — на каждый блок <test> XML-файла (в regression.xml их два)
@BeforeTest(alwaysRun = true)
public void beforeTestBlock(ITestContext context) {
    System.out.println("[test block] " + context.getName()); // "Shop regression", "Browser features"
}
```

Playwright TS: отдельного хука нет — набор задаётся `--grep`, а `globalSetup` общий для любого запуска.

**Перед классом / файлом** 💡 (в репозитории нет — примеры)

```java
// Selenide + JUnit 5: один раз на класс, метод static
@BeforeAll
static void seedCatalog() { /* общие данные класса */ }
```

```java
// Selenide + TestNG: один раз на класс, метод НЕ static (экземпляр класса один на все его методы)
@BeforeClass(alwaysRun = true)
public void seedCatalog() { /* общие данные класса */ }
```

```ts
// Playwright TS: один раз на файл... но ВНИМАНИЕ: на каждый worker, который выполняет тесты этого файла
test.beforeAll(async ({ browser }) => { /* общие данные */ });

// если ресурс нужен всем тестам worker'а — фикстура уровня worker:
export const test = base.extend<{}, { sharedUser: TestUser }>({
  sharedUser: [async ({}, use) => { await use(uniqueUser()); }, { scope: 'worker' }],
});
```

Ловушка TS: при `fullyParallel: true` тесты одного файла разъезжаются по разным workers, и `beforeAll`
выполняется несколько раз. В JUnit и в TestNG с `parallel="classes"` класс целиком идёт в одном потоке, и хук
класса выполняется ровно один раз (при `parallel="methods"` в TestNG — тоже один раз, экземпляр-то один).

**Перед каждым тестом — уровень базы**

```java
// Selenide + JUnit 5 ✅ tests/BaseTest.java — JUnit создаёт новый экземпляр класса на КАЖДЫЙ тест,
// поэтому инициализация полей и есть "before each"
@ExtendWith({SelenideSetup.class, ScreenShooterExtension.class})
public abstract class BaseTest {
    protected final BackendApi api = new BackendApi();
    protected TestUser user = TestUser.unique();
}
```

```java
// Selenide + TestNG ✅ tests/BaseTest.java — экземпляр ОДИН на все методы класса, поэтому явный хук,
// а хранение в ThreadLocal (иначе parallel="methods" перепутает пользователей разных потоков)
private static final ThreadLocal<TestUser> USER = new ThreadLocal<>();
private static final ThreadLocal<BackendApi> API = new ThreadLocal<>();

@BeforeMethod(alwaysRun = true)   // без alwaysRun фильтр групп в XML пропустит этот метод
public void createTestUser() {
    USER.set(TestUser.unique());
    API.set(new BackendApi());
}
```

```ts
// Playwright TS ✅ support/fixtures.ts — создаётся только то, что тест попросил в параметрах
export const test = base.extend<Options & Fixtures>({
  user: async ({ userKind }, use) => { await use(uniqueUser(userKind)); },
  api: async ({ playwright, baseURL, user }, use) => { /* ... */ await use(api); /* ... */ },
  storageState: async ({ authenticated, dismissPromo, api, user, baseURL }, use) => { /* логин через API */ },
});
```

**Перед каждым тестом — уровень теста / файла**

```java
// Selenide + JUnit 5 ✅ tests/PlaygroundTest.java
@BeforeEach
void openPlayground() {
    api.loginAndStartSession(user);
    playground = new PlaygroundPage().open();
}
```

```java
// Selenide + TestNG ✅ tests/PlaygroundTest.java — выполняется ПОСЛЕ BaseTest.createTestUser()
@BeforeMethod(alwaysRun = true)
public void openPlayground() {
    api().loginAndStartSession(user());
    new PlaygroundPage().open();
}
```

```ts
// Playwright TS ✅ tests/playground.spec.ts — логин уже сделан фикстурой storageState
test.beforeEach(async ({ playgroundPage }) => {
  await playgroundPage.goto();
});
```

**После каждого теста — очистка данных и браузер**

```java
// Selenide + JUnit 5 ✅ tests/BaseTest.java + core/SelenideSetup.java
@AfterEach
void cleanUpBackendData() {
    try { api.cleanup(user); }
    catch (RuntimeException e) { System.err.println("[cleanup] failed ..."); }
}
// ...а браузер закрывает extension ПОСЛЕ этого метода:
public void afterEach(ExtensionContext context) {
    if (!Config.reuseBrowser()) closeWebDriver();
    else if (WebDriverRunner.hasWebDriverStarted()) { clearBrowserCookies(); clearBrowserLocalStorage(); }
}
```

```java
// Selenide + TestNG ✅ tests/BaseTest.java — всё в одном методе; alwaysRun = выполнится и после упавшего теста
@AfterMethod(alwaysRun = true)
public void cleanUp() {
    try {
        api().cleanup(user());
    } catch (RuntimeException e) {
        System.err.println("[cleanup] failed ...");
    } finally {
        if (!Config.reuseBrowser()) closeWebDriver();
        else if (WebDriverRunner.hasWebDriverStarted()) { clearBrowserCookies(); clearBrowserLocalStorage(); }
        USER.remove();
        API.remove();
    }
}
```

```ts
// Playwright TS ✅ support/fixtures.ts — teardown = код после await use(); браузерный context закрывается сам
api: async ({ playwright, baseURL, user }, use) => {
  const request = await playwright.request.newContext({ baseURL });
  const api = new ApiClient(request);
  await use(api);            // ← здесь выполняется тест
  await api.cleanup(user);   // ← teardown, даже если тест упал
  await request.dispose();
},
```

---

## 3. BaseTest ↔ фикстуры

| | Selenide + JUnit 5 `BaseTest` | Selenide + TestNG `BaseTest` | Playwright TS фикстуры |
|---|---|---|---|
| Как подключается | `class CartTest extends BaseTest` | `public class CartTest extends BaseTest` (тесты и хуки — `public`) | `import { test, expect } from '../support/fixtures'` |
| Экземпляр тестового класса | новый на каждый тест | **один на все методы класса** | классов нет |
| Что тест получает | поля: `api`, `user`; статический Selenide API (`$`, `open`) | методы: `api()`, `user()`; статический Selenide API | параметры теста: `async ({ catalogPage, api, user, page }) => ...` |
| Где хранится состояние теста | обычные поля | `ThreadLocal` (из-за общего экземпляра и `parallel="methods"`) | у каждого теста свои фикстуры |
| Создаётся всегда? | да, всё, что в базе | да, всё, что в `@BeforeMethod` | нет, только запрошенные фикстуры и их зависимости |
| Зависимости между ресурсами | вручную, порядком строк | вручную, порядком строк | граф: `storageState` ← `api` ← `user` |
| Setup + teardown | две аннотации (`@BeforeEach` / `@AfterEach`) | две аннотации (`@BeforeMethod` / `@AfterMethod`), обе `alwaysRun = true` | одна функция: до `await use()` — setup, после — teardown |
| Опции для группы тестов | `user = TestUser.slow();` в начале теста ✅ `LoginTest` | `useUser(TestUser.slow());` в начале теста ✅ `LoginTest` | `test.use({ userKind: 'slow' })` в `describe` ✅ `login.spec.ts` |
| Тест без логина | не вызывать `loginViaApi()` | не вызывать `loginViaApi()` | `test.use({ authenticated: false })` ✅ `login.spec.ts` |
| Хелпер «залогиниться и открыть каталог» | ✅ `protected CatalogPage loginViaApi()` | ✅ `protected CatalogPage loginViaApi()` | не нужен: контекст уже залогинен |
| Page objects | создаются в тесте: `new CartPage().open()` | так же | приходят фикстурой: `async ({ cartPage }) => ...` |
| Listener'ы базы | `@ExtendWith({SelenideSetup.class, ScreenShooterExtension.class})` | `@Listeners(FailureScreenshotListener.class)` | — |

Тот же тест в трёх видах:

```java
// Selenide + JUnit 5 ✅ selenide/.../tests/CartTest.java
@Test @Regression
void emptyCartCannotBeCheckedOut() {
    api.loginAndStartSession(user);
    CartPage cart = new CartPage().open();
    cart.emptyMessage().shouldBe(visible);
    cart.checkoutButton().shouldBe(disabled);
}
```

```java
// Selenide + TestNG ✅ selenide-testng/.../tests/CartTest.java
@Test(groups = Groups.REGRESSION)
public void emptyCartCannotBeCheckedOut() {
    api().loginAndStartSession(user());
    CartPage cart = new CartPage().open();
    cart.emptyMessage().shouldBe(visible);
    cart.checkoutButton().shouldBe(disabled);
}
```

```ts
// Playwright TS ✅ playwright-ts/tests/cart.spec.ts
test('empty cart cannot be checked out', { tag: '@regression' }, async ({ cartPage }) => {
  await cartPage.goto();
  await expect(cartPage.emptyMessage).toBeVisible();
  await expect(cartPage.checkoutButton).toBeDisabled();
});
```

---

## 4. Запуски из командной строки

Selenide-модули — из корня `stage2`, Playwright TS — из `playwright-ts/`.

| Что | Selenide + JUnit 5 | Selenide + TestNG | Playwright TS |
|---|---|---|---|
| Все тесты | `./mvnw -pl selenide -am test` | `./mvnw -pl selenide-testng -am test` (= `all.xml`) | `npx playwright test` |
| Один класс / файл | `./mvnw -pl selenide -am test -Dtest=CartTest -Dsurefire.failIfNoSpecifiedTests=false` | так же, `-Dtest=CartTest ...` (XML тогда игнорируется) | `npx playwright test tests/cart.spec.ts` |
| Один тест | `... -Dtest=CartTest#emptyCartCannotBeCheckedOut ...` | так же | `npx playwright test -g "empty cart"` или `tests/cart.spec.ts:37` |
| По тегу / группе | `-Dgroups=smoke` | `-Dsuite=smoke` (группа задана в `smoke.xml`) | `--grep @smoke` (`npm run test:smoke`) |
| Несколько тегов | `-Dgroups="smoke \| critical"` | XML с `<include name="smoke"/><include name="critical"/>` 💡 | `--grep "@smoke\|@critical"` |
| Исключить тег | `-DexcludedGroups=regression` | `<exclude name="regression"/>` в XML | `--grep-invert @regression` |
| Набор (сьют) | `-Dtest=SmokeSuite -Dsurefire.failIfNoSpecifiedTests=false` | `-Dsuite=smoke` / `critical` / `regression` / `parallel-methods` | — (тег или папка) |
| Видеть браузер | `-Dheadless=false` | `-Dheadless=false` (сильнее `<parameter>` в XML) | `--headed` |
| Без параллельности | `-Dtest.parallel=false` | XML без атрибута `parallel` (например `demo-failure.xml`) | `--workers=1` |
| Число потоков | `-Dtest.threads=5` | `thread-count` в XML-файле | `--workers=5` |
| Ретраи | `-Dtest.retries=2` (surefire) | `-Dtest.retries=2` (`RetryAnalyzer`; нужен `RetryTransformer` в XML) | `--retries=2` |
| Против запущенного приложения | `-DbaseUrl=http://127.0.0.1:8080` | так же | `BASE_URL=http://127.0.0.1:8080 npx playwright test` |
| Другой браузер | `-Dbrowser=firefox` | `-Dbrowser=firefox` или `<parameter name="browser">` в XML | `--project=firefox` (после `ALL_BROWSERS=1`) |
| Только упавшие в прошлый раз | — | `-Dsuite=...` нельзя; `testng-failed.xml` из `target/surefire-reports/` 💡 | `npx playwright test --last-failed` 💡 |
| Отладка | брейкпоинт в IDE, Debug | так же (плагин TestNG в IDE умеет запускать XML-файл) | `npm run test:debug`, `npm run test:ui` |
| Отчёт | `selenide/target/surefire-reports/` | `selenide-testng/target/surefire-reports/index.html` (HTML TestNG) | `npm run report` |

---

## 5. Конфигурация построчно

### 5.1. `framework.properties` ✅ (одинаковый файл в обоих Selenide-модулях)

| Строка | Что значит | Selenide + TestNG | Аналог в Playwright TS |
|---|---|---|---|
| `baseUrl=` | адрес приложения; пусто — тесты поднимают Java-приложение сами | так же | `use.baseURL` + `webServer`; переопределение — `BASE_URL` |
| `browser=chrome` | какой браузер запускать | так же; XML может перекрыть: `<parameter name="browser" value="chrome"/>` | `projects: [{ name: 'chrome', use: { ...devices['Desktop Chrome'], channel: 'chrome' } }]` |
| `headless=true` | без окна браузера | так же; XML: `<parameter name="headless" value="true"/>` | `use.headless` (по умолчанию `true`), в CLI `--headed` |
| `timeout.seconds=10` | таймаут всех `should*()` и действий | так же | `expect.timeout: 10_000` + `use.actionTimeout: 10_000` |
| `browser.reuse=false` | новый браузер на каждый тест (`true` — один на поток с очисткой) | так же (обрабатывается в `@AfterMethod`) | нет: браузер всегда один на worker, на тест — новый `context` |
| `artifacts.dir=target/artifacts` | куда складывать скриншоты и загрузки | так же | `outputDir` (по умолчанию `test-results/`) |

Порядок приоритета: JUnit — `-D...` → файл; TestNG — `-D...` → `<parameter>` из XML → файл (`core/Config.java`);
Playwright — переменные окружения → `playwright.config.ts`.

### 5.2. `Configuration.*` Selenide ✅ — `SelenideSetup.java` (JUnit) и `SelenideConfig.java` (TestNG)

Строки в обоих файлах **одинаковые** — это настройки Selenide, а не раннера. Отличается только, кто и когда их вызывает.

| Строка | Что значит | Аналог в `playwright.config.ts` |
|---|---|---|
| `Configuration.baseUrl = Config.baseUrl();` | `open("/catalog.html")` — относительно этого адреса | `use.baseURL` |
| `Configuration.browser = Config.browser();` | имя браузера | `projects[].use` (`devices[...]`, `channel`) |
| `Configuration.headless = Config.headless();` | без окна | `use.headless` |
| `Configuration.browserSize = "1366x900";` | размер окна (внешний, с рамкой) | `use.viewport: { width: 1366, height: 900 }` (размер страницы) |
| `Configuration.timeout = ...toMillis();` | ожидание по умолчанию для `should*()`, `click()`, `setValue()` | `expect.timeout` + `use.actionTimeout` |
| `Configuration.pageLoadTimeout = 30_000;` | сколько ждать загрузку страницы в `open()` | `use.navigationTimeout: 30_000` |
| `Configuration.reportsFolder = ".../selenide-reports";` | куда класть скриншоты и HTML упавших проверок | `outputDir`; HTML-отчёт — `['html', { outputFolder }]` |
| `Configuration.screenshots = true;` | скриншот при падении `should*()` | `use.screenshot: 'only-on-failure'` |
| `Configuration.savePageSource = true;` | сохранить HTML страницы при падении | `use.trace: 'retain-on-failure'` (DOM на каждом шаге) |
| `Configuration.downloadsFolder = ".../downloads";` | куда класть скачанные файлы | `use.acceptDownloads` + `download.saveAs(...)` в тесте |
| `Configuration.fileDownload = FileDownloadMode.FOLDER;` | реально кликнуть и следить за папкой | `page.waitForEvent('download')` |
| `capabilities.setCapability(UNHANDLED_PROMPT_BEHAVIOUR, "ignore");` | не закрывать неожиданные alert'ы автоматически | настройки нет: без обработчика диалог закрывается сам |
| `Configuration.browserCapabilities = capabilities;` | доп. возможности браузера | `use.launchOptions`, `use.contextOptions`, `projects[].use` |
| `synchronized` + `if (configured) return;` | настроить глобальную статику один раз, до старта потоков | не нужно: конфиг читается один раз раннером |
| **кто вызывает** | JUnit: `GlobalLifecycle` (LauncherSessionListener) + страховка в `beforeAll`; TestNG: `@BeforeSuite` в `BaseTest` | раннер Playwright, при старте |

### 5.3. Параллельность и таймауты: `junit-platform.properties` ✅ ↔ атрибуты `<suite>` в XML ✅ ↔ `playwright.config.ts`

| Строка `junit-platform.properties` | Что значит | Selenide + TestNG (`testng/*.xml`) | Playwright TS |
|---|---|---|---|
| `junit.jupiter.execution.parallel.enabled=false` | параллельность выключена (Maven включает флагом) | нет атрибута `parallel` — последовательно (`demo-failure.xml`) | `workers: 1` выключает |
| `junit.jupiter.execution.parallel.mode.default=same_thread` | методы одного класса — последовательно | `parallel="classes"` — методы класса в одном потоке | `fullyParallel: false` |
| `junit.jupiter.execution.parallel.mode.classes.default=concurrent` | разные классы — параллельно | `parallel="classes"` ✅ (`all.xml`) | файлы и так распределяются по workers |
| `junit.jupiter.execution.parallel.config.strategy=fixed` | фиксированное число потоков | `thread-count` — всегда фиксированное | `workers` — всегда фиксированное |
| `junit.jupiter.execution.parallel.config.fixed.parallelism=3` | 3 потока | `thread-count="3"` ✅ | `workers: isCI ? 2 : 3` |
| `junit.jupiter.execution.parallel.config.fixed.max-pool-size=3` | не дать ForkJoin создать лишние потоки | не нужно: пул TestNG фиксированный | не нужно: worker = процесс |
| `junit.jupiter.execution.timeout.default=2 m` | таймаут одного теста | `time-out="120000"` на `<suite>` 💡 или `@Test(timeOut = ...)` | `timeout: 60_000` |
| — (только через `mode.default=concurrent`) | всё параллельно, даже методы класса | `parallel="methods"` ✅ (`parallel-methods.xml`) | `fullyParallel: true` ✅ |

### 5.4. Surefire ✅: родительский `pom.xml` (JUnit 5) ↔ `selenide-testng/pom.xml` (TestNG)

| Строка (JUnit 5) | Что значит | Selenide + TestNG | Playwright TS |
|---|---|---|---|
| версия плагина `3.6.1` | — | **`3.5.6`** + зависимость `surefire-testng 3.5.6`: в 3.6 нет провайдера TestNG, читающего XML | — |
| `<excludedGroups>${excludedGroups}</excludedGroups>` (= `demo-failure`) | не запускать тег по умолчанию | `<excludedGroups></excludedGroups>` — обнулено: иначе surefire исключит группу из **любого** XML | `grepInvert: /@demo-failure/` |
| `<rerunFailingTestsCount>${test.retries}</rerunFailingTestsCount>` | перезапуск упавших | нет (работает только для JUnit) → `RetryAnalyzer` + `RetryTransformer` | `retries: isCI ? 2 : 0` |
| `<configurationParameters>` с `parallel.*` | параллельность из Maven | нет: параллельность — в XML | `--workers` в CLI |
| — | — | `<suiteXmlFiles><suiteXmlFile>src/test/resources/testng/${suite}.xml</suiteXmlFile></suiteXmlFiles>` + свойство `suite=all` | — |
| `<configuration>` наследуется от родителя | — | `combine.self="override"` — JUnit-настройки родителя полностью заменены | — |
| `<trimStackTrace>false</trimStackTrace>` | полный стек ошибки | так же | — |
| `<argLine>-Djdk.net.unixdomain.tmpdir=...</argLine>` | обход бага JDK 25 на Windows | так же | — |
| `-Dgroups=smoke` (параметр запуска) | только тег | не используется: группы — в XML (`-Dsuite=smoke`) | `--grep @smoke` |

### 5.5. `playwright-ts/playwright.config.ts` ✅ — каждая строка и её аналоги

| Строка | Что значит | Selenide + JUnit 5 | Selenide + TestNG |
|---|---|---|---|
| `const port = Number(process.env.APP_PORT ?? 4321);` | порт, на котором `webServer` поднимет приложение | случайный порт в `DemoApp.startShared()` | так же |
| `const baseURL = process.env.BASE_URL ?? \`http://127.0.0.1:${port}\`;` | адрес приложения | `baseUrl` / `-DbaseUrl` | так же |
| `const isCI = !!process.env.CI;` | признак CI | — (`System.getenv("CI")`) | — |
| `testDir: './tests'` | где искать тесты | `src/test/java` (соглашение Maven) | `<packages>` / `<classes>` в XML |
| `globalSetup: require.resolve('./global-setup')` | один раз до всех тестов | `LauncherSessionListener` | `@BeforeSuite` |
| `fullyParallel: true` | параллельно даже внутри файла | `mode.default=concurrent` | `parallel="methods"` |
| `workers: isCI ? 2 : 3` | число параллельных процессов | `fixed.parallelism` / `-Dtest.threads` | `thread-count` |
| `forbidOnly: isCI` | забытый `test.only` роняет CI | — (нет `.only`) | — |
| `retries: isCI ? 2 : 0` | ретраи; прошедший со 2-й попытки = flaky | `rerunFailingTestsCount` | `IRetryAnalyzer` |
| `timeout: 60_000` | таймаут теста | `timeout.default` | `time-out` / `timeOut` |
| `expect: { timeout: 10_000 }` | сколько повторяются `expect(...)` | `Configuration.timeout` | `Configuration.timeout` |
| `toHaveScreenshot: { maxDiffPixelRatio: 0.01 }` | допуск визуального сравнения | — | — |
| `updateSnapshots: 'missing'` | первый прогон записывает эталоны | — | — |
| `grepInvert: process.env.FAILURE_DEMO ? undefined : /@demo-failure/` | исключить тег, если не просили | `excludedGroups` | `<exclude name="demo-failure"/>` |
| `reporter: [['list'], ['html', ...], ['junit', ...]]` | консоль, HTML-отчёт, JUnit XML | surefire: консоль + XML | `ConsoleLogListener` + HTML TestNG + XML |
| `use.baseURL` | база для `page.goto('/...')` | `Configuration.baseUrl` | `Configuration.baseUrl` |
| `use.testIdAttribute: 'data-test'` | атрибут для `getByTestId` | `-Dselenide.test-id.attribute=data-test` 💡 | так же 💡 |
| `use.actionTimeout: 10_000` | таймаут `click`, `fill` | `Configuration.timeout` | `Configuration.timeout` |
| `use.navigationTimeout: 30_000` | таймаут `page.goto` | `Configuration.pageLoadTimeout` | `Configuration.pageLoadTimeout` |
| `use.trace: 'retain-on-failure'` | trace упавших тестов | `savePageSource` (только итоговый HTML) | так же |
| `use.screenshot: 'only-on-failure'` | скриншот упавших | `Configuration.screenshots` + `ScreenShooterExtension` | `Configuration.screenshots` + `FailureScreenshotListener` |
| `use.video: 'retain-on-failure'` | видео упавших | — (плагин `selenide-video-recorder`) | так же |
| `projects: [{ name: 'chrome', use: { ... } }]` | браузер(ы) прогона | `Configuration.browser` + `browserSize` | так же + `<parameter name="browser">` |
| `...(process.env.ALL_BROWSERS ? [firefox, webkit] : [])` | несколько браузеров за один прогон | несколько запусков `-Dbrowser=...` | несколько запусков (Selenide `Configuration` общий на JVM) |
| `webServer: { command, url, reuseExistingServer, timeout, ... }` | поднять приложение, дождаться health | `DemoApp.startShared()` | так же |

### 5.6. `selenide-testng/src/test/resources/testng/all.xml` ✅ построчно

| Строка | Что значит | Selenide + JUnit 5 | Playwright TS |
|---|---|---|---|
| `<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">` | схема файла (подсказки в IDE) | — | — |
| `<suite name="Selenide + TestNG: all"` | имя прогона в отчёте | `@SuiteDisplayName("...")` | — |
| `parallel="classes"` | классы параллельно, методы класса — в одном потоке | `classes.default=concurrent` + `mode.default=same_thread` | `fullyParallel: false` |
| `thread-count="3"` | 3 потока = 3 браузера | `fixed.parallelism=3` | `workers: 3` |
| `<parameter name="browser" value="chrome"/>` | значение для `@Parameters({"browser", ...})` | `-Dbrowser=...` / `framework.properties` | `use` / `process.env` |
| `<parameter name="headless" value="true"/>` | то же для `headless` | `-Dheadless=...` | `use.headless` |
| `<listeners>` | подключить listener'ы ко всему прогону | `META-INF/services` / `@ExtendWith` | `reporter` в конфиге |
| `<listener class-name="...RetryTransformer"/>` | ретраи для всех тестов (только так, не через `@Listeners`) | `rerunFailingTestsCount` | `retries` |
| `<listener class-name="...ConsoleLogListener"/>` | строка в консоли на каждый тест | `TestWatcher` 💡 | `reporter: 'list'` |
| `<test name="All tests">` | именованная часть прогона (их может быть несколько) | ещё один `@Suite`-класс | `projects` |
| `<groups><run><exclude name="demo-failure"/></run></groups>` | не запускать группу | `excludedGroups` / `@ExcludeTags` | `grepInvert` |
| `<packages><package name="com.compare.testng.tests"/></packages>` | где искать тесты | `@SelectPackages(...)` | `testDir` |

---

## 6. Page Object и локаторы

> Разделы 6–10 — про библиотеку Selenide, а не про раннер, поэтому в модуле `selenide-testng` всё то же самое: page objects, локаторы, действия, ожидания и `BackendApi` скопированы без изменений (другой только пакет — `com.compare.testng`). Колонка «Selenide» здесь значит оба Java-модуля; где отличается раннер, это сказано в строке таблицы.

### 6.1. Как устроен page object

| | Selenide ✅ `pages/CatalogPage.java` | Playwright TS ✅ `pages/catalog-page.ts` |
|---|---|---|
| Поле-элемент | `private final SelenideElement search = $("#search");` | `readonly search: Locator;` + в конструкторе `this.search = page.getByPlaceholder('Search products');` |
| Поле-коллекция | `private final ElementsCollection names = $$(".product-card .name");` | `this.names = this.cards.locator('.name');` (тот же тип `Locator`) |
| Откуда берётся страница | глобально (браузер текущего потока) | передаётся в конструктор: `constructor(page: Page)` |
| Открыть страницу | `Selenide.open("/catalog.html");` | `await this.page.goto('/catalog.html');` |
| Дождаться готовности | ✅ `BasePage.waitUntilReady()`: `Wait().until(... executeJavaScript(...))` + `SPINNER.should(disappear)` | ✅ `BasePage.waitUntilReady()`: `page.waitForFunction(() => ...)` + `expect(this.spinner).toBeHidden()` |
| Возвращает | `this` / следующую страницу (fluent: `new CartPage().open().remove(...)`) | `Promise<void>`; страницы приходят фикстурами |
| Элемент «устаревает»? | нет — ленивый, ищется заново при каждом обращении | нет — ленивый так же |
| Проверки внутри page object | обычно возвращает элемент, проверяет тест: `cart.total().shouldHave(...)` | обычно публичный `readonly` локатор, тест: `await expect(cartPage.total).toHaveText(...)` |

### 6.2. Локаторы

| Что найти | Selenide | Playwright TS |
|---|---|---|
| По id / CSS | `$("#search")` | `page.locator('#search')` |
| Все по CSS | `$$(".product-card")` | `page.locator('.product-card')` |
| Внутри элемента | `row.$(".remove")` | `row.locator('.remove')` |
| По XPath | `$x("//button")` | `page.locator('xpath=//button')` |
| По тексту | `$(byText("No products found."))` 💡 | ✅ `page.getByText('No products found.')` |
| По label | `$(byLabel("Username"))` 💡 (в репо — `$("#username")`) | ✅ `page.getByLabel('Username')` |
| По роли и имени | `$(byRole("button", "Log in"))` 💡 (в репо — `$("#login")`) | ✅ `page.getByRole('button', { name: 'Log in' })` |
| По placeholder | `$(byPlaceholder("Search products"))` 💡 | ✅ `page.getByPlaceholder('Search products')` |
| По test id | ✅ `$("[data-test='login-error']")`; или `$(byTestId("login-error"))` 💡 с `-Dselenide.test-id.attribute=data-test` (по умолчанию `data-test-id`) | ✅ `page.getByTestId('login-error')` (атрибут — `testIdAttribute` в конфиге) |
| Элемент коллекции по тексту | ✅ `cards.findBy(text(product.name())).$("button")` | ✅ `cards.filter({ hasText: product.name }).getByRole('button')` |
| Первый / последний / N-й | `.first()`, `.last()`, `.get(2)` | `.first()`, `.last()`, `.nth(2)` |
| Фильтр коллекции по условию | `$$(".toast").filter(visible)` 💡 | `page.locator('.toast').filter({ visible: true })` 💡 |
| Shadow DOM | ✅ `$(shadowCss("#code", "#promo"))` — указать хост; `$(shadowDeepCss("#code"))` 💡 — через любые shadow root | ✅ просто `page.getByPlaceholder('Promo code')` — проходит сквозь shadow root |
| iframe | ✅ `switchTo().frame(supportFrame); ... switchTo().defaultContent();` | ✅ `page.frameLocator('#support-frame').getByLabel(...)` — без переключения |

Строгость: в Playwright действие над локатором, который нашёл **больше одного** элемента, падает
(strict mode). В Selenide `$()` молча берёт первый.

Пользовательские локаторы (`byRole`, `byLabel`, `byPlaceholder`, `byTestId`) есть в обоих. Разница в привычке:
в Playwright это рекомендуемый способ по умолчанию (так и написан `playwright-ts`), в Selenide-проектах чаще
пишут CSS/XPath (так написан модуль `selenide`).

---

## 7. Действия: клики, ввод, выбор, файлы

Обе библиотеки перед действием ждут элемент. Разница — в том, **чего именно** ждут:
- Selenide: элемент виден, иначе повторяет попытку до `Configuration.timeout`; если клик перехвачен — тоже повторяет;
- Playwright: «actionability» — элемент прикреплён, виден, не анимируется, включён и **не перекрыт** другим элементом.

### 7.1. Клики

| Вариант | Selenide | Playwright TS |
|---|---|---|
| Обычный клик | ✅ `button.click();` | ✅ `await button.click();` |
| Клик со своим таймаутом | ✅ `click(ClickOptions.usingDefaultMethod().timeout(Duration.ofSeconds(1)))` | ✅ `await button.click({ timeout: 1_000 })` |
| Клик через JavaScript (игнорирует перекрытия — антипаттерн) | ✅ `click(usingJavaScript())` | ✅ `await button.dispatchEvent('click')` |
| Клик без проверок «можно ли кликнуть» | — (JS-клик) | `await button.click({ force: true })` 💡 |
| Клик в точку элемента | `click(ClickOptions.usingDefaultMethod().offset(10, 5))` 💡 | `await button.click({ position: { x: 10, y: 5 } })` 💡 |
| Двойной | `doubleClick()` 💡 | `await el.dblclick()` 💡 |
| Правой кнопкой | `contextClick()` 💡 | `await el.click({ button: 'right' })` 💡 |
| С модификатором | `actions().keyDown(Keys.SHIFT).click(el).keyUp(Keys.SHIFT).perform()` 💡 | `await el.click({ modifiers: ['Shift'] })` 💡 |
| Навести мышь | `hover()` 💡 | `await el.hover()` 💡 |
| Проверить, что клик **возможен**, не кликая | — | `await el.click({ trial: true })` 💡 |
| Что будет, если элемент перекрыт модалкой | ✅ повторяет до таймаута, потом `ElementClickInterceptedException` («click intercepted») | ✅ ждёт до таймаута, потом `TimeoutError: ... intercepts pointer events` |

Тест перекрытия в обоих:

```java
// selenide CatalogTest ✅
assertThatThrownBy(() -> catalog.addButton(BACKPACK)
        .click(ClickOptions.usingDefaultMethod().timeout(Duration.ofSeconds(1))))
        .hasMessageContaining("click intercepted");
```

```ts
// playwright-ts catalog.spec.ts ✅
await expect(catalogPage.addButton(products.backpack).click({ timeout: 1_000 }))
  .rejects.toThrow(/intercepts pointer events/);
```

### 7.2. Ввод текста и клавиатура

| Вариант | Selenide | Playwright TS |
|---|---|---|
| Очистить и ввести | ✅ `username.setValue(user);` | ✅ `await this.username.fill(user);` |
| Дописать к существующему | `append("text")` 💡 | `await el.pressSequentially('text')` 💡 (печатает посимвольно) |
| Печатать по одной клавише (для автокомплита) | `sendKeys("text")` 💡 | `await el.pressSequentially('text', { delay: 50 })` 💡 |
| Уйти с поля (blur) | ✅ `firstName.setValue(first).pressTab();` | ✅ `await field.fill(value); await field.blur();` |
| Enter / Tab / Escape | `pressEnter()`, `pressTab()`, `pressEscape()` | `await el.press('Enter')`, `'Tab'`, `'Escape'` |
| Очистить | `clear()` | `await el.clear()` |

### 7.3. Выбор, чекбоксы, файлы, скролл

| Вариант | Selenide | Playwright TS |
|---|---|---|
| `<select>` по value | ✅ `sort.selectOptionByValue(value);` | ✅ `await this.sort.selectOption(value);` |
| `<select>` по тексту | `selectOption("Price (low to high)")` 💡 | `await select.selectOption({ label: 'Price (low to high)' })` 💡 |
| Чекбокс | `setSelected(true)` 💡 | `await cb.check()` / `setChecked(true)` 💡 |
| Загрузить файл с диска | ✅ `fileInput.uploadFile(file);` | `await input.setInputFiles('path/notes.txt')` 💡 |
| Загрузить файл из памяти | — (сначала записать на диск: JUnit ✅ `@TempDir`; TestNG ✅ `Files.createTempFile(...)` и удалить самому — `PlaygroundTest`) | ✅ `setInputFiles({ name: 'notes.txt', mimeType: 'text/plain', buffer: Buffer.from('...') })` |
| Прокрутить к элементу | ✅ `lazyBox.scrollIntoView("{block: 'center'}");` | ✅ `await this.lazyBox.scrollIntoViewIfNeeded();` |

---

## 8. Ожидания и проверки

### 8.1. Главное различие

| | Selenide | Playwright TS |
|---|---|---|
| Ожидающая проверка | `element.shouldHave(condition)` / `shouldBe(...)` — повторяется до `Configuration.timeout` | `await expect(locator).toHaveText(...)` — повторяется до `expect.timeout` |
| Неожидающая проверка | `element.has(condition)` / `is(condition)` — один раз, вернёт `boolean` | `await locator.isVisible()` — один раз; обычный `expect(value)` — тоже один раз |
| Чтение значения | `text()`, `getValue()`, `$$(...).texts()` — читают сразу, условие текста **не ждут** | `textContent()`, `inputValue()`, `allTextContents()` — читают сразу, текст **не ждут** |
| Ожидание «чего угодно» | ✅ `Wait().until(d -> ...)` (это `WebDriverWait`) | ✅ `page.waitForFunction(() => ...)` (в браузере), ✅ `expect.poll(() => ...)` (в тесте) |
| Повторять **действие + проверку** | ✅ `Wait().pollingEvery(...).until(d -> { if (...) return true; refresh.click(); return false; })` | ✅ `await expect(async () => { await click(); await expect(...).toHaveText(...); }).toPass()` |
| Ждать сетевой ответ | — | ✅ `page.waitForResponse(r => r.url().includes(...))` |
| Свой таймаут на одну проверку | ✅ `confirmation.shouldBe(visible, Duration.ofSeconds(20));` | ✅ `await expect(this.confirmation).toBeVisible({ timeout: 20_000 });` |

### 8.2. Условия для одного элемента

| Проверка | Selenide (`shouldHave` / `shouldBe`) | Playwright TS (`await expect(locator)...`) |
|---|---|---|
| Виден | ✅ `shouldBe(visible)` | ✅ `toBeVisible()` |
| Скрыт или его нет | ✅ `shouldBe(hidden)` | ✅ `toBeHidden()` |
| Исчез (скрыт или удалён) | ✅ `should(disappear)` | ✅ `toBeHidden()`; удалён из DOM — ✅ `waitFor({ state: 'detached' })` |
| Есть в DOM | `should(exist)` 💡 | `toBeAttached()` 💡 |
| Нет в DOM | `shouldNot(exist)` 💡 | `not.toBeAttached()` 💡 / `toHaveCount(0)` |
| Включён / выключен | ✅ `shouldBe(enabled)` / `shouldBe(disabled)` | ✅ `toBeEnabled()` / `toBeDisabled()` |
| Текст целиком | ✅ `shouldHave(exactText("Added"))` — **без учёта регистра** | ✅ `toHaveText('Added')` — **с учётом регистра**, пробелы нормализуются |
| Текст целиком, с регистром | `shouldHave(exactTextCaseSensitive("Added"))` 💡 | `toHaveText('Added')` |
| Содержит текст | ✅ `shouldHave(text("Camp Mug"))` — подстрока, без учёта регистра | ✅ `toContainText('Camp Mug')` |
| Текст по regex | `shouldHave(matchText("\\d+ products"))` 💡 | `toHaveText(/\d+ products/)` 💡 |
| Атрибут | `shouldHave(attribute("aria-valuenow", "100"))` 💡 | `toHaveAttribute('aria-valuenow', '100')` 💡 |
| Атрибут по regex | ✅ `shouldHave(attributeMatching("aria-valuenow", "[5-9]\\d\|100"))` | ✅ `toHaveAttribute('aria-valuenow', /^([5-9]\d\|100)$/)` |
| Значение поля | `shouldHave(value("abc"))` 💡 | `toHaveValue('abc')` 💡 |
| CSS-класс | `shouldHave(cssClass("added"))` 💡 | `toHaveClass(/added/)` 💡 |
| Пусто | `shouldBe(empty)` | `toBeEmpty()` |
| НЕ пусто | ✅ `resultCount.shouldNotBe(empty)` | ✅ `expect(this.resultCount).not.toBeEmpty()` |
| URL страницы | ✅ `webdriver().shouldHave(urlContaining("/catalog.html"), Duration.ofSeconds(15))` | ✅ `await expect(page).toHaveURL(/catalog\.html/)` |

Ловушка обеих библиотек: отрицательные проверки (`shouldNotBe(empty)`, `not.toBeEmpty()`, `not.toHaveText('…')`)
проходят сразу, если элемента ещё **нет в DOM**. В многостраничном приложении этого этапа это безопасно —
элемент есть с загрузки страницы; в SPA лучше проверять положительно: `toHaveText(/\d+ products/)`.

### 8.3. Условия для коллекции

| Проверка | Selenide (`ElementsCollection.shouldHave`) | Playwright TS (`await expect(locator)...`) |
|---|---|---|
| Количество | ✅ `rows().shouldHave(size(10))` | ✅ `toHaveCount(10)` |
| Больше N | `shouldHave(sizeGreaterThan(5))` 💡 | `expect.poll(() => rows.count()).toBeGreaterThan(5)` 💡 |
| Ровно эти тексты, по порядку | ✅ `names().shouldHave(exactTexts("Cotton T-Shirt"))` | ✅ `toHaveText(['Cotton T-Shirt'])` (массив = весь список) |
| Эти тексты, порядок не важен | ✅ `exactTextsCaseSensitiveInAnyOrder(BACKPACK.name(), CAMP_MUG.name())` | `toHaveText([...].sort())` не подойдёт — сравнивать через `expect.poll` 💡 |
| Каждый содержит подстроку | `texts("Camp", "Steel")` 💡 | `toContainText(['Camp', 'Steel'])` 💡 |
| Все элементы удовлетворяют условию | ✅ `prices.shouldHave(allMatch("displayed", WebElement::isDisplayed))` | — (через `expect.poll` или цикл) |
| Пусто | ✅ `shouldHave(size(0))` | ✅ `toHaveCount(0)` |

### 8.4. Сложные ожидания из репозитория

**Готовность страницы (нет запросов в полёте, спиннер скрыт)**

```java
// selenide pages/BasePage.java ✅
Wait().until(d -> Boolean.TRUE.equals(executeJavaScript(
        "return document.readyState === 'complete' && (window.__pending || 0) === 0;")));
SPINNER.should(disappear);
```

```ts
// playwright-ts pages/base-page.ts ✅
await this.page.waitForFunction(() => document.readyState === 'complete' && (window.__pending ?? 0) === 0);
await expect(this.spinner).toBeHidden();
```

**Поиск с задержкой 400 мс (debounce): дождаться нового списка**

```java
// selenide: ничего не ждём в page object — проверка в тесте сама повторяется ✅
catalog.search("shirt");
catalog.names().shouldHave(exactTexts("Cotton T-Shirt"));
```

```ts
// playwright-ts: ждём именно запрос, который вызвал ввод ✅ pages/catalog-page.ts
const response = this.page.waitForResponse(r => r.url().includes(`/api/products?q=${encodeURIComponent(query)}`));
await this.search.fill(query);
await response;
await expect(this.spinner).toBeHidden();
```

**«Жми Refresh, пока статус не станет COMPLETED»**

```java
// selenide pages/OrdersPage.java ✅
Wait().withTimeout(timeout).pollingEvery(Duration.ofSeconds(1)).until(d -> {
    if (status(orderId).has(exactText(expected))) return true;   // has() = проверка БЕЗ ожидания
    refresh.click();
    waitUntilReady();
    return false;
});
```

```ts
// playwright-ts pages/orders-page.ts ✅
await expect(async () => {
  await this.refreshButton.click();
  await expect(this.status(orderId)).toHaveText(status, { timeout: 1_000 });
}).toPass({ intervals: [1_000], timeout: 20_000 });
```

**Проверить состояние бэкенда, пока не совпадёт**

```java
// selenide CheckoutTest ✅ — один раз, без повторов (к этому моменту заказ уже точно создан)
assertThat(api.orders(user)).isEqualTo(1);
```

```ts
// playwright-ts checkout.spec.ts ✅ — повторяется до совпадения
await expect.poll(() => api.state(user)).toEqual({ cartItems: 0, orders: 1 });
```

**Список отсортирован (своё условие)**

```java
// selenide CatalogTest ✅
Wait().until(d -> isSorted(catalog.pricesInCents(), order));
assertThat(catalog.pricesInCents()).hasSize(12).isSortedAccordingTo(order);
```

```ts
// playwright-ts catalog.spec.ts ✅ — sortBy() уже дождался ответа сервера, дальше обычная проверка
await catalogPage.sortBy(sort);
const prices = await catalogPage.pricesInCents();
expect(prices).toEqual([...prices].sort((a, b) => (sort === 'price-asc' ? a - b : b - a)));
```

---

## 9. Браузерные возможности

| Что | Selenide | Playwright TS |
|---|---|---|
| Открыть страницу | ✅ `Selenide.open("/playground.html")` | ✅ `await page.goto('/playground.html')` |
| Confirm: принять | ✅ `confirmButton.click(); Selenide.confirm();` — ждёт alert сам | ✅ `const d = page.waitForEvent('dialog'); await btn.click(); await (await d).accept();` |
| Confirm: отклонить | ✅ `Selenide.dismiss()` | ✅ `page.once('dialog', d => d.dismiss())` **до** клика |
| Prompt | ✅ `promptButton.click(); Selenide.prompt("Captain");` | ✅ `page.once('dialog', d => d.accept('Captain'))` |
| Диалог без обработчика | остаётся открытым (`unhandledPromptBehavior=ignore`) | закрывается автоматически (`dismiss`) — тест может проверить не ту ветку |
| iframe | ✅ `switchTo().frame(supportFrame); try {...} finally { switchTo().defaultContent(); }` | ✅ `page.frameLocator('#support-frame').getByRole('button', { name: 'Send' }).click()` |
| Shadow DOM | ✅ `$(shadowCss("#apply", "#promo")).click()` | ✅ `page.getByRole('button', { name: 'Apply' }).click()` |
| Новая вкладка | ✅ `openReceipt.click(); switchTo().window(1); ... closeWindow(); switchTo().window(0);` | ✅ `const p = page.waitForEvent('popup'); await link.click(); const receipt = await p;` |
| Скачать файл | ✅ `download.download(DownloadOptions.using(FOLDER).withFilter(withExtension("csv")))` → `File` | ✅ `const d = page.waitForEvent('download'); await link.click(); await (await d).saveAs(path)` |
| Выполнить JS | ✅ `executeJavaScript("return [42, 1.5, ...];")` — строка с `return` | `await page.evaluate(() => [42, 1.5, ...])` — настоящая функция |
| Асинхронный JS | ✅ `executeAsyncJavaScript("const done = arguments[arguments.length - 1]; setTimeout(() => done('finished'), 500);")` | `await page.evaluate(() => new Promise(r => setTimeout(() => r('finished'), 500)))` |
| localStorage | ✅ `localStorage().setItem("token", token)` / `getItem(...)` | `await page.evaluate(k => localStorage.getItem(k), 'username')`; при создании контекста — `storageState` ✅ |
| Скриншот страницы | ✅ `screenshot("selenide-page")` (png + html) | `await page.screenshot({ path })` |
| Скриншот элемента | ✅ `promoWidget().screenshot()` | `await locator.screenshot()` |
| Вся страница целиком | ✅ только через CDP (`HasCdp`, `Page.captureScreenshot`) | `await page.screenshot({ fullPage: true })` |
| Визуальное сравнение с эталоном | — | `await expect(page).toHaveScreenshot('name.png', { mask: [...] })` |
| Подменить ответ сервера | — (нужен прокси) | ✅ `page.route('**/api/products*', r => r.fulfill({ status: 500, json: {...} }))` |
| Доступ к «сырому» драйверу | `WebDriverRunner.getWebDriver()` | `page`, `context`, `browser` и так доступны |

---

## 10. Тестовые данные, API и логин без UI

| Что | Selenide ✅ `api/BackendApi.java` | Playwright TS ✅ `support/api-client.ts` + `fixtures.ts` |
|---|---|---|
| Чем делаются запросы | `fetch()` **внутри браузера** через `executeAsyncJavaScript(FETCH, ...)` | отдельный HTTP-клиент `APIRequestContext` — браузер не нужен |
| Что нужно перед запросом | открыть страницу приложения (`ensureOnOrigin()` → `open("/blank.html")`) | ничего |
| Логин без UI | `call("POST", "/api/login", ...)` → `localStorage().setItem("token", ...)` | фикстура `storageState`: контекст **создаётся** уже с токеном в localStorage |
| Отключить промо-окно | `localStorage().setItem("promo.dismissed", "1")` | `{ name: 'promo.dismissed', value: '1' }` в `storageState`; выключить — `test.use({ dismissPromo: false })` |
| Очистка после теста | JUnit: `@AfterEach` → `api.cleanup(user)`; TestNG: `@AfterMethod(alwaysRun = true)` → `api().cleanup(user())` | teardown фикстуры `api` → `api.cleanup(user)` |
| Проверка ответа | `if (status != expected) throw new IllegalStateException(...)` | `await expect(response).toBeOK()` — в ошибке статус, URL и тело |
| Уникальный пользователь на тест | ✅ `TestUser.unique()` → `user_xxxxxxxx` (JUnit — поле `BaseTest`; TestNG — `@BeforeMethod`, `ThreadLocal`) | ✅ `uniqueUser('normal')` → `user_xxxxxxxx` |
| Медленный / заблокированный | ✅ `TestUser.slow()`, `TestUser.locked()` (TestNG: `useUser(TestUser.slow())`) | ✅ `uniqueUser('slow')`, `uniqueUser('locked')`; `test.use({ userKind: 'slow' })` |

---

## 11. Теги, сьюты, параметризация, шаги

| Что | Selenide + JUnit 5 | Selenide + TestNG | Playwright TS |
|---|---|---|---|
| Метка на тест | ✅ `@Smoke` (= `@Tag("smoke")`, `tags/Smoke.java`) | ✅ `@Test(groups = Groups.SMOKE)` (`Groups.java` — константы) | ✅ `test('...', { tag: '@smoke' }, async (...) => ...)` |
| Несколько меток | ✅ `@Smoke @Critical` | ✅ `@Test(groups = {Groups.SMOKE, Groups.CRITICAL})` — `CartTest` | ✅ `{ tag: ['@smoke', '@critical'] }` |
| Метка на группу тестов | ✅ `@Regression` на классе (`CatalogTest`) | ✅ `@Test(groups = Groups.REGRESSION)` на классе (`CatalogTest`, `OrdersTest`) | ✅ `test.describe('Catalog', { tag: '@regression' }, () => ...)` |
| Сьют | ✅ `@Suite @SelectPackages(...) @IncludeTags("smoke")` | ✅ XML-файл: `<groups><run><include name="smoke"/>` (`smoke.xml`) | npm-скрипт `test:smoke` = `playwright test --grep @smoke` |
| Явный список тестов в сьюте | `@SelectClasses(...)` / `@SelectMethod(...)` 💡 | ✅ `<classes><class name="..."><methods><include name="..."/>` (`critical.xml`) | список файлов в CLI |
| Группа тестов | класс | класс | ✅ `test.describe('...', () => { ... })` |
| Имя для отчёта | ✅ `@DisplayName("Cart")` | ✅ `@Test(description = "...")` — `LoginTest`, `CheckoutTest` | первый аргумент `describe` / `test` |
| Параметризация | ✅ `@ParameterizedTest @CsvSource({...})` / `@ValueSource(strings = {...})` | ✅ `@DataProvider` + `@Test(dataProvider = "...")` — `LoginTest`, `CatalogTest`, `CheckoutTest` | ✅ обычный цикл: `for (const zip of ['123', 'abcde']) test(\`invalid zip "${zip}"\`, ...)` |
| Параметры из конфигурации прогона | `-D...` | ✅ `<parameter>` в XML + `@Parameters` (`BaseTest.beforeSuite`) | `test.use({...})` / `process.env` |
| Именованные шаги в отчёте | — (есть в Allure: `Allure.step(...)`) | — (так же Allure; `Reporter.log(...)` 💡 — строка в HTML-отчёте TestNG) | ✅ `await test.step('check out', async () => { ... })` |
| Пропустить | `@Disabled("why")` 💡 | `@Test(enabled = false)` 💡 | `test.skip(...)` / `test.fixme(...)` 💡 |
| Пропустить по условию во время теста | ✅ `assumeTrue(...)` → aborted (`JavaScriptAndScreenshotsTest`) | ✅ `throw new SkipException(...)` → skipped (`JavaScriptAndScreenshotsTest`) | `test.skip(condition, 'why')` 💡 |
| Запустить только этот | — (`-Dtest=...`) | — (`-Dtest=...` или XML с одним методом) | `test.only(...)` 💡 (`forbidOnly` роняет CI) |
| Помечено как известный баг | — | — (`expectedExceptions` 💡 — только для исключений) | `test.fail()` 💡 — тест обязан упасть |

---

## 12. Артефакты падения и отчёты

| Что | Selenide + JUnit 5 | Selenide + TestNG | Playwright TS |
|---|---|---|---|
| Скриншот упавшей проверки | ✅ автоматически при падении `should*()` | ✅ так же (это Selenide) | ✅ `use.screenshot: 'only-on-failure'` |
| Скриншот при падении не-Selenide проверки (AssertJ) | ✅ `ScreenShooterExtension` в `@ExtendWith` | ✅ `FailureScreenshotListener` в `@Listeners` на `BaseTest` | тот же `screenshot` из конфига — для любой ошибки |
| HTML страницы | ✅ `savePageSource = true` | ✅ так же | внутри trace (снимок DOM на каждом шаге) + `error-context.md` |
| Trace (пошаговая запись с сетью и консолью) | — | — | ✅ `use.trace: 'retain-on-failure'`, `npx playwright show-trace` |
| Видео | — | — | ✅ `use.video: 'retain-on-failure'` |
| Где лежит | `selenide/target/artifacts/selenide-reports/` | `selenide-testng/target/artifacts/selenide-reports/` | `playwright-ts/test-results/<тест>/` |
| Отчёт | `selenide/target/surefire-reports/*.xml` | `selenide-testng/target/surefire-reports/`: `index.html` (HTML TestNG), `emailable-report.html`, `testng-results.xml` | `playwright-ts/playwright-report/` (HTML, `npm run report`) + `test-results/junit.xml` |
| Набор «перезапустить упавшие» | — | ✅ `testng-failed.xml` в `surefire-reports/` (появляется после падений) | `npx playwright test --last-failed` |
| Флейки в отчёте | surefire: `Flakes` (с `rerunFailingTestsCount`) | неудачные попытки — `SKIPPED`, последняя — итог | `flaky` в отчёте |
| Путь к скриншоту в тексте ошибки | ✅ да (`Screenshot: file:/...`) | ✅ да | в отчёте и в консоли (`attachment #1: screenshot`) |
| Allure | ✅ `selenide/target/allure-results/` | ✅ `selenide-testng/target/allure-results/` | ✅ `playwright-ts/allure-results/` |

### 12.1. Allure: один отчёт для трёх модулей ✅

Общий отчёт собирается из корня: `npm ci` (один раз), потом `npm run allure2:open` (классический Allure 2) или
`npm run allure3:open` (Allure 3) — см. [RUNNING.md](RUNNING.md). Фреймворк виден по метке `epic` (Allure 3: верхний
уровень дерева, Allure 2: вкладка Behaviors) и по `parentSuite` (Allure 2: вкладка Suites). Что видно в отчёте у каждого (проверено на полном прогоне):

| | Selenide + JUnit 5 | Selenide + TestNG | Playwright TS |
|---|---|---|---|
| Подключение | `allure-junit5` + `allure-selenide` в `pom.xml`; listener результатов подключается сам (ServiceLoader) | `allure-testng` + `allure-selenide`; listener подключается сам | `['allure-playwright', {...}]` в `reporter` конфига |
| Шаги | каждое действие Selenide: `AllureSelenide` (`core/AllureSupport`, подключается в `beforeEach` — listener'ы `SelenideLogger` живут в `ThreadLocal`) | так же, подключается в `@BeforeMethod` | каждое действие Playwright, `expect`, запросы API, `test.step` — без кода |
| Имя теста | имя метода; у `@ParameterizedTest` — с аргументами: `invalidZipBlocksTheOrder(String) zip='abcde'` | имя метода или `@Test(description)`; у `DataProvider` имена одинаковые, отличаются параметры | заголовок `test('...')` |
| Дерево | `parentSuite` = `Selenide + JUnit 5` (задан в `selenide/pom.xml`), `suite` = `@DisplayName` класса | `parentSuite` = `<suite name>`, `suite` = `<test name>`, `subSuite` = класс | `parentSuite` = project, `suite` = файл, `subSuite` = `describe` |
| Теги (фильтр Tags) | ✅ `@Tag` → `tag` | — группы TestNG в теги **не** попадают | ✅ `{ tag }` → `tag` |
| Параметры | аргументы `@ParameterizedTest` (`arg0`, `arg1`) | аргументы `DataProvider` **и** `@Parameters` из XML (`browser`, `headless`) — на каждом тесте | только `Project` |
| Set up / Tear down | `@AfterEach` → Tear down (нужен `junit.jupiter.extensions.autodetection.enabled=true`); поля `BaseTest` не видны — это не хук | `@BeforeSuite`, `@BeforeMethod`, `@AfterMethod` — Set up / Tear down | Before Hooks / After Hooks: фикстуры с вложенными шагами |
| Падение `should*()` | ✅ скриншот + HTML на упавшем шаге | ✅ так же | ✅ скриншот, trace, видео у теста |
| Падение AssertJ | ✅ `AllureSupport.attachFailure` из `afterTestExecution` | ✅ то же из `FailureScreenshotListener` | ✅ из конфига, без кода |
| Старые результаты | удаляет `GlobalLifecycle` до тестов | удаляет `@BeforeSuite` | удаляет `global-setup.ts` |

---

## 13. Короткий словарь «перевода»

Строки про Selenide (`$`, `should*`, `open`, …) одинаковы для обоих Java-модулей — различаются только раннеры.

| Selenide | Playwright TS |
|---|---|
| `$("#id")` | `page.locator('#id')` |
| `$$(".x")` | `page.locator('.x')` |
| `open("/path")` | `await page.goto('/path')` |
| `el.click()` | `await el.click()` |
| `el.setValue("x")` | `await el.fill('x')` |
| `el.pressTab()` | `await el.blur()` / `await el.press('Tab')` |
| `el.selectOptionByValue("v")` | `await el.selectOption('v')` |
| `el.uploadFile(file)` | `await el.setInputFiles(path)` |
| `el.shouldBe(visible)` | `await expect(el).toBeVisible()` |
| `el.should(disappear)` | `await expect(el).toBeHidden()` |
| `el.shouldHave(exactText("x"))` | `await expect(el).toHaveText('x')` |
| `el.shouldHave(text("x"))` | `await expect(el).toContainText('x')` |
| `col.shouldHave(size(3))` | `await expect(col).toHaveCount(3)` |
| `col.shouldHave(exactTexts("a", "b"))` | `await expect(col).toHaveText(['a', 'b'])` |
| `el.shouldBe(disabled)` | `await expect(el).toBeDisabled()` |
| `el.has(cond)` | `await el.isVisible()` и т. п. (без ожидания) |
| `el.text()` | `await el.textContent()` |
| `col.texts()` | `await col.allTextContents()` |
| `Wait().until(...)` | `await expect.poll(...)` / `page.waitForFunction(...)` |
| цикл в `Wait()` с действием | `await expect(async () => {...}).toPass()` |
| `executeJavaScript("return ...")` | `await page.evaluate(() => ...)` |
| `localStorage().setItem(k, v)` | `storageState` / `page.evaluate(...)` |
| `Selenide.confirm()` | `page.once('dialog', d => d.accept())` до клика |
| `switchTo().frame(f)` | `page.frameLocator(...)` |
| `switchTo().window(1)` | `const p = page.waitForEvent('popup')` |
| `$(shadowCss("#a", "#host"))` | `page.locator('#a')` |
| `$(byRole("button", "Log in"))` | `page.getByRole('button', { name: 'Log in' })` |
| `$(byLabel("Username"))` | `page.getByLabel('Username')` |
| `screenshot("name")` | `await page.screenshot({ path })` |

Раннеры:

| Selenide + JUnit 5 | Selenide + TestNG | Playwright TS |
|---|---|---|
| поля / `@BeforeEach` в `BaseTest` | `@BeforeMethod(alwaysRun = true)` в `BaseTest` | фикстура в `support/fixtures.ts` |
| `@BeforeEach` в тестовом классе | `@BeforeMethod(alwaysRun = true)` в тестовом классе | `test.beforeEach(...)` |
| `@AfterEach` | `@AfterMethod(alwaysRun = true)` | код после `await use(...)` / `test.afterEach(...)` |
| `@BeforeAll` / `@AfterAll` (static) | `@BeforeClass` / `@AfterClass` (не static) | `test.beforeAll` / `test.afterAll` (на каждый worker!) |
| `LauncherSessionListener` / `@BeforeSuite` (`@Suite`) | `@BeforeSuite` | `globalSetup` |
| `@Tag("smoke")` / `-Dgroups=smoke` | `@Test(groups = "smoke")` / `-Dsuite=smoke` | `{ tag: '@smoke' }` / `--grep @smoke` |
| `@Suite`-класс | `testng.xml` | `--grep` / `projects` |
| `@ParameterizedTest` | `@DataProvider` | цикл `for` вокруг `test(...)` |
| `@ExtendWith(...)` | `@Listeners(...)` / `<listeners>` в XML | фикстура / `reporter` |
| `rerunFailingTestsCount` | `IRetryAnalyzer` | `retries` |
| `assumeTrue(...)` | `throw new SkipException(...)` | `test.skip(cond, ...)` |
| `junit-platform.properties` (parallel) | `parallel` / `thread-count` в XML | `workers` / `fullyParallel` |
| `Configuration.timeout` | `Configuration.timeout` | `expect.timeout` + `use.actionTimeout` |
| `Configuration.baseUrl` | `Configuration.baseUrl` | `use.baseURL` |
| `-Dheadless=false` | `-Dheadless=false` / `<parameter name="headless">` | `--headed` |

---

## 14. TestNG ↔ JUnit 5 ↔ Playwright Test

Модуль [`selenide-testng/`](selenide-testng) — те же Selenide, page objects и 35 сценариев, что в `selenide/`,
но запускает их **TestNG**. Колонки ниже: TestNG (`selenide-testng`), JUnit 5 (`selenide`), Playwright Test (`playwright-ts`).

### 14.1. Где что лежит

| Роль | TestNG | JUnit 5 | Playwright TS |
|---|---|---|---|
| Раннер | `org.testng:testng` 7.12 | JUnit Jupiter 5.14 | `@playwright/test` |
| Набор тестов на запуск | **XML-файлы** ✅ `selenide-testng/src/test/resources/testng/*.xml` | Java-классы `@Suite` ✅ `selenide/.../suites/*.java` | конфиг + `--grep` / npm-скрипты |
| Группа на тесте | ✅ `@Test(groups = {Groups.SMOKE, Groups.CRITICAL})` | ✅ `@Smoke @Critical` (= `@Tag`) | ✅ `{ tag: ['@smoke', '@critical'] }` |
| Константы групп | ✅ `tests/Groups.java` | ✅ аннотации в `tags/` | строки `'@smoke'` |
| Базовый класс | ✅ `tests/BaseTest.java` (`@BeforeSuite`, `@BeforeMethod`, `@AfterMethod`, `ThreadLocal`) | ✅ `tests/BaseTest.java` + extensions | ✅ `support/fixtures.ts` |
| Listener'ы | ✅ `listeners/FailureScreenshotListener`, `RetryAnalyzer`, `RetryTransformer`, `ConsoleLogListener` | ✅ `SelenideSetup` (extension), `ScreenShooterExtension` | конфиг (`reporter`, `use.screenshot`) |
| Настройка Maven | ✅ `selenide-testng/pom.xml`: `suiteXmlFiles`, surefire **3.5.6** + `surefire-testng` | ✅ surefire 3.6.1 из родительского `pom.xml` | — |

### 14.2. Хуки: все уровни

| Уровень | TestNG | JUnit 5 | Playwright TS |
|---|---|---|---|
| Один раз за прогон | ✅ `@BeforeSuite(alwaysRun = true)` в `BaseTest` — выполняется один раз, хотя её наследуют все классы | ✅ `LauncherSessionListener` | ✅ `globalSetup` |
| После всего прогона | `@AfterSuite(alwaysRun = true)` 💡 | `launcherSessionClosed` 💡 | `globalTeardown` 💡 |
| Перед блоком `<test>` из XML | `@BeforeTest` 💡 — по разу на каждый `<test>` (в `regression.xml` их два) | — | — |
| Перед группой | `@BeforeGroups(groups = "smoke")` 💡 — перед первым тестом группы | — | — |
| Перед классом | `@BeforeClass` 💡 (метод **не** static: экземпляр класса один) | `@BeforeAll` 💡 (static) | `test.beforeAll` 💡 (на каждый worker!) |
| Перед каждым тестом, база | ✅ `BaseTest.createTestUser()` — `@BeforeMethod(alwaysRun = true)` | ✅ поля `BaseTest` (новый экземпляр на тест) | ✅ фикстуры |
| Перед каждым тестом, класс | ✅ `PlaygroundTest.openPlayground()` — `@BeforeMethod(alwaysRun = true)` | ✅ `@BeforeEach` | ✅ `test.beforeEach` |
| После каждого теста | ✅ `BaseTest.cleanUp()` — `@AfterMethod(alwaysRun = true)`: cleanup API + `closeWebDriver()` | ✅ `@AfterEach` + extension `afterEach` | ✅ teardown фикстур |
| Сразу после тела теста, до `@AfterMethod` | ✅ `IInvokedMethodListener.afterInvocation` (скриншот) | ✅ `AfterTestExecutionCallback` | `use.screenshot` в конфиге |
| Порядок в иерархии | сначала `@BeforeMethod` базового класса, потом наследника; `@AfterMethod` — наоборот | так же | фикстуры — по графу зависимостей |

**Порядок выполнения в TestNG** — проверен на `FailureDemoTest` с ретраем по выводу в консоли:

```
@BeforeSuite                         BaseTest.beforeSuite(): параметры из XML, настройка Selenide, сброс данных
  @BeforeTest                        (в репо нет) - на каждый блок <test> XML-файла
    @BeforeClass                     (в репо нет)
      @BeforeMethod (база)           BaseTest.createTestUser()
      @BeforeMethod (класс)          PlaygroundTest.openPlayground()
      @Test
      afterInvocation                FailureScreenshotListener - скриншот, браузер ещё открыт
      IRetryAnalyzer.retry           RetryAnalyzer - повторить?
      onTestFailure / onTestSkipped  ConsoleLogListener (первая попытка при ретрае = SKIPPED)
      @AfterMethod                   BaseTest.cleanUp(): cleanup данных, closeWebDriver()
    @AfterClass
  @AfterTest
@AfterSuite
```

### 14.3. `alwaysRun` — главная ловушка TestNG

Когда XML фильтрует по группам (`<include name="smoke"/>`), TestNG запускает только методы этих групп — **включая
конфигурационные**. `@BeforeMethod` без группы будет **пропущен**, и тест пойдёт без пользователя и без очистки.

| | TestNG | JUnit 5 | Playwright TS |
|---|---|---|---|
| Хук без группы при фильтре по группе | **не выполняется**, нужен `alwaysRun = true` ✅ во всех хуках `BaseTest` и `PlaygroundTest` | выполняется всегда | выполняется всегда |
| Хук после упавшего теста | `@AfterMethod(alwaysRun = true)` — гарантированно | `@AfterEach` — всегда | teardown фикстуры — всегда |

### 14.4. Сколько экземпляров тестового класса — и почему `ThreadLocal`

| | TestNG | JUnit 5 | Playwright TS |
|---|---|---|---|
| Экземпляр тестового класса | **один на все методы класса** | новый на каждый метод | классов нет; у каждого теста свой набор фикстур |
| Поле `user = TestUser.unique()` | общее для всех тестов класса — **нельзя** | своё у каждого теста — можно ✅ | — (фикстура `user`) |
| Где создавать данные теста | ✅ `@BeforeMethod` | ✅ поле или `@BeforeEach` | ✅ фикстура |
| Параллельно методы одного класса | `parallel="methods"` — **одно** поле читают несколько потоков → ✅ `ThreadLocal` в `BaseTest` | при `mode.default=concurrent` у каждого метода свой экземпляр, поля безопасны | `fullyParallel: true` — у каждого теста свои фикстуры |
| Доступ в тесте | ✅ `user()`, `api()`, `useUser(TestUser.slow())` | ✅ `user`, `api`, `user = TestUser.slow()` | ✅ `({ user, api })`, `test.use({ userKind: 'slow' })` |

### 14.5. Набор тестов на запуск: `testng.xml` ↔ `@Suite` ↔ конфиг

Один и тот же smoke-набор тремя способами:

```xml
<!-- TestNG ✅ selenide-testng/src/test/resources/testng/smoke.xml -->
<suite name="Selenide + TestNG: smoke" parallel="classes" thread-count="3">
    <listeners>
        <listener class-name="com.compare.testng.listeners.RetryTransformer"/>
        <listener class-name="com.compare.testng.listeners.ConsoleLogListener"/>
    </listeners>
    <test name="Smoke">
        <groups><run><include name="smoke"/></run></groups>
        <packages><package name="com.compare.testng.tests"/></packages>
    </test>
</suite>
```

```java
// JUnit 5 ✅ selenide/src/test/java/com/compare/selenide/suites/SmokeSuite.java
@Suite
@SelectPackages("com.compare.selenide.tests")
@IncludeTags("smoke")
public class SmokeSuite { ... }
```

```jsonc
// Playwright TS ✅ playwright-ts/package.json
"test:smoke": "playwright test --grep @smoke"
```

| Что задать | TestNG (`testng.xml`) | JUnit 5 | Playwright TS |
|---|---|---|---|
| Пакет целиком | ✅ `<packages><package name="..."/></packages>` | ✅ `@SelectPackages(...)` | `testDir` / путь в CLI |
| Конкретные классы | ✅ `<classes><class name="...CartTest"/></classes>` (`critical.xml`) | `@SelectClasses(...)` 💡 | `npx playwright test tests/cart.spec.ts` |
| Конкретные методы | ✅ `<class ...><methods><include name="userBuysProducts..."/></methods></class>` (`critical.xml`) | `@SelectMethod(...)` 💡 / `-Dtest=Class#method` | `-g "имя"` / `файл:строка` |
| Включить группы | ✅ `<run><include name="smoke"/></run>` | ✅ `@IncludeTags("smoke")` / `-Dgroups=smoke` | ✅ `--grep @smoke` |
| Исключить группы | ✅ `<run><exclude name="demo-failure"/></run>` (`all.xml`) | ✅ `excludedGroups` / `@ExcludeTags` | ✅ `grepInvert` / `--grep-invert` |
| Несколько частей прогона | ✅ несколько `<test>` в одном `<suite>` (`regression.xml`: «Shop regression» + «Browser features») | несколько `@Suite`-классов | `projects` |
| Параметры | ✅ `<parameter name="browser" value="chrome"/>` → `@Parameters` + `@Optional` в `@BeforeSuite` (`all.xml`) | системные свойства `-D...` | `use` / `process.env` |
| Listener'ы | ✅ `<listeners>` в XML (или `@Listeners` на классе) | `@ExtendWith` / `META-INF/services` | `reporter` в конфиге |
| Параллельность | ✅ `parallel="classes" thread-count="3"` прямо в XML | `junit-platform.properties` | `workers`, `fullyParallel` |
| Выбрать набор при запуске | ✅ `-Dsuite=smoke` (→ `suiteXmlFiles` в `pom.xml`) | ✅ `-Dtest=SmokeSuite` | ✅ `npm run test:smoke` |
| Перезапустить только упавшие | TestNG сам пишет `target/surefire-reports/testng-failed.xml` | `rerunFailingTestsCount` (в том же прогоне) | `npx playwright test --last-failed` 💡 |

Сьюты модуля `selenide-testng`:

| Файл | Что внутри | Команда | Проверено |
|---|---|---|---|
| `all.xml` (по умолчанию) | весь пакет без `demo-failure`, `parallel="classes"`, параметры, listener'ы | `./mvnw -pl selenide-testng -am test` | 35 тестов ✅ |
| `smoke.xml` | группа `smoke` | `... -Dsuite=smoke` | 3 ✅ |
| `critical.xml` | список классов и методов + группа `critical` | `... -Dsuite=critical` | 2 ✅ |
| `regression.xml` | два блока `<test>`: `regression` и `browser-api` | `... -Dsuite=regression` | 31 ✅ |
| `parallel-methods.xml` | `parallel="methods" thread-count="4"` | `... -Dsuite=parallel-methods` | 35 ✅ |
| `demo-failure.xml` | падающий тест + ретрай | `... -Dsuite=demo-failure -Dtest.retries=1` | 1 падение, скриншот, ретрай ✅ |

`-Dtest=CartTest` по-прежнему работает: surefire тогда игнорирует XML (проверено, 3 теста).

### 14.6. Параметризация

| | TestNG | JUnit 5 | Playwright TS |
|---|---|---|---|
| Способ | ✅ `@DataProvider` — метод, возвращающий строки | ✅ `@ParameterizedTest` + `@CsvSource` / `@ValueSource` | ✅ цикл `for` вокруг `test(...)` |
| Пример | `@DataProvider(name = "invalidCredentials")` + `@Test(dataProvider = "invalidCredentials")` (`LoginTest`) | `@CsvSource({"'', secret, ...", ...})` (`LoginTest`) | `for (const { username, password, error } of invalid) test(...)` (`login.spec.ts`) |
| Данные из файла / БД | DataProvider — обычный Java-код, читает что угодно | `@CsvFileSource`, `@MethodSource` 💡 | любой код вокруг цикла |
| Строки параллельно | `@DataProvider(parallel = true)` + `data-provider-thread-count` 💡 | как обычные тесты | каждая строка — отдельный тест |
| Имя в отчёте | метод + параметры | `@ParameterizedTest(name = "...")` ✅ | шаблонная строка в названии ✅ |

```java
// TestNG ✅ selenide-testng/.../tests/LoginTest.java
@DataProvider(name = "invalidCredentials")
public Object[][] invalidCredentials() {
    return new Object[][] {
            {"", "secret", "Username and password are required"},
            {"someone", "wrong", "Username and password do not match any user"},
    };
}

@Test(groups = Groups.REGRESSION, dataProvider = "invalidCredentials")
public void invalidCredentialsAreRejected(String username, String password, String expectedError) { ... }
```

### 14.7. Параллельность

| | TestNG (атрибуты `<suite>`) | JUnit 5 (`junit-platform.properties`) | Playwright TS |
|---|---|---|---|
| Включить | ✅ `parallel="classes"` | ✅ `parallel.enabled=true` | по умолчанию параллельно |
| Классы параллельно, методы класса по очереди | ✅ `parallel="classes"` | ✅ `classes.default=concurrent`, `mode.default=same_thread` | `fullyParallel: false` |
| Всё параллельно | ✅ `parallel="methods"` (`parallel-methods.xml`) | `mode.default=concurrent` | ✅ `fullyParallel: true` |
| Блоки `<test>` параллельно | `parallel="tests"` 💡 | — | `projects` идут параллельно |
| Число потоков | ✅ `thread-count="3"` | ✅ `fixed.parallelism=3` | ✅ `workers: 3` |
| Чем исполняется | потоки в одной JVM | потоки ForkJoin в одной JVM | **процессы** (workers) |
| Общий `Configuration` Selenide | статика: настраивается один раз в `@BeforeSuite`; разные браузеры в одном прогоне не получатся | так же, в `LauncherSessionListener` | у каждого project свой `use` |

### 14.8. Ретраи и listener'ы

| | TestNG | JUnit 5 | Playwright TS |
|---|---|---|---|
| Ретраи | ✅ `IRetryAnalyzer` (`RetryAnalyzer`): на один тест — `@Test(retryAnalyzer = ...)`, на все — ✅ `IAnnotationTransformer` (`RetryTransformer`) | ✅ `rerunFailingTestsCount` в surefire | ✅ `retries` в конфиге |
| Как видно в отчёте | неудачная попытка — **SKIPPED**, итог — по последней | `Flakes` в отчёте surefire | статус **flaky** |
| Регистрация listener'а | `@Listeners` на классе ✅ (`BaseTest`) или `<listeners>` в XML ✅; `IAnnotationTransformer` — **только** XML / ServiceLoader | `@ExtendWith` ✅ / ServiceLoader ✅ | конфиг |
| Скриншот при падении | ✅ `IInvokedMethodListener.afterInvocation` (`FailureScreenshotListener`); у Selenide есть готовый `com.codeborne.selenide.testng.ScreenShooter` 💡 | ✅ `ScreenShooterExtension` | ✅ `use.screenshot` |
| Лог каждого теста | ✅ `ITestListener` (`ConsoleLogListener`: поток, статус, группы, время) | `TestWatcher` 💡 | `reporter: 'list'` ✅ |

### 14.9. Остальные возможности раннеров

| Что | TestNG | JUnit 5 | Playwright TS |
|---|---|---|---|
| Ожидаемое исключение | `@Test(expectedExceptions = X.class, expectedExceptionsMessageRegExp = ".*")` 💡 | `assertThrows(...)` 💡 | `await expect(promise).rejects.toThrow(...)` ✅ |
| Пропустить по условию | ✅ `throw new SkipException("...")` (`JavaScriptAndScreenshotsTest`) | ✅ `assumeTrue(...)` | `test.skip(condition, '...')` 💡 |
| Отключить тест | `@Test(enabled = false)` 💡 | `@Disabled` 💡 | `test.skip(...)` / `test.fixme(...)` 💡 |
| Зависимость от другого теста | `dependsOnMethods` / `dependsOnGroups` 💡 (антипаттерн) | — | `test.describe.configure({ mode: 'serial' })` 💡 |
| Порядок тестов | `priority = 1` 💡 | `@Order` + `@TestMethodOrder` 💡 | порядок в файле при `serial` |
| Повторить N раз | `invocationCount = 5` 💡 | `@RepeatedTest(5)` 💡 | `--repeat-each=5` 💡 |
| Таймаут теста | `timeOut = 60_000` 💡 | `@Timeout` / `timeout.default` ✅ | `timeout` ✅ / `test.setTimeout` |
| Мягкие проверки | `SoftAssert` 💡 (или AssertJ `SoftAssertions`) | AssertJ `SoftAssertions` 💡 | `expect.soft(...)` 💡 |
| Описание теста | ✅ `@Test(description = "...")` | ✅ `@DisplayName` | ✅ название теста |
| Временная папка | нет — создать вручную ✅ `PlaygroundTest.fileCanBeUploaded` | ✅ `@TempDir` | ✅ `testInfo.outputPath()` |
| Отчёт | ✅ свой HTML: `surefire-reports/index.html`, `emailable-report.html` + JUnit XML | surefire XML | HTML-отчёт ✅ |

### 14.10. Ловушки, найденные при создании модуля

| Ловушка | Что было | Решение (✅ в модуле) |
|---|---|---|
| surefire 3.6 убрал провайдер TestNG | `surefire-testng:3.6.1` не существует; через JUnit Platform TestNG не читает `testng.xml` | плагин и `surefire-testng` зафиксированы на **3.5.6** в `selenide-testng/pom.xml` |
| Свойство `excludedGroups` из родительского `pom.xml` | `-Dsuite=demo-failure` запустил **0 тестов**: surefire добавил исключение группы поверх XML | `<excludedGroups></excludedGroups>` в `selenide-testng/pom.xml` |
| Один экземпляр класса на все методы | поля из JUnit-версии стали бы общими для всех тестов класса | данные теста — в `@BeforeMethod`, хранение — в `ThreadLocal` |
| Хуки пропускаются при фильтре групп | без `alwaysRun` нет пользователя и очистки | `alwaysRun = true` во всех хуках |
| `IAnnotationTransformer` в `@Listeners` не работает | ретраи не включаются | `RetryTransformer` подключён в `<listeners>` XML-файлов |
| Порядок listener'ов и `@AfterMethod` | была гипотеза, что `onTestFailure` срабатывает после закрытия браузера | проверено: `afterInvocation` → `onTestFailure` → `@AfterMethod`; скриншот снимается до закрытия браузера |
| Ретрай выглядит как «пропуск» | первая неудачная попытка в отчёте — SKIPPED | так устроен TestNG; итог считается по последней попытке |

### 14.11. Словарь TestNG ↔ JUnit 5 ↔ Playwright TS

| TestNG | JUnit 5 | Playwright TS |
|---|---|---|
| `@BeforeSuite` | `LauncherSessionListener` / `@BeforeSuite` в `@Suite` | `globalSetup` |
| `@BeforeTest` | — | — |
| `@BeforeGroups` | — | — |
| `@BeforeClass` | `@BeforeAll` (static) | `test.beforeAll` |
| `@BeforeMethod(alwaysRun = true)` | `@BeforeEach` | фикстура / `test.beforeEach` |
| `@AfterMethod(alwaysRun = true)` | `@AfterEach` | teardown фикстуры / `test.afterEach` |
| `@Test(groups = "smoke")` | `@Tag("smoke")` | `{ tag: '@smoke' }` |
| `testng.xml` + `-Dsuite=smoke` | `@Suite` + `-Dtest=SmokeSuite` / `-Dgroups=smoke` | `--grep @smoke` |
| `@DataProvider` | `@ParameterizedTest` | цикл `for` |
| `@Parameters` + `<parameter>` | `-D...` / `framework.properties` | `use` / `process.env` |
| `parallel="classes" thread-count="3"` | `junit-platform.properties` | `workers: 3` |
| `IRetryAnalyzer` | `rerunFailingTestsCount` | `retries` |
| `ITestListener` / `IInvokedMethodListener` | Extension (`TestWatcher`, `AfterTestExecutionCallback`) | reporter / фикстура |
| `SkipException` | `Assumptions.assumeTrue` | `test.skip(cond)` |
| `expectedExceptions` | `assertThrows` | `expect(...).rejects.toThrow()` |
| `testng-failed.xml` | — | `--last-failed` |

---

## 15. Что есть только в TestNG

То, чего нет **ни** в JUnit 5, **ни** в Playwright Test (или есть, но устроено иначе настолько, что переносить
привычку нельзя). Многое из этого — одновременно и возможность, и ловушка.

### 15.1. Возможности

| Что | Как пишется | В репозитории | Ближайшее в JUnit 5 / Playwright TS |
|---|---|---|---|
| Сьют как отдельный XML-файл | `<suite>` → `<test>` → `<groups>` / `<packages>` / `<classes>` / `<methods>` | ✅ `testng/*.xml` (6 файлов) | `@Suite`-класс в Java / `projects` и `--grep` в конфиге |
| Несколько именованных частей прогона в одном сьюте | несколько `<test name="...">`, у каждой свои группы, классы и параметры | ✅ `regression.xml`: «Shop regression» + «Browser features» | — / `projects` (но это разные конфигурации, не части сьюта) |
| Явный список методов в сьюте | `<class name="..."><methods><include name="..."/></methods></class>` | ✅ `critical.xml` | `@SelectMethod` (`junit-platform-suite`) / список файлов в CLI |
| Хук на блок `<test>` | `@BeforeTest` / `@AfterTest` | 💡 | — / — |
| Хук на группу | `@BeforeGroups("smoke")` / `@AfterGroups` — до первого и после последнего теста группы | 💡 | — / — |
| Параметры прогона в файле сьюта | `<parameter name="browser" value="chrome"/>` + `@Parameters({"browser"})` + `@Optional("")` | ✅ `all.xml` → `BaseTest.beforeSuite` | `-D...` / `test.use({...})`, `process.env` |
| Параметры на уровне `<test>` | `<parameter>` внутри `<test>` перекрывает параметр `<suite>` | 💡 | — / опции `projects[].use` |
| Методы одного класса параллельно **на одном экземпляре** | `parallel="methods"` | ✅ `parallel-methods.xml` | `mode.default=concurrent` (но экземпляры разные) / `fullyParallel` (процессы) |
| Параллельно блоки `<test>` или экземпляры | `parallel="tests"` / `parallel="instances"` | 💡 | — / — |
| Отдельный пул для строк `DataProvider` | `@DataProvider(parallel = true)` + `data-provider-thread-count` | ✅ атрибут в `parallel-methods.xml`, `parallel = true` 💡 | — / — |
| Зависимости между тестами | `@Test(dependsOnMethods = "login")`, `dependsOnGroups = "smoke"` — если зависимость упала, тест **SKIPPED** | 💡 | — / `test.describe.serial` (порядок, но не граф) |
| Порядок тестов числом | `@Test(priority = 1)` | 💡 | `@Order` + `@TestMethodOrder` / порядок в файле |
| Повторить тест N раз | `@Test(invocationCount = 5, threadPoolSize = 2)` | 💡 | `@RepeatedTest(5)` (без пула) / `--repeat-each=5` |
| Тест должен бросить исключение | `@Test(expectedExceptions = IllegalStateException.class)` | 💡 | `assertThrows(...)` / `expect(...).toThrow()` |
| Таймаут на весь сьют | `<suite time-out="...">` | 💡 | `junit.jupiter.execution.timeout.default` / `globalTimeout` |
| Ретраи кодом | `IRetryAnalyzer` — решает, повторять ли, по исключению, номеру попытки и т. п. | ✅ `RetryAnalyzer` | `rerunFailingTestsCount` (только число) / `retries` (только число) |
| Изменить аннотации до запуска | `IAnnotationTransformer` — например, повесить `RetryAnalyzer` на все тесты | ✅ `RetryTransformer` | — / — |
| Listener на вызов **любого** метода (тест и хуки) | `IInvokedMethodListener.beforeInvocation` / `afterInvocation` | ✅ `FailureScreenshotListener` | extensions на тест (`AfterTestExecutionCallback`) / фикстуры |
| Listener на конфигурационные методы | `IConfigurationListener` (упал `@BeforeMethod` и т. п.) | 💡 | — / — |
| Пропуск по условию | `throw new SkipException("why")` → SKIPPED | ✅ `JavaScriptAndScreenshotsTest` | `assumeTrue(...)` → aborted / `test.skip(cond)` |
| Описание теста | `@Test(description = "...")` | ✅ `LoginTest`, `CheckoutTest` | `@DisplayName` / имя `test(...)` |
| Сьют «только упавшие» | после прогона появляется `testng-failed.xml` — его можно запустить как обычный сьют | ✅ создаётся в `target/surefire-reports/` | — / `--last-failed` |
| Свой HTML-отчёт из коробки | `index.html`, `emailable-report.html` | ✅ `target/surefire-reports/` | — (XML surefire) / HTML-отчёт Playwright |
| Сообщения в отчёт | `Reporter.log("...")` — строка в HTML-отчёте у теста | 💡 | — / `testInfo.annotations`, `test.step` |
| Фабрика экземпляров | `@Factory` — создать класс несколько раз с разными аргументами конструктора | 💡 | `@ParameterizedClass` (JUnit 5.13+) / — |

### 15.2. Особенности, о которых нужно помнить

| Что | Чем грозит | Как сделано здесь |
|---|---|---|
| **Один экземпляр класса на все его методы** | поле, инициализированное в объявлении, общее для всех тестов класса | состояние теста создаётся в `@BeforeMethod` (`BaseTest.createTestUser`) |
| …и `parallel="methods"` на этом экземпляре | обычные поля перезаписывает соседний поток | `ThreadLocal<TestUser>`, `ThreadLocal<BackendApi>` + `remove()` в `@AfterMethod` |
| **`alwaysRun = true`** | без него фильтр групп в XML пропускает `@BeforeMethod`/`@AfterMethod` без группы — тест идёт без пользователя и без очистки; `@AfterMethod` без `alwaysRun` пропускается и после упавшего `@BeforeMethod` | `alwaysRun = true` на всех хуках `BaseTest` и `PlaygroundTest` |
| Хуки базового класса — до хуков наследника | так и задумано, но при двух `@BeforeMethod` в одном классе порядок не гарантирован | по одному `@BeforeMethod` на класс |
| `IAnnotationTransformer` работает только из XML / ServiceLoader / `-listener` | в `@Listeners` на классе он молча игнорируется — ретраев нет | `RetryTransformer` подключён в `<listeners>` XML-файлов |
| Повторная попытка ретрая | неудачная попытка в отчёте — **SKIPPED**, а не FAILED; итог — по последней | видно на `-Dsuite=demo-failure -Dtest.retries=1` |
| Порядок listener'ов при падении | `afterInvocation` → `IRetryAnalyzer.retry` → `onTestFailure` / `onTestSkipped` → `@AfterMethod` — скриншот надо снимать в `afterInvocation`, пока браузер жив | проверено на `FailureDemoTest`, описано в javadoc `FailureScreenshotListener` |
| `@BeforeSuite` в базовом классе | наследуют все классы, но выполняется **один раз** на `<suite>` | `BaseTest.beforeSuite` |
| Параметр из XML отсутствует | `@Parameters` без `@Optional` роняет запуск `-Dtest=CartTest` (там нет XML) | `@Optional("")` + приоритет `-D` → XML → файл в `Config` |
| `@Test` на классе | делает тестом **каждый** `public`-метод класса, в том числе хелперы | хелперы `private` (`PlaygroundTest.playground()`) |
| Maven Surefire 3.6+ | нет провайдера TestNG, читающего `testng.xml`: тесты идут через JUnit Platform, XML игнорируется | плагин и `surefire-testng` закреплены на **3.5.6** |
| Свойство `excludedGroups` из родительского `pom.xml` | surefire добавляет его к **любому** XML: `demo-failure.xml` запускал 0 тестов | в модуле `<excludedGroups></excludedGroups>` |
| `-Dtest` вместе с XML | `-Dtest` заменяет XML целиком (хуки `@BeforeSuite` работают, `<parameter>` и `<listeners>` из XML — нет) | `@Optional` в `beforeSuite`; ретраи без XML не подключаются |
