# Task 6.0: Add Strava OAuth HTTP Adapter and Configuration

<critical>Read the prd.md and techspec.md files in this folder. If you do not read those files, your task will be invalidated.</critical>

## Overview

Implement the driven API adapter that talks to Strava OAuth endpoints and builds authorization URLs. This task adds environment-backed configuration, HTTP request/response DTOs, manual mapping, timeout handling, and safe error translation.

<requirements>
- Implement `StravaOAuthProvider` in `adapter/driven/api/strava/**`.
- Build authorization URLs for `https://www.strava.com/oauth/authorize`.
- Exchange authorization codes at `https://www.strava.com/api/v3/oauth/token`.
- Refresh tokens at the same token endpoint using `grant_type=refresh_token`.
- Deauthorize at `https://www.strava.com/oauth/deauthorize`.
- Add Strava configuration properties using environment variables.
- Use strict no-log/no-response handling for client secret, authorization codes, access tokens, and refresh tokens.
- Do not introduce a third-party Strava client library for this OAuth-only scope.
</requirements>

## Subtasks

- [ ] 6.1 Add `StravaApiProperties` with client id, client secret, backend redirect URI, frontend redirect URLs, base URLs, and timeouts.
- [ ] 6.2 Add a grouped `Strava` section in `application.properties`.
- [ ] 6.3 Implement authorization URL construction with `response_type=code`, `approval_prompt=auto`, `scope=read`, and state.
- [ ] 6.4 Implement token exchange request and response mapping.
- [ ] 6.5 Implement refresh token request and response mapping.
- [ ] 6.6 Implement deauthorization request.
- [ ] 6.7 Convert Strava HTTP failures into application exceptions without leaking sensitive values.
- [ ] 6.8 Add safe adapter boundary logging for status codes and failure categories only.

## Implementation Details

Use `Integration Points`, `Technical Dependencies`, and `Monitoring and Observability` in `techspec.md`. Use Spring HTTP support already available in the project instead of adding a new Strava SDK.

## Success Criteria

- Adapter produces valid Strava OAuth URLs and form-encoded OAuth requests.
- Token responses map athlete id, access token, refresh token, expiration, and scope.
- Secrets and tokens are never logged or exposed through exception messages.
- Configuration is environment-backed and follows existing `application.properties` section formatting.

## Task Tests

- [ ] Unit tests (JUnit 5): authorization URL parameter generation, token response mapping, refresh response mapping, and safe exception message behavior.
- [ ] Integration tests (Spring Boot test, H2 as applicable): HTTP adapter tests with mocked HTTP server or Spring test client verifying request paths, methods, form fields, and failure translation.

<critical>ALWAYS CREATE AND RUN THE TASK TESTS BEFORE CONSIDERING IT COMPLETE</critical>

## Relevant Files
- `src/main/java/com/sudolife/adapter/driven/api/strava/StravaOAuthAdapter.java`
- `src/main/java/com/sudolife/adapter/driven/api/strava/StravaApiProperties.java`
- `src/main/java/com/sudolife/adapter/driven/api/strava/dto/**`
- `src/main/resources/application.properties`
- `src/test/java/com/sudolife/adapter/driven/api/strava/**`
