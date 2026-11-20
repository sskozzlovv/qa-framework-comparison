# Selenium vs Selenide vs Playwright (Java) vs Playwright (TypeScript)

Четыре UI-фреймворка с одинаковыми тестовыми сценариями против одного и того же приложения:

- три на **Java 21 + JUnit 5 + Maven** — раннер, теги, сьюты и AssertJ одинаковые, разница в коде = разница
  **библиотек**: как они ждут, кликают, исполняют JS, снимают скриншоты и управляют браузером;
- один — **типичный боевой проект на Playwright Test (TypeScript)**, без оглядки на Java: фикстуры, конфиг,
  `webServer`, `storageState`, теги, trace/видео, HTML-отчёт, визуальные снапшоты.

Подробное сравнение по темам: **[COMPARISON.md](COMPARISON.md)**.
Все команды запуска одной шпаргалкой и как войти в тестовое приложение: **[RUNNING.md](RUNNING.md)**.
Коротко по шагам — приложение, Selenide и Playwright TS: **[QUICKSTART.md](QUICKSTART.md)**.
Selenide ↔ Playwright TS построчно (хуки, BaseTest и фикстуры, конфиги, клики, ожидания): **[SELENIDE_VS_PLAYWRIGHT_TS.md](SELENIDE_VS_PLAYWRIGHT_TS.md)**.
Один отчёт Allure для Selenide (JUnit 5 и TestNG) и Playwright TS: `npm ci`, потом `npm run allure2:open` (классический) или `npm run allure3:open` (новый) из корня, подробнее — [RUNNING.md](RUNNING.md#6-allure-один-отчёт-для-selenide-junit-5-testng-и-playwright-ts).

```
qaAutomation/
├── demo-app/       Sandbox Shop: тестируемое приложение (JDK HttpServer)
├── selenium/       Framework 1: чистый Selenium WebDriver 4.51
├── selenide/       Framework 2: Selenide 7.19 (JUnit 5)
├── selenide-testng/ Framework 2b: те же Selenide-тесты на TestNG 7.12 (testng.xml, группы, DataProvider)
├── playwright/     Framework 3: Playwright for Java 1.64
└── playwright-ts/  Framework 4: Playwright Test 1.64 (TypeScript, Node.js)
```

## Почему своё приложение, а не saucedemo.com

На saucedemo нет ничего асинхронного: ни спиннеров, ни API, ни тестовых хуков для очистки данных. Sandbox Shop
сделан **намеренно неудобным для автоматизации**, чтобы на нём было что решать:

| Ловушка | Где | Чему учит |
|---|---|---|
| Случайная задержка 150–500 мс у каждого API-вызова, `slow_*` пользователи ×4 | везде | нельзя «подогнать» sleep, только ожидания по условию |
| Полноэкранный оверлей-спиннер, перехватывающий клики | все страницы | `ElementClickInterceptedException`, ожидание исчезновения |
| Поиск с debounce 400 мс, список полностью перерисовывается | каталог | stale elements; «спиннер исчез» ≠ «данные обновились» |
| Промо-модалка появляется в случайный момент 0.3–1.2 с | каталог | недетерминированный перехват кликов, отключение через localStorage |
| Кнопка «Adding…» → «Added», бейдж корзины обновляется отдельным запросом позже | каталог | ждать результат действия, а не само действие |
| Тосты, которые исчезают через ~2.4 с, могут накладываться | каталог | ожидание исчезновения коллекции |
| Удаление строки с CSS-анимацией 0.5 с | корзина | opacity ≠ invisibility, ждать удаления из DOM |
| Кнопка disabled до валидной формы, ошибки только после blur | checkout | состояние элементов, TAB/blur |
| Прогресс-бар `aria-valuenow` 0→100 | checkout | ожидание по атрибуту |
| Статус заказа PROCESSING→COMPLETED через 5 с на сервере, страница не обновляется сама | orders | «повторяй действие, пока не выполнится условие» |
| Пагинация «Load more» | orders | ожидание роста количества строк |
| confirm/prompt открываются через 0.5 с после клика | playground | ожидание alert / обработчик диалога |
| iframe, open shadow DOM, новая вкладка, upload, download, lazy-контент по скроллу | playground | браузерные API каждого инструмента |
| `window.__pending` — счётчик активных fetch | app.js | testability hook для «network idle» |

**Пользователи:** любой логин `[a-zA-Z0-9_.-]{3,40}` с паролем `secret` — так каждый тест создаёт **своего**
пользователя (изоляция данных при параллельном запуске). `locked_*` — заблокирован, `slow_*` — в 4 раза медленнее.

**Тестовое API** (заголовок `X-Test-Key: let-me-in`):

| Метод | Что делает |
|---|---|
| `POST /api/test/cleanup {username}` | удаляет корзину и все заказы пользователя (вызывается в `@AfterEach`) |
| `POST /api/test/seed-orders {username, count}` | создаёт N завершённых заказов (тестовые данные для пагинации) |
| `GET /api/test/state?username=` | `{cartItems, orders}` — проверка состояния бэкенда из теста |
| `POST /api/test/reset` | очищает всё (вызывается в `@BeforeSuite` сьютов) |

Приложение можно запустить отдельно и посмотреть руками:

```bash
./mvnw -pl demo-app install -DskipTests
```

```bash
java -cp "demo-app/target/classes;$HOME/.m2/repository/com/google/code/gson/gson/2.13.1/gson-2.13.1.jar" com.sandbox.app.DemoApp 8080
```

и открыть http://127.0.0.1:8080 (на Linux/macOS разделитель classpath — `:`).

## Запуск

Нужны JDK 21+ и установленный Google Chrome. Maven ставить не нужно — есть wrapper.

Все три Java-фреймворка (приложение стартует само на случайном порту):

```bash
./mvnw test
```

Один фреймворк:

```bash
./mvnw -pl playwright -am test
```

По тегу (`smoke`, `regression`, `critical`, `browser-api`), выражения тоже работают:

```bash
./mvnw -pl selenide -am test -Dgroups="smoke | critical"
```

Сьют (`SmokeSuite`, `CriticalSuite`, `FullRegressionSuite`) с `@BeforeSuite`/`@AfterSuite`:

```bash
./mvnw -pl selenium -am test -Dtest=SmokeSuite -Dsurefire.failIfNoSpecifiedTests=false
```

Тест, который падает специально — посмотреть артефакты падения (скриншот, HTML, console, trace.zip):

```bash
./mvnw -pl playwright -am test -Dgroups=demo-failure -DexcludedGroups= -Dsurefire.failIfNoSpecifiedTests=false
```

Полезные переключатели:

| Свойство | По умолчанию | Значение |
|---|---|---|
| `-Dheadless=false` | `true` | смотреть, как работает браузер |
| `-Dbrowser=firefox` | `chrome` / `chromium` | Selenium/Selenide: chrome, firefox, edge. Playwright: chromium, firefox, webkit |
| `-Dchannel=` | `chrome` | только Playwright: пусто = скачать свой Chromium вместо установленного Chrome |
| `-Dtest.parallel=false` | `true` | параллельный запуск классов |
| `-Dtest.threads=5` | `3` | число потоков = число одновременных браузеров |
| `-Dtest.retries=2` | `0` | перезапуск упавших (флейки видны в отчёте как Flakes) |
| `-Dtimeout.seconds=15` | `10` | таймаут ожиданий |
| `-Dbrowser.reuse=true` | `false` | Selenium/Selenide: один браузер на поток с очисткой состояния вместо нового на каждый тест |
| `-Dvideo=true` | `false` | только Playwright: видео каждого теста |
| `-DbaseUrl=http://...` | пусто | тестировать уже запущенный экземпляр приложения |
| `-Ddemo.latency=0` | `1.0` | множитель задержек приложения (0 = без задержек) |

Артефакты: `<module>/target/artifacts/` (скриншоты, failure-артефакты, загрузки, у Selenide — `selenide-reports`),
отчёты JUnit — `<module>/target/surefire-reports/`.

### Playwright TS (`playwright-ts/`)

Нужен Node.js 20+. Приложение собирать и запускать вручную не нужно: его поднимает `webServer` из
`playwright.config.ts` (при первом запуске соберёт `demo-app` через Maven).

```bash
cd playwright-ts && npm ci
```

```bash
npx playwright test
```

| Команда | Что делает |
|---|---|
| `npm run test:smoke` / `test:critical` / `test:regression` | фильтр по тегу (`--grep @smoke`) |
| `npm run test:ui` | UI mode: дерево тестов, time travel по шагам, watch |
| `npm run test:debug` | пошаговая отладка с Inspector |
| `npm run report` | HTML-отчёт: шаги, скриншоты, видео, trace упавших тестов |
| `npm run test:failure-demo` | тест, который падает специально |
| `npm run update-snapshots` | перезаписать эталоны визуальных сравнений |
| `npm run codegen` | записать тест кликами в браузере (нужен запущенный `npm run app`) |
| `npm run test:all-browsers` | + Firefox и WebKit (сначала `npx playwright install firefox webkit`) |
| `npx playwright test examples` | один сценарий в двух стилях: «привычки Selenium» vs идиоматичный |

Отчёты: `playwright-report/` (HTML), `test-results/` (артефакты упавших тестов и `junit.xml`).

### Windows + JDK 25

На JDK 25 под Windows NIO-селекторы создают AF_UNIX-сокет во временной папке, и если в пути есть короткое имя
8.3 (`C:\Users\JOHNDO~1`), падают с `Unable to establish loopback connection` — и HTTP-сервер, и ChromeDriver.
В pom.xml это обходится через `-Djdk.net.unixdomain.tmpdir=${project.build.directory}`, а `DemoApp` при запуске
через `main` сам переключается на `~/.sandbox-shop-tmp`.

## Одинаковые сценарии во всех модулях

В `playwright-ts` те же сценарии лежат в `tests/*.spec.ts` (40 тестов: плюс 2 визуальных снапшота и 2 теста
из `examples/`). Ниже — классы Java-модулей.

| Класс | Тестов | Теги | Что проверяет |
|---|---|---|---|
| `LoginTest` | 6 | smoke, regression | UI-логин, заблокированный пользователь, параметризованные неверные данные, спиннер при медленном логине |
| `CatalogTest` | 6 (+1 в Playwright) | regression | поиск с debounce, пустой результат, сортировка, stale element / ленивые локаторы, промо-модалка; в Playwright ещё мок 500-ки бэкенда |
| `CartTest` | 3 | smoke, critical, regression | добавление + тосты + бейдж + проверка бэкенда, удаление с анимацией, пустая корзина |
| `CheckoutTest` | 5 | critical, regression | полный путь покупки до статуса COMPLETED, валидация ZIP, медленный пользователь |
| `OrdersTest` | 2 | regression | пагинация на 12 сгенерированных заказах, пустой список |
| `PlaygroundTest` | 9 | browser-api | confirm/prompt, iframe, shadow DOM, новая вкладка, upload, download, lazy-контент |
| `JavaScriptAndScreenshotsTest` | 4 | browser-api | типы значений из JS, async JS, JS-клик сквозь оверлей, скриншоты viewport/элемента/всей страницы |
| `FailureDemoTest` | 1 | demo-failure (выключен) | падает специально, чтобы показать артефакты |

Общая схема жизненного цикла в Java-модулях (в TS то же самое делают `globalSetup`, `webServer` и фикстуры):

```
LauncherSessionListener.opened   один раз за запуск: старт приложения (аналог @BeforeSuite в TestNG)
  @BeforeSuite (только при запуске @Suite-класса)   POST /api/test/reset
    [extension] beforeEach          новый браузер (Selenium) / ничего (Selenide, ленивый старт) / новый BrowserContext (Playwright)
    @BeforeEach                     логин через API + токен в localStorage (быстрее UI-логина в ~10 раз)
    test
    [extension] afterTestExecution  скриншот/HTML/trace, если тест упал - браузер ещё жив
    @AfterEach                      POST /api/test/cleanup - удалить корзину и заказы этого пользователя
    [extension] afterEach           закрыть браузер / контекст
  @AfterSuite
LauncherSessionListener.closed   добить все браузеры
```
