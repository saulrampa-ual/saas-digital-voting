// Local development only. Sets the secret of each confidential client of infra/keycloak/realm/verivoto-realm.json
// (the four service accounts) in the realm "verivoto" to the value of infra/.env, which is what the services send.
// Needed when the realm was imported with other secrets: the services then get 401 "Invalid client credentials"
// from Keycloak. Prints client ids only, never secrets or tokens. Nothing else of the clients changes.
//
// Usage, from the repository root, with Keycloak running and infra/.env present:
//   node scripts/dev/keycloak-sync-secrets.mjs
// Then restart the services (scripts/dev/stop-services.ps1 and start-services.ps1).

import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { ROOT, KC, REALM, log, http, readInfraEnv } from '../demo/lib/common.mjs';

const env = readInfraEnv();
const file = JSON.parse(readFileSync(join(ROOT, 'infra', 'keycloak', 'realm', 'verivoto-realm.json'), 'utf8'));
const confidential = file.clients
  .map((c) => ({ clientId: c.clientId, variable: /^\$\{([A-Z0-9_]+)\}$/.exec(c.secret ?? '')?.[1] }))
  .filter((c) => c.variable);

const { data: tokenData } = await http('POST', `${KC}/realms/master/protocol/openid-connect/token`, { form: {
  grant_type: 'password', client_id: 'admin-cli',
  username: env.KC_BOOTSTRAP_ADMIN_USERNAME || 'local-admin', password: env.KC_BOOTSTRAP_ADMIN_PASSWORD } });
const token = tokenData.access_token;
const api = `${KC}/admin/realms/${REALM}`;

for (const { clientId, variable } of confidential) {
  const secret = env[variable];
  if (!secret) throw new Error(`${variable} is empty in infra/.env`);
  const { data: found } = await http('GET', `${api}/clients?clientId=${encodeURIComponent(clientId)}`, { token });
  if (!found.length) throw new Error(`Client not found: ${clientId} (run keycloak-missing-clients.mjs first)`);
  const { data: current } = await http('GET', `${api}/clients/${found[0].id}/client-secret`, { token });
  if (current?.value === secret) {
    log(`already matches infra/.env: ${clientId}`);
    continue;
  }
  await http('PUT', `${api}/clients/${found[0].id}`, { token, body: { ...found[0], secret } });
  log(`secret set from infra/.env (${variable}): ${clientId}`);
}
log('done');
