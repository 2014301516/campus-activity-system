package com.cas.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cas.entity.Notification;
import com.cas.mapper.NotificationMapper;
import com.cas.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    @Override
    public void send(Long userId, String title, String content, String type) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setType(type);
        n.setIsRead(0);
        this.save(n);
    }

    @Override
    public List<Notification> getMyNotifications(Long userId) {
        return this.lambdaQuery()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt)
                .last("LIMIT 20")
                .list();
    }

    @Override
    public long getUnreadCount(Long userId) {
        return this.lambdaQuery()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .count();
    }

    @Override
    public void markAllRead(Long userId) {
        this.lambdaUpdate()
                .set(Notification::getIsRead, 1)
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .update();
    }

    @Override
    public void markOneRead(Long notificationId, Long userId) {
        this.lambdaUpdate()
                .set(Notification::getIsRead, 1)
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId)
                .update();
    }

    @Override
    public void deleteNotification(Long notificationId, Long userId) {
        this.lambdaUpdate()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId)
                .remove();
    }

    @Override
    public List<Notification> searchNotifications(Long userId, String keyword) {
        return this.lambdaQuery()
                .eq(Notification::getUserId, userId)
                .and(w -> w.like(Notification::getTitle, keyword).or().like(Notification::getContent, keyword))
                .orderByDesc(Notification::getCreatedAt)
                .list();
    }
}
