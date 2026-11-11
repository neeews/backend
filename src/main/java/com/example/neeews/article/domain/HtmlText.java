package com.example.neeews.article.domain;

import org.jsoup.parser.Parser;

// Spring HtmlUtils.htmlUnescape는 &apos;를 모르므로 RSS 텍스트의 엔티티는 반드시 여기서 푼다.
public final class HtmlText {

    // 아이뉴스24처럼 엔티티를 두 번 이스케이프해 보내는 피드(&amp;apos;)가 있어, 더 바뀌지 않을 때까지 반복해 푼다.
    private static final int MAX_UNESCAPE_PASSES = 3;

    private HtmlText() {
    }

    public static String unescape(String text) {
        if (text == null) return null;
        String current = text;
        for (int i = 0; i < MAX_UNESCAPE_PASSES; i++) {
            String next = Parser.unescapeEntities(current, false);
            if (next.equals(current)) break;
            current = next;
        }
        return current;
    }

    public static String cleanTitle(String title) {
        if (title == null) return null;
        return unescape(title).strip();
    }
}
