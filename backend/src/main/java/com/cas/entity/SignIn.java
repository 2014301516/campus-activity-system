package com.cas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 签到实体
 */
@Data
@TableName("sign_ins")
public class SignIn {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long registrationId;
    private Long userId;
    private Long activityId;
    private LocalDateTime signInTime;
    private LocalDateTime signOutTime;

    /** 用户姓名（非数据库字段） */
    @TableField(exist = false)
    private String userName;
}
