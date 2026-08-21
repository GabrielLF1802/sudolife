# ADR 0009: Account Deletion Uses Hard Delete

## Status

Accepted

## Context

Sudolife needs an MVP account-deletion flow that removes the account, imported Strava data, and pending sync work. The existing Strava unlink flow deliberately preserves an inactive link row for link history, but account deletion has a stronger privacy expectation and should not retain user e-mail, Strava athlete identity, training history, coaching profile, plans, imported snapshots, or account-owned jobs.

## Decision

Account deletion will be an irreversible hard delete. The authenticated user must provide their current password and confirm the destructive action before the request is submitted. After deletion, the same e-mail address can be registered again.

The delete-account use case will remove account-owned data for the user: user credentials, training profile, coaching profile, adaptive running plans and sessions, Strava authorization states, Strava account links including inactive historical rows, imported Strava summaries, details, streams, and Strava sync jobs. Open Strava sync jobs will be cancelled before their rows are removed so processors have a terminal state to observe if they race with deletion.

Strava deauthorization is best-effort. Sudolife will capture usable Strava access or refresh tokens before local deletion, delete local account-owned data even when Strava is unavailable, then attempt deauthorization without logging tokens. A failed external deauthorization must not block local account deletion.

## Consequences

Account deletion is not recoverable in the MVP and there is no grace period. This avoids adding disabled-account state, delayed purging, restore flows, and additional login rules before they are needed.

The implementation must not rely on database cascades alone because much of the current schema is keyed by `user_email` rather than foreign keys to `users`. The application should expose a provided delete-account port, use required ports for account-owned data deletion, and keep deletion orchestration in the application layer.

Existing stateless JWTs remain valid until expiration unless the user row is gone and authentication can no longer resolve the subject. Server-side JWT revocation remains outside the MVP decision recorded in ADR 0006.
