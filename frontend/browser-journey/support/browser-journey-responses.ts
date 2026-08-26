import type { ActivityList } from '../../src/app/features/activity/services/dtos/activity-list';
import type {
  AdaptiveRunningPlan,
  ConservativeRunningPlan,
  CurrentAdaptiveRunningPlan,
  RunningGoalAssessment,
} from '../../src/app/features/activity/services/dtos/adaptive-running-plan';
import type { RunningHistorySnapshot } from '../../src/app/features/activity/services/dtos/coaching-profile';
import type { StravaActivitySyncResult } from '../../src/app/features/activity/services/dtos/strava-activity-sync';
import type { StravaDataConsentStatus } from '../../src/app/features/activity/services/dtos/strava-data-consent';
import type { StravaLinkStatus } from '../../src/app/features/activity/services/dtos/strava-link-status';
import type { TrainingProfile } from '../../src/app/features/activity/services/dtos/training-profile';

import {
  BrowserJourneyState,
  buildInitialAdaptivePlan,
  deterministicActivity,
} from './browser-journey-state';

export function stravaStatusResponse(state: BrowserJourneyState): StravaLinkStatus {
  if (!state.stravaLinked) {
    return {
      linked: false,
      athleteId: null,
      permissionState: 'UNLINKED',
      profilePermissionState: 'UNLINKED',
      activitySummaryStatus: 'UNLINKED',
      performanceDataStatus: 'UNLINKED',
      lastSummarySyncTime: null,
      lastStreamEnrichmentTime: null,
      importedActivityCount: 0,
      streamsReadyActivityCount: 0,
      failureReason: null,
    };
  }

  return {
    linked: true,
    athleteId: 24681357,
    permissionState: 'READY',
    profilePermissionState: 'AVAILABLE',
    activitySummaryStatus: state.activitiesSynced ? 'COMPLETED' : 'NOT_STARTED',
    performanceDataStatus: state.activitiesSynced ? 'READY' : 'NOT_STARTED',
    lastSummarySyncTime: state.activitiesSynced ? '2026-08-24T12:00:00' : null,
    lastStreamEnrichmentTime: state.activitiesSynced ? '2026-08-24T12:01:00' : null,
    importedActivityCount: state.activitiesSynced ? 1 : 0,
    streamsReadyActivityCount: state.activitiesSynced ? 1 : 0,
    failureReason: null,
  };
}

export function stravaDataConsentResponse(state: BrowserJourneyState): StravaDataConsentStatus {
  return {
    valid: state.stravaDataConsentValid,
    currentConsentVersion: 'strava-data-import-and-coaching-v1',
    purpose: 'STRAVA_DATA_IMPORT_AND_COACHING',
  };
}

export function stravaSyncResponse(state: BrowserJourneyState): StravaActivitySyncResult {
  return {
    status: state.stravaLinked ? 'COMPLETED' : 'UNLINKED',
    failureReason: null,
    importedActivityCount: state.stravaLinked ? 1 : 0,
    totalActivityCount: state.stravaLinked ? 1 : 0,
  };
}

export function activityListResponse(state: BrowserJourneyState, page: number): ActivityList {
  const activities = state.activitiesSynced ? [deterministicActivity()] : [];

  return {
    activities,
    page,
    size: 10,
    totalElements: activities.length,
    totalPages: activities.length === 0 ? 0 : 1,
  };
}

export function trainingProfileResponse(state: BrowserJourneyState): TrainingProfile {
  return {
    birthYear: state.trainingBirthYear,
    adaptiveCoachingEligible: state.trainingBirthYear !== null,
    heartRateZoneSource: state.trainingBirthYear === null ? 'UNAVAILABLE' : 'AGE_BASED',
    heartRateZones:
      state.trainingBirthYear === null
        ? []
        : [
            { minimumHeartRate: 111, maximumHeartRate: 130 },
            { minimumHeartRate: 131, maximumHeartRate: 149 },
            { minimumHeartRate: 150, maximumHeartRate: 168 },
          ],
  };
}

export function coachingProfileResponse(state: BrowserJourneyState) {
  return state.coachingProfile;
}

export function runningHistoryResponse(state: BrowserJourneyState): RunningHistorySnapshot {
  if (!hasCompletedSetup(state)) {
    return {
      sufficientRunningHistory: false,
      activeWeeks: state.activitiesSynced ? 1 : 0,
      runningActivityCount: state.activitiesSynced ? 1 : 0,
      totalDistanceKilometers: state.activitiesSynced ? 5.2 : 0,
      totalMovingTimeSeconds: state.activitiesSynced ? 1820 : 0,
      latestRunAt: state.activitiesSynced ? '2026-08-24T09:00:00' : null,
    };
  }

  return {
    sufficientRunningHistory: true,
    activeWeeks: 6,
    runningActivityCount: 12,
    totalDistanceKilometers: 58.4,
    totalMovingTimeSeconds: 21035,
    latestRunAt: '2026-08-24T09:00:00',
  };
}

export function runningGoalAssessmentResponse(): RunningGoalAssessment {
  return {
    realistic: true,
    reasons: [],
    longTermGoal: {
      targetDistanceKilometers: 10,
      targetPaceSecondsPerKilometer: 350,
      targetDate: '2026-10-25',
    },
    safeMilestone: {
      targetDistanceKilometers: 10,
      targetPaceSecondsPerKilometer: 350,
      targetDate: '2026-10-25',
    },
  };
}

export function conservativeRunningPlanResponse(): ConservativeRunningPlan {
  return {
    classification: 'CONSERVATIVE',
    reasons: ['INSUFFICIENT_HISTORY'],
    longTermGoalDistanceKilometers: 10,
    durationWeeks: 4,
    sessionsPerWeek: 3,
    weeklyProgressionPercent: 5,
    plannedSessions: [
      {
        weekNumber: 1,
        sessionNumber: 1,
        type: 'EASY_RUN',
        distanceKilometers: 3,
        scheduledDate: '2026-08-25',
        target: {
          type: 'PERCEIVED_EFFORT',
          minimumHeartRate: null,
          maximumHeartRate: null,
          minimumPerceivedEffort: 2,
          maximumPerceivedEffort: 4,
        },
      },
    ],
  };
}

export function adaptiveRunningPlanResponse(state: BrowserJourneyState): AdaptiveRunningPlan {
  const plan = ensureCurrentAdaptivePlan(state);

  return {
    safeMilestone: plan.safeMilestone,
    plannedSessions: plan.plannedSessions.map((session) => session.plannedSession),
    explanation: plan.explanation,
    adjustedBySafetyValidation: false,
  };
}

export function currentAdaptiveRunningPlanResponse(
  state: BrowserJourneyState,
): CurrentAdaptiveRunningPlan {
  return ensureCurrentAdaptivePlan(state);
}

export function ensureCurrentAdaptivePlan(state: BrowserJourneyState): CurrentAdaptiveRunningPlan {
  if (state.plan === null) {
    state.plan = buildInitialAdaptivePlan();
  }

  return state.plan;
}

export function hasCompletedSetup(state: BrowserJourneyState): boolean {
  return (
    state.activitiesSynced && state.trainingBirthYear !== null && state.coachingProfile.configured
  );
}
