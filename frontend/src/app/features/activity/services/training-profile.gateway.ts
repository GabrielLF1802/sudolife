import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';

import {
  SaveTrainingProfileCommand,
  TrainingProfile,
} from './dtos/training-profile';

export interface TrainingProfileGateway {
  get(): Observable<TrainingProfile>;
  save(command: SaveTrainingProfileCommand): Observable<TrainingProfile>;
}

export const TRAINING_PROFILE_GATEWAY = new InjectionToken<TrainingProfileGateway>(
  'TrainingProfileGateway',
);
