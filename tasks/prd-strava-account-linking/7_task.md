# Task 7.0: Add Strava REST Endpoints, Callback Redirects, and Security Rules

<critical>Read the prd.md and techspec.md files in this folder. If you do not read those files, your task will be invalidated.</critical>

## Overview

Add the driving REST adapter for Strava account management. This task exposes authenticated start/status/unlink endpoints, a public Strava callback endpoint protected by state validation, frontend success/failure redirects, and security configuration changes.

<requirements>
- Add `StravaAccountLinkController` under `adapter/driving/rest/strava/**`.
- Use `Authentication.getName()` as the current Sudolife user email.
- Add `POST /api/strava/link`, `GET /api/strava/callback`, `GET /api/strava/status`, and `DELETE /api/strava/link`.
- Permit unauthenticated access only to `/api/strava/callback`.
- Require authentication for all other Strava endpoints.
- Redirect callback success and failure to configured frontend URLs.
- Keep REST DTOs inside the adapter and never expose token data.
- Add `RestExceptionHandler` mappings for Strava JSON endpoint failures as needed.
</requirements>

## Subtasks

- [ ] 7.1 Add REST webmodel records for authorization URL and link status responses.
- [ ] 7.2 Implement `POST /api/strava/link` using authenticated email.
- [ ] 7.3 Implement `GET /api/strava/status` using authenticated email.
- [ ] 7.4 Implement idempotent `DELETE /api/strava/link` using authenticated email.
- [ ] 7.5 Implement `GET /api/strava/callback` with query parameters `code`, `scope`, `state`, and `error`.
- [ ] 7.6 Translate callback results into frontend success or failure redirects.
- [ ] 7.7 Update `SecurityConfig` to permit the callback and protect all other Strava endpoints.
- [ ] 7.8 Add exception handler mappings for user-safe Strava API endpoint responses.

## Implementation Details

Use `API Endpoints`, `Error handling`, and `Standards Compliance` in `techspec.md`. Controllers must only map boundary input/output and call use cases.

## Success Criteria

- Authenticated users can start linking, read status, and unlink.
- Unauthenticated users cannot call account-management endpoints.
- Strava callback is reachable without JWT but cannot complete without valid state.
- Callback redirects include stable success/failure outcome information and no sensitive values.

## Task Tests

- [ ] Unit tests (JUnit 5): mapper or redirect builder behavior if extracted into non-trivial helper classes.
- [ ] Integration tests (Spring Boot test, H2 as applicable): Web MVC tests for authentication requirements, response shapes, callback success redirect, callback failure redirects, and no token leakage in JSON responses or redirect URLs.

<critical>ALWAYS CREATE AND RUN THE TASK TESTS BEFORE CONSIDERING IT COMPLETE</critical>

## Relevant Files
- `src/main/java/com/sudolife/adapter/driving/rest/strava/StravaAccountLinkController.java`
- `src/main/java/com/sudolife/adapter/driving/rest/strava/webmodel/**`
- `src/main/java/com/sudolife/config/security/SecurityConfig.java`
- `src/main/java/com/sudolife/adapter/driving/rest/RestExceptionHandler.java`
- `src/test/java/com/sudolife/adapter/driving/rest/strava/StravaAccountLinkControllerWebMvcTest.java`
