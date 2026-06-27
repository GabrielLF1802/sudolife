import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface ActivityListItem {
  id: number;
  sourceActivityId: number;
  name: string;
  sportType: string;
  startDate: string;
  distanceMeters: number;
  movingTimeSeconds: number;
  averageSpeedMetersPerSecond: number;
  averagePaceSecondsPerKilometer: number | null;
  streamStatus: string;
}

export interface ActivityList {
  activities: ActivityListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class ActivityService {
  private readonly http = inject(HttpClient);

  list(): Observable<ActivityList> {
    return this.http.get<ActivityList>('/api/strava/activities?page=0&size=10');
  }
}
