# Technical Specification Template

## Executive Summary

Implement Strava account linking as a new `strava` feature inside the existing single application and adapter layers. The application layer owns linking state, token lifecycle metadata, ownership validation, and use case decisions. Adapters own REST endpoints, Strava OAuth HTTP calls, persistence, configuration, redirects, and logging.

The first version should use Strava OAuth authorization code flow with only the `read` scope. The backend generates the authorization URL, stores a short-lived state bound to the authenticated user's email, handles the callback, exchanges the code for Strava authorization data, persists an active link with token metadata, and redirects to configured frontend success or failure URLs. Historical unlink records are retained while active uniqueness is enforced by persistence constraints and application checks.

## System Architecture

### Component Overview

- `application/model/strava/StravaAccountLink`: domain model for a Sudolife user to Strava athlete association, active status, token metadata, and link timestamps.
- `application/model/strava/StravaAuthorizationState`: domain model for pending OAuth state, initiating user email, expiration, and consumed state.
- `application/service/strava/StartStravaAccountLinkingUseCaseImpl`: creates a pending state and authorization URL.
- `application/service/strava/CompleteStravaAccountLinkingUseCaseImpl`: validates callback state, exchanges authorization code, checks scope, enforces athlete ownership, and persists link data.
- `application/service/strava/GetStravaAccountLinkStatusUseCaseImpl`: returns the current user's active Strava link state without token data.
- `application/service/strava/UnlinkStravaAccountUseCaseImpl`: revokes Strava authorization when possible and marks the link inactive.
- `application/service/strava/ports/provided/*`: provided ports for REST controllers.
- `application/service/strava/ports/required/StravaAccountLinkRepository`: required persistence port for active and historical link records.
- `application/service/strava/ports/required/StravaAuthorizationStateRepository`: required persistence port for pending OAuth states.
- `application/service/strava/ports/required/StravaOAuthProvider`: required external provider port for authorization URL creation, token exchange, token refresh, and deauthorization.
- `application/service/strava/ports/required/TimeProvider`: required time port because application code must not access system time directly.
- `adapter/driving/rest/strava/StravaAccountLinkController`: authenticated REST endpoints for start, callback, status, and unlink.
- `adapter/driven/api/strava/StravaOAuthAdapter`: Spring HTTP client for Strava OAuth endpoints.
- `adapter/driven/api/strava/StravaApiProperties`: Strava client id, client secret, redirect URI, frontend redirect URLs, timeouts, and token encryption flag.
- `adapter/driven/persistence/strava/*`: JPA entities, repositories, mapper, and repository adapters for links and states.
- `src/main/resources/db/migration/V2__create_strava_account_linking_tables.sql`: Flyway migration for link and state persistence.
- `adapter/driving/rest/RestExceptionHandler`: add Strava application exception mappings for API endpoints that return JSON.

High-level flow: authenticated REST call starts linking, application persists state, Strava redirects back with `code`, `scope`, and `state`, callback use case validates state and exchanges code, repository enforces active one-to-one athlete ownership, and controller redirects to frontend result URLs.

## Implementation Design

### Key Interfaces

```java
public interface StartStravaAccountLinkingUseCase {
    StravaAuthorizationUrlResult execute(StartStravaAccountLinkingCommand command);
}

public record StartStravaAccountLinkingCommand(String userEmail) {
}

public record StravaAuthorizationUrlResult(String authorizationUrl) {
}
```

```java
public interface CompleteStravaAccountLinkingUseCase {
    StravaCallbackResult execute(CompleteStravaAccountLinkingCommand command);
}

public record CompleteStravaAccountLinkingCommand(String state, String code, String scope, String error) {
}

public record StravaCallbackResult(boolean linked, String failureCode) {
}
```

```java
public interface StravaOAuthProvider {
    String buildAuthorizationUrl(StravaAuthorizationRequest request);
    StravaTokenAuthorization exchangeAuthorizationCode(String code);
    StravaTokenAuthorization refresh(String refreshToken);
    void deauthorize(String accessToken);
}
```

```java
public interface StravaAccountLinkRepository {
    Optional<StravaAccountLink> findActiveByUserEmail(String userEmail);
    Optional<StravaAccountLink> findActiveByAthleteId(Long athleteId);
    StravaAccountLink save(StravaAccountLink link);
}
```

### Data Models

Application records:

- `StravaLinkStatusResult(boolean linked, Long athleteId)`
- `UnlinkStravaAccountCommand(String userEmail)`
- `StravaAuthorizationRequest(String state, String redirectUri, String scope)`
- `StravaTokenAuthorization(Long athleteId, String accessToken, String refreshToken, Instant expiresAt, String scope)`

Domain model fields:

- `StravaAccountLink`: `id`, `userEmail`, `athleteId`, `accessToken`, `refreshToken`, `expiresAt`, `active`, `linkedAt`, `unlinkedAt`.
- `StravaAuthorizationState`: `state`, `userEmail`, `expiresAt`, `consumedAt`.

REST boundary DTOs:

- `StravaAuthorizationUrlResponse(String authorizationUrl)`
- `StravaLinkStatusResponse(boolean linked, Long athleteId)`
- No token values in any response.

Persistence schema:

- `strava_account_links`: `id`, `user_email`, `athlete_id`, `access_token`, `refresh_token`, `expires_at`, `active`, `linked_at`, `unlinked_at`.
- Add an active uniqueness strategy for `athlete_id`. For PostgreSQL use a partial unique index on `(athlete_id) where active = true`; for H2 integration tests use a compatible constraint strategy or repository-level duplicate handling with a PostgreSQL migration smoke test.
- Add index on `(user_email, active)`.
- `strava_authorization_states`: `state` primary key, `user_email`, `expires_at`, `consumed_at`.

Token storage:

- MVP may store tokens directly in the database with strict no-response and no-log handling.
- Prefer an `EncryptionService` required port with a Spring adapter using an environment-provided key before persistence. If implementation time is constrained, keep the port out of the first task and document encryption as the first hardening task after MVP.

### API Endpoints

- `POST /api/strava/link`: authenticated; returns `StravaAuthorizationUrlResponse`.
- `GET /api/strava/callback`: public to Strava, but validates `state`; query parameters are `code`, `scope`, `state`, and `error`; redirects to configured frontend success or failure URL.
- `GET /api/strava/status`: authenticated; returns `StravaLinkStatusResponse`.
- `DELETE /api/strava/link`: authenticated; idempotently unlinks the current user's active Strava account and returns `204`.

`SecurityConfig` must permit `/api/strava/callback` and require authentication for all other Strava endpoints.

## Integration Points

Strava OAuth uses:

- Authorization URL: `https://www.strava.com/oauth/authorize` with `client_id`, `redirect_uri`, `response_type=code`, `approval_prompt=auto`, `scope=read`, and `state`.
- Token exchange: `POST https://www.strava.com/api/v3/oauth/token` with `client_id`, `client_secret`, `code`, and `grant_type=authorization_code`.
- Refresh readiness: same token endpoint with `grant_type=refresh_token`; replace stored refresh token whenever Strava returns a new one.
- Deauthorization: `POST https://www.strava.com/oauth/deauthorize` with the current access token.

HTTP calls should use Spring's supported client stack already available through `spring-boot-starter-webmvc`, configured with short connect/read timeouts. Strava client secret, redirect URI, frontend redirect URLs, and client id belong in environment-backed properties under a new grouped `Strava` section in `application.properties`.

Error handling:

- Missing, expired, consumed, or mismatched state redirects with `failureCode=INVALID_STATE`.
- Strava `error` query parameter redirects with `failureCode=AUTHORIZATION_DENIED`.
- Token exchange failure redirects with `failureCode=TOKEN_EXCHANGE_FAILED`.
- Missing `read` scope redirects with `failureCode=INSUFFICIENT_SCOPE`.
- Athlete already active for another user redirects with `failureCode=ATHLETE_ALREADY_LINKED`.
- Do not log access tokens, refresh tokens, authorization codes, or client secret.

## Testing Approach

### Unit Tests

- `StartStravaAccountLinkingUseCaseImplUnitTest`: creates state, uses authenticated email, requests only `read`, does not create a link.
- `CompleteStravaAccountLinkingUseCaseImplUnitTest`: valid callback creates link; invalid/expired/consumed state fails; denied authorization fails; missing `read` scope fails; duplicate athlete for another user fails; same user reconnect refreshes token metadata.
- `UnlinkStravaAccountUseCaseImplUnitTest`: active link revokes and marks inactive; missing link is idempotent; deauthorization failure still prevents local active treatment only if product accepts local unlink over external revoke failure.
- `GetStravaAccountLinkStatusUseCaseImplUnitTest`: returns linked/unlinked status without token fields.
- Non-trivial mappers: Strava token response and persistence mapper tests.

Use deterministic helpers under `src/test/java/com/sudolife/helper/StravaTestHelper.java`. Keep AAA formatting and one-line Act sections.

### Integration Tests

- Persistence adapter integration tests for active lookup, historical rows, reconnect updates, and duplicate active athlete rejection under unique constraint.
- Web MVC tests for authenticated endpoint requirements, callback redirect behavior, and response shapes without token leakage.
- Use case integration tests for happy path and key failure paths, with a fake `StravaOAuthProvider`.
- Flyway migration smoke test for `V2__create_strava_account_linking_tables.sql`.
- Disable Flyway for integration tests that are not migration tests, consistent with project rules.

## Development Sequencing

### Build Order

1. Add application models, commands, results, exceptions, and provided/required ports so boundaries are stable.
2. Add Flyway migration, JPA entities, Spring Data repositories, manual mappers, and repository adapters.
3. Implement use cases with state validation, ownership checks, reconnect behavior, unlink behavior, and `TimeProvider`.
4. Add Strava OAuth adapter and configuration properties.
5. Add REST controller, callback redirects, security permit rule, and exception mappings.
6. Add unit, persistence integration, Web MVC, and migration tests.
7. Run the full Maven test suite.

### Technical Dependencies

- Strava API application credentials: client id and client secret.
- Registered Strava callback domain and redirect URI.
- Frontend success and failure redirect URLs.
- Environment variables for Strava configuration and optional token encryption key.

## Monitoring and Observability

Log at adapter boundaries with `@Slf4j`:

- `INFO`: link started, link completed, unlink completed, duplicate athlete rejected.
- `WARN`: invalid state, denied authorization, insufficient scope, token exchange failure, deauthorization failure.
- Include correlation-safe fields only: Sudolife user email or user id when available, Strava athlete id after token exchange, failure code, and external status code.
- Never log tokens, authorization codes, client secret, raw callback URL, or full Strava response.

Track counters through the current logging baseline until a metrics library is introduced: link success, failure by reason, unlink events, and duplicate-athlete rejections.

## Technical Considerations

### Key Decisions

- Use a dedicated `strava` feature under the existing application and adapter layers, preserving the single cohesive application architecture.
- Use the `read` Strava scope only because this version identifies the authenticated athlete and does not read activities.
- Persist historical link rows and mark inactive on unlink to support audit and observability while enforcing uniqueness only for active links.
- Use backend-generated state stored server-side instead of self-contained state only, because expiry, consumption, and authenticated user binding are easier to validate safely.
- Redirect callback outcomes to frontend URLs, while API-managed status remains available through `GET /api/strava/status`.
- Prefer direct Spring HTTP integration over adding a third-party Strava client because this feature uses only OAuth endpoints and requires strict token handling.

### Known Risks

- H2 compatibility with PostgreSQL partial unique indexes may require separate test strategy. Mitigation: use repository tests for behavior and a PostgreSQL-oriented migration smoke test.
- Token encryption may slip beyond MVP. Mitigation: isolate token persistence behind mappers/adapters so encryption can be added without changing use cases.
- Callback does not carry a JWT. Mitigation: state binds the callback to the initiating user email and is single-use with expiry.
- Strava deauthorization requires a valid access token. Mitigation: if expired, refresh first, then call deauthorize; if refresh fails, mark local link inactive and log a safe warning if product accepts local unlink as the user's requested outcome.
- Concurrent linking can race. Mitigation: enforce application checks plus database active uniqueness and translate constraint violations into duplicate-athlete failures.

### Standards Compliance

- Application depends on model and required ports only; adapters depend inward on application.
- Domain models remain under `application/model/strava/**` with no Spring or JPA annotations.
- Use cases live under `application/service/strava/**` and are exposed through provided ports.
- External Strava OAuth and persistence are required ports implemented by driven adapters.
- REST DTOs remain in `adapter/driving/rest/strava/webmodel/**`; application commands/results are records in the application layer.
- Manual mapping is required between Strava DTOs, persistence entities, and application models.
- Every schema change uses Flyway, starting with `V2__create_strava_account_linking_tables.sql`.
- Application time access goes through `TimeProvider`; tests depending on time use fixed provider configuration.
- No business logic belongs in the REST controller or Strava HTTP adapter.
- Tests follow naming suffixes `*UnitTest`, `*IntegrationTest`, and `*WebMvcTest`.

### Relevant and Dependent Files

- `AGENTS.md`
- `pom.xml`
- `src/main/resources/application.properties`
- `src/main/resources/db/migration/V1__create_users_table.sql`
- `src/main/java/com/sudolife/config/security/SecurityConfig.java`
- `src/main/java/com/sudolife/config/security/JwtAuthenticationFilter.java`
- `src/main/java/com/sudolife/adapter/driving/rest/RestExceptionHandler.java`
- `src/main/java/com/sudolife/adapter/driving/rest/user/controller/UserController.java`
- `src/main/java/com/sudolife/application/service/user/GetCurrentUserUseCaseImpl.java`
- `src/main/java/com/sudolife/application/service/user/ports/required/UserRepository.java`
- `src/main/java/com/sudolife/adapter/driven/persistence/user/repository/UserRepositoryJpaAdapter.java`
- `src/test/java/com/sudolife/AuthenticationFlowIntegrationTest.java`
- `src/test/java/com/sudolife/helper/UserTestHelper.java`
