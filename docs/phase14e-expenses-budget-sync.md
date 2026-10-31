# Phase 14E — Android Expenses and Monthly Budgets

## Existing backend contract

The Django backend already exposes authenticated endpoints under `/api/v1/`:

- `GET /expenses/?month=YYYY-MM` — list the signed-in user's expenses, optionally filtered to a month.
- `POST /expenses/` — create an expense.
- `GET /expenses/{id}/`, `PATCH /expenses/{id}/`, `DELETE /expenses/{id}/` — access or change an expense owned by the signed-in user.
- `GET /expenses/summary/?month=YYYY-MM` — monthly total, budget, remaining budget, and category breakdown.
- `GET /budgets/` — list the signed-in user's monthly budgets.
- `POST /budgets/` — create or replace the budget for a month. `month` must be the first day of that month (`YYYY-MM-01`).

All calls require the existing bearer-token authentication. Ownership is assigned by the server; clients must not send a user ID.

## Phase 14E.1 scope

This phase adds serializable request/response models, API interface methods, the OkHttp client calls, and local HTTP-server tests for the endpoint paths, JSON fields, bearer headers, month query, and validation. It does not change the current device-only Expenses UI or its DataStore format.

Money values cross the API as decimal rupee strings and must be converted to/from the app's integer-paise representation by the next sync layer. Do not use floating-point arithmetic for money.

## Follow-on phases

- **14E.2:** Add a backward-compatible sync-aware expense/budget store. Preserve existing device-only data; do not silently upload it.
- **14E.3:** Add explicit, account-bound manual sync for expenses, including safe local/remote ID mapping and retry handling.
- **14E.4:** Sync monthly budgets and use the server summary to verify monthly totals.
- **14E.5:** Add user-visible sync states and perform on-device round-trip testing.

## Safety rules

- Never automatically upload pre-existing device-only expenses or budgets.
- Never mix data between MPA accounts; block account switching while a store is linked to another account.
- Do not erase local data after network errors or partial sync.
- Keep sync user-initiated until retry, conflict, and account ownership behavior is validated.
- Keep CSV export available.
