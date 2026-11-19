import { test as base, type Page } from '@playwright/test';
import { createServer } from 'node:http';
import type { AddressInfo } from 'node:net';

/**
 * Synthetic API for the E2E accessibility sweeps (IMP-63). The same resolver answers in two places:
 * 1. `page.route`, as before; it catches every request in Chromium.
 * 2. A small HTTP server per worker. In Firefox and WebKit some POST requests of the app skip `page.route` and reach
 *    the dev server proxy; for the E2E run that proxy points to `mock-router.mjs`, which forwards each request to the
 *    worker named by the `verivoto-e2e-mock` cookie. So every engine renders the screen with the same synthetic answer.
 * Synthetic data only: no real link, code, receipt or identity.
 */
export type MockReply = { status: number; contentType?: string; body?: string; headers?: Record<string, string> };
export type MockResolver = (method: string, url: URL) => MockReply;

export const MOCK_COOKIE = 'verivoto-e2e-mock';

type Fallback = { port: number; serve: (resolver: MockResolver | null) => void };
type Fixtures = { mockApi: (page: Page, resolver: MockResolver) => Promise<void> };

export const test = base.extend<Fixtures, { apiFallback: Fallback }>({
  apiFallback: [
    // eslint-disable-next-line no-empty-pattern
    async ({}, use) => {
      let current: MockResolver | null = null;
      const server = createServer((req, res) => {
        req.resume();
        const url = new URL(req.url ?? '/', 'http://127.0.0.1');
        const reply = current ? current(req.method ?? 'GET', url) : { status: 502, body: 'No E2E scenario is active' };
        res.writeHead(reply.status, { 'content-type': reply.contentType ?? 'application/json', ...reply.headers });
        res.end(reply.body ?? '');
      });
      await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
      await use({ port: (server.address() as AddressInfo).port, serve: (resolver) => { current = resolver; } });
      await new Promise((resolve) => server.close(resolve));
    },
    { scope: 'worker' },
  ],
  mockApi: async ({ apiFallback, baseURL }, use) => {
    await use(async (page, resolver) => {
      apiFallback.serve(resolver);
      await page.context().addCookies([{ name: MOCK_COOKIE, value: String(apiFallback.port), url: baseURL ?? 'http://127.0.0.1:4300' }]);
      await page.route('**/api/v1/**', (route) => {
        const request = route.request();
        return route.fulfill(resolver(request.method(), new URL(request.url())));
      });
    });
    apiFallback.serve(null);
  },
});
