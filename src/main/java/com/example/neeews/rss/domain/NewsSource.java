package com.example.neeews.rss.domain;

import lombok.Getter;

@Getter
public enum NewsSource {

    // 일반 (카테고리 태그 그대로 사용)
    YONHAP("연합뉴스", "https://www.yna.co.kr/rss/news.xml", null),
    DONGA("동아일보", "https://rss.donga.com/total.xml", null),
    KHAN("경향신문", "https://www.khan.co.kr/rss/rssdata/total_news.xml", null),

    // 정치
    YONHAP_POLITICS("연합뉴스", "https://www.yna.co.kr/rss/politics.xml", "정치"),
    HANI_POLITICS("한겨레", "https://www.hani.co.kr/rss/politics/", "정치"),

    // 경제
    YONHAP_ECONOMY("연합뉴스", "https://www.yna.co.kr/rss/economy.xml", "경제"),
    HANKYUNG("한국경제", "https://www.hankyung.com/feed/economy", "경제"),

    // 사회
    YONHAP_SOCIETY("연합뉴스", "https://www.yna.co.kr/rss/society.xml", "사회"),
    HANI_SOCIETY("한겨레", "https://www.hani.co.kr/rss/society/", "사회"),

    // 연예/문화
    YONHAP_ENTERTAINMENT("연합뉴스", "https://www.yna.co.kr/rss/entertainment.xml", "연예/문화"),
    YONHAP_CULTURE("연합뉴스", "https://www.yna.co.kr/rss/culture.xml", "연예/문화"),

    // 스포츠
    YONHAP_SPORTS("연합뉴스", "https://www.yna.co.kr/rss/sports.xml", "스포츠"),
    HANI_SPORTS("한겨레", "https://www.hani.co.kr/rss/sports/", "스포츠"),

    // 세계
    YONHAP_WORLD("연합뉴스", "https://www.yna.co.kr/rss/international.xml", "세계"),
    HANI_WORLD("한겨레", "https://www.hani.co.kr/rss/international/", "세계"),

    // IT/과학
    ETNEWS("전자신문", "https://rss.etnews.com/Section901.xml", "IT/과학"),
    ZDNET_KOREA("ZDnet코리아", "https://feeds.feedburner.com/zdkorea", "IT/과학"),
    YONHAP_IT("연합뉴스", "https://www.yna.co.kr/rss/it.xml", "IT/과학"),
    HANI_SCIENCE("한겨레", "https://www.hani.co.kr/rss/science/", "IT/과학");

    private final String displayName;
    private final String rssUrl;
    private final String category;

    NewsSource(String displayName, String rssUrl, String category) {
        this.displayName = displayName;
        this.rssUrl = rssUrl;
        this.category = category;
    }
}
