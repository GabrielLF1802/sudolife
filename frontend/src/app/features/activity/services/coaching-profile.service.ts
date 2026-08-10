import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { COACHING_PROFILE_GATEWAY, CoachingProfileGateway } from './coaching-profile.gateway';
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

@Injectable({ providedIn: 'root' })
export class CoachingProfileService {
  private readonly coachingProfileGateway: CoachingProfileGateway = inject(COACHING_PROFILE_GATEWAY);

  get(): Observable<CoachingProfile> {
    return this.coachingProfileGateway.get();
  }

  getRunningHistory(): Observable<RunningHistorySnapshot> {
    return this.coachingProfileGateway.getRunningHistory();
  }

  evaluateRunningGoal(): Observable<RunningGoalAssessment> {
    return this.coachingProfileGateway.evaluateRunningGoal();
  }

  generateConservativeRunningPlan(): Observable<ConservativeRunningPlan> {
    return this.coachingProfileGateway.generateConservativeRunningPlan();
  }

  generateAdaptiveRunningPlan(): Observable<AdaptiveRunningPlan> {
    return this.coachingProfileGateway.generateAdaptiveRunningPlan();
  }

  getCurrentAdaptiveRunningPlan(): Observable<CurrentAdaptiveRunningPlan> {
    return this.coachingProfileGateway.getCurrentAdaptiveRunningPlan();
  }

  adaptNextPlannedSession(
    command: AdaptNextPlannedSessionCommand,
  ): Observable<CurrentAdaptiveRunningPlan> {
    return this.coachingProfileGateway.adaptNextPlannedSession(command);
  }

  clearInjuryConcern(command: ClearInjuryConcernCommand): Observable<CurrentAdaptiveRunningPlan> {
    return this.coachingProfileGateway.clearInjuryConcern(command);
  }

  correctPlannedSessionMatch(
    command: CorrectPlannedSessionMatchCommand,
  ): Observable<CurrentAdaptiveRunningPlan> {
    return this.coachingProfileGateway.correctPlannedSessionMatch(command);
  }

  unlinkPlannedSessionMatch(plannedSessionId: number): Observable<CurrentAdaptiveRunningPlan> {
    return this.coachingProfileGateway.unlinkPlannedSessionMatch(plannedSessionId);
  }

  submitPostSessionPerceivedEffort(
    plannedSessionId: number,
    command: PostSessionPerceivedEffortCommand,
  ): Observable<CurrentAdaptiveRunningPlan> {
    return this.coachingProfileGateway.submitPostSessionPerceivedEffort(plannedSessionId, command);
  }

  save(command: SaveCoachingProfileCommand): Observable<CoachingProfile> {
    return this.coachingProfileGateway.save(command);
  }
}
