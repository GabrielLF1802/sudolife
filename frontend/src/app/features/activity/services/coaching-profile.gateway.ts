import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';

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

export interface CoachingProfileGateway {
  get(): Observable<CoachingProfile>;
  getRunningHistory(): Observable<RunningHistorySnapshot>;
  evaluateRunningGoal(): Observable<RunningGoalAssessment>;
  generateConservativeRunningPlan(): Observable<ConservativeRunningPlan>;
  generateAdaptiveRunningPlan(): Observable<AdaptiveRunningPlan>;
  getCurrentAdaptiveRunningPlan(): Observable<CurrentAdaptiveRunningPlan>;
  adaptNextPlannedSession(
    command: AdaptNextPlannedSessionCommand,
  ): Observable<CurrentAdaptiveRunningPlan>;
  clearInjuryConcern(command: ClearInjuryConcernCommand): Observable<CurrentAdaptiveRunningPlan>;
  correctPlannedSessionMatch(
    command: CorrectPlannedSessionMatchCommand,
  ): Observable<CurrentAdaptiveRunningPlan>;
  unlinkPlannedSessionMatch(plannedSessionId: number): Observable<CurrentAdaptiveRunningPlan>;
  submitPostSessionPerceivedEffort(
    plannedSessionId: number,
    command: PostSessionPerceivedEffortCommand,
  ): Observable<CurrentAdaptiveRunningPlan>;
  save(command: SaveCoachingProfileCommand): Observable<CoachingProfile>;
}

export const COACHING_PROFILE_GATEWAY = new InjectionToken<CoachingProfileGateway>(
  'CoachingProfileGateway',
);
