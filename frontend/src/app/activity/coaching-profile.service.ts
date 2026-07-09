import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type UserReportedReadiness = 'LOW' | 'MODERATE' | 'HIGH';

export interface CoachingProfile {
  targetDistanceKilometers: number | null;
  targetPaceSecondsPerKilometer: number | null;
  targetDate: string | null;
  readiness: UserReportedReadiness | null;
  injuryConcern: boolean;
  configured: boolean;
}

export interface SaveCoachingProfileCommand {
  targetDistanceKilometers: number | null;
  targetPaceSecondsPerKilometer: number | null;
  targetDate: string | null;
  readiness: UserReportedReadiness | '';
  injuryConcern: boolean;
}

@Injectable({ providedIn: 'root' })
export class CoachingProfileService {
  private readonly http = inject(HttpClient);

  get(): Observable<CoachingProfile> {
    return this.http.get<CoachingProfile>('/api/coaching-profiles');
  }

  save(command: SaveCoachingProfileCommand): Observable<CoachingProfile> {
    return this.http.put<CoachingProfile>('/api/coaching-profiles', command);
  }
}
