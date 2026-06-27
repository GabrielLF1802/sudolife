import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin, finalize } from 'rxjs';

import { AuthService, CurrentUser } from '../auth/auth.service';
import { ActivityList, ActivityService } from './activity.service';
import { StravaAccountService, StravaLinkStatus } from './strava-account.service';

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
  private readonly router = inject(Router);

  protected readonly currentUser = signal<CurrentUser | null>(null);
  protected readonly activityList = signal<ActivityList | null>(null);
  protected readonly stravaLinkStatus = signal<StravaLinkStatus | null>(null);
  protected readonly loading = signal(true);
  protected readonly pageLoading = signal(false);
  protected readonly linking = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly linkingErrorMessage = signal('');

  ngOnInit(): void {
    forkJoin({
      currentUser: this.authService.currentUser(),
      activityList: this.activityService.list(),
      stravaLinkStatus: this.stravaAccountService.status(),
    }).subscribe({
      next: ({ currentUser, activityList, stravaLinkStatus }) => {
        this.currentUser.set(currentUser);
        this.activityList.set(activityList);
        this.stravaLinkStatus.set(stravaLinkStatus);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Nao foi possivel carregar o painel.');
        this.loading.set(false);
      },
    });
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
}
