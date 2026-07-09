import http from 'k6/http';
import { check } from 'k6';

// VPS 내부에서 실행하는 것을 기준으로 함
const BASE_URL = 'http://localhost:9080';

// 검색에서 흔히 매칭될 만한 키워드 (기사 설명에 자주 등장하는 "OOO 기자")
const SEARCH_KEYWORDS = ['기자', '연합뉴스', '대통령', '정부'];

// 이미 리사이즈 캐시(w=800)가 있는 파일 목록 — w=333/517은 아직 캐시가 없어서 첫 요청은 무조건 리사이즈를 새로 탐
const IMAGE_FILES = ['11281.jpg', '11289.jpg', '11290.jpg', '11380.jpg', '16538.jpg', '16679.jpg', '16680.jpg', '16992.jpg', '16995.jpg', '16997.jpg', '17063.jpg', '17064.jpg', '17065.jpg', '17066.jpg', '17067.jpg', '17070.jpg'];

// 아직 프록시 캐시가 없는 외부 이미지 URL들
const PROXY_URLS = [
  'https://img.yna.co.kr/etc/inner/KR/2026/07/09/AKR20260709166800073_01_i_P2.jpg',
  'https://img.yna.co.kr/photo/etc/xi/2025/12/11/PXI20251211040001009_P2.jpg',
  'https://img.yna.co.kr/etc/inner/KR/2026/07/09/AKR20260709168900104_01_i_P2.jpg',
  'https://img.yna.co.kr/etc/inner/KR/2026/07/09/AKR20260709169500007_01_i_P2.jpg',
  'https://img.yna.co.kr/photo/cms/2026/06/25/93/PCM20260625000393007_P2.jpg',
  'https://img.yna.co.kr/etc/inner/KR/2026/07/09/AKR20260709165300005_01_i_P2.jpg',
  'https://img.yna.co.kr/etc/inner/KR/2026/07/09/AKR20260709167100057_01_i_P2.jpg',
  'https://img.yna.co.kr/etc/inner/KR/2026/07/09/AKR20260709166100079_01_i_P2.jpg',
  'https://img.yna.co.kr/etc/inner/KR/2026/07/09/AKR20260709166300007_01_i_P2.jpg',
  'https://img.yna.co.kr/photo/etc/gt/2026/07/09/PGT20260709043701009_P2.jpg',
  'https://img.yna.co.kr/etc/inner/KR/2026/07/09/AKR20260709164000005_01_i_P2.jpg',
  'https://img.yna.co.kr/photo/cms/2018/11/20/81/PCM20181120000081990_P2.jpg',
];

export const options = {
  scenarios: {
    // 1. 검색: 20 VU로 30초간 다양한 키워드 검색
    search: {
      executor: 'constant-vus',
      vus: 20,
      duration: '30s',
      exec: 'searchTest',
    },
    // 2. 이미지 리사이즈 "몰림" 재현: 15 VU가 동시에 같은 파일+같은 새 width(w=333)를 요청
    image_resize_burst: {
      executor: 'per-vu-iterations',
      vus: 15,
      iterations: 1,
      exec: 'imageResizeBurst',
      startTime: '35s',
    },
    // 3. 이미지 리사이즈: 이후 다양한 파일/새 width(w=517)로 30초간 부하
    image_resize_steady: {
      executor: 'constant-vus',
      vus: 15,
      duration: '30s',
      exec: 'imageResizeSteady',
      startTime: '40s',
    },
    // 4. 이미지 프록시 "몰림" 재현: 15 VU가 동시에 같은 외부 URL을 처음 요청
    image_proxy_burst: {
      executor: 'per-vu-iterations',
      vus: 15,
      iterations: 1,
      exec: 'imageProxyBurst',
      startTime: '1m15s',
    },
    // 5. 이미지 프록시: 이후 다양한 URL로 30초간 부하
    image_proxy_steady: {
      executor: 'constant-vus',
      vus: 15,
      duration: '30s',
      exec: 'imageProxySteady',
      startTime: '1m20s',
    },
  },
  thresholds: {
    'http_req_duration{scenario:search}': ['p(95)<300'],
    'http_req_duration{scenario:image_resize_steady}': ['p(95)<300'],
    'http_req_duration{scenario:image_proxy_steady}': ['p(95)<500'],
  },
};

export function searchTest() {
  const q = SEARCH_KEYWORDS[Math.floor(Math.random() * SEARCH_KEYWORDS.length)];
  const res = http.get(`${BASE_URL}/search?q=${encodeURIComponent(q)}&page=1&limit=10`, { tags: { name: 'search' } });
  check(res, { '검색 200': (r) => r.status === 200 });
}

export function imageResizeBurst() {
  const res = http.get(`${BASE_URL}/api/images/${IMAGE_FILES[0]}?w=333`, { tags: { name: 'image_resize_burst' } });
  check(res, { '리사이즈(몰림) 200': (r) => r.status === 200 });
}

export function imageResizeSteady() {
  const file = IMAGE_FILES[Math.floor(Math.random() * IMAGE_FILES.length)];
  const res = http.get(`${BASE_URL}/api/images/${file}?w=517`, { tags: { name: 'image_resize_steady' } });
  check(res, { '리사이즈 200': (r) => r.status === 200 });
}

export function imageProxyBurst() {
  const url = encodeURIComponent(PROXY_URLS[0]);
  const res = http.get(`${BASE_URL}/api/images/proxy?url=${url}&w=400`, { tags: { name: 'image_proxy_burst' } });
  check(res, { '프록시(몰림) 200': (r) => r.status === 200 });
}

export function imageProxySteady() {
  const url = encodeURIComponent(PROXY_URLS[Math.floor(Math.random() * PROXY_URLS.length)]);
  const res = http.get(`${BASE_URL}/api/images/proxy?url=${url}&w=450`, { tags: { name: 'image_proxy_steady' } });
  check(res, { '프록시 200': (r) => r.status === 200 });
}
