# Task 4.0: Implement Strava Authorization Callback Completion Flow

<critical>Read the prd.md and techspec.md files in this folder. If you do not read those files, your task will be invalidated.</critical>

## Overview

Implement the application use case that completes Strava linking after Strava redirects back with OAuth callback parameters. This task validates state, handles denial, exchanges authorization code, validates granted scope, enforces one-to-one athlete ownership, and stores token lifecycle metadata.

<requirements>
- Implement `CompleteStravaAccountLinkingUseCaseImpl`.
- Reject missing, invalid, expired, or consumed state.
- Treat Strava callback `error` as authorization denied.
- Exchange authorization code through `StravaOAuthProvider`.
- Require granted `read` scope before linking.
- Reject an athlete already active for a different Sudolife user.
- Allow the same user to reconnect the same athlete and replace token metadata.
- Mark callback state consumed exactly once.
- Persist access token, refresh token, and expiration metadata without exposing them.
</requirements>

## Subtasks

- [ ] 4.1 Validate state existence, expiration, and consumed status.
- [ ] 4.2 Add callback denial handling before token exchange.
- [ ] 4.3 Exchange the authorization code and map Strava athlete id/token metadata into application models.
- [ ] 4.4 Validate scope parsing for space-delimited Strava scopes and require `read`.
- [ ] 4.5 Check active athlete ownership and reject ownership by another user.
- [ ] 4.6 Implement reconnect behavior for the same user and same athlete.
- [ ] 4.7 Save active link data and consume state in a transaction.
- [ ] 4.8 Convert duplicate persistence races into the same duplicate-athlete failure result.

## Implementation Details

Use `Error handling`, `Data Models`, `Known Risks`, and `Key Decisions` in `techspec.md`. Keep callback outcome as an application result that the REST controller can translate into frontend redirects.

## Success Criteria

- Successful callback creates or refreshes one active link for the initiating user.
- Invalid and denied callbacks do not create or update active links.
- Duplicate athlete ownership is rejected without revealing another user's details.
- Refresh token rotation readiness is preserved by replacing stored token metadata on reconnect.

## Task Tests

- [ ] Unit tests (JUnit 5): valid callback, invalid state, expired state, consumed state, denied authorization, token exchange failure, insufficient scope, duplicate athlete, same-user reconnect, and persistence race handling.
- [ ] Integration tests (Spring Boot test, H2 as applicable): complete flow with real repositories and fake OAuth provider for happy path and key failure paths.

<critical>ALWAYS CREATE AND RUN THE TASK TESTS BEFORE CONSIDERING IT COMPLETE</critical>

## Relevant Files
- `src/main/java/com/sudolife/application/service/strava/CompleteStravaAccountLinkingUseCaseImpl.java`
- `src/main/java/com/sudolife/application/service/strava/ports/provided/CompleteStravaAccountLinkingUseCase.java`
- `src/main/java/com/sudolife/application/service/strava/ports/required/StravaAccountLinkRepository.java`
- `src/main/java/com/sudolife/application/service/strava/ports/required/StravaAuthorizationStateRepository.java`
- `src/test/java/com/sudolife/application/service/strava/CompleteStravaAccountLinkingUseCaseImplUnitTest.java`
