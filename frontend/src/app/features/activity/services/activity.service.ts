import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ACTIVITY_GATEWAY } from './activity.gateway';
import { ActivityDetail } from './dtos/activity-detail';
import { ActivityList } from './dtos/activity-list';

@Injectable({ providedIn: 'root' })
export class ActivityService {
  private readonly activityGateway = inject(ACTIVITY_GATEWAY);

  list(page = 0): Observable<ActivityList> {
    return this.activityGateway.list(page);
  }

  getDetail(activityId: number): Observable<ActivityDetail> {
    return this.activityGateway.getDetail(activityId);
  }
}
