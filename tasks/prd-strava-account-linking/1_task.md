# Task 1.0: Add Strava Application Contracts and Domain Models

<critical>Read the prd.md and techspec.md files in this folder. If you do not read those files, your task will be invalidated.</critical>

## Overview

Create the application-layer foundation for Strava account linking. This task defines pure domain models, commands, results, provided ports, required ports, and application exceptions without adding persistence, HTTP clients, or REST endpoints.

<requirements>
- Add Strava feature files under `application/model/strava/**` and `application/service/strava/**`.
- Keep domain models pure: no Spring, JPA, generated mapping framework, or forbidden Lombok annotations.
- Add provided ports for starting linking, completing callback processing, reading status, and unlinking.
- Add required ports for Strava account link persistence, authorization state persistence, Strava OAuth provider, and time access.
- Add application exceptions for invalid state, denied authorization, insufficient scope, duplicate athlete ownership, and external Strava authorization failures.
- Do not implement adapter behavior in this task.
</requirements>

## Subtasks

- [ ] 1.1 Read `prd.md` and `techspec.md` and confirm package placement follows the project hexagonal rules.
- [ ] 1.2 Add `StravaAccountLink` and `StravaAuthorizationState` domain models with intention-revealing methods for active/inactive state and consumed/expired state.
- [ ] 1.3 Add application command/result records for start linking, callback completion, status, unlinking, authorization URL creation, and Strava token authorization.
- [ ] 1.4 Add provided port interfaces for all Strava use cases.
- [ ] 1.5 Add required port interfaces for repositories, OAuth provider, and `TimeProvider`.
- [ ] 1.6 Add application exceptions with user-safe messages and stable failure codes where needed.
- [ ] 1.7 Add deterministic `StravaTestHelper` factory methods for domain objects and records.

## Implementation Details

Use the `System Architecture`, `Key Interfaces`, `Data Models`, and `Standards Compliance` sections in `techspec.md`. Keep all new files inside the application layer except test helpers.

## Success Criteria

- Application contracts compile and expose all use case boundaries needed by later tasks.
- Domain models have no framework annotations and do not depend on adapters.
- Exceptions belong to the application layer and contain no Strava token values.
- The task can be completed without creating database tables or REST endpoints.

## Task Tests

- [ ] Unit tests (JUnit 5): domain behavior for link activation/inactivation and state expiration/consumption.
- [ ] Integration tests (Spring Boot test, H2 as applicable): not required for this task because no adapter is implemented.

<critical>ALWAYS CREATE AND RUN THE TASK TESTS BEFORE CONSIDERING IT COMPLETE</critical>

## Relevant Files
- `src/main/java/com/sudolife/application/model/strava/StravaAccountLink.java`
- `src/main/java/com/sudolife/application/model/strava/StravaAuthorizationState.java`
- `src/main/java/com/sudolife/application/service/strava/**`
- `src/test/java/com/sudolife/helper/StravaTestHelper.java`
- `tasks/prd-strava-account-linking/prd.md`
- `tasks/prd-strava-account-linking/techspec.md`
