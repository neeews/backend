import http from 'k6/http';
import { check, sleep } from 'k6';

// 실행 예: k6 run -e ADMIN_EMAIL=... -e ADMIN_PASSWORD=... loadtest/all-apis.js
// 다른 서버를 대상으로 할 땐 -e BASE_URL=... 을 추가 (운영: https://neeews.site/api)
const BASE_URL = __ENV.BASE_URL || 'http://localhost:9080';

// 관리자 계정 하나로 일반 사용자 API와 관리자 API를 모두 호출함. 비밀번호가 커밋되지 않도록 환경변수로만 받음
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL;
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD;

const SEARCH_KEYWORDS = ['기자', '연합뉴스', '대통령', '정부'];
const CATEGORIES = ['정치', '경제', '사회'];

// 이미 리사이즈 캐시가 있을 법한 width — 새 width로 몰림을 재현하는 건 other-apis.js가 담당
const IMAGE_WIDTH = 800;

const SETUP_LIST_PAGES = 3;

// 건의사항은 이 접두어로 만들고, 테스트가 만든 것만 수정/삭제함
const SUGGESTION_PREFIX = '[loadtest]';

// 실제 라벨링(0회차)과 자기일치율(1회차) 통계에 섞이지 않도록 안 쓰는 회차에 저장
const LABEL_ROUND = 99;

const JSON_HEADERS = { 'Content-Type': 'application/json' };

// 이메일 인증 없이 가입을 시도하므로 400이 정상 응답 — 실패율 집계에서 제외
const signupRejected = http.expectedStatuses(400);

export const options = {
  scenarios: {
    // 1. 비로그인 조회
    public_read: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 20 },
        { duration: '30s', target: 20 },
        { duration: '5s', target: 0 },
      ],
      exec: 'publicRead',
    },
    // 2. 로그인 사용자 조회
    user_read: {
      executor: 'constant-vus',
      vus: 10,
      duration: '40s',
      exec: 'userRead',
      startTime: '55s',
    },
    // 3. 로그인 사용자 쓰기 (북마크, 열람·검색 기록 삭제)
    user_write: {
      executor: 'constant-vus',
      vus: 5,
      duration: '30s',
      exec: 'userWrite',
      startTime: '1m40s',
    },
    // 4. 관리자 API
    admin: {
      executor: 'constant-vus',
      vus: 3,
      duration: '30s',
      exec: 'adminFlow',
      startTime: '2m15s',
    },
    // 5. 로그인 → 토큰 재발급 → 로그아웃. 로그인할 때 같은 계정의 기존 리프레시 토큰이 지워지므로 VU는 1개만 씀
    auth_flow: {
      executor: 'constant-vus',
      vus: 1,
      duration: '20s',
      exec: 'authFlow',
      startTime: '2m50s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{scenario:public_read}': ['p(95)<300'],
    'http_req_duration{scenario:user_read}': ['p(95)<300'],
    'http_req_duration{scenario:user_write}': ['p(95)<300'],
    'http_req_duration{scenario:admin}': ['p(95)<500'],
    // 로그인은 BCrypt 검증 때문에 원래 느림
    'http_req_duration{scenario:auth_flow}': ['p(95)<1000'],
  },
};

function pick(list) {
  return list[Math.floor(Math.random() * list.length)];
}

function authParams(token, name) {
  return { headers: { Authorization: `Bearer ${token}`, ...JSON_HEADERS }, tags: { name } };
}

function login() {
  return http.post(`${BASE_URL}/auth/login`, JSON.stringify({
    email: ADMIN_EMAIL,
    password: ADMIN_PASSWORD,
  }), { headers: JSON_HEADERS, tags: { name: 'login' } });
}

function fetchSuggestions(token) {
  return http.get(`${BASE_URL}/admin/suggestions`, authParams(token, 'admin_suggestions'));
}

function deleteSuggestion(token, id) {
  return http.del(`${BASE_URL}/admin/suggestions/${id}`, null, authParams(token, 'admin_suggestion_delete'));
}

// 테스트 전체에서 한 번만 실행 — 토큰을 발급하고, 하드코딩 대신 지금 DB에 있는 기사/이미지를 대상으로 잡음
export function setup() {
  if (!ADMIN_EMAIL || !ADMIN_PASSWORD) {
    throw new Error('ADMIN_EMAIL, ADMIN_PASSWORD 환경변수가 필요합니다.');
  }

  const loginRes = login();
  if (loginRes.status !== 200) {
    throw new Error(`로그인 실패 (${loginRes.status}): ${loginRes.body}`);
  }

  // 방금 수집된 기사는 이미지가 아직 없어서, 목록 여러 페이지에서 대상을 모음
  const articles = [];
  for (let page = 1; page <= SETUP_LIST_PAGES; page++) {
    articles.push(...(http.get(`${BASE_URL}/articles?sort=latest&page=${page}`).json('articles') || []));
  }
  if (articles.length === 0) {
    throw new Error('기사가 하나도 없어 테스트 대상을 정할 수 없습니다.');
  }

  const cachedImageUrls = [];
  const proxyUrls = [];
  for (const article of articles) {
    const url = article.imageUrl;
    if (!url) continue;
    // 서버에 캐시된 이미지는 imageUrl이 "{서버주소}/api/images/파일명?w=800" 형태의 절대 주소로 내려옴
    if (/\/api\/images\/[^/?]+(\?|$)/.test(url) && !url.includes('/api/images/proxy')) {
      cachedImageUrls.push(url);
    } else if (url.startsWith('http')) {
      proxyUrls.push(url);
    }
  }

  return {
    token: loginRes.json('accessToken'),
    articleIds: articles.map((a) => a.id),
    cachedImageUrls,
    proxyUrls,
  };
}

export function publicRead(data) {
  const tag = (name) => ({ tags: { name } });

  check(http.get(`${BASE_URL}/articles/hot`, tag('hot')), { '핫이슈 200': (r) => r.status === 200 });
  check(http.get(`${BASE_URL}/articles/latest`, tag('latest')), { '최신 기사 200': (r) => r.status === 200 });
  check(http.get(`${BASE_URL}/articles/breaking`, tag('breaking')), { '속보 200': (r) => r.status === 200 });
  check(http.get(`${BASE_URL}/articles/headlines`, tag('headlines')), { '헤드라인 200': (r) => r.status === 200 });
  check(http.get(`${BASE_URL}/articles/today`, tag('today')), { '오늘의 뉴스 200': (r) => r.status === 200 });

  const category = encodeURIComponent(pick(CATEGORIES));
  check(http.get(`${BASE_URL}/articles?category=${category}&sort=latest&page=1`, tag('list')), { '목록 200': (r) => r.status === 200 });

  check(http.get(`${BASE_URL}/articles/${pick(data.articleIds)}`, tag('detail')), { '기사 상세 200': (r) => r.status === 200 });

  const q = encodeURIComponent(pick(SEARCH_KEYWORDS));
  check(http.get(`${BASE_URL}/search?q=${q}&page=1&limit=10`, tag('search')), { '검색 200': (r) => r.status === 200 });

  check(http.post(`${BASE_URL}/visits/ping`, null, tag('visit_ping')), { '방문 기록 200': (r) => r.status === 200 });

  if (data.cachedImageUrls.length > 0) {
    check(http.get(pick(data.cachedImageUrls), tag('image')), { '이미지 200': (r) => r.status === 200 });
  }
  if (data.proxyUrls.length > 0) {
    const url = encodeURIComponent(pick(data.proxyUrls));
    const res = http.get(`${BASE_URL}/api/images/proxy?url=${url}&w=${IMAGE_WIDTH}`, tag('image_proxy'));
    check(res, { '이미지 프록시 200': (r) => r.status === 200 });
  }

  sleep(1);
}

export function userRead(data) {
  const p = (name) => authParams(data.token, name);

  check(http.get(`${BASE_URL}/auth/me`, p('auth_me')), { '내 정보(auth) 200': (r) => r.status === 200 });
  check(http.get(`${BASE_URL}/users/me`, p('users_me')), { '프로필 200': (r) => r.status === 200 });
  check(http.get(`${BASE_URL}/users/me/bookmarks`, p('bookmarks')), { '북마크 목록 200': (r) => r.status === 200 });
  check(http.get(`${BASE_URL}/users/me/history`, p('history')), { '열람 기록 200': (r) => r.status === 200 });
  check(http.get(`${BASE_URL}/users/me/search-history`, p('search_history')), { '검색 기록 200': (r) => r.status === 200 });

  // 로그인 상태에서는 열람 기록·검색 기록 저장이 함께 일어남
  check(http.get(`${BASE_URL}/articles/${pick(data.articleIds)}`, p('detail_authed')), { '기사 상세(로그인) 200': (r) => r.status === 200 });
  const q = encodeURIComponent(pick(SEARCH_KEYWORDS));
  check(http.get(`${BASE_URL}/search?q=${q}&page=1&limit=10`, p('search_authed')), { '검색(로그인) 200': (r) => r.status === 200 });
  check(http.get(`${BASE_URL}/articles/headlines`, p('headlines_authed')), { '헤드라인(로그인) 200': (r) => r.status === 200 });

  sleep(1);
}

export function userWrite(data) {
  const p = (name) => authParams(data.token, name);
  // VU마다 다른 기사를 잡아, 같은 계정으로 같은 북마크를 동시에 만드는 충돌을 피함
  const articleId = data.articleIds[(__VU - 1) % data.articleIds.length];

  check(http.post(`${BASE_URL}/articles/${articleId}/bookmark`, null, p('bookmark_add')), { '북마크 추가 200': (r) => r.status === 200 });
  check(http.del(`${BASE_URL}/articles/${articleId}/bookmark`, null, p('bookmark_remove')), { '북마크 삭제 204': (r) => r.status === 204 });

  http.get(`${BASE_URL}/articles/${articleId}`, p('detail_authed'));
  check(http.del(`${BASE_URL}/users/me/history/${articleId}`, null, p('history_remove')), { '열람 기록 1건 삭제 204': (r) => r.status === 204 });
  check(http.del(`${BASE_URL}/users/me/history`, null, p('history_clear')), { '열람 기록 전체 삭제 204': (r) => r.status === 204 });
  check(http.del(`${BASE_URL}/users/me/search-history`, null, p('search_history_clear')), { '검색 기록 전체 삭제 204': (r) => r.status === 204 });

  sleep(1);
}

export function adminFlow(data) {
  const p = (name) => authParams(data.token, name);

  check(http.get(`${BASE_URL}/admin/health`, p('admin_health')), { '파이프라인 상태 200': (r) => r.status === 200 });
  check(http.get(`${BASE_URL}/admin/visits/stats`, p('admin_visit_stats')), { '방문 통계 200': (r) => r.status === 200 });
  check(http.get(`${BASE_URL}/rss/categories`, p('rss_categories')), { '카테고리 통계 200': (r) => r.status === 200 });

  check(http.get(`${BASE_URL}/admin/labeling/next?size=20&round=${LABEL_ROUND}`, p('labeling_next')), { '라벨링 대상 200': (r) => r.status === 200 });
  // VU마다 다른 기사를 잡아, (기사, 라벨러, 회차) 유니크 제약에 동시에 걸리지 않게 함
  const articleId = data.articleIds[(__VU - 1) % data.articleIds.length];
  const labelRes = http.post(`${BASE_URL}/admin/labeling/${articleId}`, JSON.stringify({
    label: pick(['HIGH', 'MEDIUM', 'LOW']),
    round: LABEL_ROUND,
  }), p('labeling_label'));
  check(labelRes, { '라벨 저장 200': (r) => r.status === 200 });
  check(http.get(`${BASE_URL}/admin/labeling/stats`, p('labeling_stats')), { '라벨링 통계 200': (r) => r.status === 200 });

  // 건의 등록 → 목록에서 방금 만든 건의 찾기 → 상태 변경 → 삭제
  const title = `${SUGGESTION_PREFIX} ${__VU}-${__ITER}-${Date.now()}`;
  const submitRes = http.post(`${BASE_URL}/suggestions`, JSON.stringify({
    title,
    content: '부하 테스트가 만든 건의사항입니다.',
  }), p('suggestion_submit'));
  check(submitRes, { '건의 등록 200': (r) => r.status === 200 });

  const listRes = fetchSuggestions(data.token);
  check(listRes, { '건의 목록 200': (r) => r.status === 200 });
  const mine = listRes.status === 200 ? listRes.json().find((s) => s.title === title) : null;
  if (mine) {
    const statusRes = http.patch(`${BASE_URL}/admin/suggestions/${mine.id}/status`,
      JSON.stringify({ status: 'IN_PROGRESS' }), p('admin_suggestion_status'));
    check(statusRes, { '건의 상태 변경 200': (r) => r.status === 200 });
    check(deleteSuggestion(data.token, mine.id), { '건의 삭제 204': (r) => r.status === 204 });
  }

  sleep(1);
}

export function authFlow() {
  const signupRes = http.post(`${BASE_URL}/auth/signup`, JSON.stringify({
    name: '부하테스트',
    email: `loadtest-${__VU}-${__ITER}-${Date.now()}@example.com`,
    password: 'password123',
  }), { headers: JSON_HEADERS, tags: { name: 'signup' }, responseCallback: signupRejected });
  check(signupRes, { '미인증 가입 거절 400': (r) => r.status === 400 });

  const loginRes = login();
  check(loginRes, { '로그인 200': (r) => r.status === 200 });
  if (loginRes.status !== 200) return;

  const refreshRes = http.post(`${BASE_URL}/auth/token/refresh`,
    JSON.stringify({ refreshToken: loginRes.json('refreshToken') }),
    { headers: JSON_HEADERS, tags: { name: 'token_refresh' } });
  check(refreshRes, { '토큰 재발급 200': (r) => r.status === 200 });
  if (refreshRes.status !== 200) return;

  const logoutRes = http.post(`${BASE_URL}/auth/logout`,
    JSON.stringify({ refreshToken: refreshRes.json('refreshToken') }),
    { headers: JSON_HEADERS, tags: { name: 'logout' } });
  check(logoutRes, { '로그아웃 204': (r) => r.status === 204 });

  sleep(1);
}

// 중간에 실패해서 남은 테스트용 건의사항을 정리
export function teardown(data) {
  const listRes = fetchSuggestions(data.token);
  if (listRes.status !== 200) return;
  for (const s of listRes.json()) {
    if (s.title && s.title.startsWith(SUGGESTION_PREFIX)) {
      deleteSuggestion(data.token, s.id);
    }
  }
}
