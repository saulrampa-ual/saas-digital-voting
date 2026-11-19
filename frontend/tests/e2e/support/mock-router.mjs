// IMP-63: target of the dev server proxy during the E2E run (proxy.e2e.conf.json). It forwards each /api request to
// the Playwright worker named by the `verivoto-e2e-mock` cookie (see api-mock.ts). It never reaches a real backend.
import { createServer, request } from 'node:http';

const port = Number(process.env.E2E_MOCK_PORT ?? 4310);

createServer((req, res) => {
  if (req.url === '/__health') {
    res.writeHead(200, { 'content-type': 'text/plain' });
    res.end('ok');
    return;
  }
  const worker = /(?:^|;\s*)verivoto-e2e-mock=(\d+)/.exec(req.headers.cookie ?? '')?.[1];
  if (!worker) {
    res.writeHead(502, { 'content-type': 'text/plain' });
    res.end('No E2E mock cookie');
    return;
  }
  const upstream = request({ host: '127.0.0.1', port: Number(worker), method: req.method, path: req.url, headers: req.headers },
    (answer) => {
      res.writeHead(answer.statusCode ?? 502, answer.headers);
      answer.pipe(res);
    });
  upstream.on('error', () => {
    if (!res.headersSent) res.writeHead(502, { 'content-type': 'text/plain' });
    res.end('E2E worker not reachable');
  });
  req.pipe(upstream);
}).listen(port, '127.0.0.1');
