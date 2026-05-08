# Task 5.0: Implement Strava Link Status and Unlink Flows

<critical>Read the prd.md and techspec.md files in this folder. If you do not read those files, your task will be invalidated.</critical>

## Overview

Implement the application use cases for reading the current user's Strava link status and unlinking the active account. Unlinking must revoke Strava authorization when possible, mark the link inactive, retain history, and be idempotent.

<requirements>
- Implement `GetStravaAccountLinkStatusUseCaseImpl`.
- Implement `UnlinkStravaAccountUseCaseImpl`.
- Return only non-sensitive status data.
- Allow only the authenticated owner to unlink their active link.
- Deauthorize through `StravaOAuthProvider` when a usable access token is available.
- If access token is expired, refresh first and then deauthorize when possible.
- Mark local link inactive and retain historical row.
- Treat repeated unlink requests as successful safe outcomes.
</requirements>

## Subtasks

- [ ] 5.1 Implement status lookup by authenticated user email.
- [ ] 5.2 Return linked status with athlete id only when an active link exists.
- [ ] 5.3 Implement unlink lookup by authenticated user email.
- [ ] 5.4 Refresh expired access tokens before deauthorization when a refresh token is available.
- [ ] 5.5 Call Strava deauthorization and handle external failure safely.
- [ ] 5.6 Mark active link inactive with `unlinkedAt` from `TimeProvider`.
- [ ] 5.7 Ensure no token values appear in status results, exceptions, or logs.

## Implementation Details

Use `API Endpoints`, `Error handling`, `Known Risks`, and `Monitoring and Observability` in `techspec.md`. The application layer decides local active state; adapters perform HTTP and persistence details.

## Success Criteria

- Status identifies linked versus unlinked state without sensitive data.
- Unlink removes active treatment while retaining historical link data.
- Repeated unlink requests do not fail.
- Deauthorization is attempted when possible without blocking local unlink on unrecoverable Strava token failure.

## Task Tests

- [ ] Unit tests (JUnit 5): linked status, unlinked status, unlink active link, repeated unlink, expired token refresh before deauthorize, deauthorization failure handling, no token fields in results.
- [ ] Integration tests (Spring Boot test, H2 as applicable): status and unlink use cases with real repositories and fake OAuth provider.

<critical>ALWAYS CREATE AND RUN THE TASK TESTS BEFORE CONSIDERING IT COMPLETE</critical>

## Relevant Files
- `src/main/java/com/sudolife/application/service/strava/GetStravaAccountLinkStatusUseCaseImpl.java`
- `src/main/java/com/sudolife/application/service/strava/UnlinkStravaAccountUseCaseImpl.java`
- `src/main/java/com/sudolife/application/service/strava/ports/provided/GetStravaAccountLinkStatusUseCase.java`
- `src/main/java/com/sudolife/application/service/strava/ports/provided/UnlinkStravaAccountUseCase.java`
- `src/test/java/com/sudolife/application/service/strava/GetStravaAccountLinkStatusUseCaseImplUnitTest.java`
- `src/test/java/com/sudolife/application/service/strava/UnlinkStravaAccountUseCaseImplUnitTest.java`
