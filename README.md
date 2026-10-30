# MPA Android

Native Android client for **MPA (Main Portal Application)**, built with Kotlin,
Jetpack Compose and Material 3.

This repository is being rebuilt from an older collection of Python/Kivy and Django
experiments. The rebuild follows a phased plan; each phase is a branch merged into
`master` through a pull request.

## Status

| Phase | Branch | Summary | State |
|---|---|---|---|
| 0 | `phase0/native-android-audit` | Audit of the legacy tree and target architecture (`docs/PHASE0_NATIVE_ANDROID_AUDIT.md`) | Merged |
| 1 | `phase1/legacy-cleanup` | Remove broken KivyMD submodule reference | Merged |
| 2 | `phase2/native-android-skeleton` | Gradle project, Compose theme | Merged |
| 3 | `phase3/native-home-navigation` | Home screen and bottom navigation | Merged |
| 4 | `phase4/core-logic-readme-ci` | Kotlin ports of legacy logic with unit tests, README, CI | Merged |
| 5 | `phase5/legacy-removal` | Remove the legacy Python/Kivy/Django files | Merged |
| 6 | `phase6/app-structure-navigation` | Split the UI into packages, Navigation Compose, string resources, Material icons | Merged |
| 7 | `phase7/games-utilities-screens` | Number Guess game, Length and Binary converters, ViewModels | Merged |
| 8 | `phase8/pong-game` | Two-player Pong on a Compose Canvas, ported from the Kivy game | Merged |
| 9 | `phase9/local-tasks` | Local to-do list on the Productivity tab, saved on the device with DataStore | Merged |
| 10 | `phase10/account-login` | Sign in with the MPA website account (JWT), Account tab | Merged |
| 11 | `phase11/website-parity-units-guess` | Unit converter with all 8 website categories; Number Guess levels, chances and stats | Merged |
| 12 | `phase12/expense-tracker` | Expense Tracker on the Productivity tab, matching the website: categories, monthly budget, breakdown, CSV export | In review |

Games (Number Guess with levels, Pong), Utilities (unit converter and Binary converter), Productivity (local
to-do list and Expense Tracker) and Account (sign in with the MPA website account) are implemented. Tasks and scores
are stored only on the device; reminders and syncing with the MPA backend are not built yet.

## Account and the MPA backend

The app signs in against the MPA website's JSON API (`/api/v1/`, see `docs/android-api-phase-10a-audit.md`
in the website repo): `POST auth/token/`, `POST auth/token/refresh/` and `GET account/`.

- HTTPS only; redirects are not followed; nothing is logged.
- The access and refresh tokens are encrypted with an Android Keystore key and kept out of backups.
- Signing out forgets the tokens on the device. The server has no revoke endpoint yet.
- Signing in uploads nothing. Local tasks are never imported or overwritten automatically.
- Create account and Forgot password open the website.

## Requirements

- Android Studio (current stable) or the command line with the Android SDK installed
- JDK 25 (see `gradle/gradle-daemon-jvm.properties`; the Gradle toolchain resolver can download it)
- `minSdk 26`, `targetSdk 37`

## Build and test

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew lintDebug           # Android lint
./gradlew assembleDebug       # debug APK
```

CI (`.github/workflows/android.yml`) runs the same three tasks on every pull request.

## Project layout

```
app/src/main/java/com/grk/mpa/app/
  MainActivity.kt   Activity entry point only
  core/             Pure Kotlin logic with no Android dependencies
    UnitConversion.kt   The website's 8 unit categories and the converter
    BinaryConverter.kt
    NumberGuessGame.kt, GuessLevel.kt, GuessStats.kt   Number Guess rules, levels and stats
    NumberFormatter.kt
    PongGame.kt     Pong physics and the fixed-step clock
    Task*.kt        Task model, list operations and the storage text format
    Money.kt, Expense*.kt   Expense Tracker rules (paise amounts, budgets, breakdown, CSV) and storage format
  data/
    api/            MpaApi, its HttpURLConnection client, models and results
    auth/           Encrypted token storage and AuthRepository
    TaskRepository.kt, DataStoreTaskRepository.kt   Local task storage
    ExpenseRepository.kt, DataStoreExpenseRepository.kt   Local expense storage
  ui/
    MpaApp.kt       Scaffold and bottom navigation bar
    navigation/     Destination enum, NavHost, icons
    home/           Home screen
    games/          Number Guess and Pong screens and ViewModels
    utilities/      Unit and Binary converter screens and ViewModels
    productivity/   Tasks and Expense Tracker screens and ViewModels
    account/        Sign-in and account screen and ViewModel
    theme/          Material 3 theme
app/src/test/       JVM unit tests
docs/               Design notes and audits
```

Anything under `core/` must stay free of Android imports so it can be unit tested on
the JVM.

## Legacy code

The old Python/Kivy and Django experiments (`HomeScreen`, `NumGuess`, `Pong`, `TodoApp`,
`MyShop`, `LengthConversion` and the root-level Python files) were removed in phase 5.
They remain in Git history (last present at commit `70ba929`). Number guess, length
conversion and binary/decimal conversion were ported to `core/`. Pong was ported in phase 8.

`static/` still holds four legacy images (`logo.jpg`, `circles.jpg`, `numbers.jpeg`,
`pencils.jpg`). They are kept until someone confirms whether `logo.jpg` is an approved
MPA logo and whether any of them should become app assets.
