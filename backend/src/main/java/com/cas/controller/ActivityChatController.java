package com.cas.controller;

import com.cas.common.Result;
import com.cas.entity.ActivityChat;
import com.cas.service.ActivityChatService;
import com.cas.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ActivityChatController {

    @Autowired private ActivityChatService chatService;
    @Autowired private SecurityUtil securityUtil;

    /** 获取某活动的聊天记录 */
    @GetMapping("/activity/{activityId}/chats")
    public Result<List<ActivityChat>> getMessages(@PathVariable Long activityId) {
        return Result.success(chatService.getMessages(activityId));
    }

    /** 发送消息 */
    @PostMapping("/activity/{activityId}/chats")
    public Result<ActivityChat> send(@PathVariable Long activityId, @RequestBody Map<String, String> body) {
        Long userId = securityUtil.getCurrentUserId();
        ActivityChat chat = chatService.send(activityId, userId, body.get("content"));
        return Result.success("发送成功", chat);
    }
}
