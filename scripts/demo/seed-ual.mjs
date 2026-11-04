// Demo data for local development only (never for production).
// Creates organization UAL with its management users and three voting processes:
//   past (TALLIED, with votes), present (OPEN, some votes) and future (SCHEDULED).
// Idempotent: existing organization, users, censuses, voters and processes (matched by title) are reused.
//
// Usage, from the repository root, with the whole stack running (infra, five services, gateway):
//   set DEMO_PASSWORD=<password for the demo management accounts>
//   node scripts/demo/seed-ual.mjs
// Reads infra/.env (Keycloak bootstrap admin). Writes the access summary to .local/demo-ual.md (git-ignored).
// The log never prints tokens, codes, personal links or passwords. Shared helpers: lib/common.mjs.

import { writeFileSync, mkdirSync } from 'node:fs';
import { join } from 'node:path';
import {
  ROOT, APP, MAILPIT, GW, log, iso, http, demoPassword, readInfraEnv, loadChromium, createKeycloakAdmin, createLogin,
  ensureOrganization, waitStatus, waitMail, sixDigits, inviteLink,
} from './lib/common.mjs';

const ORG = 'UAL';
const PASSWORD = demoPassword();
// SEED_MODE=accounts: only named local test accounts (no censuses, processes or votes)
const MODE = process.env.SEED_MODE ?? 'demo';
const chromium = loadChromium();
const kc = createKeycloakAdmin(readInfraEnv(), PASSWORD);

// ---------- people ----------
const LOCAL_ACCOUNTS = {
  admin: { fullName: 'Laura Moreno', email: 'laura.moreno@ual.test', role: 'ADMIN' },
  censo: { fullName: 'Sergio Cano', email: 'sergio.cano@ual.test', role: 'CENSUS_MANAGER' },
  auditor: { fullName: 'Irene Vega', email: 'irene.vega@ual.test', role: 'AUDITOR' },
};
const LOCAL_SUPERADMIN = { username: 'daniel.ortiz@ual.test', email: 'daniel.ortiz@ual.test', firstName: 'Daniel', lastName: 'Ortiz' };
const DEMO_MANAGERS = {
  admin: { fullName: 'Administración UAL (demo)', email: 'admin@ual.test', role: 'ADMIN' },
  censo: { fullName: 'Gestión de censo UAL (demo)', email: 'censo@ual.test', role: 'CENSUS_MANAGER' },
  auditor: { fullName: 'Auditoría UAL (demo)', email: 'auditor@ual.test', role: 'AUDITOR' },
};
const MANAGERS = MODE === 'accounts' ? LOCAL_ACCOUNTS : DEMO_MANAGERS;
const voters = (key, label, n) => Array.from({ length: n }, (_, i) => ({
  fullName: `${label} · Votante ${i + 1}`, email: `${key}.votante${i + 1}@ual.test`,
  institutionalId: `UAL-${key.toUpperCase()}-${i + 1}`, preferredLanguage: i % 3 === 2 ? 'en' : 'es',
}));

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

// ---------- voter (link + code, like the PWA) ----------
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

// ---------- main ----------
const summary = { links: {}, receipts: {}, processes: {} };
let browser;
try {
  browser = await chromium.launch();
  const login = createLogin(browser, PASSWORD);
  const adm = await kc.token();
  await kc.ensureIdentityViewRealm(adm);
  let sa;
  if (MODE === 'accounts') { await kc.ensureSuperadmin(adm, LOCAL_SUPERADMIN); sa = await login(LOCAL_SUPERADMIN.username, LOCAL_SUPERADMIN.email); }
  else { await kc.setPassword(adm, 'superadmin'); const saEmail = (await kc.user(adm, 'superadmin')).email; sa = await login('superadmin', saEmail); }
  await ensureOrganization(sa, { id: ORG, name: 'Universidad de Almería', managers: MANAGERS });
  for (const m of Object.values(MANAGERS)) await kc.setPassword(adm, m.email);
  if (MODE !== 'accounts') {

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
  }
} finally {
  if (browser) await browser.close();
}
if (MODE === 'accounts') {
  mkdirSync(join(ROOT, '.local'), { recursive: true });
  writeFileSync(join(ROOT, '.local', 'cuentas-locales.md'), ['# Cuentas locales de prueba (solo este equipo)', '',
    `Consola: ${APP}/admin · Códigos de acceso: ${MAILPIT}. Contraseña: la indicada en DEMO_PASSWORD.`, '',
    '| Rol | Nombre | Usuario |', '| --- | --- | --- |', `| Superadministrador | ${LOCAL_SUPERADMIN.firstName} ${LOCAL_SUPERADMIN.lastName} | \`${LOCAL_SUPERADMIN.username}\` |`,
    ...Object.values(MANAGERS).map((m) => `| ${m.role} (UAL) | ${m.fullName} | \`${m.email}\` |`), ''].join('\n'), 'utf8');
  log('DONE · accounts in .local/cuentas-locales.md');
  process.exit(0);
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
