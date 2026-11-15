// RNF-4 session with administrators, online modality (docs/evaluation/usabilidad/rnf4-protocolo-administradores.md).
// Local environment only, synthetic data only.
//
// Subcommands (run from the repository root with the whole stack running):
//   node scripts/demo/seed-rnf4.mjs                 seed: organization RNF4, one ADMIN account per participant
//                                                   (P01–P05 and reserves R1, R2), census «Censo de prueba RNF-4»
//                                                   with one synthetic entry; writes .local/sesion-rnf4.md
//   node scripts/demo/seed-rnf4.mjs remote-on <url> realm frontendUrl = <url> and <url> allowed in verivoto-admin
//   node scripts/demo/seed-rnf4.mjs remote-off      undo remote-on (back to http://localhost only)
//   node scripts/demo/seed-rnf4.mjs enable <Pxx>    at the start of that participant's call (accounts are created
//                                                   disabled so that nobody can explore the console beforehand)
//   node scripts/demo/seed-rnf4.mjs disable <Pxx>   after a session: disables that account and ends its sessions
//   node scripts/demo/seed-rnf4.mjs passwords       only the last step of the seed: new passwords for the participant
//                                                   accounts (all left disabled) and .local/sesion-rnf4.md. Admin API
//                                                   only, so it also works in remote mode, where the seed's browser
//                                                   login does not complete
//   node scripts/demo/seed-rnf4.mjs login-code-fixed  online sessions only: the RNF-4 participant accounts keep the
//                                                   e-mail code step, but the code is always 111111 (their mail never
//                                                   leaves Mailpit); everybody else keeps the usual random code
//                                                   (lib/login-code.mjs: conditional sub-flows + temporary role)
//   node scripts/demo/seed-rnf4.mjs login-code-off  same, but the RNF-4 accounts skip the code step
//   node scripts/demo/seed-rnf4.mjs login-code-on   usual random code for everybody (flow as in verivoto-realm.json)
//   node scripts/demo/seed-rnf4.mjs cleanup         disables every RNF-4 participant account, runs login-code-on and
//                                                   remote-off
//
// Passwords: each participant account gets its own random password, NOT temporary (no forced change during the
// task) and used for a single session (the account is disabled right after it). DEMO_PASSWORD is only used for the
// superadmin and the census manager. Passwords are written only to .local/sesion-rnf4.md (git-ignored) and never
// to the log. Run the seed BEFORE remote-on: its logins go through http://localhost.
//
//   set DEMO_PASSWORD=<12+ characters>
//   node scripts/demo/seed-rnf4.mjs
//
// Idempotent: running the seed again reuses the organization, users and census and generates NEW passwords
// (leaving every participant account disabled), so send the credentials only after the last run.

import { randomInt } from 'node:crypto';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import {
  ROOT, KC, REALM, APP, GW, log, http, demoPassword, readInfraEnv, loadChromium, createKeycloakAdmin, createLogin,
  ensureOrganization, ensureCensus,
} from './lib/common.mjs';
import * as loginCode from './lib/login-code.mjs';

const ORG = { id: 'RNF4', name: 'Organización de prueba RNF-4' };
const CODES = ['p01', 'p02', 'p03', 'p04', 'p05', 'r1', 'r2'];
// No ( ) or other characters forbidden by Keycloak's person-name validator
const participant = (code) => ({
  fullName: `Administración de prueba ${code.toUpperCase()}`, email: `admin.rnf4.${code}@verivoto.test`, role: 'ADMIN',
});
const MANAGERS = {
  admin: participant('p01'), // initial administrator of the organization
  ...Object.fromEntries(CODES.slice(1).map((c) => [c, participant(c)])),
  censo: { fullName: 'Gestión del censo de prueba RNF4', email: 'censo.rnf4@verivoto.test', role: 'CENSUS_MANAGER' },
};
const PARTICIPANTS = CODES.map((c) => ({ code: c.toUpperCase(), ...participant(c) }));
const CENSUS = 'Censo de prueba RNF-4';
const CENSUS_ENTRY = { fullName: 'Persona sintética RNF4', email: 'votante.rnf4@verivoto.test', institutionalId: 'RNF4-V1', preferredLanguage: 'es' };

/** 16 characters, letters and digits only (easy to dictate or copy), satisfies length(12)/notUsername/notEmail. */
function randomPassword() {
  const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789';
  let p = '';
  while (!(/[A-Z]/.test(p) && /[a-z]/.test(p) && /\d/.test(p))) {
    p = Array.from({ length: 16 }, () => alphabet[randomInt(alphabet.length)]).join('');
  }
  return p;
}

const env = readInfraEnv();
const kcAdmin = (password = 'unused-unused') => createKeycloakAdmin(env, password);

async function userId(adm, email) {
  const u = await kcAdmin().user(adm, email);
  if (!u) throw new Error(`Keycloak user not found: ${email}`);
  return u.id;
}

async function setEnabled(adm, email, enabled) {
  const id = await userId(adm, email);
  await http('PUT', `${KC}/admin/realms/${REALM}/users/${id}`, { token: adm, body: { enabled } });
  if (!enabled) await http('POST', `${KC}/admin/realms/${REALM}/users/${id}/logout`, { token: adm, ok: [200, 204] });
  log(`${enabled ? 'enabled' : 'disabled and logged out'}: ${email}`);
}

async function adminClient(adm) {
  const { data } = await http('GET', `${KC}/admin/realms/${REALM}/clients?clientId=verivoto-admin`, { token: adm });
  if (!data[0]) throw new Error('client verivoto-admin not found');
  return data[0];
}

const LOCAL_ORIGIN = 'http://localhost:4200';
const withoutRemote = (list) => (list ?? []).filter((u) => u.startsWith(LOCAL_ORIGIN) || u === '+' || u === '/*');

async function remote(adm, url) {
  const origin = url ? new URL(url).origin : null;
  if (origin && !origin.startsWith('https://')) throw new Error('the public URL must be https://');
  const client = await adminClient(adm);
  const redirectUris = [...withoutRemote(client.redirectUris), ...(origin ? [`${origin}/*`] : [])];
  const webOrigins = [...withoutRemote(client.webOrigins), ...(origin ? [origin] : [])];
  const logout = (client.attributes?.['post.logout.redirect.uris'] ?? '').split('##').filter(Boolean)
    .filter((u) => u.startsWith(LOCAL_ORIGIN));
  if (origin) logout.push(`${origin}/*`);
  await http('PUT', `${KC}/admin/realms/${REALM}/clients/${client.id}`, { token: adm, body: {
    ...client, redirectUris, webOrigins, attributes: { ...client.attributes, 'post.logout.redirect.uris': logout.join('##') } } });
  // Realm frontend URL: issuer and front-channel URLs of realm "verivoto" only; master and /admin stay on localhost
  await http('PUT', `${KC}/admin/realms/${REALM}`, { token: adm, body: { attributes: { frontendUrl: origin ?? '' } } });
  log(origin ? `remote ON: ${origin} (issuer ${origin}/realms/${REALM})` : 'remote OFF: back to localhost');
}

/** Sets DEMO_PASSWORD; a password-history rejection means it is already the active one (checked by the login after). */
async function setDemoPassword(kc, adm, username) {
  try {
    await kc.setPassword(adm, username);
  } catch (error) {
    if (error.status !== 400 || !String(error).includes('invalidPasswordHistory')) throw error;
    log(`password reset skipped due to password history: ${username}`);
  }
}

/** New password for every participant account (also clears a brute-force lock) and leaves it disabled. Admin API only. */
async function newPasswords(adm) {
  const credentials = [];
  for (const p of PARTICIPANTS) {
    const password = randomPassword();
    await kcAdmin(password).setPassword(adm, p.email); // temporary: false, requiredActions: [] (lib/common.mjs)
    await setEnabled(adm, p.email, false); // enabled only at the start of its session (enable <Pxx>)
    credentials.push({ ...p, password });
  }
  return credentials;
}

function writeCredentials(credentials) {
  const when = new Date().toLocaleString('es-ES', { timeZone: 'Europe/Madrid', dateStyle: 'short', timeStyle: 'short' });
  const lines = [
    '# RNF-4 online · credenciales de participantes (solo este equipo)', '',
    `Generada: ${when}. **No subir al repositorio.** Borrar este fichero cuando la hoja de registro esté completa.`, '',
    'Las cuentas están deshabilitadas. Al empezar la videollamada: `node scripts/demo/seed-rnf4.mjs enable Pxx`;',
    'al terminar la sesión: `node scripts/demo/seed-rnf4.mjs disable Pxx`. Cada cuenta se usa en una sola sesión.',
    `Acceso: \`[ENLACE]/admin\` (la URL pública del túnel). Código de acceso del login: con \`login-code-fixed\` es siempre ${loginCode.FIXED_CODE}; con \`login-code-off\` no se pide; si no, en Mailpit, filtrado por la dirección.`, '',
    '| Código | Usuario | Contraseña | Usada / deshabilitada |', '| --- | --- | --- | --- |',
    ...credentials.map((c) => `| ${c.code} | \`${c.email}\` | \`${c.password}\` | ☐ |`), '',
    `Censo de la tarea: «${CENSUS}» (1 entrada sintética). Organización: ${ORG.id}. Consola local: ${APP}/admin.`, '',
  ];
  mkdirSync(join(ROOT, '.local'), { recursive: true });
  writeFileSync(join(ROOT, '.local', 'sesion-rnf4.md'), lines.join('\n'), 'utf8');
  log(`DONE · ${credentials.length} participant accounts · credentials in .local/sesion-rnf4.md`);
}

const [command = 'seed', arg] = process.argv.slice(2);
const adm = await kcAdmin().token();

if (command === 'remote-on') {
  if (!arg) throw new Error('usage: remote-on https://<public-host>');
  await remote(adm, arg);
} else if (command === 'remote-off') {
  await remote(adm, null);
} else if (command === 'enable' || command === 'disable') {
  const p = PARTICIPANTS.find((x) => x.code === String(arg ?? '').toUpperCase());
  if (!p) throw new Error(`usage: ${command} <${PARTICIPANTS.map((x) => x.code).join('|')}>`);
  await setEnabled(adm, p.email, command === 'enable');
} else if (command === 'login-code-fixed') {
  await loginCode.fixed(adm, PARTICIPANTS.map((p) => p.email));
} else if (command === 'login-code-off') {
  await loginCode.off(adm, PARTICIPANTS.map((p) => p.email));
} else if (command === 'login-code-on') {
  await loginCode.on(adm);
} else if (command === 'cleanup') {
  for (const p of PARTICIPANTS) await setEnabled(adm, p.email, false).catch((e) => log(`skip ${p.email}: ${e.message}`));
  await loginCode.on(adm);
  await remote(adm, null);
  log('DONE · delete .local/sesion-rnf4.md once the session data are recorded');
} else if (command === 'passwords') {
  writeCredentials(await newPasswords(adm));
} else if (command === 'seed') {
  const PASSWORD = demoPassword();
  const chromium = loadChromium();
  let browser;
  const credentials = [];
  try {
    browser = await chromium.launch();
    const login = createLogin(browser, PASSWORD);
    const kc = kcAdmin(PASSWORD);
    await kc.ensureIdentityViewRealm(adm);
    await setDemoPassword(kc, adm, 'superadmin');
    const superadmin = await login('superadmin', (await kc.user(adm, 'superadmin')).email);
    await ensureOrganization(superadmin, { ...ORG, managers: MANAGERS });

    await setDemoPassword(kc, adm, MANAGERS.censo.email);
    const censo = await login(MANAGERS.censo.email);
    const census = await ensureCensus(censo, CENSUS);
    if (!census.frozen) {
      const r = await http('POST', `${GW}/api/v1/censuses/${census.id}/voters`, { token: censo, body: CENSUS_ENTRY, ok: [201, 409] });
      log(r.status === 201 ? `entry added to «${CENSUS}»` : `«${CENSUS}» already has its entry`);
    }

    credentials.push(...await newPasswords(adm));
  } finally {
    if (browser) await browser.close();
  }

  writeCredentials(credentials);
} else {
  throw new Error(`unknown command: ${command}`);
}
