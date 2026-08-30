# 101 Counter

Android-приложение для ведущего настольной игры «101». Считает очки по правилам, ведёт историю раундов, поддерживает редактируемые правила подсчёта и статистику игроков.

## Возможности

- Ведение партий с 2–10 игроками.
- Настраиваемые правила подсчёта (JSON в редакторе).
- Итоговые правила-корректировки (например, обнуление при 101).
- Лимиты на карты в руке и в раунде.
- История раундов, статистика игроков и партий.
- Пауза / возобновление игры.
- Чёрно-белая тема, без Google-зависимостей.

## Сборка из исходников

Требования:
- JDK 17.
- Android SDK (compileSdk = 37).

```bash
./gradlew :app:assembleDebug      # APK без подписи, для отладки
./gradlew :app:testDebugUnitTest  # юнит-тесты
```

## Сборка release APK (для распространения)

Release-APK должен быть **подписан** собственным ключом. Сгенерируй keystore:

```bash
keytool -genkey -v \
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

> ⚠️ **Никогда не коммить `keystore.properties` и `*.jks` в репозиторий.**

Закодируй оба файла в base64:

```powershell
# PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore\101-counter-release.jks"))
[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore.properties"))
```

```bash
# bash
base64 -i keystore/101-counter-release.jks
base64 -i keystore.properties
```

После этого:

```bash
./gradlew :app:assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

## CI/CD (GitHub Actions)

В репозитории два workflow:

| Файл | Когда запускается | Что делает |
|---|---|---|
| `.github/workflows/ci.yml` | на каждый PR и push в `main` | тесты + сборка debug APK |
| `.github/workflows/release.yml` | на push тега `v*` (например `v1.0.0`) | сборка подписанного release APK + публикация в GitHub Releases |

### Настройка GitHub Secrets

Для релизов нужны **2 секрета** в **Settings → Secrets and variables → Actions**:

| Секрет | Что туда положить |
|---|---|
| `KEYSTORE_BASE64` | `base64 -i keystore/101-counter-release.jks` (содержимое keystore в base64) |
| `KEYSTORE_PROPERTIES_BASE64` | `base64 -i keystore.properties` (содержимое keystore.properties в base64) |

> **PowerShell-эквивалент:** `[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore\101-counter-release.jks"))` и `[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore.properties"))`.

Если хотя бы один из секретов не задан, workflow собирает APK, но **подписывает debug-ключом** (старое поведение). Такой APK нельзя публиковать как «latest», поэтому в workflow он помечается как prerelease.

### Как выпустить релиз

```bash
git tag v1.0.0
git push origin v1.0.0
```

Workflow `.github/workflows/release.yml` автоматически:
1. Декодирует keystore + keystore.properties из Secrets.
2. Соберёт подписанный release APK.
3. Проверит подпись через `apksigner`.
4. Создаст GitHub Release с прикреплённым APK и автогенерируемыми release notes.
5. Пометит релиз как `latest` (если keystore задан) или как `prerelease`.

## Лицензия

Личное использование. Распространение через GitHub Releases.
