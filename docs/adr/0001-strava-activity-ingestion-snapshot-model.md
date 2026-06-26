# ADR 0001: Strava Activity Ingestion Uses Snapshot Model

## Status

Accepted

## Context

Sudolife is extending Strava account linking into a training insight foundation. The feature must import Strava history for future training analysis while also showing users their synced Strava activities. The scope includes activity list, activity detail, selected performance streams, manual sync, and scheduled polling.

Strava activity data can change after import. Users can edit names, activity type, distance, elapsed time, privacy, and other fields. Streams are heavier than summaries and may be fetched later. Sudolife also needs clear privacy behavior when a user unlinks Strava.

## Decision

Sudolife will use a snapshot model for imported Strava activity data.

Activity summaries are imported once and are immutable after first import. The uniqueness rule is one imported activity per Sudolife user and Strava activity id. Later syncs ignore repeated Strava activity ids instead of updating existing rows.

Activity details are fetched on demand when the user opens an activity detail view. If the detail snapshot has not been persisted yet, Sudolife fetches it from Strava, persists it as first seen at detail-fetch time, and returns it. Once persisted, the detail snapshot is immutable.

Performance streams are imported with Strava `resolution=low` for selected performance metrics: time, distance, velocity or pace, heart rate when available, cadence when available, and watts when available. Route coordinates are excluded. Streams are retried until first successful import, then become immutable.

Run, Walk, and Ride activities import summaries and selected performance streams. Weight Training imports summary data only.

Initial sync imports the last 7 months of eligible activities. Later syncs use the last successful summary sync time with a small overlap window and ignore duplicates. The summary cursor advances only after all summary pages in a sync run complete successfully.

Activity sync is two-phase. Summary import runs first. Stream enrichment then runs in configurable batches. Manual sync imports summaries and one stream batch immediately. Scheduled polling uses the same application use case with a configurable interval that defaults to 1 day. Opening an activity detail can prioritize that activity's stream import while still respecting Strava rate-limit stop behavior.

When Strava rate limits are reached, Sudolife persists progress, marks the relevant status as rate limited, stops gracefully, and continues in a later manual or scheduled sync.

Granting `activity:read` makes a Strava account sync-enabled and triggers the initial import asynchronously after linking or permission upgrade succeeds. Accounts without `activity:read` show a permission-upgrade-required state and do not silently fail sync.

Unlinking Strava synchronously deletes imported Strava activities, details, and streams for that user in the MVP.

## Consequences

Sudolife will not mirror later Strava edits for already imported activities. This avoids complex update semantics and keeps imported training history stable, but it means Sudolife can show stale values if the user edits the activity in Strava after import.

The detail and stream snapshots may be captured later than the summary snapshot. This is accepted because the product values a stable Sudolife copy over strict Strava mirroring.

Future AI or analytics features must treat imported data as Sudolife snapshots, not as a live mirror of Strava.

Privacy behavior is simple for the MVP: unlinking removes imported Strava data before the unlink request completes. If deletion becomes slow, asynchronous cleanup can be reconsidered in a later ADR.

