# Phase 0 — Native Android Repository Audit

## 1. Objective

Rebuild MPA-Android as a proper native Android application.
Keep the existing Django MPA website/backend as the source of truth
for accounts and persistent user data where suitable.

This phase documents the current repository and proposes a cleanup.
It does not delete legacy files or change the master branch.

## 2. Current findings

- The tracked repository contains legacy Python/Kivy experiments and
  copied Django applications.
- No Gradle settings/build files or AndroidManifest.xml were found
  within the inspected depth.
- `git submodule status` reports that no submodule mapping exists in
  `.gitmodules` for `KivyMD`.
- The tracked tree contains old TodoApp, MyShop and LengthConversion
  code, alongside number-guessing and Pong experiments.
- This is a source-tree audit only. No Android build or runtime tests
  have been performed.

## 3. Target architecture

- Language: Kotlin.
- UI: Jetpack Compose and Material 3.
- Build: Gradle with the Android Gradle Plugin.
- Networking: HTTPS requests to the existing MPA Django service.
- Authentication: choose an API-compatible mechanism after auditing
  the current Django authentication and session/CSRF behavior.
- Authorization: enforce ownership and permissions on the server.
- Persistence: keep existing MPA user data in the Django backend.
- Local state: use Android storage only where appropriate; do not
  assume local game state is synchronized with the website.
- Testing: unit tests, UI tests and backend API/security tests.

No new backend, database, paid API or continuously running worker is
proposed for the initial native-app skeleton.

## 4. Proposed file disposition

Disposition meanings:
- REMOVE: obsolete implementation, after reference checks.
- REPLACE: introduce a native equivalent or modern project file.
- INVESTIGATE: check references, dependencies and Git metadata first.
- RETAIN: keep if it serves the new project or documents needed history.

### Root files

| Path | Proposed action | Reason |
|---|---|---|
| .gitignore | REPLACE | Add Android, Gradle, IDE and secret-file ignores |
| KivyMD | INVESTIGATE | Git reports a missing .gitmodules mapping |
| __init__.py | REMOVE | Legacy Python package marker, subject to reference check |
| dec2bin.py | INVESTIGATE | Check whether the utility is useful or referenced |
| icons.py | INVESTIGATE | Check references before deciding whether assets can be discarded |
| test.py | INVESTIGATE | Identify purpose and whether any useful tests exist |
| test.html | REMOVE | Legacy test page if no required references remain |
| static/circles.jpg | INVESTIGATE | Check visual usefulness and references |
| static/logo.jpg | INVESTIGATE | Check whether this is an approved MPA logo |
| static/numbers.jpeg | INVESTIGATE | Check references and whether the asset is still useful |
| static/pencils.jpg | INVESTIGATE | Check references and whether the asset is still useful |
| README.md | ADD | Document setup, architecture, API, security and testing |

### HomeScreen

| Path | Proposed action | Reason |
|---|---|---|
| HomeScreen/MPA.py | REMOVE | Replace Kivy entry point with a native Compose screen |
| HomeScreen/__init__.py | REMOVE | Legacy Python package marker |

### Number Guess and Pong

| Path | Proposed action | Reason |
|---|---|---|
| NumGuess/MPAApp.py | REMOVE | Legacy KivyMD implementation |
| NumGuess/mpa.kv | REMOVE | Kivy layout |
| NumGuess/Pong.py | INVESTIGATE | Compare with duplicate Pong implementation |
| NumGuess/Pong.kv | INVESTIGATE | Compare with duplicate Pong layout |
| NumGuess - Copy/NumGuessApp.py | REMOVE | Duplicate legacy implementation after comparison |
| NumGuess - Copy/numguess.kv | REMOVE | Duplicate Kivy layout |
| NumGuess - Copy/test.kv | REMOVE | Legacy experiment if no unique behavior is needed |
| Pong/Pong.py | INVESTIGATE | Compare before removing duplicate game logic |
| Pong/Pong.kv | INVESTIGATE | Compare before removing duplicate layout |

### LengthConversion

| Path | Proposed action | Reason |
|---|---|---|
| LengthConversion/__init__.py | REMOVE | Legacy Python package marker |
| LengthConversion/views.py | REMOVE | Replace server-rendered converter with native logic |
| LengthConversion/urls.py | REMOVE | Legacy Django routes |
| LengthConversion/templates/bkp.html | REMOVE | Legacy backup template after reference checks |
| LengthConversion/templates/lencon.html | REMOVE | Legacy server-rendered UI |

### MyShop

| Path | Proposed action | Reason |
|---|---|---|
| MyShop/__init__.py | REMOVE | Legacy Python package marker |
| MyShop/admin.py | REMOVE | Legacy Django admin registration |
| MyShop/apps.py | REMOVE | Legacy Django app configuration |
| MyShop/models.py | REMOVE | Legacy model; do not migrate or delete backend data |
| MyShop/views.py | REMOVE | Legacy server-rendered views |
| MyShop/urls.py | REMOVE | Legacy routes |
| MyShop/tests.py | INVESTIGATE | Preserve useful test cases or security findings |
| MyShop/migrations/0001_initial.py | REMOVE | Legacy copied-app migration from this Android repository |
| MyShop/migrations/0002_rename_myshop_product_list.py | REMOVE | Legacy copied-app migration |
| MyShop/migrations/0003_delete_mainpage.py | REMOVE | Legacy copied-app migration |
| MyShop/migrations/0004_alter_product_list_image_url.py | REMOVE | Legacy copied-app migration |
| MyShop/migrations/0005_alter_product_list_image_url.py | REMOVE | Legacy copied-app migration |
| MyShop/migrations/__init__.py | REMOVE | Legacy migration package marker |
| MyShop/templates/products.html | REMOVE | Legacy server-rendered UI |
| MyShop/templates/cart4.svg | INVESTIGATE | Check asset references and possible reuse |
| MyShop/static/apples.jpg | INVESTIGATE | Check asset references and possible reuse |
| MyShop/static/numbers2.jpg | INVESTIGATE | Check asset references and possible reuse |

### TodoApp

| Path | Proposed action | Reason |
|---|---|---|
| TodoApp/Forms.py | REMOVE | Legacy Django form |
| TodoApp/__init__.py | REMOVE | Legacy Python package marker |
| TodoApp/admin.py | REMOVE | Legacy Django admin registration |
| TodoApp/apps.py | REMOVE | Legacy Django app configuration |
| TodoApp/models.py | REMOVE | Legacy copied-app model; protect backend data |
| TodoApp/views.py | REMOVE | Legacy server-rendered views |
| TodoApp/urls.py | REMOVE | Legacy routes |
| TodoApp/tests.py | INVESTIGATE | Retain useful test scenarios before removal |
| TodoApp/migrations/0001_initial.py | REMOVE | Legacy copied-app migration from this Android repository |
| TodoApp/migrations/__init__.py | REMOVE | Legacy migration package marker |
| TodoApp/templates/demoapp1/index.html | REMOVE | Legacy server-rendered UI |
| TodoApp/static/demoapp1/styles.css | REMOVE | Legacy web styling |

### Legacy Bootstrap assets

Proposed removal after confirming no retained file references them:

- TodoApp/static/demoapp1/bs/css/bootstrap-theme.css
- TodoApp/static/demoapp1/bs/css/bootstrap-theme.css.map
- TodoApp/static/demoapp1/bs/css/bootstrap-theme.min.css
- TodoApp/static/demoapp1/bs/css/bootstrap-theme.min.css.map
- TodoApp/static/demoapp1/bs/css/bootstrap.css
- TodoApp/static/demoapp1/bs/css/bootstrap.css.map
- TodoApp/static/demoapp1/bs/css/bootstrap.min.css
- TodoApp/static/demoapp1/bs/css/bootstrap.min.css.map
- TodoApp/static/demoapp1/bs/css/cerulean.min.css
- TodoApp/static/demoapp1/bs/css/flatly.min.css
- TodoApp/static/demoapp1/bs/css/united.min.css
- TodoApp/static/demoapp1/bs/fonts/glyphicons-halflings-regular.eot
- TodoApp/static/demoapp1/bs/fonts/glyphicons-halflings-regular.svg
- TodoApp/static/demoapp1/bs/fonts/glyphicons-halflings-regular.ttf
- TodoApp/static/demoapp1/bs/fonts/glyphicons-halflings-regular.woff
- TodoApp/static/demoapp1/bs/fonts/glyphicons-halflings-regular.woff2
- TodoApp/static/demoapp1/bs/js/bootstrap.js
- TodoApp/static/demoapp1/bs/js/bootstrap.min.js
- TodoApp/static/demoapp1/bs/js/npm.js

## 5. Mandatory checks before cleanup

1. Inspect the `KivyMD` Git entry and determine whether it is a gitlink,
   an ordinary tracked directory, or stale metadata.
2. Search all tracked files for imports, template/static references,
   image usage and duplicated implementations.
3. Compare duplicate Number Guess and Pong code before removing either.
4. Inspect the latest MPA Django backend before defining API contracts.
5. Confirm the existing authentication, user ownership rules, reminder
   delivery design and data persistence behavior.
6. Do not run migrations against the live MPA database as part of this
   repository cleanup.
7. Do not remove or rewrite the default branch or Git history.

## 6. API and security audit requirements

Before implementing authenticated native features, establish:

- Login/logout and session/token strategy supported by Django.
- Password reset flow that works for a native client.
- HTTPS-only API communication.
- Server-side per-user ownership checks for tasks, expenses and reminders.
- Input validation, authorization, error responses and API versioning.
- Reminder delivery constraints on the current hosting plan.
- Tests that prove one user cannot access another user's records.

## 7. Phase 0 acceptance criteria

- Current tracked-file inventory reviewed.
- Proposed file dispositions reviewed and approved.
- KivyMD Git metadata issue understood.
- References and duplicate implementations checked before deletion.
- Native architecture and backend API gaps documented.
- No legacy files deleted during this audit-only phase.
- Changes submitted on `phase0/native-android-audit` for review.
