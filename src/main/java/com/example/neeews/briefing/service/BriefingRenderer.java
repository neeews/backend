package com.example.neeews.briefing.service;

import com.example.neeews.devstack.domain.StackUpdateType;
import com.example.neeews.devstack.dto.response.StackUpdateResponse;
import com.example.neeews.huggingface.domain.CommercialUse;
import com.example.neeews.huggingface.dto.response.ModelResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// 메일 앱은 <style>과 외부 CSS를 지우는 곳이 많아 스타일을 태그마다 인라인으로 넣는다.
// 일반 텍스트 본문도 함께 만든다. HTML만 보내면 스팸으로 분류되기 쉽고, 텍스트만 보는 메일 앱도 있다.
@Component
public class BriefingRenderer {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN);
    private static final Map<String, String> SEVERITY_LABELS = Map.of(
            "critical", "심각", "high", "높음", "medium", "보통", "low", "낮음");
    private static final Map<CommercialUse, String> COMMERCIAL_USE_LABELS = Map.of(
            CommercialUse.ALLOWED, "상업 이용 가능",
            CommercialUse.NON_COMMERCIAL, "비상업용",
            CommercialUse.CHECK_REQUIRED, "라이선스 확인 필요");
    private static final long BILLION = 1_000_000_000L;
    private static final long MILLION = 1_000_000L;
    // 이보다 큰 수는 소수점 없이 쓴다 (763.2B → 763B, 209.3M → 209M).
    private static final double ROUND_ABOVE = 100;

    private static final String BODY_STYLE = "max-width:640px;margin:0 auto;padding:0 16px;color:#222;"
            + "font-family:'Apple SD Gothic Neo','Malgun Gothic',sans-serif;";
    private static final String SECTION_STYLE = "font-size:17px;margin:28px 0 10px;padding-bottom:6px;border-bottom:1px solid #e5e5e5;";
    private static final String ITEM_STYLE = "margin:0 0 12px;line-height:1.6;";
    private static final String META_STYLE = "color:#888;font-size:13px;";
    private static final String LINK_STYLE = "color:#1a56db;text-decoration:none;";

    @Value("${app.briefing.site-url}")
    private String siteUrl;

    @Value("${app.briefing.api-url}")
    private String apiUrl;

    // unsubscribeToken이 null이면(미리보기·테스트 발송) 수신 거부 링크를 넣지 않는다.
    public BriefingMail render(BriefingContent content, String unsubscribeToken) {
        String unsubscribeUrl = unsubscribeToken == null ? null
                : apiUrl + "/briefing/unsubscribe?token=" + URLEncoder.encode(unsubscribeToken, StandardCharsets.UTF_8);
        String date = content.date().format(DATE_FORMAT);
        return new BriefingMail("neeews 아침 브리핑 · " + date,
                html(content, date, unsubscribeUrl), text(content, date, unsubscribeUrl), unsubscribeUrl);
    }

    // 폼에 action을 따로 적는다. 비워 두면 현재 주소(?token=...)로 보내져 token이 두 번 실려 값이 합쳐진다.
    public String unsubscribeConfirmPage(String token) {
        if (token == null || token.isBlank()) {
            return page("링크가 올바르지 않습니다", "메일에 있는 수신 거부 링크를 다시 눌러 주세요.");
        }
        return page("아침 브리핑 수신 거부",
                "버튼을 누르면 더 이상 아침 브리핑 메일을 보내지 않습니다.</p>"
                        + "<form method=\"post\" action=\"unsubscribe\">"
                        + "<input type=\"hidden\" name=\"token\" value=\"" + escape(token) + "\">"
                        + "<button type=\"submit\" style=\"padding:10px 20px;font-size:15px;cursor:pointer;\">수신 거부</button>"
                        + "</form><p>");
    }

    public String unsubscribedPage() {
        return page("수신 거부되었습니다",
                "다시 받으려면 " + link(siteUrl, "neeews") + "의 내 정보에서 아침 브리핑을 신청하면 됩니다.");
    }

    private String html(BriefingContent content, String date, String unsubscribeUrl) {
        StringBuilder html = new StringBuilder()
                .append("<div style=\"").append(BODY_STYLE).append("\">")
                .append("<h1 style=\"font-size:22px;margin:24px 0 4px;\">neeews 아침 브리핑</h1>")
                .append("<p style=\"").append(META_STYLE).append("margin:0;\">").append(escape(date)).append("</p>");

        if (!content.events().isEmpty()) {
            section(html, "오늘의 주요 사건");
            content.events().forEach(event -> item(html, link(articleUrl(event.getId()), event.getSummary())));
        }
        if (!content.devArticles().isEmpty()) {
            section(html, "AI·개발");
            content.devArticles().forEach(article -> item(html, link(articleUrl(article.getId()), article.getTitle())
                    + " " + meta(article.getSource())));
        }
        if (!content.models().isEmpty()) {
            section(html, "새 AI 모델");
            content.models().forEach(model -> item(html, link(model.getUrl(), model.getId())
                    + "<br>" + meta(modelMeta(model))
                    + (model.getSummary() == null ? "" : "<br>" + escape(model.getSummary()))));
        }
        if (!content.stackUpdates().isEmpty()) {
            section(html, "개발 스택 소식");
            content.stackUpdates().forEach(update -> item(html, link(update.getUrl(), stackTitle(update))
                    + (isBlank(update.getAffected()) ? "" : "<br>" + meta(update.getAffected()))));
        }

        html.append("<p style=\"").append(META_STYLE).append("margin:36px 0 24px;\">")
                .append(link(siteUrl, "neeews에서 더 보기"));
        if (unsubscribeUrl != null) {
            html.append(" · 아침 브리핑을 신청하셔서 받으신 메일입니다. ").append(link(unsubscribeUrl, "수신 거부"));
        }
        return html.append("</p></div>").toString();
    }

    private String text(BriefingContent content, String date, String unsubscribeUrl) {
        StringBuilder text = new StringBuilder("neeews 아침 브리핑 · ").append(date).append("\n");
        if (!content.events().isEmpty()) {
            text.append("\n■ 오늘의 주요 사건\n");
            content.events().forEach(event -> text.append("- ").append(event.getSummary())
                    .append("\n  ").append(articleUrl(event.getId())).append("\n"));
        }
        if (!content.devArticles().isEmpty()) {
            text.append("\n■ AI·개발\n");
            content.devArticles().forEach(article -> text.append("- ").append(article.getTitle())
                    .append(" (").append(article.getSource()).append(")\n  ").append(articleUrl(article.getId())).append("\n"));
        }
        if (!content.models().isEmpty()) {
            text.append("\n■ 새 AI 모델\n");
            content.models().forEach(model -> {
                text.append("- ").append(model.getId()).append(" · ").append(modelMeta(model)).append("\n");
                if (model.getSummary() != null) text.append("  ").append(model.getSummary()).append("\n");
                text.append("  ").append(model.getUrl()).append("\n");
            });
        }
        if (!content.stackUpdates().isEmpty()) {
            text.append("\n■ 개발 스택 소식\n");
            content.stackUpdates().forEach(update -> {
                text.append("- ").append(stackTitle(update)).append("\n");
                if (!isBlank(update.getAffected())) text.append("  ").append(update.getAffected()).append("\n");
                text.append("  ").append(update.getUrl()).append("\n");
            });
        }
        text.append("\nneeews에서 더 보기: ").append(siteUrl).append("\n");
        if (unsubscribeUrl != null) text.append("수신 거부: ").append(unsubscribeUrl).append("\n");
        return text.toString();
    }

    private static String modelMeta(ModelResponse model) {
        List<String> parts = new ArrayList<>();
        parts.add(model.getAuthor());
        if (model.getPipelineTag() != null) parts.add(model.getPipelineTag());
        if (model.getParameterCount() != null) parts.add(formatParameters(model.getParameterCount()));
        String license = COMMERCIAL_USE_LABELS.get(model.getCommercialUse());
        parts.add(model.getLicense() == null ? license : license + "(" + model.getLicense() + ")");
        return String.join(" · ", parts);
    }

    private static String stackTitle(StackUpdateResponse update) {
        if (update.getType() == StackUpdateType.RELEASE) {
            return update.getProject() + " " + update.getTitle() + " 출시";
        }
        String severity = SEVERITY_LABELS.getOrDefault(update.getSeverity(), update.getSeverity());
        return "[보안·" + severity + "] " + update.getIdentifier() + " " + update.getTitle();
    }

    private static String formatParameters(long count) {
        if (count >= BILLION) return compact((double) count / BILLION) + "B";
        if (count >= MILLION) return compact((double) count / MILLION) + "M";
        return String.valueOf(count);
    }

    private static String compact(double value) {
        if (value >= ROUND_ABOVE) return String.valueOf(Math.round(value));
        return String.format(Locale.ROOT, "%.1f", value).replaceAll("\\.0$", "");
    }

    private String articleUrl(Long id) {
        return siteUrl + "/articles/" + id;
    }

    private static void section(StringBuilder html, String title) {
        html.append("<h2 style=\"").append(SECTION_STYLE).append("\">").append(escape(title)).append("</h2>");
    }

    private static void item(StringBuilder html, String body) {
        html.append("<p style=\"").append(ITEM_STYLE).append("\">").append(body).append("</p>");
    }

    private static String link(String url, String text) {
        return "<a href=\"" + escape(url) + "\" style=\"" + LINK_STYLE + "\">" + escape(text) + "</a>";
    }

    private static String meta(String text) {
        return "<span style=\"" + META_STYLE + "\">" + escape(text) + "</span>";
    }

    private static String page(String title, String bodyHtml) {
        return "<!DOCTYPE html><html lang=\"ko\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + escape(title) + "</title></head>"
                + "<body style=\"" + BODY_STYLE + "margin-top:80px;\">"
                + "<h1 style=\"font-size:20px;\">" + escape(title) + "</h1>"
                + "<p>" + bodyHtml + "</p></body></html>";
    }

    private static String escape(String text) {
        return text == null ? "" : HtmlUtils.htmlEscape(text);
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    public record BriefingMail(String subject, String html, String text, String unsubscribeUrl) {}
}
