package com.cas.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cas.entity.ActivityChat;
import com.cas.entity.User;
import com.cas.mapper.ActivityChatMapper;
import com.cas.service.ActivityChatService;
import com.cas.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ActivityChatServiceImpl extends ServiceImpl<ActivityChatMapper, ActivityChat> implements ActivityChatService {

    @Autowired private UserService userService;

    @Override
    public ActivityChat send(Long activityId, Long userId, String content) {
        ActivityChat chat = new ActivityChat();
        chat.setActivityId(activityId);
        chat.setUserId(userId);
        chat.setContent(content);
        this.save(chat);
        User u = userService.getById(userId);
        chat.setUserName(u.getRealName());
        chat.setAvatar(u.getAvatar());
        return chat;
    }

    @Override
    public List<ActivityChat> getMessages(Long activityId) {
        List<ActivityChat> list = this.lambdaQuery()
                .eq(ActivityChat::getActivityId, activityId)
                .orderByAsc(ActivityChat::getCreatedAt)
                .list();
        if (!list.isEmpty()) {
            Map<Long, User> userMap = userService.listByIds(
                list.stream().map(ActivityChat::getUserId).collect(Collectors.toSet())
            ).stream().collect(Collectors.toMap(User::getId, u -> u));
            list.forEach(c -> {
                User u = userMap.get(c.getUserId());
                c.setUserName(u != null ? u.getRealName() : "未知");
                c.setAvatar(u != null ? u.getAvatar() : null);
            });
        }
        return list;
    }
}
