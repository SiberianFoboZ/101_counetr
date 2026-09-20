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
- **Режимы ввода раунда:** `RoundInputViewModel.InputMode { CARDS, MANUAL }`. Переключатель `SingleChoiceSegmentedButtonRow` в `RoundInputScreen` над списком проигравших. В `MANUAL` движок правил не вызывается, `round_cards` не пишутся, `delta_score` берётся из `RoundInput.manualDelta: Map<Long, Int>?` напрямую; в `raw_input_json` пишется `{"mode":"manual","delta":N}`. При смене режима уже введённые данные другой корзины сбрасываются.
- **Snackbar между экранами:** «Раунд N сохранён» показывается на `GameScreen`, не на `RoundInputScreen`. `RoundInputScreen` кладёт номер в `navBackStackEntry.savedStateHandle["saved_round_number"]` и сразу зовёт `onSaved()` (навигация `popBackStack()`). Snackbar по `limitMessage` (превышение лимита карт) живёт на `RoundInputScreen` — мгновенный фидбек, не блокирует навигацию. **Не звонить `showSnackbar` синхронно перед `onSaved()`** — `SnackbarHostState.showSnackbar` suspend-функция с дефолтным `SnackbarDuration.Short` ≈ 4 сек, навигация отложится.

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

Полная версия — в `.qwen/plans/925c7d80-b6a4-475a-99c7-ff8472c1c3cd.md` (план реализации).
`task.md` (ТЗ) и `TODO.md` (roadmap) в `.gitignore` — это рабочие файлы, в репозиторий не входят.

Краткий статус (на тег v1.1.2):

- ✅ **MVP** — собран и работает: главное меню → игроки → новая игра → раунд → история → правила.
- ✅ **v1.0.0** — правила (визуальный редактор), статистика (игры + игроки, детальный экран), итоговая корректировка (FINAL_ADJUSTMENT), CardLimits, миграция БД v1→v2.
- ✅ **v1.0.1** — bump версии, фикс CI/CD (переименование APK по маске `101-counter-v<tag>.apk`).
- ✅ **v1.1.0** — иконка приложения (чёрный фон + пика + «101»).
- ✅ **v1.1.1** — иконка-сетка (сердечко с градиентом).
- ✅ **v1.1.2** — фикс правил (дефолты дам/королей, обнуление при 101), ручной ввод дельты в раунде (`InputMode.MANUAL`) для большого числа игроков, мгновенный возврат из раунда через `savedStateHandle`.
- ⬜ **v1.2+** — backlogs из `TODO.md`:
  - Распознавание карт через камеру (CameraX + ML Kit).
  - Спец-правила (бонус за единственного короля/даму и т.п.).
  - Удаление/архивирование игроков через swipe (сейчас — иконка-кнопка).
  - Перевод UI ввода раунда в wizard (сейчас — segmented CARDS/MANUAL + LazyColumn).

## CI/CD

- `.github/workflows/ci.yml` — на каждый PR/push в `main`: тесты + debug APK.
- `.github/workflows/release.yml` — на push тега `v*`: подписанный release APK + GitHub Release.
- Подпись через `keystore.properties` + 2 Secrets в GitHub (`KEYSTORE_BASE64`, `KEYSTORE_PROPERTIES_BASE64`).
- Keystore НИКОГДА не коммитится — в `.gitignore` (`keystore.properties`, `keystore/`, `*.jks`, `*.keystore`).
- APK после сборки переименовывается в `101-counter-v<tag>.apk` шагом `Rename APK with version and tag`.
- Текущая версия приложения: `versionName = "1.0.1"`, `versionCode = 2`. Меняется в `app/build.gradle.kts` перед каждым релизом.
- Текущие планы — в README.md («Roadmap» внизу файла). В QWEN.md их не дублируем.

## Известные особенности среды

- `todo_write` в Qwen Code в этой среде заблокирован политикой. Прогресс ведётся в файлах `task.md` и `TODO.md`.
- Шаблонные `MainActivity.kt`/`Theme.kt`/`Color.kt`/`strings.xml` и т.п. уже заменены — стартовые «Hello Android» / `Purple40` стёрты.
- `TODO.md`, `task.md`, `.idea/`, `local.properties` — в `.gitignore`, не отслеживаются.