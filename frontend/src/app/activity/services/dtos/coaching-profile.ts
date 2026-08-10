export type UserReportedReadiness = 'LOW' | 'MODERATE' | 'HIGH';

export type RunningDay =
  'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface CoachingProfile {
  targetDistanceKilometers: number | null;
  targetPaceSecondsPerKilometer: number | null;
  targetDate: string | null;
  readiness: UserReportedReadiness | null;
  injuryConcern: boolean;
  preferredRunningDays: RunningDay[];
  configured: boolean;
}

export interface SaveCoachingProfileCommand {
  targetDistanceKilometers: number | null;
  targetPaceSecondsPerKilometer: number | null;
  targetDate: string | null;
  readiness: UserReportedReadiness | '';
  injuryConcern: boolean;
  preferredRunningDays: RunningDay[];
}

export interface RunningHistorySnapshot {
  sufficientRunningHistory: boolean;
  activeWeeks: number;
  runningActivityCount: number;
  totalDistanceKilometers: number;
  totalMovingTimeSeconds: number;
  latestRunAt: string | null;
}
