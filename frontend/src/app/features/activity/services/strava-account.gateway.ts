import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';

import { StravaActivitySyncResult } from './dtos/strava-activity-sync';
import { StravaAuthorizationUrl } from './dtos/strava-authorization-url';
import { StartStravaLinkingRequest, StravaDataConsentStatus } from './dtos/strava-data-consent';
import { StravaLinkStatus } from './dtos/strava-link-status';

export interface StravaAccountGateway {
  status(): Observable<StravaLinkStatus>;
  consentStatus(): Observable<StravaDataConsentStatus>;
  startLinking(request: StartStravaLinkingRequest): Observable<StravaAuthorizationUrl>;
  requestSync(): Observable<StravaActivitySyncResult>;
  unlink(): Observable<void>;
}

export const STRAVA_ACCOUNT_GATEWAY = new InjectionToken<StravaAccountGateway>(
  'StravaAccountGateway',
);
