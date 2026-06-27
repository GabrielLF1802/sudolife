import { Routes } from '@angular/router';

import { ActivityDashboardComponent } from './activity/activity-dashboard.component';
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
    path: '',
    pathMatch: 'full',
    redirectTo: 'activities',
  },
  {
    path: '**',
    redirectTo: 'activities',
  },
];
