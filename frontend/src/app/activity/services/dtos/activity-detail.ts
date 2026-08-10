import { ActivityListItem } from './activity-list';

export interface ActivityDetail extends ActivityListItem {
  totalElevationGainMeters: number | null;
  maxSpeedMetersPerSecond: number | null;
  averageHeartRate: number | null;
  maxHeartRate: number | null;
  averageCadence: number | null;
  averageWatts: number | null;
  calories: number | null;
  availableStreamMetricNames: string[];
  enrichmentStatus: string;
}
