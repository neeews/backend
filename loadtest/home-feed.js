import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL, STATS, pick, authHeader, loginTokens, exposeSubmetrics, openModelScenarios } from './lib.js';

// 메인 화면 로딩 부하 테스트. 한 번의 반복 = 메인 화면 1회 로딩이다.
// 브라우저처럼 속보·최신·핫이슈·헤드라인·오늘의 뉴스를 병렬로 부르고 방문 집계를 보낸다.
// 실행 예: k6 run -e RATE=20 -e DURATION=40s loadtest/home-feed.js  (RATE = 초당 메인 화면 로딩 수)
const RATE = Number(__ENV.RATE || 20);
const DURATION = __ENV.DURATION || '40s';
const WARMUP = __ENV.WARMUP || '10s';
const MAX_VUS = Number(__ENV.MAX_VUS || 400);
const LOGGED_IN_SHARE = Number(__ENV.LOGGED_IN_SHARE || 0.3);
const TOKEN_COUNT = 20;

const FEEDS = ['headlines', 'hot', 'latest', 'breaking', 'today'];
const pageLoad = new Trend('page_load_ms', true);

export const options = {
  summaryTrendStats: STATS,
  setupTimeout: '120s',
  scenarios: openModelScenarios('home', RATE, WARMUP, DURATION, MAX_VUS),
  thresholds: exposeSubmetrics([
    'http_req_duration{phase:measure}',
    'http_req_failed{phase:measure}',
    'page_load_ms{phase:measure}',
    'page_load_ms{phase:measure,auth:no}',
    'page_load_ms{phase:measure,auth:yes}',
    ...FEEDS.map((name) => `http_req_duration{phase:measure,name:${name}}`),
    ...FEEDS.map((name) => `http_req_duration{phase:measure,auth:yes,name:${name}}`),
    'http_req_duration{phase:measure,name:ping}',
  ]),
};

export function setup() {
  return { tokens: loginTokens(TOKEN_COUNT) };
}

export function home(data) {
  const authed = data.tokens.length > 0 && Math.random() < LOGGED_IN_SHARE;
  const headers = authed ? authHeader(pick(data.tokens)) : {};
  const auth = authed ? 'yes' : 'no';
  const requests = FEEDS.map((name) => ['GET', `${BASE_URL}/articles/${name}`, null, { headers, tags: { name, auth } }]);
  requests.push(['POST', `${BASE_URL}/visits/ping`, null, { headers, tags: { name: 'ping', auth } }]);

  const started = Date.now();
  const responses = http.batch(requests);
  pageLoad.add(Date.now() - started, { auth });

  check(responses, { '메인 화면 요청 전부 200': (rs) => rs.every((r) => r.status === 200) });
}
