package com.cas.controller;

import com.cas.common.Result;
import com.cas.entity.Review;
import com.cas.service.ReviewService;
import com.cas.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 评价控制器
 */
@RestController
@RequestMapping("/api")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private SecurityUtil securityUtil;

    /**
     * 提交评价
     */
    @PostMapping("/review")
    public Result<Review> submitReview(@RequestBody Map<String, Object> body) {
        Long userId = securityUtil.getCurrentUserId();
        Long activityId = Long.valueOf(body.get("activityId").toString());
        Integer rating = Integer.valueOf(body.get("rating").toString());
        String comment = body.get("comment") != null ? body.get("comment").toString() : "";
        Review review = reviewService.submitReview(userId, activityId, rating, comment);
        return Result.success("评价成功", review);
    }

    /**
     * 活动评价列表
     */
    @GetMapping("/review/activity/{activityId}")
    public Result<List<Review>> activityReviews(@PathVariable Long activityId) {
        return Result.success(reviewService.getActivityReviews(activityId));
    }
}
