import http from 'k6/http';
import { check } from 'k6';
import encoding from 'k6/encoding';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const username = __ENV.API_USERNAME || 'demo-client';
const password = __ENV.API_PASSWORD || 'local-demo-change-this';
const runId = __ENV.RUN_ID || `${Date.now()}`;
const authorization = `Basic ${encoding.b64encode(`${username}:${password}`)}`;

export const options = {
  discardResponseBodies: true,
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 10 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{endpoint:submit}': ['p(95)<1000', 'p(99)<2000'],
  },
};

export default function () {
  const iterationKey = `${__VU}-${__ITER}`;
  const payload = JSON.stringify({
    transactionId: `LOAD-${runId}-${iterationKey}`,
    userId: `USER-${__VU}`,
    amount: 9500.00,
    country: 'NG',
    ipAddress: '192.168.1.100',
    cardAttempts: 5,
    emailAgeDays: 2,
  });
  const response = http.post(`${baseUrl}/api/v2/transactions`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': `LOAD-${runId}-${iterationKey}`,
      'Authorization': authorization,
    },
    tags: { endpoint: 'submit' },
  });
  check(response, {
    'accepted for asynchronous analysis': (result) => result.status === 202,
  });
}
