# Review: Task 2.0 - Add Strava Persistence Schema and Repository Adapters

**Reviewer**: AI Code Reviewer
**Date**: 2026-05-12
**Task file**: 2_task.md
**Status**: APPROVED WITH OBSERVATIONS

## Summary

Task 2.0 adds the Strava persistence schema, JPA entities, Spring Data repositories, manual mappers, repository adapters, and focused persistence tests. The implementation keeps persistence concerns inside the driven adapter, maps entities manually, retains inactive historical rows, supports active lookups, and translates duplicate active athlete persistence violations into the application exception. The full Maven test suite passes.

## Files Reviewed

| File | Status | Issues |
|------|--------|--------|
| src/main/java/com/sudolife/application/model/user/User.java | OK | 0 |
| tasks/prd-strava-account-linking/tasks.md | Issues | 1 |
| src/main/java/com/sudolife/adapter/driven/persistence/strava/SpringDataStravaAccountLinkRepository.java | OK | 0 |
| src/main/java/com/sudolife/adapter/driven/persistence/strava/SpringDataStravaAuthorizationStateRepository.java | OK | 0 |
| src/main/java/com/sudolife/adapter/driven/persistence/strava/StravaAccountLinkPersistenceMapper.java | OK | 0 |
| src/main/java/com/sudolife/adapter/driven/persistence/strava/StravaAuthorizationStatePersistenceMapper.java | OK | 0 |
| src/main/java/com/sudolife/adapter/driven/persistence/strava/entitymodel/StravaAccountLinkEntity.java | OK | 0 |
| src/main/java/com/sudolife/adapter/driven/persistence/strava/entitymodel/StravaAuthorizationStateEntity.java | OK | 0 |
| src/main/java/com/sudolife/adapter/driven/persistence/strava/repository/StravaAccountLinkRepositoryJpaAdapter.java | OK | 0 |
| src/main/java/com/sudolife/adapter/driven/persistence/strava/repository/StravaAuthorizationStateRepositoryJpaAdapter.java | OK | 0 |
| src/main/resources/db/migration/V2__create_strava_account_linking_tables.sql | OK | 0 |
| src/test/java/com/sudolife/adapter/driven/persistence/strava/StravaAccountLinkPersistenceMapperUnitTest.java | OK | 0 |
| src/test/java/com/sudolife/adapter/driven/persistence/strava/StravaAuthorizationStatePersistenceMapperUnitTest.java | OK | 0 |
| src/test/java/com/sudolife/adapter/driven/persistence/strava/StravaFlywayMigrationIntegrationTest.java | OK | 0 |
| src/test/java/com/sudolife/adapter/driven/persistence/strava/StravaPersistenceAdapterIntegrationTest.java | OK | 0 |

## Issues Found

### Critical Issues

No critical issues found.

### Major Issues

No major issues found.

### Minor Issues

- `tasks/prd-strava-account-linking/tasks.md:6` - Task 2.0 remains unchecked even though the implementation for this task is present and tested. Suggested fix: mark Task 2.0 complete when accepting this implementation so the task tracker reflects the actual state.

## Positive Highlights

- The persistence adapter boundary is clean: JPA entities stay under `adapter/driven/persistence/strava/**`, and application ports only exchange domain models.
- The schema uses an `active_athlete_id` uniqueness strategy that works for historical inactive rows while enforcing one active athlete owner.
- Duplicate active athlete violations are translated to `DuplicateStravaAthleteOwnershipException`, keeping persistence exceptions from leaking through the application port.
- Mapper and repository integration tests cover active lookup by user, active lookup by athlete, inactive history, authorization state lookup, and duplicate active athlete rejection.
- Flyway migration coverage confirms the new migration applies with the existing migration chain.

## Standards Compliance

| Standard | Status |
|----------|--------|
| Architecture and Boundaries | OK |
| Java and Spring Boot Standards | OK |
| API/Messaging/Integrations | OK |
| Persistence and Migrations | OK |
| Tests | OK |

## Recommendations

1. Update `tasks/prd-strava-account-linking/tasks.md` to mark Task 2.0 complete after this review is accepted.
2. In a future hardening pass, extend the migration smoke test to assert the critical unique index exists, so Flyway schema drift is caught independently from Hibernate-generated integration-test DDL.

## Verdict

Approved with observations. The implementation satisfies Task 2.0 requirements and passes `./mvnw test`; only task-tracking cleanup and optional migration-test hardening remain.
