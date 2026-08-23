import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import { StravaAccountGateway } from './strava-account.gateway';
import { StravaActivitySyncResult } from './dtos/strava-activity-sync';
import { StravaAuthorizationUrl } from './dtos/strava-authorization-url';
import { StartStravaLinkingRequest, StravaDataConsentStatus } from './dtos/strava-data-consent';
import { StravaLinkStatus } from './dtos/strava-link-status';

@Injectable()
export class StravaAccountGatewayStub implements StravaAccountGateway {
  status(): Observable<StravaLinkStatus> {
    return of({
      linked: false,
      athleteId: null,
      permissionState: 'UNLINKED',
      profilePermissionState: 'UNLINKED',
      activitySummaryStatus: 'UNLINKED',
      performanceDataStatus: 'UNLINKED',
      lastSummarySyncTime: null,
      lastStreamEnrichmentTime: null,
      importedActivityCount: 0,
      streamsReadyActivityCount: 0,
      failureReason: null,
    });
  }

  consentStatus(): Observable<StravaDataConsentStatus> {
    return of({
      valid: false,
      currentConsentVersion: 'strava-data-import-and-coaching-v1',
      purpose: 'STRAVA_DATA_IMPORT_AND_COACHING',
    });
  }

  startLinking(_request: StartStravaLinkingRequest): Observable<StravaAuthorizationUrl> {
    return of({ authorizationUrl: 'https://strava.example/oauth' });
  }

  requestSync(): Observable<StravaActivitySyncResult> {
    return of({
      status: 'UNLINKED',
      failureReason: null,
      importedActivityCount: 0,
      totalActivityCount: 0,
    });
  }

  unlink(): Observable<void> {
    return of(undefined);
  }
}
