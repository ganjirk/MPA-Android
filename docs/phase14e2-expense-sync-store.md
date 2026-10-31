# Phase 14E.2 — Backward-compatible Expenses sync store

## Changes
- Preserve the existing DataStore file `expenses` and key `expenses_v1`.
- Extend expense records with an optional remote ID and sync state.
- Keep the original six-column expense records readable. Decoded legacy records default to `LOCAL_ONLY`.
- Persist linked account ID and pending remote-delete IDs in the same encoded value, so data and sync metadata are saved together.
- Keep integer paise as the local money representation.
- Preserve remote ID and mark synced expenses `PENDING_UPDATE` when edited.
- Keep a remote-delete tombstone when a synced expense is deleted; deleting local-only data does not create a tombstone.
- This phase does not call the network and does not upload legacy data.

## Validation required before merge
Run locally from `D:\MPA-Android`:
```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Then inspect the Expenses screen and CSV export. Existing records must still appear. Do not enable network sync or automatically upload legacy data in this phase.

## Next
Phase 14E.3 will add explicit, account-bound manual sync. It must only upload newly created records marked `PENDING_CREATE` or records the user deliberately selected for migration, and must retain local data on any request failure.
