import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import {
  TRAINING_PROFILE_GATEWAY,
  TrainingProfileGateway,
} from './training-profile.gateway';
import { TrainingProfileService } from './training-profile.service';

describe('TrainingProfileService', () => {
  let service: TrainingProfileService;
  let trainingProfileGateway: jasmine.SpyObj<TrainingProfileGateway>;

  beforeEach(() => {
    trainingProfileGateway = jasmine.createSpyObj<TrainingProfileGateway>(
      'TrainingProfileGateway',
      ['get', 'save'],
    );
    trainingProfileGateway.get.and.returnValue(of(trainingProfile('AGE_BASED')));
    trainingProfileGateway.save.and.returnValue(of(trainingProfile('STRAVA')));

    TestBed.configureTestingModule({
      providers: [
        TrainingProfileService,
        { provide: TRAINING_PROFILE_GATEWAY, useValue: trainingProfileGateway },
      ],
    });

    service = TestBed.inject(TrainingProfileService);
  });

  it('should_load_training_profile', () => {
    service.get().subscribe((profile) => {
      expect(profile.birthYear).toBe(1990);
      expect(profile.adaptiveCoachingEligible).toBeTrue();
      expect(profile.heartRateZoneSource).toBe('AGE_BASED');
    });

    expect(trainingProfileGateway.get).toHaveBeenCalledOnceWith();
  });

  it('should_save_training_profile', () => {
    const command = { birthYear: 1990 };

    service.save({ birthYear: 1990 }).subscribe((profile) => {
      expect(profile.birthYear).toBe(1990);
      expect(profile.adaptiveCoachingEligible).toBeTrue();
      expect(profile.heartRateZoneSource).toBe('STRAVA');
    });

    expect(trainingProfileGateway.save).toHaveBeenCalledOnceWith(command);
  });

  function trainingProfile(heartRateZoneSource: 'AGE_BASED' | 'STRAVA') {
    return {
      birthYear: 1990,
      adaptiveCoachingEligible: true,
      heartRateZoneSource,
      heartRateZones: [],
    };
  }
});
