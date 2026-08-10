export type StravaPermissionState =
  'UNLINKED' | 'READY' | 'PERMISSION_UPGRADE_REQUIRED' | 'RECONNECT_REQUIRED';

export type StravaProfilePermissionState =
  'UNLINKED' | 'AVAILABLE' | 'OPTIONAL_UPGRADE_AVAILABLE' | 'RECONNECT_REQUIRED';

export type StravaSummaryStatus =
  | 'UNLINKED'
  | 'PERMISSION_UPGRADE_REQUIRED'
  | 'NOT_STARTED'
  | 'QUEUED'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED';

export type StravaPerformanceDataStatus =
  'UNLINKED' | 'PERMISSION_UPGRADE_REQUIRED' | 'NOT_STARTED' | 'PENDING' | 'READY' | 'FAILED';

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
  profilePermissionState: StravaProfilePermissionState;
  activitySummaryStatus: StravaSummaryStatus;
  performanceDataStatus: StravaPerformanceDataStatus;
  lastSummarySyncTime: string | null;
  lastStreamEnrichmentTime: string | null;
  importedActivityCount: number;
  streamsReadyActivityCount: number;
  failureReason: StravaSyncFailureReason | null;
}
