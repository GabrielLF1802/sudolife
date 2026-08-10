import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ActivityGateway } from './activity.gateway';
import { ActivityDetail } from './dtos/activity-detail';
import { ActivityList } from './dtos/activity-list';

@Injectable({ providedIn: 'root' })
export class ActivityGatewayImpl implements ActivityGateway {
  private readonly http = inject(HttpClient);

  list(page: number): Observable<ActivityList> {
    return this.http.get<ActivityList>(`/api/strava/activities?page=${page}&size=10`);
  }

  getDetail(activityId: number): Observable<ActivityDetail> {
    return this.http.get<ActivityDetail>(`/api/strava/activities/${activityId}`);
  }
}
