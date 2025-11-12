import http from 'k6/http';
import { check, sleep, randomSeed } from 'k6';

// Configuration via environment variables
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_PATH = __ENV.API_PATH || '/api/v1/shorten';
const TEST_DURATION = __ENV.DURATION || '1m';
const READ_RPS = Number(__ENV.READ_RPS || 100);   // redirects per second
const WRITE_RPS = Number(__ENV.WRITE_RPS || 1);   // creates per second
const SEED_COUNT = Number(__ENV.SEED_COUNT || 100);

// Reproducible randomness per test run
randomSeed(Number(__ENV.RANDOM_SEED || 12345));

function randomItemLocal(arr) {
  if (!arr || arr.length === 0) return null;
  const i = Math.floor(Math.random() * arr.length);
  return arr[i];
}

export const options = {
  scenarios: {
    redirects: {
      executor: 'constant-arrival-rate',
      rate: READ_RPS,
      timeUnit: '1s',
      duration: TEST_DURATION,
      preAllocatedVUs: Math.max(20, Math.ceil(READ_RPS * 0.5)),
      maxVUs: Math.max(50, READ_RPS * 2),
      exec: 'redirects',
    },
    shorten: {
      executor: 'constant-arrival-rate',
      rate: WRITE_RPS,
      timeUnit: '1s',
      duration: TEST_DURATION,
      preAllocatedVUs: Math.max(5, Math.ceil(WRITE_RPS)),
      maxVUs: Math.max(10, WRITE_RPS * 2),
      exec: 'shorten',
      startTime: '0s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'], // <1% errors overall
    'http_req_duration{endpoint:redirect}': ['p(95)<100'],
    'http_req_duration{endpoint:shorten}': ['p(95)<200'],
  },
};

// setup seeds the system with short URLs for the redirect scenario
export function setup() {
  const created = [];
  for (let i = 0; i < SEED_COUNT; i++) {
    const uniqueLongUrl = `https://example.com/products/item/${i}?t=${Date.now()}&r=${Math.random()}`;
    const payload = JSON.stringify({ longUrl: uniqueLongUrl });
    const res = http.post(`${BASE_URL}${API_PATH}`, payload, {
      headers: { 'Content-Type': 'application/json' },
      tags: { endpoint: 'shorten', phase: 'seed' },
    });
    check(res, {
      'seed shorten: 200': (r) => r.status === 200,
      'seed has shortUrl': (r) => Boolean(r.json('shortUrl')),
    });
    if (res.status === 200) {
      const shortUrl = res.json('shortUrl');
      const shortCode = (shortUrl || '').split('/').pop();
      if (shortCode) created.push(shortCode);
    }
    // small pacing to avoid burst on startup
    sleep(0.01);
  }
  return { codes: created };
}

export function redirects(data) {
  const code = randomItemLocal(data && data.codes);
  if (!code) {
    return;
  }
  const res = http.get(`${BASE_URL}/${code}`, {
    redirects: 0, // we expect a 301 and do not want to follow
    tags: { endpoint: 'redirect' },
  });
  check(res, {
    'redirect 301': (r) => r.status === 301,
    'has Location': (r) => Boolean(r.headers['Location']),
  });
}

export function shorten() {
  // 5% chance to attempt a custom alias
  const attemptAlias = Math.random() < 0.05;
  const longUrl = `https://test.example.com/page/${Math.floor(Math.random() * 1e9)}?v=${Date.now()}`;
  const body = attemptAlias
    ? { longUrl, customAlias: `alias_${Math.floor(Math.random() * 1e9)}` }
    : { longUrl };

  const res = http.post(`${BASE_URL}${API_PATH}`, JSON.stringify(body), {
    headers: { 'Content-Type': 'application/json' },
    tags: { endpoint: 'shorten' },
  });
  check(res, {
    'shorten 200': (r) => r.status === 200,
    'shorten has shortUrl': (r) => Boolean(r.json('shortUrl')),
  });
}

// Helpful CLI examples (adjust as needed):
// k6 run load-test/url-shortener.k6.js
// BASE_URL=http://localhost:8080 DURATION=2m READ_RPS=200 WRITE_RPS=2 k6 run load-test/url-shortener.k6.js
// BASE_URL=http://localhost:8080 SEED_COUNT=500 k6 run load-test/url-shortener.k6.js


