# 校园活动管理系统 — 给 AI 的交接说明

## 项目背景

选题「校园活动管理系统」，同时用于两门课：
- **网络综合开发实训**：要求 Vue/React 前端 + 不限后端 + 5-8 页 + 4 张表 + 多角色 + 6 张报告表格
- **Java 框架技术**：要求 Spring Boot + MyBatis + MySQL + Git 迭代提交 + 报告含摘要关键词参考文献

## 环境

- **项目路径**：`E:\keshe\campus-activity-system\`
- **GitHub**：https://github.com/2014301516/campus-activity-system
- **JDK**：17（OpenJDK）
- **Node**：v22.14.0
- **MySQL**：8.0.42，localhost:3306，root / 123，库名 `campus_activity`
- **Maven**：命令行没有，通过 IntelliJ IDEA 内置 Maven 运行
- **Git**：已配置，`gh` CLI 已登录，有 repo/write 权限

## 技术栈

前端：Vue 3 + Element Plus + Vue Router 4 + Pinia + Axios + Vite  
后端：Spring Boot 2.7.18 + MyBatis-Plus 3.5.5 + Spring Security + JWT (jjwt 0.11.5)  
数据库：MySQL 8.0，7 张表，utf8mb4

## 运行方式

```bash
# 数据库（已执行过，一般不需要重新跑）
mysql -u root -p123 < docs/schema.sql
mysql -u root -p123 < docs/data.sql

# 后端：IntelliJ IDEA → Open → backend/pom.xml → Open as Project
#      → 右键 CampusActivityApplication → Run → 监听 localhost:8080

# 前端
cd frontend
npm install   # 已装过，新建 clone 后需要
npm run dev   # → localhost:5173，/api 自动代理到 8080
```

## 测试账号（密码均为 123456）

| 用户名 | 角色 | 用途 |
|--------|------|------|
| admin | 管理员 | 后台管理、活动审核 |
| organizer01 | 组织者 | 发布活动 |
| student01 | 学生 | 报名、签到、评价 |

## 项目结构速览

```
backend/src/main/java/com/cas/
├── CampusActivityApplication.java
├── config/        # SecurityConfig, JwtAuthFilter, CorsConfig, JacksonConfig, MyBatisPlusConfig
├── controller/    # User, Activity, Category, Registration, SignIn, Review, Notice, Admin, Dashboard
├── service/       # 5 个接口 + impl
├── mapper/        # 6 个 MyBatis-Plus BaseMapper
├── entity/        # User, Activity, Category, Registration, SignIn, Review, Notice
├── dto/           # LoginDTO, RegisterDTO, ActivitySaveDTO, ActivityQueryDTO
├── common/        # Result<T>（统一返回）, GlobalExceptionHandler
└── util/          # JwtUtil, SecurityUtil

frontend/src/
├── App.vue        # 主布局 + 导航栏（按角色显示不同菜单）
├── views/         # Login, Home, ActivityDetail, MyActivities, ActivityManage, Admin, Profile
├── router/index.js  # 路由 + beforeEach 登录守卫
├── api/request.js   # Axios 实例 + 拦截器（baseURL: /api，自动挂 Token）
├── api/index.js     # 所有 API 函数（userApi, activityApi, registrationApi 等）
├── store/auth.js    # Pinia（token, userInfo, login/logout）
└── main.js          # 入口，注册 Element Plus 图标
```

## 角色与导航

| 菜单 | 学生 | 组织者 | 管理员 |
|------|:--:|:----:|:----:|
| 首页 | ✅ | ✅ | ✅ |
| 我的活动 | ✅ | ✅ | — |
| 活动管理 | — | ✅ | — |
| 后台管理 | — | — | ✅ |

## 活动状态流转

```
组织者创建活动 → pending（待审核）
              → 管理员审核通过 → approved（首页可见）
              → 管理员驳回 → 回到 pending
```

首页只显示 approved 和 ongoing 状态的活动。

## 安全配置关键点

- JWT 有效期 24 小时（application.yml 中配置）
- 密码 BCrypt 加密
- 用户 ID 从 JWT 解析后存入 SecurityContext，通过 SecurityUtil.getCurrentUserId() 获取
- SecurityConfig 中放行的公开接口：`/api/user/register`、`/api/user/login`、`GET /api/activity/**`、`GET /api/category/**`、`GET /api/review/activity/**`、`GET /api/notice/list`
- POST/PUT/DELETE /api/activity 需要 organizer 或 admin 角色
- /api/admin/** 需要 admin 角色
- 其他接口需要登录 Token

## 数据库 7 张表

users, categories, activities, registrations, sign_ins, reviews, notices  
建表语句在 `docs/schema.sql`，测试数据在 `docs/data.sql`

## 报告文档

- `docs/网络实训报告.md` — 含 6 张表格（功能模块表、页面设计表、数据表设计表、接口设计表、AI使用记录表、第三方模块信息表）
- `docs/Java框架报告.md` — 含摘要（200-250 字）、关键词 3-5 个、技术介绍、系统设计、参考文献
- `README.md` — 项目总览

## 已踩过的坑（不要再犯）

1. **LocalDateTime 反序列化**：前端发 `yyyy-MM-dd HH:mm:ss`（空格），Jackson 默认只认 `yyyy-MM-ddTHH:mm:ss`（T）。已修复：pom.xml 补了 `jackson-datatype-jsr310`，DTO 字段加了 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")`，还有 JacksonConfig 全局配置。
2. **密码哈希**：data.sql 中的 bcrypt 哈希必须是真的，不能随便写。已用 Python bcrypt 生成。
3. **分类统计一致性**：Dashboard 中 categoryStats 和 totalActivities 统计口径必须一致。
4. **活动创建默认状态**：不要设 draft（草稿），直接设 pending（待审核），否则没有 UI 入口提审。

## 当前已完成

- 后端全部功能（28 个 API，9 个 Controller）
- 前端全部页面（7 个）
- 数据库建表 + 测试数据
- Git 16 次迭代提交
- GitHub 已推送
- 两课报告文档
- README
