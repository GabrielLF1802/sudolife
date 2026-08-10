import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { ACTIVITY_GATEWAY } from './activity/activity.gateway';
import { ActivityGatewayImpl } from './activity/activity.gateway.impl';
import { COACHING_PROFILE_GATEWAY } from './activity/coaching-profile.gateway';
import { CoachingProfileGatewayImpl } from './activity/coaching-profile.gateway.impl';
import { STRAVA_ACCOUNT_GATEWAY } from './activity/strava-account.gateway';
import { StravaAccountGatewayImpl } from './activity/strava-account.gateway.impl';
import { TRAINING_PROFILE_GATEWAY } from './activity/training-profile.gateway';
import { TrainingProfileGatewayImpl } from './activity/training-profile.gateway.impl';
import { authTokenInterceptor } from './auth/auth-token.interceptor';
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
