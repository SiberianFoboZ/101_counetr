# QWEN.md — 101 Counter

Контекст для агентской работы в этом репозитории. Дополняйте по мере того, как решения закрепляются.

## Что это за проект

- Android-приложение-счётчик для карточной игры «101» (классические правила: проигрывает тот, кто первым превысил 101 очко; особые штрафные карты — дама, пиковая дама, король).
- Это **только счётчик**: логика розыгрыша, ходов, взятия взяток и т.п. в приложении не реализуется. Приложение принимает от ведущего итог раунда по каждому игроку и считает штрафные очки по настраиваемым правилам.
- Аудитория: компания 2–10 человек, играющих в «101» вживую за одним столом.
- Платформа: Android, Kotlin 2.2.10, Jetpack Compose, Room 2.7.2.

## Где что лежит

| Слой | Путь |
| --- | --- |
| Application + DI | `com.counter.game.CounterApp` (`CounterApp.kt`) |
| Nav | `ui/nav/Routes.kt`, `MainActivity.kt` (`CounterNavHost`) |
| ViewModel factory | `ui/ViewModelFactory.kt` |
| Экраны | `ui/<screen>/<Screen>{Screen,ViewModel}.kt` |
| Общие Compose-компоненты | `ui/common/` |
| Room: сущности / DAO / БД | `data/entity/`, `data/dao/`, `data/db/AppDatabase.kt` |
| Сидер | `data/db/DatabaseSeeder.kt` |
| Репозитории | `data/repo/*Repository.kt` |
| Движок правил | `engine/RuleDefinition.kt`, `engine/RuleEngine.kt` |
| Тесты | `app/src/test/...`, `app/src/androidTest/...` |

## Архитектурные решения (не менять без обсуждения)

- **Навигация:** Jetpack Navigation Compose (`androidx.navigation:navigation-compose`).
- **DI:** без фреймворка. `CounterApp` держит `AppContainer` (Room + репозитории); ViewModel создаются через `viewModelFactory { initializer { … } }`.
- **MVVM:** `ViewModel` + `StateFlow` + `kotlinx.coroutines`. Без Compose-only state-only подхода.
- **Тема:** переписана в чёрно-белое. `dynamicColor = false`, `darkTheme` не используется (фиксированная светлая схема).
- **Strings:** на русском, в `res/values/strings.xml`. `app_name = "101 Counter"`.
- **Идентификация игроков:** по `id`, дубликаты имён разрешены. Это **важное** решение, упрощающее модель.
- **Soft-delete:** только для `players` (`is_archived = true`). История игр не теряется.
- **Порог выбытия:** `> threshold_score` (строго больше), дефолт 101.
- **`delta_score` может быть отрицательным** — поддерживается на уровне БД и движка, но не используется seed-правилами.
- **Карты в БД:** по одной записи на код (`Q_hearts`, `Q_spades`, и т.д.), не «масть как поле».
- **Правила:** JSON v2, структура `{ match: { nominal, suit, condition }, then, elseAction }`. Парсер — ручной через `kotlinx.serialization.json.JsonObject`, без `@Serializable`.
- **Сидинг:** через `SupportSQLiteDatabase.execSQL` из `DatabaseSeeder.seed(handle)`. Дёргается явно в `AppContainer.seedIfNeeded()` при `count == 0`.
- **WIN:** реактивно. `GameScreen` подписан на `Flow<GameEntity>`; когда `status == FINISHED && winnerPlayerId != null` → `WinnerDialog`.
- **Long-press** — через `pointerInput.detectTapGestures` в `LongPressTextRow`. Не использовать `combinedClickable`, он плохо ложится на `Row` с весом.

## Стиль кода

- Kotlin official code style (см. `gradle.properties: kotlin.code.style=official`).
- Имена пакетов — `com.counter.game.<слой>.<экран>`. На уровне экрана — пара `<Screen>Screen` (composable) + `<Screen>ViewModel`.
- Где возможно — `data class State(...)` рядом с VM; в `combine(...)` потоков — выделить вспомогательные `transientFlow` (см. `NewGameViewModel`).
- Experimental Material3 API помечается `@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)` **точечно на функции**, а не `@file:OptIn`. Этот путь уже отлажен после сборки.
- Неиспользуемые импорты — удалять сразу, не копить.
- Не использовать `cd` в shell — только абсолютные пути.

## Что НЕ делать

- Не добавлять Hilt/Koin без явного запроса.
- Не вводить `dynamicColor`/`darkTheme`.
- Не использовать `Icons.Default.ArrowBack` (deprecated в новых Compose) — брать `Icons.AutoMirrored.Filled.ArrowBack`, если дойдёт до переименования.
- Не заводить `themes.xml` заново на Material3 — сейчас parent `android:Theme.Material.Light.NoActionBar` для statusBar/windowBackground, а MaterialTheme — через Compose. Смешивать не надо.
- Не править `.idea/` и `local.properties` — они в `.gitignore`.

## Сборка

- JDK: 17+ (локально использовался JDK 25, тоже работает).
- Android SDK: 37 (`compileSdk = release(37)`, `targetSdk = 37`).
- KSP: `2.2.10-2.0.2` (KSP1-билда для Kotlin 2.2.10 в Maven Central нет).
- Команды:
  - `./gradlew :app:assembleDebug` — сборка APK.
  - `./gradlew :app:testDebugUnitTest` — unit-тесты (правила движка).
  - `./gradlew :app:connectedDebugAndroidTest` — instrumentation-тесты (нужен эмулятор/устройство).

## Roadmap (короткая версия)

Полная версия — в корне в `task.md` (ТЗ) и в `.qwen/plans/925c7d80-b6a4-475a-99c7-ff8472c1c3cd.md` (план реализации). Кратко:

- ✅ MVP собран и работает (главное меню → игроки → новая игра → раунд → история → правила).
- 🟡 Полировка MVP: ~20 пунктов в `TODO.md` раздел 2.
- ⬜ v1.1: визуальный редактор правил, статистика (игры + игроки).
- ⬜ v2: распознавание карт через камеру, спец-правила.

## Известные особенности среды

- `todo_write` в Qwen Code в этой среде заблокирован политикой. Прогресс ведётся в файлах `task.md` и `TODO.md`.
- Шаблонные `MainActivity.kt`/`Theme.kt`/`Color.kt`/`strings.xml` и т.п. уже заменены — стартовые «Hello Android» / `Purple40` стёрты.