# Handoff

## Current Focus

Continue the `grill-with-docs` session for `TRAINING_AI_NOTES.md`, centered on Adaptive Coaching for running plans.

The latest open question was:

> Does the user provide a deadline/date for the running goal?
>
> Options discussed:
> 1. Target date required.
> 2. Target date optional.
> 3. No target date in the MVP; Sudolife chooses the safe progression rate.
> 4. Required number of weeks.

The rationale: target distance and target pace are already part of the Running Goal, but the deadline changes whether a goal is feasible now, should be preserved as a Long-Term Running Goal, or should produce a Safe Running Milestone.

## Artifacts

- `TRAINING_AI_NOTES.md`: original AI training/planning notes being grilled.
- `CONTEXT.md`: glossary updated during the grilling session. It now includes Adaptive Coaching, Training Profile, Running Goal, Long-Term Running Goal, Safe Running Milestone, readiness concepts, recovery concepts, and coaching heart-rate zone language.
- No ADR has been created yet. Consider one later only if the architecture decision around AI provider, validation authority, or Strava profile permissions becomes hard to reverse, surprising, and trade-off driven.

## Decisions Captured

Reference `CONTEXT.md` for exact domain wording. Key decisions made in conversation:

- MVP product promise is Adaptive Coaching, not just static plan drafting.
- Adaptive Coaching is scoped to running only.
- The user provides User-Reported Readiness.
- Readiness can increase or reduce load, but only within backend safety limits.
- Injury Concern allows only Recovery Sessions.
- Low Readiness can still allow easy training.
- Recovery Sessions use heart-rate limits when available and RPE 1-3 when heart-rate history is unavailable.
- Perceived Effort uses a 1-10 scale and must be structured plan data.
- Training Stress Estimate is internal only, not shown as a scientific metric.
- Coaching Heart-Rate Zones prefer imported Strava athlete zones, fall back to age-based zones from the Training Profile, and forbid HR targets when neither source exists.
- `profile:read_all` is optional enrichment, not required for Adaptive Coaching.
- Training Profile must contain birth year before adaptive coaching plans can be generated.
- Age estimate is current year minus birth year.
- The exact heart-rate formula is intentionally undecided for now.
- Sufficient Running History means imported running activities in at least three of the last four weeks.
- Insufficient history still generates a Conservative Running Plan.
- Running Goal is target distance plus optional target pace.
- Unrealistic goals should be preserved as Long-Term Running Goals while Sudolife generates a Safe Running Milestone.

## Code/Repo Context

- Current code only captures Strava athlete id in `StravaAthleteResponse` and `StravaAccountLink`.
- Current imported activity data already includes distance, moving time, pace/speed, elevation, calories, heart rate, cadence, watts, and streams where available.
- Official Strava API reference indicates `/athlete/zones` requires `profile:read_all`; use this as the basis for Strava zone import decisions: https://developers.strava.com/docs/reference/
- The project follows hexagonal architecture and feature-first organization inside application/adapter layers. Keep any future training feature under `application/model/training`, `application/service/training`, and matching adapters.

## Suggested Skills

- `grill-with-docs`: continue the unresolved design interview and keep `CONTEXT.md` updated inline.
- `domain-modeling`: required by `grill-with-docs`; challenge terms and update glossary as concepts stabilize.
- `to-prd`: use after the grilling session stabilizes enough to turn the design into a PRD.
- `to-issues`: use after a PRD exists to create implementation slices.
- `implement`: use only after the design/PRD is clear enough to start coding.

## Next Recommended Question

Ask the user to choose how target timing works for Running Goals:

1. Target date required.
2. Target date optional.
3. No target date in MVP.
4. Required number of weeks.

Then update `CONTEXT.md` immediately if the answer resolves a durable domain term.
