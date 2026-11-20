# Шпаргалка (stage 2): как запускать всё

Состояние этого этапа: **четыре фреймворка** (Selenium, Selenide, Playwright Java, Playwright TS) против
**Java-версии** Sandbox Shop (`demo-app/`, многостраничное приложение на JDK HttpServer). Angular/Next.js,
Docker, CI, Allure и линтеров здесь ещё нет — они появились на следующих этапах.

Все команды — из корня (`C:\Work\qaAutomationStages\stage2`) в Git Bash, если не сказано иное.
Объяснения — в [README.md](README.md), сравнение фреймворков — в [COMPARISON.md](COMPARISON.md).

- [0. Что нужно установить](#0-что-нужно-установить)
- [1. Как залогиниться в тестовое приложение](#1-как-залогиниться-в-тестовое-приложение)
- [2. Запустить приложение вручную](#2-запустить-приложение-вручную)
- [3. Java-фреймворки: Selenium, Selenide, Playwright Java](#3-java-фреймворки-selenium-selenide-playwright-java)
- [4. Playwright TS](#4-playwright-ts)
- [5. Где лежат отчёты и артефакты](#5-где-лежат-отчёты-и-артефакты)
- [6. Allure: один отчёт для Selenide (JUnit 5, TestNG) и Playwright TS](#6-allure-один-отчёт-для-selenide-junit-5-testng-и-playwright-ts)

---

## 0. Что нужно установить

| Для чего | Что |
|---|---|
| Java-фреймворки и само приложение | JDK 21+ (Maven не нужен — есть `./mvnw`) |
| Playwright TS | Node.js 20+ |
| Браузерные тесты | Google Chrome |

Один раз перед Playwright TS (в worktree нет `node_modules`):

```bash
cd playwright-ts && npm ci
```

---

## 1. Как залогиниться в тестовое приложение

Регистрации нет: **подходит любой логин** из букв, цифр и `_ . -` (от 3 до 40 символов),
**пароль всегда `secret`**. Каждый новый логин — новый пользователь с пустой корзиной и без заказов.

| Логин | Пароль | Что будет |
|---|---|---|
| `demo_user` (или любой другой) | `secret` | обычный пользователь |
| `slow_demo` (начинается на `slow_`) | `secret` | всё в 4 раза медленнее — видно спиннер и задержки |
| `locked_demo` (начинается на `locked_`) | `secret` | ошибка «Sorry, this user has been locked out» |
| любой | не `secret` | ошибка «Username and password do not match any user» |
| пустой | любой | ошибка «Username and password are required» |

Где открыть приложение:

| Как запущено | Адрес |
|---|---|
| вручную, раздел 2 | http://127.0.0.1:8080 |
| `npm run app` в `playwright-ts/` | http://127.0.0.1:4321 |

Страницы: `/index.html` (вход, он же `/`), `/catalog.html`, `/cart.html`, `/checkout.html`, `/orders.html`,
`/playground.html`. Каталог: поиск, сортировка, «Add to cart»; Checkout — ZIP из 5 цифр; в Orders статус
меняется с PROCESSING на COMPLETED через 5 с (жмите Refresh); Playground — диалоги, iframe, shadow DOM, новая
вкладка, загрузка и скачивание файлов. Промокод для Playground — `SAVE10`.

Тестовое API (им пользуются тесты) требует заголовок `X-Test-Key: let-me-in`:

```bash
curl -X POST http://127.0.0.1:8080/api/test/seed-orders -H "X-Test-Key: let-me-in" -H "Content-Type: application/json" -d '{"username":"demo_user","count":12}'
```

Остальные: `POST /api/test/cleanup {username}`, `GET /api/test/state?username=...`, `POST /api/test/reset`.
Тесты (кроме тестов логина) входят через `POST /api/login` и кладут токен в `localStorage`.

---

## 2. Запустить приложение вручную

Собрать:

```bash
./mvnw -q -pl demo-app package -DskipTests
```

Запустить на порту 8080:

```bash
java -cp "demo-app/target/classes;demo-app/target/lib/*" com.sandbox.app.DemoApp 8080
```

(на Linux/macOS в classpath `:` вместо `;`). Без задержек бэкенда: добавить `-Ddemo.latency=0` перед `-cp`.

Для тестов запускать приложение вручную **не нужно**: Java-фреймворки поднимают его сами внутри прогона,
Playwright TS — через `webServer`.

---

## 3. Java-фреймворки: Selenium, Selenide, Playwright Java

Приложение стартует внутри тестового процесса на случайном порту.

| Что | Команда |
|---|---|
| Все три фреймворка | `./mvnw test` |
| Только один | `./mvnw -pl selenium -am test` (или `selenide`, `playwright`) |
| Один тестовый класс | `./mvnw -pl selenide -am test -Dtest=CartTest -Dsurefire.failIfNoSpecifiedTests=false` |
| Один метод | `./mvnw -pl selenium -am test -Dtest=CartTest#emptyCartCannotBeCheckedOut -Dsurefire.failIfNoSpecifiedTests=false` |
| По тегу | `./mvnw -pl selenide -am test -Dgroups=smoke` |
| Выражение по тегам | `./mvnw test -Dgroups="smoke \| critical"` |
| Сьют с `@BeforeSuite` | `./mvnw -pl selenium -am test -Dtest=SmokeSuite -Dsurefire.failIfNoSpecifiedTests=false` |
| Тест, падающий специально | `./mvnw -pl playwright -am test -Dgroups=demo-failure -DexcludedGroups= -Dsurefire.failIfNoSpecifiedTests=false` |
| Смотреть браузер | `./mvnw -pl selenide -am test -Dheadless=false -Dtest.threads=1` |
| Против уже запущенного приложения | `./mvnw test -DbaseUrl=http://127.0.0.1:8080` |

Теги: `smoke`, `critical`, `regression`, `browser-api`, `demo-failure` (выключен по умолчанию).
Сьюты: `SmokeSuite`, `CriticalSuite`, `FullRegressionSuite`.

Переключатели (`-D...`, значения по умолчанию — в `src/test/resources/framework.properties` каждого модуля):

| Ключ | По умолчанию | Что делает |
|---|---|---|
| `baseUrl` | пусто | адрес уже запущенного приложения; пусто — поднять своё |
| `headless` | `true` | `false` — видно браузер |
| `browser` | `chrome` / `chromium` | Selenium/Selenide: `chrome`, `firefox`, `edge`; Playwright: `chromium`, `firefox`, `webkit` |
| `channel` | `chrome` | только Playwright Java: какой установленный браузер брать |
| `timeout.seconds` | `10` | таймаут ожиданий |
| `test.parallel` | `true` | параллельный запуск классов |
| `test.threads` | `3` | число потоков = одновременных браузеров |
| `test.retries` | `0` | перезапуск упавших тестов |
| `browser.reuse` | `false` | Selenium/Selenide: один браузер на поток вместо нового на каждый тест |
| `video` | `false` | только Playwright Java: видео тестов |
| `demo.latency` | `1.0` | множитель задержек приложения (`0` — без задержек) |

На этом этапе пустое значение `-Dkey=` **не** перекрывает файл: чтобы Playwright Java взял свой Chromium вместо
Chrome, поменяйте `channel=` в `playwright/src/test/resources/framework.properties`.

### Selenide на TestNG (`selenide-testng/`)

Те же сценарии, но раннер TestNG: что запускать, задаёт XML-файл `selenide-testng/src/test/resources/testng/<suite>.xml`.
`./mvnw test` из корня запускает и этот модуль (`all.xml`).

| Что | Команда |
|---|---|
| Все, кроме падающего демо (`all.xml`) | `./mvnw -pl selenide-testng -am test` |
| Smoke (`smoke.xml`) | `./mvnw -pl selenide-testng -am test -Dsuite=smoke` |
| Critical — явный список классов и методов (`critical.xml`) | `./mvnw -pl selenide-testng -am test -Dsuite=critical` |
| Regression — два блока `<test>` (`regression.xml`) | `./mvnw -pl selenide-testng -am test -Dsuite=regression` |
| Методы одного класса параллельно (`parallel-methods.xml`) | `./mvnw -pl selenide-testng -am test -Dsuite=parallel-methods` |
| Падающий тест + ретрай (`demo-failure.xml`) | `./mvnw -pl selenide-testng -am test -Dsuite=demo-failure -Dtest.retries=1` |
| Один класс без XML | `./mvnw -pl selenide-testng -am test -Dtest=CartTest -Dsurefire.failIfNoSpecifiedTests=false` |
| Видеть браузер | добавить `-Dheadless=false` (сильнее, чем `<parameter>` в XML) |

Ключи `-D...` те же, что в таблице выше, плюс `suite` (имя XML-файла) и `test.retries` — здесь ретраи делает
`RetryAnalyzer` TestNG. `-Dgroups` в этом модуле не нужен: группы задаёт XML.
Отчёты: `selenide-testng/target/surefire-reports/index.html` (HTML TestNG), `emailable-report.html`,
после падений — `testng-failed.xml` (набор только из упавших тестов).
Чем TestNG отличается от JUnit 5 и Playwright Test — разделы 14 и 15 в [SELENIDE_VS_PLAYWRIGHT_TS.md](SELENIDE_VS_PLAYWRIGHT_TS.md).

---

## 4. Playwright TS

Все команды — из папки `playwright-ts/`. Приложение поднимает `webServer` из конфига: собирает `demo-app`
через Maven (первый раз) и запускает его на :4321. Если `npm run app` уже запущен — использует его.

| Что | Команда |
|---|---|
| Все тесты | `npx playwright test` |
| Smoke / critical / regression | `npm run test:smoke` / `npm run test:critical` / `npm run test:regression` |
| Один файл | `npx playwright test tests/cart.spec.ts` |
| По имени теста | `npx playwright test -g "locked user"` |
| UI mode (дерево тестов, шаги, watch) | `npm run test:ui` |
| Видеть браузер | `npx playwright test --headed --workers=1` |
| Пошаговая отладка | `npm run test:debug` |
| HTML-отчёт последнего прогона | `npm run report` |
| Тест, падающий специально | `npm run test:failure-demo` |
| Один сценарий в двух стилях | `npx playwright test examples` |
| + Firefox и WebKit | `npx playwright install firefox webkit`, потом `npm run test:all-browsers` |
| Против любого адреса | `BASE_URL=http://127.0.0.1:8080 npx playwright test` |
| Приложение отдельно | `npm run app` (держать открытым в отдельном терминале) |
| Записать тест кликами | `npm run app`, потом в другом терминале `npm run codegen` |
| Обновить визуальные эталоны | `npm run update-snapshots` |
| Проверка типов | `npm run typecheck` |
| Открыть trace | `npx playwright show-trace test-results/<папка теста>/trace.zip` |

Переменные окружения: `BASE_URL`, `APP_PORT` (по умолчанию 4321), `ALL_BROWSERS=1`, `FAILURE_DEMO=1`.
В PowerShell: `$env:BASE_URL="http://127.0.0.1:8080"; npx playwright test`.

---

## 5. Где лежат отчёты и артефакты

| Что | Где |
|---|---|
| JUnit-отчёты Java | `<модуль>/target/surefire-reports/` |
| Скриншот, HTML, console, trace упавших Java-тестов | `<модуль>/target/artifacts/<Класс>/<тест>-<время>/` |
| Скриншоты Selenide | `selenide/target/artifacts/selenide-reports/` |
| Скриншоты из теста скриншотов | `<модуль>/target/artifacts/screenshots/` |
| HTML-отчёт Playwright TS | `playwright-ts/playwright-report/` |
| Trace, видео, скриншоты упавших TS-тестов | `playwright-ts/test-results/` |
| JUnit XML Playwright TS | `playwright-ts/test-results/junit.xml` |
| Результаты Allure | `selenide/target/allure-results/`, `selenide-testng/target/allure-results/`, `playwright-ts/allure-results/` |
| Отчёты Allure | `allure-report-2/` (Allure 2), `allure-report-3/` (Allure 3) — в корне |

Очистить результаты Java-сборок: `./mvnw clean`.

---

## 6. Allure: один отчёт для Selenide (JUnit 5, TestNG) и Playwright TS

Тесты трёх модулей пишут результаты Allure сами, при обычном запуске (разделы 3 и 4). Каждый прогон
удаляет старые результаты своего модуля. Selenium и Playwright Java в Allure не подключены.

Отчёт можно собрать **двумя генераторами** из одних и тех же результатов — чтобы сравнить:

- **Allure 2** (`allure-commandline`, работает на Java) — классический отчёт: вкладки Overview, Categories,
  Suites, Behaviors, Packages, Graphs, Timeline. Такой же показывает Jenkins-плагин Allure;
- **Allure 3** (`allure`, Node.js) — новый интерфейс: одна страница с деревом тестов, фильтрами и Quality Gates;
  настраивается в `allurerc.mjs`.

Один раз — поставить оба в корне:

```bash
npm ci
```

| Что | Allure 2 (классический) | Allure 3 |
|---|---|---|
| Собрать отчёт из всех модулей, которые запускались | `npm run allure2:generate` → `allure-report-2/` | `npm run allure3:generate` → `allure-report-3/` |
| Собрать и открыть в браузере | `npm run allure2:open` | `npm run allure3:open` |
| Только некоторые модули | `node scripts/allure-report.mjs 2 open selenide-testng` | `node scripts/allure-report.mjs 3 open selenide-testng` |

Модули: `selenide`, `selenide-testng`, `playwright-ts`.

Оба отчёта собираются **одним файлом** (`allure-report-2/index.html`, `allure-report-3/index.html`): данные и
скриншоты внутри, открывается двойным кликом, можно переслать или приложить к CI. Обычный (многофайловый) отчёт
Allure подгружает каждый тест и вкладку через `fetch()` — без работающего веб-сервера (`allure open`) он показывает
«Failed to fetch» на любом клике.

Пример: прогнать оба Selenide-модуля и Playwright TS, потом открыть общий отчёт:

```bash
./mvnw -pl selenide,selenide-testng -am test
```

```bash
cd playwright-ts && npx playwright test && cd ..
```

```bash
npm run allure2:open
```

Верхний уровень дерева:

- Allure 2, вкладка Suites — `parentSuite`: `Selenide + JUnit 5`, `Selenide + TestNG: all` (имя XML-сьюта), `chrome`
  (project Playwright); вкладка Behaviors — по `epic`: `Selenide + JUnit 5`, `Selenide + TestNG`, `Playwright TS`;
- Allure 3 — по `epic` (настроено в `allurerc.mjs`).

| | Allure 2 | Allure 3 |
|---|---|---|
| Чем запускается | Java (`allure-commandline`, внутри — `allure.bat` / `allure`) | Node.js (`allure`) |
| Интерфейс | вкладки: Overview, Categories, Suites, Behaviors, Packages, Graphs, Timeline | одна страница: дерево, фильтры (Severity, Tags…), Quality Gates, Global Attachments / Errors |
| Группировка дерева | фиксированная: Suites — `parentSuite` → `suite` → `subSuite`, Behaviors — `epic` → `feature` → `story` | любая, `groupBy` в `allurerc.mjs` |
| Настройка | параметры командной строки (`--name`) | файл `allurerc.mjs` (имя, вывод, плагины) |
| Где ещё встречается | Jenkins-плагин Allure, большинство CI | новые проекты; в CI — через `npx allure` |
| Результаты тестов | `allure-results` — **одинаковые** для обоих | |

На Windows с JDK 25 Allure 2 падает с `Unable to establish loopback connection` (та же проблема, что в разделе
про pom.xml); `scripts/allure-report.mjs` обходит её сам через `ALLURE_OPTS=-Djdk.net.unixdomain.tmpdir=...`.
Чем отличаются отчёты трёх модулей — раздел 12.1 в [SELENIDE_VS_PLAYWRIGHT_TS.md](SELENIDE_VS_PLAYWRIGHT_TS.md).

Осторожно: `npx playwright test --reporter=dot` (или любой `--reporter`) заменяет reporter'ы из конфига — Allure
тогда ничего не запишет.
