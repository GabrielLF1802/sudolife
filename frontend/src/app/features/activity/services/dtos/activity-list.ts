export interface ActivityListItem {
  id: number;
  sourceActivityId: number;
  name: string;
  sportType: string;
  startDate: string;
  distanceMeters: number;
  movingTimeSeconds: number;
  averageSpeedMetersPerSecond: number;
  averagePaceSecondsPerKilometer: number | null;
  streamStatus: string;
}

export interface ActivityList {
  activities: ActivityListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
