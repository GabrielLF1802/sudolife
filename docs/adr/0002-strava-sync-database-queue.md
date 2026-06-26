# ADR 0002: Strava Sync Uses Database-Backed Queue

## Status

Accepted

## Context

Strava activity ingestion has work that should not run entirely inside request-response flows. Initial import after OAuth can take time, stream enrichment is rate-limit sensitive, and scheduled polling needs to refresh sync-enabled accounts without requiring user action.

The MVP deployment target is a single application node. A distributed scheduler lock is not required for the first version, but duplicate sync work should still be avoided within the application.

## Decision

Sudolife will use a database-backed queue for Strava sync work in the MVP.

The OAuth callback will enqueue the initial 7-month summary import after a user grants `activity:read`, then redirect immediately to a syncing status screen. Manual sync will not enqueue duplicate work when a sync job is already queued or running for the account; it will return current sync status instead.

The queue will support separate job types:

- Summary sync jobs coalesced per Strava account link.
- Stream enrichment jobs coalesced per activity.

Opening an activity detail can trigger a quick inline stream fetch when streams are missing. If Strava is rate-limited or unavailable, the detail endpoint will enqueue or promote a high-priority stream enrichment job and return available summary/detail data without streams.

The worker will process jobs in this priority order:

1. High-priority stream jobs from activity detail views.
2. Summary sync jobs.
3. Normal stream enrichment jobs.

Queued jobs will retry transient failures with backoff up to a configurable maximum attempt count, then mark the job failed with a user-safe reason. Rate-limit stops are resumable and should preserve progress for a later run.

If token refresh indicates Strava authorization is no longer valid, Sudolife will mark reconnect required and disable sync until the user relinks or upgrades permissions. Sudolife will not automatically unlink the Strava account.

Scheduled polling will use a single-node Spring scheduled worker with no distributed lock. Daily polling will enqueue sync work for sync-enabled, permission-ready accounts. The polling interval is configurable and defaults to 1 day.

## Consequences

Initial imports and stream enrichment are durable across request completion and can continue after the user leaves the linking flow.

The queue adds persistence and worker state to the MVP, but avoids fragile long-running OAuth callbacks and gives detail-priority stream enrichment a clear execution path.

Because the scheduler has no distributed lock, multi-node deployments can duplicate polling work and waste Strava rate limit. Before running multiple application instances, Sudolife should add a database lease, distributed lock, or move this worker to a single external process.

