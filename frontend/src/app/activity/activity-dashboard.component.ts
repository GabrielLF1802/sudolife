import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';

import { AuthService, CurrentUser } from '../auth/auth.service';
import { ActivityList, ActivityService } from './activity.service';

@Component({
  selector: 'app-activity-dashboard',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './activity-dashboard.component.html',
  styleUrl: './activity-dashboard.component.scss',
})
export class ActivityDashboardComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly activityService = inject(ActivityService);
  private readonly router = inject(Router);

  protected readonly currentUser = signal<CurrentUser | null>(null);
  protected readonly activityList = signal<ActivityList | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal('');

  ngOnInit(): void {
    forkJoin({
      currentUser: this.authService.currentUser(),
      activityList: this.activityService.list(),
    }).subscribe({
      next: ({ currentUser, activityList }) => {
        this.currentUser.set(currentUser);
        this.activityList.set(activityList);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Nao foi possivel carregar o painel.');
        this.loading.set(false);
      },
    });
  }

  protected logout(): void {
    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }
}
