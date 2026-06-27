import { Routes } from '@angular/router';

import { ActivityDashboardComponent } from './activity/activity-dashboard.component';
import { StravaCallbackResultComponent } from './activity/strava-callback-result.component';
import { authGuard } from './auth/auth.guard';
import { LoginComponent } from './auth/login.component';
import { RegisterComponent } from './auth/register.component';

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
