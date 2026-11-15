// IMP-27 (D-52) · local development only. Adds the audience mapper "verivoto-api" to the clients of a realm that was
// imported before that change: Keycloak only imports infra/keycloak/realm/verivoto-realm.json when the realm does not
// exist yet, and the services now reject access tokens without that audience.
// Clients: verivoto-admin, the four service accounts, verivoto-dev-swagger (if present) and every API key client
// (clientId "api-…", RF-21). Never verivoto-voter: its ID tokens keep their own audience (RF-05b).
// Idempotent: a client that already has the mapper is left as it is. Prints client ids only, never secrets or tokens.
//
// Usage, from the repository root, with Keycloak running (infra/compose.yaml) and infra/.env present:
//   node scripts/dev/keycloak-api-audience.mjs
// Then restart nothing: new tokens carry the audience (log out and in again in the console).

import { KC, REALM, log, http, readInfraEnv } from '../demo/lib/common.mjs';

const AUDIENCE = 'verivoto-api';
const FIXED = ['verivoto-admin', 'identity-service', 'election-service', 'ballot-box-service', 'voter-access-service',
  'verivoto-dev-swagger'];
const MAPPER = {
  name: `audience ${AUDIENCE}`, protocol: 'openid-connect', protocolMapper: 'oidc-audience-mapper',
  consentRequired: false,
  config: {
    'included.custom.audience': AUDIENCE, 'access.token.claim': 'true', 'id.token.claim': 'false',
    'introspection.token.claim': 'true',
  },
};

const env = readInfraEnv();
const { data: tokenData } = await http('POST', `${KC}/realms/master/protocol/openid-connect/token`, { form: {
  grant_type: 'password', client_id: 'admin-cli',
  username: env.KC_BOOTSTRAP_ADMIN_USERNAME || 'local-admin', password: env.KC_BOOTSTRAP_ADMIN_PASSWORD } });
const token = tokenData.access_token;
const base = `${KC}/admin/realms/${REALM}/clients`;

const { data: all } = await http('GET', `${base}?first=0&max=1000`, { token });
const targets = all.filter((c) => FIXED.includes(c.clientId) || c.clientId.startsWith('api-'));
const missing = FIXED.filter((id) => id !== 'verivoto-dev-swagger' && !targets.some((c) => c.clientId === id));
if (missing.length) throw new Error(`Clients not found in realm ${REALM}: ${missing.join(', ')}`);

let added = 0;
for (const client of targets) {
  const { data: mappers } = await http('GET', `${base}/${client.id}/protocol-mappers/models`, { token });
  const present = mappers.some((m) => m.protocolMapper === 'oidc-audience-mapper'
    && m.config?.['included.custom.audience'] === AUDIENCE);
  if (present) {
    log(`already has the audience: ${client.clientId}`);
    continue;
  }
  await http('POST', `${base}/${client.id}/protocol-mappers/models`, { token, body: MAPPER });
  added += 1;
  log(`audience added: ${client.clientId}`);
}
log(`done: ${added} client(s) updated, ${targets.length - added} already up to date`);
