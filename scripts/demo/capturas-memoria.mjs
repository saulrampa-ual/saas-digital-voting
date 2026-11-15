// Screenshots of the management console for the TFM report (C2), taken from the UAL demo data (synthetic, @ual.test).
// Requires the local stack and the demo loaded by seed-ual.mjs. Signs in as the demo ADMIN like a person would
// (password + e-mail code read from Mailpit) and saves PNG files under Plantilla-TFM-Informatica-UAL/Figuras/capturas.
//
//   $env:DEMO_PASSWORD = <password of the demo management accounts, the one used with seed-ual.mjs>
//   node scripts/demo/capturas-memoria.mjs
//
// Optional: CAPTURE_USER (default admin@ual.test), CAPTURE_LANG (es | en, default es).
import { mkdirSync } from 'node:fs';
import { join } from 'node:path';
import { APP, GW, ROOT, demoPassword, http, loadChromium, log, sixDigits, sleep, waitMail } from './lib/common.mjs';

const PASSWORD = demoPassword();
const USER = process.env.CAPTURE_USER ?? 'admin@ual.test';
const LANGUAGE = process.env.CAPTURE_LANG === 'en' ? 'en' : 'es';
// Files of another account carry its name (…-auditor-es.png), so they never replace the ones of the administrator
const TAG = USER === 'admin@ual.test' ? '' : `-${USER.split('@')[0]}`;
const OUT = join(ROOT, 'Plantilla-TFM-Informatica-UAL', 'Figuras', 'capturas');
mkdirSync(OUT, { recursive: true });

const browser = await loadChromium().launch();
try {
  const context = await browser.newContext({
    viewport: { width: 1440, height: 1024 }, deviceScaleFactor: 2, locale: LANGUAGE === 'en' ? 'en-GB' : 'es-ES',
  });
  const page = await context.newPage();
  let token = null; // access token of the console, only to look up the identifiers of the demo data
  page.on('request', (request) => {
    const value = request.headers()['authorization'];
    if (value?.startsWith('Bearer ') && request.url().includes('/api/')) token = value.slice(7);
  });

  // ---- login (Keycloak page + e-mail code) ----
  await page.goto(`${APP}/admin`);
  await page.waitForSelector('#username', { timeout: 30_000 });
  await page.fill('#username', USER);
  await page.fill('#password', PASSWORD);
  const since = Date.now();
  await page.click('#kc-login');
  const otp = page.locator('input[type="text"]:visible, input[type="number"]:visible, input[inputmode="numeric"]:visible, input[autocomplete="one-time-code"]:visible').first();
  await otp.waitFor({ timeout: 30_000 });
  await otp.fill(await waitMail(USER, since, (text) => sixDigits(text), 'login code'));
  const submit = page.locator('#kc-login:visible, input[type="submit"]:visible, button[type="submit"]:visible').first();
  if (await submit.count()) await submit.click(); else await otp.press('Enter');
  await page.waitForURL((url) => url.href.startsWith(`${APP}/admin`), { timeout: 30_000 });
  await page.waitForSelector('app-admin-page', { timeout: 30_000 });
  log(`logged in: ${USER}`);

  async function settle() {
    await page.waitForLoadState('networkidle').catch(() => {});
    await sleep(1200); // Ionic page transition and charts
  }
  async function shot(name) {
    await settle();
    if (page.url().includes('/sin-permiso')) { log(`skipped (no permission for ${USER}): ${name}`); return; }
    await page.screenshot({ path: join(OUT, `${name}${TAG}-${LANGUAGE}.png`) });
    log(`saved: ${name}${TAG}-${LANGUAGE}.png`);
  }
  async function open(path, name) {
    await page.goto(`${APP}${path}`);
    await page.waitForSelector('app-admin-page', { timeout: 30_000 }).catch(() => {});
    await shot(name);
  }

  // ---- home ----
  await shot('consola-inicio-primeros-pasos');
  const done = page.locator('app-admin-onboarding button.done');
  if (await done.count()) { await done.click(); await shot('consola-inicio'); }

  // ---- identifiers of the demo data ----
  for (let i = 0; i < 20 && !token; i++) await sleep(250);
  const byTitle = (list, text) => list.find((item) => (item.title?.es ?? item.name ?? '').includes(text));
  const processes = token ? (await http('GET', `${GW}/api/v1/processes`, { token })).data : [];
  const past = byTitle(processes, 'Elección pasada');
  const current = byTitle(processes, 'Elección en curso');
  const future = byTitle(processes, 'Elección futura');
  const censuses = token ? (await http('GET', `${GW}/api/v1/censuses`, { token, ok: [200, 403] })).data : [];
  const census = Array.isArray(censuses) ? byTitle(censuses, 'elección futura') ?? censuses[0] : null;

  // ---- processes ----
  await open('/admin/procesos', 'consola-procesos');
  await open('/admin/procesos/nuevo', 'consola-proceso-nuevo');
  if (future) await open(`/admin/procesos/${future.id}`, 'consola-proceso-programado');
  if (current) await open(`/admin/procesos/${current.id}`, 'consola-proceso-abierto');

  // ---- census ----
  await open('/admin/censos', 'consola-censos');
  if (census) await open(`/admin/censos/${census.id}`, 'consola-censo-detalle');

  // ---- participation, results, audit ----
  await open(current ? `/admin/participacion?proceso=${current.id}` : '/admin/participacion', 'consola-participacion');
  await open(past ? `/admin/resultados?proceso=${past.id}` : '/admin/resultados', 'consola-resultados');
  await open(past ? `/admin/auditoria?proceso=${past.id}` : '/admin/auditoria', 'consola-auditoria');
  await open('/admin/trazabilidad', 'consola-trazabilidad');

  // ---- public results of the past election (no session needed) ----
  if (past) {
    const { data } = await http('GET', `${GW}/api/v1/processes/${past.id}`, { token });
    if (data.resultsPublicId) {
      await page.goto(`${APP}/resultados/${data.resultsPublicId}`);
      await shot('resultados-publicos');
    }
  }
  log(`DONE · screenshots in ${OUT}`);
} finally {
  await browser.close();
}
