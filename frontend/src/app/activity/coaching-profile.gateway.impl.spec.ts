import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CoachingProfileGatewayImpl } from './coaching-profile.gateway.impl';
import { PlannedSession } from './services/dtos/adaptive-running-plan';

describe('CoachingProfileGatewayImpl', () => {
  let gateway: CoachingProfileGatewayImpl;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CoachingProfileGatewayImpl, provideHttpClient(), provideHttpClientTesting()],
    });

    gateway = TestBed.inject(CoachingProfileGatewayImpl);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should_load_coaching_profiles', () => {
    gateway.get().subscribe((profile) => {
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

    gateway.save(command).subscribe((profile) => {
      expect(profile.targetPaceSecondsPerKilometer).toBe(330);
      expect(profile.configured).toBeTrue();
    });

    const request = httpTestingController.expectOne('/api/coaching-profiles');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(command);
    request.flush(coachingProfile());
  });

  it('should_load_running_history_snapshot', () => {
    gateway.getRunningHistory().subscribe((snapshot) => {
      expect(snapshot.sufficientRunningHistory).toBeTrue();
    });

    const request = httpTestingController.expectOne('/api/coaching-profiles/running-history');
    expect(request.request.method).toBe('GET');
    request.flush({
      sufficientRunningHistory: true,
      activeWeeks: 8,
      runningActivityCount: 16,
      totalDistanceKilometers: 84,
      totalMovingTimeSeconds: 30240,
      latestRunAt: '2026-07-18T10:15:00Z',
    });
  });

  it('should_request_a_structured_conservative_running_plan', () => {
    gateway.generateConservativeRunningPlan().subscribe((plan) => {
      expect(plan.classification).toBe('CONSERVATIVE');
      expect(plan.plannedSessions[0].type).toBe('EASY_RUN');
    });

    const request = httpTestingController.expectOne('/api/coaching-profiles/running-plan');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    request.flush(conservativeRunningPlan());
  });

  it('should_load_the_running_goal_assessment', () => {
    gateway.evaluateRunningGoal().subscribe((assessment) => {
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

  it('should_generate_an_adaptive_running_plan', () => {
    gateway.generateAdaptiveRunningPlan().subscribe((plan) => {
      expect(plan.safeMilestone.targetDistanceKilometers).toBe(10);
    });

    const request = httpTestingController.expectOne(
      '/api/coaching-profiles/adaptive-running-plan',
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    request.flush({
      safeMilestone: {
        targetDistanceKilometers: 10,
        targetPaceSecondsPerKilometer: 330,
        targetDate: '2026-09-20',
      },
      plannedSessions: [plannedSession()],
      explanation: 'Plano adaptado ao histórico recente.',
      adjustedBySafetyValidation: false,
    });
  });

  it('should_load_the_current_adaptive_plan', () => {
    gateway.getCurrentAdaptiveRunningPlan().subscribe((plan) => {
      expect(plan.id).toBe(31);
    });

    const request = httpTestingController.expectOne('/api/coaching-profiles/adaptive-running-plan');
    expect(request.request.method).toBe('GET');
    request.flush(currentAdaptiveRunningPlan());
  });

  it('should_adapt_the_next_planned_session', () => {
    const command = { trigger: 'LOW_READINESS' as const };

    gateway.adaptNextPlannedSession(command).subscribe((plan) => {
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

    gateway.clearInjuryConcern(command).subscribe((plan) => {
      expect(plan.acceptedAt).toBe('2026-07-18T10:15:00Z');
    });

    const request = httpTestingController.expectOne('/api/coaching-profiles/injury-concern/clear');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(command);
    request.flush(currentAdaptiveRunningPlan());
  });

  it('should_correct_a_planned_session_match', () => {
    const command = { plannedSessionId: 12, activityId: 99 };

    gateway.correctPlannedSessionMatch(command).subscribe((plan) => {
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
    gateway.unlinkPlannedSessionMatch(12).subscribe((plan) => {
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

    gateway.submitPostSessionPerceivedEffort(12, command).subscribe((plan) => {
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

  function conservativeRunningPlan() {
    return {
      classification: 'CONSERVATIVE',
      reasons: ['INSUFFICIENT_HISTORY'],
      longTermGoalDistanceKilometers: 21.1,
      durationWeeks: 4,
      sessionsPerWeek: 2,
      weeklyProgressionPercent: 5,
      plannedSessions: [plannedSession()],
    };
  }

  function currentAdaptiveRunningPlan() {
    const session = plannedSession();

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
        adaptiveSession(10, null, session, 'REPLACED', null, null, null),
        adaptiveSession(11, 10, session, 'PLANNED', 'LOW_READINESS', null, null),
        adaptiveSession(12, null, session, 'COMPLETED', null, 99, 7),
        adaptiveSession(13, null, session, 'MISSED', 'MISSED_PLANNED_SESSION', null, null),
      ],
    };
  }

  function plannedSession() {
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

  function adaptiveSession(
    id: number,
    originalPlannedSessionId: number | null,
    plannedSession: PlannedSession,
    status: 'PLANNED' | 'REPLACED' | 'COMPLETED' | 'MISSED',
    adaptationTrigger: 'MISSED_PLANNED_SESSION' | 'LOW_READINESS' | null,
    matchedActivityId: number | null,
    postSessionPerceivedEffort: number | null,
  ) {
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
