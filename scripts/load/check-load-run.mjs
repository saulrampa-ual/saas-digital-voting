// H1c (RNF-17) · local environment only. Checks one load run after k6 (docs/evaluation/carga.md, «Cuándo una ejecución
// es válida», points 7 and 8) and prints the figures to record:
//   - k6 summary (.local/load/k6-run-N.json): iterations, 201 responses, responses per status, p50/p90/p95/p99/max of
//     the vote requests, http_req_failed and checks;
//   - urn (AUDITOR auditor.load@verivoto.test): verifies the chain now and counts the VOTE_RECEIVED events;
//   - participation (ADMIN admin.load@verivoto.test): people marked as voters;
//   - validity: 201 = vote events = marked voters, intact chain, no interrupted iterations and no 401/404/409/410/422.
// Writes the same figures to .local/load/check-run-N.json (git-ignored). Never reads or prints tokens, links or codes.
// Verifying the chain is the AUDITOR action of the console: it stores the check and, if broken, a CHAIN_BROKEN incident.
//
// Usage, from the repository root, with the stack running, after k6 and with the same session as the preparation:
//   $env:DEMO_PASSWORD = <same as prepare-voting-tokens.mjs>
//   $env:LOAD_RUN = "1"
//   node scripts/load/check-load-run.mjs
// Options: K6_ONLY=1 summarises only the k6 file (no stack needed); K6_SUMMARY=<path> to read another file.

import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { ROOT, GW, log, sleep, http, demoPassword, readInfraEnv, loadChromium, createKeycloakAdmin, createLogin } from '../demo/lib/common.mjs';

const RUN = Number(process.env.LOAD_RUN ?? 1);
if (!Number.isInteger(RUN) || RUN < 1) throw new Error('LOAD_RUN must be 1, 2, 3…');
const DIR = join(ROOT, '.local', 'load');
const K6_FILE = process.env.K6_SUMMARY ?? join(DIR, `k6-run-${RUN}.json`);
const RUN_FILE = join(DIR, `run-${RUN}.json`);
const DISCARD = [401, 404, 409, 410, 422]; // invalid preparation, not performance (carga.md)
const ADMIN = 'admin.load@verivoto.test';
const AUDITOR = 'auditor.load@verivoto.test';

// ---------- k6 summary (--summary-export) ----------
function readK6(file) {
  if (!existsSync(file)) throw new Error(`k6 summary not found: ${file} (run k6 with --summary-export)`);
  const { metrics } = JSON.parse(readFileSync(file, 'utf8'));
  const d = metrics['http_req_duration{operation:cast_vote}'];
  if (!d) throw new Error('The k6 summary has no http_req_duration{operation:cast_vote}: was it run with tests/load/voting.js?');
  const byStatus = {};
  for (const [k, v] of Object.entries(metrics)) {
    const m = k.match(/^http_reqs\{operation:cast_vote,status:(\d+)\}$/);
    if (m && v.count > 0) byStatus[m[1]] = v.count;
  }
  const round = (x) => (typeof x === 'number' ? Math.round(x * 10) / 10 : null);
  return {
    iterations: metrics.iterations?.count ?? 0,
    requests: metrics.http_reqs?.count ?? 0,
    accepted201: byStatus['201'] ?? metrics.checks?.passes ?? 0,
    byStatus,
    durationMs: { p50: round(d.med), p90: round(d['p(90)']), p95: round(d['p(95)']), p99: round(d['p(99)']), max: round(d.max), avg: round(d.avg) },
    httpReqFailedRate: metrics.http_req_failed?.value ?? null,
    checksRate: metrics.checks?.value ?? null,
    hasStatusBreakdown: Object.keys(metrics).some((k) => k.startsWith('http_reqs{operation:cast_vote,status:')),
  };
}

const k6 = readK6(K6_FILE);
const result = { run: RUN, checkedAt: new Date().toISOString(), k6 };
const problems = [];
const discard = [];
if (!k6.hasStatusBreakdown) problems.push('the k6 summary has no counts per status (old tests/load/voting.js?)');
for (const s of DISCARD) if (k6.byStatus[s]) discard.push(`${k6.byStatus[s]} responses ${s}`);

if (process.env.K6_ONLY !== '1') {
  if (!existsSync(RUN_FILE)) throw new Error(`${RUN_FILE} not found: prepare this run with prepare-voting-tokens.mjs`);
  const { processId, voters } = JSON.parse(readFileSync(RUN_FILE, 'utf8'));
  result.processId = processId;
  result.expectedVoters = voters;
  if (k6.iterations !== voters) discard.push(`k6 ran ${k6.iterations} of ${voters} iterations`);

  const PASSWORD = demoPassword();
  const kc = createKeycloakAdmin(readInfraEnv(), PASSWORD);
  let browser;
  try {
    browser = await loadChromium().launch();
    const login = createLogin(browser, PASSWORD);
    const adm = await kc.token();
    const ensurePassword = async (username) => {
      try {
        await kc.setPassword(adm, username);
      } catch (error) {
        if (error.status !== 400 || !String(error).includes('invalidPasswordHistory')) throw error;
        // Do not start a second login flow here; the normal login below validates the active password.
        log(`password reset skipped due to password history: ${username}`);
      }
    };
    await ensurePassword(ADMIN);
    await ensurePassword(AUDITOR);
    const admin = await login(ADMIN);
    const auditor = await login(AUDITOR);

    // Urn: verify now (stores the check, as in the console) and count vote events page by page
    const { data: chain } = await http('POST', `${GW}/api/v1/ballot-box/processes/${processId}/audit-events/verification`, { token: auditor });
    let voteEvents = 0; let status;
    for (let page = 0; ; page++) {
      const { data } = await http('GET', `${GW}/api/v1/ballot-box/processes/${processId}/audit-events?page=${page}&size=200`, { token: auditor });
      voteEvents += data.items.filter((e) => e.type === 'VOTE_RECEIVED').length;
      status = data.status;
      if ((page + 1) * data.size >= data.totalItems || data.items.length === 0) break;
    }
    result.urn = { status, chainIntact: chain.intact, eventsChecked: chain.eventsChecked, voteEvents,
      problems: chain.problems.map((p) => p.code) };

    // Participation: the mark may arrive a little later than the vote; wait until it matches or stops changing
    let marked = -1;
    for (let i = 0; i < 12; i++) {
      const { data } = await http('GET', `${GW}/api/v1/voter-access/processes/${processId}/participation`, { token: admin });
      const stable = data.votesCast === marked;
      marked = data.votesCast;
      result.participation = { eligibleVoters: data.eligibleVoters, votesCast: data.votesCast };
      if (marked === voteEvents || stable) break;
      await sleep(5000);
    }
  } finally {
    if (browser) await browser.close();
  }
  const { voteEvents } = result.urn;
  if (!result.urn.chainIntact) problems.push(`chain NOT intact: ${result.urn.problems.join(', ')}`);
  if (!(k6.accepted201 === voteEvents && voteEvents === result.participation.votesCast)) {
    problems.push(`counts differ: 201 = ${k6.accepted201}, vote events = ${voteEvents}, marked voters = ${result.participation.votesCast}`);
  }
}

result.discard = discard;
result.problems = problems;
result.valid = discard.length === 0 && problems.length === 0 && process.env.K6_ONLY !== '1';
mkdirSync(DIR, { recursive: true });
writeFileSync(join(DIR, `check-run-${RUN}.json`), JSON.stringify(result, null, 2), 'utf8');

const d = k6.durationMs;
const errors = Object.entries(k6.byStatus).filter(([s]) => s !== '201').map(([s, n]) => `${s}: ${n}`).join(', ') || 'none';
console.log(`
H1c · run ${RUN}${result.processId ? ` · process #${result.processId}` : ''}
- k6: ${k6.iterations} iterations, ${k6.accepted201} × 201; other responses: ${errors}
- Vote request time (ms): p50 ${d.p50} · p90 ${d.p90} · p95 ${d.p95} · p99 ${d.p99} · max ${d.max}  → p95 < 2000 ms: ${d.p95 < 2000 ? 'YES' : 'NO'}
- http_req_failed ${k6.httpReqFailedRate} · checks ${k6.checksRate}${result.urn ? `
- Urn (${result.urn.status}): chain ${result.urn.chainIntact ? 'intact' : 'BROKEN'} (${result.urn.eventsChecked} events), ${result.urn.voteEvents} vote events
- Participation: ${result.participation.votesCast} of ${result.participation.eligibleVoters} marked as voters` : '\n- K6_ONLY: urn and participation not checked'}
- ${discard.length ? `DISCARD and repeat with a new LOAD_RUN: ${discard.join('; ')}` : 'No discard condition from k6'}
- ${problems.length ? `PROBLEMS (part of the result, not a discard): ${problems.join('; ')}` : (result.urn ? 'Point 8 of carga.md: OK' : '')}
- Valid run: ${result.valid ? 'YES' : 'NO'}  (also check points 1–6 of carga.md yourself: same machine, docker stats, < 10 min since preparation)
Saved: .local/load/check-run-${RUN}.json`.replace(/\n- \n/g, '\n'));
log('done');
