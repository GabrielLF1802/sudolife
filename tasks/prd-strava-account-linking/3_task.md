# Task 3.0: Implement Strava Account Linking Start Flow

<critical>Read the prd.md and techspec.md files in this folder. If you do not read those files, your task will be invalidated.</critical>

## Overview

Implement the authenticated backend flow that creates a single-use OAuth state and returns a Strava authorization URL. This task does not complete linking and must not create an active account link.

<requirements>
- Implement `StartStravaAccountLinkingUseCaseImpl`.
- Generate a backend state value and persist it with the authenticated user's email and expiration.
- Use only Strava scope `read`.
- Use `TimeProvider` for all time access.
- Delegate authorization URL construction to `StravaOAuthProvider`.
- Do not mark the user as linked when only the authorization URL is generated.
</requirements>

## Subtasks

- [ ] 3.1 Add a state generation strategy using secure random values.
- [ ] 3.2 Implement state expiration calculation through `TimeProvider`.
- [ ] 3.3 Persist the new authorization state before returning the URL.
- [ ] 3.4 Build a `StravaAuthorizationRequest` with `scope=read`.
- [ ] 3.5 Return `StravaAuthorizationUrlResult` without exposing state persistence internals.
- [ ] 3.6 Add logging at the use case or adapter boundary without secrets.

## Implementation Details

Use `API Endpoints`, `Integration Points`, and `Key Decisions` in `techspec.md`. The use case must remain independent of REST and Strava HTTP DTOs.

## Success Criteria

- Authenticated user email is bound to a persisted state.
- Returned URL contains only values created through the OAuth provider abstraction.
- No Strava account link is created during the start flow.
- The scope requested is exactly `read`.

## Task Tests

- [ ] Unit tests (JUnit 5): state is persisted, state expiration uses fixed time, OAuth provider receives `read`, link repository is not called, generated URL is returned.
- [ ] Integration tests (Spring Boot test, H2 as applicable): use case integration with persistence adapters and fake OAuth provider verifies persisted state and no active link.

<critical>ALWAYS CREATE AND RUN THE TASK TESTS BEFORE CONSIDERING IT COMPLETE</critical>

## Relevant Files
- `src/main/java/com/sudolife/application/service/strava/StartStravaAccountLinkingUseCaseImpl.java`
- `src/main/java/com/sudolife/application/service/strava/ports/provided/StartStravaAccountLinkingUseCase.java`
- `src/main/java/com/sudolife/application/service/strava/ports/required/StravaAuthorizationStateRepository.java`
- `src/main/java/com/sudolife/application/service/strava/ports/required/StravaOAuthProvider.java`
- `src/test/java/com/sudolife/application/service/strava/StartStravaAccountLinkingUseCaseImplUnitTest.java`
