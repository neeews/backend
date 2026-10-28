import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Counter } from 'k6/metrics';
import { BASE_URL, STATS, pick, authHeader, loginTokens, exposeSubmetrics, openModelScenarios } from './lib.js';

// 기사 상세(/articles/{id}) 부하 테스트. SCENARIO 환경변수로 세 가지 중 하나를 고른다.
//   hit   이미 크롤링·이미지 캐시가 끝난 기사를 일정한 도착률로 조회 (정상 상태의 지연)
//   hot   한 기사에 동시 조회가 몰릴 때 (조회수 UPDATE 경합, 조회수 유실 확인)
//   miss  아직 크롤링 안 된 새 기사가 한꺼번에 첫 조회될 때, 다른 API가 영향받는지 확인
// 기사 ID는 seed.py가 만든 ids.json을 IDS_FILE로 넘겨 읽는다.
// 실행 예: k6 run -e SCENARIO=hit -e RATE=50 -e IDS_FILE=ids.json loadtest/detail.js
const SCENARIO = __ENV.SCENARIO || 'hit';
const RATE = Number(__ENV.RATE || 50);
const DURATION = __ENV.DURATION || '40s';
const WARMUP = __ENV.WARMUP || '10s';
const MAX_VUS = Number(__ENV.MAX_VUS || 300);
const LOGGED_IN_SHARE = Number(__ENV.LOGGED_IN_SHARE || 0.3);
const HOT_VUS = Number(__ENV.HOT_VUS || 50);
const BG_RATE = Number(__ENV.BG_RATE || 20);
const BURST_VUS = Number(__ENV.BURST_VUS || 40);
const MISS_DURATION = __ENV.MISS_DURATION || '70s';
const TOKEN_COUNT = 20;
const HOT_SET_SHARE = 0.6;

const ids = JSON.parse(open(__ENV.IDS_FILE));
const viewsOk = new Counter('views_ok');

const scenarios = {
  hit: openModelScenarios('hit', RATE, WARMUP, DURATION, MAX_VUS),
  hot: {
    hot: { executor: 'constant-vus', vus: HOT_VUS, duration: DURATION, exec: 'hot', tags: { phase: 'measure' } },
  },
  miss: {
    // 평소 트래픽(가벼운 API) — 첫 조회 폭주 동안 이 요청들이 얼마나 느려지는지 본다
    bg: { executor: 'constant-arrival-rate', rate: BG_RATE, timeUnit: '1s', duration: MISS_DURATION,
      preAllocatedVUs: 30, maxVUs: 200, exec: 'bg', tags: { phase: 'measure' } },
    burst_1: { executor: 'per-vu-iterations', vus: BURST_VUS, iterations: 1, startTime: '15s', maxDuration: '60s',
      exec: 'burst', env: { WAVE_BASE: '0' }, tags: { phase: 'measure' } },
    burst_2: { executor: 'per-vu-iterations', vus: BURST_VUS, iterations: 1, startTime: '40s', maxDuration: '60s',
      exec: 'burst', env: { WAVE_BASE: '200' }, tags: { phase: 'measure' } },
  },
};

export const options = {
  summaryTrendStats: STATS,
  setupTimeout: '120s',
  scenarios: scenarios[SCENARIO],
  thresholds: exposeSubmetrics([
    'http_req_duration{phase:measure}',
    'http_req_failed{phase:measure}',
    'http_req_duration{phase:measure,auth:no}',
    'http_req_duration{phase:measure,auth:yes}',
    'http_req_duration{phase:measure,role:bg}',
    'http_req_duration{phase:measure,role:burst}',
  ]),
};

export function setup() {
  return { tokens: SCENARIO === 'hit' ? loginTokens(TOKEN_COUNT) : [] };
}

function detail(id, tags, headers = {}) {
  const res = http.get(`${BASE_URL}/articles/${id}`, { headers, tags, timeout: '120s' });
  const ok = check(res, {
    '상세 200': (r) => r.status === 200,
    '상세 응답에 본문 있음': (r) => r.status === 200 && r.json('article.content') !== null,
  });
  if (res.status === 200) viewsOk.add(1);
  return ok;
}

export function hit(data) {
  const id = Math.random() < HOT_SET_SHARE ? pick(ids.hot) : pick(ids.hit);
  const authed = data.tokens.length > 0 && Math.random() < LOGGED_IN_SHARE;
  detail(id, { name: 'detail_hit', auth: authed ? 'yes' : 'no' }, authed ? authHeader(pick(data.tokens)) : {});
}

export function hot() {
  detail(ids.hotOne, { name: 'detail_hot', auth: 'no' });
}

export function bg() {
  const url = Math.random() < 0.5
    ? `${BASE_URL}/articles/latest`
    : `${BASE_URL}/articles?category=${encodeURIComponent('정치')}&sort=latest&page=1`;
  const res = http.get(url, { tags: { name: 'bg_feed', role: 'bg' }, timeout: '120s' });
  check(res, { '평소 API 200': (r) => r.status === 200 });
}

export function burst() {
  const base = Number(__ENV.WAVE_BASE || 0);
  const id = ids.miss[base + exec.scenario.iterationInTest];
  detail(id, { name: 'detail_miss', role: 'burst', auth: 'no' });
}
