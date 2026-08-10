import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { TrainingProfileGatewayImpl } from './training-profile.gateway.impl';

describe('TrainingProfileGatewayImpl', () => {
  let gateway: TrainingProfileGatewayImpl;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [TrainingProfileGatewayImpl, provideHttpClient(), provideHttpClientTesting()],
    });

    gateway = TestBed.inject(TrainingProfileGatewayImpl);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should_load_training_profile', () => {
    gateway.get().subscribe((profile) => {
      expect(profile.birthYear).toBe(1990);
      expect(profile.adaptiveCoachingEligible).toBeTrue();
      expect(profile.heartRateZoneSource).toBe('AGE_BASED');
    });

    const request = httpTestingController.expectOne('/api/training-profile');
    expect(request.request.method).toBe('GET');
    request.flush({
      birthYear: 1990,
      adaptiveCoachingEligible: true,
      heartRateZoneSource: 'AGE_BASED',
      heartRateZones: [],
    });
  });

  it('should_save_training_profile', () => {
    const command = { birthYear: 1990 };

    gateway.save(command).subscribe((profile) => {
      expect(profile.birthYear).toBe(1990);
      expect(profile.adaptiveCoachingEligible).toBeTrue();
      expect(profile.heartRateZoneSource).toBe('STRAVA');
    });

    const request = httpTestingController.expectOne('/api/training-profile');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(command);
    request.flush({
      birthYear: 1990,
      adaptiveCoachingEligible: true,
      heartRateZoneSource: 'STRAVA',
      heartRateZones: [],
    });
  });
});
