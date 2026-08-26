import { expect, test } from '@playwright/test';

const backendURL = 'http://localhost:8081';
const athlete = {
  name: 'Ana Smoke',
  email: 'ana.smoke@sudolife.test',
  password: 'SenhaForte123!',
};

test('real frontend and backend authenticate an athlete', async ({ page, request }) => {
  const readiness = await request.get(`${backendURL}/actuator/health/readiness`);
  expect(readiness.ok()).toBe(true);
  await expect(await readiness.json()).toEqual(expect.objectContaining({ status: 'UP' }));

  await page.goto('/register');
  await page.getByLabel('Nome').fill(athlete.name);
  await page.getByLabel('Email').fill(athlete.email);
  await page.getByLabel('Senha').fill(athlete.password);
  const registrationResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/api/users/register') && response.request().method() === 'POST',
  );
  await page.getByRole('button', { name: 'Criar conta' }).click();

  expect((await registrationResponse).status()).toBe(201);
  await expect(page.getByRole('heading', { name: 'Entrar' })).toBeVisible();

  await page.getByLabel('Email').fill(athlete.email);
  await page.getByLabel('Senha').fill(athlete.password);
  const loginResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/api/users/login') && response.request().method() === 'POST',
  );
  await page.getByRole('button', { name: 'Entrar' }).click();

  expect((await loginResponse).status()).toBe(200);
  await expect(page).toHaveURL(/\/activities$/);

  const currentUser = await page.evaluate(async () => {
    const token = localStorage.getItem('sudolife.jwt');
    const response = await fetch('/api/users/me', {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    return {
      status: response.status,
      body: await response.json(),
    };
  });

  expect(currentUser).toEqual({
    status: 200,
    body: expect.objectContaining({
      name: athlete.name,
      email: athlete.email,
    }),
  });
});
