# 校园活动管理系统

Campus Activity Management System

一个面向课程设计与答辩演示的校园活动平台，覆盖学生报名签到、组织者发布活动、管理员审核与后台治理三类核心角色，形成较完整的活动管理闭环。

当前版本重点优化了以下演示体验：

- 首页内容更完整，适合直接演示推荐活动、公告、活动广场等模块。
- 活动状态支持按时间自动流转，减少手动切状态的演示成本。
- 学生端补齐报名、取消报名、签到、签退、评价等关键流程。
- 管理员后台支持数据概览、状态分布、活动审核、用户与分类管理。
- 演示数据更丰富，活动、报名、签到记录可以互相对上，避免页面“空壳感”。

## 项目简介

本项目用于模拟校园活动平台的实际业务流程，主要解决以下场景：

- 学生浏览活动、报名参加、到场签到、结束签退、提交评价。
- 组织者创建活动、完善活动信息、提交审核、查看自己发布的活动。
- 管理员审核活动、维护分类和公告、查看后台统计、管理平台用户。

项目适合作为以下用途：

- 网络综合开发实训课程设计
- Java 框架课程设计
- 前后端分离项目演示
- 校园业务系统原型展示

## 核心亮点

- 首页采用推荐区、公告区、即将开始、新上架活动、活动广场的组合布局。
- 活动支持封面图展示，首页和详情页均能直观看到活动视觉内容。
- 活动状态自动流转：`approved -> ongoing -> ended`。
- 学生“我的报名”页面支持真实签到状态展示，区分 `未签到 / 已签到 / 已签退`。
- 操作列仅保留真实可执行操作，避免出现“看起来像按钮但不能点”的假操作。
- 管理员后台新增活动状态分布饼图，更适合汇报平台整体状态。
- 后台增加了一些安全保护，降低误操作风险。

## 技术栈

| 层级     | 技术                                                                             |
| -------- | -------------------------------------------------------------------------------- |
| 前端     | Vue 3、Vite、Element Plus、Vue Router、Pinia、Axios、ECharts、Vue-ECharts        |
| 后端     | Spring Boot 2.7.18、MyBatis-Plus 3.5.5、Spring Security、JWT、Validation、Hutool |
| 数据库   | MySQL 8.0                                                                        |
| 构建工具 | Maven、npm                                                                       |

## 项目结构

```text
campus-activity-system/
├── backend/                                # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/cas/
│       │   ├── common/                     # 统一返回、全局异常处理
│       │   ├── config/                     # 安全、JWT、CORS、MyBatis 配置
│       │   ├── controller/                 # 接口层
│       │   ├── dto/                        # 请求参数对象
│       │   ├── entity/                     # 实体类
│       │   ├── mapper/                     # MyBatis-Plus Mapper
│       │   ├── service/                    # 业务层
│       │   ├── util/                       # 工具类
│       │   └── CampusActivityApplication.java
│       └── resources/
│           └── application.yml             # 数据源、端口、JWT 配置
├── frontend/                               # Vue 3 前端
│   ├── package.json
│   └── src/
│       ├── api/                            # 请求封装
│       ├── router/                         # 路由与权限守卫
│       ├── store/                          # 登录态管理
│       ├── views/                          # 页面组件
│       ├── App.vue
│       └── main.js
├── docs/
│   ├── schema.sql                          # 建表脚本
│   ├── data.sql                            # 初始化演示数据
│   ├── 网络实训报告.md
│   ├── Java框架报告.md
│   ├── 给AI的交接说明.md
│   └── 新项目启动模板-自习室预约.md
└── README.md
```

## 功能模块

### 学生端

- 浏览首页推荐活动、公告和活动广场
- 查看活动详情
- 报名与取消报名
- 在允许时间内进行签到、签退
- 在活动结束后提交评价
- 在“我的报名”中查看报名状态和签到状态

### 组织者端

- 创建活动
- 编辑和删除自己发布的活动
- 维护活动封面、地点、时间、人数等信息
- 提交活动审核
- 查看自己活动的报名情况

### 管理员端

- 审核活动
- 查看后台数据概览
- 查看活动状态分布
- 管理全部用户
- 管理活动分类
- 发布与删除公告
- 查看全量活动列表

## 角色权限矩阵

| 功能               | 学生 | 组织者 | 管理员 |
| ------------------ | :--: | :----: | :----: |
| 浏览活动列表与详情 |  ✅  |   ✅   |   ✅   |
| 报名与取消报名     |  ✅  |   -   |   -   |
| 签到与签退         |  ✅  |   -   |   -   |
| 提交评价           |  ✅  |   -   |   -   |
| 创建活动           |  -  |   ✅   |   -   |
| 管理自己的活动     |  -  |   ✅   |   -   |
| 审核活动           |  -  |   -   |   ✅   |
| 用户管理           |  -  |   -   |   ✅   |
| 分类管理           |  -  |   -   |   ✅   |
| 公告管理           |  -  |   -   |   ✅   |
| 后台统计与概览     |  -  |   -   |   ✅   |

说明：

- 学生专属接口只允许学生角色调用。
- 管理员主要负责平台治理，不直接作为活动发布者参与业务流程。

## 活动状态流转

当前项目主流程使用以下活动状态：

- `pending`：待审核
- `approved`：审核通过，可报名
- `rejected`：审核驳回
- `ongoing`：进行中
- `ended`：已结束
- `cancelled`：已取消

状态流转规则如下：

```text
组织者创建活动 -> pending
管理员审核通过 -> approved
管理员审核驳回 -> rejected
驳回后再次编辑提交 -> pending
到达活动开始时间 -> approved 自动变为 ongoing
到达活动结束时间 -> approved / ongoing 自动变为 ended
```

## 首页与演示效果

首页当前主要由以下模块组成：

- 首屏推荐区
- 最新公告
- 即将开始
- 新上架活动
- 活动广场

其中已经做过以下演示优化：

- `浏览全部活动` 会重置筛选条件并滚动到活动广场。
- `查看热门活动` 会切换热门排序并滚动到活动广场。
- 推荐活动和详情页都支持展示封面图。
- 活动数据、报名记录和签到状态尽量保持一致，便于答辩时演示真实业务链路。

## 环境要求

| 工具    | 建议版本                                |
| ------- | --------------------------------------- |
| JDK     | 17 及以上                               |
| Maven   | 3.6 及以上，或 IntelliJ IDEA 自带 Maven |
| Node.js | 18 及以上                               |
| npm     | 9 及以上                                |
| MySQL   | 8.0                                     |

## 快速启动

### 1. 初始化数据库

先创建数据库：

```sql
CREATE DATABASE campus_activity DEFAULT CHARACTER SET utf8mb4;
```

再执行脚本：

```sql
source E:/keshe/campus-activity-system/docs/schema.sql;
source E:/keshe/campus-activity-system/docs/data.sql;
```

也可以在项目根目录使用命令行导入：

```bash
mysql -u root -p123 campus_activity < docs/schema.sql
mysql -u root -p123 campus_activity < docs/data.sql
```

默认数据库配置位于 [application.yml](file:///e:/keshe/campus-activity-system/backend/src/main/resources/application.yml)：

- 地址：`localhost:3306`
- 数据库：`campus_activity`
- 用户名：`root`
- 密码：`123`

### 2. 启动后端

方式一：使用 IntelliJ IDEA

1. 打开 `backend/pom.xml`
2. 等待 Maven 依赖下载完成
3. 运行 `CampusActivityApplication`

方式二：使用命令行

```bash
cd backend
mvn spring-boot:run
```

默认后端地址：

- `http://localhost:8080`

### 3. 启动前端

```bash
cd E:\keshe\campus-activity-system\frontend
npm install
npm run dev
```

默认前端地址：

- `http://localhost:5173`

前端已配置代理，`/api` 请求会自动转发到 `http://localhost:8080`。相关配置可见 [vite.config.js](file:///e:/keshe/campus-activity-system/frontend/vite.config.js)。

## 演示账号

| 用户名      | 密码   | 角色   |
| ----------- | ------ | ------ |
| admin       | 123456 | 管理员 |
| organizer01 | 123456 | 组织者 |
| organizer02 | 123456 | 组织者 |
| student01   | 123456 | 学生   |
| student02   | 123456 | 学生   |
| student03   | 123456 | 学生   |

## 推荐演示路径

如果用于课堂答辩，建议按以下顺序操作：

1. 进入首页，展示推荐活动、公告、即将开始和活动广场。
2. 使用学生账号登录，演示报名、签到、签退、评价和“我的报名”。
3. 使用组织者账号登录，演示创建活动、编辑活动、提交审核。
4. 使用管理员账号登录，演示活动审核、后台统计、状态分布、分类管理和公告管理。

这样能够完整串起三类角色的主业务流程。

## 后台管理说明

管理员后台当前包含以下模块：

- 数据概览
- 用户管理
- 活动审核
- 全部活动
- 分类管理
- 公告管理

后台还加入了一些演示期常见保护逻辑：

- 全部活动是真正的全量列表，支持分页查看。
- 当前登录管理员不能被自己禁用。
- 最后一个可用管理员不能被禁用。
- 删除分类前会检查是否仍被活动使用。

## 接口返回格式

系统接口统一返回如下结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

前端已在 [request.js](file:///e:/keshe/campus-activity-system/frontend/src/api/request.js) 中统一处理登录失效、业务异常和网络错误。

## 文档说明

- `docs/schema.sql`：数据库建表脚本
- `docs/data.sql`：初始化演示数据
- `docs/网络实训报告.md`：网络综合开发实训课程报告
- `docs/Java框架报告.md`：Java 框架课程报告
- `docs/给AI的交接说明.md`：项目阶段性实现说明

## 说明

- 本项目更偏向课程设计演示与功能完整性展示，而不是生产环境系统。
- 若页面数据与数据库改动不一致，通常需要重新导入 `docs/data.sql` 或重启后端。
- 如果使用命令行直接执行 SQL，请注意终端编码，避免中文数据出现乱码或问号。
