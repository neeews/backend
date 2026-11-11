package com.example.neeews.article.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlTextTest {

    @Test
    void 두_번_이스케이프된_엔티티를_끝까지_푼다() {
        assertThat(HtmlText.cleanTitle("컴투스, &amp;apos;2026 챔피언십&amp;apos; 개최"))
                .isEqualTo("컴투스, '2026 챔피언십' 개최");
    }

    @Test
    void Spring_HtmlUtils가_모르는_apos를_푼다() {
        assertThat(HtmlText.unescape("&apos;b&apos; &quot;c&quot;")).isEqualTo("'b' \"c\"");
    }

    @Test
    void 엔티티가_아닌_앰퍼샌드는_그대로_둔다() {
        assertThat(HtmlText.cleanTitle("AT&T, R&D 투자 확대")).isEqualTo("AT&T, R&D 투자 확대");
    }

    @Test
    void null은_null로_돌려준다() {
        assertThat(HtmlText.cleanTitle(null)).isNull();
        assertThat(HtmlText.unescape(null)).isNull();
    }
}
