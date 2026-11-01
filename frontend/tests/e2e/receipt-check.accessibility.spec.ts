import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';
import { mkdir, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const viewportCases = [
  { name: 'desktop', width: 1280, height: 800 },
  { name: 'mobile', width: 360, height: 800 },
] as const;

const languageCases = [
  { code: 'es', heading: 'Comprobar mi recibo' },
  { code: 'en', heading: 'Check my receipt' },
] as const;

for (const viewport of viewportCases) {
  for (const language of languageCases) {
    test(`receipt check initial state: ${viewport.name}, ${language.code}`, async ({ page }) => {
      const consoleErrors: string[] = [];
      page.on('console', (message) => {
        if (message.type() === 'error') consoleErrors.push(message.text());
      });
      page.on('pageerror', (error) => consoleErrors.push(error.name));

      await page.setViewportSize({ width: viewport.width, height: viewport.height });
      await page.goto('/recibo/7');

      await page.locator(`ion-segment-button[value="${language.code}"]`).click();

      await expect(page.getByRole('heading', { name: language.heading })).toBeVisible();

      const axe = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'])
        .analyze();
      const summary = {
        screen: 'receipt-check',
        state: 'initial',
        viewport: viewport.name,
        language: language.code,
        counts: {
          critical: axe.violations.reduce((total, item) => total + (item.impact === 'critical' ? item.nodes.length : 0), 0),
          serious: axe.violations.reduce((total, item) => total + (item.impact === 'serious' ? item.nodes.length : 0), 0),
          moderate: axe.violations.reduce((total, item) => total + (item.impact === 'moderate' ? item.nodes.length : 0), 0),
          minor: axe.violations.reduce((total, item) => total + (item.impact === 'minor' ? item.nodes.length : 0), 0),
        },
        rules: axe.violations.map((item) => item.id),
        violations: axe.violations.map((item) => ({
          id: item.id,
          impact: item.impact,
          help: item.help,
          nodes: item.nodes.map((node) => ({
            target: node.target,
            html: node.html,
            failureSummary: node.failureSummary,
          })),
        })),
      };
      const artifactDir = process.env.E2E_ARTIFACTS_DIR ?? join(tmpdir(), 'verivoto-p7-e2e');
      await mkdir(artifactDir, { recursive: true });
      await writeFile(join(artifactDir, `${viewport.name}-${language.code}-axe.json`),
        `${JSON.stringify(summary, null, 2)}\n`, 'utf8');

      await page.keyboard.press('Tab');
      const focusIsVisible = await page.evaluate(() => {
        const focused = document.activeElement;
        if (!focused || focused === document.body) return false;
        const bounds = focused.getBoundingClientRect();
        return bounds.width > 0 && bounds.height > 0;
      });
      expect(focusIsVisible).toBe(true);

      const sensitiveStorageKeys = await page.evaluate(() =>
        [...Object.keys(localStorage), ...Object.keys(sessionStorage)]
          .filter((key) => /token|receipt|otp|link/i.test(key)));
      expect(sensitiveStorageKeys).toEqual([]);
      expect(new URL(page.url()).search).toBe('');
      expect(new URL(page.url()).hash).toBe('');
      expect(consoleErrors).toEqual([]);
      expect(summary.counts.critical + summary.counts.serious, JSON.stringify(summary)).toBe(0);
    });
  }
}
