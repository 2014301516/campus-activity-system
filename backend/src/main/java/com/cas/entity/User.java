package com.cas.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String studentId;
    private String phone;
    private String email;
    private String role;        // student / organizer / admin
    private String avatar;
    private Integer status;     // 0禁用 / 1正常
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
