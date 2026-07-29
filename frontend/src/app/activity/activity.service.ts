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

export interface ActivityDetail extends ActivityListItem {
  totalElevationGainMeters: number | null;
  maxSpeedMetersPerSecond: number | null;
  averageHeartRate: number | null;
  maxHeartRate: number | null;
  averageCadence: number | null;
  averageWatts: number | null;
  calories: number | null;
  availableStreamMetricNames: string[];
  enrichmentStatus: string;
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

  list(page = 0): Observable<ActivityList> {
    return this.http.get<ActivityList>(`/api/strava/activities?page=${page}&size=10`);
  }

  getDetail(activityId: number): Observable<ActivityDetail> {
    return this.http.get<ActivityDetail>(`/api/strava/activities/${activityId}`);
  }
}
