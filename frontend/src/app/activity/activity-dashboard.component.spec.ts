import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { ActivityDashboardComponent } from './activity-dashboard.component';
import { ActivityList, ActivityService } from './activity.service';
import { StravaAccountService, StravaLinkStatus } from './strava-account.service';

describe('ActivityDashboardComponent', () => {
  let fixture: ComponentFixture<ActivityDashboardComponent>;
  let activityService: jasmine.SpyObj<ActivityService>;
  let stravaAccountService: jasmine.SpyObj<StravaAccountService>;

  beforeEach(async () => {
    activityService = jasmine.createSpyObj<ActivityService>('ActivityService', ['list']);
    activityService.list.and.returnValue(of(emptyActivityList()));

    stravaAccountService = jasmine.createSpyObj<StravaAccountService>('StravaAccountService', [
      'status',
      'startLinking',
      'requestSync',
    ]);
    stravaAccountService.status.and.returnValue(of(stravaStatus('UNLINKED')));
    stravaAccountService.startLinking.and.returnValue(
      of({ authorizationUrl: 'https://strava.example/oauth' }),
    );
    stravaAccountService.requestSync.and.returnValue(
      of({
        status: 'COMPLETED',
        failureReason: null,
        importedActivityCount: 2,
        totalActivityCount: 12,
      }),
    );

    await TestBed.configureTestingModule({
      imports: [ActivityDashboardComponent],
      providers: [
        {
          provide: AuthService,
          useValue: {
            currentUser: () => of({ id: 1, name: 'Gabriel', email: 'gabriel@example.com' }),
            logout: () => undefined,
          },
        },
        {
          provide: ActivityService,
          useValue: activityService,
        },
        { provide: StravaAccountService, useValue: stravaAccountService },
        { provide: Router, useValue: { navigateByUrl: () => Promise.resolve(true) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ActivityDashboardComponent);
  });

  it('should_render_connect_action_when_strava_is_unlinked', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nao conectado');
    expect(fixture.nativeElement.textContent).toContain('Conectar Strava');
  });

  it('should_render_reconnect_action_when_strava_is_linked', () => {
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Conectado ao atleta 123');
    expect(fixture.nativeElement.textContent).toContain('Reconectar Strava');
  });

  it('should_render_manual_sync_action_when_dashboard_loads', () => {
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Sincronizar agora');
  });

  it('should_render_permission_upgrade_action_when_scope_is_incomplete', () => {
    stravaAccountService.status.and.returnValue(of(stravaStatus('PERMISSION_UPGRADE_REQUIRED')));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Permissoes incompletas');
    expect(fixture.nativeElement.textContent).toContain('Atualizar permissoes');
  });

  it('should_render_imported_activity_summary_fields', () => {
    activityService.list.and.returnValue(of(activityListWithSummaries()));
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    const textContent = fixture.nativeElement.textContent;
    expect(textContent).toContain('Morning Run');
    expect(textContent).toContain('RUN');
    expect(textContent).toContain('10/05/2026 06:00');
    expect(textContent).toContain('5.0 km');
    expect(textContent).toContain('25 min');
    expect(textContent).toContain('5:00 /km');
    expect(textContent).toContain('PENDING');
  });

  it('should_filter_loaded_activity_page_by_type', () => {
    activityService.list.and.returnValue(of(filterableActivityList()));
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    selectFilterValue(0, 'RIDE');

    expect(pageText()).toContain('Older Ride');
    expect(pageText()).not.toContain('Recent Run');
    expect(pageText()).not.toContain('Tempo Run');
    expect(pageText()).toContain('Mostrando 1 de 3 atividades nesta pagina carregada.');
  });

  it('should_filter_loaded_activity_page_by_period', () => {
    activityService.list.and.returnValue(of(filterableActivityList()));
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    selectFilterValue(1, 'LAST_7_DAYS');

    expect(pageText()).toContain('Recent Run');
    expect(pageText()).not.toContain('Older Ride');
    expect(pageText()).toContain('Mostrando 1 de 3 atividades nesta pagina carregada.');
  });

  it('should_filter_loaded_activity_page_by_distance_in_kilometers', () => {
    activityService.list.and.returnValue(of(filterableActivityList()));
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    typeDistanceValue('input[aria-label="Distancia minima em quilometros"]', '6');
    typeDistanceValue('input[aria-label="Distancia maxima em quilometros"]', '15');

    expect(pageText()).toContain('Tempo Run');
    expect(pageText()).not.toContain('Recent Run');
    expect(pageText()).not.toContain('Older Ride');
    expect(pageText()).toContain('Mostrando 1 de 3 atividades nesta pagina carregada.');
  });

  it('should_show_filtered_empty_state_for_loaded_page_only', () => {
    activityService.list.and.returnValue(of(filterableActivityList()));
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    typeDistanceValue('input[aria-label="Distancia minima em quilometros"]', '80');

    expect(pageText()).toContain('Mostrando 0 de 3 atividades nesta pagina carregada.');
    expect(pageText()).toContain(
      'Nenhuma atividade nesta pagina carregada corresponde aos filtros.',
    );
    expect(pageText()).not.toContain('Recent Run');
  });

  it('should_show_connected_empty_state_without_summary_metric_cards', () => {
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nenhuma atividade importada ainda.');
    expect(fixture.nativeElement.querySelector('.metrics')).toBeNull();
  });

  it('should_show_reconnect_guidance_when_strava_is_not_sync_enabled', () => {
    stravaAccountService.status.and.returnValue(of(stravaStatus('PERMISSION_UPGRADE_REQUIRED')));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'Conecte ou atualize a conexao com o Strava para importar atividades.',
    );
    expect(fixture.nativeElement.textContent).toContain('Atualizar permissoes');
    expect(fixture.nativeElement.textContent).not.toContain('Nenhuma atividade importada ainda.');
  });

  it('should_load_next_activity_page', () => {
    activityService.list.and.returnValues(
      of(activityListWithSummaries()),
      of(secondActivityPage()),
    );
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    fixture.nativeElement.querySelector('.next-page').click();
    fixture.detectChanges();

    expect(activityService.list).toHaveBeenCalledWith(1);
    expect(fixture.nativeElement.textContent).toContain('Evening Ride');
  });

  it('should_show_linking_error_when_oauth_launch_fails', () => {
    stravaAccountService.startLinking.and.returnValue(throwError(() => new Error('failed')));
    fixture.detectChanges();

    stravaButton().click();
    fixture.detectChanges();

    expect(stravaAccountService.startLinking).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'Nao foi possivel iniciar a conexao com o Strava.',
    );
  });

  it('should_show_manual_sync_result', () => {
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    syncButton().click();
    fixture.detectChanges();

    expect(stravaAccountService.requestSync).toHaveBeenCalled();
    expect(pageText()).toContain('Sincronizacao solicitada');
    expect(pageText()).toContain('Importadas2');
    expect(pageText()).toContain('Total12');
  });

  it('should_show_manual_sync_failure_reason_with_reconnect_guidance', () => {
    stravaAccountService.status.and.returnValue(of(stravaStatus('PERMISSION_UPGRADE_REQUIRED')));
    stravaAccountService.requestSync.and.returnValue(
      of({
        status: 'FAILED',
        failureReason: 'PERMISSION_UPGRADE_REQUIRED',
        importedActivityCount: 0,
        totalActivityCount: 4,
      }),
    );
    fixture.detectChanges();

    syncButton().click();
    fixture.detectChanges();

    expect(pageText()).toContain('Sincronizacao nao solicitada');
    expect(pageText()).toContain('Importadas0');
    expect(pageText()).toContain('Total4');
    expect(pageText()).toContain('Atualize as permissoes do Strava para importar atividades.');
    expect(pageText()).toContain('Atualizar permissoes');
  });

  it('should_keep_dashboard_usable_when_manual_sync_request_fails', () => {
    activityService.list.and.returnValue(of(activityListWithSummaries()));
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    stravaAccountService.requestSync.and.returnValue(throwError(() => new Error('failed')));
    fixture.detectChanges();

    syncButton().click();
    fixture.detectChanges();

    expect(pageText()).toContain('Nao foi possivel solicitar a sincronizacao.');
    expect(pageText()).toContain('Morning Run');
    expect(fixture.nativeElement.querySelector('.next-page').disabled).toBeFalse();
  });

  function stravaButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.strava-action');
  }

  function syncButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.sync-action');
  }

  function pageText(): string {
    return fixture.nativeElement.textContent.replace(/\s+/g, ' ').trim();
  }

  function selectFilterValue(index: number, value: string): void {
    const select = fixture.nativeElement.querySelectorAll('select')[index] as HTMLSelectElement;
    select.value = value;

    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  function typeDistanceValue(selector: string, value: string): void {
    const input = fixture.nativeElement.querySelector(selector) as HTMLInputElement;
    input.value = value;

    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function stravaStatus(permissionState: StravaLinkStatus['permissionState']): StravaLinkStatus {
    return {
      linked: permissionState !== 'UNLINKED',
      athleteId: permissionState === 'UNLINKED' ? null : 123,
      permissionState,
      activitySummaryStatus: activitySummaryStatus(permissionState),
      performanceDataStatus: permissionState === 'READY' ? 'PENDING' : permissionState,
      lastSummarySyncTime: permissionState === 'READY' ? '2026-05-11T12:00:00Z' : null,
      lastStreamEnrichmentTime: null,
      importedActivityCount: permissionState === 'READY' ? 2 : 0,
      streamsReadyActivityCount: 0,
      failureReason:
        permissionState === 'PERMISSION_UPGRADE_REQUIRED' ? 'PERMISSION_UPGRADE_REQUIRED' : null,
    };
  }

  function activitySummaryStatus(
    permissionState: StravaLinkStatus['permissionState'],
  ): StravaLinkStatus['activitySummaryStatus'] {
    if (permissionState === 'READY') {
      return 'COMPLETED';
    }

    return permissionState;
  }

  function emptyActivityList(): ActivityList {
    return {
      activities: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    };
  }

  function activityListWithSummaries(): ActivityList {
    return {
      activities: [
        {
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
        },
      ],
      page: 0,
      size: 10,
      totalElements: 2,
      totalPages: 2,
    };
  }

  function filterableActivityList(): ActivityList {
    return {
      activities: [
        {
          id: 200,
          sourceActivityId: 2001,
          name: 'Recent Run',
          sportType: 'RUN',
          startDate: daysAgo(3),
          distanceMeters: 5000,
          movingTimeSeconds: 1500,
          averageSpeedMetersPerSecond: 3.33,
          averagePaceSecondsPerKilometer: 300,
          streamStatus: 'PENDING',
        },
        {
          id: 201,
          sourceActivityId: 2002,
          name: 'Tempo Run',
          sportType: 'RUN',
          startDate: daysAgo(15),
          distanceMeters: 10000,
          movingTimeSeconds: 3000,
          averageSpeedMetersPerSecond: 3.33,
          averagePaceSecondsPerKilometer: 300,
          streamStatus: 'PENDING',
        },
        {
          id: 202,
          sourceActivityId: 2003,
          name: 'Older Ride',
          sportType: 'RIDE',
          startDate: daysAgo(40),
          distanceMeters: 30000,
          movingTimeSeconds: 3600,
          averageSpeedMetersPerSecond: 8.33,
          averagePaceSecondsPerKilometer: null,
          streamStatus: 'IMPORTED',
        },
      ],
      page: 0,
      size: 10,
      totalElements: 20,
      totalPages: 2,
    };
  }

  function daysAgo(days: number): string {
    const date = new Date();
    date.setDate(date.getDate() - days);

    return date.toISOString();
  }

  function secondActivityPage(): ActivityList {
    return {
      activities: [
        {
          id: 100,
          sourceActivityId: 789,
          name: 'Evening Ride',
          sportType: 'RIDE',
          startDate: '2026-05-11T21:00:00Z',
          distanceMeters: 30000,
          movingTimeSeconds: 3600,
          averageSpeedMetersPerSecond: 8.33,
          averagePaceSecondsPerKilometer: null,
          streamStatus: 'IMPORTED',
        },
      ],
      page: 1,
      size: 10,
      totalElements: 2,
      totalPages: 2,
    };
  }
});
