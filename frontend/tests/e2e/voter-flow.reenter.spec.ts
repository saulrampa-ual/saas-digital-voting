import { expect } from '@playwright/test';
import { test, type MockReply } from './support/api-mock';

/**
 * IMP-72 (M1, 24/11/2026): after «Volver a entrar» on the expired-session screen, the tab once showed the welcome
 * screen of another voter's link opened earlier in the same tab. `enterAgain()` does not navigate, so this checks
 * that the voter stays on their own link and goes back to the code step, also when another link was opened before
 * in the same tab. Synthetic API only (no real link, code or identity).
 */
const LINK = 'R'.repeat(64);
const OTHER = 'O'.repeat(64);
const future = (days: number) => new Date(Date.now() + days * 86_400_000).toISOString();
const past = (days: number) => new Date(Date.now() - days * 86_400_000).toISOString();

const json = (status: number, body: unknown): MockReply => ({
  status, contentType: status >= 400 ? 'application/problem+json' : 'application/json', body: JSON.stringify(body),
});
const link = {
  processId: 4, title: { es: 'Votación de prueba', en: 'Test vote' }, status: 'OPEN', opensAt: past(1),
  closesAt: future(6), defaultLanguage: 'es', authentication: 'EMAIL_CODE',
};
const codeSent = { maskedEmail: 'p0•@verivoto.test', expiresAt: future(0.007), resendAvailableAt: future(0.0007) };
const granted = { hasVoted: false, votedOn: null, votingToken: 'synthetic-voting-token', votingTokenExpiresAt: future(0.01) };

test('IMP-72: «Volver a entrar» keeps the same link and returns to the code step', async ({ page, mockApi }) => {
  const requested: string[] = [];
  await mockApi(page, (method, url) => {
    const path = url.pathname;
    const token = path.match(/\/links\/([^/]+)/)?.[1];
    if (token) requested.push(token);
    if (path.endsWith('/otp/verify')) return json(200, granted);
    if (path.endsWith('/otp') && method === 'POST') return json(202, codeSent);
    if (/\/voter-access\/links\/[^/]+$/.test(path)) return json(200, link);
    // The voting token has expired when the ballot is asked for: «Tu sesión ha caducado» (V6)
    if (path.endsWith('/ballot')) return json(410, { type: 'about:blank', title: 'Synthetic', status: 410 });
    return json(404, {});
  });

  // Another voter's link first, in the same tab, as in the manual review of 24/11/2026
  await page.goto(`/votar/${OTHER}`);
  await page.getByRole('button', { name: /^ES\b/ }).first().click();
  await page.getByRole('button', { name: /empezar/i }).waitFor();

  await page.goto(`/votar/${LINK}`);
  await page.getByRole('button', { name: /empezar/i }).click();
  await page.locator('ion-input-otp input').first().click();
  await page.keyboard.type('123456');
  await expect(page.getByRole('heading', { level: 1 })).toHaveText(/sesión ha caducado/i);

  requested.length = 0;
  await page.getByRole('button', { name: /volver a entrar/i }).press('Enter');

  // Back to the code step of the same link (the code is asked for again with «Enviar código»)
  await expect(page.getByRole('heading', { level: 1 })).toHaveText(/código de acceso/i);
  await expect(page.locator('app-access-code-step')).toBeVisible();
  expect(new URL(page.url()).pathname).toBe(`/votar/${LINK}`);
  expect(requested.every((token) => token === LINK), 'requests made with another link').toBe(true);
});
