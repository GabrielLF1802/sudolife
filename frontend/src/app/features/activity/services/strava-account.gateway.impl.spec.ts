import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { StravaAccountGatewayImpl } from './strava-account.gateway.impl';

describe('StravaAccountGatewayImpl', () => {
  let gateway: StravaAccountGatewayImpl;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [StravaAccountGatewayImpl, provideHttpClient(), provideHttpClientTesting()],
    });

    gateway = TestBed.inject(StravaAccountGatewayImpl);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should_load_strava_link_status', () => {
    gateway.status().subscribe((status) => {
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
      profilePermissionState: 'AVAILABLE',
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
    gateway
      .startLinking({ acceptedStravaDataConsent: true, language: 'pt-BR' })
      .subscribe((result) => {
      expect(result.authorizationUrl).toBe('https://strava.example/oauth');
    });

    const request = httpTestingController.expectOne('/api/strava/link');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      acceptedStravaDataConsent: true,
      language: 'pt-BR',
    });
    request.flush({ authorizationUrl: 'https://strava.example/oauth' });
  });

  it('should_load_strava_data_consent_status', () => {
    gateway.consentStatus().subscribe((status) => {
      expect(status.valid).toBeTrue();
      expect(status.currentConsentVersion).toBe('strava-data-import-and-coaching-v1');
    });

    const request = httpTestingController.expectOne('/api/strava/data-consent/status');
    expect(request.request.method).toBe('GET');
    request.flush({
      valid: true,
      currentConsentVersion: 'strava-data-import-and-coaching-v1',
      purpose: 'STRAVA_DATA_IMPORT_AND_COACHING',
    });
  });

  it('should_request_manual_activity_sync', () => {
    gateway.requestSync().subscribe((result) => {
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

  it('should_unlink_strava_account', () => {
    let completed = false;

    gateway.unlink().subscribe(() => {
      completed = true;
    });

    const request = httpTestingController.expectOne('/api/strava/link');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
    expect(completed).toBeTrue();
  });
});
