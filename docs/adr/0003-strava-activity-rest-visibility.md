# ADR 0003: Strava Activity REST API Exposes Snapshots Without Stream Samples

## Status

Accepted

## Context

Sudolife needs backend REST endpoints for synced Strava activities before adding frontend screens. The API must support future activity history views and training insight work while keeping sensitive stream samples internal for the MVP.

The imported data follows a snapshot model. The API should make ownership, pagination, unit behavior, stream visibility, and user-safe statuses explicit.

## Decision

Sudolife will expose Strava activity ingestion through backend REST endpoints only in the MVP.

The endpoint intent is:

- `POST /api/strava/sync` triggers manual sync for the authenticated user.
- `GET /api/strava/status` returns existing link status plus permission state, sync statuses, last sync times, imported activity count, and streams-ready activity count.
- `GET /api/strava/activities` lists imported activity summaries.
- `GET /api/strava/activities/{activityId}` returns an activity detail by Sudolife internal activity id.
- `POST /api/strava/link` remains the link or permission-upgrade entry point and requests `activity:read` for this feature.

Activity list responses will include the Sudolife internal activity id and the source Strava activity id. Detail routes will use the Sudolife internal id. If an authenticated user requests an activity id that does not belong to them, the API will return not found.

The activity list will support page and size pagination and will always sort newest first. Unsupported Strava activity types will be ignored during import.

Sudolife will store both normalized Sudolife activity type and raw Strava sport type. The MVP normalized activity types are `RUN`, `WALK`, `RIDE`, and `WEIGHT_TRAINING`.

Activity list fields are name, sport type, start date and time, distance, moving time, average speed, average pace per kilometer when distance is present, and stream status.

Activity detail fields are name, sport type, start date and time, distance, moving time, elevation gain, average speed, average pace per kilometer when distance is present, max speed when available, average heart rate when available, max heart rate when available, average cadence when available, average watts when available, calories when available, source Strava activity id, stream status, and available stream metric names.

The backend API will return raw metric values plus derived display fields such as pace per kilometer. Derived display fields are response projections and are not persisted training rollups.

Activity detail responses will not include raw performance stream samples in the MVP. They will expose stream status plus available metric names from persisted stream metadata.

If detail enrichment fails but an activity summary exists, the detail endpoint will return summary-based detail with a user-safe enrichment status instead of failing the whole request.

The stable user-safe failure reason codes are:

- `PERMISSION_UPGRADE_REQUIRED`
- `RECONNECT_REQUIRED`
- `STRAVA_RATE_LIMITED`
- `STRAVA_UNAVAILABLE`
- `SYNC_ALREADY_RUNNING`
- `UNKNOWN_SYNC_FAILURE`

## Consequences

The API gives future frontend screens enough information for status, list, and detail views without exposing large or sensitive stream payloads.

Future AI and analytics features can consume persisted stream samples internally without forcing those samples into the public REST contract.

Using internal activity ids for detail routes keeps Sudolife API ownership separate from Strava's provider identifiers while preserving source traceability in responses.

