package com.cas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cas.entity.Notification;

import java.util.List;

public interface NotificationService extends IService<Notification> {

    /** 发送通知 */
    void send(Long userId, String title, String content, String type);

    /** 获取用户的通知列表（最新20条） */
    List<Notification> getMyNotifications(Long userId);

    /** 获取未读数量 */
    long getUnreadCount(Long userId);

    /** 标记全部已读 */
    void markAllRead(Long userId);

    /** 标记单条已读 */
    void markOneRead(Long notificationId, Long userId);

    /** 删除通知 */
    void deleteNotification(Long notificationId, Long userId);

    /** 搜索通知 */
    List<Notification> searchNotifications(Long userId, String keyword);
}
