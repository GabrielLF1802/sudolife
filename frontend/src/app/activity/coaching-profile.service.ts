import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type UserReportedReadiness = 'LOW' | 'MODERATE' | 'HIGH';
export type RunningDay =
  'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface CoachingProfile {
  targetDistanceKilometers: number | null;
  targetPaceSecondsPerKilometer: number | null;
  targetDate: string | null;
  readiness: UserReportedReadiness | null;
  injuryConcern: boolean;
  preferredRunningDays: RunningDay[];
  configured: boolean;
}

export interface SaveCoachingProfileCommand {
  targetDistanceKilometers: number | null;
  targetPaceSecondsPerKilometer: number | null;
  targetDate: string | null;
  readiness: UserReportedReadiness | '';
  injuryConcern: boolean;
  preferredRunningDays: RunningDay[];
}

export interface RunningHistorySnapshot {
  sufficientRunningHistory: boolean;
  activeWeeks: number;
  runningActivityCount: number;
  totalDistanceKilometers: number;
  totalMovingTimeSeconds: number;
  latestRunAt: string | null;
}

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

@Injectable({ providedIn: 'root' })
export class CoachingProfileService {
  private readonly http = inject(HttpClient);

  get(): Observable<CoachingProfile> {
    return this.http.get<CoachingProfile>('/api/coaching-profiles');
  }

  getRunningHistory(): Observable<RunningHistorySnapshot> {
    return this.http.get<RunningHistorySnapshot>('/api/coaching-profiles/running-history');
  }

  evaluateRunningGoal(): Observable<RunningGoalAssessment> {
    return this.http.get<RunningGoalAssessment>('/api/coaching-profiles/running-goal-assessment');
  }

  generateConservativeRunningPlan(): Observable<ConservativeRunningPlan> {
    return this.http.post<ConservativeRunningPlan>('/api/coaching-profiles/running-plan', null);
  }

  generateAdaptiveRunningPlan(): Observable<AdaptiveRunningPlan> {
    return this.http.post<AdaptiveRunningPlan>(
      '/api/coaching-profiles/adaptive-running-plan',
      null,
    );
  }

  getCurrentAdaptiveRunningPlan(): Observable<CurrentAdaptiveRunningPlan> {
    return this.http.get<CurrentAdaptiveRunningPlan>(
      '/api/coaching-profiles/adaptive-running-plan',
    );
  }

  adaptNextPlannedSession(
    command: AdaptNextPlannedSessionCommand,
  ): Observable<CurrentAdaptiveRunningPlan> {
    return this.http.post<CurrentAdaptiveRunningPlan>(
      '/api/coaching-profiles/adaptive-running-plan/adapt',
      command,
    );
  }

  clearInjuryConcern(command: ClearInjuryConcernCommand): Observable<CurrentAdaptiveRunningPlan> {
    return this.http.post<CurrentAdaptiveRunningPlan>(
      '/api/coaching-profiles/injury-concern/clear',
      command,
    );
  }

  correctPlannedSessionMatch(
    command: CorrectPlannedSessionMatchCommand,
  ): Observable<CurrentAdaptiveRunningPlan> {
    return this.http.put<CurrentAdaptiveRunningPlan>(
      '/api/coaching-profiles/adaptive-running-plan/session-match',
      command,
    );
  }

  unlinkPlannedSessionMatch(plannedSessionId: number): Observable<CurrentAdaptiveRunningPlan> {
    return this.http.delete<CurrentAdaptiveRunningPlan>(
      `/api/coaching-profiles/adaptive-running-plan/sessions/${plannedSessionId}/match`,
    );
  }

  submitPostSessionPerceivedEffort(
    plannedSessionId: number,
    command: PostSessionPerceivedEffortCommand,
  ): Observable<CurrentAdaptiveRunningPlan> {
    return this.http.put<CurrentAdaptiveRunningPlan>(
      `/api/coaching-profiles/adaptive-running-plan/sessions/${plannedSessionId}/perceived-effort`,
      command,
    );
  }

  save(command: SaveCoachingProfileCommand): Observable<CoachingProfile> {
    return this.http.put<CoachingProfile>('/api/coaching-profiles', command);
  }
}
