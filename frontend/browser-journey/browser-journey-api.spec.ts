import { expect, test } from '@playwright/test';

import { browserJourneyAthlete } from './support/browser-journey-state';
import { installBrowserJourneyApi } from './support/browser-journey-api';

test('serves_the_deterministic_browser_journey_api', async ({ page }) => {
  await installBrowserJourneyApi(page);
  await page.goto('/login');

  const initialState = await getInitialState(page);

  expect(initialState.stravaStatus.linked).toBe(false);
  expect(initialState.consent.valid).toBe(false);
  expect(initialState.activities.totalElements).toBe(0);
  expect(initialState.trainingProfile.adaptiveCoachingEligible).toBe(false);
  expect(initialState.coachingProfile.configured).toBe(false);
  expect(initialState.runningHistory.sufficientRunningHistory).toBe(false);

  await post(page, '/api/users/register', {
    name: browserJourneyAthlete.name,
    email: browserJourneyAthlete.email,
    password: browserJourneyAthlete.password,
  });

  const authResult = await post(page, '/api/users/login', {
    email: browserJourneyAthlete.email,
    password: browserJourneyAthlete.password,
  });
  const currentUser = await get(page, '/api/users/me');

  expect(authResult.token).toBe(browserJourneyAthlete.token);
  expect(currentUser.email).toBe(browserJourneyAthlete.email);

  const linkResult = await post(page, '/api/strava/link', {
    acceptedStravaDataConsent: true,
    language: 'pt-BR',
  });
  const syncResult = await post(page, '/api/strava/sync', {});
  const syncedActivities = await get(page, '/api/strava/activities?page=0&size=10');

  expect(linkResult.authorizationUrl).toBe('/strava/success?outcome=success');
  expect(syncResult.importedActivityCount).toBe(1);
  expect(syncedActivities.activities[0].name).toBe('Corrida regenerativa');

  const trainingProfile = await put(page, '/api/training-profile', { birthYear: 1992 });
  const coachingProfile = await put(page, '/api/coaching-profiles', {
    targetDistanceKilometers: 10,
    targetPaceSecondsPerKilometer: 350,
    targetDate: '2026-10-25',
    readiness: 'MODERATE',
    injuryConcern: false,
    preferredRunningDays: ['TUESDAY', 'THURSDAY', 'SATURDAY'],
  });
  const runningHistory = await get(page, '/api/coaching-profiles/running-history');
  const plan = await get(page, '/api/coaching-profiles/adaptive-running-plan');

  expect(trainingProfile.adaptiveCoachingEligible).toBe(true);
  expect(coachingProfile.configured).toBe(true);
  expect(runningHistory.sufficientRunningHistory).toBe(true);
  expect(plan.plannedSessions).toEqual(
    expect.arrayContaining([
      expect.objectContaining({
        id: 701,
        status: 'COMPLETED',
        matchedActivityId: 501,
        postSessionPerceivedEffort: null,
      }),
      expect.objectContaining({ id: 702, status: 'PLANNED' }),
    ]),
  );

  const effortPlan = await put(
    page,
    '/api/coaching-profiles/adaptive-running-plan/sessions/701/perceived-effort',
    { perceivedEffort: 7 },
  );
  const adaptedPlan = await post(page, '/api/coaching-profiles/adaptive-running-plan/adapt', {
    trigger: 'LOW_READINESS',
  });

  expect(effortPlan.plannedSessions[0].postSessionPerceivedEffort).toBe(7);
  expect(adaptedPlan.plannedSessions).toEqual(
    expect.arrayContaining([
      expect.objectContaining({ id: 702, status: 'REPLACED', adaptationTrigger: 'LOW_READINESS' }),
      expect.objectContaining({ id: 703, status: 'PLANNED', adaptationTrigger: 'LOW_READINESS' }),
    ]),
  );
});

async function getInitialState(page: import('@playwright/test').Page) {
  return {
    stravaStatus: await get(page, '/api/strava/status'),
    consent: await get(page, '/api/strava/data-consent/status'),
    activities: await get(page, '/api/strava/activities?page=0&size=10'),
    trainingProfile: await get(page, '/api/training-profile'),
    coachingProfile: await get(page, '/api/coaching-profiles'),
    runningHistory: await get(page, '/api/coaching-profiles/running-history'),
  };
}

async function get(page: import('@playwright/test').Page, path: string): Promise<any> {
  return request(page, path, { method: 'GET' });
}

async function post(
  page: import('@playwright/test').Page,
  path: string,
  body: unknown,
): Promise<any> {
  return request(page, path, { method: 'POST', body: JSON.stringify(body) });
}

async function put(
  page: import('@playwright/test').Page,
  path: string,
  body: unknown,
): Promise<any> {
  return request(page, path, { method: 'PUT', body: JSON.stringify(body) });
}

async function request(
  page: import('@playwright/test').Page,
  path: string,
  init: RequestInit,
): Promise<any> {
  return page.evaluate(
    async ({ path, init }) => {
      const response = await fetch(path, {
        ...init,
        headers: { 'Content-Type': 'application/json' },
      });
      const text = await response.text();

      if (!response.ok) {
        throw new Error(text || `Request failed with status ${response.status}`);
      }

      return text ? JSON.parse(text) : null;
    },
    { path, init },
  );
}
