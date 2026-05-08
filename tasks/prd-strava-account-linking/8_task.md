# Task 8.0: Add End-to-End Integration Coverage and Observability Checks

<critical>Read the prd.md and techspec.md files in this folder. If you do not read those files, your task will be invalidated.</critical>

## Overview

Validate the complete Strava account linking feature across REST, application use cases, persistence, security, and fake Strava OAuth behavior. This task closes coverage gaps and verifies observability and sensitive-data handling.

<requirements>
- Add feature-level integration tests for the full link, status, reconnect, duplicate rejection, and unlink flows.
- Use deterministic test data only.
- Use fake or stubbed Strava OAuth behavior, not live Strava API calls.
- Verify no token values appear in API responses, redirects, or application logs captured by tests where practical.
- Verify relevant success/failure events are logged with safe fields.
- Run the full Maven test suite before marking the feature complete.
</requirements>

## Subtasks

- [ ] 8.1 Add a fake `StravaOAuthProvider` test configuration for feature integration tests.
- [ ] 8.2 Add full happy-path integration test: register/login, start link, callback, status linked.
- [ ] 8.3 Add reconnect integration test for same user and same athlete replacing token metadata.
- [ ] 8.4 Add duplicate-athlete integration test across two Sudolife users.
- [ ] 8.5 Add unlink integration test proving active status is removed and historical row remains.
- [ ] 8.6 Add callback failure integration tests for invalid state, denied authorization, insufficient scope, and token exchange failure.
- [ ] 8.7 Add observability checks for safe log messages and absence of token leakage where feasible.
- [ ] 8.8 Run `./mvnw test` and fix any failures.

## Implementation Details

Use `Testing Approach`, `Monitoring and Observability`, and `Known Risks` in `techspec.md`. This task should not add new product behavior unless tests reveal a defect in earlier tasks.

## Success Criteria

- Feature-level tests cover core business outcomes from the PRD.
- All sensitive token values remain absent from HTTP responses, redirects, and checked logs.
- Duplicate athlete behavior is verified through the same public flow users exercise.
- Full test suite passes.

## Task Tests

- [ ] Unit tests (JUnit 5): add only if new test-only helpers contain non-trivial behavior.
- [ ] Integration tests (Spring Boot test, H2 as applicable): full feature flow tests, failure path tests, duplicate ownership tests, unlink history tests, and safe observability tests.

<critical>ALWAYS CREATE AND RUN THE TASK TESTS BEFORE CONSIDERING IT COMPLETE</critical>

## Relevant Files
- `src/test/java/com/sudolife/StravaAccountLinkingFlowIntegrationTest.java`
- `src/test/java/com/sudolife/helper/StravaTestHelper.java`
- `src/test/java/com/sudolife/adapter/driving/rest/strava/**`
- `src/test/java/com/sudolife/adapter/driven/persistence/strava/**`
- `src/test/java/com/sudolife/application/service/strava/**`
