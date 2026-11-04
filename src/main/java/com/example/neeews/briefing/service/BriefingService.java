package com.example.neeews.briefing.service;

import com.example.neeews.article.domain.PublishedTime;
import com.example.neeews.article.service.ArticleService;
import com.example.neeews.devstack.service.StackUpdateService;
import com.example.neeews.huggingface.service.ModelReleaseService;
import com.example.neeews.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BriefingService {

    // 아침에 훑어보는 메일이라 사건은 오늘의 뉴스 상위 몇 건만 싣는다. 나머지는 사이트에서 본다.
    private static final int EVENT_LIMIT = 7;
    private static final int DEV_ARTICLE_LIMIT = 5;
    private static final String DEV_CATEGORY = "AI/개발";
    // 매일 같은 시각에 보내므로 직전 발송 이후, 곧 하루치 소식을 담는다.
    private static final int LOOKBACK_HOURS = 24;

    private final ArticleService articleService;
    private final ModelReleaseService modelReleaseService;
    private final StackUpdateService stackUpdateService;
    private final UserService userService;
    private final BriefingRenderer briefingRenderer;
    private final BriefingMailer briefingMailer;

    public BriefingContent compose() {
        LocalDateTime since = PublishedTime.now().minusHours(LOOKBACK_HOURS);
        return new BriefingContent(
                PublishedTime.today(),
                articleService.getDailySummaries().stream().limit(EVENT_LIMIT).toList(),
                articleService.getTopArticlesSince(DEV_CATEGORY, since, DEV_ARTICLE_LIMIT),
                modelReleaseService.getNewlyVisibleSince(since),
                stackUpdateService.getUpdatesSince(since));
    }

    // 내용은 한 번만 만들고 수신 거부 링크만 사람마다 바꾼다. 한 명에게 실패해도(주소 오류 등) 나머지에게는 계속 보낸다.
    public int sendDailyBriefing() {
        List<UserService.BriefingRecipient> recipients = userService.getBriefingRecipients();
        if (recipients.isEmpty()) return 0;

        BriefingContent content = compose();
        int sent = 0;
        for (UserService.BriefingRecipient recipient : recipients) {
            try {
                briefingMailer.send(recipient.email(), briefingRenderer.render(content, recipient.unsubscribeToken()));
                sent++;
            } catch (Exception e) {
                log.warn("[브리핑] 발송 실패 to={}", recipient.email(), e);
            }
        }
        return sent;
    }

    public String preview() {
        return briefingRenderer.render(compose(), null).html();
    }

    public void sendTest(String email) {
        briefingMailer.send(email, briefingRenderer.render(compose(), null));
    }

    public String unsubscribeConfirmPage(String token) {
        return briefingRenderer.unsubscribeConfirmPage(token);
    }

    public String unsubscribe(String token) {
        if (token != null && !token.isBlank()) {
            userService.unsubscribeBriefing(token);
        }
        return briefingRenderer.unsubscribedPage();
    }
}
