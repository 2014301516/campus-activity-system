package com.cas.dto;

import lombok.Data;

/**
 * 活动查询条件 DTO
 */
@Data
public class ActivityQueryDTO {

    private Integer page = 1;
    private Integer size = 10;
    private String keyword;       // 标题关键词搜索
    private Integer categoryId;   // 分类筛选
    private String status;        // 状态筛选
    private Long organizerId;     // 组织者筛选（我的活动）
    private String sort = "newest"; // newest / popular / startTimeAsc
    private Boolean includeAll = false; // 管理员查看全部活动
}
