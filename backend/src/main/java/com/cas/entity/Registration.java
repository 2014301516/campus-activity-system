package com.cas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报名实体
 */
@Data
@TableName("registrations")
public class Registration {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long activityId;
    private String status;      // registered / cancelled
    private LocalDateTime registeredAt;

    /** 用户名（非数据库字段） */
    @TableField(exist = false)
    private String userName;

    /** 学号（非数据库字段） */
    @TableField(exist = false)
    private String studentId;

    /** 活动标题（非数据库字段） */
    @TableField(exist = false)
    private String activityTitle;
}
