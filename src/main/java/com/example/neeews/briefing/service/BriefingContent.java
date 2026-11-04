package com.example.neeews.briefing.service;

import com.example.neeews.article.dto.response.ArticleResponse;
import com.example.neeews.article.dto.response.DailySummaryResponse;
import com.example.neeews.devstack.dto.response.StackUpdateResponse;
import com.example.neeews.huggingface.dto.response.ModelResponse;

import java.time.LocalDate;
import java.util.List;

public record BriefingContent(LocalDate date,
                              List<DailySummaryResponse> events,
                              List<ArticleResponse> devArticles,
                              List<ModelResponse> models,
                              List<StackUpdateResponse> stackUpdates) {
}
