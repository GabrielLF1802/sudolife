import type { CurrentUser } from '../../src/app/core/auth/auth.service';
import type { ActivityDetail } from '../../src/app/features/activity/services/dtos/activity-detail';
import type { ActivityListItem } from '../../src/app/features/activity/services/dtos/activity-list';
import type {
  AdaptiveRunningPlanSession,
  CurrentAdaptiveRunningPlan,
} from '../../src/app/features/activity/services/dtos/adaptive-running-plan';
import type {
  RunningDay,
  UserReportedReadiness,
} from '../../src/app/features/activity/services/dtos/coaching-profile';

export const browserJourneyAthlete = {
  id: 101,
  name: 'Ana Corredora',
  email: 'ana.corredora@sudolife.test',
  password: 'SenhaForte123!',
  token: 'browser-journey-token',
} as const;

export interface BrowserJourneyState {
  registered: boolean;
  authenticated: boolean;
  stravaLinked: boolean;
  stravaDataConsentValid: boolean;
  activitiesSynced: boolean;
  trainingBirthYear: number | null;
  coachingProfile: BrowserJourneyCoachingProfile;
  plan: CurrentAdaptiveRunningPlan | null;
}

export interface BrowserJourneyCoachingProfile {
  targetDistanceKilometers: number | null;
  targetPaceSecondsPerKilometer: number | null;
  targetDate: string | null;
  readiness: UserReportedReadiness | null;
  injuryConcern: boolean;
  preferredRunningDays: RunningDay[];
  configured: boolean;
}

export interface SaveBrowserJourneyCoachingProfileCommand {
  targetDistanceKilometers: number | null;
  targetPaceSecondsPerKilometer: number | null;
  targetDate: string | null;
  readiness: UserReportedReadiness | '';
  injuryConcern: boolean;
  preferredRunningDays: RunningDay[];
}

export function createBrowserJourneyState(): BrowserJourneyState {
  return {
    registered: false,
    authenticated: false,
    stravaLinked: false,
    stravaDataConsentValid: false,
    activitiesSynced: false,
    trainingBirthYear: null,
    coachingProfile: {
      targetDistanceKilometers: null,
      targetPaceSecondsPerKilometer: null,
      targetDate: null,
      readiness: null,
      injuryConcern: false,
      preferredRunningDays: [],
      configured: false,
    },
    plan: null,
  };
}

export function currentUser(): CurrentUser {
  return {
    id: browserJourneyAthlete.id,
    name: browserJourneyAthlete.name,
    email: browserJourneyAthlete.email,
  };
}

export function deterministicActivity(): ActivityListItem {
  return {
    id: 501,
    sourceActivityId: 987654321,
    name: 'Corrida regenerativa',
    sportType: 'RUN',
    startDate: '2026-08-24T09:00:00',
    distanceMeters: 5200,
    movingTimeSeconds: 1820,
    averageSpeedMetersPerSecond: 2.86,
    averagePaceSecondsPerKilometer: 350,
    streamStatus: 'READY',
  };
}

export function deterministicActivityDetail(): ActivityDetail {
  return {
    ...deterministicActivity(),
    totalElevationGainMeters: 34,
    maxSpeedMetersPerSecond: 3.8,
    averageHeartRate: 142,
    maxHeartRate: 166,
    averageCadence: 172,
    averageWatts: null,
    calories: 374,
    availableStreamMetricNames: ['HEART_RATE', 'CADENCE', 'PACE'],
    enrichmentStatus: 'READY',
  };
}

export function buildInitialAdaptivePlan(): CurrentAdaptiveRunningPlan {
  return {
    id: 701,
    safeMilestone: {
      targetDistanceKilometers: 10,
      targetPaceSecondsPerKilometer: 350,
      targetDate: '2026-10-25',
    },
    explanation: 'Plano adaptativo criado com a corrida importada e sua disponibilidade semanal.',
    acceptedAt: '2026-08-24T12:00:00',
    plannedSessions: [completedMatchedSession(null), plannedNextSession()],
  };
}

export function recordPerceivedEffort(
  plan: CurrentAdaptiveRunningPlan,
  sessionId: number,
  perceivedEffort: number,
): CurrentAdaptiveRunningPlan {
  return {
    ...plan,
    plannedSessions: plan.plannedSessions.map((session) =>
      session.id === sessionId
        ? { ...session, postSessionPerceivedEffort: perceivedEffort }
        : session,
    ),
  };
}

export function adaptNextSessionForLowReadiness(
  plan: CurrentAdaptiveRunningPlan,
): CurrentAdaptiveRunningPlan {
  const nextSession = plan.plannedSessions.find((session) => session.status === 'PLANNED');

  if (!nextSession) {
    return plan;
  }

  const replacedSession: AdaptiveRunningPlanSession = {
    ...nextSession,
    status: 'REPLACED',
    adaptationTrigger: 'LOW_READINESS',
  };

  const adaptedSession: AdaptiveRunningPlanSession = {
    id: 703,
    originalPlannedSessionId: nextSession.id,
    plannedSession: {
      ...nextSession.plannedSession,
      type: 'RECOVERY',
      distanceKilometers: 2.5,
      adapted: true,
      adaptationTrigger: 'LOW_READINESS',
      target: {
        type: 'PERCEIVED_EFFORT',
        minimumHeartRate: null,
        maximumHeartRate: null,
        minimumPerceivedEffort: 1,
        maximumPerceivedEffort: 2,
      },
    },
    status: 'PLANNED',
    adaptationTrigger: 'LOW_READINESS',
    matchedActivityId: null,
    postSessionPerceivedEffort: null,
  };

  return {
    ...plan,
    explanation: 'Plano atualizado para baixa prontidão, reduzindo a próxima sessão.',
    plannedSessions: plan.plannedSessions.flatMap((session) =>
      session.id === nextSession.id ? [replacedSession, adaptedSession] : [session],
    ),
  };
}

function completedMatchedSession(perceivedEffort: number | null): AdaptiveRunningPlanSession {
  return {
    id: 701,
    originalPlannedSessionId: null,
    plannedSession: {
      weekNumber: 1,
      sessionNumber: 1,
      type: 'EASY_RUN',
      distanceKilometers: 5.2,
      scheduledDate: '2026-08-24',
      target: {
        type: 'PERCEIVED_EFFORT',
        minimumHeartRate: null,
        maximumHeartRate: null,
        minimumPerceivedEffort: 2,
        maximumPerceivedEffort: 4,
      },
    },
    status: 'COMPLETED',
    adaptationTrigger: null,
    matchedActivityId: deterministicActivity().id,
    postSessionPerceivedEffort: perceivedEffort,
  };
}

function plannedNextSession(): AdaptiveRunningPlanSession {
  return {
    id: 702,
    originalPlannedSessionId: null,
    plannedSession: {
      weekNumber: 1,
      sessionNumber: 2,
      type: 'EASY_RUN',
      distanceKilometers: 4,
      scheduledDate: '2026-08-27',
      target: {
        type: 'PERCEIVED_EFFORT',
        minimumHeartRate: null,
        maximumHeartRate: null,
        minimumPerceivedEffort: 3,
        maximumPerceivedEffort: 5,
      },
    },
    status: 'PLANNED',
    adaptationTrigger: null,
    matchedActivityId: null,
    postSessionPerceivedEffort: null,
  };
}
