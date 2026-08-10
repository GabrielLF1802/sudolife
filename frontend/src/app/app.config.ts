import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { authTokenInterceptor } from './core/auth/auth-token.interceptor';
import { ACTIVITY_GATEWAY } from './features/activity/services/activity.gateway';
import { ActivityGatewayImpl } from './features/activity/services/activity.gateway.impl';
import { COACHING_PROFILE_GATEWAY } from './features/activity/services/coaching-profile.gateway';
import { CoachingProfileGatewayImpl } from './features/activity/services/coaching-profile.gateway.impl';
import { STRAVA_ACCOUNT_GATEWAY } from './features/activity/services/strava-account.gateway';
import { StravaAccountGatewayImpl } from './features/activity/services/strava-account.gateway.impl';
import { TRAINING_PROFILE_GATEWAY } from './features/activity/services/training-profile.gateway';
import { TrainingProfileGatewayImpl } from './features/activity/services/training-profile.gateway.impl';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideHttpClient(withInterceptors([authTokenInterceptor])),
    provideRouter(routes),
    { provide: ACTIVITY_GATEWAY, useClass: ActivityGatewayImpl },
    { provide: STRAVA_ACCOUNT_GATEWAY, useClass: StravaAccountGatewayImpl },
    { provide: TRAINING_PROFILE_GATEWAY, useClass: TrainingProfileGatewayImpl },
    { provide: COACHING_PROFILE_GATEWAY, useClass: CoachingProfileGatewayImpl },
  ],
};
