package com.example.neeews.articleread.service;

import com.example.neeews.article.domain.Article;
import com.example.neeews.article.repository.ArticleRepository;
import com.example.neeews.articleread.domain.ArticleRead;
import com.example.neeews.articleread.repository.ArticleReadRepository;
import com.example.neeews.auth.domain.User;
import com.example.neeews.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ArticleReadService {

    private final ArticleReadRepository articleReadRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;

    @Transactional
    public void markAsRead(Long articleId, String email) {
        if (email == null) return;
        User user = getUser(email);
        if (articleReadRepository.existsByUserAndArticleId(user, articleId)) return;
        Article article = articleRepository.getReferenceById(articleId);
        articleReadRepository.save(ArticleRead.builder().user(user).article(article).build());
    }

    @Transactional(readOnly = true)
    public Set<Long> getReadArticleIds(String email, List<Long> articleIds) {
        if (email == null || articleIds.isEmpty()) return Collections.emptySet();
        User user = getUser(email);
        return Set.copyOf(articleReadRepository.findReadArticleIds(user, articleIds));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
