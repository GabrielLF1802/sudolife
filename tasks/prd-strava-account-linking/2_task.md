# Task 2.0: Add Strava Persistence Schema and Repository Adapters

<critical>Read the prd.md and techspec.md files in this folder. If you do not read those files, your task will be invalidated.</critical>

## Overview

Persist Strava account links and OAuth authorization states through driven persistence adapters. This task adds Flyway schema changes, JPA entities, Spring Data repositories, manual mappers, and repository adapter implementations.

<requirements>
- Add a Flyway migration for `strava_account_links` and `strava_authorization_states`.
- Retain historical link rows and enforce one active Strava athlete association.
- Keep JPA entities inside `adapter/driven/persistence/strava/**`.
- Never expose JPA entities outside persistence adapters.
- Use manual mapping between persistence entities and application models.
- Support lookup by active user email, active athlete id, and state value.
</requirements>

## Subtasks

- [ ] 2.1 Create `V2__create_strava_account_linking_tables.sql` with link and state tables.
- [ ] 2.2 Add active uniqueness strategy for Strava athlete ownership and indexes for active user lookups.
- [ ] 2.3 Add `StravaAccountLinkEntity` and `StravaAuthorizationStateEntity`.
- [ ] 2.4 Add Spring Data repositories for link and state persistence queries.
- [ ] 2.5 Add manual persistence mappers.
- [ ] 2.6 Implement `StravaAccountLinkRepository` and `StravaAuthorizationStateRepository` adapters.
- [ ] 2.7 Translate database uniqueness violations into application-level duplicate athlete failures.

## Implementation Details

Use `Data Models`, `Known Risks`, and `Development Sequencing` in `techspec.md`. Pay special attention to the H2 versus PostgreSQL active uniqueness note.

## Success Criteria

- Flyway can create all Strava persistence structures.
- Repository adapters save and retrieve domain models without exposing entities.
- Historical inactive link records remain queryable while active lookups return only active links.
- Concurrent duplicate active athlete attempts are protected by persistence behavior.

## Task Tests

- [ ] Unit tests (JUnit 5): persistence mapper tests for all fields, including token metadata and inactive timestamps.
- [ ] Integration tests (Spring Boot test, H2 as applicable): repository adapter tests for save, active lookup by user, active lookup by athlete, historical inactive records, state lookup, and duplicate active athlete rejection.

<critical>ALWAYS CREATE AND RUN THE TASK TESTS BEFORE CONSIDERING IT COMPLETE</critical>

## Relevant Files
- `src/main/resources/db/migration/V2__create_strava_account_linking_tables.sql`
- `src/main/java/com/sudolife/adapter/driven/persistence/strava/**`
- `src/main/java/com/sudolife/application/service/strava/ports/required/StravaAccountLinkRepository.java`
- `src/main/java/com/sudolife/application/service/strava/ports/required/StravaAuthorizationStateRepository.java`
- `src/test/java/com/sudolife/adapter/driven/persistence/strava/**`
