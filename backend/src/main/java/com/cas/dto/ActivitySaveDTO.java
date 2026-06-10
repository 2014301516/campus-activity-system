package com.cas.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 活动创建/编辑 DTO
 */
@Data
public class ActivitySaveDTO {

    private Long id;

    @NotBlank(message = "活动标题不能为空")
    private String title;

    @NotBlank(message = "活动描述不能为空")
    private String description;

    private String coverImage;

    @NotNull(message = "请选择活动分类")
    private Integer categoryId;

    @NotBlank(message = "活动地点不能为空")
    private String location;

    @NotNull(message = "请设置开始时间")
    private LocalDateTime startTime;

    @NotNull(message = "请设置结束时间")
    private LocalDateTime endTime;

    @NotNull(message = "请设置最大报名人数")
    private Integer maxParticipants;
}
