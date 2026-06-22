package com.cas.controller;

import com.cas.common.Result;
import com.cas.dto.AiAskDTO;
import com.cas.service.AiChatService;
import com.cas.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @Autowired private AiChatService aiChatService;
    @Autowired private SecurityUtil securityUtil;

    @PostMapping("/ask")
    public Result<Map<String, Object>> ask(@Valid @RequestBody AiAskDTO dto) {
        Long userId = securityUtil.getCurrentUserId();
        Map<String, Object> result = aiChatService.ask(userId, dto.getQuestion(), dto.getActivityId(), dto.getMessages(), dto.getPage());
        return Result.success(result);
    }

    /** AI 生成活动描述 */
    @PostMapping("/generate-description")
    public Result<Map<String, String>> generateDescription(@RequestBody Map<String, Object> body) {
        String title = body.get("title").toString();
        Long categoryId = Long.valueOf(body.get("categoryId").toString());
        String description = aiChatService.generateDescription(title, categoryId);
        Map<String, String> result = new java.util.HashMap<>();
        result.put("description", description);
        return Result.success(result);
    }

    /** AI 审核建议 */
    @GetMapping("/audit-suggestion")
    public Result<Map<String, String>> getAuditSuggestion(@RequestParam Long activityId) {
        String suggestion = aiChatService.getAuditSuggestion(activityId);
        Map<String, String> result = new java.util.HashMap<>();
        result.put("suggestion", suggestion);
        return Result.success(result);
    }
}
