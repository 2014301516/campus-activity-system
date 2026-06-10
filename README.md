# 校园活动管理系统

Campus Activity Management System — 两课通用课程设计

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3 + Element Plus + Vue Router + Pinia + Axios |
| 后端 | Spring Boot 2.7 + MyBatis-Plus + Spring Security + JWT |
| 数据库 | MySQL 8.0 |
| 构建 | Maven + Vite |

## 项目结构

```
campus-activity-system/
├── backend/                         # Spring Boot 后端
│   ├── pom.xml                      # Maven 依赖配置
│   └── src/main/java/com/cas/
│       ├── CampusActivityApplication.java  # 启动类
│       ├── config/       # Security, JWT, CORS, Jackson, MyBatis-Plus 配置
│       ├── controller/   # 9 个 Controller, 28 个 API
│       ├── service/      # 服务层接口 + impl 实现
│       ├── mapper/       # MyBatis-Plus Mapper
│       ├── entity/       # 7 个实体类
│       ├── dto/          # 请求 DTO
│       ├── common/       # 统一返回 Result, 全局异常处理
│       └── util/         # JWT 工具, 安全工具
├── frontend/                       # Vue 3 前端
│   └── src/
│       ├── views/        # 7 个页面组件
│       ├── router/       # 路由配置 + 登录守卫
│       ├── api/          # Axios 封装 + API 函数
│       ├── store/        # Pinia 登录状态管理
│       └── App.vue       # 主布局 + 导航栏
└── docs/
    ├── schema.sql        # 建表 DDL（7 张表）
    ├── data.sql          # 测试数据
    ├── 网络实训报告.md
    └── Java框架报告.md
```

## 环境要求

| 工具 | 版本 |
|------|------|
| JDK | 17+ |
| Maven | 3.6+（或直接用 IntelliJ IDEA 内置） |
| Node.js | 18+ |
| MySQL | 8.0 |

## 快速启动

### 1. 数据库

打开 MySQL，依次执行：

```sql
source E:/keshe/campus-activity-system/docs/schema.sql;
source E:/keshe/campus-activity-system/docs/data.sql;
```

或者用命令行：
```bash
mysql -u root -p123 < docs/schema.sql
mysql -u root -p123 < docs/data.sql
```

> 数据库连接信息：localhost:3306，root / 123，库名 campus_activity

### 2. 后端

1. 打开 IntelliJ IDEA
2. File → Open → 选择 `backend/pom.xml` → Open as Project
3. 等待 Maven 下载依赖（首次较慢）
4. 展开 `src/main/java/com/cas` → 右键 `CampusActivityApplication` → Run

控制台显示：
```
========================================
  校园活动管理系统后端启动成功！
  http://localhost:8080
========================================
```

### 3. 前端

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 http://localhost:5173

> Vite 已配置代理：`/api` 请求自动转发到 `localhost:8080`

### 4. 登录

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | 123456 | 管理员 |
| organizer01 | 123456 | 活动组织者 |
| student01 | 123456 | 学生 |

## 角色权限

| 功能 | 学生 | 组织者 | 管理员 |
|------|:--:|:----:|:----:|
| 浏览活动列表 | ✅ | ✅ | ✅ |
| 报名/取消报名 | ✅ | ✅ | — |
| 签到/签退 | ✅ | ✅ | — |
| 评价活动 | ✅ | ✅ | — |
| 发布活动 | — | ✅ | — |
| 管理自己的活动 | — | ✅ | — |
| 查看报名/签到名单 | — | ✅ | — |
| 活动审核 | — | — | ✅ |
| 用户管理 | — | — | ✅ |
| 分类管理 | — | — | ✅ |
| 公告管理 | — | — | ✅ |
| 数据统计 | — | — | ✅ |

## 活动状态流转

```
组织者发布 → pending（待审核）
           → 管理员审核通过 → approved（已通过，首页可见）
           → 管理员驳回 → pending（回到待审核）
```

## API 概览

| 模块 | 接口数 | 说明 |
|------|:-----:|------|
| 用户 | 6 | 注册、登录、信息修改、用户管理 |
| 活动 | 8 | CRUD、分页筛选、审核 |
| 报名 | 4 | 报名、取消、我的报名、报名名单 |
| 签到 | 4 | 签到、签退、签到状态、签到记录 |
| 评价 | 2 | 提交评价、评价列表 |
| 分类 | 3 | 列表、新增、删除 |
| 公告 | 3 | 列表、发布、删除 |
| 统计 | 1 | 仪表盘数据 |

所有接口返回统一格式 `{ code: 200, message: "...", data: ... }`

## Git 提交历史

共 16 次 commit，完整展示迭代开发过程：

```
init → 数据库 → 用户模块 → 活动模块 → 报名签到评价 → 管理员功能
→ 前端搭建 → 首页列表详情 → 我的活动管理后台 → 仪表盘完善
→ 文档报告 → 数据修复 → 布局重构 → 日期修复 → 数据一致性修复
```

## 两个报告的对应关系

- **网络综合开发实训**：用 `docs/网络实训报告.md`（含功能模块表、页面设计表、数据表设计表、接口设计表、AI使用记录表、第三方模块信息表）
- **Java框架技术**：用 `docs/Java框架报告.md`（含摘要、关键词、技术介绍、系统设计、参考文献）

两个报告共享核心数据（表结构、API、页面设计），按各自模板格式编写。
