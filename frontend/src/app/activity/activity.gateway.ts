import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';

import { ActivityDetail } from './services/dtos/activity-detail';
import { ActivityList } from './services/dtos/activity-list';

export interface ActivityGateway {
  list(page: number): Observable<ActivityList>;
  getDetail(activityId: number): Observable<ActivityDetail>;
}

export const ACTIVITY_GATEWAY = new InjectionToken<ActivityGateway>('ActivityGateway');
