package com.cas.controller;

import com.cas.common.Result;
import com.cas.dto.AiRecommendationDTO;
import com.cas.service.AiRecommendationService;
import com.cas.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 推荐控制器
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiRecommendationService aiRecommendationService;

    @Autowired
    private SecurityUtil securityUtil;

    /**
     * 获取当前登录学生的 AI 推荐活动
     */
    @GetMapping("/recommendations")
    public Result<List<AiRecommendationDTO>> getRecommendations() {
        securityUtil.requireStudent();
        Long userId = securityUtil.getCurrentUserId();
        return Result.success(aiRecommendationService.getRecommendations(userId));
    }
}
