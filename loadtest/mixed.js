import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL, STATS, pick, weightedPick, authHeader, loginTokens, exposeSubmetrics } from './lib.js';

// 검색과 메인 화면이 동시에 들어올 때, 한쪽 부하가 다른 쪽 응답을 얼마나 늦추는지 본다.
// (두 API는 DB 커넥션 풀을 같이 쓴다.) 실행 예: k6 run -e SEARCH_RATE=2 -e FEED_RATE=3 loadtest/mixed.js
const SEARCH_RATE = Number(__ENV.SEARCH_RATE || 2);
const FEED_RATE = Number(__ENV.FEED_RATE || 3);   // 초당 메인 화면 로딩 수
const DURATION = __ENV.DURATION || '45s';
const WARMUP = __ENV.WARMUP || '10s';
const MAX_VUS = Number(__ENV.MAX_VUS || 300);
const FEEDS = ['headlines', 'hot', 'latest', 'breaking', 'today'];
const KEYWORDS = [
  { w: 20, words: ['기자', '정부', '대통령', '연합뉴스'] },
  { w: 35, words: ['환율', '삼성전자'] },
  { w: 35, words: ['손흥민', '태풍'] },
  { w: 10, words: ['존재하지않는검색어'] },
];
const pageLoad = new Trend('page_load_ms', true);

function pair(role, exec, rate) {
  const base = { executor: 'constant-arrival-rate', rate, timeUnit: '1s', preAllocatedVUs: 30, maxVUs: MAX_VUS, exec };
  return {
    [`${role}_warmup`]: { ...base, duration: WARMUP, tags: { phase: 'warmup', role } },
    [`${role}_measure`]: { ...base, duration: DURATION, startTime: WARMUP, tags: { phase: 'measure', role } },
  };
}

export const options = {
  summaryTrendStats: STATS,
  setupTimeout: '120s',
  scenarios: { ...pair('search', 'search', SEARCH_RATE), ...pair('feed', 'feed', FEED_RATE) },
  thresholds: exposeSubmetrics([
    'http_req_duration{phase:measure,role:search}',
    'http_req_failed{phase:measure,role:search}',
    'http_req_failed{phase:measure,role:feed}',
    'page_load_ms{phase:measure,role:feed}',
    ...FEEDS.map((name) => `http_req_duration{phase:measure,role:feed,name:${name}}`),
    'http_req_duration{phase:measure,role:feed,name:ping}',
  ]),
};

export function setup() {
  return { tokens: loginTokens(20) };
}

export function search() {
  const q = pick(weightedPick(KEYWORDS).words);
  const res = http.get(`${BASE_URL}/search?q=${encodeURIComponent(q)}&page=1&limit=10`, { tags: { name: 'search' }, timeout: '60s' });
  check(res, { '검색 200': (r) => r.status === 200 });
}

export function feed(data) {
  const authed = data.tokens.length > 0 && Math.random() < 0.3;
  const headers = authed ? authHeader(pick(data.tokens)) : {};
  const requests = FEEDS.map((name) => ['GET', `${BASE_URL}/articles/${name}`, null, { headers, tags: { name } }]);
  requests.push(['POST', `${BASE_URL}/visits/ping`, null, { headers, tags: { name: 'ping' } }]);
  const started = Date.now();
  const responses = http.batch(requests);
  pageLoad.add(Date.now() - started);
  check(responses, { '메인 화면 요청 전부 200': (rs) => rs.every((r) => r.status === 200) });
}
