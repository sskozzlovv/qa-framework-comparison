# Сравнение: Selenium vs Selenide vs Playwright Java vs Playwright TS

Все примеры — из кода этого репозитория. Три Java-модуля используют одинаковый раннер (JUnit 5), поэтому их
различия — это различия библиотек. `playwright-ts` — типичный проект на Playwright Test: там различается уже
**всё**: раннер, конфигурация, способ организовать фреймворк.

## 0. Один и тот же тест четыре раза

Сценарий: залогиниться, положить два товара, оформить заказ, дождаться статуса COMPLETED (страница статус сама
не обновляет — нужно жать Refresh). Показан код теста **и** то, что нужно, чтобы он заработал.

Объём «инфраструктуры» ниже — это строки в `core/`, `api/` и `BaseTest` (вместе с комментариями), без page
objects и тестов.

### Selenium — тест + ~690 строк своей инфраструктуры

```java
@ExtendWith(BrowserExtension.class)              // своё: старт/quit драйвера, скриншот при падении
class CheckoutTest extends BaseTest {            // своё: BackendApi, cleanup в @AfterEach

    @Test @Critical
    void userBuysProductsAndOrderGetsCompleted() {
        CatalogPage catalog = loginViaApi().addToCart(BACKPACK).addToCart(CAMP_MUG);
        catalog.header().waitCartCount(2);
        CheckoutPage checkout = catalog.header().openCart().checkout();
        assertThat(checkout.isPlaceOrderEnabled()).isFalse();

        String orderId = checkout.fill("Ada", "Lovelace", "12345")
                .placeOrder().waitProgressAtLeast(50).waitForConfirmation();

        OrdersPage orders = checkout.viewOrders();
        orders.refreshUntilStatus(orderId, "COMPLETED", Duration.ofSeconds(20));
        assertThat(api.orders(user)).isEqualTo(1);
    }
}
```

Тест выглядит прилично, но только потому, что вся работа спрятана в page objects: каждый метод сам ждёт
(`waits.click`, `textToBe`, `stalenessOf`, свой `WebDriverWait` с кликом внутри для Refresh). Плюс `core/`:
`DriverFactory`, `Browser` (ThreadLocal), `BrowserExtension`, `FailureArtifacts`, `Waits`, `Js`, `GlobalLifecycle`.

### Selenide — тест + ~270 строк инфраструктуры

```java
@ExtendWith({SelenideSetup.class, ScreenShooterExtension.class})
class CheckoutTest extends BaseTest {

    @Test @Critical
    void userBuysProductsAndOrderGetsCompleted() {
        CatalogPage catalog = loginViaApi().addToCart(BACKPACK).addToCart(CAMP_MUG);
        catalog.header().cartBadge().shouldHave(exactText("2"));
        CheckoutPage checkout = catalog.header().openCart().checkout();
        checkout.placeOrderButton().shouldBe(disabled);

        String orderId = checkout.fill("Ada", "Lovelace", "12345")
                .placeOrder().waitProgressAtLeastHalf().waitForConfirmation();

        OrdersPage orders = checkout.viewOrders();
        orders.refreshUntilStatus(orderId, "COMPLETED", Duration.ofSeconds(20));
        assertThat(api.orders(user)).isEqualTo(1);
    }
}
```

Проверки (`shouldHave`, `shouldBe`) сами ждут — page objects стали тоньше. Инфраструктура: настройка
`Configuration`, закрытие браузера, вызов API через браузер.

### Playwright Java — тест + ~400 строк инфраструктуры

```java
@ExtendWith(PlaywrightExtension.class)           // своё: Browser на поток, Context на тест, trace при падении
class CheckoutTest extends BaseTest {

    @Test @Critical
    void userBuysProductsAndOrderGetsCompleted() {
        CatalogPage catalog = loginViaApi().addToCart(BACKPACK).addToCart(CAMP_MUG);
        assertThat(catalog.header().cartBadge()).hasText("2");
        CheckoutPage checkout = catalog.header().openCart().checkout();
        assertThat(checkout.placeOrderButton()).isDisabled();

        String orderId = checkout.fill("Ada", "Lovelace", "12345")
                .placeOrder().waitProgressAtLeastHalf().waitForConfirmation();

        OrdersPage orders = checkout.viewOrders();
        orders.refreshUntilStatus(orderId, "COMPLETED", Duration.ofSeconds(20)); // цикл while с дедлайном
        org.assertj.core.api.Assertions.assertThat(api.orders(user)).isEqualTo(1);
    }
}
```

Почти Selenide по форме. Но фикстуры, trace, работу с браузерами по потокам пришлось написать самому
(`PlaywrightExtension`, `BrowserPerThread`, `GlobalLifecycle`), а `toPass` в Java нет.

### Playwright TS — тест + ~220 строк, из них почти половина — конфиг

```ts
test('user buys products and the order gets completed', { tag: '@critical' },
  async ({ catalogPage, cartPage, checkoutPage, ordersPage, api, user }) => {
    await test.step('put two products into the cart', async () => {
      await catalogPage.goto();
      await catalogPage.addToCart(products.backpack, products.campMug);
      await expect(catalogPage.header.cartBadge).toHaveText('2');
    });

    const orderId = await test.step('check out', async () => {
      await catalogPage.header.cartLink.click();
      await cartPage.checkoutButton.click();
      await expect(checkoutPage.placeOrderButton).toBeDisabled();
      await checkoutPage.fill(customer);
      return checkoutPage.placeOrder();
    });

    await test.step('order becomes COMPLETED', async () => {
      await checkoutPage.viewOrdersLink.click();
      await ordersPage.waitForStatus(orderId, 'COMPLETED');   // expect(...).toPass() внутри
    });

    await expect.poll(() => api.state(user)).toEqual({ cartItems: 0, orders: 1 });
  });
```

Что здесь не видно, и в этом суть:
- **нет логина** в тесте — браузерный контекст создаётся уже залогиненным (фикстура подменяет `storageState`);
- **нет cleanup** — он в teardown фикстуры `api`, после `await use(...)`;
- **нет `BaseTest`, extension'ов, скриншотов, trace, ретраев, запуска приложения** — это строки в
  `playwright.config.ts`;
- page objects просто держат локаторы, потому что любой `expect(locator)` ждёт сам.

Вся «инфраструктура» `playwright-ts` — это `playwright.config.ts` (78 строк), `support/fixtures.ts` (81),
`support/api-client.ts` (52) и `global-setup.ts` (12). По строкам это близко к Selenide. Разница в другом: в
Selenide этот код настраивает библиотеку, а здесь почти всё — декларативные опции раннера и фикстуры
приложения (логин, данные, cleanup).

### Тот же сценарий в TS, но с привычками из Selenium

В [tests/examples/java-style-vs-idiomatic.spec.ts](playwright-ts/tests/examples/java-style-vs-idiomatic.spec.ts) один
сценарий написан двумя способами. Первый тест зелёный, но в нём типичные ошибки команды, которая перешла с
WebDriver:

| Привычка из Selenium | Что не так в Playwright | Как правильно |
|---|---|---|
| UI-логин в `beforeEach` | секунды на тест, все тесты зависят от формы логина | фикстура со `storageState` |
| `let user` + `beforeEach`/`afterEach` | общий изменяемый стейт, порядок хуков | фикстуры `user`, `api` с teardown |
| `page.fill('#id')`, `page.click('#id')` | устаревший API, CSS-идентификаторы | локаторы `getByRole`/`getByLabel` |
| `waitForTimeout(1500)` | sleep: медленно и всё равно флейки | `await expect(...).toHaveText(...)` |
| `const t = await textContent(); expect(t).toBe(...)` | одно чтение, без повторов | web-first assertion `toHaveText` |
| `waitForSelector` перед `click` | действие и так ждёт | убрать |
| `waitForLoadState('networkidle')` | «500 мс тишины» — не состояние приложения | ждать конкретный ответ или элемент |
| cleanup через `page.evaluate(fetch)` | ломается, если страница упала или заблокирована диалогом | `APIRequestContext` в фикстуре |
| `for` + `sleep` для опроса статуса | ручной polling | `expect(async () => {...}).toPass()` |

---

## 1. Коротко

| Тема | Selenium 4 | Selenide 7 | Playwright Java | Playwright TS |
|---|---|---|---|---|
| Раннер | JUnit 5 | JUnit 5 | JUnit 5 | **Playwright Test** (свой) |
| Конфигурация | свой `framework.properties` + код | `Configuration.*` | свой код + properties | `playwright.config.ts` |
| Управление браузером | вы: фабрика, `ThreadLocal`, `quit()` | Selenide, ленивый старт | вы: Browser на поток, Context на тест | раннер: worker = браузер, тест = контекст |
| Подготовка теста | `BaseTest` + `@BeforeEach` | то же | то же + параметры из extension | **фикстуры** с зависимостями и teardown |
| Ожидания | вручную, `WebDriverWait` | `should*()` ждут сами | auto-wait + `assertThat(locator)` | то же + `toPass`, `expect.poll` |
| Элемент | `WebElement` → **stale** | ленивый `SelenideElement` | ленивый `Locator` | ленивый `Locator` |
| Клик под оверлеем | исключение сразу | повторяет до таймаута | ждёт actionability | ждёт actionability |
| JS | `executeScript`, async через callback | `executeJavaScript` | `evaluate("arg => ...")` строкой | `evaluate(arg => ...)` — **настоящая функция**, типизирована |
| Логин без UI | localStorage после открытия origin | то же | `addInitScript` | **`storageState`** контекста |
| API-клиент | нет (fetch в браузере) | нет (то же) | `APIRequestContext` | `APIRequestContext` |
| Артефакты падения | свой код | авто на `should*()` | свой код | **конфиг**: `trace`, `screenshot`, `video` |
| Отчёт | surefire XML | surefire + selenide-reports | surefire XML | **HTML-отчёт** с trace, + junit.xml |
| Ретраи | surefire `rerunFailingTestsCount` | то же | то же | `retries` + статус flaky |
| Визуальное сравнение | нет | нет | нет (только скриншоты) | **`toHaveScreenshot`** с эталонами |
| Мок сети | CDP/BiDi или прокси | прокси | `page.route` | `page.route`, `route.fulfill({ json })` |
| Запуск приложения | свой код | свой код | свой код | `webServer` в конфиге |
| Несколько браузеров за прогон | разные запуски | разные запуски | разные запуски | `projects` |
| Отладка | IDE-брейкпоинты | IDE | IDE, Inspector | **UI mode**, `--debug`, codegen, trace viewer |
| Браузеры | любые с драйвером, Grid, Appium | как Selenium | Chromium, Firefox, WebKit + Chrome/Edge | как Playwright Java |

---

## 2. Жизненный цикл и «before suite»

| | Один раз на запуск | На каждый тест | Изоляция |
|---|---|---|---|
| Selenium | `LauncherSessionListener` (старт приложения), `@BeforeSuite` в `@Suite`-классах | extension: новый `ChromeDriver` → `quit()` | новый браузер (~1–2 с) или ручная чистка |
| Selenide | то же + там же настройка `Configuration` (глобальная статика!) | `closeWebDriver()` в extension | новый браузер или `clearBrowserLocalStorage()` |
| Playwright Java | то же + закрытие всех Playwright в конце | extension: `browser.newContext()` → `close()` | новый контекст ~10 мс |
| Playwright TS | `webServer` (старт приложения), `globalSetup` (сброс данных), можно setup-проект | встроенные фикстуры `context`/`page` | новый контекст ~10 мс |

**Selenium** — всё руками ([BrowserExtension.java](selenium/src/test/java/com/compare/selenium/core/BrowserExtension.java)):

```java
private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>(); // параллельные тесты = свой драйвер на поток

public void beforeEach(ExtensionContext c) { if (!Browser.isStarted()) Browser.start(); }
public void afterEach(ExtensionContext c) {
    if (Config.reuseBrowser()) Browser.resetState();   // закрыть лишние окна, localStorage.clear(), deleteAllCookies()
    else Browser.quit();                               // чистый браузер на каждый тест
}
```

Ловушка: `localStorage` принадлежит origin'у, поэтому для очистки сначала нужно открыть страницу приложения
(`/blank.html`). На `about:blank` вы очистите не то хранилище.

**Selenide** ([SelenideSetup.java](selenide/src/test/java/com/compare/selenide/core/SelenideSetup.java)): браузер
стартует сам при первом `open()`, на каждый поток свой. Ловушка: `Configuration` — глобальная статика, писать
в неё из параллельных потоков нельзя, поэтому она настраивается один раз до старта тестов.

**Playwright Java** ([PlaywrightExtension.java](playwright/src/test/java/com/compare/playwright/core/PlaywrightExtension.java)):
`Browser` живёт весь прогон (один на поток — объекты Playwright не потокобезопасны), а изоляцию даёт
`BrowserContext`. `Page` и `ApiClient` внедряются параметрами — это самодельные «фикстуры».

**Playwright TS** ([fixtures.ts](playwright-ts/support/fixtures.ts)): всё перечисленное делает раннер. Свой код —
только то, что относится к приложению:

```ts
export const test = base.extend<Options & Fixtures>({
  userKind: ['normal', { option: true }],            // test.use({ userKind: 'slow' }) в describe

  user: async ({ userKind }, use) => { await use(uniqueUser(userKind)); },

  api: async ({ playwright, baseURL, user }, use) => {
    const request = await playwright.request.newContext({ baseURL });
    const api = new ApiClient(request);
    await use(api);                                   // ← тест выполняется здесь
    await api.cleanup(user);                          // teardown, даже если тест упал
    await request.dispose();
  },

  // переопределение встроенной опции: контекст СОЗДАЁТСЯ уже залогиненным
  storageState: async ({ authenticated, api, user, baseURL }, use) => {
    const token = await api.login(user);
    await use({ cookies: [], origins: [{ origin: baseURL!, localStorage: [{ name: 'token', value: token }, ...] }] });
  },
  catalogPage: async ({ page }, use) => use(new CatalogPage(page)),
});
```

Фикстуры создаются **только если тест их запросил** и в порядке зависимостей. Тест, которому нужен только
`loginPage`, не будет логиниться через API.

## 3. Сьюты, группы, параметризация

| | Java (все три) | Playwright TS |
|---|---|---|
| Тег | `@Smoke` = `@Tag("smoke")` | `test('...', { tag: '@smoke' }, ...)`, на `describe` тоже |
| Запуск по тегу | `-Dgroups="smoke \| critical"` | `--grep "@smoke\|@critical"`, `--grep-invert` |
| Исключение по умолчанию | surefire `excludedGroups=demo-failure` | `grepInvert: /@demo-failure/` в конфиге |
| Сьют | `@Suite @SelectPackages @IncludeTags` + `@BeforeSuite` | npm-скрипт / `projects` с `grep` / папка `testDir` |
| Параметризация | `@ParameterizedTest @CsvSource` | обычный `for` вокруг `test(...)` |
| Опции на группу тестов | наследование / поля | `test.use({ userKind: 'slow' })` в `describe` |
| Параллельность | классы параллельно, методы последовательно; `max-pool-size`, иначе лишние браузеры | `fullyParallel: true` — параллельно всё, `workers: 3` |
| Ретраи | `rerunFailingTestsCount` | `retries`, в отчёте статус **flaky** |
| `test.only` забыли в коде | — | `forbidOnly: !!process.env.CI` роняет CI |

## 4. Ожидания — главное отличие

| Ситуация | Selenium | Selenide | Playwright Java / TS |
|---|---|---|---|
| Страница готова: fetch завершены, спиннер скрыт | `until(js "readyState && __pending===0")` + `invisibilityOfElementLocated` | `Wait().until(js)` + `should(disappear)` | `waitForFunction(...)` + `toBeHidden()` |
| Поиск с debounce 400 мс | запомнить старую карточку → `stalenessOf` → спиннер | ничего: `names().shouldHave(exactTexts(...))` повторяется | `waitForResponse(...)` вокруг `fill` |
| «Added» на кнопке | `textToBe` | `shouldHave(exactText)` | `toHaveText` |
| Бейдж обновляется позже | `textToBe(badge, "2")` | `shouldHave(exactText("2"))` | `toHaveText('2')` |
| Тосты исчезли | `until(findElements().isEmpty())` | `shouldHave(size(0))` | `toHaveCount(0)` |
| Строка удаляется с анимацией | `invisibilityOfElementLocated` | `should(disappear)` | `waitFor({ state: 'detached' })` |
| Прогресс ≥ 50% | лямбда с `getDomAttribute` | `attributeMatching(..., regex)` | `toHaveAttribute(..., /regex/)` |
| Долгая операция | `until(..., 20 s)` | `shouldBe(visible, 20 s)` | `toBeVisible({ timeout: 20_000 })` |
| «Жми Refresh, пока не COMPLETED» | `WebDriverWait` с кликом внутри | `Wait().pollingEvery(1s)` + `has()` | Java: цикл с дедлайном; **TS: `expect(async () => {...}).toPass()`** |
| Проверка бэкенда, пока не совпадёт | `until(...)` | `Wait().until(...)` | Java: цикл; **TS: `expect.poll(() => api.state(user))`** |
| Alert через 0.5 с | `until(alertIsPresent())` | `Selenide.confirm()` ждёт сам | обработчик/`waitForEvent('dialog')` **до** клика |

Ловушки, которые видны только в Selenium (и частично в Selenide):

- **Не смешивать implicit и explicit waits.** С implicit wait каждый `findElement` внутри `ExpectedConditions`
  блокируется, и «дождаться невидимости» может занять timeout × timeout. В `DriverFactory` implicit wait = 0.
- **«Спиннер исчез» ≠ «данные обновились».** После ввода в поиск спиннер появляется только через 400 мс;
  проверка «спиннера нет» проходит сразу, и тест читает старый список.
- **`elementToBeClickable` не знает про оверлеи** — проверяет только displayed + enabled. Поэтому
  `Waits.click()` повторяет find+click, пока клик перехватывается.
- **`getText()` возвращает только видимый текст, а `opacity: 0` считается невидимым.** Карточки появляются с
  fade-in 0.35 с; при полном прогоне тест сортировки однажды прочитал цену `""`. Исправление — дождаться, пока
  все карточки `isDisplayed()`. Selenide наследует это (`texts()` = `getText()`). У Playwright
  `textContent()` видимость не учитывает.

## 5. Stale element и перехват кликов

```java
// Selenium: WebElement указывает на конкретный DOM-узел
WebElement firstCard = catalog.firstCard();
catalog.search("mug");                                   // список перерисован
assertThatThrownBy(firstCard::getText).isInstanceOf(StaleElementReferenceException.class);
```
```java
firstCard.shouldHave(text("Steel Bottle"));              // Selenide: тот же объект находит новый узел
```
```ts
await expect(firstCard).toContainText('Steel Bottle');  // Playwright: то же
```

Клик, когда сверху промо-модалка:

| | Поведение | Ошибка |
|---|---|---|
| Selenium `click()` | падает сразу | `ElementClickInterceptedException: ... Other element would receive the click` |
| Selenide `click()` | повторяет до `Configuration.timeout` | то же внутри `UIAssertionError` |
| Playwright | ждёт actionability до таймаута | `TimeoutError: ... <div id="promo-modal"> intercepts pointer events` |

JS-клик (`arguments[0].click()`, `click(usingJavaScript())`, `dispatchEvent('click')`) проходит сквозь модалку —
тест зелёный там, где пользователь кликнуть не может. Это антипаттерн, тесты показывают почему.

## 6. Выполнение JavaScript

| | Selenium | Selenide | Playwright Java | Playwright TS |
|---|---|---|---|---|
| Синхронно | `executeScript("return arguments[0] + 1", 41)` | `executeJavaScript(...)` | `evaluate("x => x + 1", 41)` — строка | `evaluate(x => x + 1, 41)` — **функция в коде**, IDE и типы работают |
| Асинхронно | `executeAsyncScript`: вызвать callback до `scriptTimeout` | `executeAsyncJavaScript` | `evaluate("async () => ...")` | `evaluate(async () => ...)` |
| Число 42 | `Long` | `Long` | `Integer` | `number` |
| Объект | `Map` | `Map` | `Map` | типизированный объект |

В TS разница принципиальная: тело `evaluate` — это обычная TypeScript-функция рядом с тестом (с проверкой
типов и автодополнением), которую Playwright сериализует в браузер. В Java это всегда строка.

**Вызов `/api/test/cleanup`:**

Selenium и Selenide делают `fetch()` внутри браузера через `executeAsyncScript`. Ловушки: браузер должен стоять
на origin приложения; открытый alert блокирует скрипт; если браузер упал, очистки не будет; `@AfterEach`
выполняется до `AfterEachCallback`, поэтому браузер в этот момент ещё жив.

Playwright Java и TS используют отдельный `APIRequestContext` — страница не нужна. В TS это ещё и teardown
фикстуры, т.е. в тестах cleanup не виден вообще.

**Логин без UI:** Selenium/Selenide — открыть origin, записать localStorage. Playwright Java — `addInitScript`
(срабатывает на каждой навигации, нужен guard). Playwright TS — `storageState`: контекст сразу создаётся с
нужным localStorage/cookies, это официальный способ.

## 7. Скриншоты, артефакты, отчёты

Порядок JUnit 5 для одного теста (важно для трёх Java-модулей):

```
BeforeEachCallback → @BeforeEach → test → AfterTestExecutionCallback → @AfterEach → AfterEachCallback → TestWatcher
                                          ↑ скриншот здесь: браузер жив       ↑ браузер закрывается        ↑ уже поздно
```

| | При падении | Как настроено |
|---|---|---|
| Selenium | `screenshot.png`, `page.html`, `console.log`, `info.txt` (URL, текст открытого alert) | свой `FailureArtifacts` (перед скриншотом закрыть alert, иначе `UnhandledAlertException`) |
| Selenide | `.png` + `.html`, пути прямо в тексте ошибки | автоматически; `ScreenShooterExtension` для не-Selenide ошибок |
| Playwright Java | `screenshot.png`, `page.html`, `trace.zip` | свой код в extension |
| Playwright TS | скриншот, **видео**, `trace.zip`, `error-context.md` (снимок a11y-дерева), всё внутри HTML-отчёта | три строки в конфиге: `trace`, `screenshot`, `video: 'retain-on-failure'` |

| | Viewport | Элемент | Вся страница | Маскирование | Визуальная регрессия |
|---|---|---|---|---|---|
| Selenium | `getScreenshotAs` | `element.getScreenshotAs` | Firefox: `getFullPageScreenshotAs`; Chrome: CDP | нет | нет |
| Selenide | `screenshot("name")` | `$.screenshot()` | плагин или CDP | нет | нет |
| Playwright Java | `page.screenshot()` | `locator.screenshot()` | `setFullPage(true)` | `setMask(...)` | нет |
| Playwright TS | `page.screenshot()` | `locator.screenshot()` | `fullPage: true` | `mask: [...]` | **`expect(page).toHaveScreenshot()`** с эталонами в репозитории |

Визуальные эталоны (`*-snapshots/*-chrome-win32.png`) зависят от браузера **и ОС**: шрифты рендерятся по-разному.
Генерировать их нужно там же, где бежит CI (обычно в Docker-образе Playwright).

## 8. Браузерные возможности

```java
// iframe
driver.switchTo().frame(...); try { ... } finally { driver.switchTo().defaultContent(); }   // Selenium
switchTo().frame($("#support-frame")); ... switchTo().defaultContent();                       // Selenide
page.frameLocator("#support-frame").getByLabel("Message to support").fill(msg);              // Playwright Java
```
```ts
await page.frameLocator('#support-frame').getByLabel('Message to support').fill(msg);       // Playwright TS
```

| | Shadow DOM | Новая вкладка | Download | Upload |
|---|---|---|---|---|
| Selenium | `getShadowRoot()`, только CSS | перебор `getWindowHandles()` | prefs браузера + опрос папки | `sendKeys(path)` (+ `LocalFileDetector` на Grid) |
| Selenide | `$(shadowCss("#code", "#promo"))` | `switchTo().window(1)` | `$.download(...)` | `uploadFile(file)` |
| Playwright Java | обычные локаторы проходят shadow root | `context.waitForPage(() -> click)` | `page.waitForDownload(() -> click)` | `setInputFiles(path)` |
| Playwright TS | то же | `const p = page.waitForEvent('popup'); await click; await p` | `waitForEvent('download')` + `testInfo.outputPath()` | `setInputFiles({ name, buffer })` — **из памяти, без файла** |

Диалоги: в Selenium по умолчанию `unhandledPromptBehavior = dismiss and notify` — неожиданный alert закрывается,
а падает **следующая** команда (в фреймворке выставлено `IGNORE`). В Playwright без обработчика диалог
автоматически закрывается, и `confirm()` молча возвращает `false` — тест проверяет не ту ветку.

## 9. Сеть

```ts
await page.route('**/api/products*', route => route.fulfill({ status: 500, json: { error: 'Database is down' } }));
```

Есть в обоих Playwright (в TS `json:` вместо ручной сериализации). В Selenium — CDP `NetworkInterceptor`
(привязан к версии Chrome) или WebDriver BiDi; в Selenide — встроенный прокси.

## 10. Локаторы

| | Стиль |
|---|---|
| Selenium | `By.id`, `By.cssSelector`, XPath для поиска карточки по тексту: `//div[contains(@class,'product-card')][.//h3[normalize-space()='Camp Mug']]//button` |
| Selenide | `$("#id")`, `$$(".product-card").findBy(text("Camp Mug")).$("button")` |
| Playwright Java / TS | `getByLabel('Username')`, `getByRole('button', { name: 'Log in' })`, `getByTestId('login-error')`, `cards.filter({ hasText: 'Camp Mug' }).getByRole('button')` |

Ловушка Playwright: имя в `getByRole` сравнивается как **подстрока** без учёта регистра. Ссылка `Orders` в шапке
совпала бы со ссылкой «View my orders» на странице заказа → strict mode violation. Поэтому в шапке — `#nav-orders`
(или `exact: true`).

Page objects: в Selenium хранят `By` (не `WebElement` — протухнет) и обязаны ждать сами; в Selenide и Playwright
хранят ленивые элементы, а ждут проверки в тестах. В TS page object обычно ещё тоньше: `readonly` локаторы +
несколько составных действий, и создаётся фикстурой.

## 11. Скорость

Одна машина, headless Chrome, 3 потока/воркера, все искусственные задержки приложения включены:

| | Тестов | Время |
|---|---|---|
| Selenium | 35 | 1 мин 10 с |
| Selenide | 35 | 1 мин 01 с |
| Playwright Java | 36 | 47 с |
| Playwright TS | 40 | 56 с (вместе со стартом приложения через `webServer` и медленным тестом из `examples/`) |

Playwright быстрее за счёт модели «браузер на поток + контекст на тест». Selenium/Selenide с
`browser.reuse=false` запускают Chrome на каждый тест.

## 12. Что выбрать

- **Selenium** — когда нужен стандарт W3C WebDriver: Selenium Grid, облачные фермы, реальные мобильные
  устройства (Appium), редкие браузеры. Цена — свой слой ожиданий, управления драйвером и артефактов
  (весь `core/` этого модуля).
- **Selenide** — Java-команда и Selenium-инфраструктура: тот же WebDriver, но ожидания, ленивые элементы,
  скриншоты, файлы и диалоги уже решены. Самый короткий Java-код из трёх.
- **Playwright Java** — Java-команда, которой нужны преимущества Playwright (actionability, контексты, сеть,
  WebKit) без перехода на Node. Но раннер остаётся JUnit, и фикстуры, trace и отчёты придётся собирать самим.
- **Playwright TS** — если команда готова к TypeScript, это самый «полный» вариант: раннер, фикстуры, отчёт,
  trace viewer, UI mode, визуальные сравнения, `projects`, `webServer` — из коробки. Фреймворк сводится к конфигу,
  фикстурам и page objects.
