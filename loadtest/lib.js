import http from 'k6/http';

// 운영 서버에는 쓰기 시나리오(조회수 증가, 방문 집계)가 섞여 있어 기본값을 로컬로 둔다.
export const BASE_URL = __ENV.BASE_URL || 'http://localhost:9080';
export const JSON_HEADERS = { 'Content-Type': 'application/json' };
export const STATS = ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max', 'count'];

export function pick(list) {
  return list[Math.floor(Math.random() * list.length)];
}

export function weightedPick(items) {
  const total = items.reduce((sum, item) => sum + item.w, 0);
  let r = Math.random() * total;
  for (const item of items) {
    r -= item.w;
    if (r <= 0) return item;
  }
  return items[items.length - 1];
}

// seed.py가 만든 lt1~ltN@example.com 계정으로 로그인해 토큰을 모은다 (setup()에서 한 번만 호출)
export function loginTokens(count) {
  const tokens = [];
  for (let i = 1; i <= count; i++) {
    const res = http.post(`${BASE_URL}/auth/login`,
      JSON.stringify({ email: `lt${i}@example.com`, password: 'password123' }),
      { headers: JSON_HEADERS, tags: { name: 'setup_login' } });
    if (res.status === 200) tokens.push(res.json('accessToken'));
  }
  return tokens;
}

export function authHeader(token) {
  return { Authorization: `Bearer ${token}` };
}

// k6는 임계값이 있는 서브메트릭만 요약에 내보낸다. 실패하지 않는 느슨한 임계값으로 노출만 시킨다.
export function exposeSubmetrics(selectors) {
  const thresholds = {};
  for (const selector of selectors) {
    thresholds[selector] = [selector.startsWith('http_req_failed') ? 'rate<2' : 'p(95)<1000000000'];
  }
  return thresholds;
}

// 워밍업 구간(JIT·버퍼 풀)과 측정 구간을 분리해, 측정 구간 샘플만 비교한다.
export function openModelScenarios(exec, rate, warmup, duration, maxVUs) {
  const base = { executor: 'constant-arrival-rate', rate, timeUnit: '1s', preAllocatedVUs: Math.min(50, maxVUs), maxVUs, exec };
  return {
    warmup: { ...base, duration: warmup, tags: { phase: 'warmup' } },
    measure: { ...base, duration, startTime: warmup, tags: { phase: 'measure' } },
  };
}
