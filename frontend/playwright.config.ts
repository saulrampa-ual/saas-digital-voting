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
    baseURL: process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4200',
    trace: 'retain-on-failure',
    ...devices['Desktop Chrome'],
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        command: 'npm run start -- --host 127.0.0.1 --port 4200',
        url: 'http://127.0.0.1:4200',
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
      },
});
