import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import { StravaAccountGateway } from './strava-account.gateway';
import { StravaActivitySyncResult } from './dtos/strava-activity-sync';
import { StravaAuthorizationUrl } from './dtos/strava-authorization-url';
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

  startLinking(): Observable<StravaAuthorizationUrl> {
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
