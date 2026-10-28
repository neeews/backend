// 상세 조회의 "첫 조회 크롤링"(본문·이미지)을 재현하기 위한 가짜 언론사 서버.
// ExternalImageFetcher가 루프백을 막아서 이미지는 받지 못하지만, 본문 크롤링(Jsoup)은 이 서버로 나간다.
//
// 실행: DELAY_MS=1000 node loadtest/stub-news-server.js
//   DELAY_MS  응답 지연(ms). 실제 언론사 응답 시간을 흉내 낸다 (기본 1000)
//   PORT      기본 19090
// 통계: GET /__stats (동시 처리 최대치, 총 요청 수), GET /__reset
const http = require('http');

const PORT = Number(process.env.PORT || 19090);
const DELAY_MS = Number(process.env.DELAY_MS || 1000);

let inflight = 0;
let maxInflight = 0;
let total = 0;

const paragraph = '정부는 오늘 관련 부처 합동 점검 결과를 발표하고 후속 대책을 마련하겠다고 밝혔다. '
  + '관계자는 현재 상황을 예의주시하고 있으며 필요한 조치를 신속히 취하겠다고 설명했다. ';

function page(id) {
  const body = Array.from({ length: 6 }, () => `<p>${paragraph.repeat(2)}</p>`).join('\n');
  return `<!doctype html><html><head><meta charset="utf-8">
<meta property="og:image" content="http://127.0.0.1:${PORT}/img/${id}.jpg">
<title>기사 ${id}</title></head><body><article>${body}</article></body></html>`;
}

http.createServer((req, res) => {
  if (req.url === '/__stats') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ inflight, maxInflight, total }));
    return;
  }
  if (req.url === '/__reset') {
    maxInflight = 0;
    total = 0;
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('ok');
    return;
  }
  total++;
  inflight++;
  maxInflight = Math.max(maxInflight, inflight);
  setTimeout(() => {
    inflight--;
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(page(req.url.split('/').pop()));
  }, DELAY_MS);
}).listen(PORT, '127.0.0.1', () => console.log(`stub news server on :${PORT}, delay ${DELAY_MS}ms`));
