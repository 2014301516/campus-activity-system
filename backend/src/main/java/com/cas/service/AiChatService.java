package com.cas.service;

import java.util.Map;

public interface AiChatService {
    Map<String, Object> ask(Long userId, String question, Long activityId);
}
