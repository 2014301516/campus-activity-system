package com.cas.controller;

import com.cas.common.Result;
import com.cas.entity.Notification;
import com.cas.service.NotificationService;
import com.cas.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SecurityUtil securityUtil;

    /** 获取我的通知列表 */
    @GetMapping("/notifications")
    public Result<List<Notification>> getMyNotifications() {
        Long userId = securityUtil.getCurrentUserId();
        return Result.success(notificationService.getMyNotifications(userId));
    }

    /** 获取未读数量 */
    @GetMapping("/notifications/unread-count")
    public Result<Map<String, Object>> getUnreadCount() {
        Long userId = securityUtil.getCurrentUserId();
        Map<String, Object> result = new HashMap<>();
        result.put("count", notificationService.getUnreadCount(userId));
        return Result.success(result);
    }

    /** 全部标记已读 */
    @PutMapping("/notifications/read-all")
    public Result<?> markAllRead() {
        Long userId = securityUtil.getCurrentUserId();
        notificationService.markAllRead(userId);
        return Result.success("已全部标为已读");
    }

    @PutMapping("/notifications/{id}/read")
    public Result<?> markOneRead(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        notificationService.markOneRead(id, userId);
        return Result.success("已标为已读");
    }
}
