import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import {
  CoachingProfileService,
  ConservativeRunningPlan,
  CurrentAdaptiveRunningPlan,
  PlannedSession,
} from './coaching-profile.service';

describe('CoachingProfileService', () => {
  let service: CoachingProfileService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CoachingProfileService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(CoachingProfileService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should_load_coaching_profiles', () => {
    service.get().subscribe((profile) => {
      expect(profile.targetDistanceKilometers).toBe(10);
      expect(profile.readiness).toBe('LOW');
      expect(profile.injuryConcern).toBeTrue();
    });

    const request = httpTestingController.expectOne('/api/coaching-profiles');
    expect(request.request.method).toBe('GET');
    request.flush(coachingProfile());
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

    const request = httpTestingController.expectOne('/api/coaching-profiles');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(command);
    request.flush(coachingProfile());
  });

  it('should_load_running_history_snapshot', () => {
    service.getRunningHistory().subscribe((snapshot) => {
      expect(snapshot.sufficientRunningHistory).toBeTrue();
    });

    const request = httpTestingController.expectOne('/api/coaching-profiles/running-history');

    request.flush({ sufficientRunningHistory: true });
  });

  it('should_request_a_structured_conservative_running_plan', () => {
    service.generateConservativeRunningPlan().subscribe((plan) => {
      expect(plan.classification).toBe('CONSERVATIVE');
      expect(plan.plannedSessions[0].type).toBe('EASY_RUN');
    });

    const request = httpTestingController.expectOne('/api/coaching-profiles/running-plan');
    expect(request.request.method).toBe('POST');
    request.flush(conservativeRunningPlan());
  });

  it('should_load_the_running_goal_assessment', () => {
    service.evaluateRunningGoal().subscribe((assessment) => {
      expect(assessment.realistic).toBeFalse();
      expect(assessment.longTermGoal.targetDistanceKilometers).toBe(42.2);
      expect(assessment.safeMilestone.targetDistanceKilometers).toBe(7.3);
    });

    const request = httpTestingController.expectOne(
      '/api/coaching-profiles/running-goal-assessment',
    );
    expect(request.request.method).toBe('GET');
    request.flush({
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
    });
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

    const request = httpTestingController.expectOne('/api/coaching-profiles/adaptive-running-plan');
    expect(request.request.method).toBe('GET');
    request.flush(currentPlan);
  });

  it('should_adapt_the_next_planned_session', () => {
    const command = { trigger: 'LOW_READINESS' as const };

    service.adaptNextPlannedSession(command).subscribe((plan) => {
      expect(plan.id).toBe(31);
    });

    const request = httpTestingController.expectOne(
      '/api/coaching-profiles/adaptive-running-plan/adapt',
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(command);
    request.flush(currentAdaptiveRunningPlan());
  });

  it('should_clear_the_injury_concern', () => {
    const command = { readiness: 'MODERATE' as const };

    service.clearInjuryConcern(command).subscribe((plan) => {
      expect(plan.acceptedAt).toBe('2026-07-18T10:15:00Z');
    });

    const request = httpTestingController.expectOne('/api/coaching-profiles/injury-concern/clear');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(command);
    request.flush(currentAdaptiveRunningPlan());
  });

  it('should_correct_a_planned_session_match', () => {
    const command = { plannedSessionId: 12, activityId: 99 };

    service.correctPlannedSessionMatch(command).subscribe((plan) => {
      expect(plan.plannedSessions[2].matchedActivityId).toBe(99);
    });

    const request = httpTestingController.expectOne(
      '/api/coaching-profiles/adaptive-running-plan/session-match',
    );
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(command);
    request.flush(currentAdaptiveRunningPlan());
  });

  it('should_unlink_a_planned_session_match', () => {
    service.unlinkPlannedSessionMatch(12).subscribe((plan) => {
      expect(plan.plannedSessions.length).toBe(4);
    });

    const request = httpTestingController.expectOne(
      '/api/coaching-profiles/adaptive-running-plan/sessions/12/match',
    );
    expect(request.request.method).toBe('DELETE');
    expect(request.request.body).toBeNull();
    request.flush(currentAdaptiveRunningPlan());
  });

  it('should_submit_post_session_perceived_effort', () => {
    const command = { perceivedEffort: 7 };

    service.submitPostSessionPerceivedEffort(12, command).subscribe((plan) => {
      expect(plan.plannedSessions[2].postSessionPerceivedEffort).toBe(7);
    });

    const request = httpTestingController.expectOne(
      '/api/coaching-profiles/adaptive-running-plan/sessions/12/perceived-effort',
    );
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(command);
    request.flush(currentAdaptiveRunningPlan());
  });

  function coachingProfile() {
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
