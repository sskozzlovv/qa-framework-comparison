# Быстрый старт (stage 2): приложение, Selenide, Playwright TS

Только самое нужное, по шагам. Полный список вариантов — в [RUNNING.md](RUNNING.md).

Все команды — в **Git Bash**. Папка проекта: `C:\Work\qaAutomationStages\stage2`.

---

## Шаг 0. Один раз: что должно быть установлено

- JDK 21+ (`java -version`)
- Node.js 20+ (`node -v`)
- Google Chrome

Maven ставить не нужно — в проекте есть `./mvnw`.

---

## Шаг 1. Запустить приложение (если хотите посмотреть его руками)

Для самих тестов этот шаг **не обязателен**: и Selenide, и Playwright TS поднимают приложение сами.

Терминал 1 — собрать и запустить, окно не закрывать:

```bash
cd /c/Work/qaAutomationStages/stage2
```

```bash
./mvnw -q -pl demo-app package -DskipTests
```

```bash
java -cp "demo-app/target/classes;demo-app/target/lib/*" com.sandbox.app.DemoApp 8080
```

Должно появиться `Sandbox Shop running at http://127.0.0.1:8080`. Остановить — `Ctrl+C`.

Открыть в браузере: **http://127.0.0.1:8080**

### Под каким пользователем входить

| Логин | Пароль | Что будет |
|---|---|---|
| `demo_user` (подходит **любой** логин, 3–40 символов: буквы, цифры, `_ . -`) | `secret` | обычный пользователь, своя пустая корзина |
| `slow_demo` (любой логин на `slow_`) | `secret` | всё в 4 раза медленнее |
| `locked_demo` (любой логин на `locked_`) | `secret` | вход запрещён: «Sorry, this user has been locked out» |

Пароль у всех — `secret`. Промокод на странице Playground — `SAVE10`.

---

## Шаг 2. Selenide

Терминал 2 (или тот же, если приложение из шага 1 не запускали):

```bash
cd /c/Work/qaAutomationStages/stage2
```

**Вариант А — тесты сами поднимают приложение** (на случайном порту; запущенное на 8080 не нужно):

```bash
./mvnw -pl selenide -am test
```

**Вариант Б — против приложения из шага 1** (на 8080):

```bash
./mvnw -pl selenide -am test -DbaseUrl=http://127.0.0.1:8080
```

Полезные варианты:

| Что | Команда |
|---|---|
| Только smoke (3 теста, ~20 с) | `./mvnw -pl selenide -am test -Dgroups=smoke` |
| Один класс | `./mvnw -pl selenide -am test -Dtest=CartTest -Dsurefire.failIfNoSpecifiedTests=false` |
| Смотреть, как работает браузер | `./mvnw -pl selenide -am test -Dgroups=smoke -Dheadless=false -Dtest.threads=1` |

Результат в конце вывода: `Tests run: 35, Failures: 0, Errors: 0` и `BUILD SUCCESS`.
Скриншоты и HTML упавших тестов: `selenide/target/artifacts/selenide-reports/`.

### Те же Selenide-тесты на TestNG

Модуль `selenide-testng` — те же 35 сценариев, наборы задаются XML-файлами в
`selenide-testng/src/test/resources/testng/`:

```bash
./mvnw -pl selenide-testng -am test
```

| Что | Команда |
|---|---|
| Все (`all.xml`) | `./mvnw -pl selenide-testng -am test` |
| Smoke / critical / regression | `./mvnw -pl selenide-testng -am test -Dsuite=smoke` (или `critical`, `regression`) |
| Параллельно по методам | `./mvnw -pl selenide-testng -am test -Dsuite=parallel-methods` |
| Один класс без XML | `./mvnw -pl selenide-testng -am test -Dtest=CartTest -Dsurefire.failIfNoSpecifiedTests=false` |
| Против запущенного приложения | добавить `-DbaseUrl=http://127.0.0.1:8080` |

Отчёт TestNG: `selenide-testng/target/surefire-reports/index.html`.

---

## Шаг 3. Playwright TS

Один раз — установить зависимости:

```bash
cd /c/Work/qaAutomationStages/stage2/playwright-ts
```

```bash
npm ci
```

**Вариант А — тесты сами поднимают приложение** (на порту 4321; первый раз соберут его через Maven):

```bash
npx playwright test
```

**Вариант Б — против приложения из шага 1** (на 8080):

```bash
BASE_URL=http://127.0.0.1:8080 npx playwright test
```

Полезные варианты:

| Что | Команда |
|---|---|
| Только smoke (3 теста, ~15 с) | `npm run test:smoke` |
| Один файл | `npx playwright test tests/cart.spec.ts` |
| Смотреть, как работает браузер | `npx playwright test --headed --workers=1` |
| UI mode: список тестов, запуск по кнопке, шаги | `npm run test:ui` |
| HTML-отчёт последнего прогона | `npm run report` |

Результат в конце вывода: `40 passed`. Trace, видео и скриншоты упавших тестов — в `playwright-ts/test-results/`.

В PowerShell вместо `BASE_URL=... npx ...` пишется так:

```powershell
$env:BASE_URL="http://127.0.0.1:8080"; npx playwright test
```

---

## Шаг 4. Общий отчёт Allure

После шагов 2 и 3 (Selenide, TestNG, Playwright TS) — из корня проекта:

```bash
cd /c/Work/qaAutomationStages/stage2
```

```bash
npm ci
```

```bash
npm run allure2:open
```

Откроется классический отчёт Allure 2 (как в Jenkins): вкладки Overview, Suites, Behaviors, Graphs, Timeline;
у каждого теста — шаги, setup/teardown, скриншот упавшего шага. Отчёт — один файл `allure-report-2/index.html`, его можно открыть и двойным кликом.

Тот же отчёт в новом интерфейсе Allure 3 — `npm run allure3:open`. Сравнение двух генераторов — в
[RUNNING.md](RUNNING.md#6-allure-один-отчёт-для-selenide-junit-5-testng-и-playwright-ts).

---

## Если что-то пошло не так

| Симптом | Что сделать |
|---|---|
| `Address already in use` / порт 8080 занят | закрыть приложение из шага 1 (`Ctrl+C`) или запустить на другом порту: `... DemoApp 8081` |
| Playwright TS долго стартует в первый раз | это сборка `demo-app` через Maven, дальше быстро |
| `Unable to establish loopback connection` (JDK 25 на Windows) | уже обойдено в `pom.xml`; при ручном запуске `java` приложение обходит это само |
| Тесты не видят приложение на 8080 | проверить, что оно отвечает: `curl http://127.0.0.1:8080/api/health` → `{"status":"UP"}` |
