import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import { ActivityGateway } from './activity.gateway';
import { ActivityDetail } from './dtos/activity-detail';
import { ActivityList, ActivityListItem } from './dtos/activity-list';

@Injectable()
export class ActivityGatewayStub implements ActivityGateway {
  list(page = 0): Observable<ActivityList> {
    return of({
      activities: [],
      page,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    });
  }

  getDetail(activityId: number): Observable<ActivityDetail> {
    const summary: ActivityListItem = {
      id: activityId,
      sourceActivityId: activityId,
      name: 'Stub run',
      sportType: 'RUN',
      startDate: '2026-05-10T09:00:00Z',
      distanceMeters: 0,
      movingTimeSeconds: 0,
      averageSpeedMetersPerSecond: 0,
      averagePaceSecondsPerKilometer: null,
      streamStatus: 'PENDING',
    };

    return of({
      ...summary,
      totalElevationGainMeters: null,
      maxSpeedMetersPerSecond: null,
      averageHeartRate: null,
      maxHeartRate: null,
      averageCadence: null,
      averageWatts: null,
      calories: null,
      availableStreamMetricNames: [],
      enrichmentStatus: 'PENDING',
    });
  }
}
