import http from 'k6/http';
import { check, sleep } from 'k6';

// 인텔리제이로 스프링 부트를 로컬에서 띄운 상태를 기준으로 함 (application.properties: server.port=9080)
const BASE_URL = 'http://localhost:9080';

// 로그인이 필요한 API를 테스트하려면 미리 회원가입시킨 테스트 계정으로 바꿔주세요.
const TEST_EMAIL = 'loadtest@example.com';
const TEST_PASSWORD = 'password123';

export const options = {
  stages: [
    { duration: '30s', target: 20 }, // 램프업
    { duration: '1m', target: 20 },  // 유지
    { duration: '30s', target: 0 },  // 램프다운
  ],
  thresholds: {
    http_req_duration: ['p(95)<300'], // 95%가 300ms 이내
    http_req_failed: ['rate<0.01'],   // 실패율 1% 미만
    'http_req_duration{name:hot}': ['p(95)<300'],
    'http_req_duration{name:latest}': ['p(95)<300'],
    'http_req_duration{name:breaking}': ['p(95)<300'],
    'http_req_duration{name:list}': ['p(95)<300'],
  },
};

// setup()은 테스트 전체에서 한 번만 실행됨 — 로그인해서 VU들이 공유할 토큰을 발급
export function setup() {
  const res = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
    email: TEST_EMAIL,
    password: TEST_PASSWORD,
  }), { headers: { 'Content-Type': 'application/json' } });

  check(res, { '로그인 성공': (r) => r.status === 200 });
  return { token: res.json('accessToken') };
}

export default function (data) {
  const headers = data.token ? { Authorization: `Bearer ${data.token}` } : {};

  const hot = http.get(`${BASE_URL}/articles/hot`, { headers, tags: { name: 'hot' } });
  check(hot, { '핫이슈 200': (r) => r.status === 200 });

  const latest = http.get(`${BASE_URL}/articles/latest`, { headers, tags: { name: 'latest' } });
  check(latest, { '최신 기사 200': (r) => r.status === 200 });

  const breaking = http.get(`${BASE_URL}/articles/breaking`, { headers, tags: { name: 'breaking' } });
  check(breaking, { '속보 200': (r) => r.status === 200 });

  const list = http.get(`${BASE_URL}/articles?category=${encodeURIComponent('정치')}&sort=latest&page=1`, { headers, tags: { name: 'list' } });
  check(list, { '목록 200': (r) => r.status === 200 });

  sleep(1); // 실제 사용자처럼 요청 사이 텀
}
