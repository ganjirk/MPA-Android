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
| 4 | `phase4/core-logic-readme-ci` | Kotlin ports of legacy logic with unit tests, README, CI | In review |
| 5 | `phase5/legacy-removal` | Remove the legacy Python/Kivy/Django files | In review |
| 6 | `phase6/app-structure-navigation` | Split the UI into packages, Navigation Compose, string resources, Material icons | In review |
| 7 | `phase7/games-utilities-screens` | Number Guess game, Length and Binary converters, ViewModels | Merged |
| 8 | `phase8/pong-game` | Two-player Pong on a Compose Canvas, ported from the Kivy game | In review |
| 9 | `phase9/local-tasks` | Local to-do list on the Productivity tab, saved on the device with DataStore | In review |

Games (Number Guess, Pong), Utilities (Length and Binary converters) and Productivity (local
to-do list) are implemented. The Account tab is still a placeholder. Tasks are stored only on
the device; reminders and sync with the MPA backend are not built yet.

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
    LengthConverter.kt
    BinaryConverter.kt
    NumberGuessGame.kt
    NumberFormatter.kt
    PongGame.kt     Pong physics and the fixed-step clock
    Task*.kt        Task model, list operations and the storage text format
  data/             TaskRepository and its DataStore implementation
  ui/
    MpaApp.kt       Scaffold and bottom navigation bar
    navigation/     Destination enum, NavHost, icons
    home/           Home screen
    games/          Number Guess and Pong screens and ViewModels
    utilities/      Length and Binary converter screens and ViewModels
    productivity/   Task list screen and ViewModel
    common/         Shared composables (placeholder screen)
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
