# ADR 0007: API Rate Limiting Uses Bucket4j With In-Process Buckets First

## Status

Accepted

## Context

Sudolife needs rate limiting primarily to reduce brute-force abuse against login and registration, and secondarily to provide a basic generic API limit. The current deployment is a single Spring Boot backend behind the frontend Nginx proxy, so a separate Spring Cloud Gateway or Redis-backed distributed limiter would add operational complexity before there is a routing or horizontal-scaling boundary that requires it.

At the same time, authentication abuse rules should be more precise than a single IP counter. Login needs to consider both the source origin and the targeted credential without revealing whether an email address belongs to an existing user. Sudolife should rely on a proven rate-limit algorithm implementation instead of maintaining custom concurrency and refill logic by hand.

## Decision

Sudolife will implement API rate limiting inside the backend inbound REST adapter using Bucket4j. The first implementation will use in-process buckets, behind a small bucket registry or storage boundary that can later be backed by Redis without changing controller or filter behavior.

Login rate limiting will use a hybrid policy:

1. A pre-authentication IP limit counts every login request to protect password hashing, database access, and request handling.
2. A failed-login email limit counts failed attempts against the normalized target email across origins.
3. A failed-login email-and-IP limit counts failed attempts for a normalized target email from a specific origin.
4. A successful login clears only failed-login counters for that normalized email and email-origin pair. It does not clear the pre-authentication IP counter.

Registration will use separate limits by origin IP and normalized email. Login and registration will not also pass through the generic API rate limit. If a specific login or registration rule blocks a request, the external response remains generic for that endpoint: login returns `429 LOGIN_RATE_LIMIT_EXCEEDED`, and registration returns `429 REGISTER_RATE_LIMIT_EXCEEDED`.

The generic API rate limit will apply after the specific authentication limits. Unauthenticated routes are keyed by origin IP. Authenticated routes are keyed by authenticated user, with origin IP as a fallback. The health actuator endpoints and the Strava OAuth callback are excluded from the generic API limit.

If the Bucket4j bucket store becomes unavailable in a future external-store implementation, login and registration will fail closed with service unavailable semantics, while the generic API limiter will fail open to preserve availability.

## Consequences

The MVP avoids adding Redis or Spring Cloud Gateway before the deployment topology needs them, while still using a dedicated rate-limit library rather than custom counter logic. The future Redis migration remains explicit and localized.

The bucket storage boundary exists even though the first implementation is in-process. This is accepted because authentication rate limiting is a security-sensitive concern with clear future distributed-storage requirements when Sudolife adds multiple backend instances or needs buckets to survive backend deploys.

In-process buckets reset when the backend restarts and are not shared across instances. Before running multiple backend instances, Sudolife should back Bucket4j with Redis or move rate limiting to a dedicated edge or gateway component.
