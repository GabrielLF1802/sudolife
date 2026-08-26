import { defineConfig, devices } from '@playwright/test';

const frontendPort = 4200;
const backendPort = 8081;
const baseURL = `http://localhost:${frontendPort}`;
const backendURL = `http://localhost:${backendPort}`;

export default defineConfig({
  testDir: './integrated-smoke',
  fullyParallel: false,
  retries: process.env['CI'] ? 1 : 0,
  reporter: [['html', { outputFolder: 'playwright-report/integrated-smoke' }]],
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      name: 'backend',
      command: `./mvnw spring-boot:run -Dspring-boot.run.profiles=test -Dspring-boot.run.arguments=--server.port=${backendPort}`,
      cwd: '..',
      url: `${backendURL}/actuator/health/readiness`,
      reuseExistingServer: false,
      timeout: 180_000,
      env: {
        API_SECURITY_TOKEN_SECRET: 'test-only-token-secret-with-at-least-thirty-two-characters',
        STRAVA_CLIENT_SECRET: 'test-only-strava-client-secret',
        MISSED_SESSION_SCHEDULING_ENABLED: 'false',
        STRAVA_SUMMARY_SYNC_SCHEDULING_ENABLED: 'false',
        STRAVA_STREAM_SYNC_SCHEDULING_ENABLED: 'false',
      },
    },
    {
      name: 'frontend',
      command: `npm start -- --port ${frontendPort}`,
      url: baseURL,
      reuseExistingServer: false,
      timeout: 120_000,
    },
  ],
});
