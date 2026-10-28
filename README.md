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

The Games, Utilities, Productivity and Account tabs are still placeholders.

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
  MainActivity.kt   Compose shell: home screen and bottom navigation
  core/             Pure Kotlin logic with no Android dependencies
    LengthConverter.kt
    BinaryConverter.kt
    NumberGuessGame.kt
  ui/theme/         Material 3 theme
app/src/test/       JVM unit tests for core/
docs/               Design notes and audits
```

Anything under `core/` must stay free of Android imports so it can be unit tested on
the JVM.

## Legacy code

The `HomeScreen`, `NumGuess`, `Pong`, `TodoApp`, `MyShop` and `LengthConversion`
folders, and the root-level Python files, are legacy. They are kept for reference
until their removal is approved; see the file disposition table in the Phase 0 audit.
The legacy logic that is worth keeping has been ported to `core/`.

## Backend

The existing MPA Django service remains the source of truth for accounts and
persistent user data. No API integration exists yet. Before any authenticated feature is
built, the Django authentication model and API contract must be audited, including
per-user ownership checks (see section 6 of the Phase 0 audit).

## Secrets

Never commit signing keys or credentials. `*.jks`, `*.keystore`, `keystore.properties`
and `local.properties` are git-ignored.
