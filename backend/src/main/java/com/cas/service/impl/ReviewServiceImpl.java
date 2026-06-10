package com.cas.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cas.entity.Registration;
import com.cas.entity.Review;
import com.cas.entity.User;
import com.cas.mapper.ReviewMapper;
import com.cas.service.RegistrationService;
import com.cas.service.ReviewService;
import com.cas.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评价服务实现
 */
@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserService userService;

    @Override
    public Review submitReview(Long userId, Long activityId, Integer rating, String comment) {
        // 检查是否报名
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getUserId, userId)
               .eq(Registration::getActivityId, activityId)
               .eq(Registration::getStatus, "registered");
        if (registrationService.count(wrapper) == 0) {
            throw new RuntimeException("您未报名该活动，无法评价");
        }

        // 检查是否已评价
        LambdaQueryWrapper<Review> rWrapper = new LambdaQueryWrapper<>();
        rWrapper.eq(Review::getUserId, userId)
                .eq(Review::getActivityId, activityId);
        if (this.count(rWrapper) > 0) {
            throw new RuntimeException("您已评价过该活动");
        }

        if (rating < 1 || rating > 5) {
            throw new RuntimeException("评分必须在1-5之间");
        }

        Review review = new Review();
        review.setUserId(userId);
        review.setActivityId(activityId);
        review.setRating(rating);
        review.setComment(comment);
        this.save(review);

        return review;
    }

    @Override
    public List<Review> getActivityReviews(Long activityId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getActivityId, activityId)
               .orderByDesc(Review::getCreatedAt);
        List<Review> list = this.list(wrapper);

        // 填充用户姓名
        if (!list.isEmpty()) {
            Map<Long, String> userMap = userService.listByIds(
                    list.stream().map(Review::getUserId).collect(Collectors.toSet())
            ).stream().collect(Collectors.toMap(User::getId, User::getRealName));
            for (Review r : list) {
                r.setUserName(userMap.getOrDefault(r.getUserId(), "未知"));
            }
        }

        return list;
    }
}
