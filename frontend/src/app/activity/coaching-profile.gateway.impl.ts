import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

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
} from './services/dtos/adaptive-running-plan';
import {
  CoachingProfile,
  RunningHistorySnapshot,
  SaveCoachingProfileCommand,
} from './services/dtos/coaching-profile';

@Injectable({ providedIn: 'root' })
export class CoachingProfileGatewayImpl implements CoachingProfileGateway {
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
