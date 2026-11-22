import { defineConfig, devices } from '@playwright/test';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const artifactsDir = process.env.E2E_ARTIFACTS_DIR ?? join(tmpdir(), 'verivoto-p7-e2e');

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  reporter: [
    ['list'],
    ['html', { outputFolder: join(artifactsDir, 'playwright-report'), open: 'never' }],
  ],
  outputDir: join(artifactsDir, 'test-results'),
  use: {
    // Own port (not 4200), so a running `npm start` with the real proxy is never reused for the sweep
    baseURL: process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4300',
    trace: 'retain-on-failure',
    ...devices['Desktop Chrome'],
  },
  // RNF-2 (mejora M2): E2E_BROWSERS=all adds Firefox and WebKit (Safari engine); by default, and in CI, only Chromium.
  // Install them once with: npx playwright install firefox webkit
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    ...(process.env.E2E_BROWSERS === 'all'
      ? [
          // Slower engines on Windows: more time per test; run them with few workers (see PLAN.md, M2)
          { name: 'firefox', timeout: 90_000, use: { ...devices['Desktop Firefox'] } },
          { name: 'webkit', timeout: 90_000, use: { ...devices['Desktop Safari'] } },
        ]
      : []),
  ],
  // IMP-63: the dev server proxies /api to mock-router.mjs, so requests that skip `page.route` (POST in Firefox and
  // WebKit) still get the synthetic answer of the test (tests/e2e/support/api-mock.ts). Never a real backend.
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : [
        {
          command: 'node tests/e2e/support/mock-router.mjs',
          url: 'http://127.0.0.1:4310/__health',
          reuseExistingServer: !process.env.CI,
          timeout: 30_000,
        },
        {
          command: 'npm run start -- --host 127.0.0.1 --port 4300 --proxy-config proxy.e2e.conf.json',
          url: 'http://127.0.0.1:4300',
          reuseExistingServer: !process.env.CI,
          timeout: 120_000,
        },
      ],
});
