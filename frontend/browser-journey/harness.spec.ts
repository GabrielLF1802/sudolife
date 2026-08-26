import { expect, test } from '@playwright/test';

test('redirects_unauthenticated_athlete_to_login', async ({ page }) => {
  await page.goto('/');

  await expect(page.getByRole('heading', { name: 'Entrar' })).toBeVisible();
});
