#!/usr/bin/env python3
"""neeews 부하 테스트용 시드 데이터 생성기 (표준 라이브러리만 사용).

운영 DB를 건드리지 않고 격리된 DB에 운영과 비슷한 규모·분포의 기사를 채우기 위한 도구다.
  articles.csv  LOAD DATA용 기사 데이터
  load.sql      articles.csv를 적재하는 SQL
  ids.json      k6 스크립트가 읽는 기사 ID 목록 (캐시 히트/미스/인기)

분포는 코드와 커밋 메시지에서 확인한 값에 맞춘 가정이다.
  - 본문 길이: 크롤링 성공 기사 평균 1,018자, RSS 요약만 있는 기사 평균 359자 (RssFetchService 주석)
  - 요약 후보: ai_importance_score >= 0.93 (app.summary.min-score 기본값)
  - 같은 사건을 여러 매체가 쓴 묶음: 제목이 비슷한 기사를 서로 다른 매체로 생성

사용 예: python loadtest/seed.py --out <디렉터리> [--articles 100000]
"""
import argparse
import csv
import datetime as dt
import json
import math
import os
import random

# (NewsSource 이름, 표시명, 카테고리, 비중). 수집 중단된 소스는 뺐다.
SOURCES = [
    ("YONHAP_POLITICS", "연합뉴스", "정치", 3.0), ("HANI_POLITICS", "한겨레", "정치", 1.2),
    ("KHAN_POLITICS", "경향신문", "정치", 1.2), ("CHOSUN_POLITICS", "조선일보", "정치", 1.5),
    ("DONGA_POLITICS", "동아일보", "정치", 1.5), ("SBS_POLITICS", "SBS", "정치", 1.0),
    ("YONHAP_ECONOMY", "연합뉴스", "경제", 3.0), ("KHAN_ECONOMY", "경향신문", "경제", 1.2),
    ("CHOSUN_ECONOMY", "조선일보", "경제", 1.5), ("DONGA_ECONOMY", "동아일보", "경제", 1.5),
    ("SBS_ECONOMY", "SBS", "경제", 1.0),
    ("YONHAP_SOCIETY", "연합뉴스", "사회", 3.0), ("HANI_SOCIETY", "한겨레", "사회", 1.2),
    ("KHAN_SOCIETY", "경향신문", "사회", 1.2), ("CHOSUN_SOCIETY", "조선일보", "사회", 1.5),
    ("DONGA_SOCIETY", "동아일보", "사회", 1.5),
    ("YONHAP_ENTERTAINMENT", "연합뉴스", "연예/문화", 2.0), ("YONHAP_CULTURE", "연합뉴스", "연예/문화", 2.0),
    ("KHAN_CULTURE", "경향신문", "연예/문화", 1.0),
    ("YONHAP_SPORTS", "연합뉴스", "스포츠", 3.0), ("HANI_SPORTS", "한겨레", "스포츠", 1.0),
    ("KHAN_SPORTS", "경향신문", "스포츠", 1.0), ("CHOSUN_SPORTS", "조선일보", "스포츠", 1.5),
    ("YONHAP_WORLD", "연합뉴스", "세계", 3.0), ("HANI_WORLD", "한겨레", "세계", 1.0),
    ("KHAN_WORLD", "경향신문", "세계", 1.0), ("CHOSUN_WORLD", "조선일보", "세계", 1.5),
    ("DONGA_WORLD", "동아일보", "세계", 1.5),
    ("ETNEWS", "전자신문", "IT/과학", 2.0), ("ZDNET_KOREA", "ZDnet코리아", "IT/과학", 1.5),
    ("HANI_SCIENCE", "한겨레", "IT/과학", 0.8), ("INEWS24_IT", "아이뉴스24", "IT/과학", 2.0),
    ("DONGA_SCIENCE", "동아일보", "IT/과학", 1.0),
]

# 검색 테스트용 키워드는 일부러 어휘에서 빼고 따로 주입해 포함 비율(선택도)을 통제한다.
CONTROLLED_KEYWORDS = [("정부", 0.20), ("대통령", 0.09), ("환율", 0.018),
                       ("삼성전자", 0.013), ("손흥민", 0.004), ("태풍", 0.005)]
BYLINE_PROBABILITY = 0.93  # "기자"가 들어간 본문 비율

NOUNS = {
    "정치": ["국회", "여당", "야당", "총리", "장관", "의원", "대표", "청문회", "탄핵", "선거", "공천", "예산안",
           "국정감사", "외교", "안보", "북한", "정상회담", "법안", "개헌", "특검", "검찰", "여야", "민주당",
           "국민의힘", "대통령실", "국방부", "통일부", "외교부", "원내대표", "최고위원회의"],
    "경제": ["금리", "코스피", "반도체", "물가", "수출", "부동산", "주택", "기준금리", "한국은행", "증시", "달러",
           "유가", "무역", "관세", "SK하이닉스", "현대차", "배터리", "은행", "대출", "가계부채", "실업률",
           "경기침체", "투자", "상장", "인수합병", "적자", "흑자", "소비자물가", "공급망", "스타트업"],
    "사회": ["경찰", "검찰", "법원", "사고", "화재", "폭우", "교통", "학교", "교육부", "병원", "의료", "노동",
           "집회", "재판", "판결", "범죄", "실종", "폭행", "구속영장", "항소심", "대법원", "소방당국",
           "응급실", "의대", "근로자", "노조", "임금", "재해", "지자체", "주민"],
    "세계": ["미국", "중국", "일본", "러시아", "우크라이나", "중동", "이스라엘", "유럽", "유엔", "정상회담",
           "전쟁", "휴전", "제재", "백악관", "의회", "총선", "대선", "난민", "NATO", "EU", "인도",
           "대만", "북핵", "이란", "가자지구", "관세협상", "무역분쟁", "외교장관", "대사관", "국제사회"],
    "IT/과학": ["AI", "인공지능", "반도체", "스마트폰", "갤럭시", "애플", "구글", "네이버", "카카오", "로봇",
              "우주", "누리호", "양자컴퓨터", "보안", "해킹", "클라우드", "데이터센터", "전기차", "배터리",
              "자율주행", "생성형", "챗봇", "메타버스", "5G", "6G", "위성", "연구팀", "논문", "신약", "백신"],
    "스포츠": ["프로야구", "K리그", "월드컵", "올림픽", "메이저리그", "이강인", "김민재", "축구", "야구",
             "농구", "배구", "골프", "우승", "감독", "선수", "리그", "결승", "준결승", "국가대표", "대표팀",
             "트레이드", "FA", "홈런", "연승", "연패", "승점", "플레이오프", "아시안게임", "마라톤", "수영"],
    "연예/문화": ["드라마", "영화", "가수", "아이돌", "콘서트", "박스오피스", "넷플릭스", "배우", "음악",
              "전시", "공연", "축제", "출연", "컴백", "앨범", "예능", "시상식", "개봉", "관객", "흥행",
              "뮤지컬", "웹툰", "소설", "베스트셀러", "팬미팅", "월드투어", "OST", "감독상", "제작발표회", "시즌2"],
}
GENERIC = ["정책", "계획", "방안", "논의", "상황", "결과", "관계자", "당국", "전문가", "업계", "시장", "지역",
           "사업", "예산", "조사", "분석", "협의", "입장", "대응", "발표", "추진", "확대", "지원", "점검",
           "규모", "비율", "수준", "기준", "과정", "내용"]
PREDICATES = ["급등", "급락", "논란 확산", "협상 난항", "합의 도출", "전망 엇갈려", "공식 발표", "재개",
              "중단", "검토 착수", "강행", "제동", "우려 커져", "역대 최대", "첫 공개", "본격화",
              "갈등 격화", "수사 확대", "징계 논의", "출시 임박", "속도 낸다", "재추진", "반발", "급물살"]
TITLE_TAGS = ["[속보] ", "[단독] ", "[종합] ", "[1보] ", "[이슈] "]
VERBS = ["밝혔다", "말했다", "전했다", "강조했다", "설명했다", "주장했다", "확인했다", "발표했다", "예고했다", "요청했다"]
TIMES = ["오늘", "어제", "지난달", "올해", "이번 주", "내년", "이날", "최근", "지난해", "앞서"]
CONNECT = ["이에 대해", "한편", "이와 관련해", "다만", "그러면서", "또", "특히", "아울러"]
SURNAMES = "김이박최정강조윤장임한오서신권황안송류전홍고문양손배조백허유남심노하곽성차주우구민나진지엄채원천방공"
GIVEN = ["민준", "서연", "지훈", "서윤", "도윤", "하은", "예준", "지우", "시우", "수아", "주원", "다은", "현우", "채원", "건우"]
HOUR_WEIGHTS = [1, 1, 1, 1, 1, 1.5, 2.5, 4, 5, 5, 5, 5, 4.5, 4.5, 4.5, 4.5, 4.5, 4.5, 4, 3.5, 3, 2.5, 2, 1.5]

SUMMARY_MIN_SCORE = 0.93
RSS_ONLY_RATIO = 0.30
CRAWLED_MEDIAN_CHARS, CRAWLED_SIGMA = 850, 0.55   # 평균 약 990자
RSS_ONLY_MEDIAN_CHARS, RSS_ONLY_SIGMA = 330, 0.40  # 평균 약 357자
HIGH_RATIO = 0.5
UNJUDGED_WINDOW_MINUTES = 90
CACHE_WINDOW_DAYS = 6   # 이미지 정리 스케줄러(7일 미조회 삭제)에 걸리지 않게 이 안에서만 조회 처리
MISS_POOL_SIZE = 800
HIT_POOL_SIZE = 2000
HOT_POOL_SIZE = 50
EVENT_SIZES = [1, 2, 3, 4, 5]
EVENT_SIZE_WEIGHTS = [35, 30, 20, 10, 5]

STUB_BASE = "http://127.0.0.1:19090"
APP_BASE = "http://localhost:9080"


def has_batchim(word):
    ch = word[-1]
    return '가' <= ch <= '힣' and (ord(ch) - 0xAC00) % 28 != 0


def josa(word, pair):
    return word + (pair[0] if has_batchim(word) else pair[1])


class Generator:
    def __init__(self, seed):
        self.rng = random.Random(seed)
        self.source_weights = [s[3] for s in SOURCES]

    def pick(self, items):
        return items[self.rng.randrange(len(items))]

    def person(self):
        return self.pick(SURNAMES) + self.pick(GIVEN)

    def fact(self):
        r = self.rng.random()
        if r < 0.4:
            return f"{self.rng.randint(1, 99)}%"
        if r < 0.7:
            return f"{self.rng.randint(2, 900)}억원"
        return f"{self.rng.randint(2, 500)}명"

    def words(self, category, n):
        return [self.pick(NOUNS[category]) if self.rng.random() < 0.75 else self.pick(GENERIC) for _ in range(n)]

    def sentence(self, category):
        a, b, c = self.words(category, 3)
        t = self.pick(TIMES)
        verb = self.pick(VERBS)
        form = self.rng.randrange(6)
        if form == 0:
            return f"{t} {josa(a, ('은', '는'))} {josa(b, ('과', '와'))} 관련해 {c} 방안을 {verb}."
        if form == 1:
            return f"{self.pick(CONNECT)} {josa(a, ('이', '가'))} {b} {josa(c, ('을', '를'))} {self.fact()} 규모로 확대하겠다고 {verb}."
        if form == 2:
            return f"{josa(a, ('은', '는'))} {t} {b} 문제에 대해 \"{c} 점검이 필요하다\"고 {verb}."
        if form == 3:
            return f"{self.pick(CONNECT)} {josa(b, ('은', '는'))} 지난해보다 {self.fact()} 늘어난 것으로 {verb}."
        if form == 4:
            return f"{self.person()} {self.pick(GENERIC)}은 {josa(a, ('과', '와'))} {b} {c}에 관해 입장을 {verb}."
        return f"{t} {josa(a, ('은', '는'))} {josa(b, ('이', '가'))} {c} 단계에 들어섰다고 {verb}."

    def paragraphs(self, category, target_chars):
        out, total = [], 0
        while total < target_chars:
            sentences = [self.sentence(category) for _ in range(self.rng.randint(2, 4))]
            para = " ".join(sentences)
            out.append(para)
            total += len(para)
        return "\n\n".join(out)

    def title(self, category):
        a, b = self.words(category, 2)
        tag = self.pick(TITLE_TAGS) if self.rng.random() < 0.08 else ""
        pred = self.pick(PREDICATES)
        if self.rng.random() < 0.5:
            return f"{tag}{a} {b}, {pred}"
        return f"{tag}{josa(a, ('이', '가'))} {b} {pred}…{self.pick(GENERIC)} 논의"

    def sample_timestamp(self, today0, now, days):
        while True:
            day = self.rng.randrange(days)
            hour = self.rng.choices(range(24), HOUR_WEIGHTS)[0]
            ts = today0 - dt.timedelta(days=day) + dt.timedelta(
                hours=hour, minutes=self.rng.randrange(60), seconds=self.rng.randrange(60))
            if ts <= now - dt.timedelta(minutes=3):
                return ts


def fmt(ts):
    return ts.strftime("%Y-%m-%d %H:%M:%S") if ts else r"\N"


def lognormal_chars(rng, median, sigma, lo, hi):
    return int(min(hi, max(lo, rng.lognormvariate(math.log(median), sigma))))


def build_articles(gen, total, days):
    rng = gen.rng
    now = dt.datetime.now().replace(microsecond=0)
    today0 = now.replace(hour=0, minute=0, second=0)
    cache_cutoff = now - dt.timedelta(days=CACHE_WINDOW_DAYS)
    unjudged_after = now - dt.timedelta(minutes=UNJUDGED_WINDOW_MINUTES)

    rows = []
    for _ in range(total):
        source = rng.choices(SOURCES, gen.source_weights)[0]
        ts = gen.sample_timestamp(today0, now, days)
        rows.append({"published_at": ts, "source": source})
    rows.sort(key=lambda r: r["published_at"])

    for i, r in enumerate(rows, start=1):
        enum_name, display, category, _ = r["source"]
        ts = r["published_at"]
        r.update(id=i, display=display, category=category, enum_name=enum_name)

        r["fetched_at"] = min(ts + dt.timedelta(seconds=rng.randint(20, 600)), now - dt.timedelta(seconds=1))
        if r["fetched_at"] < ts:
            r["fetched_at"] = ts

        r["rss_only"] = rng.random() < RSS_ONLY_RATIO
        r["judged"] = r["fetched_at"] < unjudged_after
        if r["judged"]:
            high = rng.random() < HIGH_RATIO
            r["importance"] = "HIGH" if high else "LOW"
            if high:
                r["score"] = round(0.5 + 0.5 * rng.random(), 4)
            else:
                r["score"] = None if rng.random() < 0.10 else round(0.5 * rng.random(), 4)
            r["judged_at"] = min(r["fetched_at"] + dt.timedelta(seconds=rng.randint(30, 400)), now)
        else:
            r["importance"], r["score"], r["judged_at"] = None, None, None

        recent = ts >= cache_cutoff
        viewed = rng.random() < (0.35 if recent else 0.25)
        r["view_count"] = min(20000, int(rng.paretovariate(1.2))) if viewed else 0
        r["last_viewed"] = None
        r["cached"] = None
        r["image_url"] = f"https://img.example.com/news/{i}.jpg" if rng.random() < 0.7 else None
        if viewed:
            lo = max(ts, cache_cutoff) if recent else ts
            hi = now if recent else min(now, ts + dt.timedelta(days=10))
            if hi <= lo:
                hi = lo + dt.timedelta(minutes=1)
            r["last_viewed"] = lo + dt.timedelta(seconds=rng.randrange(max(1, int((hi - lo).total_seconds()))))
            if recent:
                r["cached"] = f"{i}.jpg" if rng.random() < 0.9 else "none"
                r["rss_only"] = False
                if r["cached"] != "none":
                    r["image_url"] = f"{APP_BASE}/api/images/{i}.jpg?w=800"

        r["summary"] = None
        r["summarized_at"] = None
        r["event_title_core"] = None
    return rows, now


def assign_summaries_and_events(gen, rows, now):
    rng = gen.rng
    candidates = [r for r in rows
                  if r["judged"] and r["importance"] == "HIGH" and r["score"] >= SUMMARY_MIN_SCORE
                  and r["published_at"] >= now - dt.timedelta(days=30)
                  and r["fetched_at"] < now - dt.timedelta(minutes=UNJUDGED_WINDOW_MINUTES)
                  and rng.random() < 0.92]

    groups = {}
    for r in candidates:
        groups.setdefault((r["published_at"].date(), r["category"]), []).append(r)

    event_count = 0
    for members in groups.values():
        rng.shuffle(members)
        idx = 0
        while idx < len(members):
            size = rng.choices(EVENT_SIZES, EVENT_SIZE_WEIGHTS)[0]
            chunk = members[idx: idx + size]
            idx += size
            category = chunk[0]["category"]
            core = gen.words(category, 3)
            core_sentences = [gen.sentence(category) for _ in range(2)]
            event_count += 1
            for r in chunk:
                pred = gen.pick(PREDICATES)
                r["event_title"] = f"{core[0]} {core[1]} {core[2]} {pred}"
                r["summary"] = " ".join(core_sentences + [gen.sentence(category)])
                r["summarized_at"] = min(r["judged_at"] + dt.timedelta(minutes=rng.randint(5, 120)), now)
    return len(candidates), event_count


def finalize_text(gen, rows):
    rng = gen.rng
    kw_pool = CONTROLLED_KEYWORDS
    for r in rows:
        category, display = r["category"], r["display"]
        if r.get("event_title"):
            title = r["event_title"]
        else:
            title = gen.title(category)

        injected = []
        for kw, p in kw_pool:
            if rng.random() < p:
                injected.append(kw)
        if injected and rng.random() < 0.5:
            title = f"{injected[0]} {title}"

        if r["rss_only"]:
            chars = lognormal_chars(rng, RSS_ONLY_MEDIAN_CHARS, RSS_ONLY_SIGMA, 80, 900)
        else:
            chars = lognormal_chars(rng, CRAWLED_MEDIAN_CHARS, CRAWLED_SIGMA, 300, 8000)
        body = gen.paragraphs(category, chars)
        for kw in injected:
            pos = rng.randrange(len(body) + 1)
            body = body[:pos] + f" {kw} 관련 논의가 이어지고 있다. " + body[pos:]

        byline = ""
        if rng.random() < BYLINE_PROBABILITY:
            name = gen.person()
            if display == "연합뉴스":
                byline = f"(서울=연합뉴스) {name} 기자 = "
            else:
                byline = f"{name} 기자 "
        r["title"] = title[:250]
        r["description"] = byline + body
        r["author"] = display


def write_outputs(rows, out_dir):
    os.makedirs(out_dir, exist_ok=True)
    csv_path = os.path.join(out_dir, "articles.csv")
    with open(csv_path, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f, delimiter="\t", quotechar='"', quoting=csv.QUOTE_MINIMAL, lineterminator="\n")
        for r in rows:
            def nz(v):
                return r"\N" if v is None else v
            w.writerow([
                r["id"], r["title"], f"{STUB_BASE}/a/{r['id']}", r["description"], r["author"], r["category"],
                nz(r["image_url"]), r["enum_name"], fmt(r["published_at"]), r["view_count"], fmt(r["fetched_at"]),
                nz(r["cached"]), fmt(r["last_viewed"]) if r["last_viewed"] else r"\N",
                nz(r["summary"]), fmt(r["summarized_at"]) if r["summarized_at"] else r"\N",
                nz(r["importance"]), nz(r["score"]), fmt(r["judged_at"]) if r["judged_at"] else r"\N",
                1 if r["content_crawled"] else 0,
            ])
    sql_path = os.path.join(out_dir, "load.sql")
    with open(sql_path, "w", encoding="utf-8") as f:
        f.write(
            "LOAD DATA LOCAL INFILE '" + csv_path.replace("\\", "/") + "' INTO TABLE articles\n"
            "CHARACTER SET utf8mb4\n"
            "FIELDS TERMINATED BY '\\t' OPTIONALLY ENCLOSED BY '\"' ESCAPED BY '\\\\'\n"
            "LINES TERMINATED BY '\\n'\n"
            "(id, title, link, description, author, category, image_url, source, published_at, view_count,\n"
            " fetched_at, cached_image_path, last_viewed_at, ai_summary, ai_summarized_at, ai_importance,\n"
            " ai_importance_score, ai_importance_at, @cc)\n"
            "SET content_crawled = IF(@cc = '1', b'1', b'0');\n"
        )
    return csv_path, sql_path


def pick_pools(gen, rows, now):
    rng = gen.rng
    two_days = now - dt.timedelta(days=2)
    miss_candidates = [r for r in rows if r["published_at"] >= two_days and r["view_count"] == 0
                       and r["cached"] is None and not r["summary"]]
    rng.shuffle(miss_candidates)
    miss = miss_candidates[:MISS_POOL_SIZE]
    for r in miss:
        # 첫 조회 때 이미지·본문을 둘 다 크롤링하게 되는 새 기사
        r["image_url"] = None
        r["rss_only"] = True
        r["content_crawled"] = False
    return miss


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--articles", type=int, default=100_000)
    ap.add_argument("--days", type=int, default=45)
    ap.add_argument("--seed", type=int, default=20261001)
    args = ap.parse_args()

    gen = Generator(args.seed)
    rows, now = build_articles(gen, args.articles, args.days)
    for r in rows:
        r["content_crawled"] = not r["rss_only"]
    candidates, events = assign_summaries_and_events(gen, rows, now)
    finalize_text(gen, rows)
    miss = pick_pools(gen, rows, now)
    miss_ids = {r["id"] for r in miss}
    for r in miss:
        r["description"] = r["description"][:RSS_ONLY_MEDIAN_CHARS + 60]

    cached_recent = [r for r in rows if r["cached"] and r["cached"] != "none" and r["content_crawled"]
                     and r["id"] not in miss_ids]
    cached_recent.sort(key=lambda r: -r["view_count"])
    hot = [r["id"] for r in cached_recent[:HOT_POOL_SIZE]]
    rest = cached_recent[HOT_POOL_SIZE:]
    gen.rng.shuffle(rest)
    hit = [r["id"] for r in rest[:HIT_POOL_SIZE]]

    csv_path, sql_path = write_outputs(rows, args.out)
    ids_path = os.path.join(args.out, "ids.json")
    with open(ids_path, "w", encoding="utf-8") as f:
        json.dump({"hit": hit, "hot": hot, "hotOne": hot[0], "miss": sorted(miss_ids),
                   "generatedAt": now.isoformat()}, f)

    today0 = now.replace(hour=0, minute=0, second=0)
    today = [r for r in rows if r["published_at"] >= today0]
    desc_chars = sum(len(r["description"]) for r in rows) / len(rows)
    print(f"articles={len(rows)} now={now} avg_description_chars={desc_chars:.0f}")
    print(f"today={len(today)} today_HIGH={sum(1 for r in today if r['importance'] == 'HIGH')} "
          f"today_summarized_candidates={sum(1 for r in today if r['summary'])}")
    print(f"summarized_total={sum(1 for r in rows if r['summary'])} candidate_events_total={events} "
          f"(candidates before drop={candidates})")
    print(f"pools: hit={len(hit)} hot={len(hot)} miss={len(miss_ids)}")
    print(f"csv={csv_path} ({os.path.getsize(csv_path) / 1e6:.0f}MB) sql={sql_path} ids={ids_path}")


if __name__ == "__main__":
    main()
