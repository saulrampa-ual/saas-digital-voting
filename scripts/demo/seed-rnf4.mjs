// RNF-4 repeat session with administrators (docs/evaluation/usabilidad/rnf4-protocolo-administradores.md, section
// «Preparación») · local environment only. Builds the synthetic scenario in one go:
//   organization RNF4 with admin.rnf4@verivoto.test (ADMIN, the account the moderator signs in with before every
//   participant) and censo.rnf4@verivoto.test (CENSUS_MANAGER);
//   census «Censo de prueba RNF-4» with three synthetic entries (programming a process needs a census with people).
// It does NOT create any process: each participant creates and schedules their own («Elección de prueba Pxx»).
// Then it lists the processes already in the organization, so the moderator sees what earlier participants left.
// Idempotent: run it again before a session day (existing organization, users, census and entries are reused).
// The log never prints passwords, codes or tokens.
//
// Usage, from the repository root, with the whole stack running (infra, five services, gateway, frontend):
//   $env:DEMO_PASSWORD = "<password of the RNF4 management accounts, 12+ characters>"
//   node scripts/demo/seed-rnf4.mjs

import {
  APP, GW, log, http, asList, demoPassword, readInfraEnv, loadChromium, createKeycloakAdmin, createLogin,
  ensureOrganization, ensureCensus, mailboxUrl,
} from './lib/common.mjs';

const ORG = { id: 'RNF4', name: 'Organización de prueba RNF-4' };
const MANAGERS = {
  // No ( ) or other characters forbidden by Keycloak's person-name validator (see seed-usab.mjs)
  admin: { fullName: 'Administración de prueba RNF4', email: 'admin.rnf4@verivoto.test', role: 'ADMIN' },
  censo: { fullName: 'Gestión del censo de prueba RNF4', email: 'censo.rnf4@verivoto.test', role: 'CENSUS_MANAGER' },
};
const CENSUS = 'Censo de prueba RNF-4';
const VOTERS = [1, 2, 3].map((n) => ({
  fullName: `Persona sintética RNF4 ${n}`, email: `rnf4.persona${n}@verivoto.test`,
  institutionalId: `RNF4-${n}`, preferredLanguage: n === 3 ? 'en' : 'es',
}));
const STATUS = { DRAFT: 'Borrador', SCHEDULED: 'Programado', OPEN: 'Abierto', CLOSED: 'Cerrado', TALLIED: 'Escrutado', ARCHIVED: 'Archivado' };

const PASSWORD = demoPassword();
const kc = createKeycloakAdmin(readInfraEnv(), PASSWORD);

/** Sets the password; a password-history rejection means it is already the active one, checked by the login below. */
async function setPassword(adm, username) {
  try {
    await kc.setPassword(adm, username);
  } catch (error) {
    if (error.status !== 400 || !String(error).includes('invalidPasswordHistory')) throw error;
    log(`password reset skipped due to password history: ${username}`);
  }
}

let browser;
let processes = [];
let census;
try {
  browser = await loadChromium().launch();
  const login = createLogin(browser, PASSWORD);
  const adm = await kc.token();
  await kc.ensureIdentityViewRealm(adm);
  const superadminEmail = (await kc.user(adm, 'superadmin')).email;
  await setPassword(adm, 'superadmin');
  const superadmin = await login('superadmin', superadminEmail);
  await ensureOrganization(superadmin, { ...ORG, managers: MANAGERS });
  for (const m of Object.values(MANAGERS)) await setPassword(adm, m.email);

  const censo = await login(MANAGERS.censo.email);
  census = await ensureCensus(censo, CENSUS);
  if (census.frozen) {
    log(`census «${CENSUS}» is frozen (a participant already scheduled a process); entries not changed`);
  } else {
    for (const v of VOTERS) {
      const r = await http('POST', `${GW}/api/v1/censuses/${census.id}/voters`, { token: censo, body: v, ok: [201, 409] });
      log(r.status === 201 ? `entry added: ${v.email}` : `entry exists: ${v.email}`);
    }
  }
  const { data: view } = await http('GET', `${GW}/api/v1/censuses/${census.id}`, { token: censo, ok: [200, 404] });
  census = { ...census, voterCount: view?.voterCount ?? null };

  const admin = await login(MANAGERS.admin.email);
  processes = asList((await http('GET', `${GW}/api/v1/processes`, { token: admin })).data);
} finally {
  if (browser) await browser.close();
}

const left = processes.map((p) => `  - #${p.id} «${p.title?.es ?? '—'}» · ${STATUS[p.status] ?? p.status}`).join('\n') || '  (ninguno)';
console.log(`
Escenario RNF-4 listo (organización ${ORG.id})
- Consola: ${APP}/admin · iniciar sesión con ${MANAGERS.admin.email} y la contraseña de DEMO_PASSWORD
  (código de acceso por correo en Mailpit: ${mailboxUrl(MANAGERS.admin.email)}).
- Censo para la ficha: «${CENSUS}»${census?.voterCount != null ? ` · ${census.voterCount} personas sintéticas` : ''}.
- Procesos ya existentes en la organización:
${left}
- Antes de cada participante: iniciar sesión, dejar la consola en «Inicio» y abrir la documentación en otra pestaña.
`);
log('done');
