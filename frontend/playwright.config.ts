import { defineConfig, devices } from '@playwright/test';

const port = 4200;
const baseURL = `http://localhost:${port}`;

export default defineConfig({
  testDir: './browser-journey',
  fullyParallel: false,
  retries: process.env['CI'] ? 1 : 0,
  reporter: [['html']],
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
  webServer: {
    command: 'npm start',
    url: baseURL,
    reuseExistingServer: !process.env['CI'],
    timeout: 120_000,
  },
});
