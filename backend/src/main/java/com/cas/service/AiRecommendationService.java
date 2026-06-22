package com.cas.service;

import com.cas.dto.AiRecommendationDTO;

import java.util.List;

/**
 * AI 推荐服务
 */
public interface AiRecommendationService {

    /**
     * 获取当前学生的推荐活动
     */
    List<AiRecommendationDTO> getRecommendations(Long userId, boolean enableAi);
}
