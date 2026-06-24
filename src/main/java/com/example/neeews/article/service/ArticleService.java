package com.example.neeews.article.service;

import com.example.neeews.article.domain.Article;
import com.example.neeews.article.dto.ArticleDetailResponse;
import com.example.neeews.article.dto.ArticleResponse;
import com.example.neeews.article.repository.ArticleRepository;
import com.example.neeews.bookmark.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final BookmarkService bookmarkService;

    @Transactional(readOnly = true)
    public List<ArticleResponse> getBreakingArticles() {
        return articleRepository.findTop10ByOrderByPublishedAtDesc()
                .stream().map(ArticleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> getLatestArticles() {
        return articleRepository.findTop5ByOrderByPublishedAtDesc()
                .stream().map(ArticleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> getHotArticles() {
        return articleRepository.findTop6ByOrderByViewCountDesc()
                .stream().map(ArticleResponse::from).toList();
    }

    @Transactional
    public ArticleDetailResponse getArticleDetail(Long id, String email) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("기사를 찾을 수 없습니다."));
        article.incrementViewCount();
        List<ArticleResponse> related = getRelated(article);
        boolean isBookmarked = bookmarkService.isBookmarked(id, email);
        return ArticleDetailResponse.of(article, related, isBookmarked);
    }

    @Transactional(readOnly = true)
    public Page<ArticleResponse> getArticlesByCategory(String category, String sort, int page) {
        Sort s = "popular".equals(sort)
                ? Sort.by(Sort.Direction.DESC, "viewCount")
                : Sort.by(Sort.Direction.DESC, "publishedAt");
        Pageable pageable = PageRequest.of(page - 1, 10, s);
        return articleRepository.findByCategoryOptional(category, pageable).map(ArticleResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ArticleResponse> searchArticles(String q, Pageable pageable) {
        return articleRepository.searchByKeyword(q, pageable).map(ArticleResponse::from);
    }

    private List<ArticleResponse> getRelated(Article article) {
        List<Article> related = null;
        if (article.getCategory() != null) {
            related = articleRepository.findTop5ByCategoryAndIdNotOrderByPublishedAtDesc(
                    article.getCategory(), article.getId());
        }
        if (related == null || related.isEmpty()) {
            Pageable p = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "publishedAt"));
            related = articleRepository.findBySource(article.getSource(), p)
                    .stream().filter(a -> !a.getId().equals(article.getId())).limit(5).toList();
        }
        return related.stream().map(ArticleResponse::from).toList();
    }
}
