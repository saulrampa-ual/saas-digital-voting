// Usability session H1b (docs/evaluation/usabilidad/preparacion-sesion.md) · local environment only.
// Builds the synthetic scenario of section 1 of that guide in one go:
//   organization USAB with admin.usab@verivoto.test (ADMIN) and censo.usab@verivoto.test (CENSUS_MANAGER);
//   census «Sesión de usabilidad» imported from docs/evaluation/usabilidad/censo-sesion.csv (P01–P05, R1, R2);
//   process «Votación de prueba: actividad del club» (single choice, blank vote, email code, CENSUS_ONLY), scheduled;
//   rehearsal: census «Ensayo de la sesión» (2 entries) and process «Ensayo: actividad del club», so that the
//   rehearsal never spends an entry of the real census.
// Then waits for the 9 invitations and for both processes to open, and writes the moderator sheet to
// .local/sesion-usab.md (git-ignored): one row per entry with its Mailpit "inbox". It never opens a personal link,
// requests a code or votes, and the log never prints tokens, codes, links or passwords.
// Idempotent: run it again to rebuild the sheet (existing organization, users, censuses and processes are reused).
//
// Usage, from the repository root, with the whole stack running (infra, five services, gateway, frontend):
//   set DEMO_PASSWORD=<password for the USAB management accounts, 12+ characters>
//   node scripts/demo/seed-usab.mjs
// Options (environment variables):
//   USAB_OPEN_MINUTES  minutes from now until the processes open (default 5, minimum 2)
//   USAB_DAYS          days the processes stay open (default 7; must cover every session)
//   USAB_REHEARSAL=0   do not create the rehearsal census and process

import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { join } from 'node:path';
import {
  ROOT, APP, MAILPIT, GW, log, iso, http, demoPassword, readInfraEnv, loadChromium, createKeycloakAdmin, createLogin,
  ensureOrganization, ensureCensus, ensureScheduledProcess, waitStatus, inviteLink, mailboxUrl,
} from './lib/common.mjs';

const ORG = { id: 'USAB', name: 'Organización de prueba (usabilidad)' };
const MANAGERS = {
  admin: { fullName: 'Administración USAB (prueba)', email: 'admin.usab@verivoto.test', role: 'ADMIN' },
  censo: { fullName: 'Gestión del censo USAB (prueba)', email: 'censo.usab@verivoto.test', role: 'CENSUS_MANAGER' },
};
const CSV_PATH = join(ROOT, 'docs', 'evaluation', 'usabilidad', 'censo-sesion.csv');
const OPEN_MINUTES = Math.max(2, Number(process.env.USAB_OPEN_MINUTES ?? 5));
const DAYS = Math.max(1, Number(process.env.USAB_DAYS ?? 7));
const REHEARSAL = process.env.USAB_REHEARSAL !== '0';

const BALLOT = {
  voteType: 'SINGLE_CHOICE', blankVoteEnabled: true,
  question: { es: '¿Qué actividad debería organizar el club el próximo mes?', en: 'Which activity should the club organise next month?' },
  options: [
    { position: 1, text: { es: 'Ruta de senderismo', en: 'Hiking trip' } },
    { position: 2, text: { es: 'Taller de fotografía', en: 'Photography workshop' } },
    { position: 3, text: { es: 'Visita a un museo', en: 'Museum visit' } },
  ],
};
const DESCRIPTION = {
  es: 'Votación ficticia para una prueba de usabilidad. No tiene ningún efecto.',
  en: 'Fictitious vote for a usability test. It has no effect.',
};
const processBody = (censusId, title) => ({
  censusId, title, description: DESCRIPTION,
  opensAt: iso(Date.now() + OPEN_MINUTES * 60_000), closesAt: iso(Date.now() + DAYS * 86_400_000),
  defaultLanguage: 'es', resultsVisibility: 'CENSUS_ONLY', ballot: BALLOT, voterAuthentication: 'EMAIL_CODE',
});

const SESSION = {
  census: 'Sesión de usabilidad',
  title: { es: 'Votación de prueba: actividad del club', en: 'Test vote: club activity' },
};
const REHEARSAL_SETUP = {
  census: 'Ensayo de la sesión',
  title: { es: 'Ensayo: actividad del club', en: 'Rehearsal: club activity' },
  voters: [1, 2].map((n) => ({ fullName: `Ensayo E${n}`, email: `e${n}@verivoto.test`, institutionalId: `USAB-E${n}`, preferredLanguage: 'es' })),
};

/** Entries of the session CSV (header: full_name,email,phone,institutional_id,preferred_language). */
function sessionVoters() {
  const [, ...rows] = readFileSync(CSV_PATH, 'utf8').split(/\r?\n/).filter((l) => l.trim());
  return rows.map((row) => { const [fullName, email] = row.split(','); return { fullName, email }; });
}

/** Imports the CSV in two steps like screen A2 (dry run, then apply) unless the census already has its voters. */
async function importSessionCensus(token, census, expected) {
  if (census.frozen) { log(`census «${census.name}» is frozen (process scheduled); voters not changed`); return; }
  const upload = (dryRun) => {
    const form = new FormData();
    form.append('file', new Blob([readFileSync(CSV_PATH)], { type: 'text/csv' }), 'censo-sesion.csv');
    return http('POST', `${GW}/api/v1/censuses/${census.id}/imports?dryRun=${dryRun}&discardInvalidLines=false`, { token, multipart: form });
  };
  const { data: check } = await upload(true);
  if (check.invalidLines?.length) throw new Error(`censo-sesion.csv has ${check.invalidLines.length} invalid line(s)`);
  if (check.toCreate === 0 && check.duplicates?.length === expected) { log(`census «${census.name}» already has its ${expected} entries`); return; }
  const { data: applied } = await upload(false);
  log(`census «${census.name}»: ${applied.toCreate} added, ${applied.duplicates?.length ?? 0} already there`);
}

async function addVoters(token, census, voters) {
  if (census.frozen) return;
  for (const v of voters) {
    const r = await http('POST', `${GW}/api/v1/censuses/${census.id}/voters`, { token, body: v, ok: [201, 409] });
    if (r.status === 201) log(`entry added to «${census.name}»: ${v.email}`);
  }
}

const PASSWORD = demoPassword();
const chromium = loadChromium();
const kc = createKeycloakAdmin(readInfraEnv(), PASSWORD);
const people = sessionVoters();
const result = { session: null, rehearsal: null, links: {} };
let browser;
try {
  browser = await chromium.launch();
  const login = createLogin(browser, PASSWORD);
  const adm = await kc.token();
  await kc.ensureIdentityViewRealm(adm);
  await kc.setPassword(adm, 'superadmin');
  const superadmin = await login('superadmin', (await kc.user(adm, 'superadmin')).email);
  await ensureOrganization(superadmin, { ...ORG, managers: MANAGERS });
  for (const m of Object.values(MANAGERS)) await kc.setPassword(adm, m.email);

  const censo = await login(MANAGERS.censo.email);
  const sessionCensus = await ensureCensus(censo, SESSION.census);
  await importSessionCensus(censo, sessionCensus, people.length);
  const rehearsalCensus = REHEARSAL ? await ensureCensus(censo, REHEARSAL_SETUP.census) : null;
  if (rehearsalCensus) await addVoters(censo, rehearsalCensus, REHEARSAL_SETUP.voters);

  const admin = await login(MANAGERS.admin.email);
  const scheduledAt = Date.now();
  const session = await ensureScheduledProcess(admin, processBody(sessionCensus.id, SESSION.title));
  const rehearsal = rehearsalCensus ? await ensureScheduledProcess(admin, processBody(rehearsalCensus.id, REHEARSAL_SETUP.title)) : null;

  // A process scheduled just now: only invitations sent after scheduling count (older ones would be from a
  // previous local environment); one scheduled in an earlier run: its newest invitation.
  const since = (p) => (p.status === 'DRAFT' ? scheduledAt : 0);
  for (const v of people) result.links[v.email] = await inviteLink(v.email, since(session));
  if (rehearsal) for (const v of REHEARSAL_SETUP.voters) result.links[v.email] = await inviteLink(v.email, since(rehearsal));
  log(`invitations received: ${Object.keys(result.links).length}`);

  log(`waiting for the processes to open (about ${OPEN_MINUTES} min)…`);
  result.session = await waitStatus(admin, session.id, ['OPEN', 'CLOSED', 'TALLIED', 'ARCHIVED'], OPEN_MINUTES + 3);
  if (rehearsal) result.rehearsal = await waitStatus(admin, rehearsal.id, ['OPEN', 'CLOSED', 'TALLIED', 'ARCHIVED'], 3);
  log(`session process #${result.session.id}: ${result.session.status}` + (result.rehearsal ? ` · rehearsal #${result.rehearsal.id}: ${result.rehearsal.status}` : ''));
} finally {
  if (browser) await browser.close();
}

// ---------- moderator sheet (local file, git-ignored) ----------
const STATUS = { DRAFT: 'Borrador', SCHEDULED: 'Programado', OPEN: 'Abierto', CLOSED: 'Cerrado', TALLIED: 'Escrutado', ARCHIVED: 'Archivado' };
const when = (value) => new Date(value).toLocaleString('es-ES', { timeZone: 'Europe/Madrid', dateStyle: 'short', timeStyle: 'short' });
const rows = (voters) => voters.map((v, i) => `| ${i + 1} | ${v.fullName} | \`${v.email}\` | [abrir buzón](${mailboxUrl(v.email)}) | ${APP}/votar/${result.links[v.email]} | ☐ |`);
const table = ['| # | Entrada | Dirección | Buzón (Mailpit) | Enlace personal (solo si falla el buzón) | Usada |', '| --- | --- | --- | --- | --- | --- |'];
const lines = [
  '# Sesión de usabilidad H1b · hoja de la persona moderadora (solo este equipo)', '',
  `Generada: ${when(Date.now())}. Guía: \`docs/evaluation/usabilidad/preparacion-sesion.md\` · Mailpit: ${MAILPIT}.`, '',
  '**No compartir ni subir al repositorio.** Cada enlace sirve para votar una sola vez. No abrir ningún enlace antes de su',
  'sesión; la persona participante abre su invitación desde el buzón de Mailpit en una ventana privada nueva.', '',
  `## Sesiones · proceso #${result.session.id} «${SESSION.title.es}» · ${STATUS[result.session.status] ?? result.session.status}`, '',
  `Abre ${when(result.session.opensAt)} · cierra ${when(result.session.closesAt)} · opción de la tarjeta: «Taller de fotografía».`, '',
  'Orden de uso: P01 a P05; R1 y R2 solo si una sesión no es válida por una incidencia de entorno (guía, apartado 7).', '',
  ...table, ...rows(people), '',
];
if (result.rehearsal) {
  lines.push(`## Ensayo · proceso #${result.rehearsal.id} «${REHEARSAL_SETUP.title.es}» · ${STATUS[result.rehearsal.status] ?? result.rehearsal.status}`, '',
    'Solo para el ensayo con una persona del equipo (no se registra en la hoja de resultados).', '',
    ...table, ...rows(REHEARSAL_SETUP.voters), '');
}
lines.push('## Cuentas de gestión de USAB', '',
  `Consola: ${APP}/admin · contraseña: la de DEMO_PASSWORD · código de acceso en Mailpit.`, '',
  ...Object.values(MANAGERS).map((m) => `- ${m.role}: \`${m.email}\``),
  '', 'Durante las sesiones, «Participación» (cuenta ADMIN) muestra solo el número de votos: debe subir uno por sesión.', '');
mkdirSync(join(ROOT, '.local'), { recursive: true });
writeFileSync(join(ROOT, '.local', 'sesion-usab.md'), lines.join('\n'), 'utf8');
log('DONE · moderator sheet in .local/sesion-usab.md');
