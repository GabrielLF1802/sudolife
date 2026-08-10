import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ACTIVITY_GATEWAY, ActivityGateway } from './activity.gateway';
import { ActivityService } from './activity.service';

describe('ActivityService', () => {
  let service: ActivityService;
  let activityGateway: jasmine.SpyObj<ActivityGateway>;

  beforeEach(() => {
    activityGateway = jasmine.createSpyObj<ActivityGateway>('ActivityGateway', [
      'list',
      'getDetail',
    ]);
    activityGateway.list.and.returnValue(
      of({
        activities: [activitySummary()],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
      }),
    );
    activityGateway.getDetail.and.returnValue(of(activityDetail()));

    TestBed.configureTestingModule({
      providers: [ActivityService, { provide: ACTIVITY_GATEWAY, useValue: activityGateway }],
    });

    service = TestBed.inject(ActivityService);
  });

  it('should_load_first_activity_summary_page_by_default', () => {
    service.list().subscribe((activityList) => {
      expect(activityList.totalElements).toBe(1);
    });

    expect(activityGateway.list).toHaveBeenCalledOnceWith(0);
  });

  it('should_load_requested_activity_summary_page', () => {
    service.list(2).subscribe((activityList) => {
      expect(activityList.totalElements).toBe(1);
    });

    expect(activityGateway.list).toHaveBeenCalledOnceWith(2);
  });

  it('should_load_activity_detail', () => {
    service.getDetail(99).subscribe((activity) => {
      expect(activity.enrichmentStatus).toBe('READY');
    });

    expect(activityGateway.getDetail).toHaveBeenCalledOnceWith(99);
  });

  function activitySummary() {
    return {
      id: 99,
      sourceActivityId: 123456,
      name: 'Morning Run',
      sportType: 'RUN',
      startDate: '2026-05-10T09:00:00Z',
      distanceMeters: 5000,
      movingTimeSeconds: 1500,
      averageSpeedMetersPerSecond: 3.33,
      averagePaceSecondsPerKilometer: 300,
      streamStatus: 'PENDING',
    };
  }

  function activityDetail() {
    return {
      ...activitySummary(),
      totalElevationGainMeters: 82.5,
      maxSpeedMetersPerSecond: 4.8,
      averageHeartRate: 148,
      maxHeartRate: 172,
      averageCadence: 176,
      averageWatts: 245,
      calories: 410,
      availableStreamMetricNames: ['HEART_RATE', 'CADENCE'],
      enrichmentStatus: 'READY',
    };
  }
});
