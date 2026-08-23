import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { NEVER, of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import {
  ActivityDashboardComponent,
  deriveTodayAction,
  TodayActionState,
} from './activity-dashboard.component';
import { ActivityService } from '../../services/activity.service';
import { CoachingProfileService } from '../../services/coaching-profile.service';
import { ActivityDetail } from '../../services/dtos/activity-detail';
import { ActivityList } from '../../services/dtos/activity-list';
import { CurrentAdaptiveRunningPlan } from '../../services/dtos/adaptive-running-plan';
import { StravaLinkStatus } from '../../services/dtos/strava-link-status';
import { StravaAccountService } from '../../services/strava-account.service';
import { TrainingProfileService } from '../../services/training-profile.service';

describe('ActivityDashboardComponent', () => {
  let fixture: ComponentFixture<ActivityDashboardComponent>;
  let activityService: jasmine.SpyObj<ActivityService>;
  let stravaAccountService: jasmine.SpyObj<StravaAccountService>;
  let trainingProfileService: jasmine.SpyObj<TrainingProfileService>;
  let coachingProfileService: jasmine.SpyObj<CoachingProfileService>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    activityService = jasmine.createSpyObj<ActivityService>('ActivityService', [
      'list',
      'getDetail',
    ]);
    activityService.list.and.returnValue(of(emptyActivityList()));
    activityService.getDetail.and.returnValue(of(activityDetail()));

    stravaAccountService = jasmine.createSpyObj<StravaAccountService>('StravaAccountService', [
      'status',
      'consentStatus',
      'startLinking',
      'requestSync',
      'unlink',
    ]);
    stravaAccountService.status.and.returnValue(of(stravaStatus('UNLINKED')));
    stravaAccountService.consentStatus.and.returnValue(of(stravaDataConsentStatus(false)));
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
    stravaAccountService.unlink.and.returnValue(of(undefined));

    trainingProfileService = jasmine.createSpyObj<TrainingProfileService>(
      'TrainingProfileService',
      ['get', 'save'],
    );
    trainingProfileService.get.and.returnValue(of(trainingProfile(null, false, 'UNAVAILABLE')));
    trainingProfileService.save.and.returnValue(of(trainingProfile(1990, true, 'AGE_BASED')));

    coachingProfileService = jasmine.createSpyObj<CoachingProfileService>(
      'CoachingProfileService',
      [
        'get',
        'getRunningHistory',
        'evaluateRunningGoal',
        'generateConservativeRunningPlan',
        'generateAdaptiveRunningPlan',
        'getCurrentAdaptiveRunningPlan',
        'adaptNextPlannedSession',
        'correctPlannedSessionMatch',
        'unlinkPlannedSessionMatch',
        'submitPostSessionPerceivedEffort',
        'clearInjuryConcern',
        'save',
      ],
    );
    coachingProfileService.get.and.returnValue(of(coachingProfile(false)));
    coachingProfileService.getRunningHistory.and.returnValue(of(runningHistory(false)));
    coachingProfileService.evaluateRunningGoal.and.returnValue(of(runningGoalAssessment()));
    coachingProfileService.generateConservativeRunningPlan.and.returnValue(
      of(conservativeRunningPlan()),
    );
    coachingProfileService.generateAdaptiveRunningPlan.and.returnValue(of(adaptiveRunningPlan()));
    coachingProfileService.getCurrentAdaptiveRunningPlan.and.returnValue(
      throwError(() => new Error('plan not found')),
    );
    coachingProfileService.adaptNextPlannedSession.and.returnValue(of(currentAdaptivePlan()));
    coachingProfileService.correctPlannedSessionMatch.and.returnValue(of(currentAdaptivePlan()));
    coachingProfileService.unlinkPlannedSessionMatch.and.returnValue(of(currentAdaptivePlan()));
    coachingProfileService.submitPostSessionPerceivedEffort.and.returnValue(
      of(currentAdaptivePlan()),
    );
    coachingProfileService.clearInjuryConcern.and.returnValue(of(currentAdaptivePlan()));
    coachingProfileService.save.and.returnValue(of(coachingProfile(true)));

    authService = jasmine.createSpyObj<AuthService>('AuthService', [
      'currentUser',
      'logout',
      'changePassword',
      'deleteAccount',
    ]);
    authService.currentUser.and.returnValue(
      of({ id: 1, name: 'Gabriel', email: 'gabriel@example.com' }),
    );
    authService.changePassword.and.returnValue(of(undefined));
    authService.deleteAccount.and.returnValue(of(undefined));

    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    router.navigateByUrl.and.resolveTo(true);

    await TestBed.configureTestingModule({
      imports: [ActivityDashboardComponent],
      providers: [
        {
          provide: AuthService,
          useValue: authService,
        },
        {
          provide: ActivityService,
          useValue: activityService,
        },
        { provide: StravaAccountService, useValue: stravaAccountService },
        { provide: TrainingProfileService, useValue: trainingProfileService },
        { provide: CoachingProfileService, useValue: coachingProfileService },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ActivityDashboardComponent);
  });

  it('should_render_connect_action_when_strava_is_unlinked', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Não conectado');
    expect(fixture.nativeElement.textContent).toContain('Conectar Strava');
  });

  it('should_retry_dashboard_after_initial_load_failure', () => {
    activityService.list.and.returnValues(
      throwError(() => new Error('offline')),
      of(emptyActivityList()),
    );
    fixture.detectChanges();

    clickAndRefresh(recoveryButton('Tentar novamente'));

    expect(pageText()).toContain('Sua semana em movimento');
    expect(pageText()).not.toContain('Seus dados não foram carregados');
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
    expect(fixture.nativeElement.textContent).toContain('Última sincronização concluída em');
    expect(fixture.nativeElement.textContent).toContain('11/05/2026');
  });

  it('should_open_dashboard_on_today_and_keep_secondary_views_hidden', () => {
    fixture.detectChanges();

    expect(dashboardView('.today-view').hidden).toBeFalse();
    expect(dashboardView('.plan-view').hidden).toBeTrue();
    expect(dashboardView('.activities-view').hidden).toBeTrue();
    expect(dashboardView('.settings-view').hidden).toBeTrue();
  });

  it('should_switch_to_plan_from_dashboard_navigation', () => {
    fixture.detectChanges();

    dashboardNavigationButton('Plano').click();
    fixture.detectChanges();

    expect(dashboardView('.today-view').hidden).toBeTrue();
    expect(dashboardView('.plan-view').hidden).toBeFalse();
    expect(dashboardNavigationButton('Plano').getAttribute('aria-current')).toBe('page');
  });

  it('should_keep_durable_configuration_out_of_today', () => {
    fixture.detectChanges();

    const today = dashboardView('.today-view');

    expect(today.querySelector('.settings-disclosure')).toBeNull();
    expect(today.querySelector('.coaching-profile-panel')).toBeNull();
    expect(today.querySelector('.strava-panel')).toBeNull();
  });

  it('should_group_durable_configuration_under_adjustments', () => {
    fixture.detectChanges();

    dashboardNavigationButton('Ajustes').click();
    fixture.detectChanges();

    const settings = dashboardView('.settings-view');
    expect(settings.hidden).toBeFalse();
    expect(settings.querySelector('.strava-panel')).not.toBeNull();
    expect(settings.querySelector('.coaching-profile-panel')).not.toBeNull();
    expect(dashboardNavigationButton('Ajustes').getAttribute('aria-current')).toBe('page');
  });

  it('should_prevent_password_change_when_required_fields_are_empty', () => {
    fixture.detectChanges();
    dashboardNavigationButton('Ajustes').click();
    fixture.detectChanges();

    passwordChangeButton().click();
    fixture.detectChanges();

    expect(passwordChangeButton().disabled).toBeTrue();
    expect(authService.changePassword).not.toHaveBeenCalled();
  });

  it('should_change_password_and_navigate_to_login_after_success', () => {
    fixture.detectChanges();
    dashboardNavigationButton('Ajustes').click();
    fixture.detectChanges();

    typePasswordChangeInput('input[autocomplete="current-password"]', 'Str0ng!Password');
    typePasswordChangeInput('input[autocomplete="new-password"]', 'An0ther!Password');
    passwordChangeButton().click();
    fixture.detectChanges();

    expect(authService.changePassword).toHaveBeenCalledOnceWith({
      currentPassword: 'Str0ng!Password',
      newPassword: 'An0ther!Password',
    });
    expect(router.navigateByUrl).toHaveBeenCalledOnceWith('/login');
  });

  it('should_show_password_policy_feedback_from_backend_errors', () => {
    authService.changePassword.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: {
              code: 'PASSWORD_POLICY_VIOLATION',
              message: 'Password is invalid',
              violations: ['TOO_SHORT', 'MISSING_SPECIAL_CHARACTER'],
            },
          }),
      ),
    );
    fixture.detectChanges();
    dashboardNavigationButton('Ajustes').click();
    fixture.detectChanges();

    typePasswordChangeInput('input[autocomplete="current-password"]', 'Str0ng!Password');
    typePasswordChangeInput('input[autocomplete="new-password"]', 'AnotherPassword1');
    passwordChangeButton().click();
    fixture.detectChanges();

    expect(pageText()).toContain('A nova senha ainda não atende à política de segurança.');
    expect(rejectedPasswordPolicyItems().map((item) => item.textContent?.trim())).toEqual([
      'Pelo menos 12 caracteres',
      'Um caractere especial',
    ]);
    expect(router.navigateByUrl).not.toHaveBeenCalledWith('/login');
  });

  it('should_prevent_account_deletion_when_current_password_is_empty', () => {
    fixture.detectChanges();
    dashboardNavigationButton('Ajustes').click();
    fixture.detectChanges();

    accountDeletionButton().click();
    fixture.detectChanges();

    expect(accountDeletionButton().disabled).toBeTrue();
    expect(authService.deleteAccount).not.toHaveBeenCalled();
  });

  it('should_require_explicit_confirmation_before_account_deletion', () => {
    fixture.detectChanges();
    dashboardNavigationButton('Ajustes').click();
    fixture.detectChanges();

    typeAccountDeletionPassword('Str0ng!Password');
    accountDeletionButton().click();
    fixture.detectChanges();

    expect(accountDeletionButton().disabled).toBeTrue();
    expect(authService.deleteAccount).not.toHaveBeenCalled();
  });

  it('should_delete_account_and_navigate_to_login_after_confirmation', () => {
    fixture.detectChanges();
    dashboardNavigationButton('Ajustes').click();
    fixture.detectChanges();

    typeAccountDeletionPassword('Str0ng!Password');
    confirmAccountDeletion();
    accountDeletionButton().click();
    fixture.detectChanges();

    expect(authService.deleteAccount).toHaveBeenCalledOnceWith({
      currentPassword: 'Str0ng!Password',
    });
    expect(router.navigateByUrl).toHaveBeenCalledOnceWith('/login');
  });

  it('should_show_invalid_credentials_error_when_account_deletion_password_is_wrong', () => {
    authService.deleteAccount.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 401,
            error: {
              code: 'INVALID_CREDENTIALS',
              message: 'Invalid credentials',
            },
          }),
      ),
    );
    fixture.detectChanges();
    dashboardNavigationButton('Ajustes').click();
    fixture.detectChanges();

    typeAccountDeletionPassword('wrong-password');
    confirmAccountDeletion();
    accountDeletionButton().click();
    fixture.detectChanges();

    expect(pageText()).toContain('A senha atual não confere.');
    expect(router.navigateByUrl).not.toHaveBeenCalledWith('/login');
  });

  it('should_lead_today_with_the_next_decision_before_weekly_context', () => {
    fixture.detectChanges();

    const priority = fixture.nativeElement.querySelector('.today-priority');
    const weeklyRhythm = fixture.nativeElement.querySelector('app-weekly-rhythm');

    expect(
      priority.compareDocumentPosition(weeklyRhythm) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
  });

  it('should_show_the_next_session_prescription_in_the_today_priority', () => {
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    trainingProfileService.get.and.returnValue(of(trainingProfile(1990, true, 'AGE_BASED')));
    coachingProfileService.get.and.returnValue(
      of({ ...coachingProfile(true), injuryConcern: false }),
    );
    coachingProfileService.getRunningHistory.and.returnValue(of(runningHistory(true)));
    coachingProfileService.getCurrentAdaptiveRunningPlan.and.returnValue(of(currentAdaptivePlan()));

    fixture.detectChanges();

    const priorityText = fixture.nativeElement.querySelector('.today-priority').textContent;
    expect(priorityText).toContain('Corrida leve');
    expect(priorityText).toContain('16/07/2026');
    expect(priorityText).toContain('3 km');
    expect(priorityText).toContain('Esforço percebido 2-4');
    expect(priorityText).toContain('Consulte sua próxima sessão');
  });

  it('should_render_permission_upgrade_action_when_scope_is_incomplete', () => {
    stravaAccountService.status.and.returnValue(of(stravaStatus('PERMISSION_UPGRADE_REQUIRED')));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Permissões incompletas');
    expect(fixture.nativeElement.textContent).toContain('Atualizar permissões');
  });

  it('should_render_imported_activity_summary_fields', () => {
    activityService.list.and.returnValue(of(activityListWithSummaries()));
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    const textContent = fixture.nativeElement.textContent;
    expect(textContent).toContain('Morning Run');
    expect(textContent).toContain('Corrida');
    expect(textContent).toContain('10/05/2026 06:00');
    expect(textContent).toContain('5.0 km');
    expect(textContent).toContain('25 min');
    expect(textContent).toContain('5:00 /km');
    expect(textContent).toContain('Dados de desempenho');
    expect(textContent).toContain('Sendo preparados');
  });

  it('should_load_and_close_inline_activity_details_without_changing_the_list', () => {
    activityService.list.and.returnValue(of(activityListWithSummaries()));
    fixture.detectChanges();
    dashboardNavigationButton('Atividades').click();
    fixture.detectChanges();

    activityDetailButton('Ver detalhes').click();
    fixture.detectChanges();

    expect(activityService.getDetail).toHaveBeenCalledOnceWith(99);
    expect(pageText()).toContain('FC média150 bpm');
    expect(pageText()).not.toContain('Energia');
    expect(pageText()).toContain('Morning Run');

    activityDetailButton('Fechar detalhes').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.activity-detail')).toBeNull();
  });

  it('should_retry_only_the_activity_detail_that_failed', () => {
    activityService.list.and.returnValue(of(activityListWithSummaries()));
    activityService.getDetail.and.returnValues(
      throwError(() => new Error('offline')),
      of(activityDetail()),
    );
    fixture.detectChanges();
    dashboardNavigationButton('Atividades').click();
    fixture.detectChanges();

    activityDetailButton('Ver detalhes').click();
    fixture.detectChanges();
    activityDetailButton('Tentar novamente').click();
    fixture.detectChanges();

    expect(activityService.getDetail).toHaveBeenCalledTimes(2);
    expect(pageText()).toContain('FC média150 bpm');
  });

  it('should_confirm_strava_unlink_once_and_update_the_local_status', () => {
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    stravaButtonByLabel('Desvincular').click();
    fixture.detectChanges();
    expect(stravaAccountService.unlink).not.toHaveBeenCalled();

    stravaButtonByLabel('Desvincular', 1).click();
    fixture.detectChanges();

    expect(stravaAccountService.unlink).toHaveBeenCalledTimes(1);
    expect(pageText()).toContain('Não conectado');
    expect(pageText()).toContain('dados já importados foram preservados');
  });

  it('should_clear_injury_concern_with_readiness_and_authoritative_plan', () => {
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    trainingProfileService.get.and.returnValue(of(trainingProfile(1990, true, 'AGE_BASED')));
    coachingProfileService.get.and.returnValue(of(coachingProfile(true)));
    fixture.detectChanges();

    stravaButtonByLabel('Retome quando estiver pronto').click();
    fixture.detectChanges();
    const readiness = fixture.nativeElement.querySelector(
      '.injury-clear-panel select',
    ) as HTMLSelectElement;
    readiness.value = 'MODERATE';
    readiness.dispatchEvent(new Event('change'));
    stravaButtonByLabel('Encerrar preocupação').click();
    fixture.detectChanges();

    expect(coachingProfileService.clearInjuryConcern).toHaveBeenCalledOnceWith({
      readiness: 'MODERATE',
    });
    expect(pageText()).toContain('Preocupação encerrada');
    expect(pageText()).not.toContain('com preocupação de lesão');
  });

  it('should_derive_exactly_one_today_action_using_the_defined_priority', () => {
    const ready: TodayActionState = {
      stravaLinked: true,
      profileComplete: true,
      injuryConcern: false,
      activitySyncPending: false,
      matchPending: false,
      effortPending: false,
    };
    const cases: Array<[Partial<TodayActionState>, ReturnType<typeof deriveTodayAction>]> = [
      [{ stravaLinked: false, profileComplete: false, injuryConcern: true }, 'CONNECT_STRAVA'],
      [{ profileComplete: false, injuryConcern: true }, 'COMPLETE_PROFILE'],
      [{ injuryConcern: true, activitySyncPending: true }, 'CLEAR_INJURY_CONCERN'],
      [{ activitySyncPending: true, matchPending: true }, 'SYNC_ACTIVITY'],
      [{ matchPending: true, effortPending: true }, 'REVIEW_MATCH'],
      [{ effortPending: true }, 'REPORT_EFFORT'],
      [{}, 'VIEW_NEXT_SESSION'],
    ];

    cases.forEach(([state, action]) =>
      expect(deriveTodayAction({ ...ready, ...state })).toBe(action),
    );
  });

  it('should_filter_loaded_activity_page_by_type', () => {
    activityService.list.and.returnValue(of(filterableActivityList()));
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    selectFilterValue(0, 'RIDE');

    expect(activityListText()).toContain('Older Ride');
    expect(activityListText()).not.toContain('Recent Run');
    expect(activityListText()).not.toContain('Tempo Run');
    expect(pageText()).toContain('1 correspondem aos filtros.');
  });

  it('should_filter_loaded_activity_page_by_period', () => {
    activityService.list.and.returnValue(of(filterableActivityList()));
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    selectFilterValue(1, 'LAST_7_DAYS');

    expect(activityListText()).toContain('Recent Run');
    expect(activityListText()).not.toContain('Older Ride');
    expect(pageText()).toContain('1 correspondem aos filtros.');
  });

  it('should_filter_loaded_activity_page_by_distance_in_kilometers', () => {
    activityService.list.and.returnValue(of(filterableActivityList()));
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    typeDistanceValue('input[aria-label="Distância mínima em quilômetros"]', '6');
    typeDistanceValue('input[aria-label="Distância máxima em quilômetros"]', '15');

    expect(activityListText()).toContain('Tempo Run');
    expect(activityListText()).not.toContain('Recent Run');
    expect(activityListText()).not.toContain('Older Ride');
    expect(pageText()).toContain('1 correspondem aos filtros.');
  });

  it('should_show_filtered_empty_state_for_loaded_page_only', () => {
    activityService.list.and.returnValue(of(filterableActivityList()));
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    typeDistanceValue('input[aria-label="Distância mínima em quilômetros"]', '80');

    expect(pageText()).toContain('0 correspondem aos filtros.');
    expect(pageText()).toContain('Nenhuma atividade desta página corresponde aos filtros.');
    expect(activityListText()).not.toContain('Recent Run');
  });

  it('should_show_connected_empty_state_without_summary_metric_cards', () => {
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Seu histórico ainda está vazio');
    expect(fixture.nativeElement.textContent).toContain('Sincronizar atividades');
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

    expect(pageText()).toContain('Orientação de treino disponível');
    expect(pageText()).toContain('Zonas calculadas pelo ano de nascimento.');
    expect(trainingProfileInput().value).toBe('1990');
  });

  it('should_render_strava_zone_enrichment_as_optional', () => {
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    trainingProfileService.get.and.returnValue(of(trainingProfile(1990, true, 'STRAVA')));

    fixture.detectChanges();

    expect(pageText()).toContain('Zonas de frequência cardíaca importadas do Strava.');
    expect(pageText()).toContain('Reconectar Strava');
  });

  it('should_save_training_profile_birth_year', () => {
    fixture.detectChanges();

    typeTrainingBirthYear('1990');
    trainingProfileButton().click();
    fixture.detectChanges();

    expect(trainingProfileService.save).toHaveBeenCalledWith({ birthYear: 1990 });
    expect(pageText()).toContain('Perfil salvo.');
    expect(pageText()).toContain('Orientação de treino disponível');
  });

  it('should_show_training_profile_validation_error_when_save_fails', () => {
    trainingProfileService.save.and.returnValue(throwError(() => new Error('invalid')));
    fixture.detectChanges();

    typeTrainingBirthYear('');
    trainingProfileButton().click();
    fixture.detectChanges();

    expect(trainingProfileService.save).toHaveBeenCalledWith({ birthYear: null });
    expect(pageText()).toContain('O ano informado foi preservado');
  });

  it('should_render_coaching_profiles_form_when_inputs_are_missing', () => {
    fixture.detectChanges();

    expect(pageText()).toContain('Meta e prontidão');
    expect(pageText()).toContain('Informe sua meta de corrida');
    expect(pageText()).toContain('Salvar contexto de treino');
  });

  it('should_split_coaching_context_into_progressive_sections_with_safety_visible', () => {
    fixture.detectChanges();

    const sections = fixture.nativeElement.querySelectorAll('.coaching-section');

    expect(sections.length).toBe(4);
    expect(sections[0].textContent).toContain('Sua meta');
    expect(sections[1].textContent).toContain('Sua disponibilidade');
    expect(sections[2].textContent).toContain('Como você está hoje');
    expect(sections[3].textContent).toContain('Dor ou preocupação de lesão');
    expect(
      sections[3].querySelector('input[aria-label="Estou com dor ou preocupação de lesão"]'),
    ).not.toBeNull();
  });

  it('should_render_current_coaching_profiles_when_saved', () => {
    coachingProfileService.get.and.returnValue(of(coachingProfile(true)));

    fixture.detectChanges();

    expect(pageText()).toContain('Meta atual: 10 km');
    expect(pageText()).toContain('Prontidão: Baixa · com preocupação de lesão');
    expect(coachingInput('input[aria-label="Distância alvo em quilômetros"]').value).toBe('10');
    expect(coachingInput('input[aria-label="Ritmo alvo por quilometro"]').value).toBe('5:30');
    expect(coachingInput('input[aria-label="Data alvo"]').value).toBe('2026-05-12');
  });

  it('should_distinguish_the_preserved_long_term_goal_from_the_safe_milestone', () => {
    coachingProfileService.get.and.returnValue(of(coachingProfile(true)));

    fixture.detectChanges();

    expect(pageText()).toContain('Destino');
    expect(pageText()).toContain('42.2 km');
    expect(pageText()).toContain('Foco agora');
    expect(pageText()).toContain('7.3 km');
    expect(pageText()).toContain('Sua meta continua a mesma');
  });

  it('should_display_recovery_sessions_without_medical_diagnosis_language_for_injury_concern', () => {
    coachingProfileService.get.and.returnValue(of(coachingProfile(true)));
    coachingProfileService.generateConservativeRunningPlan.and.returnValue(
      of(recoveryRunningPlan()),
    );
    fixture.detectChanges();

    expect(coachingProfileService.generateConservativeRunningPlan).toHaveBeenCalled();
    expect(pageText()).toContain('Sessão de recuperação');
    expect(pageText()).toContain('Esforço percebido 1-3');
    expect(pageText()).not.toContain('avaliação médica');
    expect(pageText()).not.toContain('diagnóstico');
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

  it('should_render_accepted_adaptive_plan_and_explanation_for_sufficient_history', () => {
    coachingProfileService.get.and.returnValue(
      of({ ...coachingProfile(true), readiness: 'MODERATE', injuryConcern: false }),
    );
    coachingProfileService.getRunningHistory.and.returnValue(of(runningHistory(true)));
    fixture.detectChanges();

    dashboardNavigationButton('Plano').click();
    fixture.detectChanges();

    expect(coachingProfileService.generateAdaptiveRunningPlan).toHaveBeenCalled();
    expect(pageText()).toContain('Seu plano está pronto');
    expect(pageText()).toContain('O plano foi validado pelo Sudolife');
  });

  it('should_render_next_session_before_history_and_translate_all_session_states', () => {
    coachingProfileService.get.and.returnValue(
      of({ ...coachingProfile(true), readiness: 'MODERATE', injuryConcern: false }),
    );
    coachingProfileService.getRunningHistory.and.returnValue(of(runningHistory(true)));
    coachingProfileService.getCurrentAdaptiveRunningPlan.and.returnValue(of(currentAdaptivePlan()));
    fixture.detectChanges();

    dashboardNavigationButton('Plano').click();
    fixture.detectChanges();

    const planText = dashboardView('.plan-view').textContent;
    expect(planText.indexOf('Próxima sessão')).toBeLessThan(planText.indexOf('Histórico do plano'));
    expect(planText).toContain('Planejada');
    expect(planText).toContain('Concluída');
    expect(planText).toContain('Perdida');
    expect(planText).toContain('Substituída');
  });

  it('should_order_adaptive_history_by_date_and_replacement_chain_deterministically', () => {
    coachingProfileService.get.and.returnValue(
      of({ ...coachingProfile(true), readiness: 'MODERATE', injuryConcern: false }),
    );
    coachingProfileService.getRunningHistory.and.returnValue(of(runningHistory(true)));
    coachingProfileService.getCurrentAdaptiveRunningPlan.and.returnValue(of(currentAdaptivePlan()));
    fixture.detectChanges();

    dashboardNavigationButton('Plano').click();
    fixture.detectChanges();

    const sessions = Array.from(
      fixture.nativeElement.querySelectorAll('.timeline-session'),
    ) as HTMLElement[];
    expect(sessions.map((session) => session.getAttribute('aria-label'))).toEqual([
      'Concluída: Corrida leve, 16/07/2026',
      'Perdida: Corrida longa, 18/07/2026',
      'Substituída: Corrida longa, 20/07/2026',
      'Planejada: Sessão de recuperação, 20/07/2026',
    ]);
  });

  it('should_explain_replacement_completion_and_missed_session_context', () => {
    coachingProfileService.get.and.returnValue(
      of({ ...coachingProfile(true), readiness: 'MODERATE', injuryConcern: false }),
    );
    coachingProfileService.getRunningHistory.and.returnValue(of(runningHistory(true)));
    coachingProfileService.getCurrentAdaptiveRunningPlan.and.returnValue(of(currentAdaptivePlan()));
    fixture.detectChanges();

    dashboardNavigationButton('Plano').click();
    fixture.detectChanges();

    expect(pageText()).toContain('Substituída por Sessão de recuperação em 20/07/2026');
    expect(pageText()).toContain('Corrida associada · atividade 501');
    expect(pageText()).toContain('Esforço percebido registrado: 7 de 10');
    expect(pageText()).toContain('A próxima sessão pode ter sido ajustada');
  });

  it('should_correct_the_session_match_with_a_descriptive_activity_option', () => {
    loadCurrentAdaptivePlanWithActivities();

    const select = fixture.nativeElement.querySelector('#activity-match-11') as HTMLSelectElement;
    select.value = '99';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    sessionButton(11, 'Salvar corrida').click();
    fixture.detectChanges();

    expect(select.textContent).toContain('Morning Run');
    expect(select.textContent).toContain('5,0 km');
    expect(coachingProfileService.correctPlannedSessionMatch).toHaveBeenCalledWith({
      plannedSessionId: 11,
      activityId: 99,
    });
    expect(pageText()).toContain('Corrida da sessão atualizada.');
  });

  it('should_allow_matching_a_completed_session_without_showing_effort_first', () => {
    loadCurrentAdaptivePlanWithActivities(currentAdaptivePlanWithUnmatchedCompletedSession());

    expect(pageText()).toContain('Nenhuma corrida associada ainda.');
    expect(fixture.nativeElement.querySelector('#activity-match-11')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#perceived-effort-11')).toBeNull();

    const select = fixture.nativeElement.querySelector('#activity-match-11') as HTMLSelectElement;
    select.value = '99';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    buttonByText('Salvar corrida').click();
    fixture.detectChanges();

    expect(coachingProfileService.correctPlannedSessionMatch).toHaveBeenCalledWith({
      plannedSessionId: 11,
      activityId: 99,
    });
  });

  it('should_reject_empty_session_match_without_sending_activity_zero', () => {
    loadCurrentAdaptivePlanWithActivities(currentAdaptivePlanWithUnmatchedCompletedSession());

    buttonByText('Salvar corrida').click();
    fixture.detectChanges();

    expect(coachingProfileService.correctPlannedSessionMatch).not.toHaveBeenCalled();
    expect(pageText()).toContain('Escolha a corrida que corresponde a esta sessão.');
  });

  it('should_require_inline_confirmation_before_unlinking_a_session', () => {
    loadCurrentAdaptivePlanWithActivities();

    sessionButton(11, 'Retirar corrida da sessão').click();
    fixture.detectChanges();

    expect(coachingProfileService.unlinkPlannedSessionMatch).not.toHaveBeenCalled();
    expect(pageText()).toContain('não a exclui da área Atividades');

    sessionButton(11, 'Cancelar').click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.unlink-confirmation')).toBeNull();
  });

  it('should_unlink_only_after_confirmation_and_keep_the_activity_loaded', () => {
    loadCurrentAdaptivePlanWithActivities();

    sessionButton(11, 'Retirar corrida da sessão').click();
    fixture.detectChanges();
    sessionButton(11, 'Retirar corrida').click();
    fixture.detectChanges();

    expect(coachingProfileService.unlinkPlannedSessionMatch).toHaveBeenCalledOnceWith(11);
    expect(pageText()).toContain('Ela continua disponível em Atividades.');
    expect(pageText()).toContain('Morning Run');
  });

  it('should_reject_invalid_perceived_effort_without_sending_it', () => {
    loadCurrentAdaptivePlanWithActivities();

    typeSessionInput('#perceived-effort-11', '5.5');
    sessionButton(11, 'Salvar esforço').click();
    fixture.detectChanges();

    expect(coachingProfileService.submitPostSessionPerceivedEffort).not.toHaveBeenCalled();
    expect(pageText()).toContain('Informe um número inteiro de 1 a 10.');
  });

  it('should_submit_effort_and_announce_the_authoritative_next_session_change', () => {
    const adaptedPlan = currentAdaptivePlan();
    adaptedPlan.plannedSessions[0].plannedSession.distanceKilometers = 1.5;
    adaptedPlan.plannedSessions[0].adaptationTrigger = 'UNEXPECTEDLY_HIGH_EFFORT';
    coachingProfileService.submitPostSessionPerceivedEffort.and.returnValue(of(adaptedPlan));
    loadCurrentAdaptivePlanWithActivities();

    typeSessionInput('#perceived-effort-11', '9');
    sessionButton(11, 'Salvar esforço').click();
    fixture.detectChanges();

    expect(coachingProfileService.submitPostSessionPerceivedEffort).toHaveBeenCalledWith(11, {
      perceivedEffort: 9,
    });
    expect(pageText()).toContain('Próxima sessão adaptada');
    expect(pageText()).toContain('Antes');
    expect(pageText()).toContain('Agora');
    expect(pageText()).toContain('esforço acima do esperado');
  });

  it('should_expose_only_low_readiness_as_a_manual_adaptation', () => {
    loadCurrentAdaptivePlanWithActivities();

    const readinessButton = fixture.nativeElement.querySelector(
      '.readiness-action',
    ) as HTMLButtonElement;
    readinessButton.click();
    fixture.detectChanges();

    expect(coachingProfileService.adaptNextPlannedSession).toHaveBeenCalledOnceWith({
      trigger: 'LOW_READINESS',
    });
    expect(pageText()).not.toContain('Esforço acima do esperado');
  });

  it('should_retry_plan_generation_without_losing_profile', () => {
    coachingProfileService.get.and.returnValue(
      of({ ...coachingProfile(true), readiness: 'LOW', injuryConcern: false }),
    );
    coachingProfileService.generateConservativeRunningPlan.and.returnValues(
      throwError(() => new Error('offline')),
      of(conservativeRunningPlan()),
    );
    fixture.detectChanges();

    dashboardNavigationButton('Plano').click();
    fixture.detectChanges();
    clickAndRefresh(recoveryButton('Tentar novamente'));

    expect(pageText()).not.toContain('Seu perfil foi preservado, mas não foi possível atualizar');
    expect(pageText()).toContain('Meta atual: 10 km');
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
    expect(pageText()).toContain('Sua semana em movimento');
    expect(pageText()).toContain('Corrida de hoje');
    expect(pageText()).toContain('16/07/2026');
    expect(pageText()).toContain('Ver detalhes');
  });

  it('should_save_coaching_profiles_with_low_readiness_and_injury_concern', () => {
    fixture.detectChanges();

    typeCoachingInput('input[aria-label="Distância alvo em quilômetros"]', '10');
    typeCoachingInput('input[aria-label="Ritmo alvo por quilometro"]', '5:30');
    typeCoachingInput('input[aria-label="Data alvo"]', '2026-05-12');
    selectCoachingReadiness('LOW');
    toggleInjuryConcern(true);
    togglePreferredRunningDay('Ter', true);
    togglePreferredRunningDay('Sáb', true);
    coachingProfileButton().click();
    fixture.detectChanges();

    expect(coachingProfileService.save).toHaveBeenCalledWith({
      targetDistanceKilometers: 10,
      targetPaceSecondsPerKilometer: 330,
      targetDate: '2026-05-12',
      readiness: 'LOW',
      injuryConcern: true,
      preferredRunningDays: ['TUESDAY', 'SATURDAY'],
    });
    expect(pageText()).toContain('Meta e prontidão salvas.');
  });

  it('should_identify_the_invalid_goal_distance_before_saving', () => {
    fixture.detectChanges();

    typeCoachingInput('input[aria-label="Distância alvo em quilômetros"]', '0');
    coachingProfileButton().click();
    fixture.detectChanges();

    expect(coachingProfileService.save).not.toHaveBeenCalled();
    expect(pageText()).toContain('Distância da meta deve ser maior que zero. Exemplo: 5 km.');
  });

  it('should_identify_the_invalid_goal_pace_before_saving', () => {
    fixture.detectChanges();

    typeCoachingInput('input[aria-label="Ritmo alvo por quilometro"]', 'ritmo');
    coachingProfileButton().click();
    fixture.detectChanges();

    expect(coachingProfileService.save).not.toHaveBeenCalled();
    expect(pageText()).toContain(
      'Ritmo da meta deve usar minutos e segundos por quilômetro. Exemplo: 5:30.',
    );
  });

  it('should_show_coaching_profiles_validation_error_when_save_fails', () => {
    coachingProfileService.save.and.returnValue(throwError(() => new Error('invalid')));
    fixture.detectChanges();

    coachingProfileButton().click();
    fixture.detectChanges();

    expect(pageText()).toContain('Sua meta, prontidão e preocupação de lesão foram preservadas');
    expect(coachingInput('input[aria-label="Distância alvo em quilômetros"]').value).toBe('');
  });

  it('should_show_reconnect_guidance_when_strava_is_not_sync_enabled', () => {
    stravaAccountService.status.and.returnValue(of(stravaStatus('PERMISSION_UPGRADE_REQUIRED')));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'Conecte ou atualize a conexão com o Strava para importar atividades.',
    );
    expect(fixture.nativeElement.textContent).toContain('Atualizar permissões');
    expect(fixture.nativeElement.textContent).not.toContain('Seu histórico ainda está vazio');
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

  it('should_preserve_current_activities_when_next_page_fails', () => {
    activityService.list.and.returnValues(
      of(activityListWithSummaries()),
      throwError(() => new Error('offline')),
    );
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    fixture.nativeElement.querySelector('.next-page').click();
    fixture.detectChanges();

    expect(pageText()).toContain('As atividades atuais foram preservadas');
    expect(pageText()).toContain('Morning Run');
  });

  it('should_show_linking_error_when_oauth_launch_fails', () => {
    stravaAccountService.startLinking.and.returnValue(throwError(() => new Error('failed')));
    stravaAccountService.consentStatus.and.returnValue(of(stravaDataConsentStatus(true)));
    fixture.detectChanges();

    stravaButton().click();
    fixture.detectChanges();

    expect(stravaAccountService.startLinking).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'Não foi possível abrir a conexão com o Strava.',
    );
  });

  it('should_block_strava_oauth_until_data_consent_is_checked', () => {
    fixture.detectChanges();

    stravaButton().click();
    fixture.detectChanges();

    expect(stravaAccountService.startLinking).not.toHaveBeenCalled();
    expect(pageText()).toContain('Aceite o uso dos dados do Strava para continuar a conexão.');
  });

  it('should_start_strava_oauth_with_explicit_data_consent_when_checked', () => {
    stravaAccountService.startLinking.and.returnValue(NEVER);
    fixture.detectChanges();

    const consentCheckbox = fixture.nativeElement.querySelector(
      '.strava-consent-check input',
    ) as HTMLInputElement;
    consentCheckbox.checked = true;
    consentCheckbox.dispatchEvent(new Event('change'));
    stravaButton().click();
    fixture.detectChanges();

    expect(stravaAccountService.startLinking).toHaveBeenCalledOnceWith(true);
  });

  it('should_start_strava_oauth_without_reprompting_when_current_consent_exists', () => {
    stravaAccountService.consentStatus.and.returnValue(of(stravaDataConsentStatus(true)));
    stravaAccountService.startLinking.and.returnValue(NEVER);
    fixture.detectChanges();

    stravaButton().click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.strava-consent-panel')).toBeNull();
    expect(stravaAccountService.startLinking).toHaveBeenCalledOnceWith(false);
  });

  it('should_show_manual_sync_result', () => {
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    fixture.detectChanges();

    syncButton().click();
    fixture.detectChanges();

    expect(stravaAccountService.requestSync).toHaveBeenCalled();
    expect(pageText()).toContain('Sincronização iniciada');
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

    expect(pageText()).toContain('Sincronização não iniciada');
    expect(pageText()).toContain('Importadas0');
    expect(pageText()).toContain('Total4');
    expect(pageText()).toContain('Atualize as permissões do Strava para importar atividades.');
    expect(pageText()).toContain('Atualizar permissões');
  });

  it('should_keep_dashboard_usable_when_manual_sync_request_fails', () => {
    activityService.list.and.returnValue(of(activityListWithSummaries()));
    stravaAccountService.status.and.returnValue(of(stravaStatus('READY')));
    stravaAccountService.requestSync.and.returnValue(throwError(() => new Error('failed')));
    fixture.detectChanges();

    syncButton().click();
    fixture.detectChanges();

    expect(pageText()).toContain('Não foi possível iniciar a sincronização.');
    expect(pageText()).toContain('Morning Run');
    expect(fixture.nativeElement.querySelector('.next-page').disabled).toBeFalse();
  });

  function stravaButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.strava-action');
  }

  function syncButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.sync-action');
  }

  function activityDetailButton(label: string): HTMLButtonElement {
    return [...fixture.nativeElement.querySelectorAll('.activity-list button')].find(
      (button: HTMLButtonElement) => button.textContent.trim() === label,
    ) as HTMLButtonElement;
  }

  function stravaButtonByLabel(label: string, index = 0): HTMLButtonElement {
    return [...fixture.nativeElement.querySelectorAll('button')].filter(
      (button: HTMLButtonElement) => button.textContent.trim() === label,
    )[index] as HTMLButtonElement;
  }

  function dashboardNavigationButton(label: string): HTMLButtonElement {
    return [...fixture.nativeElement.querySelectorAll('.dashboard-navigation button')].find(
      (button: HTMLButtonElement) => button.textContent.trim() === label,
    ) as HTMLButtonElement;
  }

  function dashboardView(selector: string): HTMLDivElement {
    return fixture.nativeElement.querySelector(selector);
  }

  function recoveryButton(label: string): HTMLButtonElement {
    return [...fixture.nativeElement.querySelectorAll('.recovery-panel button')].find(
      (button: HTMLButtonElement) => button.textContent.trim() === label,
    ) as HTMLButtonElement;
  }

  function clickAndRefresh(button: HTMLButtonElement): void {
    button.click();
    fixture.detectChanges();
  }

  function trainingProfileInput(): HTMLInputElement {
    return fixture.nativeElement.querySelector('input[aria-label="Ano de nascimento"]');
  }

  function trainingProfileButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector(
      '.training-profile-panel:not(.password-change-panel):not(.account-deletion-panel) button',
    );
  }

  function passwordChangeButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.password-change-panel button[type="submit"]');
  }

  function accountDeletionButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.account-deletion-panel button[type="submit"]');
  }

  function typePasswordChangeInput(selector: string, value: string): void {
    const input = fixture.nativeElement.querySelector(
      `.password-change-panel ${selector}`,
    ) as HTMLInputElement;
    input.value = value;

    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function rejectedPasswordPolicyItems(): HTMLElement[] {
    return [...fixture.nativeElement.querySelectorAll('.password-policy-list li.rejected')];
  }

  function typeAccountDeletionPassword(value: string): void {
    const input = fixture.nativeElement.querySelector(
      '.account-deletion-panel input[type="password"]',
    ) as HTMLInputElement;
    input.value = value;

    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function confirmAccountDeletion(): void {
    const input = fixture.nativeElement.querySelector(
      '.account-deletion-panel input[type="checkbox"]',
    ) as HTMLInputElement;
    input.checked = true;

    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  function coachingProfileButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.coaching-profile-panel button');
  }

  function togglePreferredRunningDay(label: string, checked: boolean): void {
    const input = fixture.nativeElement.querySelector(
      `input[aria-label="${label}"]`,
    ) as HTMLInputElement;
    input.checked = checked;
    input.dispatchEvent(new Event('change'));
  }

  function coachingInput(selector: string): HTMLInputElement {
    return fixture.nativeElement.querySelector(selector);
  }

  function pageText(): string {
    return fixture.nativeElement.textContent.replace(/\s+/g, ' ').trim();
  }

  function activityListText(): string {
    return fixture.nativeElement.querySelector('.activity-list')?.textContent ?? '';
  }

  function loadCurrentAdaptivePlanWithActivities(plan = currentAdaptivePlan()): void {
    activityService.list.and.returnValue(of(activityListWithSummaries()));
    coachingProfileService.get.and.returnValue(
      of({ ...coachingProfile(true), readiness: 'MODERATE', injuryConcern: false }),
    );
    coachingProfileService.getRunningHistory.and.returnValue(of(runningHistory(true)));
    coachingProfileService.getCurrentAdaptiveRunningPlan.and.returnValue(of(plan));
    fixture.detectChanges();
    dashboardNavigationButton('Plano').click();
    fixture.detectChanges();
  }

  function buttonByText(label: string): HTMLButtonElement {
    return [...fixture.nativeElement.querySelectorAll('button')].find(
      (button: HTMLButtonElement) => button.textContent.trim() === label,
    ) as HTMLButtonElement;
  }

  function sessionButton(sessionId: number, label: string): HTMLButtonElement {
    const session = fixture.nativeElement
      .querySelector(`#perceived-effort-${sessionId}`)
      .closest('.timeline-session') as HTMLElement;

    return [...session.querySelectorAll('button')].find(
      (button) => button.textContent?.trim() === label,
    ) as HTMLButtonElement;
  }

  function typeSessionInput(selector: string, value: string): void {
    const input = fixture.nativeElement.querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
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
      'select[aria-label="Prontidão informada"]',
    ) as HTMLSelectElement;
    select.value = value;

    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  function toggleInjuryConcern(checked: boolean): void {
    const input = coachingInput('input[aria-label="Estou com dor ou preocupação de lesão"]');
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

  function stravaDataConsentStatus(valid: boolean) {
    return {
      valid,
      currentConsentVersion: 'strava-data-import-and-coaching-v1',
      purpose: 'STRAVA_DATA_IMPORT_AND_COACHING' as const,
    };
  }

  function activityDetail(): ActivityDetail {
    return {
      ...activityListWithSummaries().activities[0],
      totalElevationGainMeters: 42,
      maxSpeedMetersPerSecond: 4,
      averageHeartRate: 150,
      maxHeartRate: 170,
      averageCadence: null,
      averageWatts: null,
      calories: null,
      availableStreamMetricNames: ['heartrate', 'distance'],
      enrichmentStatus: 'COMPLETED',
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
      preferredRunningDays: configured ? ['TUESDAY' as const, 'SATURDAY' as const] : [],
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
          scheduledDate: '2026-07-16',
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

  function adaptiveRunningPlan() {
    return {
      safeMilestone: {
        targetDistanceKilometers: 7.3,
        targetPaceSecondsPerKilometer: 332,
        targetDate: '2026-08-11',
      },
      plannedSessions: conservativeRunningPlan().plannedSessions,
      explanation: 'O plano foi validado pelo Sudolife antes de ser exibido.',
      adjustedBySafetyValidation: true,
    };
  }

  function currentAdaptivePlan(): CurrentAdaptiveRunningPlan {
    const session = (
      id: number,
      status: 'PLANNED' | 'REPLACED' | 'COMPLETED' | 'MISSED',
      scheduledDate: string,
      type: 'EASY_RUN' | 'LONG_RUN' | 'RECOVERY',
      originalPlannedSessionId: number | null = null,
    ) => ({
      id,
      originalPlannedSessionId,
      status,
      adaptationTrigger: status === 'REPLACED' ? ('LOW_READINESS' as const) : null,
      matchedActivityId: status === 'COMPLETED' ? 501 : null,
      postSessionPerceivedEffort: status === 'COMPLETED' ? 7 : null,
      plannedSession: {
        weekNumber: 1,
        sessionNumber: id,
        type,
        distanceKilometers: type === 'RECOVERY' ? 2 : 5,
        scheduledDate,
        target: {
          type: 'PERCEIVED_EFFORT' as const,
          minimumHeartRate: null,
          maximumHeartRate: null,
          minimumPerceivedEffort: 2,
          maximumPerceivedEffort: 4,
        },
      },
    });

    return {
      id: 41,
      safeMilestone: {
        targetDistanceKilometers: 7.3,
        targetPaceSecondsPerKilometer: 332,
        targetDate: '2026-08-11',
      },
      explanation: 'O plano acompanha sua evolução recente.',
      acceptedAt: '2026-07-15T10:00:00Z',
      plannedSessions: [
        session(14, 'PLANNED', '2026-07-20', 'RECOVERY', 13),
        session(12, 'MISSED', '2026-07-18', 'LONG_RUN'),
        session(13, 'REPLACED', '2026-07-20', 'LONG_RUN'),
        session(11, 'COMPLETED', '2026-07-16', 'EASY_RUN'),
      ],
    };
  }

  function currentAdaptivePlanWithUnmatchedCompletedSession(): CurrentAdaptiveRunningPlan {
    return {
      ...currentAdaptivePlan(),
      plannedSessions: currentAdaptivePlan().plannedSessions.map((session) =>
        session.id === 11
          ? { ...session, matchedActivityId: null, postSessionPerceivedEffort: null }
          : session,
      ),
    };
  }

  function recoveryRunningPlan() {
    return {
      classification: 'RECOVERY_ONLY' as const,
      reasons: ['INJURY_CONCERN' as const],
      longTermGoalDistanceKilometers: 42.2,
      durationWeeks: 4,
      sessionsPerWeek: 2,
      weeklyProgressionPercent: 0,
      plannedSessions: [
        {
          weekNumber: 1,
          sessionNumber: 1,
          type: 'RECOVERY' as const,
          distanceKilometers: 0,
          scheduledDate: '2026-07-16',
          target: {
            type: 'PERCEIVED_EFFORT' as const,
            minimumHeartRate: null,
            maximumHeartRate: null,
            minimumPerceivedEffort: 1,
            maximumPerceivedEffort: 3,
          },
        },
      ],
    };
  }

  function runningGoalAssessment() {
    return {
      realistic: false,
      reasons: ['UNREALISTIC_DISTANCE' as const],
      longTermGoal: {
        targetDistanceKilometers: 42.2,
        targetPaceSecondsPerKilometer: 240,
        targetDate: '2026-10-01',
      },
      safeMilestone: {
        targetDistanceKilometers: 7.3,
        targetPaceSecondsPerKilometer: 332,
        targetDate: '2026-08-11',
      },
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
