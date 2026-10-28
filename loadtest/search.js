import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, STATS, pick, weightedPick, exposeSubmetrics, openModelScenarios } from './lib.js';

// 검색(/search) 부하 테스트. 도착률(RPS)을 고정해 같은 부하에서의 지연을 비교한다.
// 실행 예: k6 run -e RATE=5 -e DURATION=40s loadtest/search.js
const RATE = Number(__ENV.RATE || 5);
const DURATION = __ENV.DURATION || '40s';
const WARMUP = __ENV.WARMUP || '10s';
const MAX_VUS = Number(__ENV.MAX_VUS || 300);
const CATEGORY_FILTER_SHARE = 0.2;

// 포함 비율(선택도)별로 나눈 검색어. 흔한 단어일수록 정렬·카운트 비용이 커진다.
const KEYWORDS = [
  { kind: 'common', w: 20, words: ['기자', '정부', '대통령', '연합뉴스'] },
  { kind: 'medium', w: 35, words: ['환율', '삼성전자'] },
  { kind: 'rare', w: 35, words: ['손흥민', '태풍'] },
  { kind: 'none', w: 10, words: ['존재하지않는검색어'] },
];
const CATEGORIES = ['정치', '경제', '사회', '세계', 'IT/과학', '스포츠', '연예/문화'];

// 개선 전후로 검색 결과 개수(total)가 같은지 확인하는 기준 질의
const TOTAL_CHECKS = [['기자'], ['정부'], ['대통령'], ['연합뉴스'], ['환율'], ['삼성전자'], ['손흥민'], ['태풍'],
  ['존재하지않는검색어'], ['환율', '경제'], ['삼성전자', 'IT/과학'], ['기자', '정치']];

export const options = {
  summaryTrendStats: STATS,
  setupTimeout: '600s',
  scenarios: openModelScenarios('search', RATE, WARMUP, DURATION, MAX_VUS),
  thresholds: exposeSubmetrics([
    'http_req_duration{phase:measure}',
    'http_req_duration{phase:measure,kind:common}',
    'http_req_duration{phase:measure,kind:medium}',
    'http_req_duration{phase:measure,kind:rare}',
    'http_req_duration{phase:measure,kind:none}',
    'http_req_failed{phase:measure}',
  ]),
};

function url(q, category, limit = 10) {
  const filter = category ? `&categories=${encodeURIComponent(category)}` : '';
  return `${BASE_URL}/search?q=${encodeURIComponent(q)}&page=1&limit=${limit}${filter}`;
}

export function setup() {
  if (__ENV.TOTALS !== '1') return {};
  const totals = {};
  for (const [q, category] of TOTAL_CHECKS) {
    const res = http.get(url(q, category, 1), { tags: { name: 'setup_total' }, timeout: '120s' });
    totals[category ? `${q}+${category}` : q] = res.status === 200 ? res.json('total') : `HTTP ${res.status}`;
  }
  console.log(`SEARCH_TOTALS ${JSON.stringify(totals)}`);
  return totals;
}

export function search() {
  const group = weightedPick(KEYWORDS);
  const q = pick(group.words);
  const category = Math.random() < CATEGORY_FILTER_SHARE ? pick(CATEGORIES) : null;
  const res = http.get(url(q, category), { tags: { name: 'search', kind: group.kind }, timeout: '60s' });
  check(res, {
    '검색 200': (r) => r.status === 200,
    '검색 응답 형식': (r) => r.status === 200 && Array.isArray(r.json('articles')) && typeof r.json('total') === 'number',
  });
}
