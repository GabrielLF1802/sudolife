import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { StravaAccountGateway } from './strava-account.gateway';
import { StravaActivitySyncResult } from './dtos/strava-activity-sync';
import { StravaAuthorizationUrl } from './dtos/strava-authorization-url';
import { StartStravaLinkingRequest, StravaDataConsentStatus } from './dtos/strava-data-consent';
import { StravaLinkStatus } from './dtos/strava-link-status';

@Injectable({ providedIn: 'root' })
export class StravaAccountGatewayImpl implements StravaAccountGateway {
  private readonly http = inject(HttpClient);

  status(): Observable<StravaLinkStatus> {
    return this.http.get<StravaLinkStatus>('/api/strava/status');
  }

  consentStatus(): Observable<StravaDataConsentStatus> {
    return this.http.get<StravaDataConsentStatus>('/api/strava/data-consent/status');
  }

  startLinking(request: StartStravaLinkingRequest): Observable<StravaAuthorizationUrl> {
    return this.http.post<StravaAuthorizationUrl>('/api/strava/link', request);
  }

  requestSync(): Observable<StravaActivitySyncResult> {
    return this.http.post<StravaActivitySyncResult>('/api/strava/sync', {});
  }

  unlink(): Observable<void> {
    return this.http.delete<void>('/api/strava/link');
  }
}
