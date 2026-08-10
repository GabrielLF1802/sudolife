import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { StravaAccountGateway, STRAVA_ACCOUNT_GATEWAY } from './strava-account.gateway';
import { StravaActivitySyncResult } from './dtos/strava-activity-sync';
import { StravaAuthorizationUrl } from './dtos/strava-authorization-url';
import { StravaLinkStatus } from './dtos/strava-link-status';

@Injectable({ providedIn: 'root' })
export class StravaAccountService {
  private readonly stravaAccountGateway: StravaAccountGateway = inject(STRAVA_ACCOUNT_GATEWAY);

  status(): Observable<StravaLinkStatus> {
    return this.stravaAccountGateway.status();
  }

  startLinking(): Observable<StravaAuthorizationUrl> {
    return this.stravaAccountGateway.startLinking();
  }

  requestSync(): Observable<StravaActivitySyncResult> {
    return this.stravaAccountGateway.requestSync();
  }

  unlink(): Observable<void> {
    return this.stravaAccountGateway.unlink();
  }
}
