import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { TrainingProfileGateway } from './training-profile.gateway';
import {
  SaveTrainingProfileCommand,
  TrainingProfile,
} from './services/dtos/training-profile';

@Injectable({ providedIn: 'root' })
export class TrainingProfileGatewayImpl implements TrainingProfileGateway {
  private readonly http = inject(HttpClient);

  get(): Observable<TrainingProfile> {
    return this.http.get<TrainingProfile>('/api/training-profile');
  }

  save(command: SaveTrainingProfileCommand): Observable<TrainingProfile> {
    return this.http.put<TrainingProfile>('/api/training-profile', command);
  }
}
