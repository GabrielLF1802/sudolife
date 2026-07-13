import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin, finalize } from 'rxjs';

import { AuthService, CurrentUser } from '../auth/auth.service';
import { ActivityList, ActivityService } from './activity.service';
import {
  CoachingProfile,
  CoachingProfileService,
  ConservativeRunningPlan,
  PlannedSession,
  RunningHistorySnapshot,
  UserReportedReadiness,
} from './coaching-profile.service';
import {
  StravaAccountService,
  StravaActivitySyncFailureReason,
  StravaActivitySyncResult,
  StravaActivitySyncStatus,
  StravaLinkStatus,
} from './strava-account.service';
import { TrainingProfile, TrainingProfileService } from './training-profile.service';
import { WeeklyRhythmComponent } from './weekly-rhythm.component';

type ActivityPeriodFilter = 'ALL' | 'LAST_7_DAYS' | 'LAST_30_DAYS';
type DashboardView = 'TODAY' | 'PLAN' | 'ACTIVITIES';

@Component({
  selector: 'app-activity-dashboard',
  imports: [DatePipe, DecimalPipe, WeeklyRhythmComponent],
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
  protected readonly syncResult = signal<StravaActivitySyncResult | null>(null);
  protected readonly birthYear = signal('');
  protected readonly targetDistanceKilometers = signal('');
  protected readonly targetPace = signal('');
  protected readonly targetDate = signal('');
  protected readonly readiness = signal<UserReportedReadiness | ''>('');
  protected readonly injuryConcern = signal(false);
  protected readonly activeView = signal<DashboardView>('TODAY');
  protected readonly selectedActivityType = signal('ALL');
  protected readonly selectedPeriod = signal<ActivityPeriodFilter>('ALL');
  protected readonly minimumDistanceKilometers = signal('');
  protected readonly maximumDistanceKilometers = signal('');
  private readonly currentDate = signal(new Date());
  protected readonly futurePlanSessions = computed(
    () =>
      this.conservativeRunningPlan()?.plannedSessions.filter((session) => session.weekNumber > 1) ??
      [],
  );
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

  protected movingTimeLabel(seconds: number): string {
    const totalMinutes = Math.round(seconds / 60);
    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;

    if (hours > 0) {
      return `${hours}h ${minutes}min`;
    }

    return `${minutes} min`;
  }

  protected paceOrSpeedLabel(activity: ActivityList['activities'][number]): string {
    if (activity.averagePaceSecondsPerKilometer !== null) {
      const minutes = Math.floor(activity.averagePaceSecondsPerKilometer / 60);
      const seconds = Math.round(activity.averagePaceSecondsPerKilometer % 60)
        .toString()
        .padStart(2, '0');

      return `${minutes}:${seconds} /km`;
    }

    return `${(activity.averageSpeedMetersPerSecond * 3.6).toFixed(1)} km/h`;
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
        this.linkingErrorMessage.set('Nao foi possivel iniciar a conexao com o Strava.');
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
        },
        error: () => {
          this.syncErrorMessage.set('Nao foi possivel solicitar a sincronizacao.');
        },
      });
  }

  protected saveTrainingProfile(): void {
    this.savingTrainingProfile.set(true);
    this.trainingProfileErrorMessage.set('');
    this.trainingProfileSuccessMessage.set('');

    this.trainingProfileService
      .save({ birthYear: this.parsedBirthYear() })
      .pipe(finalize(() => this.savingTrainingProfile.set(false)))
      .subscribe({
        next: (profile) => {
          this.trainingProfile.set(profile);
          this.birthYear.set(profile.birthYear?.toString() ?? '');
          this.trainingProfileSuccessMessage.set('Perfil de treino salvo.');
        },
        error: () => {
          this.trainingProfileErrorMessage.set(
            'Não foi possível salvar. O ano informado foi preservado; revise o valor e tente novamente.',
          );
        },
      });
  }

  protected saveCoachingProfile(): void {
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
      })
      .pipe(finalize(() => this.savingCoachingProfile.set(false)))
      .subscribe({
        next: (profile) => {
          this.coachingProfile.set(profile);
          this.fillCoachingProfileForm(profile);
          this.coachingProfileSuccessMessage.set('Dados de coaching salvos.');
          this.conservativeRunningPlan.set(null);

          const runningHistory = this.runningHistory();
          if (runningHistory !== null) {
            this.loadConservativeRunningPlan(profile, runningHistory);
          }
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

    return 'Nao informada';
  }

  protected plannedSessionTypeLabel(session: PlannedSession): string {
    return session.type === 'EASY_RUN' ? 'Corrida leve' : 'Corrida longa';
  }

  protected plannedSessionTargetLabel(session: PlannedSession): string {
    if (session.target.type === 'HEART_RATE') {
      return `${session.target.minimumHeartRate}-${session.target.maximumHeartRate} bpm`;
    }

    return `Esforco percebido ${session.target.minimumPerceivedEffort}-${session.target.maximumPerceivedEffort}`;
  }

  protected stravaActionLabel(status: StravaLinkStatus): string {
    if (status.permissionState === 'PERMISSION_UPGRADE_REQUIRED') {
      return 'Atualizar permissoes';
    }

    if (status.linked) {
      return 'Reconectar Strava';
    }

    return 'Conectar Strava';
  }

  protected stravaStatusText(status: StravaLinkStatus): string {
    if (status.permissionState === 'PERMISSION_UPGRADE_REQUIRED') {
      return 'Permissoes incompletas';
    }

    if (status.linked) {
      return `Conectado ao atleta ${status.athleteId}`;
    }

    return 'Nao conectado';
  }

  protected profileZoneStatusText(
    profile: TrainingProfile,
    status: StravaLinkStatus | null,
  ): string {
    if (profile.heartRateZoneSource === 'STRAVA') {
      return 'Zonas de frequencia cardiaca importadas do Strava.';
    }

    if (profile.heartRateZoneSource === 'AGE_BASED') {
      return 'Zonas calculadas pelo ano de nascimento.';
    }

    if (status?.profilePermissionState === 'OPTIONAL_UPGRADE_AVAILABLE') {
      return 'Zonas do Strava sao opcionais; atualize a conexao para tentar importar.';
    }

    return 'Zonas do Strava sao opcionais e nao bloqueiam o coaching.';
  }

  protected syncStatusLabel(status: StravaActivitySyncStatus): string {
    if (status === 'COMPLETED') {
      return 'Sincronizacao solicitada';
    }

    if (status === 'UNLINKED') {
      return 'Strava nao conectado';
    }

    return 'Sincronizacao nao solicitada';
  }

  protected syncFailureReasonLabel(failureReason: StravaActivitySyncFailureReason): string {
    if (failureReason === 'PERMISSION_UPGRADE_REQUIRED') {
      return 'Atualize as permissoes do Strava para importar atividades.';
    }

    if (failureReason === 'SYNC_ALREADY_RUNNING') {
      return 'Ja existe uma sincronizacao em andamento.';
    }

    if (failureReason === 'STRAVA_RATE_LIMITED') {
      return 'O Strava limitou novas sincronizacoes no momento.';
    }

    return 'O Strava esta indisponivel no momento.';
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
      !coachingProfile.injuryConcern &&
      (!runningHistory.sufficientRunningHistory || coachingProfile.readiness === 'LOW');

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

  protected retryConservativeRunningPlan(): void {
    const profile = this.coachingProfile();
    const history = this.runningHistory();

    if (profile !== null && history !== null) {
      this.loadConservativeRunningPlan(profile, history);
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

  private fillCoachingProfileForm(profile: CoachingProfile): void {
    this.targetDistanceKilometers.set(profile.targetDistanceKilometers?.toString() ?? '');
    this.targetPace.set(this.paceInputValue(profile.targetPaceSecondsPerKilometer));
    this.targetDate.set(profile.targetDate ?? '');
    this.readiness.set(profile.readiness ?? '');
    this.injuryConcern.set(profile.injuryConcern);
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
