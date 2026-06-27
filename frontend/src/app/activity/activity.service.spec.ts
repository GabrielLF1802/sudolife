import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ActivityService } from './activity.service';

describe('ActivityService', () => {
  let service: ActivityService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ActivityService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(ActivityService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should_load_first_activity_summary_page_by_default', () => {
    service.list().subscribe((activityList) => {
      expect(activityList.totalElements).toBe(1);
    });

    const request = httpTestingController.expectOne('/api/strava/activities?page=0&size=10');
    expect(request.request.method).toBe('GET');
    request.flush({
      activities: [activitySummary()],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
    });
  });

  it('should_load_requested_activity_summary_page', () => {
    service.list(2).subscribe((activityList) => {
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
});
