import type { Page } from '@playwright/test';

import { browserJourneyAthlete } from './browser-journey-state';

export async function registerAthlete(page: Page): Promise<void> {
  await page.goto('/register');
  await page.getByLabel('Nome').fill(browserJourneyAthlete.name);
  await page.getByLabel('Email').fill(browserJourneyAthlete.email);
  await page.getByLabel('Senha').fill(browserJourneyAthlete.password);
  await page.getByRole('button', { name: 'Criar conta' }).click();
}

export async function loginAthlete(page: Page): Promise<void> {
  await page.getByLabel('Email').fill(browserJourneyAthlete.email);
  await page.getByLabel('Senha').fill(browserJourneyAthlete.password);
  await page.getByRole('button', { name: 'Entrar' }).click();
}

export async function selectDashboardView(page: Page, name: string): Promise<void> {
  await page
    .getByRole('navigation', { name: 'Áreas do painel' })
    .getByRole('button', { name })
    .click();
}

export async function openDisclosure(page: Page, text: string): Promise<void> {
  await page.getByText(text, { exact: true }).click();
}
