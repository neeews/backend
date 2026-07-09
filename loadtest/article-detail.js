import http from 'k6/http';
import { check, sleep } from 'k6';

// VPS 내부에서 실행하는 것을 기준으로 함 (docker-compose network_mode: host, server.port=9080)
const BASE_URL = 'http://localhost:9080';

// 캐시 미스 후보: 최근 수집돼서 아직 이미지/본문 크롤링이 안 된 기사
const CACHE_MISS_IDS = [17061, 17062, 17063, 17064, 17065, 17066, 17067, 17068, 17069, 17070, 17071, 17072, 17073, 17074, 17075];
// 캐시 히트 후보: 이미 이미지/본문 처리가 끝난 기사
const CACHE_HIT_IDS = [16997, 16995, 16992, 16680, 16679, 16538, 11380, 11290, 11289, 11281, 9954, 9631, 8869, 7467, 7187];

export const options = {
  scenarios: {
    cache_miss: {
      executor: 'per-vu-iterations',
      vus: 5,
      iterations: 1, // 각 VU가 1번씩만 돌아서 매번 "첫 조회" 상황을 흉내냄
      exec: 'cacheMiss',
      maxDuration: '1m',
    },
    cache_hit: {
      executor: 'constant-vus',
      vus: 10,
      duration: '30s',
      exec: 'cacheHit',
      startTime: '1m', // cache_miss 시나리오가 끝난 뒤 시작
    },
  },
  thresholds: {
    'http_req_duration{scenario:cache_hit}': ['p(95)<300'],
  },
};

export function cacheMiss() {
  const id = CACHE_MISS_IDS[__VU % CACHE_MISS_IDS.length];
  const res = http.get(`${BASE_URL}/articles/${id}`, { tags: { name: 'detail_cache_miss' } });
  check(res, { '기사 상세 200 (캐시 미스)': (r) => r.status === 200 });
}

export function cacheHit() {
  const id = CACHE_HIT_IDS[Math.floor(Math.random() * CACHE_HIT_IDS.length)];
  const res = http.get(`${BASE_URL}/articles/${id}`, { tags: { name: 'detail_cache_hit' } });
  check(res, { '기사 상세 200 (캐시 히트)': (r) => r.status === 200 });
  sleep(0.5);
}
