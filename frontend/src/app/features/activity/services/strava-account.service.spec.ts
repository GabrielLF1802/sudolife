import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { STRAVA_ACCOUNT_GATEWAY, StravaAccountGateway } from './strava-account.gateway';
import { StravaAccountService } from './strava-account.service';

describe('StravaAccountService', () => {
  let service: StravaAccountService;
  let stravaAccountGateway: jasmine.SpyObj<StravaAccountGateway>;

  beforeEach(() => {
    stravaAccountGateway = jasmine.createSpyObj<StravaAccountGateway>('StravaAccountGateway', [
      'status',
      'consentStatus',
      'startLinking',
      'requestSync',
      'unlink',
    ]);
    stravaAccountGateway.status.and.returnValue(of(stravaStatus()));
    stravaAccountGateway.consentStatus.and.returnValue(of(stravaDataConsentStatus()));
    stravaAccountGateway.startLinking.and.returnValue(
      of({ authorizationUrl: 'https://strava.example/oauth' }),
    );
    stravaAccountGateway.requestSync.and.returnValue(
      of({
        status: 'COMPLETED',
        failureReason: null,
        importedActivityCount: 3,
        totalActivityCount: 12,
      }),
    );
    stravaAccountGateway.unlink.and.returnValue(of(undefined));

    TestBed.configureTestingModule({
      providers: [
        StravaAccountService,
        { provide: STRAVA_ACCOUNT_GATEWAY, useValue: stravaAccountGateway },
      ],
    });

    service = TestBed.inject(StravaAccountService);
  });

  it('should_load_strava_link_status', () => {
    service.status().subscribe((status) => {
      expect(status.permissionState).toBe('READY');
      expect(status.activitySummaryStatus).toBe('COMPLETED');
      expect(status.importedActivityCount).toBe(4);
    });

    expect(stravaAccountGateway.status).toHaveBeenCalledOnceWith();
  });

  it('should_start_strava_linking', () => {
    service.startLinking(true).subscribe((result) => {
      expect(result.authorizationUrl).toBe('https://strava.example/oauth');
    });

    expect(stravaAccountGateway.startLinking).toHaveBeenCalledOnceWith({
      acceptedStravaDataConsent: true,
      language: 'pt-BR',
    });
  });

  it('should_load_strava_data_consent_status', () => {
    service.consentStatus().subscribe((status) => {
      expect(status.valid).toBeTrue();
      expect(status.currentConsentVersion).toBe('strava-data-import-and-coaching-v1');
    });

    expect(stravaAccountGateway.consentStatus).toHaveBeenCalledOnceWith();
  });

  it('should_request_manual_activity_sync', () => {
    service.requestSync().subscribe((result) => {
      expect(result.status).toBe('COMPLETED');
      expect(result.importedActivityCount).toBe(3);
      expect(result.totalActivityCount).toBe(12);
    });

    expect(stravaAccountGateway.requestSync).toHaveBeenCalledOnceWith();
  });

  it('should_unlink_strava_account', () => {
    let completed = false;

    service.unlink().subscribe(() => {
      completed = true;
    });

    expect(stravaAccountGateway.unlink).toHaveBeenCalledOnceWith();
    expect(completed).toBeTrue();
  });

  function stravaStatus() {
    return {
      linked: true,
      athleteId: 123,
      permissionState: 'READY' as const,
      profilePermissionState: 'AVAILABLE' as const,
      activitySummaryStatus: 'COMPLETED' as const,
      performanceDataStatus: 'PENDING' as const,
      lastSummarySyncTime: '2026-05-11T12:00:00Z',
      lastStreamEnrichmentTime: null,
      importedActivityCount: 4,
      streamsReadyActivityCount: 1,
      failureReason: null,
    };
  }

  function stravaDataConsentStatus() {
    return {
      valid: true,
      currentConsentVersion: 'strava-data-import-and-coaching-v1',
      purpose: 'STRAVA_DATA_IMPORT_AND_COACHING' as const,
    };
  }
});
