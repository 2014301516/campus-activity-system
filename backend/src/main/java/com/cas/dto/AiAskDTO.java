package com.cas.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AiAskDTO {
    @NotBlank(message = "问题不能为空")
    private String question;

    private Long activityId;  // 可选：用户当前查看的活动ID，提供上下文
}
