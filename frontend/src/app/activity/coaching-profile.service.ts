import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type UserReportedReadiness = 'LOW' | 'MODERATE' | 'HIGH';
export type RunningDay =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

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
  | 'UNREALISTIC_DISTANCE'
  | 'UNREALISTIC_PACE'
  | 'UNREALISTIC_TARGET_DATE';

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
  | 'INSUFFICIENT_HISTORY'
  | 'LOW_READINESS'
  | 'INJURY_CONCERN';
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
    return this.http.get<RunningGoalAssessment>(
      '/api/coaching-profiles/running-goal-assessment',
    );
  }

  generateConservativeRunningPlan(): Observable<ConservativeRunningPlan> {
    return this.http.post<ConservativeRunningPlan>('/api/coaching-profiles/running-plan', null);
  }

  save(command: SaveCoachingProfileCommand): Observable<CoachingProfile> {
    return this.http.put<CoachingProfile>('/api/coaching-profiles', command);
  }
}
