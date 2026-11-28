// Screenshots of the voter flow for the TFM report (C2, 24/11/2026), taken from the UAL demo data (synthetic, @ual.test).
// Requires the local stack and the demo loaded by seed-ual.mjs. Goes through the flow like a voter of the open process
// («Elección en curso»): personal link, e-mail code (read from Mailpit), ballot, review, receipt and receipt check.
// IT CASTS A REAL VOTE with that voter (the first option of the ballot), so a voter used here can no longer vote.
//
//   node scripts/demo/capturas-votante.mjs
//
// Optional: CAPTURE_VOTER (default presente.votante5@ual.test), CAPTURE_LANG (es | en, default es) and CAPTURE_LINK
// (the 64-character token of the voter's personal link, when Mailpit no longer keeps the invitation; it is in
// .local/demo-ual.md).
// Images: Plantilla-TFM-Informatica-UAL/Figuras/capturas/votante-<step>-<lang>.png (mobile viewport, 390 × 844).
// Vote secrecy: the images show only the synthetic voter's own screens; the receipt code is the demo's, not a person's.
import { mkdirSync } from 'node:fs';
import { join } from 'node:path';
import { APP, ROOT, inviteLink, loadChromium, log, sixDigits, sleep, waitMail } from './lib/common.mjs';

const VOTER = process.env.CAPTURE_VOTER ?? 'presente.votante5@ual.test';
const LANGUAGE = process.env.CAPTURE_LANG === 'en' ? 'en' : 'es';
const OUT = join(ROOT, 'Plantilla-TFM-Informatica-UAL', 'Figuras', 'capturas');
mkdirSync(OUT, { recursive: true });

const link = process.env.CAPTURE_LINK ?? await inviteLink(VOTER);
const browser = await loadChromium().launch();
try {
  const context = await browser.newContext({
    viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true,
    locale: LANGUAGE === 'en' ? 'en-GB' : 'es-ES',
  });
  const page = await context.newPage();
  async function shot(name) {
    await page.waitForLoadState('networkidle').catch(() => {});
    await sleep(900); // Ionic transitions and focus moves
    await page.screenshot({ path: join(OUT, `votante-${name}-${LANGUAGE}.png`) });
    log(`saved: votante-${name}-${LANGUAGE}.png`);
  }

  // ---- welcome ----
  await page.goto(`${APP}/votar/${link}`);
  await page.locator('main h1, h1').first().waitFor({ timeout: 30_000 });
  const language = page.getByRole('button', { name: new RegExp(`^${LANGUAGE.toUpperCase()}\\b`) }).first();
  if (await language.count()) await language.click();
  await shot('bienvenida');

  // ---- e-mail code ----
  const since = Date.now();
  await page.getByRole('button', { name: /empezar|start/i }).click();
  await page.locator('ion-input-otp').waitFor({ timeout: 30_000 });
  await shot('codigo');
  const code = await waitMail(VOTER, since, (t) => !t.includes('/votar/') && sixDigits(t), 'voter code');
  await page.locator('ion-input-otp input').first().click();
  await page.keyboard.type(code);

  // ---- ballot, review, receipt ----
  const ballot = page.locator('ion-radio-group');
  const already = page.locator('app-error-state');
  await Promise.race([ballot.waitFor({ timeout: 30_000 }), already.waitFor({ timeout: 30_000 })]);
  if (await already.isVisible()) {
    await shot('ya-voto');
    throw new Error(`${VOTER} has already voted: use another voter (CAPTURE_VOTER) or a new process`);
  }
  await page.locator('ion-radio').first().click();
  await shot('papeleta');
  await page.getByRole('button', { name: /revisar|review/i }).click();
  await page.locator('app-review-step').waitFor();
  await shot('revision');
  await page.locator('app-review-step ion-button').last().click();
  await page.locator('app-receipt-step').waitFor({ timeout: 30_000 });
  await shot('recibo');
  const receipt = (await page.locator('app-receipt-step p.code').textContent())?.trim() ?? '';

  // ---- receipt check (RF-10) ----
  await page.locator('app-receipt-step ion-button').last().click();
  const field = page.getByRole('textbox');
  await field.waitFor({ timeout: 30_000 });
  if (!(await field.inputValue())) { await field.click(); await page.keyboard.type(receipt); }
  await page.getByRole('button', { name: /^(comprobar|check)$/i }).click();
  await page.locator('p.result').waitFor({ timeout: 30_000 });
  await shot('comprobacion');
  log(`DONE · voter flow screenshots in ${OUT}`);
} finally {
  await browser.close();
}
