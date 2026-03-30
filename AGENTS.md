# Project Rules

This project follows **Hexagonal Architecture (Ports & Adapters)** with **feature-first organization inside the
application and adapter layer**.

---

## 1) Architecture (non-negotiable)

There is: **one adapter** and **one application**. Features are expressed **inside those layers**, not as top-level
hexagons

### Application structure

```
application/
├── model/
│   ├── featureA/
│   │   ├── EntityA.java
│   │   └── ValueObjectA.java
│   └── featureB/
│       ├── EntityB.java
│       └── EnumB.java
│
├── service/
│   ├── featureA/
│   │   ├── FeatureAUseCaseImpl.java
│   │   └── ports/
│   │       ├── provided/
│   │       │   └── FeatureAUseCase.java
│   │       └── required/
│   │           └── FeatureASomethingProvider.java
│   └── featureB/
│       └── ...
│
└── repository/
    ├── FeatureARepository.java
    └── FeatureBRepository.java
```

### Adapter structure

Adapters are organized separately and depend on the application layer.

```
adapter/
├── driving/                             # inbound: enters the application
│   ├── rest/
│   │   └── feature-a/
│   │       ├── webmodel/
│   │       │   ├── FeatureARequest.java
│   │       │   └── FeatureAResponse.java
│   │       ├── FeatureAController.java
│   │       └── FeatureARestMapper.java
│   └── messaging/                      
│       └── feature-b/
│           ├── FeatureBEventListener.java
│           ├── FeatureBMessage.java
│           └── FeatureBMessageMapper.java
└── driven/                              # outbound: leaves the application
    ├── persistence/
    │   └── featureA/
    │       ├── entitymodel/              
    │       │   └── FeatureAEntity.java
    │       ├── FeatureAJpaRepository.java
    │       ├── FeatureAPersistenceMapper.java
    │       └── FeatureARepositoryAdapter.java
    └── api/                             # outbound HTTP clients, SDKs
        └── featureA/
            ├── dto/
            ├── PaymentProviderAClient.java
            ├── PaymentProviderAMapper.java
            └── PaymentProviderAAdapter.java

```

### Dependency rules

- CORRECT: `adapter/* -> application/*`
- WRONG: `application/* -> adapter/*`
- CORRECT: `service -> model`
- WRONG: `model -> service`

### Domain purity rules

- Domain models live in `application/model/**`
- Domain code must be **pure**
    - No Spring annotations
    - No JPA annotations
    - Only allow the following Lombok annotations (@ToString, @AllArgsConstructor. @RequiredArgsConstructor,
      @NoArgsConstructor, @Getter)
    - Avoid weak construction patterns like the lombok @Builder/@SuperBuilder, to avoid entities invalid state

This structure represents **one cohesive application, not multiple mini-applications**.
Hexagonal boundaries are enforced by **ports**, not by folder duplication.

## 2) Ports & Use Cases according to Clean Architecture

- Use cases live in `application/service/**`.
- Every use case is exposed via a **provided port** (interface).
- Naming rules:
    - Use intention-revealing names (`CreateSomethingUseCase`, `ExecuteSomethingUseCase`)
- Use cases accept **command DTOs** as input:
    - Commands live in the **application layer**
    - Prefer **records**
    - Pattern:

      ```java
      public interface SomeUseCase {
          Output execute(SomeCommand command);
      }
      ```

- Dependencies required by use cases are expressed as **required ports**:
    - External systems
    - Persistence
    - Message publishers
- Exceptions:
    - Use case exceptions belong to the application layer
    - Do not throw framework exceptions outside adapters

---

## 3) DTO Rules (Boundary Safety)

DTOs **must not leak across architectural boundaries**.

### Allowed

- **Application DTOs**:
    - Input commands
    - Output results returned by use cases

### Not Allowed

- Boundary-specific DTOs must stay inside their adapter:
    - REST request/response DTOs
    - Messaging payload DTOs
- Adapters are responsible for mapping:
    - Boundary DTO → application command
    - Application result → boundary DTO

---

## 4) Messaging (Consumer Rules)

This is a **consumer-first** messaging project.

- Entry points live in `adapter/driving/**`.
- Listener responsibilities:
    - Deserialize message
    - Validate message *shape* (required fields)
    - Map to an application command
    - Call a use case
    - Handle ack/nack

- **Idempotency is mandatory**:
    - Messages may be delivered more than once
    - Use cases must be idempotent
    - Prefer a persisted idempotency key

- No business logic in listeners.

---

## 5) Persistence

- Persistence is an adapter.
- JPA entities live only in `adapter/driven/persistence/**`.
- Domain models:
    - No persistence annotations
- Repositories:
    - Spring Data interfaces are allowed
- Mapping between entity and domain is **manual**, don't use mapping frameworks
- Never expose JPA entities outside persistence adapters.

---

## 6) Database Migrations

- Use **Flyway**.
- Rules:
    - Every schema change requires a migration
    - Migrations are immutable
    - Prefer small, incremental changes

---

## 7) Testing Rules

- **Always** run all tests after each task you finish
- **Always** suffixes tests with (*UnitTest, *IntegrationTest, *WebMvcTest) so they're easy to identify

- **Style**:
    - **AAA** (Arrange / Act / Assert)
    - Separate the 3 A's with a blank line
    - The Act session should only have one line (the method execution)
    - The tests should be small and focused
    - Avoid multiple mocks per test. But a test can have multiple stubs/spies
    - avoid having more than 4 lines for the Arrange if needed use meaningfully named factory methods to create the
      state

### Unit Tests

- Minimal setup
- Test method naming: **snake_case**
- Unit tests cover:
    - Use cases
    - Domain behavior
    - Non-trivial mappers

### Test Data Strategy

- Central helper:
    - `/src/test/**/helper/TestHelper.java`
- Feature helpers:
    - `/src/test/**/helper/<Feature>TestHelper.java`
- Rules:
    - No random data
    - Prefer deterministic factory methods
    - Test helper methods that use persistence must only be accessed through: `TestHelper`

### Integration Tests

- Disable flyway for integration tests
- Prefer **H2** for speed
- Integration tests cover:
    - Persistence adapters
    - Repository behavior
    - Flyway migrations (smoke test only)
- Integration tests exist for **every use case** (happy path + key failure paths)

---

## 8) Lombok Policy

- Lombok is allowed everywhere **except**:
    - Avoid setters on domain-heavy objects
- DTOs:
    - Prefer Java **records**

---

## 9) Mapping Policy

- Mapping is **manual by default** (not generated by frameworks)
- Prefer:
    - Explicit constructors
    - Static factory methods
- Introduce mapping frameworks only when necessary and documented

---

## 10) Logging & Configuration

- Logging:
    - Use `@Slf4j`
    - Log I/O at adapters
    - Avoid logging inside domain for normal flows

- Configuration:
    - Use Spring profiles
    - Configuration injected via environment variables

---

## 11) Never Do

- Never reference Spring or JPA in domain/application models
- Never leak adapter DTOs into the application layer
- Never place business logic in adapters
- Never assume single message delivery
- Never expose persistence entities
- Never use random test data

---

## 12) Clean Code Conventions

- Never write comments
- Method names must be meaningful and intention revealing
- Avoid methods with more than **3 parameters** (except constructors and factories)
- Avoid non-expressive names (e.g., `processor`, `info`, `data`, `manager`, `handler`)
- Small functions; explicit names; no nesting beyond 2 levels.
- Avoid excessive cleverness; choose clarity.
- Always insert a blank line immediately after the class declaration opening brace.

## 13) Important

- ALWAYS run all tests before finish a task
- Time and Clock access:
    - Application code must never access system time directly and must rely exclusively on TimeProvider.
    - Integration tests that assert or depend on time must use @Import(FixedTimeProvider.class).
    - Integration tests that do not depend on time must not import or reference FixedTimeProvider.
- Application Properties File:
    - Sections should be grouped by commented headers, framed with lines of # to create visual blocks (
      e.g., ############################## plus a # Section Name line).
    - Comments start with # and are used both for section headers and separators.
    - No indentation or multiline values; each property is on its own line.
  
## 14) API Documentation (Swagger/OpenAPI) Rules

- Swagger is an inbound delivery concern and belongs exclusively to the adapter layer.
- OpenAPI annotations (such as `@Operation`, `@Schema`, `@ApiResponse`, `@Tag`) are **strictly allowed only** in the `adapter/driving/rest/**` packages.
- Allowed locations for Swagger annotations:
  - REST Controllers (e.g., `FeatureAController.java`)
  - Boundary Web DTOs (e.g., `FeatureARequest.java`, `FeatureAResponse.java`)
- **Never** leak Swagger/OpenAPI annotations into the application layer:
  - Domain models (`application/model/**`) must remain 100% pure.
  - Application Commands and Output DTOs used by Use Cases must not contain `@Schema` or any API documentation metadata.
- Keep the documentation focused solely on the HTTP contract (endpoints, HTTP status codes, and web payload shapes) without exposing internal business rules or domain logic.
