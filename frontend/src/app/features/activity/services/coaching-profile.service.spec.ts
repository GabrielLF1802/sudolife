import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import {
  ConservativeRunningPlan,
  CurrentAdaptiveRunningPlan,
  PlannedSession,
} from './dtos/adaptive-running-plan';
import { CoachingProfile } from './dtos/coaching-profile';
import { COACHING_PROFILE_GATEWAY, CoachingProfileGateway } from './coaching-profile.gateway';
import { CoachingProfileService } from './coaching-profile.service';

describe('CoachingProfileService', () => {
  let service: CoachingProfileService;
  let coachingProfileGateway: jasmine.SpyObj<CoachingProfileGateway>;

  beforeEach(() => {
    coachingProfileGateway = jasmine.createSpyObj<CoachingProfileGateway>(
      'CoachingProfileGateway',
      [
        'get',
        'getRunningHistory',
        'evaluateRunningGoal',
        'generateConservativeRunningPlan',
        'generateAdaptiveRunningPlan',
        'getCurrentAdaptiveRunningPlan',
        'adaptNextPlannedSession',
        'clearInjuryConcern',
        'correctPlannedSessionMatch',
        'unlinkPlannedSessionMatch',
        'submitPostSessionPerceivedEffort',
        'save',
      ],
    );
    coachingProfileGateway.get.and.returnValue(of(coachingProfile()));
    coachingProfileGateway.save.and.returnValue(of(coachingProfile()));
    coachingProfileGateway.getRunningHistory.and.returnValue(
      of({
        sufficientRunningHistory: true,
        activeWeeks: 8,
        runningActivityCount: 16,
        totalDistanceKilometers: 84,
        totalMovingTimeSeconds: 30240,
        latestRunAt: '2026-07-18T10:15:00Z',
      }),
    );
    coachingProfileGateway.generateConservativeRunningPlan.and.returnValue(
      of(conservativeRunningPlan()),
    );
    coachingProfileGateway.evaluateRunningGoal.and.returnValue(
      of({
        realistic: false,
        reasons: ['UNREALISTIC_DISTANCE'],
        longTermGoal: {
          targetDistanceKilometers: 42.2,
          targetPaceSecondsPerKilometer: 240,
          targetDate: '2026-10-01',
        },
        safeMilestone: {
          targetDistanceKilometers: 7.3,
          targetPaceSecondsPerKilometer: 332,
          targetDate: '2026-08-11',
        },
      }),
    );
    coachingProfileGateway.getCurrentAdaptiveRunningPlan.and.returnValue(
      of(currentAdaptiveRunningPlan()),
    );
    coachingProfileGateway.adaptNextPlannedSession.and.returnValue(of(currentAdaptiveRunningPlan()));
    coachingProfileGateway.clearInjuryConcern.and.returnValue(of(currentAdaptiveRunningPlan()));
    coachingProfileGateway.correctPlannedSessionMatch.and.returnValue(
      of(currentAdaptiveRunningPlan()),
    );
    coachingProfileGateway.unlinkPlannedSessionMatch.and.returnValue(
      of(currentAdaptiveRunningPlan()),
    );
    coachingProfileGateway.submitPostSessionPerceivedEffort.and.returnValue(
      of(currentAdaptiveRunningPlan()),
    );

    TestBed.configureTestingModule({
      providers: [
        CoachingProfileService,
        { provide: COACHING_PROFILE_GATEWAY, useValue: coachingProfileGateway },
      ],
    });

    service = TestBed.inject(CoachingProfileService);
  });

  it('should_load_coaching_profiles', () => {
    service.get().subscribe((profile) => {
      expect(profile.targetDistanceKilometers).toBe(10);
      expect(profile.readiness).toBe('LOW');
      expect(profile.injuryConcern).toBeTrue();
    });

    expect(coachingProfileGateway.get).toHaveBeenCalledOnceWith();
  });

  it('should_save_coaching_profiles', () => {
    const command = {
      targetDistanceKilometers: 10,
      targetPaceSecondsPerKilometer: 330,
      targetDate: '2026-05-12',
      readiness: 'LOW' as const,
      injuryConcern: true,
      preferredRunningDays: ['TUESDAY' as const, 'SATURDAY' as const],
    };

    service.save(command).subscribe((profile) => {
      expect(profile.targetPaceSecondsPerKilometer).toBe(330);
      expect(profile.configured).toBeTrue();
    });

    expect(coachingProfileGateway.save).toHaveBeenCalledOnceWith(command);
  });

  it('should_load_running_history_snapshot', () => {
    service.getRunningHistory().subscribe((snapshot) => {
      expect(snapshot.sufficientRunningHistory).toBeTrue();
    });

    expect(coachingProfileGateway.getRunningHistory).toHaveBeenCalledOnceWith();
  });

  it('should_request_a_structured_conservative_running_plan', () => {
    service.generateConservativeRunningPlan().subscribe((plan) => {
      expect(plan.classification).toBe('CONSERVATIVE');
      expect(plan.plannedSessions[0].type).toBe('EASY_RUN');
    });

    expect(coachingProfileGateway.generateConservativeRunningPlan).toHaveBeenCalledOnceWith();
  });

  it('should_load_the_running_goal_assessment', () => {
    service.evaluateRunningGoal().subscribe((assessment) => {
      expect(assessment.realistic).toBeFalse();
      expect(assessment.longTermGoal.targetDistanceKilometers).toBe(42.2);
      expect(assessment.safeMilestone.targetDistanceKilometers).toBe(7.3);
    });

    expect(coachingProfileGateway.evaluateRunningGoal).toHaveBeenCalledOnceWith();
  });

  it('should_preserve_the_complete_current_adaptive_plan', () => {
    const currentPlan = currentAdaptiveRunningPlan();

    service.getCurrentAdaptiveRunningPlan().subscribe((plan) => {
      expect(plan).toEqual(currentPlan);
      expect(plan.plannedSessions.map((session) => session.status)).toEqual([
        'REPLACED',
        'PLANNED',
        'COMPLETED',
        'MISSED',
      ]);
    });

    expect(coachingProfileGateway.getCurrentAdaptiveRunningPlan).toHaveBeenCalledOnceWith();
  });

  it('should_adapt_the_next_planned_session', () => {
    const command = { trigger: 'LOW_READINESS' as const };

    service.adaptNextPlannedSession(command).subscribe((plan) => {
      expect(plan.id).toBe(31);
    });

    expect(coachingProfileGateway.adaptNextPlannedSession).toHaveBeenCalledOnceWith(command);
  });

  it('should_clear_the_injury_concern', () => {
    const command = { readiness: 'MODERATE' as const };

    service.clearInjuryConcern(command).subscribe((plan) => {
      expect(plan.acceptedAt).toBe('2026-07-18T10:15:00Z');
    });

    expect(coachingProfileGateway.clearInjuryConcern).toHaveBeenCalledOnceWith(command);
  });

  it('should_correct_a_planned_session_match', () => {
    const command = { plannedSessionId: 12, activityId: 99 };

    service.correctPlannedSessionMatch(command).subscribe((plan) => {
      expect(plan.plannedSessions[2].matchedActivityId).toBe(99);
    });

    expect(coachingProfileGateway.correctPlannedSessionMatch).toHaveBeenCalledOnceWith(command);
  });

  it('should_unlink_a_planned_session_match', () => {
    service.unlinkPlannedSessionMatch(12).subscribe((plan) => {
      expect(plan.plannedSessions.length).toBe(4);
    });

    expect(coachingProfileGateway.unlinkPlannedSessionMatch).toHaveBeenCalledOnceWith(12);
  });

  it('should_submit_post_session_perceived_effort', () => {
    const command = { perceivedEffort: 7 };

    service.submitPostSessionPerceivedEffort(12, command).subscribe((plan) => {
      expect(plan.plannedSessions[2].postSessionPerceivedEffort).toBe(7);
    });

    expect(coachingProfileGateway.submitPostSessionPerceivedEffort).toHaveBeenCalledOnceWith(
      12,
      command,
    );
  });

  function coachingProfile(): CoachingProfile {
    return {
      targetDistanceKilometers: 10,
      targetPaceSecondsPerKilometer: 330,
      targetDate: '2026-05-12',
      readiness: 'LOW',
      injuryConcern: true,
      preferredRunningDays: ['TUESDAY', 'SATURDAY'],
      configured: true,
    };
  }

  function conservativeRunningPlan(): ConservativeRunningPlan {
    return {
      classification: 'CONSERVATIVE',
      reasons: ['INSUFFICIENT_HISTORY'],
      longTermGoalDistanceKilometers: 21.1,
      durationWeeks: 4,
      sessionsPerWeek: 2,
      weeklyProgressionPercent: 5,
      plannedSessions: [
        {
          weekNumber: 1,
          sessionNumber: 1,
          type: 'EASY_RUN',
          distanceKilometers: 3,
          scheduledDate: '2026-07-18',
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

  function currentAdaptiveRunningPlan(): CurrentAdaptiveRunningPlan {
    const sessions = conservativeRunningPlan().plannedSessions;

    return {
      id: 31,
      safeMilestone: {
        targetDistanceKilometers: 10,
        targetPaceSecondsPerKilometer: 330,
        targetDate: '2026-09-20',
      },
      explanation: 'Plano adaptado ao histórico recente.',
      acceptedAt: '2026-07-18T10:15:00Z',
      plannedSessions: [
        adaptiveSession(10, null, sessions[0], 'REPLACED', null, null, null),
        adaptiveSession(11, 10, sessions[0], 'PLANNED', 'LOW_READINESS', null, null),
        adaptiveSession(12, null, sessions[0], 'COMPLETED', null, 99, 7),
        adaptiveSession(13, null, sessions[0], 'MISSED', 'MISSED_PLANNED_SESSION', null, null),
      ],
    };
  }

  function adaptiveSession(
    id: number,
    originalPlannedSessionId: number | null,
    plannedSession: PlannedSession,
    status: 'PLANNED' | 'REPLACED' | 'COMPLETED' | 'MISSED',
    adaptationTrigger: 'MISSED_PLANNED_SESSION' | 'LOW_READINESS' | null,
    matchedActivityId: number | null,
    postSessionPerceivedEffort: number | null,
  ): CurrentAdaptiveRunningPlan['plannedSessions'][number] {
    return {
      id,
      originalPlannedSessionId,
      plannedSession,
      status,
      adaptationTrigger,
      matchedActivityId,
      postSessionPerceivedEffort,
    };
  }
});
