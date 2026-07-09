# Training AI Notes

## Context

Sudolife already imports Strava activity summaries, optional detail snapshots, and internal performance streams. The imported data is enough to support an MVP for training analysis and workout plan generation.

Available training-relevant data includes:

- Activity type, start date, distance, moving time, pace, speed, elevation, calories, heart rate, cadence, and watts when available.
- Internal stream samples for time, distance, velocity, heart rate, cadence, and watts when Strava provides them.
- A snapshot model where imported activity data is treated as Sudolife's stable copy, not as a live mirror of Strava.

## Recommendation

Use an open-source or open-weight LLM as an assistant for interpretation and workout plan generation, but keep the backend as the authority for calculations, safety limits, validation, and persistence.

The recommended MVP approach is structured RAG, not fine-tuning. The backend should retrieve the user's recent training data, calculate deterministic training metrics, build a structured `TrainingSnapshot`, and send that snapshot to the LLM as context.

Fine-tuning can improve the model's style, schema discipline, and general planning tendencies later, but it does not replace user-specific context. A trained model still does not know the user's latest activities, availability, objective, fatigue signals, or recent gaps unless the backend provides that information at request time.

The recommended flow is:

```text
Backend loads imported activities
Backend calculates deterministic training context
Backend sends structured context to the LLM
LLM proposes a structured workout plan
Backend validates, adjusts, or rejects the plan
Backend saves or returns the accepted plan
```

In this flow, RAG means giving the LLM a precise, backend-generated context package before it generates a plan. It does not mean letting the LLM query the database directly.

## Backend Responsibilities

The backend should calculate the metrics that must be deterministic and auditable before calling the LLM:

- Weekly volume.
- Training frequency.
- Recent long activity distance.
- Average pace or speed.
- Basic intensity distribution when heart rate, pace, or watts are available.
- Recent consistency and gaps.
- Conservative limits for progression and workout load.

The backend should not send only raw activity rows and expect the LLM to calculate all training metrics. Raw imported data such as activity summaries, activity details, and performance streams should be transformed into a decision-oriented `TrainingSnapshot`.

Example `TrainingSnapshot` fields:

- Analysis period.
- Sport.
- Weekly volume history.
- Current weekly volume.
- Average weekly volume.
- Training frequency.
- Longest recent activity.
- Recent gaps.
- Whether there is enough history.
- Basic intensity distribution when available.
- Safety limits for the next plan.

The backend should also enforce safety rules, such as:

- Weekly volume must not increase beyond a configured threshold.
- The long workout must stay within a safe percentage of weekly volume.
- No hard workouts on consecutive days.
- Limit the number of intense workouts per week.
- Require at least one rest day or very easy day.
- Generate conservative plans when the activity history is insufficient.

These rules can be simple in the first version. The goal is not to build a full sports science engine immediately, but to keep critical constraints outside the LLM.

## LLM Responsibilities

The LLM should receive a structured `TrainingSnapshot` and be used for:

- Interpreting the structured training context.
- Producing a readable explanation of the plan.
- Generating a weekly workout plan in a fixed JSON schema.
- Adapting the plan to the user's stated objective and availability.
- Suggesting conservative variations when data is incomplete.

The LLM should not be the only place where safety limits are defined. Prompts, skills, agents, or system instructions are useful, but they are not reliable enough as the final enforcement mechanism.

The LLM should not be responsible for:

- Querying Sudolife's database.
- Calculating authoritative weekly volumes.
- Enforcing final safety limits.
- Persisting accepted plans.
- Deciding whether an unsafe plan can be saved.

## RAG Strategy

Use structured RAG for the MVP.

Structured RAG means the backend sends compact, explicit, typed context to the LLM, for example:

```json
{
  "goal": "run_10k",
  "period": {
    "from": "2026-06-08",
    "to": "2026-07-05"
  },
  "sport": "RUN",
  "weeklyVolumesKm": [12.4, 15.2, 16.8, 18.0],
  "currentWeeklyVolumeKm": 18.0,
  "averageWeeklyVolumeKm": 15.6,
  "trainingFrequencyPerWeek": 3,
  "longestRecentActivityKm": 7.2,
  "hasEnoughHistory": true,
  "safetyLimits": {
    "maxNextWeekVolumeKm": 19.8,
    "maxLongActivityKm": 8.0,
    "maxHardSessionsPerWeek": 1,
    "allowConsecutiveHardDays": false
  }
}
```

This is different from document RAG with embeddings and vector search. Sudolife does not need vector search for the first version of workout planning. The important context is already structured in the database and should be summarized by backend code.

RAG should be understood as runtime context, not model training:

- Training or fine-tuning teaches the model general behavior.
- RAG provides the current user's specific facts.
- Backend validation enforces rules that must always hold.

The recommended first version is:

```text
LLM ready-made model
+ stable prompt
+ structured TrainingSnapshot
+ backend safety validator
```

Fine-tuning should only be considered later if the model repeatedly fails on style, schema adherence, or general planning patterns even after prompt and schema improvements.

## Prompt and Skill Strategy

It is still useful to define a stable prompt or skill for the workout planning behavior. That prompt should include:

- The assistant role.
- The expected JSON schema.
- Conservative training principles.
- Medical and injury disclaimers.
- Rules about insufficient data.
- Instructions to avoid diagnoses or medical prescriptions.

The prompt guides the LLM. The backend validates the result.

## Container Architecture

Run the LLM in a separate container from the Spring Boot backend.

Recommended local MVP architecture:

```text
frontend
backend Spring Boot
postgres
llm server
```

The backend should call the LLM server through a driven adapter, for example:

```text
application/service/training/ports/required/WorkoutPlanAiProvider.java
adapter/driven/ai/ollama/OllamaWorkoutPlanAiAdapter.java
```

This keeps the application layer independent from the LLM runtime. Ollama is a good first option for local development. vLLM or llama.cpp server can be considered later if performance, GPU usage, or deployment requirements justify the change.

The LLM container should not access the database directly. It should only receive structured context from the backend and return a structured proposal.

## Suggested MVP Scope

Start with a small `training` feature:

- `TrainingSnapshot`
- `TrainingActivitySummary`
- `WorkoutPlan`
- `WorkoutSession`
- `GenerateWorkoutPlanUseCase`
- `WorkoutPlanAiProvider`
- `WorkoutPlanSafetyValidator`

The first version can use only activity summaries and details. Raw stream analysis can be added later for advanced insights like pace stability, cardiac drift, and power or cadence consistency.

## Decision Summary

Use the LLM for reasoning, explanation, and plan drafting. Use backend code for deterministic metrics, boundaries, validation, and storage. Keep the LLM in a separate container and integrate it through a hexagonal driven adapter.
