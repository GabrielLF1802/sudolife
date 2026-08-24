import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';
import { LoginComponent } from './core/auth/login.component';
import { PasswordRecoveryCompleteComponent } from './core/auth/password-recovery-complete.component';
import { PasswordRecoveryRequestComponent } from './core/auth/password-recovery-request.component';
import { RegisterComponent } from './core/auth/register.component';
import { PrivacyPolicyComponent } from './core/privacy/privacy-policy.component';
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
    path: 'password-recovery',
    component: PasswordRecoveryRequestComponent,
  },
  {
    path: 'password-recovery/complete',
    component: PasswordRecoveryCompleteComponent,
  },
  {
    path: 'privacy-policy',
    component: PrivacyPolicyComponent,
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
