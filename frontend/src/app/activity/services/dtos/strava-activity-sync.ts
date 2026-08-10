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
