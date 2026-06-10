# 《Java框架技术》课程报告

## 校园活动管理系统的设计与实现

---

## 摘要

本报告设计并实现了一个基于 Spring Boot 框架的校园活动管理系统。系统采用 SSM（Spring Boot + MyBatis-Plus）作为核心技术栈，MySQL 作为数据库，实现了用户管理、活动管理、活动报名、签到考勤、活动评价、分类管理、公告管理和数据统计等功能模块。系统支持三种用户角色（学生、活动组织者、管理员），具备完整的增删改查操作，后端采用分层架构设计（Controller-Service-Mapper），通过 Spring Security 与 JWT 实现无状态认证，使用 MyBatis-Plus 实现高效的数据持久化操作。系统通过 Postman 进行了接口测试，功能完备，运行正常。

**关键词**：Spring Boot；MyBatis-Plus；校园活动管理；JWT认证；分层架构

---

## 目录

1. 系统概述
2. 技术介绍
3. 需求分析
4. 系统设计
5. 系统实现
6. 总结
7. 参考文献

---

## 一、系统概述

### 1.1 项目背景

随着高校校园文化活动的日益丰富，传统的人工管理方式效率低下、信息不对称等问题日益突出。为解决这一问题，开发一套校园活动管理系统，实现活动信息发布、在线报名、签到管理和数据统计的信息化，具有重要的现实意义。

### 1.2 项目目标

本项目旨在构建一个功能完备、操作便捷的校园活动管理系统，实现以下目标：
- 为学生提供便捷的活动浏览、报名、签到和评价功能
- 为活动组织者提供活动发布、报名管理和签到统计功能
- 为管理员提供全面的用户管理、活动审核和系统管理功能

### 1.3 系统功能概述

系统包含七大功能模块：用户模块、活动模块、报名模块、签到模块、评价模块、公告模块和数据统计模块。支持学生、组织者、管理员三种角色的权限控制。

---

## 二、技术介绍

### 2.1 Spring Boot 框架

Spring Boot 是基于 Spring 框架的快速开发脚手架，采用"约定优于配置"的理念，通过自动配置和起步依赖（Starter）简化了 Spring 应用的搭建和开发过程。本系统使用 Spring Boot 2.7.18 作为基础框架，利用其内嵌 Tomcat 容器实现独立部署，通过 `spring-boot-starter-web` 提供 RESTful API 开发能力。

### 2.2 MyBatis-Plus 框架

MyBatis-Plus 是 MyBatis 的增强工具包，在 MyBatis 的基础上提供了大量便捷功能：
- **通用 CRUD**：通过继承 `BaseMapper<T>` 自动获得增删改查方法，无需编写 XML 映射
- **条件构造器**：`LambdaQueryWrapper` 提供类型安全的动态 SQL 构建，避免字段名硬编码
- **分页插件**：内置物理分页支持，自动生成分页查询语句
- **主键策略**：支持自增、雪花算法等多种主键生成策略

### 2.3 Spring Security + JWT

Spring Security 提供了完善的认证和授权机制。本系统结合 JSON Web Token（JWT）实现无状态认证：
- **认证流程**：用户登录成功返回 JWT Token，后续请求在 Header 中携带 Token
- **过滤器机制**：自定义 `JwtAuthFilter` 拦截请求解析 Token，将认证信息注入 `SecurityContext`
- **密码加密**：使用 BCryptPasswordEncoder 对用户密码进行哈希加密存储
- **权限控制**：通过 `@EnableWebSecurity` + 路径规则实现基于角色的访问控制

### 2.4 MySQL 数据库

MySQL 是最流行的开源关系型数据库，本系统使用 MySQL 8.0 作为数据存储方案。通过 MyBatis-Plus 的 `map-underscore-to-camel-case` 配置实现数据库下划线命名与 Java 驼峰命名的自动映射。

### 2.5 其他依赖技术

- **Lombok**：通过注解简化实体类的 getter/setter/构造器代码
- **Hutool**：提供日期处理、类型转换、Bean 拷贝等常用工具方法
- **Spring Validation**：实现 DTO 层的数据校验（@NotBlank、@Pattern 等注解）

---

## 三、需求分析

### 3.1 功能需求

**用户模块**：
- 用户注册（用户名、学号、姓名、手机号、密码，含表单校验）
- 用户登录（返回 JWT Token，含身份信息）
- 个人信息修改
- 管理员：用户列表分页查询、搜索、禁用/启用

**活动模块**：
- 活动列表（分页、按分类筛选、按关键词搜索、按最新/热门排序）
- 活动详情查看
- 组织者：创建活动（草稿）、编辑活动、删除活动
- 管理员：审核活动（通过/驳回）
- 活动状态流转：草稿 → 待审核 → 已通过 → 进行中 → 已结束

**报名模块**：
- 学生报名活动（校验人数上限、校验重复报名）
- 取消报名
- 查看报名名单

**签到模块**：
- 签到（校验报名状态、校验时间窗口）
- 签退
- 查看签到记录

**评价模块**：
- 对已报名活动进行 1-5 星评分和文字评价
- 查看活动评价列表

**公告模块**：
- 管理员发布公告
- 首页公告展示

**数据统计模块**：
- 仪表盘概览（活动总数、进行中活动数、用户总数）
- 分类统计、状态分布

### 3.2 角色需求

| 角色 | 权限描述 |
|------|----------|
| 学生（student） | 浏览活动、报名/取消报名、签到/签退、评价活动 |
| 组织者（organizer） | 发布活动、编辑/删除自己的活动、查看报名名单和签到记录 |
| 管理员（admin） | 用户管理、活动审核、分类管理、公告管理、数据统计 |

---

## 四、系统设计

### 4.1 系统架构

本系统采用前后端分离的 B/S 架构：

```
浏览器（Vue 3 + Element Plus）
        ↕ HTTP RESTful API
Spring Boot 应用（Controller → Service → Mapper → MyBatis-Plus）
        ↕ JDBC
MySQL 8.0 数据库
```

后端采用经典三层架构：
- **Controller 层**：接收 HTTP 请求，参数校验，调用 Service，返回 Result 封装
- **Service 层**：业务逻辑处理，事务管理，数据组装
- **Mapper 层**：数据持久化，MyBatis-Plus BaseMapper 提供基础 CRUD

### 4.2 数据库设计

系统共设计 7 张数据表：

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| users | 用户表 | id, username, password, real_name, student_id, phone, role, status |
| categories | 分类表 | id, name, description |
| activities | 活动表 | id, title, description, category_id, organizer_id, location, start_time, end_time, max_participants, status |
| registrations | 报名表 | id, user_id, activity_id, status, registered_at |
| sign_ins | 签到表 | id, registration_id, user_id, activity_id, sign_in_time, sign_out_time |
| reviews | 评价表 | id, user_id, activity_id, rating, comment |
| notices | 公告表 | id, title, content, publisher_id |

表关系：
- users 1:N activities（一个用户可发布多个活动）
- categories 1:N activities（一个分类下有多个活动）
- users : activities = M:N（通过 registrations 关联）
- registrations 1:1 sign_ins（一条报名记录对应一条签到记录）

### 4.3 API 接口设计

系统共设计 28 个 RESTful API 接口，遵循以下规范：
- 资源路径使用名词复数形式（如 `/api/activity/list`）
- HTTP 方法表达操作语义（GET 查询、POST 创建、PUT 更新、DELETE 删除）
- 统一返回格式 `{ code: 200, message: "success", data: ... }`
- 认证使用 Header: `Authorization: Bearer <token>`

完整接口清单参见实训报告中的"接口设计表"。

### 4.4 安全设计

- **认证**：基于 JWT 的无状态认证，Token 有效期 24 小时
- **授权**：基于角色（ROLE_STUDENT、ROLE_ORGANIZER、ROLE_ADMIN）的路径级权限控制
- **密码安全**：BCrypt 加密存储，不可逆
- **CORS**：配置跨域过滤器解决前后端分离跨域问题

---

## 五、系统实现

### 5.1 开发环境

| 项目 | 版本/工具 |
|------|-----------|
| JDK | OpenJDK 17.0.19 |
| 框架 | Spring Boot 2.7.18 |
| ORM | MyBatis-Plus 3.5.5 |
| 数据库 | MySQL 8.0 |
| 构建工具 | Maven |
| IDE | IntelliJ IDEA |
| 版本控制 | Git / GitHub |
| 测试工具 | Postman |

### 5.2 项目结构

```
backend/
└── src/main/java/com/cas/
    ├── CampusActivityApplication.java       # 启动类
    ├── config/
    │   ├── SecurityConfig.java              # Spring Security 配置
    │   ├── JwtAuthFilter.java               # JWT 认证过滤器
    │   ├── CorsConfig.java                  # 跨域配置
    │   └── MyBatisPlusConfig.java           # MyBatis-Plus 分页插件配置
    ├── controller/
    │   ├── UserController.java              # 用户接口
    │   ├── ActivityController.java          # 活动接口
    │   ├── CategoryController.java          # 分类接口
    │   ├── RegistrationController.java      # 报名接口
    │   ├── SignInController.java            # 签到接口
    │   ├── ReviewController.java            # 评价接口
    │   ├── NoticeController.java            # 公告接口
    │   ├── AdminController.java             # 管理员接口
    │   └── DashboardController.java         # 统计接口
    ├── service/                             # 服务接口 + impl/ 实现
    ├── mapper/                              # MyBatis Mapper 接口
    ├── entity/                              # 实体类（7个）
    ├── dto/                                 # 数据传输对象
    ├── common/
    │   ├── Result.java                      # 统一返回结果
    │   └── GlobalExceptionHandler.java      # 全局异常处理
    └── util/
        ├── JwtUtil.java                     # JWT 工具类
        └── SecurityUtil.java                # 认证上下文工具
```

### 5.3 核心实现说明

#### 5.3.1 统一返回结果封装

设计 `Result<T>` 泛型类，封装 code、message、data 三个字段，提供 `success()` 和 `error()` 静态工厂方法。Controller 层统一返回 `Result` 对象，前端通过拦截器统一处理。

```java
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }
}
```

#### 5.3.2 JWT 认证流程

1. 用户登录 → UserServiceImpl 校验用户名/密码 → 调用 JwtUtil.generateToken()
2. JwtUtil 基于 HS256 算法，将 userId、username、role 作为 Claims 生成 Token
3. 后续请求 → JwtAuthFilter 的 doFilterInternal() 提取 Token → 解析 Claims → 设置 SecurityContext
4. SecurityConfig 中配置路径权限规则，基于角色控制访问

#### 5.3.3 MyBatis-Plus 条件查询

使用 `LambdaQueryWrapper` 实现类型安全的动态查询，避免字段名字符串硬编码：

```java
LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(Activity::getCategoryId, query.getCategoryId())
       .like(Activity::getTitle, query.getKeyword())
       .orderByDesc(Activity::getCreatedAt);
Page<Activity> result = this.page(pageParam, wrapper);
```

#### 5.3.4 分层架构示例（活动模块）

```
ActivityController（接收请求，参数校验）
  → ActivityService（接口定义业务方法）
    → ActivityServiceImpl（实现业务逻辑，事务管理）
      → ActivityMapper（继承 BaseMapper<Activity>，提供基础 CRUD）
```

服务层实现通过 `@Service` 注解注入 Spring 容器，通过 `@Autowired` 注入依赖的 Mapper 和其他 Service。

#### 5.3.5 全局异常处理

使用 `@RestControllerAdvice` 注解的 `GlobalExceptionHandler` 统一处理异常，将业务异常（RuntimeException）和系统异常的友好信息返回给前端。

### 5.4 系统测试

使用 Postman 对 28 个 API 接口逐一进行了功能测试：
- **用户模块**：验证注册（含重复用户名拦截）、登录（返回 Token）、Token 认证
- **活动模块**：验证 CRUD 操作、分页查询、分类筛选、关键词搜索
- **报名模块**：验证报名、取消报名、人数上限校验、重复报名检测
- **签到模块**：验证签到、签退、时间窗口校验
- **评价模块**：验证评价提交、重复评价检测、评价列表
- **权限控制**：验证学生/组织者/管理员的权限隔离

测试结果：所有接口功能正常，异常情况处理正确。

---

## 六、总结

### 6.1 项目成果

本系统成功实现了校园活动管理系统的完整后端开发，包括：
- 9 个 Controller，28 个 RESTful API 接口
- 7 张数据库表，7 个实体类和对应的 Mapper
- 完整的 JWT 认证与角色权限控制
- 分层架构（Controller-Service-Mapper），代码结构清晰
- 统一返回格式和全局异常处理

### 6.2 技术收获

通过本次课程设计，我深入掌握了：
1. Spring Boot 的自动配置机制和项目结构规范
2. MyBatis-Plus 的通用 CRUD、条件构造器和分页插件
3. Spring Security + JWT 的无状态认证方案
4. RESTful API 设计原则和统一返回格式
5. 分层架构中各层职责的划分和依赖关系

### 6.3 不足与展望

- 当前系统的活动状态（ongoing/ended）需要手动触发，未来可加入定时任务自动更新
- 并发报名场景下的超员问题，可使用 Redis 分布式锁进一步优化
- 可扩展文件上传功能（活动封面图）和 Excel 导出功能（报名名单）
- 可引入 Swagger/Knife4j 实现 API 文档自动生成

---

## 七、参考文献

[1] Craig Walls. Spring Boot in Action[M]. Manning Publications, 2016.

[2] 杨开振等. Spring Boot 2 企业级应用开发实战[M]. 人民邮电出版社, 2019.

[3] MyBatis-Plus 官方文档[EB/OL]. https://baomidou.com, 2024.

[4] Spring Security 官方文档[EB/OL]. https://spring.io/projects/spring-security, 2024.

[5] JJWT GitHub 仓库[EB/OL]. https://github.com/jwtk/jjwt, 2024.

[6] 朱运乔. Spring Boot + Vue 全栈开发实战[M]. 清华大学出版社, 2020.
