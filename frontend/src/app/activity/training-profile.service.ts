import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface TrainingProfile {
  birthYear: number | null;
  adaptiveCoachingEligible: boolean;
  heartRateZoneSource: 'AGE_BASED' | 'STRAVA' | 'UNAVAILABLE';
  heartRateZones: TrainingHeartRateZone[];
}

export interface TrainingHeartRateZone {
  minimumHeartRate: number;
  maximumHeartRate: number;
}

export interface SaveTrainingProfileCommand {
  birthYear: number | null;
}

@Injectable({ providedIn: 'root' })
export class TrainingProfileService {
  private readonly http = inject(HttpClient);

  get(): Observable<TrainingProfile> {
    return this.http.get<TrainingProfile>('/api/training-profile');
  }

  save(command: SaveTrainingProfileCommand): Observable<TrainingProfile> {
    return this.http.put<TrainingProfile>('/api/training-profile', command);
  }
}
