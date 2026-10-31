# Phase 14D — Android Tasks synchronization

## Current behavior

Synchronization is manual: the user taps **Sync** on the Tasks screen. There is no background worker.

- The Android task ID remains the stable device-side ID. The API task ID is stored separately as `remoteId`.
- Existing four-column DataStore records remain readable and are treated as `LOCAL_ONLY`.
- On the first sync, the app verifies the signed-in account, imports that account's server tasks, and records the account ID locally.
- Existing device-only tasks are never uploaded automatically during first sync.
- Tasks created after the device has been linked to an account are marked `PENDING_CREATE` and uploaded on the next explicit sync. Each successful create is persisted immediately so a later failed create can be retried without duplicating earlier tasks.
- Changes to a linked task's completion state are marked `PENDING_UPDATE` and sent with PATCH.
- Deleting a linked task records a persistent deletion tombstone. It is cleared only after the API confirms deletion (or returns 404 because the task is already absent).
- Device-only tasks remain on the device. An explicit legacy-task migration/selection flow is not part of this phase.
- If a different MPA account is signed in, sync is blocked. The app does not silently reassign server IDs or upload data to another account.
- If the network fails, local tasks and pending changes remain stored for retry.

## Current data limitations

The Android Task model currently exposes title and completion only. The API's notes, priority and due date fields are not edited or overwritten by Android PATCH requests. They remain server-owned fields until a later UI phase adds them.

A remote task deleted outside Android is not automatically removed from the local list in this phase; the app preserves the local record rather than making a destructive assumption.

## Verification

Run from the Android repository root:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Lint is a separate check; an existing unrelated resource-lookup warning/error in the Reminders screen was observed during Phase 14D.1.
