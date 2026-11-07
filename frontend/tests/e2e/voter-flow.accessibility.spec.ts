import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { mkdir, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

/**
 * H1a (RNF-1), automatic part: axe on every screen and state of the voter flow (V1–V8, B1–B5, C3–C4), at 1280 and
 * 360 px, in Spanish and English. The API is replaced by synthetic responses that follow the voter-access and
 * ballot-box contracts, so the real Angular/Ionic screens are rendered without a backend. axe checks the rendered DOM;
 * it does not certify WCAG conformance and does not replace the manual review (docs/evaluation/accesibilidad-manual.md).
 * Results go to E2E_ARTIFACTS_DIR (outside the repository). Synthetic data only: no real link, code or receipt.
 */

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];
const LINK = 'S'.repeat(64);
const PROCESS_ID = 4;
const RECEIPT = '5e'.repeat(32); // synthetic receipt code with the real format (64 hex characters)
const future = (days: number) => new Date(Date.now() + days * 86_400_000).toISOString();
const past = (days: number) => new Date(Date.now() - days * 86_400_000).toISOString();

const viewports = [
  { name: 'desktop', width: 1280, height: 800 },
  { name: 'mobile', width: 360, height: 800 },
] as const;
const languages = ['es', 'en'] as const;

type Reply = { status: number; body?: unknown };
type Api = Partial<Record<'link' | 'otp' | 'verify' | 'ballot' | 'vote' | 'receipt' | 'result' | 'counted', Reply>>;

const link = (overrides: Record<string, unknown> = {}) => ({
  processId: PROCESS_ID,
  title: { es: 'Votación de prueba: actividad del club', en: 'Test vote: club activity' },
  status: 'OPEN', opensAt: past(1), closesAt: future(6), defaultLanguage: 'es', authentication: 'EMAIL_CODE',
  ...overrides,
});
const codeSent = { maskedEmail: 'p0•@verivoto.test', expiresAt: future(0.007), resendAvailableAt: future(0.0007) };
const granted = { hasVoted: false, votedOn: null, votingToken: 'synthetic-voting-token', votingTokenExpiresAt: future(0.01) };
const ballot = {
  processId: PROCESS_ID, voteType: 'SINGLE_CHOICE', blankVoteEnabled: true, closesAt: future(6),
  question: { es: '¿Qué actividad debería organizar el club el próximo mes?', en: 'Which activity should the club organise next month?' },
  options: [
    { id: 11, position: 1, text: { es: 'Ruta de senderismo', en: 'Hiking trip' } },
    { id: 12, position: 2, text: { es: 'Taller de fotografía', en: 'Photography workshop' } },
    { id: 13, position: 3, text: { es: 'Visita a un museo', en: 'Museum visit' } },
  ],
};
const result = {
  processId: PROCESS_ID, status: 'TALLIED', talliedAt: past(1), voteType: 'SINGLE_CHOICE', question: ballot.question,
  options: ballot.options.map((o, i) => ({ optionId: o.id, position: o.position, text: o.text, votes: [2, 3, 1][i], percentage: [28.57, 42.86, 14.29][i] })),
  blankVoteEnabled: true, blankVotes: 1, blankPercentage: 14.29, totalVotes: 7, eligibleVoters: 7, participation: 100,
  finalHash: 'a'.repeat(64), resultHash: 'b'.repeat(64),
};
const problem = (status: number, extra: Record<string, unknown> = {}): Reply =>
  ({ status, body: { type: 'about:blank', title: 'Synthetic', status, ...extra } });

async function mockApi(page: Page, api: Api): Promise<void> {
  const answer = (reply: Reply | undefined) => ({
    status: reply?.status ?? 500,
    contentType: reply && reply.status >= 400 ? 'application/problem+json' : 'application/json',
    body: JSON.stringify(reply?.body ?? {}),
  });
  await page.route('**/api/v1/**', (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    const method = route.request().method();
    if (path.endsWith('/otp/verify')) return route.fulfill(answer(api.verify));
    if (path.endsWith('/otp') && method === 'POST') return route.fulfill(answer(api.otp));
    if (/\/voter-access\/links\/[^/]+$/.test(path)) return route.fulfill(answer(api.link));
    if (path.endsWith('/ballot')) return route.fulfill(answer(api.ballot));
    if (path.endsWith('/votes')) return route.fulfill(answer(api.vote));
    if (path.includes('/receipts/')) return route.fulfill(answer(api.receipt));
    if (path.endsWith('/results/counted-votes')) return route.fulfill(answer(api.counted));
    if (path.endsWith('/results')) return route.fulfill(answer(api.result));
    return route.fulfill({ status: 404, contentType: 'application/problem+json', body: '{}' });
  });
}

async function chooseLanguage(page: Page, language: 'es' | 'en'): Promise<void> {
  await page.getByRole('button', { name: new RegExp(`^${language.toUpperCase()}\\b`) }).first().click();
}

async function typeCode(page: Page, code: string): Promise<void> {
  const first = page.locator('ion-input-otp input').first();
  await first.click();
  await page.keyboard.type(code);
}

/** Types the receipt code like a person (so Ionic emits ionInput) and submits it with the «Comprobar» button. */
async function checkReceipt(page: Page, value: string): Promise<void> {
  const field = page.getByRole('textbox');
  await field.click();
  await expect(field).toBeFocused();
  await page.keyboard.type(value);
  await expect(field).toHaveValue(value);
  await page.getByRole('button', { name: /^(comprobar|check)$/i }).click();
}

async function audit(page: Page, screen: string, viewport: string, language: string): Promise<void> {
  await page.waitForTimeout(400); // focus move and Ionic transitions of the new screen
  const axe = await new AxeBuilder({ page }).withTags(TAGS).analyze();
  const count = (impact: string) => axe.violations.reduce((n, v) => n + (v.impact === impact ? v.nodes.length : 0), 0);
  const summary = {
    screen, viewport, language,
    counts: { critical: count('critical'), serious: count('serious'), moderate: count('moderate'), minor: count('minor') },
    rules: axe.violations.map((v) => ({ id: v.id, impact: v.impact, nodes: v.nodes.length, help: v.help,
      targets: v.nodes.slice(0, 5).map((n) => n.target) })),
    passes: axe.passes.length, incomplete: axe.incomplete.map((v) => v.id),
    engine: axe.testEngine.version, url: new URL(page.url()).pathname.replace(LINK, ':linkToken'),
  };
  const dir = join(process.env.E2E_ARTIFACTS_DIR ?? join(tmpdir(), 'verivoto-p7-e2e'), 'voter-flow-axe');
  await mkdir(dir, { recursive: true });
  await writeFile(join(dir, `${screen}-${viewport}-${language}.json`), `${JSON.stringify(summary, null, 2)}\n`, 'utf8');
  // Sensitive values never in the URL or in storage (D-33, D-50)
  const stored = await page.evaluate(() => [...Object.keys(localStorage), ...Object.keys(sessionStorage)]
    .filter((key) => /token|receipt|otp|code/i.test(key)));
  expect.soft(stored, `${screen}: storage keys`).toEqual([]);
  expect.soft(new URL(page.url()).search, `${screen}: query string`).toBe('');
  expect.soft(summary.counts.critical + summary.counts.serious, `${screen}: ${JSON.stringify(summary.rules)}`).toBe(0);
}

/** Each scenario reaches one screen; `api` gives the synthetic answers, `go` performs the interaction. */
const scenarios: { screen: string; api: Api; go?: (page: Page) => Promise<void>; path?: string; knownDefect?: string }[] = [
  { screen: 'V1-welcome', api: { link: { status: 200, body: link() } } },
  { screen: 'B1-code', api: { link: { status: 200, body: link() }, otp: { status: 202, body: codeSent } },
    go: async (p) => { await p.getByRole('button', { name: /empezar|start/i }).click(); await p.locator('ion-input-otp').waitFor(); } },
  { screen: 'V2-code-wrong', api: { link: { status: 200, body: link() }, otp: { status: 202, body: codeSent }, verify: problem(400, { remainingAttempts: 4 }) },
    go: async (p) => { await p.getByRole('button', { name: /empezar|start/i }).click(); await typeCode(p, '000000');
      await p.locator('#code-error').waitFor(); } },
  { screen: 'B2-ballot', api: { link: { status: 200, body: link() }, otp: { status: 202, body: codeSent }, verify: { status: 200, body: granted }, ballot: { status: 200, body: ballot } },
    go: async (p) => { await p.getByRole('button', { name: /empezar|start/i }).click(); await typeCode(p, '123456');
      await p.locator('ion-radio-group').waitFor(); } },
  { screen: 'B2-ballot-missing', api: { link: { status: 200, body: link() }, otp: { status: 202, body: codeSent }, verify: { status: 200, body: granted }, ballot: { status: 200, body: ballot } },
    go: async (p) => { await p.getByRole('button', { name: /empezar|start/i }).click(); await typeCode(p, '123456');
      await p.locator('ion-radio-group').waitFor(); await p.getByRole('button', { name: /revisar|review/i }).click();
      await p.locator('#ballot-error').waitFor(); } },
  { screen: 'B3-review', api: { link: { status: 200, body: link() }, otp: { status: 202, body: codeSent }, verify: { status: 200, body: granted }, ballot: { status: 200, body: ballot } },
    go: async (p) => { await p.getByRole('button', { name: /empezar|start/i }).click(); await typeCode(p, '123456');
      await p.locator('ion-radio[value="12"]').click(); await p.getByRole('button', { name: /revisar|review/i }).click();
      await p.locator('app-review-step').waitFor(); } },
  { screen: 'B4-receipt', api: { link: { status: 200, body: link() }, otp: { status: 202, body: codeSent }, verify: { status: 200, body: granted }, ballot: { status: 200, body: ballot }, vote: { status: 201, body: { receiptCode: RECEIPT } } },
    go: async (p) => { await p.getByRole('button', { name: /empezar|start/i }).click(); await typeCode(p, '123456');
      await p.locator('ion-radio[value="12"]').click(); await p.getByRole('button', { name: /revisar|review/i }).click();
      await p.locator('app-review-step ion-button').last().click(); await p.locator('app-receipt-step').waitFor(); } },
  { screen: 'V7-not-recorded', api: { link: { status: 200, body: link() }, otp: { status: 202, body: codeSent }, verify: { status: 200, body: granted }, ballot: { status: 200, body: ballot }, vote: problem(503) },
    go: async (p) => { await p.getByRole('button', { name: /empezar|start/i }).click(); await typeCode(p, '123456');
      await p.locator('ion-radio[value="12"]').click(); await p.getByRole('button', { name: /revisar|review/i }).click();
      await p.locator('app-review-step ion-button').last().click(); await p.locator('app-error-state').waitFor(); } },
  { screen: 'V3-already-voted', api: { link: { status: 200, body: link() }, otp: { status: 202, body: codeSent }, verify: { status: 200, body: { ...granted, hasVoted: true, votedOn: past(1), votingToken: null } } },
    go: async (p) => { await p.getByRole('button', { name: /empezar|start/i }).click(); await typeCode(p, '123456'); await p.locator('app-error-state').waitFor(); } },
  { screen: 'V6-session-expired', api: { link: { status: 200, body: link() }, otp: { status: 202, body: codeSent }, verify: { status: 200, body: granted }, ballot: problem(410) },
    go: async (p) => { await p.getByRole('button', { name: /empezar|start/i }).click(); await typeCode(p, '123456'); await p.locator('app-error-state').waitFor(); } },
  { screen: 'C4-locked', api: { link: { status: 200, body: link() }, otp: { status: 202, body: codeSent }, verify: problem(423, { lockedUntil: future(0.01) }) },
    go: async (p) => { await p.getByRole('button', { name: /empezar|start/i }).click(); await typeCode(p, '123456'); await p.locator('app-error-state').waitFor(); } },
  { screen: 'V4-not-open', api: { link: { status: 200, body: link({ status: 'SCHEDULED', opensAt: future(1) }) } } },
  { screen: 'V5-closed', api: { link: { status: 200, body: link({ status: 'CLOSED', closesAt: past(1) }) } } },
  { screen: 'C3-invalid-link', api: { link: problem(404) } },
  { screen: 'C3-expired-link', api: { link: problem(410) } },
  { screen: 'SSO-institutional', api: { link: { status: 200, body: link({ authentication: 'INSTITUTIONAL_SSO' }) } },
    go: async (p) => { await p.getByRole('button', { name: /empezar|start/i }).click(); await p.locator('#sso-title').waitFor(); } },
  { screen: 'V8-results-ready', api: { link: { status: 200, body: link({ status: 'TALLIED', closesAt: past(2) }) } } },
  { screen: 'V8-results', api: { link: { status: 200, body: link({ status: 'TALLIED', closesAt: past(2) }) }, otp: { status: 202, body: codeSent },
      verify: { status: 200, body: { ...granted, votingToken: null, hasVoted: true, votedOn: past(3), resultsToken: 'synthetic-results-token', resultsTokenExpiresAt: future(0.01) } },
      result: { status: 200, body: result }, counted: problem(503) },
    go: async (p) => { await p.getByRole('button', { name: /resultado|result/i }).first().click(); await typeCode(p, '123456');
      await p.locator('app-result-summary').waitFor(); } },
  { screen: 'B5-receipt-check', path: `/recibo/${PROCESS_ID}`, api: {} },
  // DEF-06 (fixed 05/11/2026): the format error must be visible and announced (WCAG 3.3.1). This check was a known
  // failure (test.fail) while the defect was open; it now guards against a regression.
  { screen: 'B5-receipt-format-error', path: `/recibo/${PROCESS_ID}`, api: {},
    go: async (p) => { await checkReceipt(p, 'abc'); await p.waitForTimeout(800);
      expect.soft(await p.getByText(/debe tener 64|must have 64/).first().isVisible(), 'DEF-06: format error visible').toBe(true);
      expect.soft(await p.getByRole('alert').filter({ hasText: /debe tener 64|must have 64/ }).count(), 'DEF-06: error announced').toBe(1); } },
  { screen: 'B5-receipt-present', path: `/recibo/${PROCESS_ID}`, api: { receipt: { status: 200, body: { present: true } } },
    go: async (p) => { await checkReceipt(p, RECEIPT); await p.locator('p.result').waitFor(); } },
  { screen: 'B5-receipt-absent', path: `/recibo/${PROCESS_ID}`, api: { receipt: { status: 200, body: { present: false } } },
    go: async (p) => { await checkReceipt(p, RECEIPT); await p.locator('p.result').waitFor(); } },
];

for (const viewport of viewports) {
  for (const language of languages) {
    for (const scenario of scenarios) {
      test(`H1a axe ${scenario.screen} · ${viewport.width}px · ${language}`, async ({ page }) => {
        test.fail(Boolean(scenario.knownDefect), `${scenario.knownDefect ?? ''} open (docs/evaluation/resultados-p7/defectos.md)`);
        await page.setViewportSize({ width: viewport.width, height: viewport.height });
        await mockApi(page, scenario.api);
        await page.goto(scenario.path ?? `/votar/${LINK}`);
        await chooseLanguage(page, language);
        await page.locator('main h1, h1').first().waitFor();
        if (scenario.go) await scenario.go(page);
        await audit(page, scenario.screen, viewport.name, language);
      });
    }
  }
}
