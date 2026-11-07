// Shared helpers of the local demo scripts (seed-ual.mjs, seed-usab.mjs). Local development only.
// HTTP with readable errors, Mailpit, Keycloak admin API and the management login (Authorization Code + PKCE +
// Email OTP, like the console). Nothing here prints tokens, codes, personal links or passwords.

import { createRequire } from 'node:module';
import { createHash, randomBytes } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

export const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '..');
export const KC = process.env.KC_URL ?? 'http://localhost:8080';
export const GW = process.env.GATEWAY_URL ?? 'http://localhost:8000';
export const MAILPIT = process.env.MAILPIT_URL ?? 'http://localhost:8025';
export const APP = process.env.APP_URL ?? 'http://localhost:4200';
export const REALM = 'verivoto';

export const log = (...a) => console.log(new Date().toISOString().slice(11, 19), ...a);
export const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
export const iso = (ms) => new Date(ms).toISOString();

/** Password of the demo management accounts (DEMO_PASSWORD, 12+ characters like the realm policy). */
export function demoPassword() {
  const password = process.env.DEMO_PASSWORD;
  if (!password || password.length < 12) throw new Error('DEMO_PASSWORD (12+ characters) is required');
  return password;
}

/** infra/.env as an object (Keycloak bootstrap admin). */
export function readInfraEnv() {
  return Object.fromEntries(readFileSync(join(ROOT, 'infra', '.env'), 'utf8').split(/\r?\n/)
    .filter((l) => /^[A-Z0-9_]+=/.test(l)).map((l) => [l.slice(0, l.indexOf('=')), l.slice(l.indexOf('=') + 1).trim()]));
}

/** Playwright from the frontend's node_modules (no extra install). */
export function loadChromium() {
  const require = createRequire(join(ROOT, 'frontend', 'package.json'));
  return require('@playwright/test').chromium;
}

// ---------- HTTP ----------
export async function http(method, url, { token, body, headers = {}, form, multipart, ok = [200, 201, 202, 204] } = {}) {
  const h = { ...headers };
  if (token) h.Authorization = `Bearer ${token}`;
  let payload;
  if (form) { h['Content-Type'] = 'application/x-www-form-urlencoded'; payload = new URLSearchParams(form).toString(); }
  else if (multipart) { payload = multipart; }
  else if (body !== undefined) { h['Content-Type'] = 'application/json'; payload = JSON.stringify(body); }
  const res = await fetch(url, { method, headers: h, body: payload });
  const text = await res.text();
  const data = text ? (() => { try { return JSON.parse(text); } catch { return text; } })() : null;
  if (!ok.includes(res.status)) {
    const shown = typeof data === 'string' ? data.slice(0, 200) : JSON.stringify(data)?.slice(0, 300);
    const err = new Error(`${method} ${url.replace(/\/links\/[^/]+/, '/links/…')} -> ${res.status} ${shown}`);
    err.status = res.status; throw err;
  }
  return { status: res.status, data };
}

/** Lists returned either as arrays or as pages ({ content }, { items }). */
export const asList = (data) => (Array.isArray(data) ? data : (data?.content ?? data?.items ?? []));

// ---------- Mailpit ----------
export async function mails(to, since) {
  const { data } = await http('GET', `${MAILPIT}/api/v1/search?query=${encodeURIComponent(`to:"${to}"`)}&limit=50`);
  return (data.messages ?? []).filter((m) => !since || Date.parse(m.Created) >= since - 2000);
}
export async function mailText(id) { return (await http('GET', `${MAILPIT}/api/v1/message/${id}`)).data.Text ?? ''; }
/** Newest matching mail first (Mailpit returns the newest first); 2 min at most. */
export async function waitMail(to, since, predicate, label) {
  for (let i = 0; i < 60; i++) {
    for (const m of await mails(to, since)) {
      const v = predicate(await mailText(m.ID), m);
      if (v) return v;
    }
    await sleep(2000);
  }
  throw new Error(`Mail not received (${label}) for ${to}`);
}
export const sixDigits = (t) => (t.match(/(?<![\d/.:-])\d{6}(?![\d/.:-])/) ?? [])[0];
/** Personal link token of the newest invitation of that address. */
export const inviteLink = (email, since = 0) =>
  waitMail(email, since, (t) => (t.match(/\/votar\/([A-Za-z0-9_-]{64})/) ?? [])[1], 'invitation');
/** Mailpit page filtered by one address (the participant's "inbox" in the usability session). */
export const mailboxUrl = (email) => `${MAILPIT}/search?q=${encodeURIComponent(`to:${email}`)}`;

// ---------- Keycloak admin ----------
export function createKeycloakAdmin(env, password) {
  async function token() {
    const { data } = await http('POST', `${KC}/realms/master/protocol/openid-connect/token`, { form: {
      grant_type: 'password', client_id: 'admin-cli',
      username: env.KC_BOOTSTRAP_ADMIN_USERNAME || 'local-admin', password: env.KC_BOOTSTRAP_ADMIN_PASSWORD } });
    return data.access_token;
  }
  async function user(adm, username) {
    const { data } = await http('GET', `${KC}/admin/realms/${REALM}/users?username=${encodeURIComponent(username)}&exact=true`, { token: adm });
    return data[0];
  }
  async function setPassword(adm, username) {
    const u = await user(adm, username);
    if (!u) throw new Error(`Keycloak user not found: ${username}`);
    await http('PUT', `${KC}/admin/realms/${REALM}/users/${u.id}/reset-password`, { token: adm, body: { type: 'password', value: password, temporary: false } });
    await http('PUT', `${KC}/admin/realms/${REALM}/users/${u.id}`, { token: adm, body: { requiredActions: [], emailVerified: true } });
    // Unlock in case brute-force detection locked it during earlier attempts
    await http('DELETE', `${KC}/admin/realms/${REALM}/attack-detection/brute-force/users/${u.id}`, { token: adm, ok: [200, 204, 404] });
    log(`password set: ${username}`);
  }
  async function ensureSuperadmin(adm, u) { // realm-level account with role SUPERADMIN (no organization)
    let found = await user(adm, u.username);
    if (!found) {
      await http('POST', `${KC}/admin/realms/${REALM}/users`, { token: adm, body: { username: u.username, email: u.email, firstName: u.firstName, lastName: u.lastName, enabled: true, emailVerified: true } });
      found = await user(adm, u.username);
      log(`superadmin created: ${u.username}`);
    } else log(`superadmin exists: ${u.username}`);
    const { data: role } = await http('GET', `${KC}/admin/realms/${REALM}/roles/SUPERADMIN`, { token: adm });
    await http('POST', `${KC}/admin/realms/${REALM}/users/${found.id}/role-mappings/realm`, { token: adm, body: [role] });
    await setPassword(adm, u.username);
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
  return { token, user, setPassword, ensureSuperadmin, ensureIdentityViewRealm };
}

// ---------- management login (Authorization Code + PKCE + Email OTP, like the console) ----------
export function createLogin(browser, password) {
  return async function login(username, email = username) {
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
      await page.fill('#password', password);
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
  };
}

// ---------- organizations, censuses and processes ----------
/** Creates the organization with its initial administrator if missing, then the other management users. */
export async function ensureOrganization(token, { id, name, managers }) {
  const org = await http('GET', `${GW}/api/v1/organizations/${id}`, { token, ok: [200, 404] });
  if (org.status === 404) {
    await http('POST', `${GW}/api/v1/organizations`, { token, body: { id, name,
      initialAdministrator: { fullName: managers.admin.fullName, email: managers.admin.email } } });
    log(`organization ${id} created`);
  } else log(`organization ${id} already exists`);
  const users = asList((await http('GET', `${GW}/api/v1/organizations/${id}/users`, { token })).data);
  for (const m of Object.values(managers)) {
    if (users.some((u) => (u.email ?? '').toLowerCase() === m.email)) { log(`user exists: ${m.email}`); continue; }
    await http('POST', `${GW}/api/v1/organizations/${id}/users`, { token, body: m });
    log(`user created: ${m.email} (${m.role})`);
  }
}

/** Finds a census by name or creates it. */
export async function ensureCensus(token, name) {
  const found = asList((await http('GET', `${GW}/api/v1/censuses`, { token })).data).find((c) => c.name === name);
  if (found) { log(`census exists: ${name}`); return found; }
  const created = (await http('POST', `${GW}/api/v1/censuses`, { token, body: { name } })).data;
  log(`census created: ${name}`);
  return created;
}

/** Finds a process by its Spanish title or creates it as a draft; then schedules it if it is still a draft. */
export async function ensureScheduledProcess(token, body) {
  let p = asList((await http('GET', `${GW}/api/v1/processes`, { token })).data).find((x) => x.title?.es === body.title.es);
  if (!p) { p = (await http('POST', `${GW}/api/v1/processes`, { token, body })).data; log(`process created: #${p.id} ${body.title.es}`); }
  else log(`process exists: #${p.id} (${p.status}) ${body.title.es}`);
  if (p.status === 'DRAFT') { await http('POST', `${GW}/api/v1/processes/${p.id}/schedule`, { token }); log(`process scheduled: #${p.id}`); }
  return p;
}

export async function waitStatus(token, id, wanted, minutes) {
  for (let i = 0; i < minutes * 12; i++) {
    const { data } = await http('GET', `${GW}/api/v1/processes/${id}`, { token });
    if (wanted.includes(data.status)) return data;
    await sleep(5000);
  }
  throw new Error(`Process ${id} did not reach ${wanted.join('/')} in ${minutes} min`);
}
