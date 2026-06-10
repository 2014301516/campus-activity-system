package com.cas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cas.entity.Review;

import java.util.List;

/**
 * 评价服务接口
 */
public interface ReviewService extends IService<Review> {

    /**
     * 提交评价
     */
    Review submitReview(Long userId, Long activityId, Integer rating, String comment);

    /**
     * 获取活动评价列表
     */
    List<Review> getActivityReviews(Long activityId);
}
