# Sudolife Context

## Glossary

### Strava Account Link

An authorization relationship between a Sudolife user and a Strava athlete. The link stores the Strava athlete identity and token lifecycle metadata needed to call Strava on behalf of that user.

### Activity Sync

The process that imports eligible Strava activity summaries into Sudolife for a linked account with `activity:read` permission. Activity sync uses a 7-month initial window, then a cursor with an overlap window for later syncs.

### Sync-Enabled Strava Account

A Strava account link that has granted `activity:read` and is eligible for manual sync and scheduled polling. Granting `activity:read` enables sync and starts the initial import asynchronously.

### Activity Summary

The first persisted snapshot of a Strava activity from activity listing data. Activity summaries are immutable after first import and are unique per Sudolife user and Strava activity id.

### Activity Detail

An optional enriched snapshot of a Strava activity fetched when the user opens the activity detail view. Activity details are persisted as first seen at detail-fetch time and are immutable after first successful enrichment.

### Performance Streams

Low-resolution Strava activity streams for training-relevant samples: time, distance, velocity or pace, heart rate when present, cadence when present, and watts when present. Performance streams exclude route coordinates.

### Stream Enrichment

The process that imports performance streams for eligible activities after summaries have been imported. Stream enrichment happens in batches, can be prioritized by opening an activity detail, retries until successful, and treats persisted streams as immutable.

### Snapshot Model

The data consistency model for imported Strava activities. Sudolife stores first-seen copies of summaries, details, and streams instead of mirroring later edits from Strava.

### Permission Upgrade Required

A Strava account state where the user is linked but has not granted `activity:read`. Sudolife must show that activity sync requires a Strava permission upgrade instead of silently failing sync.

### Reconnect Required

A Strava account state where Sudolife can no longer refresh authorization with Strava. Sync is disabled until the user reconnects or upgrades the Strava account link.

### Stream Status

The user-safe state of performance stream enrichment for an activity. The activity detail API exposes stream status and available stream metric names, but not raw stream samples.

### Sync Job

A database-backed unit of Strava sync work processed by the scheduler. Summary sync jobs are coalesced per Strava account link, and stream enrichment jobs are coalesced per activity.

### Sudolife Activity Type

The normalized activity type used by Sudolife product logic. Strava imports support `RUN`, `WALK`, `RIDE`, and `WEIGHT_TRAINING` in the MVP while also retaining the raw Strava sport type for traceability.
