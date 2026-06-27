import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { ActivityDashboardComponent } from './activity-dashboard.component';
import { ActivityList, ActivityService } from './activity.service';
import { StravaAccountService } from './strava-account.service';

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
    ]);
    stravaAccountService.status.and.returnValue(
      of({ linked: false, athleteId: null, permissionState: 'UNLINKED' }),
    );
    stravaAccountService.startLinking.and.returnValue(
      of({ authorizationUrl: 'https://strava.example/oauth' }),
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
    stravaAccountService.status.and.returnValue(
      of({ linked: true, athleteId: 123, permissionState: 'READY' }),
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Conectado ao atleta 123');
    expect(fixture.nativeElement.textContent).toContain('Reconectar Strava');
  });

  it('should_render_permission_upgrade_action_when_scope_is_incomplete', () => {
    stravaAccountService.status.and.returnValue(
      of({ linked: true, athleteId: 123, permissionState: 'PERMISSION_UPGRADE_REQUIRED' }),
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Permissoes incompletas');
    expect(fixture.nativeElement.textContent).toContain('Atualizar permissoes');
  });

  it('should_render_imported_activity_summary_fields', () => {
    activityService.list.and.returnValue(of(activityListWithSummaries()));
    stravaAccountService.status.and.returnValue(
      of({ linked: true, athleteId: 123, permissionState: 'READY' }),
    );
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

  it('should_show_connected_empty_state_without_summary_metric_cards', () => {
    stravaAccountService.status.and.returnValue(
      of({ linked: true, athleteId: 123, permissionState: 'READY' }),
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nenhuma atividade importada ainda.');
    expect(fixture.nativeElement.querySelector('.metrics')).toBeNull();
  });

  it('should_show_reconnect_guidance_when_strava_is_not_sync_enabled', () => {
    stravaAccountService.status.and.returnValue(
      of({ linked: true, athleteId: 123, permissionState: 'PERMISSION_UPGRADE_REQUIRED' }),
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'Conecte ou atualize a conexao com o Strava para importar atividades.',
    );
    expect(fixture.nativeElement.textContent).toContain('Atualizar permissoes');
    expect(fixture.nativeElement.textContent).not.toContain('Nenhuma atividade importada ainda.');
  });

  it('should_load_next_activity_page', () => {
    activityService.list.and.returnValues(of(activityListWithSummaries()), of(secondActivityPage()));
    stravaAccountService.status.and.returnValue(
      of({ linked: true, athleteId: 123, permissionState: 'READY' }),
    );
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

  function stravaButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.strava-action');
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
