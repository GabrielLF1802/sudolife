import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { catchError, forkJoin, finalize } from 'rxjs';

import { AuthService, CurrentUser } from '../../../../core/auth/auth.service';
import { ActivityService } from '../../services/activity.service';
import {
  CoachingProfileService,
} from '../../services/coaching-profile.service';
import {
  AdaptiveRunningPlanSession,
  AdaptiveRunningPlanSessionStatus,
  AdaptiveRunningPlan,
  AdaptationTrigger,
  ConservativeRunningPlan,
  CurrentAdaptiveRunningPlan,
  PlannedSession,
  RunningGoalAssessment,
} from '../../services/dtos/adaptive-running-plan';
import { ActivityDetail } from '../../services/dtos/activity-detail';
import { ActivityList, ActivityListItem } from '../../services/dtos/activity-list';
import {
  CoachingProfile,
  RunningHistorySnapshot,
  RunningDay,
  UserReportedReadiness,
} from '../../services/dtos/coaching-profile';
import {
  StravaActivitySyncFailureReason,
  StravaActivitySyncResult,
  StravaActivitySyncStatus,
} from '../../services/dtos/strava-activity-sync';
import { StravaLinkStatus } from '../../services/dtos/strava-link-status';
import {
  StravaAccountService,
} from '../../services/strava-account.service';
import { TrainingProfile } from '../../services/dtos/training-profile';
import { TrainingProfileService } from '../../services/training-profile.service';
import {
  ActivityListItemComponent,
  ActivityListItemOptions,
} from '../activity-list-item/activity-list-item.component';
import { WeeklyRhythmComponent } from '../weekly-rhythm/weekly-rhythm.component';

type ActivityPeriodFilter = 'ALL' | 'LAST_7_DAYS' | 'LAST_30_DAYS';
type DashboardView = 'TODAY' | 'PLAN' | 'ACTIVITIES' | 'SETTINGS';

export type TodayAction =
  | 'CONNECT_STRAVA'
  | 'COMPLETE_PROFILE'
  | 'CLEAR_INJURY_CONCERN'
  | 'SYNC_ACTIVITY'
  | 'REVIEW_MATCH'
  | 'REPORT_EFFORT'
  | 'VIEW_NEXT_SESSION';

export interface TodayActionState {
  stravaLinked: boolean;
  profileComplete: boolean;
  injuryConcern: boolean;
  activitySyncPending: boolean;
  matchPending: boolean;
  effortPending: boolean;
}

export function deriveTodayAction(state: TodayActionState): TodayAction {
  if (!state.stravaLinked) return 'CONNECT_STRAVA';
  if (!state.profileComplete) return 'COMPLETE_PROFILE';
  if (state.injuryConcern) return 'CLEAR_INJURY_CONCERN';
  if (state.activitySyncPending) return 'SYNC_ACTIVITY';
  if (state.matchPending) return 'REVIEW_MATCH';
  if (state.effortPending) return 'REPORT_EFFORT';
  return 'VIEW_NEXT_SESSION';
}

@Component({
  selector: 'app-activity-dashboard',
  imports: [DatePipe, DecimalPipe, ActivityListItemComponent, WeeklyRhythmComponent],
  templateUrl: './activity-dashboard.component.html',
  styleUrl: './activity-dashboard.component.scss',
})
export class ActivityDashboardComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly activityService = inject(ActivityService);
  private readonly stravaAccountService = inject(StravaAccountService);
  private readonly trainingProfileService = inject(TrainingProfileService);
  private readonly coachingProfileService = inject(CoachingProfileService);
  private readonly router = inject(Router);

  protected readonly currentUser = signal<CurrentUser | null>(null);
  protected readonly activityList = signal<ActivityList | null>(null);
  protected readonly stravaLinkStatus = signal<StravaLinkStatus | null>(null);
  protected readonly trainingProfile = signal<TrainingProfile | null>(null);
  protected readonly coachingProfile = signal<CoachingProfile | null>(null);
  protected readonly runningHistory = signal<RunningHistorySnapshot | null>(null);
  protected readonly conservativeRunningPlan = signal<ConservativeRunningPlan | null>(null);
  protected readonly adaptiveRunningPlan = signal<AdaptiveRunningPlan | null>(null);
  protected readonly currentAdaptiveRunningPlan = signal<CurrentAdaptiveRunningPlan | null>(null);
  protected readonly runningGoalAssessment = signal<RunningGoalAssessment | null>(null);
  protected readonly loading = signal(true);
  protected readonly pageLoading = signal(false);
  protected readonly planLoading = signal(false);
  protected readonly linking = signal(false);
  protected readonly syncing = signal(false);
  protected readonly savingTrainingProfile = signal(false);
  protected readonly savingCoachingProfile = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly pageErrorMessage = signal('');
  protected readonly planErrorMessage = signal('');
  protected readonly linkingErrorMessage = signal('');
  protected readonly syncErrorMessage = signal('');
  protected readonly trainingProfileErrorMessage = signal('');
  protected readonly trainingProfileSuccessMessage = signal('');
  protected readonly coachingProfileErrorMessage = signal('');
  protected readonly coachingProfileSuccessMessage = signal('');
  protected readonly sessionOperationIds = signal<ReadonlySet<number>>(new Set());
  protected readonly sessionErrors = signal<Readonly<Partial<Record<number, string>>>>({});
  protected readonly sessionSuccesses = signal<Readonly<Partial<Record<number, string>>>>({});
  protected readonly selectedMatchActivityIds = signal<Readonly<Partial<Record<number, string>>>>(
    {},
  );
  protected readonly unlinkConfirmationId = signal<number | null>(null);
  protected readonly perceivedEfforts = signal<Readonly<Partial<Record<number, string>>>>({});
  protected readonly adaptationLoading = signal(false);
  protected readonly adaptationErrorMessage = signal('');
  protected readonly adaptationAnnouncement = signal('');
  protected readonly previousNextSession = signal<AdaptiveRunningPlanSession | null>(null);
  protected readonly syncResult = signal<StravaActivitySyncResult | null>(null);
  protected readonly activityDetails = signal<Readonly<Partial<Record<number, ActivityDetail>>>>(
    {},
  );
  protected readonly openActivityIds = signal<ReadonlySet<number>>(new Set());
  protected readonly activityDetailLoadingIds = signal<ReadonlySet<number>>(new Set());
  protected readonly activityDetailErrors = signal<Readonly<Partial<Record<number, string>>>>({});
  protected readonly unlinkingStrava = signal(false);
  protected readonly stravaUnlinkConfirmationOpen = signal(false);
  protected readonly stravaUnlinkMessage = signal('');
  protected readonly clearInjuryConfirmationOpen = signal(false);
  protected readonly clearInjuryReadiness = signal<UserReportedReadiness | ''>('');
  protected readonly clearingInjuryConcern = signal(false);
  protected readonly clearInjuryErrorMessage = signal('');
  protected readonly clearInjuryAnnouncement = signal('');
  protected readonly birthYear = signal('');
  protected readonly targetDistanceKilometers = signal('');
  protected readonly targetPace = signal('');
  protected readonly targetDate = signal('');
  protected readonly readiness = signal<UserReportedReadiness | ''>('');
  protected readonly injuryConcern = signal(false);
  protected readonly preferredRunningDays = signal<RunningDay[]>([]);
  protected readonly runningDayOptions: ReadonlyArray<{ value: RunningDay; label: string }> = [
    { value: 'MONDAY', label: 'Seg' },
    { value: 'TUESDAY', label: 'Ter' },
    { value: 'WEDNESDAY', label: 'Qua' },
    { value: 'THURSDAY', label: 'Qui' },
    { value: 'FRIDAY', label: 'Sex' },
    { value: 'SATURDAY', label: 'Sáb' },
    { value: 'SUNDAY', label: 'Dom' },
  ];
  protected readonly activeView = signal<DashboardView>('TODAY');
  protected readonly selectedActivityType = signal('ALL');
  protected readonly selectedPeriod = signal<ActivityPeriodFilter>('ALL');
  protected readonly minimumDistanceKilometers = signal('');
  protected readonly maximumDistanceKilometers = signal('');
  protected readonly currentDate = signal(new Date());
  protected readonly futurePlanSessions = computed(
    () =>
      this.conservativeRunningPlan()?.plannedSessions.filter((session) => session.weekNumber > 1) ??
      [],
  );
  protected readonly nextAdaptivePlanSession = computed(() => {
    const sessions = this.currentAdaptiveRunningPlan()?.plannedSessions ?? [];

    return (
      sessions
        .filter((session) => session.status === 'PLANNED')
        .sort((left, right) => this.compareAdaptiveSessions(left, right))[0] ?? null
    );
  });
  protected readonly nextTrainingSession = computed<PlannedSession | null>(() => {
    const acceptedSession = this.nextAdaptivePlanSession()?.plannedSession;

    if (acceptedSession) {
      return acceptedSession;
    }

    const sessions =
      this.conservativeRunningPlan()?.plannedSessions ??
      this.adaptiveRunningPlan()?.plannedSessions ??
      [];

    return (
      [...sessions].sort((left, right) =>
        left.scheduledDate.localeCompare(right.scheduledDate),
      )[0] ?? null
    );
  });
  protected readonly adaptivePlanHistory = computed(() => {
    const sessions = this.currentAdaptiveRunningPlan()?.plannedSessions ?? [];

    return [...sessions].sort((left, right) => this.compareAdaptiveSessions(left, right));
  });
  protected readonly activityTypes = computed(() => {
    const activityList = this.activityList();

    if (activityList === null) {
      return [];
    }

    return [...new Set(activityList.activities.map((activity) => activity.sportType))].sort();
  });
  protected readonly filteredActivities = computed(() => {
    const activityList = this.activityList();

    if (activityList === null) {
      return [];
    }

    return activityList.activities.filter((activity) => {
      if (
        this.selectedActivityType() !== 'ALL' &&
        activity.sportType !== this.selectedActivityType()
      ) {
        return false;
      }

      if (!this.matchesSelectedPeriod(activity.startDate)) {
        return false;
      }

      const minimumDistanceMeters = this.distanceMeters(this.minimumDistanceKilometers());
      const maximumDistanceMeters = this.distanceMeters(this.maximumDistanceKilometers());

      if (minimumDistanceMeters !== null && activity.distanceMeters < minimumDistanceMeters) {
        return false;
      }

      return maximumDistanceMeters === null || activity.distanceMeters <= maximumDistanceMeters;
    });
  });
  protected readonly hasActiveFilters = computed(
    () =>
      this.selectedActivityType() !== 'ALL' ||
      this.selectedPeriod() !== 'ALL' ||
      this.minimumDistanceKilometers().trim() !== '' ||
      this.maximumDistanceKilometers().trim() !== '',
  );
  protected readonly importedRuns = computed(
    () => this.activityList()?.activities.filter((activity) => activity.sportType === 'RUN') ?? [],
  );
  protected readonly todayAction = computed(() => {
    const strava = this.stravaLinkStatus();
    const training = this.trainingProfile();
    const coaching = this.coachingProfile();
    const sessions = this.currentAdaptiveRunningPlan()?.plannedSessions ?? [];

    return deriveTodayAction({
      stravaLinked: strava?.linked ?? false,
      profileComplete: Boolean(training?.adaptiveCoachingEligible && coaching?.configured),
      injuryConcern: coaching?.injuryConcern ?? false,
      activitySyncPending: strava?.activitySummaryStatus !== 'COMPLETED',
      matchPending: sessions.some(
        (session) => session.status === 'COMPLETED' && !session.matchedActivityId,
      ),
      effortPending: sessions.some(
        (session) => session.status === 'COMPLETED' && session.postSessionPerceivedEffort === null,
      ),
    });
  });

  ngOnInit(): void {
    this.loadDashboard();
  }

  protected loadDashboard(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    forkJoin({
      currentUser: this.authService.currentUser(),
      activityList: this.activityService.list(),
      stravaLinkStatus: this.stravaAccountService.status(),
      trainingProfile: this.trainingProfileService.get(),
      coachingProfile: this.coachingProfileService.get(),
      runningHistory: this.coachingProfileService.getRunningHistory(),
    }).subscribe({
      next: ({
        currentUser,
        activityList,
        stravaLinkStatus,
        trainingProfile,
        coachingProfile,
        runningHistory,
      }) => {
        this.currentUser.set(currentUser);
        this.activityList.set(activityList);
        this.stravaLinkStatus.set(stravaLinkStatus);
        this.trainingProfile.set(trainingProfile);
        this.coachingProfile.set(coachingProfile);
        this.runningHistory.set(runningHistory);
        this.birthYear.set(trainingProfile.birthYear?.toString() ?? '');
        this.fillCoachingProfileForm(coachingProfile);
        this.loading.set(false);
        this.loadConservativeRunningPlan(coachingProfile, runningHistory);
        this.loadAdaptiveRunningPlan(coachingProfile, runningHistory);
        this.loadRunningGoalAssessment(coachingProfile);
      },
      error: () => {
        this.errorMessage.set(
          'Não foi possível carregar seus dados de treino. Verifique sua conexão e tente novamente.',
        );
        this.loading.set(false);
      },
    });
  }

  protected updateActivityTypeFilter(event: Event): void {
    this.selectedActivityType.set((event.target as HTMLSelectElement).value);
  }

  protected updatePeriodFilter(event: Event): void {
    this.selectedPeriod.set((event.target as HTMLSelectElement).value as ActivityPeriodFilter);
  }

  protected updateMinimumDistanceFilter(event: Event): void {
    this.minimumDistanceKilometers.set((event.target as HTMLInputElement).value);
  }

  protected updateMaximumDistanceFilter(event: Event): void {
    this.maximumDistanceKilometers.set((event.target as HTMLInputElement).value);
  }

  protected updateBirthYear(event: Event): void {
    this.birthYear.set((event.target as HTMLInputElement).value);
  }

  protected updateTargetDistance(event: Event): void {
    this.targetDistanceKilometers.set((event.target as HTMLInputElement).value);
  }

  protected updateTargetPace(event: Event): void {
    this.targetPace.set((event.target as HTMLInputElement).value);
  }

  protected updateTargetDate(event: Event): void {
    this.targetDate.set((event.target as HTMLInputElement).value);
  }

  protected updateReadiness(event: Event): void {
    this.readiness.set((event.target as HTMLSelectElement).value as UserReportedReadiness | '');
  }

  protected updateInjuryConcern(event: Event): void {
    this.injuryConcern.set((event.target as HTMLInputElement).checked);
  }

  protected updatePreferredRunningDay(day: RunningDay, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    const selectedDays = this.preferredRunningDays();

    this.preferredRunningDays.set(
      checked ? [...selectedDays, day] : selectedDays.filter((selectedDay) => selectedDay !== day),
    );
  }

  protected selectView(view: DashboardView): void {
    this.activeView.set(view);
  }

  protected clearActivityFilters(): void {
    this.selectedActivityType.set('ALL');
    this.selectedPeriod.set('ALL');
    this.minimumDistanceKilometers.set('');
    this.maximumDistanceKilometers.set('');
  }

  protected loadPage(page: number): void {
    this.pageLoading.set(true);
    this.pageErrorMessage.set('');

    this.activityService
      .list(page)
      .pipe(finalize(() => this.pageLoading.set(false)))
      .subscribe({
        next: (activityList) => {
          this.activityList.set(activityList);
        },
        error: () => {
          this.pageErrorMessage.set(
            'Não foi possível carregar outra página. As atividades atuais foram preservadas.',
          );
        },
      });
  }

  protected retryCurrentPage(): void {
    const currentPage = this.activityList()?.page ?? 0;

    this.loadPage(currentPage);
  }

  protected hasPreviousPage(activityList: ActivityList): boolean {
    return activityList.page > 0;
  }

  protected hasNextPage(activityList: ActivityList): boolean {
    return activityList.page + 1 < activityList.totalPages;
  }

  protected isSyncEnabled(status: StravaLinkStatus): boolean {
    return status.permissionState === 'READY';
  }

  protected shouldShowImportedEmptyState(status: StravaLinkStatus | null): boolean {
    return status === null || this.isSyncEnabled(status);
  }

  protected logout(): void {
    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }

  protected startStravaLinking(): void {
    this.linking.set(true);
    this.linkingErrorMessage.set('');

    this.stravaAccountService.startLinking().subscribe({
      next: (result) => {
        window.location.assign(result.authorizationUrl);
      },
      error: () => {
        this.linking.set(false);
        this.linkingErrorMessage.set(
          'Não foi possível abrir a conexão com o Strava. Verifique sua conexão e tente novamente.',
        );
      },
    });
  }

  protected requestActivitySync(): void {
    this.syncing.set(true);
    this.syncErrorMessage.set('');
    this.syncResult.set(null);

    this.stravaAccountService
      .requestSync()
      .pipe(finalize(() => this.syncing.set(false)))
      .subscribe({
        next: (result) => {
          this.syncResult.set(result);
          if (result.status === 'COMPLETED') {
            const status = this.stravaLinkStatus();
            if (status !== null)
              this.stravaLinkStatus.set({ ...status, activitySummaryStatus: 'COMPLETED' });
          }
        },
        error: () => {
          this.syncErrorMessage.set(
            'Não foi possível iniciar a sincronização. Verifique sua conexão e tente novamente.',
          );
        },
      });
  }

  protected toggleActivityDetail(activityId: number): void {
    if (this.openActivityIds().has(activityId)) {
      this.openActivityIds.update((ids) => {
        const next = new Set(ids);
        next.delete(activityId);
        return next;
      });
      return;
    }

    this.openActivityIds.update((ids) => new Set([...ids, activityId]));
    if (!this.activityDetails()[activityId]) this.loadActivityDetail(activityId);
  }

  protected loadActivityDetail(activityId: number): void {
    if (this.activityDetailLoadingIds().has(activityId)) return;

    this.activityDetailLoadingIds.update((ids) => new Set([...ids, activityId]));
    this.activityDetailErrors.update(({ [activityId]: _, ...errors }) => errors);
    this.activityService
      .getDetail(activityId)
      .pipe(
        finalize(() => {
          this.activityDetailLoadingIds.update((ids) => {
            const next = new Set(ids);
            next.delete(activityId);
            return next;
          });
        }),
      )
      .subscribe({
        next: (detail) =>
          this.activityDetails.update((details) => ({ ...details, [activityId]: detail })),
        error: () =>
          this.activityDetailErrors.update((errors) => ({
            ...errors,
            [activityId]: 'Não foi possível carregar os detalhes desta atividade.',
          })),
      });
  }

  protected requestStravaUnlink(): void {
    this.stravaUnlinkConfirmationOpen.set(true);
    this.stravaUnlinkMessage.set('');
  }

  protected cancelStravaUnlink(): void {
    if (!this.unlinkingStrava()) this.stravaUnlinkConfirmationOpen.set(false);
  }

  protected unlinkStrava(): void {
    if (this.unlinkingStrava()) return;
    this.unlinkingStrava.set(true);
    this.stravaUnlinkMessage.set('');
    this.stravaAccountService
      .unlink()
      .pipe(finalize(() => this.unlinkingStrava.set(false)))
      .subscribe({
        next: () => {
          const status = this.stravaLinkStatus();
          if (status !== null)
            this.stravaLinkStatus.set({
              ...status,
              linked: false,
              athleteId: null,
              permissionState: 'UNLINKED',
              profilePermissionState: 'UNLINKED',
              activitySummaryStatus: 'UNLINKED',
              performanceDataStatus: 'UNLINKED',
            });
          this.stravaUnlinkConfirmationOpen.set(false);
          this.stravaUnlinkMessage.set(
            'Conta Strava desvinculada. Seus dados já importados foram preservados.',
          );
        },
        error: () =>
          this.stravaUnlinkMessage.set(
            'Não foi possível desvincular. Sua conexão foi preservada; tente novamente.',
          ),
      });
  }

  protected updateClearInjuryReadiness(event: Event): void {
    this.clearInjuryReadiness.set(
      (event.target as HTMLSelectElement).value as UserReportedReadiness | '',
    );
  }

  protected clearInjuryConcern(): void {
    const readiness = this.clearInjuryReadiness();
    if (!readiness || this.clearingInjuryConcern()) {
      this.clearInjuryErrorMessage.set('Selecione sua prontidão atual antes de continuar.');
      return;
    }

    const previousSession = this.nextAdaptivePlanSession();
    this.clearingInjuryConcern.set(true);
    this.clearInjuryErrorMessage.set('');
    this.coachingProfileService
      .clearInjuryConcern({ readiness })
      .pipe(finalize(() => this.clearingInjuryConcern.set(false)))
      .subscribe({
        next: (plan) => {
          const profile = this.coachingProfile();
          if (profile !== null) {
            const updated = { ...profile, injuryConcern: false, readiness };
            this.coachingProfile.set(updated);
            this.fillCoachingProfileForm(updated);
          }
          this.currentAdaptiveRunningPlan.set(plan);
          this.conservativeRunningPlan.set(null);
          this.clearInjuryConfirmationOpen.set(false);
          this.announceNextSessionChange(previousSession);
          this.clearInjuryAnnouncement.set(
            'Preocupação encerrada. Sua retomada conservadora e a próxima sessão já foram atualizadas.',
          );
        },
        error: () =>
          this.clearInjuryErrorMessage.set(
            'Não foi possível encerrar a preocupação. Seu perfil e plano anteriores foram preservados.',
          ),
      });
  }

  protected todayActionTitle(action: TodayAction): string {
    return {
      CONNECT_STRAVA: 'Conecte o Strava',
      COMPLETE_PROFILE: 'Complete seu perfil e objetivo',
      CLEAR_INJURY_CONCERN: 'Retome quando estiver pronto',
      SYNC_ACTIVITY: 'Sincronize sua corrida mais recente',
      REVIEW_MATCH: 'Confirme qual corrida concluiu sua sessão',
      REPORT_EFFORT: 'Informe como foi sua sessão',
      VIEW_NEXT_SESSION: 'Consulte sua próxima sessão',
    }[action];
  }

  protected activateTodayAction(action: TodayAction): void {
    if (action === 'CONNECT_STRAVA') {
      this.startStravaLinking();
      return;
    }
    if (action === 'CLEAR_INJURY_CONCERN') {
      this.clearInjuryConfirmationOpen.set(true);
      return;
    }
    if (action === 'SYNC_ACTIVITY') {
      this.requestActivitySync();
      return;
    }
    this.selectView(
      action === 'COMPLETE_PROFILE'
        ? 'SETTINGS'
        : action === 'VIEW_NEXT_SESSION' || action === 'REVIEW_MATCH' || action === 'REPORT_EFFORT'
          ? 'PLAN'
          : 'ACTIVITIES',
    );
  }

  protected saveTrainingProfile(): void {
    if (this.savingTrainingProfile()) {
      return;
    }

    const birthYear = this.parsedBirthYear();
    if (!this.isValidBirthYear(birthYear)) {
      this.trainingProfileSuccessMessage.set('');
      this.trainingProfileErrorMessage.set('Informe um ano entre 1900 e o ano atual.');
      return;
    }

    this.savingTrainingProfile.set(true);
    this.trainingProfileErrorMessage.set('');
    this.trainingProfileSuccessMessage.set('');

    this.trainingProfileService
      .save({ birthYear })
      .pipe(finalize(() => this.savingTrainingProfile.set(false)))
      .subscribe({
        next: (profile) => {
          this.trainingProfile.set(profile);
          this.birthYear.set(profile.birthYear?.toString() ?? '');
          this.trainingProfileSuccessMessage.set(
            'Perfil salvo. O ano de nascimento já pode orientar seus treinos.',
          );
        },
        error: () => {
          this.trainingProfileErrorMessage.set(
            'Não foi possível salvar. O ano informado foi preservado; revise o valor e tente novamente.',
          );
        },
      });
  }

  protected saveCoachingProfile(): void {
    if (this.savingCoachingProfile()) {
      return;
    }

    const coachingProfileValidationMessage = this.coachingProfileValidationMessage();
    if (coachingProfileValidationMessage !== null) {
      this.coachingProfileSuccessMessage.set('');
      this.coachingProfileErrorMessage.set(coachingProfileValidationMessage);
      return;
    }

    this.savingCoachingProfile.set(true);
    this.coachingProfileErrorMessage.set('');
    this.coachingProfileSuccessMessage.set('');

    this.coachingProfileService
      .save({
        targetDistanceKilometers: this.parsedTargetDistance(),
        targetPaceSecondsPerKilometer: this.parsedTargetPaceSeconds(),
        targetDate: this.targetDate().trim() || null,
        readiness: this.readiness(),
        injuryConcern: this.injuryConcern(),
        preferredRunningDays: this.preferredRunningDays(),
      })
      .pipe(finalize(() => this.savingCoachingProfile.set(false)))
      .subscribe({
        next: (profile) => {
          this.coachingProfile.set(profile);
          this.fillCoachingProfileForm(profile);
          this.coachingProfileSuccessMessage.set(
            'Meta e prontidão salvas. Seu plano será atualizado com essas informações.',
          );
          this.conservativeRunningPlan.set(null);
          this.adaptiveRunningPlan.set(null);
          this.currentAdaptiveRunningPlan.set(null);
          this.runningGoalAssessment.set(null);

          const runningHistory = this.runningHistory();
          if (runningHistory !== null) {
            this.loadConservativeRunningPlan(profile, runningHistory);
            this.loadAdaptiveRunningPlan(profile, runningHistory);
          }
          this.loadRunningGoalAssessment(profile);
        },
        error: () => {
          this.coachingProfileErrorMessage.set(
            'Não foi possível salvar. Sua meta, prontidão e preocupação de lesão foram preservadas.',
          );
        },
      });
  }

  protected readinessLabel(readiness: UserReportedReadiness | null): string {
    if (readiness === 'LOW') {
      return 'Baixa';
    }

    if (readiness === 'MODERATE') {
      return 'Moderada';
    }

    if (readiness === 'HIGH') {
      return 'Alta';
    }

    return 'Não informada';
  }

  protected plannedSessionTypeLabel(session: PlannedSession): string {
    if (session.type === 'RECOVERY') {
      return 'Sessão de recuperação';
    }

    return session.type === 'EASY_RUN' ? 'Corrida leve' : 'Corrida longa';
  }

  protected plannedSessionTargetLabel(session: PlannedSession): string {
    if (session.target.type === 'HEART_RATE') {
      return `${session.target.minimumHeartRate}-${session.target.maximumHeartRate} bpm`;
    }

    return `Esforço percebido ${session.target.minimumPerceivedEffort}-${session.target.maximumPerceivedEffort}`;
  }

  protected plannedSessionDateLabel(session: PlannedSession): string {
    const [year, month, day] = session.scheduledDate.split('-');

    return `${day}/${month}/${year}`;
  }

  protected adaptiveSessionStatusLabel(status: AdaptiveRunningPlanSessionStatus): string {
    const labels: Record<AdaptiveRunningPlanSessionStatus, string> = {
      PLANNED: 'Planejada',
      COMPLETED: 'Concluída',
      MISSED: 'Perdida',
      REPLACED: 'Substituída',
    };

    return labels[status];
  }

  protected replacementFor(session: AdaptiveRunningPlanSession): AdaptiveRunningPlanSession | null {
    return (
      this.currentAdaptiveRunningPlan()?.plannedSessions.find(
        (candidate) => candidate.originalPlannedSessionId === session.id,
      ) ?? null
    );
  }

  protected adaptiveSessionAriaLabel(session: AdaptiveRunningPlanSession): string {
    return `${this.adaptiveSessionStatusLabel(session.status)}: ${this.plannedSessionTypeLabel(session.plannedSession)}, ${this.plannedSessionDateLabel(session.plannedSession)}`;
  }

  protected activityById(activityId: number | null): ActivityListItem | null {
    return this.activityList()?.activities.find((activity) => activity.id === activityId) ?? null;
  }

  protected activityOptionLabel(activity: ActivityListItem): string {
    return `${activity.name} · ${new Date(activity.startDate).toLocaleDateString('pt-BR')} · ${(activity.distanceMeters / 1000).toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })} km`;
  }

  protected updateMatchActivity(sessionId: number, event: Event): void {
    this.selectedMatchActivityIds.update((values) => ({
      ...values,
      [sessionId]: (event.target as HTMLSelectElement).value,
    }));
  }

  protected correctSessionMatch(sessionId: number): void {
    const selectedActivityId = this.selectedMatchActivityIds()[sessionId]?.trim() ?? '';
    const activityId = Number(selectedActivityId);

    if (selectedActivityId === '' || !Number.isInteger(activityId)) {
      this.setSessionError(sessionId, 'Escolha a corrida que corresponde a esta sessão.');
      return;
    }

    this.beginSessionOperation(sessionId);
    this.coachingProfileService
      .correctPlannedSessionMatch({ plannedSessionId: sessionId, activityId })
      .pipe(finalize(() => this.endSessionOperation(sessionId)))
      .subscribe({
        next: (plan) => {
          this.currentAdaptiveRunningPlan.set(plan);
          this.setSessionSuccess(sessionId, 'Corrida da sessão atualizada.');
        },
        error: () =>
          this.setSessionError(
            sessionId,
            'Não foi possível trocar a corrida desta sessão. O plano atual foi preservado.',
          ),
      });
  }

  protected requestUnlinkConfirmation(sessionId: number): void {
    this.unlinkConfirmationId.set(sessionId);
  }

  protected cancelUnlinkConfirmation(): void {
    this.unlinkConfirmationId.set(null);
  }

  protected unlinkSessionMatch(sessionId: number): void {
    this.beginSessionOperation(sessionId);
    this.coachingProfileService
      .unlinkPlannedSessionMatch(sessionId)
      .pipe(finalize(() => this.endSessionOperation(sessionId)))
      .subscribe({
        next: (plan) => {
          this.currentAdaptiveRunningPlan.set(plan);
          this.unlinkConfirmationId.set(null);
          this.setSessionSuccess(
            sessionId,
            'Corrida retirada da sessão. Ela continua disponível em Atividades.',
          );
        },
        error: () =>
          this.setSessionError(
            sessionId,
            'Não foi possível retirar a corrida desta sessão. O plano e a corrida foram preservados.',
          ),
      });
  }

  protected updatePerceivedEffort(sessionId: number, event: Event): void {
    this.perceivedEfforts.update((values) => ({
      ...values,
      [sessionId]: (event.target as HTMLInputElement).value,
    }));
  }

  protected submitPerceivedEffort(sessionId: number): void {
    const effort = Number(this.perceivedEfforts()[sessionId]);

    if (!Number.isInteger(effort) || effort < 1 || effort > 10) {
      this.setSessionError(sessionId, 'Informe um número inteiro de 1 a 10.');
      return;
    }

    const previousNextSession = this.nextAdaptivePlanSession();
    this.beginSessionOperation(sessionId);
    this.coachingProfileService
      .submitPostSessionPerceivedEffort(sessionId, { perceivedEffort: effort })
      .pipe(finalize(() => this.endSessionOperation(sessionId)))
      .subscribe({
        next: (plan) => {
          this.currentAdaptiveRunningPlan.set(plan);
          this.setSessionSuccess(sessionId, `Esforço ${effort} de 10 registrado.`);
          this.announceNextSessionChange(previousNextSession);
        },
        error: () =>
          this.setSessionError(
            sessionId,
            'Não foi possível salvar o esforço. O plano anterior foi preservado.',
          ),
      });
  }

  protected adaptForLowReadiness(): void {
    if (this.nextAdaptivePlanSession() === null || this.adaptationLoading()) {
      return;
    }

    const previousNextSession = this.nextAdaptivePlanSession();
    this.adaptationLoading.set(true);
    this.adaptationErrorMessage.set('');
    this.coachingProfileService
      .adaptNextPlannedSession({ trigger: 'LOW_READINESS' })
      .pipe(finalize(() => this.adaptationLoading.set(false)))
      .subscribe({
        next: (plan) => {
          this.currentAdaptiveRunningPlan.set(plan);
          this.announceNextSessionChange(previousNextSession);
        },
        error: () =>
          this.adaptationErrorMessage.set(
            'Não foi possível adaptar a próxima sessão. O plano anterior foi preservado.',
          ),
      });
  }

  protected isSessionOperating(sessionId: number): boolean {
    return this.sessionOperationIds().has(sessionId);
  }

  protected activityTypeLabel(activityType: string): string {
    const labels: Record<string, string> = {
      RUN: 'Corrida',
      WALK: 'Caminhada',
      RIDE: 'Pedalada',
      WEIGHT_TRAINING: 'Musculação',
    };

    return labels[activityType] ?? 'Outra atividade';
  }

  protected performanceDataStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      IMPORTED: 'Disponíveis',
      COMPLETED: 'Disponíveis',
      PENDING: 'Sendo preparados',
      RUNNING: 'Sendo preparados',
      FAILED: 'Indisponíveis no momento',
    };

    return labels[status] ?? 'Ainda não disponíveis';
  }

  protected activityListItemOptions(activity: ActivityListItem): ActivityListItemOptions {
    return {
      activity,
      detail: this.activityDetails()[activity.id] ?? null,
      detailError: this.activityDetailErrors()[activity.id] ?? '',
      detailLoading: this.activityDetailLoadingIds().has(activity.id),
      open: this.openActivityIds().has(activity.id),
    };
  }

  protected stravaActionLabel(status: StravaLinkStatus): string {
    if (status.permissionState === 'PERMISSION_UPGRADE_REQUIRED') {
      return 'Atualizar permissões';
    }

    if (status.linked) {
      return 'Reconectar Strava';
    }

    return 'Conectar Strava';
  }

  protected stravaStatusText(status: StravaLinkStatus): string {
    if (status.permissionState === 'PERMISSION_UPGRADE_REQUIRED') {
      return 'Permissões incompletas';
    }

    if (status.linked) {
      return `Conectado ao atleta ${status.athleteId}`;
    }

    return 'Não conectado';
  }

  protected profileZoneStatusText(
    profile: TrainingProfile,
    status: StravaLinkStatus | null,
  ): string {
    if (profile.heartRateZoneSource === 'STRAVA') {
      return 'Zonas de frequência cardíaca importadas do Strava.';
    }

    if (profile.heartRateZoneSource === 'AGE_BASED') {
      return 'Zonas calculadas pelo ano de nascimento.';
    }

    if (status?.profilePermissionState === 'OPTIONAL_UPGRADE_AVAILABLE') {
      return 'As zonas do Strava são opcionais. Atualize a conexão para tentar importá-las.';
    }

    return 'As zonas do Strava são opcionais e não impedem a orientação de treino.';
  }

  protected syncStatusLabel(status: StravaActivitySyncStatus): string {
    if (status === 'COMPLETED') {
      return 'Sincronização iniciada';
    }

    if (status === 'UNLINKED') {
      return 'Strava não conectado';
    }

    return 'Sincronização não iniciada';
  }

  protected syncFailureReasonLabel(failureReason: StravaActivitySyncFailureReason): string {
    if (failureReason === 'PERMISSION_UPGRADE_REQUIRED') {
      return 'Atualize as permissões do Strava para importar atividades.';
    }

    if (failureReason === 'SYNC_ALREADY_RUNNING') {
      return 'Já existe uma sincronização em andamento. Aguarde alguns instantes e atualize novamente.';
    }

    if (failureReason === 'STRAVA_RATE_LIMITED') {
      return 'O Strava limitou novas sincronizações no momento. Tente novamente mais tarde.';
    }

    return 'O Strava está indisponível no momento. Tente novamente mais tarde.';
  }

  private matchesSelectedPeriod(startDate: string): boolean {
    if (this.selectedPeriod() === 'ALL') {
      return true;
    }

    const periodStart = new Date(this.currentDate());
    periodStart.setHours(0, 0, 0, 0);
    periodStart.setDate(periodStart.getDate() - this.selectedPeriodDays());

    return new Date(startDate) >= periodStart;
  }

  private loadConservativeRunningPlan(
    coachingProfile: CoachingProfile,
    runningHistory: RunningHistorySnapshot,
  ): void {
    const requiresConservativePlan =
      coachingProfile.configured &&
      (coachingProfile.injuryConcern ||
        !runningHistory.sufficientRunningHistory ||
        coachingProfile.readiness === 'LOW');

    if (!requiresConservativePlan) {
      this.planErrorMessage.set('');
      return;
    }

    this.planLoading.set(true);
    this.planErrorMessage.set('');
    this.coachingProfileService
      .generateConservativeRunningPlan()
      .pipe(finalize(() => this.planLoading.set(false)))
      .subscribe({
        next: (plan) => this.conservativeRunningPlan.set(plan),
        error: () =>
          this.planErrorMessage.set(
            'Seu perfil foi preservado, mas não foi possível atualizar o plano.',
          ),
      });
  }

  private loadAdaptiveRunningPlan(
    coachingProfile: CoachingProfile,
    runningHistory: RunningHistorySnapshot,
  ): void {
    const requiresConservativePlan =
      coachingProfile.injuryConcern ||
      !runningHistory.sufficientRunningHistory ||
      coachingProfile.readiness === 'LOW';

    if (!coachingProfile.configured || requiresConservativePlan) {
      return;
    }

    this.planLoading.set(true);
    this.planErrorMessage.set('');
    this.coachingProfileService
      .getCurrentAdaptiveRunningPlan()
      .pipe(catchError(() => this.coachingProfileService.generateAdaptiveRunningPlan()))
      .pipe(finalize(() => this.planLoading.set(false)))
      .subscribe({
        next: (plan) => {
          if ('id' in plan) {
            this.currentAdaptiveRunningPlan.set(plan);
            this.adaptiveRunningPlan.set(null);
            return;
          }

          this.adaptiveRunningPlan.set(plan);
        },
        error: () =>
          this.planErrorMessage.set(
            'Seu perfil foi preservado, mas não foi possível atualizar o plano.',
          ),
      });
  }

  private compareAdaptiveSessions(
    left: AdaptiveRunningPlanSession,
    right: AdaptiveRunningPlanSession,
  ): number {
    const dateComparison = left.plannedSession.scheduledDate.localeCompare(
      right.plannedSession.scheduledDate,
    );

    if (dateComparison !== 0) {
      return dateComparison;
    }

    if (right.originalPlannedSessionId === left.id) {
      return -1;
    }

    if (left.originalPlannedSessionId === right.id) {
      return 1;
    }

    return left.id - right.id;
  }

  protected adaptationTriggerLabel(
    trigger: AdaptationTrigger | null | undefined,
  ): string {
    switch (trigger) {
      case 'MISSED_PLANNED_SESSION':
        return 'sessão anterior perdida';
      case 'COMPLETED_PLANNED_SESSION':
        return 'sessão anterior concluída';
      case 'INJURY_CONCERN':
        return 'preocupação de lesão';
      case 'LOW_READINESS':
        return 'baixa prontidão';
      case 'UNEXPECTEDLY_HIGH_EFFORT':
        return 'esforço acima do esperado';
      case 'UNEXPECTEDLY_LOW_EFFORT':
        return 'esforço abaixo do esperado';
      default:
        return 'contexto recente de treino';
    }
  }

  private beginSessionOperation(sessionId: number): void {
    this.sessionOperationIds.update((ids) => new Set([...ids, sessionId]));
    this.sessionErrors.update(({ [sessionId]: _, ...messages }) => messages);
    this.sessionSuccesses.update(({ [sessionId]: _, ...messages }) => messages);
  }

  private endSessionOperation(sessionId: number): void {
    this.sessionOperationIds.update((ids) => {
      const remainingIds = new Set(ids);
      remainingIds.delete(sessionId);
      return remainingIds;
    });
  }

  private setSessionError(sessionId: number, message: string): void {
    this.sessionErrors.update((messages) => ({ ...messages, [sessionId]: message }));
    this.sessionSuccesses.update(({ [sessionId]: _, ...messages }) => messages);
  }

  private setSessionSuccess(sessionId: number, message: string): void {
    this.sessionSuccesses.update((messages) => ({ ...messages, [sessionId]: message }));
    this.sessionErrors.update(({ [sessionId]: _, ...messages }) => messages);
  }

  private announceNextSessionChange(previousSession: AdaptiveRunningPlanSession | null): void {
    const nextSession = this.nextAdaptivePlanSession();

    if (
      previousSession === null ||
      nextSession === null ||
      this.sameSession(previousSession, nextSession)
    ) {
      this.adaptationAnnouncement.set('Esforço salvo. A próxima sessão foi mantida.');
      this.previousNextSession.set(null);
      return;
    }

    this.previousNextSession.set(previousSession);
    this.adaptationAnnouncement.set(
      `Próxima sessão adaptada para ${this.plannedSessionTypeLabel(nextSession.plannedSession)}, considerando ${this.adaptationTriggerLabel(nextSession.adaptationTrigger)}.`,
    );
  }

  private sameSession(
    left: AdaptiveRunningPlanSession,
    right: AdaptiveRunningPlanSession,
  ): boolean {
    return (
      left.id === right.id &&
      left.plannedSession.type === right.plannedSession.type &&
      left.plannedSession.distanceKilometers === right.plannedSession.distanceKilometers &&
      left.plannedSession.scheduledDate === right.plannedSession.scheduledDate
    );
  }

  private loadRunningGoalAssessment(coachingProfile: CoachingProfile): void {
    if (!coachingProfile.configured) {
      return;
    }

    this.coachingProfileService.evaluateRunningGoal().subscribe({
      next: (assessment) => this.runningGoalAssessment.set(assessment),
      error: () => this.runningGoalAssessment.set(null),
    });
  }

  protected retryConservativeRunningPlan(): void {
    const profile = this.coachingProfile();
    const history = this.runningHistory();

    if (profile !== null && history !== null) {
      this.loadConservativeRunningPlan(profile, history);
      this.loadAdaptiveRunningPlan(profile, history);
    }
  }

  private selectedPeriodDays(): number {
    if (this.selectedPeriod() === 'LAST_7_DAYS') {
      return 7;
    }

    return 30;
  }

  private distanceMeters(distanceKilometers: string): number | null {
    const normalizedDistance = distanceKilometers.trim().replace(',', '.');

    if (normalizedDistance === '') {
      return null;
    }

    const parsedDistance = Number(normalizedDistance);

    if (!Number.isFinite(parsedDistance)) {
      return null;
    }

    return parsedDistance * 1000;
  }

  private parsedBirthYear(): number | null {
    const trimmedBirthYear = this.birthYear().trim();

    if (trimmedBirthYear === '') {
      return null;
    }

    return Number(trimmedBirthYear);
  }

  private isValidBirthYear(birthYear: number | null): boolean {
    return (
      birthYear === null ||
      (Number.isInteger(birthYear) &&
        birthYear >= 1900 &&
        birthYear <= this.currentDate().getFullYear())
    );
  }

  private coachingProfileValidationMessage(): string | null {
    const distance = this.parsedTargetDistance();
    const pace = this.parsedTargetPaceSeconds();

    if (distance !== null && (!Number.isFinite(distance) || distance <= 0)) {
      return 'Distância da meta deve ser maior que zero. Exemplo: 5 km.';
    }

    if (pace !== null && (!Number.isFinite(pace) || pace <= 0)) {
      return 'Ritmo da meta deve usar minutos e segundos por quilômetro. Exemplo: 5:30.';
    }

    return null;
  }

  private fillCoachingProfileForm(profile: CoachingProfile): void {
    this.targetDistanceKilometers.set(profile.targetDistanceKilometers?.toString() ?? '');
    this.targetPace.set(this.paceInputValue(profile.targetPaceSecondsPerKilometer));
    this.targetDate.set(profile.targetDate ?? '');
    this.readiness.set(profile.readiness ?? '');
    this.injuryConcern.set(profile.injuryConcern);
    this.preferredRunningDays.set(profile.preferredRunningDays ?? []);
  }

  private parsedTargetDistance(): number | null {
    const normalizedDistance = this.targetDistanceKilometers().trim().replace(',', '.');

    if (normalizedDistance === '') {
      return null;
    }

    return Number(normalizedDistance);
  }

  private parsedTargetPaceSeconds(): number | null {
    const trimmedPace = this.targetPace().trim();

    if (trimmedPace === '') {
      return null;
    }

    const paceParts = trimmedPace.split(':');

    if (paceParts.length !== 2) {
      return Number(trimmedPace);
    }

    return Number(paceParts[0]) * 60 + Number(paceParts[1]);
  }

  private paceInputValue(seconds: number | null): string {
    if (seconds === null) {
      return '';
    }

    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = (seconds % 60).toString().padStart(2, '0');

    return `${minutes}:${remainingSeconds}`;
  }
}
