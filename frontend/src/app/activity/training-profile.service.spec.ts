import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { TrainingProfileService } from './training-profile.service';

describe('TrainingProfileService', () => {
  let service: TrainingProfileService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [TrainingProfileService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(TrainingProfileService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should_load_training_profile', () => {
    service.get().subscribe((profile) => {
      expect(profile.birthYear).toBe(1990);
      expect(profile.adaptiveCoachingEligible).toBeTrue();
    });

    const request = httpTestingController.expectOne('/api/training-profile');
    expect(request.request.method).toBe('GET');
    request.flush({ birthYear: 1990, adaptiveCoachingEligible: true });
  });

  it('should_save_training_profile', () => {
    service.save({ birthYear: 1990 }).subscribe((profile) => {
      expect(profile.birthYear).toBe(1990);
      expect(profile.adaptiveCoachingEligible).toBeTrue();
    });

    const request = httpTestingController.expectOne('/api/training-profile');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ birthYear: 1990 });
    request.flush({ birthYear: 1990, adaptiveCoachingEligible: true });
  });
});
