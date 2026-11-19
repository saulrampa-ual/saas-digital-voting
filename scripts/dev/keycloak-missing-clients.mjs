// Local development only. Creates in the realm "verivoto" the clients of infra/keycloak/realm/verivoto-realm.json that
// the realm does not have yet (Keycloak only imports that file when the realm does not exist, so a realm imported
// before a client was added to the file lacks it), with their service-account roles. Existing clients are never
// changed. Secrets come from infra/.env, as in the import; the script prints client ids only, never secrets or tokens.
//
// Usage, from the repository root, with Keycloak running (infra/compose.yaml) and infra/.env present:
//   node scripts/dev/keycloak-missing-clients.mjs
// Then: node scripts/dev/keycloak-api-audience.mjs, and restart the services (start-services.ps1) so they get new tokens.

import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { ROOT, KC, REALM, log, http, readInfraEnv } from '../demo/lib/common.mjs';

const env = readInfraEnv();
const raw = readFileSync(join(ROOT, 'infra', 'keycloak', 'realm', 'verivoto-realm.json'), 'utf8');
const realm = JSON.parse(raw.replace(/\$\{([A-Z0-9_]+)\}/g, (match, name) => {
  if (!(name in env)) throw new Error(`${name} is not defined in infra/.env`);
  return JSON.stringify(env[name]).slice(1, -1);
}));

const { data: tokenData } = await http('POST', `${KC}/realms/master/protocol/openid-connect/token`, { form: {
  grant_type: 'password', client_id: 'admin-cli',
  username: env.KC_BOOTSTRAP_ADMIN_USERNAME || 'local-admin', password: env.KC_BOOTSTRAP_ADMIN_PASSWORD } });
const token = tokenData.access_token;
const api = `${KC}/admin/realms/${REALM}`;

const { data: existing } = await http('GET', `${api}/clients?first=0&max=1000`, { token });
const missing = realm.clients.filter((c) => !existing.some((e) => e.clientId === c.clientId));
if (!missing.length) log('no client missing');

for (const client of missing) {
  await http('POST', `${api}/clients`, { token, body: client });
  log(`client created: ${client.clientId}`);
  if (!client.serviceAccountsEnabled) continue;
  const roles = realm.users?.find((u) => u.serviceAccountClientId === client.clientId)?.realmRoles ?? [];
  if (!roles.length) continue;
  const { data: created } = await http('GET', `${api}/clients?clientId=${encodeURIComponent(client.clientId)}`, { token });
  const { data: account } = await http('GET', `${api}/clients/${created[0].id}/service-account-user`, { token });
  const representations = [];
  for (const role of roles) representations.push((await http('GET', `${api}/roles/${encodeURIComponent(role)}`, { token })).data);
  await http('POST', `${api}/users/${account.id}/role-mappings/realm`, { token, body: representations });
  log(`service account roles of ${client.clientId}: ${roles.join(', ')}`);
}
log(`done: ${missing.length} client(s) created`);
