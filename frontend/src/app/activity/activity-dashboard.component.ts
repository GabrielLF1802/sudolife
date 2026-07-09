import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin, finalize } from 'rxjs';

import { AuthService, CurrentUser } from '../auth/auth.service';
import { ActivityList, ActivityService } from './activity.service';
import {
  StravaAccountService,
  StravaActivitySyncFailureReason,
  StravaActivitySyncResult,
  StravaActivitySyncStatus,
  StravaLinkStatus,
} from './strava-account.service';
import { TrainingProfile, TrainingProfileService } from './training-profile.service';

type ActivityPeriodFilter = 'ALL' | 'LAST_7_DAYS' | 'LAST_30_DAYS';

@Component({
  selector: 'app-activity-dashboard',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './activity-dashboard.component.html',
  styleUrl: './activity-dashboard.component.scss',
})
export class ActivityDashboardComponent implements OnInit {

  private readonly authService = inject(AuthService);
  private readonly activityService = inject(ActivityService);
  private readonly stravaAccountService = inject(StravaAccountService);
  private readonly trainingProfileService = inject(TrainingProfileService);
  private readonly router = inject(Router);

  protected readonly currentUser = signal<CurrentUser | null>(null);
  protected readonly activityList = signal<ActivityList | null>(null);
  protected readonly stravaLinkStatus = signal<StravaLinkStatus | null>(null);
  protected readonly trainingProfile = signal<TrainingProfile | null>(null);
  protected readonly loading = signal(true);
  protected readonly pageLoading = signal(false);
  protected readonly linking = signal(false);
  protected readonly syncing = signal(false);
  protected readonly savingTrainingProfile = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly linkingErrorMessage = signal('');
  protected readonly syncErrorMessage = signal('');
  protected readonly trainingProfileErrorMessage = signal('');
  protected readonly trainingProfileSuccessMessage = signal('');
  protected readonly syncResult = signal<StravaActivitySyncResult | null>(null);
  protected readonly birthYear = signal('');
  protected readonly selectedActivityType = signal('ALL');
  protected readonly selectedPeriod = signal<ActivityPeriodFilter>('ALL');
  protected readonly minimumDistanceKilometers = signal('');
  protected readonly maximumDistanceKilometers = signal('');
  private readonly currentDate = signal(new Date());
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
    forkJoin({
      currentUser: this.authService.currentUser(),
      activityList: this.activityService.list(),
      stravaLinkStatus: this.stravaAccountService.status(),
      trainingProfile: this.trainingProfileService.get(),
    }).subscribe({
      next: ({ currentUser, activityList, stravaLinkStatus, trainingProfile }) => {
        this.currentUser.set(currentUser);
        this.activityList.set(activityList);
        this.stravaLinkStatus.set(stravaLinkStatus);
        this.trainingProfile.set(trainingProfile);
        this.birthYear.set(trainingProfile.birthYear?.toString() ?? '');
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Nao foi possivel carregar o painel.');
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

  protected clearActivityFilters(): void {
    this.selectedActivityType.set('ALL');
    this.selectedPeriod.set('ALL');
    this.minimumDistanceKilometers.set('');
    this.maximumDistanceKilometers.set('');
  }

  protected loadPage(page: number): void {
    this.pageLoading.set(true);
    this.errorMessage.set('');

    this.activityService
      .list(page)
      .pipe(finalize(() => this.pageLoading.set(false)))
      .subscribe({
        next: (activityList) => {
          this.activityList.set(activityList);
        },
        error: () => {
          this.errorMessage.set('Nao foi possivel carregar as atividades.');
        },
      });
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
          this.trainingProfileErrorMessage.set('Informe um ano de nascimento valido.');
        },
      });
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
}
