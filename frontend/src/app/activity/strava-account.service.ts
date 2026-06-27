import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type StravaPermissionState = 'UNLINKED' | 'READY' | 'PERMISSION_UPGRADE_REQUIRED';

export interface StravaLinkStatus {
  linked: boolean;
  athleteId: number | null;
  permissionState: StravaPermissionState;
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
}
