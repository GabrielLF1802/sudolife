import { expect, test } from '@playwright/test';

import {
  loginAthlete,
  openDisclosure,
  registerAthlete,
  selectDashboardView,
} from './support/browser-journey-actions';
import { installBrowserJourneyApi } from './support/browser-journey-api';

test('athlete completes the MVP training journey', async ({ page }) => {
  await installBrowserJourneyApi(page);

  await registerAthlete(page);
  await expect(page.getByRole('heading', { name: 'Entrar' })).toBeVisible();

  await loginAthlete(page);
  await expect(page).toHaveURL(/\/activities$/);
  await expect(page.getByRole('heading', { name: 'Hoje' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Conecte o Strava' })).toBeVisible();

  await selectDashboardView(page, 'Ajustes');
  await openDisclosure(page, 'Não conectado');
  await page.getByLabel('Li e aceito o uso dos meus dados do Strava nesta versão.').check();
  await page.getByRole('button', { name: 'Conectar Strava' }).click();

  await expect(page.getByRole('heading', { name: 'Conexão concluída' })).toBeVisible();
  await expect(
    page.getByText('Sua conta Strava foi conectada. Agora você pode sincronizar suas atividades.'),
  ).toBeVisible();

  await page.getByRole('link', { name: 'Voltar ao painel de treino' }).click();
  await expect(page.getByRole('heading', { name: 'Hoje' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Complete seu perfil e objetivo' })).toBeVisible();

  await selectDashboardView(page, 'Atividades');
  await page.getByRole('button', { name: 'Sincronizar atividades' }).click();
  const importedActivity = page
    .getByRole('listitem')
    .filter({ hasText: 'Corrida · 24/08/2026 09:00' });
  await expect(importedActivity.getByText('Corrida regenerativa')).toBeVisible();
  await expect(importedActivity.getByText('5.2 km')).toBeVisible();

  await selectDashboardView(page, 'Ajustes');
  await openDisclosure(page, 'Informe seu ano de nascimento');
  await page.getByLabel('Ano de nascimento').fill('1992');
  await page.getByRole('button', { name: 'Salvar perfil' }).click();
  await expect(
    page.getByText('Perfil salvo. O ano de nascimento já pode orientar seus treinos.'),
  ).toBeVisible();

  await openDisclosure(page, 'Informe sua meta de corrida');
  await page.getByLabel('Distância alvo em quilômetros').fill('10');
  await page.getByLabel('Ritmo alvo por quilometro').fill('5:50');
  await page.getByLabel('Data alvo').fill('2026-10-25');
  await page.getByLabel('Prontidão informada').selectOption('MODERATE');
  await page.getByRole('checkbox', { name: 'Ter' }).check();
  await page.getByRole('checkbox', { name: 'Qui' }).check();
  await page.getByRole('checkbox', { name: 'Sáb' }).check();
  await page.getByRole('button', { name: 'Salvar contexto de treino' }).click();
  await expect(
    page.getByText('Meta e prontidão salvas. Seu plano será atualizado com essas informações.'),
  ).toBeVisible();

  await selectDashboardView(page, 'Plano');
  await expect(page.getByLabel('Plano de corrida adaptativo atual')).toBeVisible();
  await expect(page.getByText('Seu plano está em andamento')).toBeVisible();
  await expect(page.getByText('Corrida associada ·')).toBeVisible();
  await expect(page.getByText('Corrida regenerativa · 24/08/2026 · 5,2 km').first()).toBeVisible();

  const completedSession = page.getByLabel('Concluída: Corrida leve, 24/08/2026');
  await completedSession.getByLabel('Esforço percebido').fill('7');
  await completedSession.getByRole('button', { name: 'Salvar esforço' }).click();
  await expect(page.getByText('Esforço 7 de 10 registrado.')).toBeVisible();
  await expect(page.getByText('Esforço percebido registrado:')).toBeVisible();
  await expect(completedSession.getByText('7 de 10', { exact: true }).first()).toBeVisible();

  await page.getByRole('button', { name: 'Minha prontidão caiu' }).click();
  await expect(
    page.getByText(
      'Próxima sessão adaptada para Sessão de recuperação, considerando baixa prontidão.',
    ),
  ).toBeVisible();
  await expect(page.getByText('Agora Sessão de recuperação · 2.5 km')).toBeVisible();
  await expect(page.getByText('Esta sessão considera baixa prontidão.')).toBeVisible();
});
