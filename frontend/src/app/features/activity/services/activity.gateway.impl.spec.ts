import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ActivityGatewayImpl } from './activity.gateway.impl';

describe('ActivityGatewayImpl', () => {
  let gateway: ActivityGatewayImpl;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ActivityGatewayImpl, provideHttpClient(), provideHttpClientTesting()],
    });

    gateway = TestBed.inject(ActivityGatewayImpl);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should_load_requested_activity_summary_page', () => {
    gateway.list(2).subscribe((activityList) => {
      expect(activityList.page).toBe(2);
    });

    const request = httpTestingController.expectOne('/api/strava/activities?page=2&size=10');
    expect(request.request.method).toBe('GET');
    request.flush({
      activities: [],
      page: 2,
      size: 10,
      totalElements: 30,
      totalPages: 3,
    });
  });

  it('should_load_activity_detail', () => {
    const detail = {
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

    gateway.getDetail(99).subscribe((activity) => {
      expect(activity).toEqual(detail);
    });

    const request = httpTestingController.expectOne('/api/strava/activities/99');
    expect(request.request.method).toBe('GET');
    request.flush(detail);
  });
});
