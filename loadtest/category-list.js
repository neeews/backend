import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, STATS, pick, weightedPick, exposeSubmetrics, openModelScenarios } from './lib.js';

// 카테고리 목록(/articles?category=) 부하 테스트. 최신순·인기순·2~3페이지를 섞는다.
// 실행 예: k6 run -e RATE=10 -e DURATION=40s loadtest/category-list.js
const RATE = Number(__ENV.RATE || 10);
const DURATION = __ENV.DURATION || '40s';
const WARMUP = __ENV.WARMUP || '10s';
const MAX_VUS = Number(__ENV.MAX_VUS || 300);

const CATEGORIES = ['정치', '경제', '사회', '세계', 'IT/과학', '스포츠', '연예/문화'];
const MIX = [
  { name: 'list_latest', w: 60, sort: 'latest', pages: [1] },
  { name: 'list_popular', w: 25, sort: 'popular', pages: [1] },
  { name: 'list_deep', w: 15, sort: 'latest', pages: [2, 3] },
];

export const options = {
  summaryTrendStats: STATS,
  scenarios: openModelScenarios('list', RATE, WARMUP, DURATION, MAX_VUS),
  thresholds: exposeSubmetrics([
    'http_req_duration{phase:measure}',
    'http_req_failed{phase:measure}',
    ...MIX.map((m) => `http_req_duration{phase:measure,name:${m.name}}`),
  ]),
};

export function list() {
  const kind = weightedPick(MIX);
  const category = encodeURIComponent(pick(CATEGORIES));
  const url = `${BASE_URL}/articles?category=${category}&sort=${kind.sort}&page=${pick(kind.pages)}`;
  const res = http.get(url, { tags: { name: kind.name }, timeout: '60s' });
  check(res, { '목록 200': (r) => r.status === 200 });
}
