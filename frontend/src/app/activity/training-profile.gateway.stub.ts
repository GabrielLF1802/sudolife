import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import { TrainingProfileGateway } from './training-profile.gateway';
import {
  SaveTrainingProfileCommand,
  TrainingProfile,
} from './services/dtos/training-profile';

@Injectable()
export class TrainingProfileGatewayStub implements TrainingProfileGateway {
  get(): Observable<TrainingProfile> {
    return of(this.profile(null));
  }

  save(command: SaveTrainingProfileCommand): Observable<TrainingProfile> {
    return of(this.profile(command.birthYear));
  }

  private profile(birthYear: number | null): TrainingProfile {
    return {
      birthYear,
      adaptiveCoachingEligible: birthYear !== null,
      heartRateZoneSource: birthYear === null ? 'UNAVAILABLE' : 'AGE_BASED',
      heartRateZones: [],
    };
  }
}
