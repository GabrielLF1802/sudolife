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

  function coachingProfile() {
    return {
      targetDistanceKilometers: 10,
      targetPaceSecondsPerKilometer: 330,
      targetDate: '2026-05-12',
      readiness: 'LOW',
      injuryConcern: true,
      configured: true,
    };
  }
});
