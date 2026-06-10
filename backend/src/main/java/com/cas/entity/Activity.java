package com.cas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动实体
 */
@Data
@TableName("activities")
public class Activity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String coverImage;
    private Integer categoryId;
    private Long organizerId;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private String status;   // draft / pending / approved / ongoing / ended / cancelled
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 组织者姓名（非数据库字段） */
    @TableField(exist = false)
    private String organizerName;

    /** 分类名称（非数据库字段） */
    @TableField(exist = false)
    private String categoryName;
}
