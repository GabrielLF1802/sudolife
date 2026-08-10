import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import { CoachingProfileGateway } from './coaching-profile.gateway';
import {
  AdaptNextPlannedSessionCommand,
  AdaptiveRunningPlan,
  ClearInjuryConcernCommand,
  ConservativeRunningPlan,
  CorrectPlannedSessionMatchCommand,
  CurrentAdaptiveRunningPlan,
  PostSessionPerceivedEffortCommand,
  RunningGoalAssessment,
} from './dtos/adaptive-running-plan';
import {
  CoachingProfile,
  RunningHistorySnapshot,
  SaveCoachingProfileCommand,
} from './dtos/coaching-profile';

@Injectable()
export class CoachingProfileGatewayStub implements CoachingProfileGateway {
  get(): Observable<CoachingProfile> {
    return of(this.profile(false));
  }

  getRunningHistory(): Observable<RunningHistorySnapshot> {
    return of({
      sufficientRunningHistory: false,
      activeWeeks: 0,
      runningActivityCount: 0,
      totalDistanceKilometers: 0,
      totalMovingTimeSeconds: 0,
      latestRunAt: null,
    });
  }

  evaluateRunningGoal(): Observable<RunningGoalAssessment> {
    return of({
      realistic: true,
      reasons: [],
      longTermGoal: this.goalSummary(),
      safeMilestone: this.goalSummary(),
    });
  }

  generateConservativeRunningPlan(): Observable<ConservativeRunningPlan> {
    return of({
      classification: 'CONSERVATIVE',
      reasons: ['INSUFFICIENT_HISTORY'],
      longTermGoalDistanceKilometers: 10,
      durationWeeks: 4,
      sessionsPerWeek: 2,
      weeklyProgressionPercent: 5,
      plannedSessions: [this.plannedSession()],
    });
  }

  generateAdaptiveRunningPlan(): Observable<AdaptiveRunningPlan> {
    return of({
      safeMilestone: this.goalSummary(),
      plannedSessions: [this.plannedSession()],
      explanation: 'Plano inicial.',
      adjustedBySafetyValidation: false,
    });
  }

  getCurrentAdaptiveRunningPlan(): Observable<CurrentAdaptiveRunningPlan> {
    return of(this.currentPlan());
  }

  adaptNextPlannedSession(
    command: AdaptNextPlannedSessionCommand,
  ): Observable<CurrentAdaptiveRunningPlan> {
    return of({
      ...this.currentPlan(),
      plannedSessions: [
        {
          ...this.currentPlan().plannedSessions[0],
          adaptationTrigger: command.trigger,
        },
      ],
    });
  }

  clearInjuryConcern(command: ClearInjuryConcernCommand): Observable<CurrentAdaptiveRunningPlan> {
    return of({
      ...this.currentPlan(),
      explanation: `Readiness ${command.readiness}`,
    });
  }

  correctPlannedSessionMatch(
    command: CorrectPlannedSessionMatchCommand,
  ): Observable<CurrentAdaptiveRunningPlan> {
    return of({
      ...this.currentPlan(),
      plannedSessions: [
        {
          ...this.currentPlan().plannedSessions[0],
          id: command.plannedSessionId,
          matchedActivityId: command.activityId,
        },
      ],
    });
  }

  unlinkPlannedSessionMatch(plannedSessionId: number): Observable<CurrentAdaptiveRunningPlan> {
    return of({
      ...this.currentPlan(),
      plannedSessions: [
        {
          ...this.currentPlan().plannedSessions[0],
          id: plannedSessionId,
          matchedActivityId: null,
        },
      ],
    });
  }

  submitPostSessionPerceivedEffort(
    plannedSessionId: number,
    command: PostSessionPerceivedEffortCommand,
  ): Observable<CurrentAdaptiveRunningPlan> {
    return of({
      ...this.currentPlan(),
      plannedSessions: [
        {
          ...this.currentPlan().plannedSessions[0],
          id: plannedSessionId,
          postSessionPerceivedEffort: command.perceivedEffort,
        },
      ],
    });
  }

  save(command: SaveCoachingProfileCommand): Observable<CoachingProfile> {
    return of({
      ...command,
      readiness: command.readiness === '' ? null : command.readiness,
      configured: true,
    });
  }

  private profile(configured: boolean): CoachingProfile {
    return {
      targetDistanceKilometers: null,
      targetPaceSecondsPerKilometer: null,
      targetDate: null,
      readiness: null,
      injuryConcern: false,
      preferredRunningDays: [],
      configured,
    };
  }

  private currentPlan(): CurrentAdaptiveRunningPlan {
    return {
      id: 1,
      safeMilestone: this.goalSummary(),
      explanation: 'Plano atual.',
      acceptedAt: '2026-07-18T10:15:00Z',
      plannedSessions: [
        {
          id: 1,
          originalPlannedSessionId: null,
          plannedSession: this.plannedSession(),
          status: 'PLANNED',
          adaptationTrigger: null,
          matchedActivityId: null,
          postSessionPerceivedEffort: null,
        },
      ],
    };
  }

  private goalSummary() {
    return {
      targetDistanceKilometers: 10,
      targetPaceSecondsPerKilometer: null,
      targetDate: null,
    };
  }

  private plannedSession() {
    return {
      weekNumber: 1,
      sessionNumber: 1,
      type: 'EASY_RUN' as const,
      distanceKilometers: 3,
      scheduledDate: '2026-07-18',
      target: {
        type: 'PERCEIVED_EFFORT' as const,
        minimumHeartRate: null,
        maximumHeartRate: null,
        minimumPerceivedEffort: 2,
        maximumPerceivedEffort: 4,
      },
    };
  }
}
