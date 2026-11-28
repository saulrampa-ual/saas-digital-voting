// Timed check of RNF-8 (25/11/2026): «every action that takes more than 1 second shows a progress indicator» (1 s is
// the proposed value). For each action that calls the server, it measures in the browser:
//   feedback_ms: from the click (or the last key) to the first visible change of the page (text, spinner, disabled
//                button, new screen), with a MutationObserver started just before the action;
//   total_ms:    from the click to the final state of the action (next screen, result, download).
// An action cumple if total_ms <= 1000, or if it took longer but feedback_ms <= 1000. The first change is recorded
// (cambio) so a person can check that it is visible and not an incidental change of the page.
//
// Requires the local stack and the UAL demo. The voter part CASTS A REAL VOTE with a voter that has not voted yet:
//   $env:RNF8_VOTER = 'presente.votante3@ual.test'   # a voter of the open election that has not voted
//   $env:RNF8_LINK  = '<64-character token of its link>' # optional, when Mailpit no longer keeps the invitation
//   $env:DEMO_PASSWORD = '<demo password>'           # console part (admin@ual.test, auditor@ual.test); without it, skipped
//   node scripts/demo/medir-rnf8.mjs
// Optional: RNF8_PROCESS_TITLE (text in the past election's title, default «Elección pasada»; with several, the newest
// tallied one is used), RNF8_RUNS (default 3,
// console actions only: the voter actions can be done once per voter).
// Output: docs/evaluation/revision-heuristica/rnf8-cronometraje.csv (appends rows) and a summary in the console.
// Vote secrecy: it records only times and the kind of change, never codes, links, receipts or choices.
import { appendFileSync, existsSync, mkdirSync } from 'node:fs';
import { join } from 'node:path';
import { APP, GW, ROOT, http, inviteLink, loadChromium, log, sixDigits, sleep, waitMail } from './lib/common.mjs';

const OUT_DIR = join(ROOT, 'docs', 'evaluation', 'revision-heuristica');
const OUT = join(OUT_DIR, 'rnf8-cronometraje.csv');
const RUNS = Number(process.env.RNF8_RUNS ?? 3);
const VOTER = process.env.RNF8_VOTER;
const PAST = process.env.RNF8_PROCESS_TITLE ?? 'Elección pasada';
const DATE = new Date().toISOString().slice(0, 16).replace('T', ' ');
const rows = [];
const q = (v) => `"${String(v).replace(/"/g, '""')}"`;
/** Each row is written as soon as it is measured, so an error later does not lose it (25/11/2026). */
function save(r) {
  mkdirSync(OUT_DIR, { recursive: true });
  if (!existsSync(OUT)) appendFileSync(OUT, 'fecha,accion,realimentacion_ms,total_ms,primer_cambio,decision\n', 'utf8');
  appendFileSync(OUT, [DATE, q(r.name), r.feedback ?? '', r.total, q(r.change), r.ok ? 'cumple' : 'no cumple'].join(',') + '\n', 'utf8');
}

/** Starts watching the page; the action runs; returns {feedback, total, change}. */
async function timed(page, name, act, done) {
  await page.evaluate(() => {
    const w = window;
    w.__rnf8 = { t0: null, fb: null, change: null };
    const mark = () => { if (w.__rnf8.t0 === null) w.__rnf8.t0 = performance.now(); };
    w.__rnf8Mark = mark;
    document.addEventListener('click', mark, { capture: true, once: true });
    const describe = (m) => m.type === 'attributes' ? `${m.attributeName} en ${m.target.nodeName.toLowerCase()}`
      : m.type === 'characterData' ? 'texto' : `${m.addedNodes[0]?.nodeName?.toLowerCase() ?? 'nodo'} ${m.addedNodes.length ? 'añadido' : 'quitado'}`;
    w.__rnf8Obs?.disconnect();
    w.__rnf8Obs = new MutationObserver((list) => {
      if (w.__rnf8.t0 === null || w.__rnf8.fb !== null) return;
      w.__rnf8.fb = performance.now(); w.__rnf8.change = describe(list[0]);
    });
    w.__rnf8Obs.observe(document.body, { subtree: true, childList: true, characterData: true, attributes: true,
      attributeFilter: ['disabled', 'aria-busy', 'aria-disabled', 'hidden'] });
  });
  const start = Date.now();
  await act();
  await done();
  const total = Date.now() - start;
  const r = await page.evaluate(() => ({ ...window.__rnf8 }));
  const feedback = r.t0 !== null && r.fb !== null ? Math.round(r.fb - r.t0) : null;
  const ok = total <= 1000 || (feedback !== null && feedback <= 1000);
  rows.push({ name, feedback, total, change: r.change ?? '—', ok });
  save(rows.at(-1));
  log(`${name}: feedback ${feedback ?? '—'} ms, total ${total} ms → ${ok ? 'cumple' : 'NO cumple'}`);
}

const browser = await loadChromium().launch();
try {
  // ---------- voter ----------
  if (VOTER) {
    const link = process.env.RNF8_LINK ?? await inviteLink(VOTER);
    const page = await (await browser.newContext({ viewport: { width: 390, height: 844 }, locale: 'es-ES' })).newPage();
    await page.goto(`${APP}/votar/${link}`);
    await page.getByRole('button', { name: /empezar/i }).waitFor({ timeout: 30_000 });
    let since = Date.now();
    await timed(page, 'Votante: pedir el código («Empezar»)',
      () => page.getByRole('button', { name: /empezar/i }).click(), () => page.locator('ion-input-otp').waitFor());
    const code = await waitMail(VOTER, since, (t) => !t.includes('/votar/') && sixDigits(t), 'voter code');
    await page.locator('ion-input-otp input').first().click();
    await page.keyboard.type(code.slice(0, 5));
    await timed(page, 'Votante: verificar el código (última cifra)',
      async () => { await page.evaluate(() => window.__rnf8Mark()); await page.keyboard.type(code.slice(5)); },
      () => page.locator('ion-radio-group, app-error-state').first().waitFor({ timeout: 30_000 }));
    if (await page.locator('app-error-state').isVisible()) throw new Error(`${VOTER} cannot vote (already voted or locked)`);
    await page.locator('ion-radio').first().click();
    await timed(page, 'Votante: revisar el voto (sin servidor)',
      () => page.getByRole('button', { name: /revisar/i }).click(), () => page.locator('app-review-step').waitFor());
    await timed(page, 'Votante: confirmar y votar',
      () => page.locator('app-review-step ion-button').last().click(), () => page.locator('app-receipt-step').waitFor({ timeout: 30_000 }));
    const receipt = (await page.locator('app-receipt-step p.code').textContent())?.trim() ?? '';
    await page.locator('app-receipt-step ion-button').last().click();
    const field = page.getByRole('textbox');
    await field.waitFor();
    if (!(await field.inputValue())) { await field.click(); await page.keyboard.type(receipt); }
    await timed(page, 'Votante: comprobar el recibo',
      () => page.getByRole('button', { name: /^comprobar$/i }).click(), () => page.locator('p.result').waitFor({ timeout: 30_000 }));
  } else log('RNF8_VOTER not set: voter actions skipped');

  // ---------- console: results as admin@ual.test, ballot-box verification as auditor@ual.test (RF-12 is AUDITOR only) ----------
  if (process.env.DEMO_PASSWORD) {
    async function consoleLogin(user) {
      const context = await browser.newContext({ viewport: { width: 1440, height: 1024 }, locale: 'es-ES', acceptDownloads: true });
      const page = await context.newPage();
      let token = null;
      page.on('request', (r) => { const v = r.headers().authorization; if (v?.startsWith('Bearer ') && r.url().includes('/api/')) token = v.slice(7); });
      await page.goto(`${APP}/admin`);
      await page.waitForSelector('#username', { timeout: 30_000 });
      await page.fill('#username', user);
      await page.fill('#password', process.env.DEMO_PASSWORD);
      const since = Date.now();
      await page.click('#kc-login');
      const otp = page.locator('input[type="text"]:visible:not(#username), input[inputmode="numeric"]:visible, input[autocomplete="one-time-code"]:visible').first();
      await otp.waitFor({ timeout: 30_000 });
      await otp.fill(await waitMail(user, since, (t) => sixDigits(t), 'login code'));
      const submit = page.locator('#kc-login:visible, input[type="submit"]:visible, button[type="submit"]:visible').first();
      if (await submit.count()) await submit.click(); else await otp.press('Enter');
      await page.waitForSelector('app-admin-page', { timeout: 30_000 });
      for (let i = 0; i < 20 && !token; i++) await sleep(250);
      return { page, token };
    }
    const admin = await consoleLogin('admin@ual.test');
    const processes = (await http('GET', `${GW}/api/v1/processes`, { token: admin.token })).data;
    const past = processes.filter((p) => (p.title?.es ?? '').includes(PAST) && ['TALLIED', 'ARCHIVED'].includes(p.status))
      .sort((a, b) => b.id - a.id)[0];
    if (!past) throw new Error(`no tallied process whose title contains «${PAST}»`);
    log(`console actions on process #${past.id}`);
    let page = admin.page;
    for (let run = 1; run <= RUNS; run++) {
      await page.goto(`${APP}/admin/resultados?proceso=${past.id}`);
      await page.getByRole('button', { name: /exportar resultados/i }).waitFor({ timeout: 30_000 });
      await sleep(800);
      let download = page.waitForEvent('download');
      await timed(page, `Consola: exportar resultados CSV (${run})`,
        () => page.getByRole('button', { name: /exportar resultados/i }).click(), () => download);
      download = page.waitForEvent('download');
      await timed(page, `Consola: generar acta PDF (${run})`,
        () => page.getByRole('button', { name: /generar acta/i }).first().click(), () => download);
    }
    page = (await consoleLogin('auditor@ual.test')).page;
    for (let run = 1; run <= RUNS; run++) {
      await page.goto(`${APP}/admin/auditoria?proceso=${past.id}`);
      const verify = page.getByRole('button', { name: /verificar integridad/i });
      await verify.waitFor({ timeout: 30_000 });
      await sleep(800);
      await timed(page, `Consola (auditor): verificar la integridad de la urna (${run})`,
        () => verify.click(), async () => { await sleep(50); await page.waitForFunction(() =>
          !document.querySelector('ion-spinner[name="crescent"]'), null, { timeout: 30_000 }); });
    }
  } else log('DEMO_PASSWORD not set: console actions skipped');
} finally {
  await browser.close();
}

const failed = rows.filter((r) => !r.ok);
log(`DONE · ${rows.length} actions, ${failed.length} not meeting RNF-8 · ${OUT}`);
