package com.cas.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

@Data
public class AiAskDTO {
    @NotBlank(message = "问题不能为空")
    private String question;

    private Long activityId;  // 可选：用户当前查看的活动ID，提供上下文

    private List<Map<String, String>> messages;  // 对话历史 [{role:"user"|"ai", content:"..."}]

    private String page;  // 当前页面: home/manage/admin/my-activities/activity/:id/profile/notifications
}
