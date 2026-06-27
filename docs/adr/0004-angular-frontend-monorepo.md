# ADR 0004: Angular Frontend Lives in the Backend Repository

## Status

Accepted

## Context

Sudolife needs a frontend for the athlete user to log in, connect Strava, view sync status, list imported activities, and open activity details. The frontend and backend will have separate build and deployment pipelines, but keeping them in one repository preserves product-level cohesion while the application is still small.

## Decision

Sudolife will add an Angular frontend to this repository as a monorepo. The Angular app will be the athlete-facing web application for the MVP and will consume the Spring Boot REST API under `/api/**`.

The Angular workspace will live under `frontend/`, keeping frontend tooling and dependency lockfiles separate from the Spring Boot backend while sharing the same Git repository.

The MVP will use Angular's standard application stack with standalone components, Angular Router, HttpClient, and local state through Angular primitives such as signals. Sudolife will not introduce NgRx in the MVP unless shared frontend state becomes complex enough to justify it.

The backend and frontend will build and deploy separately. The monorepo is a source organization choice, not a requirement to ship both applications as one artifact.

## Consequences

The repository will contain two independently deployable applications. Tooling, CI, and documentation should treat backend and frontend commands separately.

Avoiding NgRx keeps the MVP smaller and easier to change, but future frontend features may introduce a dedicated state-management layer if cross-screen state coordination becomes difficult.
