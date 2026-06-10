-- ============================================
-- 校园活动管理系统 - 数据库建表脚本
-- Database: campus_activity
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS campus_activity
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE campus_activity;

-- ============================================
-- 1. 用户表 (users)
-- ============================================
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名（学号）',
    password    VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name   VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    student_id  VARCHAR(30)  NOT NULL COMMENT '学号',
    phone       VARCHAR(20)  NOT NULL COMMENT '手机号',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    role        VARCHAR(20)  NOT NULL DEFAULT 'student' COMMENT '角色: student/organizer/admin',
    avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0禁用/1正常',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 2. 分类表 (categories)
-- ============================================
DROP TABLE IF EXISTS categories;
CREATE TABLE categories (
    id          INT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name        VARCHAR(50)  NOT NULL COMMENT '分类名称',
    description VARCHAR(200) DEFAULT NULL COMMENT '分类描述',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动分类表';

-- ============================================
-- 3. 活动表 (activities)
-- ============================================
DROP TABLE IF EXISTS activities;
CREATE TABLE activities (
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    title                VARCHAR(100) NOT NULL COMMENT '活动标题',
    description          TEXT         NOT NULL COMMENT '活动描述',
    cover_image          VARCHAR(255) DEFAULT NULL COMMENT '封面图URL',
    category_id          INT          NOT NULL COMMENT '分类ID',
    organizer_id         BIGINT       NOT NULL COMMENT '组织者用户ID',
    location             VARCHAR(200) NOT NULL COMMENT '活动地点',
    start_time           DATETIME     NOT NULL COMMENT '开始时间',
    end_time             DATETIME     NOT NULL COMMENT '结束时间',
    max_participants     INT          NOT NULL DEFAULT 0 COMMENT '最大报名人数',
    current_participants INT          NOT NULL DEFAULT 0 COMMENT '当前报名人数',
    status               VARCHAR(20)  NOT NULL DEFAULT 'draft' COMMENT '状态: draft/pending/approved/ongoing/ended/cancelled',
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_category_id (category_id),
    KEY idx_organizer_id (organizer_id),
    KEY idx_status (status),
    KEY idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';

-- ============================================
-- 4. 报名表 (registrations)
-- ============================================
DROP TABLE IF EXISTS registrations;
CREATE TABLE registrations (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id       BIGINT      NOT NULL COMMENT '用户ID',
    activity_id   BIGINT      NOT NULL COMMENT '活动ID',
    status        VARCHAR(20) NOT NULL DEFAULT 'registered' COMMENT '状态: registered/cancelled',
    registered_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_activity (user_id, activity_id),
    KEY idx_activity_id (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报名表';

-- ============================================
-- 5. 签到表 (sign_ins)
-- ============================================
DROP TABLE IF EXISTS sign_ins;
CREATE TABLE sign_ins (
    id              BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    registration_id BIGINT   NOT NULL COMMENT '报名记录ID',
    user_id         BIGINT   NOT NULL COMMENT '用户ID',
    activity_id     BIGINT   NOT NULL COMMENT '活动ID',
    sign_in_time    DATETIME DEFAULT NULL COMMENT '签到时间',
    sign_out_time   DATETIME DEFAULT NULL COMMENT '签退时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_registration (registration_id),
    KEY idx_activity_id (activity_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到表';

-- ============================================
-- 6. 评价表 (reviews)
-- ============================================
DROP TABLE IF EXISTS reviews;
CREATE TABLE reviews (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    activity_id BIGINT   NOT NULL COMMENT '活动ID',
    rating      TINYINT  NOT NULL COMMENT '评分: 1-5',
    comment     TEXT     DEFAULT NULL COMMENT '评论内容',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_activity (user_id, activity_id),
    KEY idx_activity_id (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- ============================================
-- 7. 公告表 (notices)
-- ============================================
DROP TABLE IF EXISTS notices;
CREATE TABLE notices (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    title        VARCHAR(200) NOT NULL COMMENT '公告标题',
    content      TEXT         NOT NULL COMMENT '公告内容',
    publisher_id BIGINT       NOT NULL COMMENT '发布者用户ID',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    PRIMARY KEY (id),
    KEY idx_publisher_id (publisher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';
