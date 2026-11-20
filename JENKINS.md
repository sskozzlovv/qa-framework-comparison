# Локальный CI-стенд: Jenkins + Selenium Grid (stage 2)

Jenkins, его агент, Selenium Grid и тестируемое приложение в Docker на этом компьютере. Всё уже настроено кодом
(Configuration as Code), а ниже описано, как сделать то же самое руками в UI — чтобы потрогать.

- [1. Запустить и остановить](#1-запустить-и-остановить)
- [2. Что где](#2-что-где)
- [3. Как это устроено](#3-как-это-устроено)
- [4. Запустить джобы](#4-запустить-джобы)
- [5. Руками: агент (нода)](#5-руками-агент-нода)
- [6. Руками: Allure в Tools](#6-руками-allure-в-tools)
- [7. Руками: джобы](#7-руками-джобы)
- [8. Эксперименты](#8-эксперименты)
- [9. Если что-то не так](#9-если-что-то-не-так)

---

## 1. Запустить и остановить

Нужен запущенный **Docker Desktop**. Из корня `stage2` в Git Bash:

```bash
bash jenkins/up.sh
```

Первый запуск — 5–15 минут: скачиваются образы (Jenkins, Grid, Chrome), ставятся плагины. Дальше — секунды.
Jenkins отвечает примерно через минуту после старта контейнеров.

Остановить (всё сохранится: джобы, история сборок, отчёты):

```bash
docker compose -f jenkins/docker-compose.yml down
```

Остановить и удалить всё, включая историю сборок и кэши Maven/npm:

```bash
docker compose -f jenkins/docker-compose.yml down -v
```

> **Джобы берут код из git**, ветка `stage2` — как настоящий CI. Незакоммиченные изменения Jenkins не видит:
> поменял тест или `Jenkinsfile` → закоммить → Build Now.

---

## 2. Что где

| Что | Адрес |
|---|---|
| Jenkins | http://localhost:8081 — без логина (стенд локальный, порты открыты только на 127.0.0.1) |
| Selenium Grid: ноды, слоты, сессии, очередь | http://localhost:4444/ui/ |
| Браузер Grid вживую (noVNC) | http://localhost:7900 → Connect, без пароля |
| Тестируемое приложение | http://localhost:8080 (логин любой, пароль `secret`) |

---

## 3. Как это устроено

```
Jenkins controller (контейнер jenkins, 0 executors — сам ничего не собирает)
   │ SSH, ключ из jenkins/secrets/ (делает up.sh)
   ▼
Агент agent-1 (контейнер agent, 2 executors, labels: maven playwright)
   │  git clone file:///repo.git (ветка stage2)  ← git-репозиторий этого компьютера, только чтение
   │
   ├── джоба selenide-testng: ./mvnw test -Dremote=http://selenium-hub:4444 -DbaseUrl=http://demo-app:8080
   │         │ RemoteWebDriver
   │         ▼
   │     selenium-hub  ──►  chrome (нода, 3 слота) ──► http://demo-app:8080
   │
   └── джоба playwright-ts: npx playwright test
             браузер (Google Chrome) — прямо на агенте; приложение поднимает webServer на агенте
```

| Файл | Что в нём |
|---|---|
| [`jenkins/docker-compose.yml`](jenkins/docker-compose.yml) | все контейнеры, порты, тома |
| [`jenkins/up.sh`](jenkins/up.sh) | запуск: путь к git-репозиторию и SSH-ключ агента (один раз, в `jenkins/secrets/`) → в `jenkins/.env` (оба не в git, секретов нет: путь и *публичный* ключ) → `docker compose up --build` |
| [`jenkins/controller/`](jenkins/controller) | образ Jenkins: LTS + плагины ([`plugins.txt`](jenkins/controller/plugins.txt)) |
| [`jenkins/agent/Dockerfile`](jenkins/agent/Dockerfile) | образ агента: JDK 21 (для `./mvnw`) + Node.js + Google Chrome (для Playwright) |
| [`jenkins/app/Dockerfile`](jenkins/app/Dockerfile) | приложение отдельным контейнером, слушает все интерфейсы (`-Ddemo.host=0.0.0.0`) |
| [`jenkins/casc/jenkins.yaml`](jenkins/casc/jenkins.yaml) | **Configuration as Code**: агент, SSH-ключ, Allure, обе джобы |
| [`jenkins/Jenkinsfile.selenide-testng`](jenkins/Jenkinsfile.selenide-testng) | пайплайн Selenide + TestNG |
| [`jenkins/Jenkinsfile.playwright-ts`](jenkins/Jenkinsfile.playwright-ts) | пайплайн Playwright TS |

Изменения в коде ради стенда:
- `demo-app`: `-Ddemo.host=0.0.0.0` — слушать не только loopback, иначе браузер из другого контейнера не достучится;
- `selenide-testng`: `-Dremote=<url Grid>` → `Configuration.remote`, скачивание файлов через `HTTPGET`
  (папка загрузок браузера — на ноде Grid, а не на агенте);
- `mvnw` стал исполняемым в git (на Linux-агенте иначе `permission denied`).

---

## 4. Запустить джобы

> **Где кнопка Build with Parameters?** Параметры описаны в `Jenkinsfile` (`parameters { ... }`), и Jenkins узнаёт
> их только **во время** build'а. Поэтому у новой джобы сначала есть лишь **Build Now** — первый build идёт с
> параметрами по умолчанию (`${SUITE:-smoke}` в `Jenkinsfile`), со второго появляется **Build with Parameters**.
> На стенде это повторяется после каждого перезапуска Jenkins: JCasC (Job DSL) заново создаёт джобы из
> `casc/jenkins.yaml`, и выученные параметры сбрасываются.

Jenkins → **selenide-testng** → **Build with Parameters**:
- `SUITE` — какой XML-сьют (`smoke`, `critical`, `regression`, `all`, `parallel-methods`);
- `HEADLESS` — по умолчанию `false`: открой http://localhost:7900 и смотри, как тесты кликают в браузере Grid.

Во время прогона открой http://localhost:4444/ui/ — видно, как занимаются 3 слота ноды.

Jenkins → **playwright-ts** → **Build with Parameters** (`GREP` пустой — все тесты, `@smoke` — только smoke).

После сборки на странице джобы и build'а:
- **Allure Report** — классический Allure 2 (плагин сам скачал Allure 2.46.1, см. раздел 6);
- **Playwright Report** (только playwright-ts) — родной HTML-отчёт Playwright, у упавших тестов — trace;
- **Test Result** — JUnit-результаты, тренд прошло/упало по сборкам;
- **Build Artifacts** — скриншоты и HTML упавших проверок, trace/видео.

Жёлтый build (UNSTABLE) — тесты упали, но сборка дошла до отчётов. Красный (FAILURE) — сломалась сама сборка.

---

## 5. Руками: агент (нода)

Агент `agent-1` создан из [`casc/jenkins.yaml`](jenkins/casc/jenkins.yaml). Повторить руками:

1. **Manage Jenkins → Nodes** → `agent-1` → **Delete Agent**. Контейнер `agent` продолжает работать, Jenkins
   просто забыл о нём.
2. **New Node** → Node name `agent-1` → ☑ Permanent Agent → Create.
3. Заполнить:

   | Поле | Значение | Зачем |
   |---|---|---|
   | Number of executors | `2` | сколько сборок одновременно на агенте |
   | Remote root directory | `/home/jenkins/agent` | где на агенте лежат workspace'ы |
   | Labels | `maven playwright` | по ним `Jenkinsfile` выбирает агента: `agent { label 'maven' }` |
   | Usage | Use this node as much as possible | |
   | Launch method | **Launch agents via SSH** | controller сам подключается к агенту |
   | Host | `agent` | имя контейнера в сети Docker |
   | Credentials | `jenkins (SSH key for agent-1 …)` | ключ уже лежит в Credentials (из `jenkins/secrets/agent_key`) |
   | Host Key Verification Strategy | Non verifying Verification Strategy | локально, ключ хоста не проверяем |
   | Advanced → JavaPath | `/opt/java/openjdk/bin/java` | где на агенте Java для `agent.jar` |

4. **Save** → через несколько секунд агент online. В его **Log** видно, как controller по SSH копирует и запускает
   `agent.jar`.

Хочешь создать и ключ руками: **Manage Jenkins → Credentials → System → Global → Add Credentials** →
Kind: *SSH Username with private key*, Username `jenkins`, Private Key: Enter directly → содержимое
`jenkins/secrets/agent_key`.

> Изменения, сделанные руками, живут в `jenkins_home` (том Docker) и переживают перезапуск. Но при старте JCasC
> снова применяет `jenkins.yaml`: агент `agent-1` из файла вернётся к описанным там настройкам. Чтобы ручной вариант
> «победил», убери блок `nodes:` из `jenkins.yaml`.

---

## 6. Руками: Allure в Tools

Шаг `allure commandline: 'allure'` в `Jenkinsfile` ищет инструмент с именем `allure`.
**Manage Jenkins → Tools → Allure Commandline installations**. Два способа:

| Способ | Как заполнить | Плюсы и минусы |
|---|---|---|
| **Уже установлен на агенте** (так сделано на стенде) | Name `allure`, ☐ Install automatically, `ALLURE_HOME` = `/opt/allure` | версия ровно та, что в образе агента ([`jenkins/agent/Dockerfile`](jenkins/agent/Dockerfile)); обновлять — пересборкой образа |
| **Автоустановка** | Name `allure`, ☑ Install automatically → *From Maven Central*, версия из списка | ставится сама на каждый агент при первой сборке; но список версий приходит из update center Jenkins и **отстаёт**: на стенде в нём последняя 2.43.0, а 2.46.1 нет. Версию, которой нет в списке, плагин не установит («Can't find allure commandline») |

Попробуй переключить на автоустановку с версией из списка — при следующей сборке в логе будет видно, как плагин
скачивает zip с Maven Central в `/home/jenkins/agent/tools/` на агенте.

---

## 7. Руками: джобы

Обе джобы — **Pipeline script from SCM**: в самой джобе только «откуда взять `Jenkinsfile`», весь пайплайн —
в репозитории. Повторить руками:

1. **New Item** → имя `selenide-testng-manual` → **Pipeline** → OK.
2. Раздел **Pipeline**:
   - Definition: **Pipeline script from SCM**;
   - SCM: Git, Repository URL `file:///repo.git` (credentials не нужны);
   - Branch Specifier: `*/stage2`;
   - Script Path: `jenkins/Jenkinsfile.selenide-testng`;
   - ☑ Lightweight checkout.
3. **Save** → **Build Now**. Первый build пройдёт с параметрами по умолчанию и зарегистрирует их — со второго
   появится **Build with Parameters**.

Для сравнения — та же джоба **кликами, без Jenkinsfile** (Freestyle):

1. **New Item** → `selenide-testng-freestyle` → **Freestyle project**.
2. **General** → ☑ Restrict where this project can be run → Label Expression `maven`.
   ☑ This project is parameterized → Choice Parameter `SUITE`: `smoke`, `regression`.
3. **Source Code Management** → Git → `file:///repo.git`, ветка `*/stage2`.
4. **Build Steps** → Execute shell:
   ```
   ./mvnw -B -pl selenide-testng -am test -Dsuite=$SUITE -Dremote=http://selenium-hub:4444 -DbaseUrl=http://demo-app:8080 -Dmaven.test.failure.ignore=true
   ```
5. **Post-build Actions** → Allure Report → Path `selenide-testng/target/allure-results`;
   Publish JUnit test result report → `selenide-testng/target/surefire-reports/TEST-*.xml`.

Разница: у Freestyle вся логика хранится в Jenkins (`jenkins_home/jobs/<имя>/config.xml`) и меняется кликами.
У Pipeline — в репозитории, версионируется и проходит ревью как код.

---

## 8. Эксперименты

| Что сделать | Что увидеть |
|---|---|
| Запустить `selenide-testng` (regression) два раза подряд, не дожидаясь первого | агент: оба build'а идут (2 executors); Grid: 6 запросов на 3 слота — половина в **очереди** (Sessions Queue в UI Grid) |
| Nodes → agent-1 → Configure → executors `1`, запустить обе джобы | вторая ждёт в очереди Jenkins: «Waiting for next available executor» |
| `docker compose -f jenkins/docker-compose.yml up -d --scale chrome=2 --no-recreate` (сначала убрать `ports` у `chrome` — две ноды не могут занять один порт 7900) | в UI Grid вторая нода появилась сама, слотов 6 — регистрировать ничего не нужно |
| `docker compose -f jenkins/docker-compose.yml stop agent` | агент offline, build'ы ждут; `start agent` — Jenkins переподключится сам |
| Поломать тест, закоммитить, Build Now | жёлтый build, в Allure — шаг со скриншотом, в Playwright Report — trace |

---

## 9. Если что-то не так

| Симптом | Что сделать |
|---|---|
| `run jenkins/up.sh` при `docker compose ...` | нет `jenkins/.env` (путь к git-репозиторию и SSH-ключ агента) — его создаёт `bash jenkins/up.sh`, запусти его один раз |
| Агент offline | Nodes → agent-1 → Log; `docker compose -f jenkins/docker-compose.yml logs agent` |
| Изменения в коде не попали в build | они не закоммичены: Jenkins клонирует ветку `stage2` из git |
| Нет Build with Parameters, только Build Now | первый build после создания джобы или после перезапуска Jenkins — см. раздел 4 |
| Allure: `Can't find allure commandline` | инструмент `allure` указывает на версию, которой нет на агенте: в образе агента — `/opt/allure`, у автоустановки — только версии из её списка (раздел 6) |
| Playwright: все тесты падают с `Executable doesn't exist .../ffmpeg-linux` | на агенте нет ffmpeg для записи видео — в образе агента он ставится (`playwright install ffmpeg`), пересобери: `bash jenkins/up.sh` |
| Selenide: `Could not start a new session` | все слоты Grid заняты дольше 2 минут или нода упала: http://localhost:4444/ui/ |
| Пустая страница UI Grid | адрес без слэша `/ui` — UI ищет свой скрипт в `/index.js` (404). Открывай `http://localhost:4444/ui/` или просто `http://localhost:4444` |
| Пустая страница Playwright Report | CSP: на стенде он отключён в `jenkins/controller/Dockerfile`; на настоящем Jenkins это решает админ (Resource Root URL) |
| Логи всего | `docker compose -f jenkins/docker-compose.yml logs -f jenkins` |
