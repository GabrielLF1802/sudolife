import { UserReportedReadiness } from './coaching-profile';

export type RunningGoalAssessmentReason =
  'UNREALISTIC_DISTANCE' | 'UNREALISTIC_PACE' | 'UNREALISTIC_TARGET_DATE';

export interface RunningGoalSummary {
  targetDistanceKilometers: number;
  targetPaceSecondsPerKilometer: number | null;
  targetDate: string | null;
}

export interface RunningGoalAssessment {
  realistic: boolean;
  reasons: RunningGoalAssessmentReason[];
  longTermGoal: RunningGoalSummary;
  safeMilestone: RunningGoalSummary;
}

export type ConservativeRunningPlanReason =
  'INSUFFICIENT_HISTORY' | 'LOW_READINESS' | 'INJURY_CONCERN';

export type PlannedSessionType = 'EASY_RUN' | 'LONG_RUN' | 'RECOVERY';

export type PlannedSessionTargetType = 'HEART_RATE' | 'PERCEIVED_EFFORT';

export interface PlannedSessionTarget {
  type: PlannedSessionTargetType;
  minimumHeartRate: number | null;
  maximumHeartRate: number | null;
  minimumPerceivedEffort: number | null;
  maximumPerceivedEffort: number | null;
}

export interface PlannedSession {
  weekNumber: number;
  sessionNumber: number;
  type: PlannedSessionType;
  distanceKilometers: number;
  target: PlannedSessionTarget;
  scheduledDate: string;
  durationSeconds?: number | null;
  adapted?: boolean;
  adaptationTrigger?: AdaptationTrigger | null;
}

export type AdaptationTrigger =
  | 'MISSED_PLANNED_SESSION'
  | 'COMPLETED_PLANNED_SESSION'
  | 'INJURY_CONCERN'
  | 'LOW_READINESS'
  | 'UNEXPECTEDLY_HIGH_EFFORT'
  | 'UNEXPECTEDLY_LOW_EFFORT';

export type AdaptiveRunningPlanSessionStatus = 'PLANNED' | 'REPLACED' | 'COMPLETED' | 'MISSED';

export interface AdaptiveRunningPlanSession {
  id: number;
  originalPlannedSessionId: number | null;
  plannedSession: PlannedSession;
  status: AdaptiveRunningPlanSessionStatus;
  adaptationTrigger: AdaptationTrigger | null;
  matchedActivityId: number | null;
  postSessionPerceivedEffort: number | null;
}

export interface CurrentAdaptiveRunningPlan {
  id: number;
  safeMilestone: RunningGoalSummary;
  explanation: string;
  acceptedAt: string;
  plannedSessions: AdaptiveRunningPlanSession[];
}

export interface AdaptNextPlannedSessionCommand {
  trigger: AdaptationTrigger;
}

export interface ClearInjuryConcernCommand {
  readiness: UserReportedReadiness;
}

export interface CorrectPlannedSessionMatchCommand {
  plannedSessionId: number;
  activityId: number;
}

export interface PostSessionPerceivedEffortCommand {
  perceivedEffort: number;
}

export interface ConservativeRunningPlan {
  classification: 'CONSERVATIVE' | 'RECOVERY_ONLY';
  reasons: ConservativeRunningPlanReason[];
  longTermGoalDistanceKilometers: number;
  durationWeeks: number;
  sessionsPerWeek: number;
  weeklyProgressionPercent: number;
  plannedSessions: PlannedSession[];
}

export interface AdaptiveRunningPlan {
  safeMilestone: RunningGoalSummary;
  plannedSessions: PlannedSession[];
  explanation: string;
  adjustedBySafetyValidation: boolean;
}
