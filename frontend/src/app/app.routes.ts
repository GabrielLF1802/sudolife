import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';
import { LoginComponent } from './core/auth/login.component';
import { RegisterComponent } from './core/auth/register.component';
import { ActivityDashboardComponent } from './features/activity/components/activity-dashboard-page/activity-dashboard.component';
import { StravaCallbackResultComponent } from './features/activity/components/strava-callback-result/strava-callback-result.component';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
  },
  {
    path: 'register',
    component: RegisterComponent,
  },
  {
    path: 'activities',
    component: ActivityDashboardComponent,
    canActivate: [authGuard],
  },
  {
    path: 'strava/success',
    component: StravaCallbackResultComponent,
    canActivate: [authGuard],
  },
  {
    path: 'strava/failure',
    component: StravaCallbackResultComponent,
    canActivate: [authGuard],
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'activities',
  },
  {
    path: '**',
    redirectTo: 'activities',
  },
];
