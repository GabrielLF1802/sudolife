import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { ActivityDashboardComponent } from './activity-dashboard.component';
import { ActivityList, ActivityService } from './activity.service';
import { CoachingProfileService } from './coaching-profile.service';
import { StravaAccountService, StravaLinkStatus } from './strava-account.service';
import { TrainingProfileService } from './training-profile.service';

describe('ActivityDashboardComponent', () => {
  let fixture: ComponentFixture<ActivityDashboardComponent>;
  let activityService: jasmine.SpyObj<ActivityService>;
  let stravaAccountService: jasmine.SpyObj<StravaAccountService>;
  let trainingProfileService: jasmine.SpyObj<TrainingProfileService>;
  let coachingProfileService: jasmine.SpyObj<CoachingProfileService>;

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

    trainingProfileService = jasmine.createSpyObj<TrainingProfileService>(
      'TrainingProfileService',
      ['get', 'save'],
    );
    trainingProfileService.get.and.returnValue(of(trainingProfile(null, false, 'UNAVAILABLE')));
    trainingProfileService.save.and.returnValue(of(trainingProfile(1990, true, 'AGE_BASED')));

    coachingProfileService = jasmine.createSpyObj<CoachingProfileService>(
      'CoachingProfileService',
      ['get', 'getRunningHistory', 'generateConservativeRunningPlan', 'save'],
    );
    coachingProfileService.get.and.returnValue(of(coachingProfile(false)));
    coachingProfileService.getRunningHistory.and.returnValue(of(runningHistory(false)));
    coachingProfileService.generateConservativeRunningPlan.and.returnValue(
      of(conservativeRunningPlan()),
    );
    coachingProfileService.save.and.returnValue(of(coachingProfile(true)));

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
        { provide: TrainingProfileService, useValue: trainingProfileService },
        { provide: CoachingProfileService, useValue: coachingProfileService },
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

  it('should_open_dashboard_on_today_and_separate_plan_and_activities', () => {
    fixture.detectChanges();

    expect(dashboardView('.today-view').hidden).toBeFalse();
    expect(dashboardView('.plan-view').hidden).toBeTrue();
    expect(dashboardView('.activities-view').hidden).toBeTrue();
  });

  it('should_switch_to_plan_from_dashboard_navigation', () => {
    fixture.detectChanges();

    dashboardNavigationButton('Plano').click();
    fixture.detectChanges();

    expect(dashboardView('.today-view').hidden).toBeTrue();
    expect(dashboardView('.plan-view').hidden).toBeFalse();
    expect(dashboardNavigationButton('Plano').getAttribute('aria-current')).toBe('page');
  });

  it('should_keep_recurring_settings_collapsed_on_today', () => {
    fixture.detectChanges();

    const settings = fixture.nativeElement.querySelectorAll('.settings-disclosure');

    expect(settings.length).toBeGreaterThan(0);
    expect([...settings].every((setting: HTMLDetailsElement) => !setting.open)).toBeTrue();
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
    expect(pageText()).toContain('Mostrando 1 de 3 atividades nesta página carregada.');
  });

  it('should_filter_loaded_activity_page_by_period', () => {
    activityService.list.and.returnValue(of(filterableActivityList()));
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    selectFilterValue(1, 'LAST_7_DAYS');

    expect(pageText()).toContain('Recent Run');
    expect(pageText()).not.toContain('Older Ride');
    expect(pageText()).toContain('Mostrando 1 de 3 atividades nesta página carregada.');
  });

  it('should_filter_loaded_activity_page_by_distance_in_kilometers', () => {
    activityService.list.and.returnValue(of(filterableActivityList()));
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    typeDistanceValue('input[aria-label="Distância mínima em quilômetros"]', '6');
    typeDistanceValue('input[aria-label="Distância máxima em quilômetros"]', '15');

    expect(pageText()).toContain('Tempo Run');
    expect(pageText()).not.toContain('Recent Run');
    expect(pageText()).not.toContain('Older Ride');
    expect(pageText()).toContain('Mostrando 1 de 3 atividades nesta página carregada.');
  });

  it('should_show_filtered_empty_state_for_loaded_page_only', () => {
    activityService.list.and.returnValue(of(filterableActivityList()));
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    typeDistanceValue('input[aria-label="Distância mínima em quilômetros"]', '80');

    expect(pageText()).toContain('Mostrando 0 de 3 atividades nesta página carregada.');
    expect(pageText()).toContain(
      'Nenhuma atividade nesta página carregada corresponde aos filtros.',
    );
    expect(pageText()).not.toContain('Recent Run');
  });

  it('should_show_connected_empty_state_without_summary_metric_cards', () => {
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nenhuma atividade importada ainda.');
    expect(fixture.nativeElement.querySelector('.metrics')).toBeNull();
  });

  it('should_render_training_profile_form_when_birth_year_is_missing', () => {
    fixture.detectChanges();

    expect(pageText()).toContain('Perfil de treino');
    expect(pageText()).toContain('Informe seu ano de nascimento');
    expect(pageText()).toContain('Salvar perfil');
  });

  it('should_render_adaptive_coaching_enabled_when_training_profile_exists', () => {
    trainingProfileService.get.and.returnValue(of(trainingProfile(1990, true, 'AGE_BASED')));

    fixture.detectChanges();

    expect(pageText()).toContain('Coaching adaptativo habilitado');
    expect(pageText()).toContain('Zonas calculadas pelo ano de nascimento.');
    expect(trainingProfileInput().value).toBe('1990');
  });

  it('should_render_strava_zone_enrichment_as_optional', () => {
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    trainingProfileService.get.and.returnValue(of(trainingProfile(1990, true, 'STRAVA')));

    fixture.detectChanges();

    expect(pageText()).toContain('Zonas de frequencia cardiaca importadas do Strava.');
    expect(pageText()).toContain('Reconectar Strava');
  });

  it('should_save_training_profile_birth_year', () => {
    fixture.detectChanges();

    typeTrainingBirthYear('1990');
    trainingProfileButton().click();
    fixture.detectChanges();

    expect(trainingProfileService.save).toHaveBeenCalledWith({ birthYear: 1990 });
    expect(pageText()).toContain('Perfil de treino salvo.');
    expect(pageText()).toContain('Coaching adaptativo habilitado');
  });

  it('should_show_training_profile_validation_error_when_save_fails', () => {
    trainingProfileService.save.and.returnValue(throwError(() => new Error('invalid')));
    fixture.detectChanges();

    typeTrainingBirthYear('');
    trainingProfileButton().click();
    fixture.detectChanges();

    expect(trainingProfileService.save).toHaveBeenCalledWith({ birthYear: null });
    expect(pageText()).toContain('Informe um ano de nascimento valido.');
  });

  it('should_render_coaching_profiles_form_when_inputs_are_missing', () => {
    fixture.detectChanges();

    expect(pageText()).toContain('Coaching');
    expect(pageText()).toContain('Informe sua meta de corrida');
    expect(pageText()).toContain('Salvar coaching');
  });

  it('should_render_current_coaching_profiles_when_saved', () => {
    coachingProfileService.get.and.returnValue(of(coachingProfile(true)));

    fixture.detectChanges();

    expect(pageText()).toContain('Meta atual: 10 km');
    expect(pageText()).toContain('Prontidao: Baixa - com preocupacao de lesao');
    expect(coachingInput('input[aria-label="Distância alvo em quilômetros"]').value).toBe('10');
    expect(coachingInput('input[aria-label="Ritmo alvo por quilometro"]').value).toBe('5:30');
    expect(coachingInput('input[aria-label="Data alvo"]').value).toBe('2026-05-12');
  });

  it('should_render_conservative_classification_and_planned_sessions_for_incomplete_history', () => {
    coachingProfileService.get.and.returnValue(
      of({
        ...coachingProfile(true),
        readiness: 'MODERATE',
        injuryConcern: false,
      }),
    );

    fixture.detectChanges();

    expect(coachingProfileService.generateConservativeRunningPlan).toHaveBeenCalled();
    expect(pageText()).toContain('Plano da semana');
    expect(pageText()).toContain('Corrida leve');
    expect(pageText()).toContain('3 km');
    expect(pageText()).toContain('Esforço percebido 2-4');
  });

  it('should_render_weekly_rhythm_from_monday_to_sunday_with_supported_plan_state', () => {
    activityService.list.and.returnValue(of(activityListWithActivityToday()));
    coachingProfileService.get.and.returnValue(
      of({ ...coachingProfile(true), readiness: 'MODERATE', injuryConcern: false }),
    );

    fixture.detectChanges();

    const weekDays = fixture.nativeElement.querySelectorAll('.week-track > li');
    expect(weekDays.length).toBe(7);
    expect(weekDays[0].textContent).toContain('seg');
    expect(weekDays[6].textContent).toContain('dom');
    expect(pageText()).toContain('Ritmo da semana');
    expect(pageText()).toContain('Corrida de hoje');
    expect(pageText()).toContain('Sem dia definido');
    expect(pageText()).toContain('Ver detalhes');
  });

  it('should_save_coaching_profiles_with_low_readiness_and_injury_concern', () => {
    fixture.detectChanges();

    typeCoachingInput('input[aria-label="Distância alvo em quilômetros"]', '10');
    typeCoachingInput('input[aria-label="Ritmo alvo por quilometro"]', '5:30');
    typeCoachingInput('input[aria-label="Data alvo"]', '2026-05-12');
    selectCoachingReadiness('LOW');
    toggleInjuryConcern(true);
    coachingProfileButton().click();
    fixture.detectChanges();

    expect(coachingProfileService.save).toHaveBeenCalledWith({
      targetDistanceKilometers: 10,
      targetPaceSecondsPerKilometer: 330,
      targetDate: '2026-05-12',
      readiness: 'LOW',
      injuryConcern: true,
    });
    expect(pageText()).toContain('Dados de coaching salvos.');
  });

  it('should_show_coaching_profiles_validation_error_when_save_fails', () => {
    coachingProfileService.save.and.returnValue(throwError(() => new Error('invalid')));
    fixture.detectChanges();

    coachingProfileButton().click();
    fixture.detectChanges();

    expect(pageText()).toContain('Revise a meta e a prontidao informadas.');
  });

  it('should_show_reconnect_guidance_when_strava_is_not_sync_enabled', () => {
    stravaAccountService.status.and.returnValue(of(stravaStatus('PERMISSION_UPGRADE_REQUIRED')));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'Conecte ou atualize a conexão com o Strava para importar atividades.',
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

  function dashboardNavigationButton(label: string): HTMLButtonElement {
    return [...fixture.nativeElement.querySelectorAll('.dashboard-navigation button')].find(
      (button: HTMLButtonElement) => button.textContent.trim() === label,
    ) as HTMLButtonElement;
  }

  function dashboardView(selector: string): HTMLDivElement {
    return fixture.nativeElement.querySelector(selector);
  }

  function trainingProfileInput(): HTMLInputElement {
    return fixture.nativeElement.querySelector('input[aria-label="Ano de nascimento"]');
  }

  function trainingProfileButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.training-profile-panel button');
  }

  function coachingProfileButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.coaching-profile-panel button');
  }

  function coachingInput(selector: string): HTMLInputElement {
    return fixture.nativeElement.querySelector(selector);
  }

  function pageText(): string {
    return fixture.nativeElement.textContent.replace(/\s+/g, ' ').trim();
  }

  function selectFilterValue(index: number, value: string): void {
    const select = fixture.nativeElement.querySelectorAll('.activity-filters select')[
      index
    ] as HTMLSelectElement;
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

  function typeTrainingBirthYear(value: string): void {
    const input = trainingProfileInput();
    input.value = value;

    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function typeCoachingInput(selector: string, value: string): void {
    const input = coachingInput(selector);
    input.value = value;

    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function selectCoachingReadiness(value: string): void {
    const select = fixture.nativeElement.querySelector(
      'select[aria-label="Prontidao reportada"]',
    ) as HTMLSelectElement;
    select.value = value;

    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  function toggleInjuryConcern(checked: boolean): void {
    const input = coachingInput('input[aria-label="Estou com preocupacao de lesao"]');
    input.checked = checked;

    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  function stravaStatus(permissionState: StravaLinkStatus['permissionState']): StravaLinkStatus {
    return {
      linked: permissionState !== 'UNLINKED',
      athleteId: permissionState === 'UNLINKED' ? null : 123,
      permissionState,
      profilePermissionState:
        permissionState === 'UNLINKED' ? 'UNLINKED' : 'OPTIONAL_UPGRADE_AVAILABLE',
      activitySummaryStatus: activitySummaryStatus(permissionState),
      performanceDataStatus: performanceDataStatus(permissionState),
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

    if (permissionState === 'RECONNECT_REQUIRED') {
      return 'FAILED';
    }

    return permissionState;
  }

  function performanceDataStatus(
    permissionState: StravaLinkStatus['permissionState'],
  ): StravaLinkStatus['performanceDataStatus'] {
    if (permissionState === 'READY') {
      return 'PENDING';
    }

    if (permissionState === 'RECONNECT_REQUIRED') {
      return 'FAILED';
    }

    return permissionState;
  }

  function trainingProfile(
    birthYear: number | null,
    adaptiveCoachingEligible: boolean,
    heartRateZoneSource: 'AGE_BASED' | 'STRAVA' | 'UNAVAILABLE',
  ) {
    return {
      birthYear,
      adaptiveCoachingEligible,
      heartRateZoneSource,
      heartRateZones:
        heartRateZoneSource === 'UNAVAILABLE'
          ? []
          : [
              { minimumHeartRate: 100, maximumHeartRate: 120 },
              { minimumHeartRate: 121, maximumHeartRate: 140 },
              { minimumHeartRate: 141, maximumHeartRate: 160 },
              { minimumHeartRate: 161, maximumHeartRate: 180 },
              { minimumHeartRate: 181, maximumHeartRate: 200 },
            ],
    };
  }

  function coachingProfile(configured: boolean) {
    return {
      targetDistanceKilometers: configured ? 10 : null,
      targetPaceSecondsPerKilometer: configured ? 330 : null,
      targetDate: configured ? '2026-05-12' : null,
      readiness: configured ? ('LOW' as const) : null,
      injuryConcern: configured,
      configured,
    };
  }

  function runningHistory(sufficientRunningHistory: boolean) {
    return {
      sufficientRunningHistory,
      activeWeeks: sufficientRunningHistory ? 3 : 1,
      runningActivityCount: sufficientRunningHistory ? 3 : 1,
      totalDistanceKilometers: sufficientRunningHistory ? 18 : 5,
      totalMovingTimeSeconds: sufficientRunningHistory ? 5400 : 1800,
      latestRunAt: '2026-07-08T12:00:00Z',
    };
  }

  function conservativeRunningPlan() {
    return {
      classification: 'CONSERVATIVE' as const,
      reasons: ['INSUFFICIENT_HISTORY' as const],
      longTermGoalDistanceKilometers: 21.1,
      durationWeeks: 4,
      sessionsPerWeek: 2,
      weeklyProgressionPercent: 5,
      plannedSessions: [
        {
          weekNumber: 1,
          sessionNumber: 1,
          type: 'EASY_RUN' as const,
          distanceKilometers: 3,
          target: {
            type: 'PERCEIVED_EFFORT' as const,
            minimumHeartRate: null,
            maximumHeartRate: null,
            minimumPerceivedEffort: 2,
            maximumPerceivedEffort: 4,
          },
        },
      ],
    };
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

  function activityListWithActivityToday(): ActivityList {
    return {
      activities: [
        {
          id: 301,
          sourceActivityId: 3001,
          name: 'Corrida de hoje',
          sportType: 'RUN',
          startDate: new Date().toISOString(),
          distanceMeters: 5200,
          movingTimeSeconds: 1680,
          averageSpeedMetersPerSecond: 3.1,
          averagePaceSecondsPerKilometer: 323,
          streamStatus: 'IMPORTED',
        },
      ],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
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
