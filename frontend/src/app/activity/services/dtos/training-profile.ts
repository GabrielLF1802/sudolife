export interface TrainingProfile {
  birthYear: number | null;
  adaptiveCoachingEligible: boolean;
  heartRateZoneSource: 'AGE_BASED' | 'STRAVA' | 'UNAVAILABLE';
  heartRateZones: TrainingHeartRateZone[];
}

export interface TrainingHeartRateZone {
  minimumHeartRate: number;
  maximumHeartRate: number;
}

export interface SaveTrainingProfileCommand {
  birthYear: number | null;
}
