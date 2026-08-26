import type { Page, Route } from '@playwright/test';
import type { SaveBrowserJourneyCoachingProfileCommand } from './browser-journey-state';

import {
  adaptNextSessionForLowReadiness,
  browserJourneyAthlete,
  createBrowserJourneyState,
  currentUser,
  deterministicActivityDetail,
  recordPerceivedEffort,
} from './browser-journey-state';
import {
  activityListResponse,
  adaptiveRunningPlanResponse,
  coachingProfileResponse,
  conservativeRunningPlanResponse,
  currentAdaptiveRunningPlanResponse,
  ensureCurrentAdaptivePlan,
  runningGoalAssessmentResponse,
  runningHistoryResponse,
  stravaDataConsentResponse,
  stravaStatusResponse,
  stravaSyncResponse,
  trainingProfileResponse,
} from './browser-journey-responses';

export async function installBrowserJourneyApi(page: Page): Promise<void> {
  const state = createBrowserJourneyState();

  await page.route('**/api/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;
    const method = request.method();

    if (method === 'POST' && path === '/api/users/register') {
      const body = await request.postDataJSON();
      if (
        body.email !== browserJourneyAthlete.email ||
        body.password !== browserJourneyAthlete.password
      ) {
        await badRequest(route);
        return;
      }
      state.registered = true;
      await empty(route);
      return;
    }

    if (method === 'POST' && path === '/api/users/login') {
      const body = await request.postDataJSON();
      if (
        !state.registered ||
        body.email !== browserJourneyAthlete.email ||
        body.password !== browserJourneyAthlete.password
      ) {
        await unauthorized(route);
        return;
      }
      state.authenticated = true;
      await json(route, { token: browserJourneyAthlete.token });
      return;
    }

    if (method === 'GET' && path === '/api/users/me') {
      await json(route, currentUser());
      return;
    }

    if (method === 'GET' && path === '/api/strava/status') {
      await json(route, stravaStatusResponse(state));
      return;
    }

    if (method === 'GET' && path === '/api/strava/data-consent/status') {
      await json(route, stravaDataConsentResponse(state));
      return;
    }

    if (method === 'POST' && path === '/api/strava/link') {
      const body = await request.postDataJSON();
      if (body.acceptedStravaDataConsent !== true || body.language !== 'pt-BR') {
        await badRequest(route);
        return;
      }
      state.stravaLinked = true;
      state.stravaDataConsentValid = true;
      await json(route, { authorizationUrl: '/strava/success?outcome=success' });
      return;
    }

    if (method === 'DELETE' && path === '/api/strava/link') {
      state.stravaLinked = false;
      await empty(route);
      return;
    }

    if (method === 'POST' && path === '/api/strava/sync') {
      if (state.stravaLinked) {
        state.activitiesSynced = true;
      }
      await json(route, stravaSyncResponse(state));
      return;
    }

    if (method === 'GET' && path === '/api/strava/activities') {
      await json(route, activityListResponse(state, Number(url.searchParams.get('page') ?? 0)));
      return;
    }

    if (method === 'GET' && path === '/api/strava/activities/501') {
      await json(route, deterministicActivityDetail());
      return;
    }

    if (method === 'GET' && path === '/api/training-profile') {
      await json(route, trainingProfileResponse(state));
      return;
    }

    if (method === 'PUT' && path === '/api/training-profile') {
      const body = await request.postDataJSON();
      if (!Number.isInteger(body.birthYear)) {
        await badRequest(route);
        return;
      }
      state.trainingBirthYear = body.birthYear;
      await json(route, trainingProfileResponse(state));
      return;
    }

    if (method === 'GET' && path === '/api/coaching-profiles') {
      await json(route, coachingProfileResponse(state));
      return;
    }

    if (method === 'PUT' && path === '/api/coaching-profiles') {
      const body = (await request.postDataJSON()) as SaveBrowserJourneyCoachingProfileCommand;
      if (!isEssentialCoachingProfilePayload(body)) {
        await badRequest(route);
        return;
      }
      state.coachingProfile = { ...body, readiness: 'MODERATE', configured: true };
      state.plan = ensureCurrentAdaptivePlan(state);
      await json(route, coachingProfileResponse(state));
      return;
    }

    if (method === 'GET' && path === '/api/coaching-profiles/running-history') {
      await json(route, runningHistoryResponse(state));
      return;
    }

    if (method === 'GET' && path === '/api/coaching-profiles/running-goal-assessment') {
      await json(route, runningGoalAssessmentResponse());
      return;
    }

    if (method === 'POST' && path === '/api/coaching-profiles/running-plan') {
      await json(route, conservativeRunningPlanResponse());
      return;
    }

    if (method === 'POST' && path === '/api/coaching-profiles/adaptive-running-plan') {
      await json(route, adaptiveRunningPlanResponse(state));
      return;
    }

    if (method === 'GET' && path === '/api/coaching-profiles/adaptive-running-plan') {
      await json(route, currentAdaptiveRunningPlanResponse(state));
      return;
    }

    if (method === 'POST' && path === '/api/coaching-profiles/adaptive-running-plan/adapt') {
      const body = await request.postDataJSON();
      if (body.trigger !== 'LOW_READINESS') {
        await badRequest(route);
        return;
      }
      state.plan = adaptNextSessionForLowReadiness(ensureCurrentAdaptivePlan(state));
      await json(route, currentAdaptiveRunningPlanResponse(state));
      return;
    }

    const perceivedEffortSessionMatch = path.match(
      /^\/api\/coaching-profiles\/adaptive-running-plan\/sessions\/(\d+)\/perceived-effort$/,
    );

    if (method === 'PUT' && perceivedEffortSessionMatch) {
      const sessionId = Number(perceivedEffortSessionMatch[1]);
      const body = await request.postDataJSON();
      if (!Number.isInteger(body.perceivedEffort)) {
        await badRequest(route);
        return;
      }
      state.plan = recordPerceivedEffort(
        ensureCurrentAdaptivePlan(state),
        sessionId,
        body.perceivedEffort,
      );
      await json(route, currentAdaptiveRunningPlanResponse(state));
      return;
    }

    await notFound(route);
  });
}

async function json(route: Route, body: unknown, status = 200): Promise<void> {
  await route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  });
}

async function empty(route: Route): Promise<void> {
  await route.fulfill({ status: 204 });
}

async function badRequest(route: Route): Promise<void> {
  await json(route, { code: 'BROWSER_JOURNEY_BAD_REQUEST' }, 400);
}

async function unauthorized(route: Route): Promise<void> {
  await json(route, { code: 'BROWSER_JOURNEY_UNAUTHORIZED' }, 401);
}

async function notFound(route: Route): Promise<void> {
  await json(route, { code: 'BROWSER_JOURNEY_ROUTE_NOT_FOUND' }, 404);
}

function isEssentialCoachingProfilePayload(
  body: SaveBrowserJourneyCoachingProfileCommand,
): boolean {
  return (
    body.targetDistanceKilometers === 10 &&
    body.targetPaceSecondsPerKilometer === 350 &&
    body.targetDate === '2026-10-25' &&
    body.readiness === 'MODERATE' &&
    Array.isArray(body.preferredRunningDays) &&
    body.preferredRunningDays.includes('TUESDAY') &&
    body.preferredRunningDays.includes('THURSDAY') &&
    body.preferredRunningDays.includes('SATURDAY')
  );
}
