import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';

import { StravaActivitySyncResult } from './services/dtos/strava-activity-sync';
import { StravaAuthorizationUrl } from './services/dtos/strava-authorization-url';
import { StravaLinkStatus } from './services/dtos/strava-link-status';

export interface StravaAccountGateway {
  status(): Observable<StravaLinkStatus>;
  startLinking(): Observable<StravaAuthorizationUrl>;
  requestSync(): Observable<StravaActivitySyncResult>;
  unlink(): Observable<void>;
}

export const STRAVA_ACCOUNT_GATEWAY = new InjectionToken<StravaAccountGateway>(
  'StravaAccountGateway',
);
