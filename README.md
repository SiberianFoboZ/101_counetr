# 101 Counter

Android-приложение для ведущего настольной игры «101». Считает очки по настраиваемым правилам, ведёт историю раундов, поддерживает статистику игроков и партий. Распространяется через GitHub Releases.

**Версия:** 1.0.1 (`versionCode = 2`)

## Возможности

### Игровой процесс
- Партии с 2–10 игроками.
- Пошаговый ввод раунда: выбор победителя → распределение карт.
- Бонус победителю: плашки `0 / −20 / −40 / −50` (отрицательные — штрафы победителю).
- Автопауза при уходе приложения в фон (`ON_STOP`).
- Завершение игры вместо паузы через явную кнопку «Завершить игру».
- Чёрно-белая тема, без Google-зависимостей.

### История и статистика
- **История раундов** — карточки по раундам с дельтами каждого игрока.
- **Статистика → Игры** — список всех партий с паузой/финалом/в процессе; тап на FINISHED → детали.
- **Статистика → Игроки** — рейтинг по числу побед; тап → детальный экран игрока со списком его игр и раундов.

### Правила подсчёта (Settings → Правила)
- Визуальный редактор: список правил + форма с 7 полями (название, `applies_to_card`, `match.nominal/suit/condition`, `then`, `else`).
- Два типа правил:
  - **PER_CARD** — применяется к одной карте в руке (стандартное).
  - **FINAL_ADJUSTMENT** — применяется ко всему итогу игрока ПОСЛЕ per-card правил (например, обнуление при 101).
- Операнды: `card_count`, `round_delta_so_far`, `distinct_card_codes`, `total_score`.
- Действия: `const`, `base_value`, `setting` (queen_value / queen_spades_value / king_value / threshold_score), `subtract_total`.
- Превью на примере руки: для per-card — таблица `count × карта → дельта`, для final — таблица `итог до → дельта`.
- Человекочитаемые метки операндов и действий с пояснениями.

### Лимиты карт (CardLimits.kt)
- **Общие лимиты по раунду** (как в колоде): 4 базовых карты, 4 дамы, 4 короля.
- **Индивидуальные лимиты на руку**: 1 пика, 3 не-пик, 3 не-пик короля.
- **Динамические лимиты от `winnerDelta`**:
  - `−20` → пул дам уменьшен (общий лимит 3, индивидуальный для не-пик = 2).
  - `−40` → `Q_spades` заблокирована для других игроков.
  - `−50` → `K_spades` заблокирована для других игроков.

## Сборка из исходников
Требования:
- JDK 17.
- Android SDK (`compileSdk = 37`, `minSdk = 24`).

```bash
./gradlew :app:assembleDebug          # APK без подписи, для отладки
./gradlew :app:testDebugUnitTest      # юнит-тесты (28 тестов)
./gradlew :app:assembleRelease        # подписанный release APK (требует keystore)
```

## Сборка release APK

Release-APK подписывается собственным ключом. Сгенерируй keystore:

```bash
keytool -genkeypair -v \
  -keystore keystore/101-counter-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias 101counter
```

В корне проекта создай `keystore.properties` (этот файл в `.gitignore`):

```properties
storeFile=keystore/101-counter-release.jks
storePassword=ТВОЙ_ПАРОЛЬ_ХРАНИЛИЩА
keyAlias=101counter
keyPassword=ТВОЙ_ПАРОЛЬ_КЛЮЧА
```

> ⚠️ **Никогда не коммить `keystore.properties` и `*.jks` в репозиторий.** Храни keystore в надёжном месте — без него невозможно выпускать обновления с тем же сертификатом.

После этого:

```bash
./gradlew :app:assembleRelease
# → app/build/outputs/apk/release/app-release.apk (переименовывается в 101-counter-v<tag>.apk в CI)
```

Закодируй оба файла в base64 для CI:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore\101-counter-release.jks"))
[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore.properties"))
```

## CI/CD (GitHub Actions)

| Файл | Триггер | Что делает |
|---|---|---|
| `.github/workflows/ci.yml` | каждый PR и push в `main` | тесты + сборка debug APK |
| `.github/workflows/release.yml` | push тега `v*` (например `v1.0.1`) | сборка подписанного release APK + публикация в GitHub Releases |

### Настройка GitHub Secrets

`Settings → Secrets and variables → Actions → New repository secret`:

| Секрет | Содержимое |
|---|---|
| `KEYSTORE_BASE64` | `base64` от `keystore/101-counter-release.jks` |
| `KEYSTORE_PROPERTIES_BASE64` | `base64` от `keystore.properties` |

Если хотя бы один из секретов не задан, workflow собирает APK, подписанный debug-ключом, и помечает релиз как **prerelease** (не «latest»).

### Как выпустить релиз

```bash
# 1. Поднять версию в app/build.gradle.kts (versionCode / versionName)
# 2. Закоммитить и запушить:
git add app/build.gradle.kts
git commit -m "chore: bump version to X.Y.Z"
git push origin main
# 3. Создать тег:
git tag -a vX.Y.Z -m "101 Counter vX.Y.Z"
git push origin vX.Y.Z
```

Workflow автоматически:
1. Декодирует keystore + `keystore.properties` из Secrets.
2. Соберёт подписанный release APK.
3. Переименует APK в `101-counter-v<tag>.apk`.
4. Проверит подпись через `apksigner`.
5. Создаст GitHub Release с прикреплённым APK и release notes.
6. Пометит релиз как `latest` (если keystore задан) или как `prerelease`.

## Структура проекта

```
app/
  src/main/java/com/counter/game/
    CounterApp.kt, MainActivity.kt      — Application + Activity, DI без Hilt/Koin
    data/
      entity/                            — Room entities (Player, Game, Round, Rule, …)
      dao/                               — DAO-интерфейсы и data-классы для запросов
      repo/                              — Repositories (Player, Game, Rules, Settings, …)
      db/AppDatabase.kt                  — Room DB (version 2 + миграция v1→v2)
      db/DatabaseSeeder.kt               — seed данных
    engine/
      RuleDefinition.kt                  — модель правила + JSON-парсер
      RuleSerializer.kt                  — UI-форма ↔ JSON
      RuleEngine.kt                      — двухфазный движок (compute + computeWith)
    ui/
      home/, players/, settings/, newgame/, game/, round/, history/
        rules/, statistics/, player/     — экраны + ViewModel-и
      common/                            — переиспользуемые компоненты
      theme/                             — MaterialTheme
      nav/Routes.kt                      — sealed-класс маршрутов
      ViewModelFactory.kt                — единая фабрика VM
.gradle/
.github/workflows/                     — CI + release workflows
```

## Roadmap

Планы по развитию проекта (по приоритету):

- **Распознавание карт через камеру** — CameraX + ML Kit Object Detection, чтобы ведущий просто фотографировал руку, а приложение само заполняло счётчики.
- **Спец-правила** — бонус за единственного короля/даму и другие особые карточные комбинации.
- **Swipe для архивирования игроков** — заменить текущую иконку-кнопку на `SwipeToDismiss` (Material3).
- **Улучшение ввода раунда** — сейчас 2 секции (победитель + карты), обсудить wizard с 3 шагами.
- **Дополнительные настройки** — выбор темы (зарезервировано `theme_accent` в Settings), drawables и иконки, i18n / RTL.

Подробный список — в `TODO.md` (в `.gitignore`, локальный файл roadmap).

## Лицензия

Личное использование. Распространение через GitHub Releases.
