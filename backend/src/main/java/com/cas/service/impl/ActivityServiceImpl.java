package com.cas.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cas.dto.ActivityQueryDTO;
import com.cas.dto.ActivitySaveDTO;
import com.cas.entity.Activity;
import com.cas.entity.Category;
import com.cas.entity.User;
import com.cas.mapper.ActivityMapper;
import com.cas.service.ActivityService;
import com.cas.service.CategoryService;
import com.cas.service.NotificationService;
import com.cas.service.UserService;
import com.cas.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 活动服务实现
 */
@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    private static final Duration STATUS_REFRESH_MIN_INTERVAL = Duration.ofSeconds(5);

    private LocalDateTime lastStatusRefreshAt;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private NotificationService notificationService;

    @Override
    public Page<Activity> getActivityPage(ActivityQueryDTO query) {
        refreshActivityStatuses();

        Page<Activity> pageParam = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        boolean includeAll = Boolean.TRUE.equals(query.getIncludeAll())
                && "admin".equals(securityUtil.getCurrentUserRole());

        // 关键词搜索
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(Activity::getTitle, query.getKeyword());
        }

        // 分类筛选
        if (query.getCategoryId() != null) {
            wrapper.eq(Activity::getCategoryId, query.getCategoryId());
        }

        // 状态筛选
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Activity::getStatus, query.getStatus());
        }

        // 指定组织者
        if (query.getOrganizerId() != null) {
            wrapper.eq(Activity::getOrganizerId, query.getOrganizerId());
        }

        // 默认只展示已通过和进行中的活动（首页）
        if (!includeAll && !StringUtils.hasText(query.getStatus()) && query.getOrganizerId() == null) {
            wrapper.in(Activity::getStatus, "approved", "ongoing");
        }

        // 排序
        if ("popular".equals(query.getSort())) {
            wrapper.orderByDesc(Activity::getCurrentParticipants);
        } else if ("startTimeAsc".equals(query.getSort())) {
            wrapper.orderByAsc(Activity::getStartTime);
        } else {
            wrapper.orderByDesc(Activity::getCreatedAt);
        }

        Page<Activity> result = this.page(pageParam, wrapper);

        // 填充关联信息
        fillRelatedInfo(result.getRecords());

        return result;
    }

    @Override
    public Activity getActivityDetail(Long id) {
        refreshActivityStatuses();

        Activity activity = this.getById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }
        fillRelatedInfo(java.util.Collections.singletonList(activity));
        return activity;
    }

    @Override
    public synchronized void refreshActivityStatuses() {
        LocalDateTime now = LocalDateTime.now();
        if (lastStatusRefreshAt != null
                && Duration.between(lastStatusRefreshAt, now).compareTo(STATUS_REFRESH_MIN_INTERVAL) < 0) {
            return;
        }

        this.lambdaUpdate()
                .set(Activity::getStatus, "ended")
                .in(Activity::getStatus, "approved", "ongoing")
                .le(Activity::getEndTime, now)
                .update();

        this.lambdaUpdate()
                .set(Activity::getStatus, "ongoing")
                .eq(Activity::getStatus, "approved")
                .le(Activity::getStartTime, now)
                .gt(Activity::getEndTime, now)
                .update();

        lastStatusRefreshAt = now;
    }

    @Override
    public Activity createActivity(ActivitySaveDTO dto, Long userId) {
        Activity activity = new Activity();
        BeanUtil.copyProperties(dto, activity);
        activity.setOrganizerId(userId);
        activity.setStatus("pending");
        activity.setCurrentParticipants(0);
        activity.setCancelRequestReason(null);
        activity.setRejectReason(null);

        this.save(activity);
        return activity;
    }

    @Override
    public Activity updateActivity(ActivitySaveDTO dto, Long userId) {
        Activity activity = this.getById(dto.getId());
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }
        if (!canManageActivity(activity, userId)) {
            throw new RuntimeException("无权修改此活动");
        }
        // 已结束的活动不能修改
        if ("ended".equals(activity.getStatus())) {
            throw new RuntimeException("已结束的活动不能修改");
        }

        String currentStatus = activity.getStatus();
        BeanUtil.copyProperties(dto, activity);
        // 被驳回的活动在修改后重新进入待审核，便于重新演示审核流程
        if ("rejected".equals(currentStatus)) {
            activity.setStatus("pending");
            activity.setRejectReason(null);
        }
        this.updateById(activity);
        return activity;
    }

    @Override
    public void deleteActivity(Long id, Long userId) {
        Activity activity = this.getById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }
        if (!canManageActivity(activity, userId)) {
            throw new RuntimeException("无权删除此活动");
        }
        this.removeById(id);
    }

    @Override
    public void requestCancelActivity(Long id, Long userId, String cancelRequestReason) {
        Activity activity = this.getById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }
        if (!canManageActivity(activity, userId)) {
            throw new RuntimeException("无权申请取消此活动");
        }
        if ("cancel_pending".equals(activity.getStatus())) {
            throw new RuntimeException("取消申请已提交，请勿重复操作");
        }
        if ("cancelled".equals(activity.getStatus())) {
            throw new RuntimeException("活动已取消，请勿重复操作");
        }
        if ("ended".equals(activity.getStatus())) {
            throw new RuntimeException("已结束的活动不能申请取消");
        }
        if (!"approved".equals(activity.getStatus()) && !"ongoing".equals(activity.getStatus())) {
            throw new RuntimeException("只有已通过或进行中的活动才可申请取消");
        }
        if (!StringUtils.hasText(cancelRequestReason)) {
            throw new RuntimeException("请填写取消申请理由");
        }
        activity.setStatus("cancel_pending");
        activity.setCancelRequestReason(cancelRequestReason.trim());
        activity.setRejectReason(null);
        this.updateById(activity);
    }

    @Override
    public void auditActivity(Long id, String status, String rejectReason) {
        if (!"approved".equals(status) && !"rejected".equals(status)) {
            throw new RuntimeException("审核状态无效");
        }
        if ("rejected".equals(status) && !StringUtils.hasText(rejectReason)) {
            throw new RuntimeException("请填写驳回理由");
        }
        Activity activity = this.getById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }
        if ("pending".equals(activity.getStatus())) {
            activity.setStatus(status);
            activity.setRejectReason("rejected".equals(status) ? rejectReason.trim() : null);
            this.updateById(activity);
            // 驳回时通知组织者
            if ("rejected".equals(status)) {
                notificationService.send(activity.getOrganizerId(),
                    "活动审核被驳回", "你发布的活动「" + activity.getTitle() + "」未通过审核。理由：" + rejectReason.trim(),
                    "audit_reject");
            }
            return;
        }
        if ("cancel_pending".equals(activity.getStatus())) {
            if ("approved".equals(status)) {
                activity.setStatus("cancelled");
                activity.setRejectReason(null);
            } else {
                activity.setStatus(resolveActiveStatus(activity));
                activity.setRejectReason(rejectReason.trim());
                // 驳回取消申请时通知组织者
                notificationService.send(activity.getOrganizerId(),
                    "取消申请被驳回", "你申请取消的活动「" + activity.getTitle() + "」未通过审核，活动已恢复正常状态。理由：" + rejectReason.trim(),
                    "cancel_reject");
            }
            this.updateById(activity);
            return;
        }
        throw new RuntimeException("只能审核待审核或取消待审核状态的活动");
    }

    private String resolveActiveStatus(Activity activity) {
        LocalDateTime now = LocalDateTime.now();
        if (activity.getEndTime() != null && !activity.getEndTime().isAfter(now)) {
            return "ended";
        }
        if (activity.getStartTime() != null && !activity.getStartTime().isAfter(now)) {
            return "ongoing";
        }
        return "approved";
    }

    @Override
    public void cancelActivity(Long id) {
        Activity activity = this.getById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }
        if ("ended".equals(activity.getStatus())) {
            throw new RuntimeException("已结束的活动不能取消");
        }
        if ("cancelled".equals(activity.getStatus())) {
            throw new RuntimeException("活动已取消，请勿重复操作");
        }
        activity.setStatus("cancelled");
        this.updateById(activity);
    }

    private boolean canManageActivity(Activity activity, Long userId) {
        String role = securityUtil.getCurrentUserRole();
        return "admin".equals(role) || activity.getOrganizerId().equals(userId);
    }

    /**
     * 填充组织者姓名和分类名称
     */
    private void fillRelatedInfo(java.util.List<Activity> activities) {
        if (activities.isEmpty()) return;

        // 批量查询用户
        java.util.Set<Long> userIds = activities.stream()
                .map(Activity::getOrganizerId).collect(Collectors.toSet());
        Map<Long, String> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getRealName));

        // 批量查询分类
        java.util.Set<Integer> categoryIds = activities.stream()
                .map(Activity::getCategoryId).collect(Collectors.toSet());
        Map<Integer, String> categoryMap = categoryService.listByIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        for (Activity activity : activities) {
            activity.setOrganizerName(userMap.getOrDefault(activity.getOrganizerId(), "未知"));
            activity.setCategoryName(categoryMap.getOrDefault(activity.getCategoryId(), "未知"));
        }
    }
}
