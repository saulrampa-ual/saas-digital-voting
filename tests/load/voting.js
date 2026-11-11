import http from 'k6/http';
import { check, fail } from 'k6';
import { SharedArray } from 'k6/data';

const vus = Number(__ENV.VUS || 500);
const iterations = Number(__ENV.ITERATIONS || 1);
const baseUrl = __ENV.BASE_URL;
const processId = __ENV.PROCESS_ID;
const tokensFile = __ENV.TOKENS_FILE;
const optionId = Number(__ENV.OPTION_ID);

if (!baseUrl || !processId || !tokensFile || !Number.isSafeInteger(optionId)) {
  throw new Error('Define BASE_URL, PROCESS_ID, TOKENS_FILE y OPTION_ID con datos sintéticos.');
}

const tokens = new SharedArray('synthetic voting tokens', () => JSON.parse(open(tokensFile)));

if (tokens.length < vus * iterations) {
  throw new Error(`TOKENS_FILE necesita al menos ${vus * iterations} tokens sintéticos distintos.`);
}
if (new Set(tokens.slice(0, vus * iterations)).size !== vus * iterations) {
  throw new Error('Los tokens asignados a los VU deben ser distintos.');
}

const STATUSES = [0, 201, 400, 401, 403, 404, 409, 410, 422, 423, 429, 500, 502, 503, 504];

export const options = {
  scenarios: {
    cast_votes: {
      executor: 'per-vu-iterations',
      vus,
      iterations,
      maxDuration: '2m',
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  thresholds: {
    'http_req_duration{operation:cast_vote}': ['p(95)<2000'],
    // Count per HTTP status only, so the summary export shows them (check-load-run.mjs and the discard rules of
    // docs/evaluation/carga.md). 'count>=0' never fails and does not change what is measured. 0 = no response.
    ...Object.fromEntries(STATUSES.map((s) => [`http_reqs{operation:cast_vote,status:${s}}`, ['count>=0']])),
  },
};

export default function () {
  const token = tokens[(__VU - 1) * iterations + __ITER];
  if (typeof token !== 'string' || token.length === 0) {
    fail('Falta un token sintético para este VU/iteración.');
  }

  const response = http.post(
    `${baseUrl}/api/v1/ballot-box/processes/${processId}/votes`,
    JSON.stringify({ votingToken: token, selection: { optionId } }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { operation: 'cast_vote' },
    },
  );

  check(response, {
    'vote accepted (201)': (res) => res.status === 201,
  });
}
