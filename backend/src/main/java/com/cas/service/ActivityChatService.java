package com.cas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cas.entity.ActivityChat;

import java.util.List;

public interface ActivityChatService extends IService<ActivityChat> {
    ActivityChat send(Long activityId, Long userId, String content);
    List<ActivityChat> getMessages(Long activityId);
}
