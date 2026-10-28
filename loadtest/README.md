# 부하 테스트 (k6)

결과와 해석은 [RESULTS.md](RESULTS.md)에 있다. 이 문서는 같은 측정을 다시 하는 방법이다.

## 먼저 알아둘 것

- **`.env`의 DB로 앱을 띄우지 않는다.** `DB_URL`이 운영 DB SSH 터널 포트(13306)로 보인다. 앱은 기동 즉시 이미지 정리 스케줄러가 DB의 캐시 이미지 컬럼을 지우고 RSS 수집을 시작한다. 부하 테스트는 임시 DB에서만 한다.
- 이전 스크립트(`hot-articles.js`, `article-detail.js`, `other-apis.js`)는 VPS의 기사 ID·외부 이미지 URL에 묶여 있다. 아래 스크립트는 `seed.py`가 만든 데이터를 기준으로 하며 서로 독립이다.
- `detail.js`의 `hot`·`miss` 시나리오는 DB를 바꾼다(조회수 증가, 크롤링 결과 반영). 전후 비교를 하려면 매번 같은 스냅샷에서 시작한다.

## 구성

| 파일 | 역할 |
|---|---|
| `seed.py` | 기사 10만 건·ID 목록(`ids.json`)·적재 SQL 생성 (표준 라이브러리만 사용) |
| `stub-news-server.js` | 첫 조회 크롤링을 흉내 내는 가짜 언론사 (응답 지연 `DELAY_MS`) |
| `lib.js` | 공용 설정·헬퍼 |
| `search.js` | 검색. 검색어를 흔한/중간/희소/없음으로 나눠 태그를 단다 |
| `home-feed.js` | 메인 화면 1회 로딩(속보·최신·핫이슈·헤드라인·오늘의 뉴스·방문 집계) |
| `detail.js` | 기사 상세. `SCENARIO=hit\|hot\|miss` |
| `category-list.js` | 카테고리 목록(최신순·인기순·심층 페이지) |
| `mixed.js` | 검색과 메인 화면을 동시에 (커넥션 풀 공유 영향 확인) |

## 준비

1. 임시 MySQL을 띄우고 DB·계정을 만든다 (포트는 3306·13306이 아닌 것).
2. 앱을 한 번 띄워 `ddl-auto=update`로 테이블을 만든다. 아래 환경변수로 실행한다.
3. 시드를 만들고 적재한다.

```bash
python loadtest/seed.py --out <출력 디렉터리> --articles 100000
mysql ... --local-infile=1 -D <db> -e "source <출력 디렉터리>/load.sql"
```

4. 테스트 계정 `lt1~lt200@example.com`(비밀번호 `password123`)을 `users`에 넣는다. BCrypt 해시는 `python -c "import bcrypt;print(bcrypt.hashpw(b'password123', bcrypt.gensalt(10)).decode())"`로 만들고, 같은 해시로 200행을 넣으면 된다 (`role`은 `ROLE_USER`).
5. 검색 개선본을 측정하려면 `migrations/20261001_article_fulltext_ngram.sql`을 적용한다.

### 앱 실행 환경변수

```
DB_URL / DB_USER / DB_PASSWORD   임시 DB
JWT_SECRET                       임의의 32바이트 이상 문자열
MAIL_USERNAME / MAIL_PASSWORD    더미 값 (메일을 보내는 API는 호출하지 않는다)
APP_SCHEDULING_ENABLED=false     RSS 수집·요약·판정 스케줄러 끔
SUMMARY_ENABLED=false  IMPORTANCE_ENABLED=false
MANAGEMENT_HEALTH_MAIL_ENABLED=false   더미 메일 계정이라 헬스가 503이 되는 것을 막는다
```

앱은 `.env`가 없는 디렉터리에서 실행한다 (`DotEnvPostProcessor`가 현재 디렉터리의 `.env`를 읽는다).

## 실행 예

```bash
node loadtest/stub-news-server.js            # miss 시나리오에만 필요 (DELAY_MS=1000)

k6 run -e RATE=3 -e TOTALS=1 loadtest/search.js
k6 run -e RATE=20 loadtest/home-feed.js
k6 run -e SCENARIO=hit  -e RATE=100 -e IDS_FILE=<ids.json 경로> loadtest/detail.js
k6 run -e SCENARIO=hot  -e IDS_FILE=<ids.json 경로> loadtest/detail.js
k6 run -e SCENARIO=miss -e IDS_FILE=<ids.json 경로> loadtest/detail.js
k6 run -e SEARCH_RATE=2 -e FEED_RATE=3 loadtest/mixed.js
```

- 도착률(RPS)을 고정하는 방식이라 같은 부하에서 지연을 비교할 수 있다. 앞 10초는 워밍업(`phase:warmup`)이고, 요약의 서브메트릭(`{phase:measure}`)만 비교한다.
- `search.js`는 `TOTALS=1`이면 `setup()`에서 12개 질의의 결과 개수를 `SEARCH_TOTALS`로 출력한다. 개선 전후 값이 같아야 한다.
- `hot` 시나리오의 조회수 유실은 k6 요약의 `views_ok`와 DB의 `view_count` 증가분을 비교해 구한다.
