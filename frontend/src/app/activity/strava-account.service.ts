import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type StravaPermissionState = 'UNLINKED' | 'READY' | 'PERMISSION_UPGRADE_REQUIRED';

export type StravaSummaryStatus =
  | 'UNLINKED'
  | 'PERMISSION_UPGRADE_REQUIRED'
  | 'NOT_STARTED'
  | 'QUEUED'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED';

export type StravaPerformanceDataStatus =
  | 'UNLINKED'
  | 'PERMISSION_UPGRADE_REQUIRED'
  | 'NOT_STARTED'
  | 'PENDING'
  | 'READY'
  | 'FAILED';

export type StravaSyncFailureReason =
  | 'SYNC_ALREADY_RUNNING'
  | 'PERMISSION_UPGRADE_REQUIRED'
  | 'RECONNECT_REQUIRED'
  | 'STRAVA_RATE_LIMITED'
  | 'STRAVA_UNAVAILABLE'
  | 'UNKNOWN_SYNC_FAILURE';

export interface StravaLinkStatus {
  linked: boolean;
  athleteId: number | null;
  permissionState: StravaPermissionState;
  activitySummaryStatus: StravaSummaryStatus;
  performanceDataStatus: StravaPerformanceDataStatus;
  lastSummarySyncTime: string | null;
  lastStreamEnrichmentTime: string | null;
  importedActivityCount: number;
  streamsReadyActivityCount: number;
  failureReason: StravaSyncFailureReason | null;
}

export type StravaActivitySyncStatus = 'UNLINKED' | 'COMPLETED' | 'FAILED';

export type StravaActivitySyncFailureReason =
  | 'SYNC_ALREADY_RUNNING'
  | 'PERMISSION_UPGRADE_REQUIRED'
  | 'RECONNECT_REQUIRED'
  | 'STRAVA_RATE_LIMITED'
  | 'STRAVA_UNAVAILABLE'
  | 'UNKNOWN_SYNC_FAILURE';

export interface StravaActivitySyncResult {
  status: StravaActivitySyncStatus;
  failureReason: StravaActivitySyncFailureReason | null;
  importedActivityCount: number;
  totalActivityCount: number;
}

interface StravaAuthorizationUrl {
  authorizationUrl: string;
}

@Injectable({ providedIn: 'root' })
export class StravaAccountService {
  private readonly http = inject(HttpClient);

  status(): Observable<StravaLinkStatus> {
    return this.http.get<StravaLinkStatus>('/api/strava/status');
  }

  startLinking(): Observable<StravaAuthorizationUrl> {
    return this.http.post<StravaAuthorizationUrl>('/api/strava/link', {});
  }

  requestSync(): Observable<StravaActivitySyncResult> {
    return this.http.post<StravaActivitySyncResult>('/api/strava/sync', {});
  }
}
