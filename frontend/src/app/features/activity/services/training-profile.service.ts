import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { TRAINING_PROFILE_GATEWAY, TrainingProfileGateway } from './training-profile.gateway';
import {
  SaveTrainingProfileCommand,
  TrainingProfile,
} from './dtos/training-profile';

@Injectable({ providedIn: 'root' })
export class TrainingProfileService {
  private readonly trainingProfileGateway: TrainingProfileGateway = inject(TRAINING_PROFILE_GATEWAY);

  get(): Observable<TrainingProfile> {
    return this.trainingProfileGateway.get();
  }

  save(command: SaveTrainingProfileCommand): Observable<TrainingProfile> {
    return this.trainingProfileGateway.save(command);
  }
}
