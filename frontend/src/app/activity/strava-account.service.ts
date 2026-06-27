import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type StravaPermissionState = 'UNLINKED' | 'READY' | 'PERMISSION_UPGRADE_REQUIRED';

export interface StravaLinkStatus {
  linked: boolean;
  athleteId: number | null;
  permissionState: StravaPermissionState;
}

export type StravaActivitySyncStatus = 'UNLINKED' | 'COMPLETED' | 'FAILED';

export type StravaActivitySyncFailureReason =
  | 'SYNC_ALREADY_RUNNING'
  | 'PERMISSION_UPGRADE_REQUIRED'
  | 'STRAVA_RATE_LIMITED'
  | 'STRAVA_UNAVAILABLE';

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
