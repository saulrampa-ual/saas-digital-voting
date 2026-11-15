import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { createHash } from 'node:crypto';
import { mkdir, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

/**
 * H1a (RNF-1), automatic part, second block (A4): axe on the management console (every page and role) and on the
 * public results page (V8, D-16), at 1280 and 360 px, in Spanish and English. Keycloak and the API are replaced by
 * synthetic responses that follow the contracts, so the real Angular/Ionic screens are rendered without a backend.
 * The Keycloak login screens themselves are not covered here (they are Keycloak templates, measured on 03/11/2026).
 * axe checks the rendered DOM; it does not certify WCAG conformance. Results go to E2E_ARTIFACTS_DIR (outside the
 * repository). Synthetic data only.
 */

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];
const KEYCLOAK = 'http://localhost:8080/realms/verivoto/protocol/openid-connect';
const ORG = 'DEMOUAL'; // identifier format of identity-service: 1–20 letters or digits
const PUBLIC_ID = 'pub-' + 'c'.repeat(28);
const day = 86_400_000;
const at = (days: number) => new Date(Date.now() + days * day).toISOString();
const sha = (text: string) => createHash('sha256').update(text).digest('hex');

type Role = 'SUPERADMIN' | 'ADMIN' | 'CENSUS_MANAGER' | 'AUDITOR';

const viewports = [
  { name: 'desktop', width: 1280, height: 800 },
  { name: 'mobile', width: 360, height: 800 },
] as const;
const languages = ['es', 'en'] as const;

// ---------- Synthetic data (demo-like, no real people) ----------

const t = (es: string, en: string) => ({ es, en });
const censuses = [
  { id: 1, name: 'Estudiantado de la Escuela (sintético)', voterCount: 7, frozen: true, createdAt: at(-20) },
  { id: 2, name: 'Personal de prueba', voterCount: 0, frozen: false, createdAt: at(-2) },
];
const summaries = [
  { id: 5, title: t('Borrador: delegación de curso', 'Draft: class delegate'), status: 'DRAFT', opensAt: at(5), closesAt: at(9) },
  { id: 4, title: t('Programada: comisión de biblioteca', 'Scheduled: library committee'), status: 'SCHEDULED', opensAt: at(2), closesAt: at(6) },
  { id: 3, title: t('Representantes en Junta de Centro', 'Representatives on the School Board'), status: 'OPEN', opensAt: at(-3), closesAt: at(4) },
  { id: 2, title: t('Actividad del club', 'Club activity'), status: 'TALLIED', opensAt: at(-12), closesAt: at(-5) },
  { id: 1, title: t('Consulta archivada', 'Archived consultation'), status: 'ARCHIVED', opensAt: at(-40), closesAt: at(-33) },
];
const options = [
  { id: 11, position: 1, text: t('Ruta de senderismo', 'Hiking trip') },
  { id: 12, position: 2, text: t('Taller de fotografía', 'Photography workshop') },
  { id: 13, position: 3, text: t('Visita a un museo', 'Museum visit') },
];
const processOf = (id: number) => {
  const s = summaries.find((p) => p.id === id) ?? summaries[2];
  return {
    ...s, censusId: 1, description: t('Proceso sintético para la evaluación.', 'Synthetic process for the evaluation.'),
    defaultLanguage: 'es', resultsVisibility: id === 2 ? 'PUBLIC_LINK' : 'CENSUS_ONLY', voterAuthentication: 'EMAIL_CODE',
    resultsPublicId: id === 2 ? PUBLIC_ID : null,
    ballot: { voteType: 'SINGLE_CHOICE', question: t('¿Qué actividad debería organizar el club?', 'Which activity should the club organise?'),
      blankVoteEnabled: true, options },
  };
};
const history = (id: number) => [
  { from: 'DRAFT', to: 'SCHEDULED', changedAt: at(-6), changedBy: 'Admin Demo' },
  ...(id <= 3 ? [{ from: 'SCHEDULED', to: 'OPEN', changedAt: at(-3), changedBy: 'system' }] : []),
];

// Counted votes and result with the same hashes the ballot box would publish (verifier passes)
const finalHash = sha('final');
const votes = [
  ['0a', 11], ['1b', 11], ['2c', 12], ['3d', 12], ['4e', 12], ['5f', 13], ['6a', null],
].map(([v, o]) => ({ voteId: `${v}`.padEnd(36, '0'), optionId: o as number | null }));
const votesHash = sha([...votes].sort((a, b) => (a.voteId < b.voteId ? -1 : 1)).map((v) => `${v.voteId}:${v.optionId ?? 'blank'}`).join('\n'));
const resultHash = sha(['result', 2, finalHash, 7, 7, 1, '11:2', '12:3', '13:1', votesHash].join('|'));
const result = {
  processId: 2, status: 'TALLIED', talliedAt: at(-5), voteType: 'SINGLE_CHOICE', question: processOf(2).ballot.question,
  options: options.map((o, i) => ({ optionId: o.id, position: o.position, text: o.text, votes: [2, 3, 1][i], percentage: [28.57, 42.86, 14.29][i] })),
  blankVoteEnabled: true, blankVotes: 1, blankPercentage: 14.29, totalVotes: 7, eligibleVoters: 7, participation: 100,
  finalHash, resultHash,
};
const counted = { processId: 2, finalHash, resultHash, eligibleVoters: 7, optionIds: [11, 12, 13], votes };
const participation = (id: number) => ({
  processId: id, status: 'OPEN', opensAt: at(-3), closesAt: at(4), eligibleVoters: 7, votesCast: 4, pending: 3, participation: 57.14,
  byDay: [-3, -2, -1, 0].map((d, i) => ({ day: at(d).slice(0, 10), votes: [1, 2, 0, 1][i], cumulative: [1, 3, 3, 4][i] })),
});
const notifications = (id: number) => ({
  processId: id, status: 'OPEN', opensAt: at(-3), closesAt: at(4),
  counts: {
    INVITATION: { pending: 0, delivered: 6, failed: 1, cancelled: 0 },
    REMINDER: { pending: 1, delivered: 0, failed: 0, cancelled: 0 },
    CLOSING: { pending: 0, delivered: 0, failed: 0, cancelled: 0 },
  },
  failed: [{ type: 'INVITATION', displayName: 'Persona Sintética 7', email: 'p07@verivoto.test', attempts: 3, reason: 'Mailbox unavailable', createdAt: at(-3) }],
  reminders: [{ id: 1, sendAt: at(1), status: 'SCHEDULED', recipients: null, createdAt: at(-1) }],
  maxReminders: 3,
});
const chainCheck = (id: number) => ({ processId: id, intact: true, eventsChecked: 10, checkedAt: at(-1), finalHash, problems: [] });
const auditEvents = (id: number) => {
  const types = ['OPENED', ...Array(7).fill('VOTE_RECEIVED'), 'CLOSED', 'FINAL_HASH_PUBLISHED'];
  let previous = '0'.repeat(64);
  const items = types.map((type, i) => {
    const eventHash = sha(`${id}-${i}`);
    const event = { sequence: i + 1, type, occurredAt: type === 'VOTE_RECEIVED' ? null : at(-12 + i), payloadHash: sha(`p${i}`), previousHash: previous, eventHash };
    previous = eventHash;
    return event;
  });
  return { items, page: 0, size: 20, totalItems: items.length, status: 'TALLIED', closesAt: at(-5), finalHash };
};
const voters = {
  items: Array.from({ length: 7 }, (_, i) => ({ id: i + 1, fullName: `Persona Sintética ${i + 1}`, email: `p0${i + 1}@verivoto.test`,
    phone: null, institutionalId: i % 2 ? null : `SINT-${1000 + i}`, preferredLanguage: i % 3 ? 'es' : 'en' })),
  page: 0, size: 20, totalItems: 7,
};
const adminActions = {
  items: [
    { occurredAt: at(-1), actorId: 'u1', actorName: 'Admin Demo', action: 'PROCESS_SCHEDULED', objectType: 'PROCESS', objectId: '4', objectLabel: 'Comisión de biblioteca', detail: null, processId: 4, source: 'CONSOLE' },
    { occurredAt: at(-2), actorId: 'u2', actorName: 'Gestora de censo Demo', action: 'VOTERS_IMPORTED', objectType: 'CENSUS', objectId: '1', objectLabel: 'Estudiantado', detail: '7', processId: null, source: 'CONSOLE' },
    { occurredAt: at(-5), actorId: 'u3', actorName: 'Auditor Demo', action: 'CHAIN_VERIFIED', objectType: 'PROCESS', objectId: '2', objectLabel: 'Actividad del club', detail: null, processId: 2, source: 'CONSOLE' },
  ],
  page: 0, size: 20, totalItems: 3,
};
const actors = [{ actorId: 'u1', actorName: 'Admin Demo' }, { actorId: 'u2', actorName: 'Gestora de censo Demo' }, { actorId: 'u3', actorName: 'Auditor Demo' }];
const apiKeys = {
  items: [
    { id: 1, name: 'Integración de prueba', clientId: 'vv-api-0001', createdByName: 'Admin Demo', createdAt: at(-10), revokedAt: null, active: true },
    { id: 2, name: 'Clave antigua', clientId: 'vv-api-0002', createdByName: 'Admin Demo', createdAt: at(-30), revokedAt: at(-15), active: false },
  ],
  maxActive: 5,
};
const privacySubject = {
  fullName: 'Persona Sintética 3',
  entries: [{ census: 'Estudiantado de la Escuela (sintético)', fullName: 'Persona Sintética 3', email: 'p03@verivoto.test', phone: null,
    institutionalId: 'SINT-1002', preferredLanguage: 'es', addedAt: at(-20), erasure: 'ANONYMIZE',
    processes: [{ id: 2, title: processOf(2).title, status: 'TALLIED' }, { id: 3, title: processOf(3).title, status: 'OPEN' }] }],
  erasureAllowed: false, blockingProcesses: [{ id: 3, title: processOf(3).title, status: 'OPEN' }], toDelete: 0, toAnonymize: 1,
};
const organizations = {
  items: [
    { id: ORG, name: 'Demo UAL (sintética)', status: 'ACTIVE' },
    { id: 'ASOCPRUEBA', name: 'Asociación de prueba', status: 'SUSPENDED' },
  ],
  page: 0, size: 50, totalItems: 2,
};

// ---------- Mocks ----------

const b64 = (value: object) => Buffer.from(JSON.stringify(value)).toString('base64url');

async function mockKeycloak(page: Page, role: Role): Promise<void> {
  let nonce = '';
  await page.route(`${KEYCLOAK}/auth**`, (route) => {
    const url = new URL(route.request().url());
    nonce = url.searchParams.get('nonce') ?? '';
    const target = `${url.searchParams.get('redirect_uri')}#state=${url.searchParams.get('state')}&session_state=s&code=synthetic-code`;
    return route.fulfill({ status: 200, contentType: 'text/html', body: `<script>location.replace(${JSON.stringify(target)})</script>` });
  });
  await page.route(`${KEYCLOAK}/token`, (route) => {
    const now = Math.floor(Date.now() / 1000);
    const claims = {
      exp: now + 3600, iat: now, nonce, typ: 'Bearer', azp: 'verivoto-admin', sub: `synthetic-${role.toLowerCase()}`,
      roles: [role], name: `${role} Demo`, preferred_username: role.toLowerCase(),
      ...(role === 'SUPERADMIN' ? {} : { organization_id: ORG }),
    };
    const jwt = (extra: object = {}) => `${b64({ alg: 'none', typ: 'JWT' })}.${b64({ ...claims, ...extra })}.c2ln`;
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
      access_token: jwt(), id_token: jwt({ typ: 'ID' }), refresh_token: jwt({ typ: 'Refresh' }),
      token_type: 'Bearer', expires_in: 3600, refresh_expires_in: 3600, session_state: 's', scope: 'openid',
    }) });
  });
}

async function mockApi(page: Page): Promise<void> {
  await page.route('**/api/v1/**', (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace(/^\/api\/v1/, '');
    const method = route.request().method();
    const ok = (body: unknown) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) });
    const problem = (status: number) => route.fulfill({ status, contentType: 'application/problem+json',
      body: JSON.stringify({ type: 'about:blank', title: 'Synthetic', status }) });
    let m: RegExpMatchArray | null;
    if (path === '/organizations') return ok(organizations);
    if ((m = path.match(/^\/organizations\/([^/]+)\/users$/))) return ok({ items: [{ role: 'ADMIN' }, { role: 'CENSUS_MANAGER' }, { role: 'AUDITOR' }], page: 0, size: 100, totalItems: 3 });
    if ((m = path.match(/^\/organizations\/([^/]+)$/))) return ok(organizations.items.find((o) => o.id === decodeURIComponent(m![1])) ?? organizations.items[0]);
    if (path === '/processes') return ok(summaries);
    if ((m = path.match(/^\/processes\/(\d+)\/status-history$/))) return ok(history(+m[1]));
    if ((m = path.match(/^\/processes\/(\d+)$/))) return ok(processOf(+m[1]));
    if (path === '/censuses') return ok(censuses);
    if ((m = path.match(/^\/censuses\/(\d+)\/voters$/))) return ok(+m[1] === 1 ? voters : { items: [], page: 0, size: 20, totalItems: 0 });
    if ((m = path.match(/^\/censuses\/(\d+)$/))) return ok(censuses.find((c) => c.id === +m![1]) ?? censuses[0]);
    if ((m = path.match(/^\/voter-access\/processes\/(\d+)\/participation$/))) return ok(participation(+m[1]));
    if ((m = path.match(/^\/voter-access\/processes\/(\d+)\/notifications$/))) return ok(notifications(+m[1]));
    if ((m = path.match(/^\/ballot-box\/processes\/(\d+)\/results\/counted-votes$/))) return +m[1] === 2 ? ok(counted) : problem(409);
    if ((m = path.match(/^\/ballot-box\/processes\/(\d+)\/results$/))) return +m[1] === 2 ? ok(result) : problem(409);
    if ((m = path.match(/^\/ballot-box\/processes\/(\d+)\/audit-events\/verification$/))) return ok(chainCheck(+m[1]));
    if ((m = path.match(/^\/ballot-box\/processes\/(\d+)\/audit-events$/))) return ok(auditEvents(+m[1]));
    if (path === `/ballot-box/public/results/${PUBLIC_ID}/counted-votes`) return ok(counted);
    if (path === `/ballot-box/public/results/${PUBLIC_ID}`) return ok(result);
    if (path === '/admin-actions/actors') return ok(actors);
    if (path === '/admin-actions') return ok(adminActions);
    if (path === '/api-keys' && method === 'GET') return ok(apiKeys);
    if (path === '/privacy/search' && method === 'POST') return ok(privacySubject);
    return problem(404);
  });
}

// ---------- Audit ----------

async function chooseLanguage(page: Page, language: 'es' | 'en'): Promise<void> {
  await page.getByRole('button', { name: new RegExp(`^${language.toUpperCase()}\\b`) }).first().click();
}

async function audit(page: Page, screen: string, viewport: string, language: string): Promise<void> {
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(500); // Ionic transitions and focus move of the new page
  const axe = await new AxeBuilder({ page }).withTags(TAGS).analyze();
  const count = (impact: string) => axe.violations.reduce((n, v) => n + (v.impact === impact ? v.nodes.length : 0), 0);
  const summary = {
    screen, viewport, language,
    counts: { critical: count('critical'), serious: count('serious'), moderate: count('moderate'), minor: count('minor') },
    rules: axe.violations.map((v) => ({ id: v.id, impact: v.impact, nodes: v.nodes.length, help: v.help,
      targets: v.nodes.slice(0, 5).map((n) => n.target) })),
    passes: axe.passes.length, incomplete: axe.incomplete.map((v) => v.id),
    engine: axe.testEngine.version, url: new URL(page.url()).pathname + new URL(page.url()).search,
  };
  const dir = join(process.env.E2E_ARTIFACTS_DIR ?? join(tmpdir(), 'verivoto-p7-e2e'), 'admin-console-axe');
  await mkdir(dir, { recursive: true });
  await writeFile(join(dir, `${screen}-${viewport}-${language}.json`), `${JSON.stringify(summary, null, 2)}\n`, 'utf8');
  if (process.env.E2E_SCREENSHOTS) await page.screenshot({ path: join(dir, `${screen}-${viewport}-${language}.png`), fullPage: true });
  expect.soft(summary.counts.critical + summary.counts.serious, `${screen}: ${JSON.stringify(summary.rules)}`).toBe(0);
}

// ---------- Scenarios ----------

/** `knownDefect`: combinations that fail because of an open defect (docs/evaluation/resultados-p7/defectos.md). */
type Known = { id: string; at: string[] };
type Scenario = { screen: string; role?: Role; path: string; ready: string; go?: (page: Page) => Promise<void>; knownDefect?: Known };

const scenarios: Scenario[] = [
  { screen: 'E0-home-superadmin', role: 'SUPERADMIN', path: '/admin/inicio', ready: 'app-admin-home' },
  { screen: 'E2-organizations', role: 'SUPERADMIN', path: '/admin/organizaciones', ready: 'table, ul, app-admin-page' },
  { screen: 'E0-home-admin', role: 'ADMIN', path: '/admin/inicio', ready: 'app-admin-home' },
  { screen: 'E0-home-admin-menu-open', role: 'ADMIN', path: '/admin/inicio', ready: 'app-admin-home',
    go: async (p) => { await p.locator('#admin-nav button.toggle').click(); await p.locator('#admin-nav.expanded').waitFor(); } },
  { screen: 'E0-home-census-manager', role: 'CENSUS_MANAGER', path: '/admin/inicio', ready: 'app-admin-home' },
  { screen: 'E0-home-auditor', role: 'AUDITOR', path: '/admin/inicio', ready: 'app-admin-home' },
  { screen: 'C1-censuses', role: 'CENSUS_MANAGER', path: '/admin/censos', ready: 'app-admin-page' },
  { screen: 'C2-census-detail', role: 'CENSUS_MANAGER', path: '/admin/censos/1', ready: 'app-admin-page' },
  { screen: 'C2-census-empty', role: 'ADMIN', path: '/admin/censos/2', ready: 'app-admin-page' },
  { screen: 'A1-processes', role: 'ADMIN', path: '/admin/procesos', ready: 'app-admin-page' },
  { screen: 'A3-process-new', role: 'ADMIN', path: '/admin/procesos/nuevo', ready: 'nav.steps' },
  { screen: 'A3-process-new-errors', role: 'ADMIN', path: '/admin/procesos/nuevo', ready: 'nav.steps',
    go: async (p) => { await p.getByRole('button', { name: /^(siguiente|next)/i }).click(); await p.locator('.field-error').first().waitFor(); } },
  { screen: 'A4-process-edit', role: 'ADMIN', path: '/admin/procesos/5/editar', ready: 'nav.steps' },
  { screen: 'A4-process-edit-ballot', role: 'ADMIN', path: '/admin/procesos/5/editar', ready: 'nav.steps',
    go: async (p) => { for (let i = 0; i < 3; i++) { await p.getByRole('button', { name: /^(siguiente|next)/i }).click(); await p.waitForTimeout(300); }
      await p.getByRole('heading', { level: 2, name: /papeleta|ballot/i }).waitFor(); } },
  { screen: 'A2-process-detail-open', role: 'ADMIN', path: '/admin/procesos/3', ready: 'app-admin-page' },
  { screen: 'A2-process-detail-tallied', role: 'AUDITOR', path: '/admin/procesos/2', ready: 'app-admin-page' },
  { screen: 'A5-participation', role: 'ADMIN', path: '/admin/participacion?proceso=3', ready: 'app-admin-page' },
  { screen: 'F1-results', role: 'ADMIN', path: '/admin/resultados?proceso=2', ready: 'app-admin-page' },
  { screen: 'F1-results-not-tallied', role: 'AUDITOR', path: '/admin/resultados?proceso=3', ready: 'app-admin-page' },
  { screen: 'H1-audit', role: 'AUDITOR', path: '/admin/auditoria?proceso=2', ready: 'app-admin-page' },
  { screen: 'H2-traceability', role: 'ADMIN', path: '/admin/trazabilidad', ready: 'app-admin-page' },
  { screen: 'G1-api-keys', role: 'ADMIN', path: '/admin/claves-api', ready: 'app-admin-page' },
  { screen: 'G2-privacy', role: 'ADMIN', path: '/admin/privacidad', ready: 'app-admin-page' },
  { screen: 'G2-privacy-found', role: 'ADMIN', path: '/admin/privacidad', ready: 'app-admin-page',
    go: async (p) => { await p.locator('form.find input').fill('p03@verivoto.test'); await p.locator('form.find ion-button').click();
      await p.getByText('Persona Sintética 3').first().waitFor(); } },
  { screen: 'E9-forbidden', role: 'CENSUS_MANAGER', path: '/admin/sin-permiso', ready: 'app-admin-page, main' },
  { screen: 'V8-public-results', path: `/resultados/${PUBLIC_ID}`, ready: 'main' },
  { screen: 'X-not-found', path: '/no-existe', ready: 'app-not-found' },
];

for (const viewport of viewports) {
  for (const language of languages) {
    for (const scenario of scenarios) {
      test(`H1a axe ${scenario.screen} · ${viewport.width}px · ${language}`, async ({ page }) => {
        const known = scenario.knownDefect;
        test.fail(Boolean(known?.at.includes(`${viewport.name}-${language}`)), `${known?.id ?? ''} open (docs/evaluation/resultados-p7/defectos.md)`);
        await page.setViewportSize({ width: viewport.width, height: viewport.height });
        if (scenario.role) await mockKeycloak(page, scenario.role);
        await mockApi(page);
        await page.addInitScript((lang) => localStorage.setItem('verivoto.language', lang), language);
        await page.goto(scenario.path);
        await page.locator('h1').first().waitFor();
        // the 404 page has no language switcher: it follows the stored language
        if (await page.getByRole('button', { name: /^ES\b/ }).count()) await chooseLanguage(page, language);
        await page.locator(scenario.ready).first().waitFor();
        await page.waitForLoadState('networkidle');
        if (scenario.go) await scenario.go(page);
        await audit(page, scenario.screen, viewport.name, language);
      });
    }
  }
}
