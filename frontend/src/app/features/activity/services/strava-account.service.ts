import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { StravaAccountGateway, STRAVA_ACCOUNT_GATEWAY } from './strava-account.gateway';
import { StravaActivitySyncResult } from './dtos/strava-activity-sync';
import { StravaAuthorizationUrl } from './dtos/strava-authorization-url';
import { StravaDataConsentStatus } from './dtos/strava-data-consent';
import { StravaLinkStatus } from './dtos/strava-link-status';

@Injectable({ providedIn: 'root' })
export class StravaAccountService {
  private readonly stravaAccountGateway: StravaAccountGateway = inject(STRAVA_ACCOUNT_GATEWAY);

  status(): Observable<StravaLinkStatus> {
    return this.stravaAccountGateway.status();
  }

  consentStatus(): Observable<StravaDataConsentStatus> {
    return this.stravaAccountGateway.consentStatus();
  }

  startLinking(acceptedStravaDataConsent: boolean): Observable<StravaAuthorizationUrl> {
    return this.stravaAccountGateway.startLinking({
      acceptedStravaDataConsent,
      language: 'pt-BR',
    });
  }

  requestSync(): Observable<StravaActivitySyncResult> {
    return this.stravaAccountGateway.requestSync();
  }

  unlink(): Observable<void> {
    return this.stravaAccountGateway.unlink();
  }
}
