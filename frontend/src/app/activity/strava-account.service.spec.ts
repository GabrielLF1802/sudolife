import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { StravaAccountService } from './strava-account.service';

describe('StravaAccountService', () => {
  let service: StravaAccountService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [StravaAccountService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(StravaAccountService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should_load_strava_link_status', () => {
    service.status().subscribe((status) => {
      expect(status.permissionState).toBe('READY');
      expect(status.activitySummaryStatus).toBe('COMPLETED');
      expect(status.importedActivityCount).toBe(4);
    });

    const request = httpTestingController.expectOne('/api/strava/status');
    expect(request.request.method).toBe('GET');
    request.flush({
      linked: true,
      athleteId: 123,
      permissionState: 'READY',
      activitySummaryStatus: 'COMPLETED',
      performanceDataStatus: 'PENDING',
      lastSummarySyncTime: '2026-05-11T12:00:00Z',
      lastStreamEnrichmentTime: null,
      importedActivityCount: 4,
      streamsReadyActivityCount: 1,
      failureReason: null,
    });
  });

  it('should_start_strava_linking', () => {
    service.startLinking().subscribe((result) => {
      expect(result.authorizationUrl).toBe('https://strava.example/oauth');
    });

    const request = httpTestingController.expectOne('/api/strava/link');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
    request.flush({ authorizationUrl: 'https://strava.example/oauth' });
  });

  it('should_request_manual_activity_sync', () => {
    service.requestSync().subscribe((result) => {
      expect(result.status).toBe('COMPLETED');
      expect(result.importedActivityCount).toBe(3);
      expect(result.totalActivityCount).toBe(12);
    });

    const request = httpTestingController.expectOne('/api/strava/sync');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
    request.flush({
      status: 'COMPLETED',
      failureReason: null,
      importedActivityCount: 3,
      totalActivityCount: 12,
    });
  });
});
