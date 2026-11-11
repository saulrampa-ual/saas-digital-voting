// H1c (RNF-17) · local environment only. Prepares one load run of tests/load/voting.js (docs/evaluation/carga.md):
//   organization LOAD with admin.load@verivoto.test (ADMIN), censo.load@verivoto.test (CENSUS_MANAGER) and
//   auditor.load@verivoto.test (AUDITOR, used by check-load-run.mjs to verify the chain after the run);
//   census «Carga H1c · ejecución N» with LOAD_VOTERS synthetic entries (load-N-0001@verivoto.test, …);
//   process «Carga H1c · ejecución N» (single choice, email code), scheduled and opened;
//   then, like the PWA, for every entry: invitation link (Mailpit) → code → voting token.
// Writes the tokens to .local/load/tokens-run-N.json (git-ignored; delete it after the run) and prints the
// PowerShell lines with BASE_URL, PROCESS_ID, OPTION_ID and TOKENS_FILE for k6, and .local/load/run-N.json with the
// process id only (no tokens), read by scripts/load/check-load-run.mjs. Voting tokens live 15 minutes
// (voter-access voting-token-validity), so run k6 right after this script.
// It deletes from Mailpit only the messages it read (by id), never the whole mailbox. Mailpit keeps 500 messages by
// default and drops the oldest ones, so do NOT run it while the H1b session mailboxes (USAB) are still needed.
// The log never prints tokens, codes, links or passwords.
//
// Usage, from the repository root, with the whole stack running:
//   $env:DEMO_PASSWORD = <password of the LOAD management accounts, 12+ characters, see docs/evaluation/carga.md>
//   $env:LOAD_RUN = "1"            # 1, 2 or 3: each run needs a new process (a token votes once)
//   node scripts/load/prepare-voting-tokens.mjs
// Options: LOAD_VOTERS (default 500), LOAD_CONCURRENCY (default 10), LOAD_OPEN_MINUTES (default 2, minimum 2).

import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import {
  ROOT, GW, MAILPIT, log, sleep, iso, http, demoPassword, readInfraEnv, loadChromium, createKeycloakAdmin, createLogin,
  ensureOrganization, ensureCensus, ensureScheduledProcess, waitStatus, mails, mailText, sixDigits,
} from '../demo/lib/common.mjs';

const RUN = Number(process.env.LOAD_RUN ?? 1);
const VOTERS = Math.max(1, Number(process.env.LOAD_VOTERS ?? 500));
const CONCURRENCY = Math.max(1, Number(process.env.LOAD_CONCURRENCY ?? 10));
const OPEN_MINUTES = Math.max(2, Number(process.env.LOAD_OPEN_MINUTES ?? 2));
if (!Number.isInteger(RUN) || RUN < 1) throw new Error('LOAD_RUN must be 1, 2, 3…');

const ORG = { id: 'LOAD', name: 'Organización de prueba de carga' };
const MANAGERS = {
  admin: { fullName: 'Administración de carga LOAD', email: 'admin.load@verivoto.test', role: 'ADMIN' },
  censo: { fullName: 'Gestión del censo de carga LOAD', email: 'censo.load@verivoto.test', role: 'CENSUS_MANAGER' },
  auditor: { fullName: 'Auditoría de carga LOAD', email: 'auditor.load@verivoto.test', role: 'AUDITOR' },
};
const pad = (n) => String(n).padStart(4, '0');
const voters = Array.from({ length: VOTERS }, (_, i) => ({
  fullName: `Votante sintético ${RUN}-${pad(i + 1)}`, email: `load-${RUN}-${pad(i + 1)}@verivoto.test`,
}));
const TITLE = { es: `Carga H1c · ejecución ${RUN}`, en: `Load H1c · run ${RUN}` };

/** Runs `work` over `items` with at most `limit` at a time. */
async function pool(items, limit, work) {
  const results = new Array(items.length);
  let next = 0;
  await Promise.all(Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (next < items.length) { const i = next++; results[i] = await work(items[i], i); }
  }));
  return results;
}

/** Newest message for `to` sent after `since` whose text satisfies `pick`; returns { value, id }. */
async function waitFor(to, since, pick, label) {
  for (let i = 0; i < 90; i++) {
    for (const m of await mails(to, since)) {
      const value = pick(await mailText(m.ID));
      if (value) return { value, id: m.ID };
    }
    await sleep(2000);
  }
  throw new Error(`Mail not received (${label}) for one synthetic voter`);
}

async function deleteMails(ids) {
  for (let i = 0; i < ids.length; i += 200) {
    const batch = ids.slice(i, i + 200);
    if (batch.length) await http('DELETE', `${MAILPIT}/api/v1/messages`, { body: { IDs: batch } }); // never empty: empty = all
  }
}

/** Waits for the management API and voter-access service to agree that the process is open. */
async function requestOtpWhenOpen(token, id, link) {
  for (let i = 0; i < 90; i++) {
    const { data: process } = await http('GET', `${GW}/api/v1/processes/${id}`, { token });
    if (process.status === 'OPEN') {
      try {
        await http('POST', `${GW}/api/v1/voter-access/links/${link}/otp`, { ok: [202, 200] });
        return;
      } catch (error) {
        // The voter-access service can lag behind the management API during the scheduled transition.
        if (error.status !== 422 || !String(error).includes('todavía no está abierta')) throw error;
      }
    }
    await sleep(2000);
  }
  throw new Error(`Process #${id} did not become available for OTP requests`);
}

async function importCensus(token, census) {
  if (census.frozen) { log(`census «${census.name}» is frozen; voters not changed`); return; }
  const csv = ['full_name,email,phone,institutional_id,preferred_language',
    ...voters.map((v, i) => `${v.fullName},${v.email},,LOAD${RUN}-${pad(i + 1)},es`)].join('\n');
  const form = new FormData();
  form.append('file', new Blob([csv], { type: 'text/csv' }), `carga-${RUN}.csv`);
  const { data } = await http('POST', `${GW}/api/v1/censuses/${census.id}/imports?dryRun=false&discardInvalidLines=false`,
    { token, multipart: form });
  log(`census «${census.name}»: ${data.toCreate} added, ${data.duplicates?.length ?? 0} already there`);
}

const PASSWORD = demoPassword();
const kc = createKeycloakAdmin(readInfraEnv(), PASSWORD);
let browser;
let processId;
let admin;
try {
  browser = await loadChromium().launch();
  const login = createLogin(browser, PASSWORD);
  const setLoadPassword = async (adm, username) => {
    try {
      await kc.setPassword(adm, username);
    } catch (error) {
      if (error.status !== 400 || !String(error).includes('invalidPasswordHistory')) throw error;
      // The normal login below validates the active password; avoid triggering an extra OTP email here.
      log(`password reset skipped due to password history: ${username}`);
    }
  };
  const adm = await kc.token();
  await kc.ensureIdentityViewRealm(adm);
  const superadminEmail = (await kc.user(adm, 'superadmin')).email;
  await setLoadPassword(adm, 'superadmin');
  const superadmin = await login('superadmin', superadminEmail);
  await ensureOrganization(superadmin, { ...ORG, managers: MANAGERS });
  for (const m of Object.values(MANAGERS)) await setLoadPassword(adm, m.email);
  const censo = await login(MANAGERS.censo.email);
  const census = await ensureCensus(censo, TITLE.es);
  await importCensus(censo, census);
  admin = await login(MANAGERS.admin.email);
  const scheduledAt = Date.now();
  const proc = await ensureScheduledProcess(admin, {
    censusId: census.id, title: TITLE, description: { es: 'Proceso sintético de prueba de carga.', en: 'Synthetic load test process.' },
    opensAt: iso(Date.now() + OPEN_MINUTES * 60_000), closesAt: iso(Date.now() + 86_400_000), defaultLanguage: 'es',
    resultsVisibility: 'CENSUS_ONLY', voterAuthentication: 'EMAIL_CODE',
    ballot: { voteType: 'SINGLE_CHOICE', blankVoteEnabled: true, question: { es: '¿Opción de prueba?', en: 'Test option?' },
      options: [{ position: 1, text: { es: 'Opción A', en: 'Option A' } }, { position: 2, text: { es: 'Opción B', en: 'Option B' } }] },
  });
  if (!['DRAFT', 'SCHEDULED', 'OPEN'].includes(proc.status)) {
    throw new Error(`Process «${TITLE.es}» already exists (${proc.status}); use a new LOAD_RUN`);
  }
  processId = proc.id;
  const opened = await waitStatus(admin, proc.id, ['OPEN', 'CLOSED', 'TALLIED', 'ARCHIVED'], OPEN_MINUTES + 3);
  if (opened.status !== 'OPEN') throw new Error(`Process #${proc.id} is ${opened.status}, not OPEN`);
} finally {
  if (browser) await browser.close();
}

log(`collecting ${VOTERS} invitations…`);
const invitations = await pool(voters, CONCURRENCY, (v) =>
  waitFor(v.email, 0, (t) => (t.match(/\/votar\/([A-Za-z0-9_-]{64})/) ?? [])[1], 'invitation'));
log(`invitations: ${invitations.length} (kept in Mailpit until tokens are ready)`);

// The scheduled/open transition can lag behind the first status response. Confirm OPEN again
// immediately before requesting OTPs so a transient or stale status cannot consume invitations.
const ready = await waitStatus(admin, processId, ['OPEN', 'CLOSED', 'TALLIED', 'ARCHIVED'], OPEN_MINUTES + 3);
if (ready.status !== 'OPEN') throw new Error(`Process #${processId} is ${ready.status}, not OPEN`);

const started = Date.now();
const codeMails = [];
const tokens = await pool(voters, CONCURRENCY, async (v, i) => {
  const link = invitations[i].value;
  const since = Date.now();
  await requestOtpWhenOpen(admin, processId, link);
  const code = await waitFor(v.email, since, (t) => (!t.includes('/votar/') && sixDigits(t)) || null, 'code');
  codeMails.push(code.id);
  const { data } = await http('POST', `${GW}/api/v1/voter-access/links/${link}/otp/verify`, { body: { code: code.value } });
  if (!data.votingToken) throw new Error('A synthetic voter got no voting token (already voted?)');
  if ((i + 1) % 50 === 0) log(`tokens: ${i + 1}/${VOTERS}`);
  return data.votingToken;
});
if (new Set(tokens).size !== tokens.length) throw new Error('Duplicated voting tokens');
const { data: ballot } = await http('GET', `${GW}/api/v1/ballot-box/processes/${processId}/ballot`,
  { headers: { 'X-Voting-Token': tokens[0] } });
await deleteMails([...invitations.map((m) => m.id), ...codeMails]);
log(`invitations and OTP emails: deleted from Mailpit after ${tokens.length} tokens were generated`);

const dir = join(ROOT, '.local', 'load');
mkdirSync(dir, { recursive: true });
const file = join(dir, `tokens-run-${RUN}.json`);
writeFileSync(file, JSON.stringify(tokens), 'utf8');
writeFileSync(join(dir, `run-${RUN}.json`), JSON.stringify({ run: RUN, processId, voters: VOTERS, preparedAt: new Date().toISOString() }), 'utf8');
const minutes = ((Date.now() - started) / 60_000).toFixed(1);
log(`DONE · ${tokens.length} tokens in ${minutes} min · valid for about ${Math.max(0, 15 - Number(minutes)).toFixed(0)} more min`);
console.log(`\n$env:BASE_URL = "${GW}"\n$env:PROCESS_ID = "${processId}"\n$env:OPTION_ID = "${ballot.options[0].id}"\n$env:TOKENS_FILE = "${file}"\n$env:VUS = "${VOTERS}"; $env:ITERATIONS = "1"\nk6 run --summary-export .local/load/k6-run-${RUN}.json tests/load/voting.js\n\n# after k6:  node scripts/load/check-load-run.mjs   (same DEMO_PASSWORD and LOAD_RUN)\n`);
