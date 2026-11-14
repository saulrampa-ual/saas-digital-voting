// RNF-4 online sessions only: the e-mail code of the management login (email-otp-authenticator, D-05) can be skipped
// for the RNF-4 participant accounts, whose mail never leaves Mailpit. Everybody else keeps the code.
//
// How: in the browser flow, the REQUIRED "email-otp-form" step moves into a CONDITIONAL sub-flow whose condition is
// "the user does NOT have the realm role RNF4_LOGIN_WITHOUT_CODE" (Condition - user role, negated). The role is given
// only to the participant accounts. `on()` puts the flow back exactly as in verivoto-realm.json and deletes the role.
// Both are idempotent and never leave the realm without the code step for the rest of the users.

import { KC, REALM, log, http } from './common.mjs';

export const ROLE = 'RNF4_LOGIN_WITHOUT_CODE';
const FLOW = 'verivoto browser forms';
const SUBFLOW = 'verivoto email otp unless rnf4';
const SUBFLOW_FIXED = 'verivoto email otp fixed rnf4';
/** Code of the RNF-4 accounts with login-code-fixed: one repeated digit, the only fixed code the authenticator allows (D-61). */
export const FIXED_CODE = '111111';
const OTP = 'email-otp-form';
const OTP_CONFIG_ALIAS = 'verivoto email otp';

const base = `${KC}/admin/realms/${REALM}`;
const flowUrl = (alias) => `${base}/authentication/flows/${encodeURIComponent(alias)}`;

async function executions(adm, alias) {
  return (await http('GET', `${flowUrl(alias)}/executions`, { token: adm })).data;
}

async function setRequirement(adm, alias, execution, requirement) {
  await http('PUT', `${flowUrl(alias)}/executions`, { token: adm, body: { ...execution, requirement } });
}

/** Adds an authenticator to a flow with the given requirement and optional config; returns the new execution. */
async function addStep(adm, alias, provider, requirement, config) {
  const before = new Set((await executions(adm, alias)).map((e) => e.id));
  await http('POST', `${flowUrl(alias)}/executions/execution`, { token: adm, body: { provider } });
  const added = (await executions(adm, alias)).find((e) => !before.has(e.id) && e.level === 0);
  await setRequirement(adm, alias, added, requirement);
  if (config) {
    await http('POST', `${base}/authentication/executions/${added.id}/config`, { token: adm, body: config });
  }
  return added;
}

async function otpConfig(adm, execution) {
  if (!execution?.authenticationConfig) throw new Error(`the ${OTP} step has no configuration`);
  return (await http('GET', `${base}/authentication/config/${execution.authenticationConfig}`, { token: adm })).data.config;
}

async function ensureRole(adm) {
  await http('POST', `${base}/roles`, { token: adm, ok: [201, 409], body: {
    name: ROLE, description: 'RNF-4 online sessions only: login without the e-mail code. Removed by seed-rnf4.mjs login-code-on.' } });
  return (await http('GET', `${base}/roles/${ROLE}`, { token: adm })).data;
}

/** Removes the fixed-code sub-flow, if any (the role then means "no code" again, or nothing after `on`). */
async function removeFixed(adm) {
  const sub = (await executions(adm, FLOW)).find((e) => e.level === 0 && e.displayName === SUBFLOW_FIXED);
  if (!sub) return;
  await http('DELETE', `${base}/authentication/executions/${sub.id}`, { token: adm, ok: [204] });
  log('login flow: fixed-code sub-flow removed');
}

/**
 * The given accounts keep the code step, but their code is always FIXED_CODE; everybody else keeps the random code.
 * Built on `off` (role + conditional sub-flow for the others) plus a second CONDITIONAL sub-flow, condition
 * "the user HAS the role", with the same authenticator and a one-character alphabet.
 */
export async function fixed(adm, emails) {
  await off(adm, emails);
  const inner = (await executions(adm, SUBFLOW)).find((e) => e.level === 0 && e.providerId === OTP);
  const config = await otpConfig(adm, inner);
  await http('POST', `${flowUrl(FLOW)}/executions/flow`, { token: adm, body: {
    alias: SUBFLOW_FIXED, type: 'basic-flow', provider: 'registration-page-form',
    description: `E-mail code step with the fixed code ${FIXED_CODE} for the RNF-4 participant accounts` } });
  const sub = (await executions(adm, FLOW)).find((e) => e.level === 0 && e.displayName === SUBFLOW_FIXED);
  await setRequirement(adm, FLOW, sub, 'CONDITIONAL');
  await addStep(adm, SUBFLOW_FIXED, 'conditional-user-role', 'REQUIRED',
    { alias: 'rnf4 participant', config: { condUserRole: ROLE, negate: 'false' } });
  await addStep(adm, SUBFLOW_FIXED, OTP, 'REQUIRED', { alias: `${OTP_CONFIG_ALIAS} rnf4 fixed`,
    config: { ...config, 'code-alphabet': FIXED_CODE[0], 'code-length': String(FIXED_CODE.length) } });
  log(`login flow: role ${ROLE} keeps the code step with the fixed code ${FIXED_CODE}`);
}

/** Skips the e-mail code for the given accounts only. */
export async function off(adm, emails) {
  await removeFixed(adm);
  const top = await executions(adm, FLOW);
  const current = top.find((e) => e.level === 0 && e.providerId === OTP);
  const leftover = top.find((e) => e.level === 0 && e.displayName === SUBFLOW);
  if (current) {
    const config = await otpConfig(adm, current);
    // A sub-flow left by an interrupted run is rebuilt from scratch (the unconditional code step is still there)
    if (leftover) await http('DELETE', `${base}/authentication/executions/${leftover.id}`, { token: adm, ok: [204] });
    // 1. New conditional sub-flow with the code (the original step is still there, so the code is never missing)
    await http('POST', `${flowUrl(FLOW)}/executions/flow`, { token: adm, body: {
      alias: SUBFLOW, type: 'basic-flow', provider: 'registration-page-form',
      description: 'E-mail code for everybody except the RNF-4 participant accounts' } });
    const sub = (await executions(adm, FLOW)).find((e) => e.level === 0 && e.displayName === SUBFLOW);
    await setRequirement(adm, FLOW, sub, 'CONDITIONAL');
    await addStep(adm, SUBFLOW, 'conditional-user-role', 'REQUIRED',
      { alias: 'unless rnf4 participant', config: { condUserRole: ROLE, negate: 'true' } });
    await addStep(adm, SUBFLOW, OTP, 'REQUIRED', { alias: `${OTP_CONFIG_ALIAS} rnf4`, config });
    // 2. Only then the unconditional step goes away
    await http('DELETE', `${base}/authentication/executions/${current.id}`, { token: adm, ok: [204] });
    log(`login flow: e-mail code now skipped for role ${ROLE}`);
  } else if (leftover) {
    log('login flow: already conditional');
  } else {
    throw new Error(`step ${OTP} not found in «${FLOW}»: the flow is not the one of verivoto-realm.json`);
  }
  const role = await ensureRole(adm);
  for (const email of emails) {
    const { data } = await http('GET', `${base}/users?username=${encodeURIComponent(email)}&exact=true`, { token: adm });
    if (!data[0]) throw new Error(`Keycloak user not found: ${email}`);
    await http('POST', `${base}/users/${data[0].id}/role-mappings/realm`, { token: adm, body: [role] });
  }
  log(`login without code: ${emails.length} account(s)`);
}

/** Puts the e-mail code back for everybody (flow as in verivoto-realm.json) and deletes the role. */
export async function on(adm) {
  await removeFixed(adm);
  const top = await executions(adm, FLOW);
  const sub = top.find((e) => e.level === 0 && e.displayName === SUBFLOW);
  if (sub) {
    const inner = (await executions(adm, SUBFLOW)).find((e) => e.level === 0 && e.providerId === OTP);
    const config = await otpConfig(adm, inner);
    // 1. Unconditional code step first, 2. then the sub-flow goes away
    if (!top.some((e) => e.level === 0 && e.providerId === OTP)) {
      await addStep(adm, FLOW, OTP, 'REQUIRED', { alias: OTP_CONFIG_ALIAS, config });
    }
    await http('DELETE', `${base}/authentication/executions/${sub.id}`, { token: adm, ok: [204] });
    log('login flow: e-mail code required for everybody again');
  } else {
    log('login flow: already as in verivoto-realm.json');
  }
  const role = await http('GET', `${base}/roles/${ROLE}`, { token: adm, ok: [200, 404] });
  if (role.status === 200) {
    await http('DELETE', `${base}/roles/${ROLE}`, { token: adm, ok: [204] });
    log(`role ${ROLE} deleted`);
  }
}
