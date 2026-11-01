// Demo data for local development only (never for production).
// Creates organization UAL with its management users and three voting processes:
//   past (TALLIED, with votes), present (OPEN, some votes) and future (SCHEDULED).
// Idempotent: existing organization, users, censuses, voters and processes (matched by title) are reused.
//
// Usage, from the repository root, with the whole stack running (infra, five services, gateway):
//   set DEMO_PASSWORD=<password for the demo management accounts>
//   node scripts/demo/seed-ual.mjs
// Reads infra/.env (Keycloak bootstrap admin). Writes the access summary to .local/demo-ual.md (git-ignored).
// The log never prints tokens, codes, personal links or passwords.

import { createRequire } from 'node:module';
import { createHash, randomBytes } from 'node:crypto';
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const require = createRequire(join(ROOT, 'frontend', 'package.json'));
const { chromium } = require('@playwright/test');

const KC = process.env.KC_URL ?? 'http://localhost:8080';
const GW = process.env.GATEWAY_URL ?? 'http://localhost:8000';
const MAILPIT = process.env.MAILPIT_URL ?? 'http://localhost:8025';
const APP = 'http://localhost:4200';
const REALM = 'verivoto';
const ORG = 'UAL';
const PASSWORD = process.env.DEMO_PASSWORD;
if (!PASSWORD || PASSWORD.length < 12) throw new Error('DEMO_PASSWORD (12+ characters) is required');

const env = Object.fromEntries(readFileSync(join(ROOT, 'infra', '.env'), 'utf8').split(/\r?\n/)
  .filter((l) => /^[A-Z0-9_]+=/.test(l)).map((l) => [l.slice(0, l.indexOf('=')), l.slice(l.indexOf('=') + 1).trim()]));

const log = (...a) => console.log(new Date().toISOString().slice(11, 19), ...a);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// ---------- people ----------
const MANAGERS = {
  admin: { fullName: 'Administración UAL (demo)', email: 'admin@ual.test', role: 'ADMIN' },
  censo: { fullName: 'Gestión de censo UAL (demo)', email: 'censo@ual.test', role: 'CENSUS_MANAGER' },
  auditor: { fullName: 'Auditoría UAL (demo)', email: 'auditor@ual.test', role: 'AUDITOR' },
};
const voters = (key, label, n) => Array.from({ length: n }, (_, i) => ({
  fullName: `${label} · Votante ${i + 1}`, email: `${key}.votante${i + 1}@ual.test`,
  institutionalId: `UAL-${key.toUpperCase()}-${i + 1}`, preferredLanguage: i % 3 === 2 ? 'en' : 'es',
}));

const iso = (ms) => new Date(ms).toISOString();
const ELECTIONS = [
  {
    key: 'pasada', census: 'Demo UAL · Censo elección pasada',
    title: { es: 'Demo UAL · Elección pasada: Delegación de curso 2026', en: 'UAL demo · Past election: Class delegate 2026' },
    description: { es: 'Votación ficticia ya cerrada y escrutada.', en: 'Fictitious vote, already closed and tallied.' },
    times: () => ({ opensAt: iso(Date.now() + 90_000), closesAt: iso(Date.now() + 90_000 + 240_000) }), resultsVisibility: 'PUBLIC_LINK',
    ballot: { voteType: 'SINGLE_CHOICE', blankVoteEnabled: true,
      question: { es: '¿Quién debe ser la delegación del curso?', en: 'Who should be the class delegate?' },
      options: [
        { position: 1, text: { es: 'Candidatura A · Marta', en: 'Candidate A · Marta' } },
        { position: 2, text: { es: 'Candidatura B · Pablo', en: 'Candidate B · Pablo' } },
        { position: 3, text: { es: 'Candidatura C · Irene', en: 'Candidate C · Irene' } },
      ] },
    voters: voters('pasada', 'Elección pasada', 5), votes: [0, 1, 0, 'blank'],
  },
  {
    key: 'presente', census: 'Demo UAL · Censo elección en curso',
    title: { es: 'Demo UAL · Elección en curso: Representantes en Junta de Centro', en: 'UAL demo · Ongoing election: School board representatives' },
    description: { es: 'Votación ficticia abierta durante 14 días.', en: 'Fictitious vote open for 14 days.' },
    times: () => ({ opensAt: iso(Date.now() + 90_000), closesAt: iso(Date.now() + 14 * 86_400_000) }), resultsVisibility: 'CENSUS_ONLY',
    ballot: { voteType: 'SINGLE_CHOICE', blankVoteEnabled: true,
      question: { es: '¿Qué candidatura apoyas para la Junta de Centro?', en: 'Which list do you support for the school board?' },
      options: [
        { position: 1, text: { es: 'Lista Participa', en: 'Participa list' } },
        { position: 2, text: { es: 'Lista Campus Abierto', en: 'Open Campus list' } },
      ] },
    voters: voters('presente', 'Elección en curso', 5), votes: [0, 1],
  },
  {
    key: 'futura', census: 'Demo UAL · Censo elección futura',
    title: { es: 'Demo UAL · Elección futura: Presupuestos participativos 2027', en: 'UAL demo · Future election: Participatory budget 2027' },
    description: { es: 'Votación ficticia programada para dentro de 30 días.', en: 'Fictitious vote scheduled in 30 days.' },
    times: () => ({ opensAt: iso(Date.now() + 30 * 86_400_000), closesAt: iso(Date.now() + 32 * 86_400_000) }), resultsVisibility: 'CENSUS_ONLY',
    ballot: { voteType: 'YES_NO_ABSTAIN', blankVoteEnabled: false,
      question: { es: '¿Apruebas destinar el 5 % del presupuesto a proyectos de estudiantes?', en: 'Do you approve allocating 5% of the budget to student projects?' } },
    voters: voters('futura', 'Elección futura', 4), votes: [],
  },
];

// ---------- HTTP helpers ----------
async function http(method, url, { token, body, headers = {}, form, ok = [200, 201, 202, 204] } = {}) {
  const h = { ...headers };
  if (token) h.Authorization = `Bearer ${token}`;
  let payload;
  if (form) { h['Content-Type'] = 'application/x-www-form-urlencoded'; payload = new URLSearchParams(form).toString(); }
  else if (body !== undefined) { h['Content-Type'] = 'application/json'; payload = JSON.stringify(body); }
  const res = await fetch(url, { method, headers: h, body: payload });
  const text = await res.text();
  const data = text ? (() => { try { return JSON.parse(text); } catch { return text; } })() : null;
  if (!ok.includes(res.status)) {
    const err = new Error(`${method} ${url.replace(/\/links\/[^/]+/, '/links/…')} -> ${res.status} ${typeof data === 'string' ? data.slice(0, 200) : JSON.stringify(data)?.slice(0, 300)}`);
    err.status = res.status; throw err;
  }
  return { status: res.status, data };
}

// ---------- Mailpit ----------
async function mails(to, since) {
  const { data } = await http('GET', `${MAILPIT}/api/v1/search?query=${encodeURIComponent(`to:"${to}"`)}&limit=50`);
  return (data.messages ?? []).filter((m) => !since || Date.parse(m.Created) >= since - 2000);
}
async function mailText(id) { return (await http('GET', `${MAILPIT}/api/v1/message/${id}`)).data.Text ?? ''; }
async function waitMail(to, since, predicate, label) {
  for (let i = 0; i < 60; i++) {
    for (const m of await mails(to, since)) {
      const text = await mailText(m.ID);
      const v = predicate(text, m);
      if (v) return v;
    }
    await sleep(2000);
  }
  throw new Error(`Mail not received (${label}) for ${to}`);
}
const sixDigits = (t) => (t.match(/(?<![\d/.:-])\d{6}(?![\d/.:-])/) ?? [])[0];

// ---------- Keycloak admin ----------
async function adminToken() {
  const { data } = await http('POST', `${KC}/realms/master/protocol/openid-connect/token`, { form: {
    grant_type: 'password', client_id: 'admin-cli',
    username: env.KC_BOOTSTRAP_ADMIN_USERNAME || 'local-admin', password: env.KC_BOOTSTRAP_ADMIN_PASSWORD } });
  return data.access_token;
}
async function kcUser(adm, username) {
  const { data } = await http('GET', `${KC}/admin/realms/${REALM}/users?username=${encodeURIComponent(username)}&exact=true`, { token: adm });
  return data[0];
}
async function setPassword(adm, username) {
  const u = await kcUser(adm, username);
  if (!u) throw new Error(`Keycloak user not found: ${username}`);
  await http('PUT', `${KC}/admin/realms/${REALM}/users/${u.id}/reset-password`, { token: adm, body: { type: 'password', value: PASSWORD, temporary: false } });
  await http('PUT', `${KC}/admin/realms/${REALM}/users/${u.id}`, { token: adm, body: { requiredActions: [], emailVerified: true } });
  // Unlock in case brute-force detection locked it during earlier attempts
  await http('DELETE', `${KC}/admin/realms/${REALM}/attack-detection/brute-force/users/${u.id}`, { token: adm, ok: [200, 204, 404] });
  log(`password set: ${username}`);
}
async function ensureIdentityViewRealm(adm) { // DEF-04 for realms imported before the fix
  const { data: clients } = await http('GET', `${KC}/admin/realms/${REALM}/clients?clientId=identity-service`, { token: adm });
  const { data: sa } = await http('GET', `${KC}/admin/realms/${REALM}/clients/${clients[0].id}/service-account-user`, { token: adm });
  const { data: rm } = await http('GET', `${KC}/admin/realms/${REALM}/clients?clientId=realm-management`, { token: adm });
  const { data: assigned } = await http('GET', `${KC}/admin/realms/${REALM}/users/${sa.id}/role-mappings/clients/${rm[0].id}`, { token: adm });
  if (assigned.some((r) => r.name === 'view-realm')) { log('identity-service already has view-realm'); return; }
  const { data: role } = await http('GET', `${KC}/admin/realms/${REALM}/clients/${rm[0].id}/roles/view-realm`, { token: adm });
  await http('POST', `${KC}/admin/realms/${REALM}/users/${sa.id}/role-mappings/clients/${rm[0].id}`, { token: adm, body: [role] });
  log('DEF-04: view-realm granted to identity-service in the local realm');
}

// ---------- management login (Authorization Code + PKCE + Email OTP, like the console) ----------
let browser;
async function login(username, email = username) {
  const verifier = randomBytes(32).toString('base64url');
  const challenge = createHash('sha256').update(verifier).digest('base64url');
  const redirect = `${APP}/admin`;
  const authUrl = `${KC}/realms/${REALM}/protocol/openid-connect/auth?` + new URLSearchParams({
    client_id: 'verivoto-admin', redirect_uri: redirect, response_type: 'code', scope: 'openid',
    code_challenge: challenge, code_challenge_method: 'S256', state: randomBytes(8).toString('hex'), nonce: randomBytes(8).toString('hex') });
  const context = await browser.newContext();
  const page = await context.newPage();
  let code;
  const grab = (url) => { if (!code && url.startsWith(APP)) { const c = new URL(url).searchParams.get('code'); if (c) code = c; } };
  await context.route((url) => url.href.startsWith(APP), (route) => { grab(route.request().url()); route.fulfill({ status: 200, body: 'ok' }); });
  page.on('request', (req) => grab(req.url()));
  page.on('framenavigated', (frame) => grab(frame.url()));
  try {
    await page.goto(authUrl);
    await page.fill('#username', username);
    await page.fill('#password', PASSWORD);
    const since = Date.now();
    await page.click('#kc-login');
    for (let i = 0; i < 40 && !code; i++) {
      const otp = page.locator('input[type="text"]:visible, input[type="number"]:visible, input[inputmode="numeric"]:visible, input[autocomplete="one-time-code"]:visible').first();
      if (await otp.count()) {
        const value = await waitMail(email, since, (t) => sixDigits(t), 'login code');
        await otp.fill(value);
        const submit = page.locator('#kc-login:visible, input[type="submit"]:visible, button[type="submit"]:visible').first();
        if (await submit.count()) await submit.click(); else await otp.press('Enter');
        for (let j = 0; j < 20 && !code; j++) await sleep(500);
        break;
      }
      const error = page.locator('.kc-feedback-text, #input-error, .pf-v5-c-alert__title').first();
      if (await error.count() && await error.isVisible()) throw new Error(`Login error for ${username}: ${(await error.innerText()).slice(0, 120)}`);
      await sleep(500);
    }
    grab(page.url());
    if (!code) throw new Error(`Login did not complete for ${username} (page: ${(await page.title()).slice(0, 60)})`);
  } finally { await context.close(); }
  const { data } = await http('POST', `${KC}/realms/${REALM}/protocol/openid-connect/token`, { form: {
    grant_type: 'authorization_code', client_id: 'verivoto-admin', code, redirect_uri: redirect, code_verifier: verifier } });
  log(`logged in: ${username}`);
  return data.access_token;
}

// ---------- voter (link + code, like the PWA) ----------
async function inviteLink(email) {
  return waitMail(email, 0, (t) => (t.match(/\/votar\/([A-Za-z0-9_-]{64})/) ?? [])[1], 'invitation');
}
async function vote(election, voter, choice, processId) {
  const link = await inviteLink(voter.email);
  const since = Date.now();
  await http('POST', `${GW}/api/v1/voter-access/links/${link}/otp`, { ok: [202, 200] });
  const code = await waitMail(voter.email, since, (t) => !t.includes('/votar/') && sixDigits(t), 'voter code');
  const { data: access } = await http('POST', `${GW}/api/v1/voter-access/links/${link}/otp/verify`, { body: { code } });
  if (access.hasVoted) { log(`${voter.email}: already voted`); return null; }
  const { data: ballot } = await http('GET', `${GW}/api/v1/ballot-box/processes/${processId}/ballot`, { headers: { 'X-Voting-Token': access.votingToken } });
  const selection = choice === 'blank' ? { blank: true } : { optionId: ballot.options[choice].id };
  const { data: receipt } = await http('POST', `${GW}/api/v1/ballot-box/processes/${processId}/votes`, { body: { votingToken: access.votingToken, selection } });
  const { data: check } = await http('GET', `${GW}/api/v1/ballot-box/processes/${processId}/receipts/${receipt.receiptCode}`);
  log(`${election.key}: vote cast by voter ${election.voters.indexOf(voter) + 1} (receipt present: ${check.present})`);
  return receipt.receiptCode;
}

async function waitStatus(token, id, wanted, minutes) {
  for (let i = 0; i < minutes * 12; i++) {
    const { data } = await http('GET', `${GW}/api/v1/processes/${id}`, { token });
    if (wanted.includes(data.status)) return data;
    await sleep(5000);
  }
  throw new Error(`Process ${id} did not reach ${wanted.join('/')} in ${minutes} min`);
}

// ---------- main ----------
const summary = { links: {}, receipts: {}, processes: {} };
try {
  browser = await chromium.launch();
  const adm = await adminToken();
  await ensureIdentityViewRealm(adm);
  await setPassword(adm, 'superadmin');
  const saEmail = (await kcUser(adm, 'superadmin')).email;

  const sa = await login('superadmin', saEmail);
  const org = await http('GET', `${GW}/api/v1/organizations/${ORG}`, { token: sa, ok: [200, 404] });
  if (org.status === 404) {
    await http('POST', `${GW}/api/v1/organizations`, { token: sa, body: { id: ORG, name: 'Universidad de Almería',
      initialAdministrator: { fullName: MANAGERS.admin.fullName, email: MANAGERS.admin.email } } });
    log('organization UAL created');
  } else log('organization UAL already exists');
  const { data: users } = await http('GET', `${GW}/api/v1/organizations/${ORG}/users`, { token: sa });
  const list = Array.isArray(users) ? users : (users.content ?? users.items ?? []);
  for (const m of Object.values(MANAGERS)) {
    if (list.some((u) => (u.email ?? '').toLowerCase() === m.email)) { log(`user exists: ${m.email}`); continue; }
    await http('POST', `${GW}/api/v1/organizations/${ORG}/users`, { token: sa, body: m });
    log(`user created: ${m.email} (${m.role})`);
  }
  for (const m of Object.values(MANAGERS)) await setPassword(adm, m.email);

  const censo = await login(MANAGERS.censo.email);
  const { data: censuses } = await http('GET', `${GW}/api/v1/censuses`, { token: censo });
  for (const e of ELECTIONS) {
    let c = censuses.find((x) => x.name === e.census);
    if (!c) { c = (await http('POST', `${GW}/api/v1/censuses`, { token: censo, body: { name: e.census } })).data; log(`census created: ${e.census}`); }
    e.censusId = c.id;
    if (!c.frozen) {
      for (const v of e.voters) {
        const r = await http('POST', `${GW}/api/v1/censuses/${c.id}/voters`, { token: censo, body: v, ok: [201, 409] });
        if (r.status === 201) log(`voter added to ${e.key}: ${v.email}`);
      }
    } else log(`census ${e.key} already frozen; voters not changed`);
  }

  const admin = await login(MANAGERS.admin.email);
  const { data: existing } = await http('GET', `${GW}/api/v1/processes`, { token: admin });
  for (const e of ELECTIONS) {
    let p = existing.find((x) => x.title?.es === e.title.es);
    if (!p) {
      p = (await http('POST', `${GW}/api/v1/processes`, { token: admin, body: {
        censusId: e.censusId, title: e.title, description: e.description, ...e.times(),
        defaultLanguage: 'es', resultsVisibility: e.resultsVisibility, ballot: e.ballot, voterAuthentication: 'EMAIL_CODE' } })).data;
      log(`process created: ${e.key} (#${p.id})`);
    } else log(`process exists: ${e.key} (#${p.id}, ${p.status})`);
    e.processId = p.id;
    if (p.status === 'DRAFT') { await http('POST', `${GW}/api/v1/processes/${p.id}/schedule`, { token: admin }); log(`process scheduled: ${e.key}`); }
  }

  for (const e of ELECTIONS) for (const v of e.voters) summary.links[v.email] = await inviteLink(v.email);
  log('invitations received for every voter');

  for (const e of ELECTIONS.filter((x) => x.votes.length)) {
    const st = await waitStatus(admin, e.processId, ['OPEN', 'CLOSED', 'TALLIED', 'ARCHIVED'], 6);
    if (st.status !== 'OPEN') { log(`${e.key}: status ${st.status}, no new votes`); continue; }
    for (let i = 0; i < e.votes.length; i++) {
      const receipt = await vote(e, e.voters[i], e.votes[i], e.processId);
      if (receipt) summary.receipts[e.voters[i].email] = receipt;
    }
  }

  const past = ELECTIONS[0];
  log('waiting for the past election to close and be tallied…');
  await waitStatus(admin, past.processId, ['TALLIED', 'ARCHIVED'], 10);
  log('past election tallied');
  const auditor = await login(MANAGERS.auditor.email);
  const { data: chain } = await http('POST', `${GW}/api/v1/ballot-box/processes/${past.processId}/audit-events/verification`, { token: auditor });
  log(`auditor verification of the past election: intact=${chain.intact}, events=${chain.eventsChecked}`);
  for (const e of ELECTIONS) summary.processes[e.key] = (await http('GET', `${GW}/api/v1/processes/${e.processId}`, { token: admin })).data;
} finally {
  if (browser) await browser.close();
}

// ---------- access summary (local file, git-ignored) ----------
const lines = [
  '# Demo UAL · datos de acceso (solo entorno local)', '',
  `Generado: ${new Date().toISOString()}. Consola: ${APP}/admin · Correos y códigos: ${MAILPIT}`, '',
  '## Cuentas de gestión (contraseña: la indicada en DEMO_PASSWORD; después, código de 6 cifras en Mailpit)', '',
  '| Rol | Usuario |', '| --- | --- |', '| Superadministrador | `superadmin` |',
  ...Object.values(MANAGERS).map((m) => `| ${m.role} (UAL) | \`${m.email}\` |`), '',
];
for (const e of ELECTIONS) {
  const p = summary.processes[e.key];
  lines.push(`## ${e.title.es}`, '', `Proceso #${p?.id} · estado ${p?.status} · abre ${p?.opensAt} · cierra ${p?.closesAt}` +
    (p?.resultsPublicId ? ` · resultados públicos: ${APP}/resultados/${p.resultsPublicId}` : ''), '',
    '| Votante | Correo | Ya votó (script) | Enlace personal | Recibo |', '| --- | --- | --- | --- | --- |',
    ...e.voters.map((v) => `| ${v.fullName} | \`${v.email}\` | ${summary.receipts[v.email] ? 'sí' : 'no'} | ${APP}/votar/${summary.links[v.email]} | ${summary.receipts[v.email] ?? '—'} |`), '');
}
mkdirSync(join(ROOT, '.local'), { recursive: true });
writeFileSync(join(ROOT, '.local', 'demo-ual.md'), lines.join('\n'), 'utf8');
log('DONE · summary in .local/demo-ual.md');
