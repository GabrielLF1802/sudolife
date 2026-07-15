import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CoachingProfileService } from './coaching-profile.service';

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
});
