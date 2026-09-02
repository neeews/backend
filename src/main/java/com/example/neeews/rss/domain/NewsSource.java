package com.example.neeews.rss.domain;

import lombok.Getter;

@Getter
public enum NewsSource {

    // 더 이상 수집하지 않음 — 기존 DB 데이터 호환용
    YONHAP("연합뉴스", "https://www.yna.co.kr/rss/news.xml", "종합"),
    DONGA("동아일보", "https://rss.donga.com/total.xml", "종합"),
    KHAN("경향신문", "https://www.khan.co.kr/rss/rssdata/total_news.xml", "종합"),

    // 정치
    YONHAP_POLITICS("연합뉴스", "https://www.yna.co.kr/rss/politics.xml", "정치"),
    HANI_POLITICS("한겨레", "https://www.hani.co.kr/rss/politics", "정치"),
    KHAN_POLITICS("경향신문", "https://www.khan.co.kr/rss/rssdata/politic_news.xml", "정치"),
    CHOSUN_POLITICS("조선일보", "https://www.chosun.com/arc/outboundfeeds/rss/category/politics/?outputType=xml", "정치"),
    DONGA_POLITICS("동아일보", "https://rss.donga.com/politics.xml", "정치"),
    SBS_POLITICS("SBS", "https://news.sbs.co.kr/news/SectionRssFeed.do?sectionId=02", "정치"),

    // 경제
    YONHAP_ECONOMY("연합뉴스", "https://www.yna.co.kr/rss/economy.xml", "경제"),
    HANKYUNG("한국경제", "https://www.hankyung.com/feed/economy", "경제"),
    KHAN_ECONOMY("경향신문", "https://www.khan.co.kr/rss/rssdata/economy_news.xml", "경제"),
    CHOSUN_ECONOMY("조선일보", "https://www.chosun.com/arc/outboundfeeds/rss/category/economy/?outputType=xml", "경제"),
    DONGA_ECONOMY("동아일보", "https://rss.donga.com/economy.xml", "경제"),
    SBS_ECONOMY("SBS", "https://news.sbs.co.kr/news/SectionRssFeed.do?sectionId=03", "경제"),

    // 사회
    YONHAP_SOCIETY("연합뉴스", "https://www.yna.co.kr/rss/society.xml", "사회"),
    HANI_SOCIETY("한겨레", "https://www.hani.co.kr/rss/society", "사회"),
    KHAN_SOCIETY("경향신문", "https://www.khan.co.kr/rss/rssdata/society_news.xml", "사회"),
    CHOSUN_SOCIETY("조선일보", "https://www.chosun.com/arc/outboundfeeds/rss/category/national/?outputType=xml", "사회"),
    DONGA_SOCIETY("동아일보", "https://rss.donga.com/national.xml", "사회"),

    // 연예/문화
    YONHAP_ENTERTAINMENT("연합뉴스", "https://www.yna.co.kr/rss/entertainment.xml", "연예/문화"),
    YONHAP_CULTURE("연합뉴스", "https://www.yna.co.kr/rss/culture.xml", "연예/문화"),
    KHAN_CULTURE("경향신문", "https://www.khan.co.kr/rss/rssdata/culture_news.xml", "연예/문화"),

    // 스포츠
    YONHAP_SPORTS("연합뉴스", "https://www.yna.co.kr/rss/sports.xml", "스포츠"),
    HANI_SPORTS("한겨레", "https://www.hani.co.kr/rss/sports", "스포츠"),
    KHAN_SPORTS("경향신문", "https://www.khan.co.kr/rss/rssdata/kh_sports.xml", "스포츠"),
    CHOSUN_SPORTS("조선일보", "https://www.chosun.com/arc/outboundfeeds/rss/category/sports/?outputType=xml", "스포츠"),

    // 세계
    YONHAP_WORLD("연합뉴스", "https://www.yna.co.kr/rss/international.xml", "세계"),
    HANI_WORLD("한겨레", "https://www.hani.co.kr/rss/international", "세계"),
    KHAN_WORLD("경향신문", "https://www.khan.co.kr/rss/rssdata/kh_world.xml", "세계"),
    CHOSUN_WORLD("조선일보", "https://www.chosun.com/arc/outboundfeeds/rss/category/international/?outputType=xml", "세계"),
    DONGA_WORLD("동아일보", "https://rss.donga.com/international.xml", "세계"),

    // IT/과학
    ETNEWS("전자신문", "https://rss.etnews.com/Section901.xml", "IT/과학"),
    ZDNET_KOREA("ZDnet코리아", "https://feeds.feedburner.com/zdkorea", "IT/과학"),
    YONHAP_IT("연합뉴스", "https://www.yna.co.kr/rss/it.xml", "IT/과학"),
    HANI_SCIENCE("한겨레", "https://www.hani.co.kr/rss/science", "IT/과학"),
    INEWS24_IT("아이뉴스24", "https://www.inews24.com/rss/news_it.xml", "IT/과학"),
    DONGA_SCIENCE("동아일보", "https://rss.donga.com/science.xml", "IT/과학"),
    // 피드가 2025-03-19 이후 갱신 중단 — 수집 대상에서 제외했으나 기존 DB 데이터 호환용으로 남긴다
    KHAN_IT("경향신문", "https://www.khan.co.kr/rss/rssdata/it_news.xml", "IT/과학");

    private final String displayName;
    private final String rssUrl;
    private final String category;

    NewsSource(String displayName, String rssUrl, String category) {
        this.displayName = displayName;
        this.rssUrl = rssUrl;
        this.category = category;
    }
}
