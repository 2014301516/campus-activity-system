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
        Map<String, Object> result = aiChatService.ask(userId, dto.getQuestion(), dto.getActivityId(), dto.getMessages());
        return Result.success(result);
    }
}
