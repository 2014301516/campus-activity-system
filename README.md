# 校园活动管理系统

## 技术栈

- 前端：Vue 3 + Element Plus + Vue Router + Pinia + Axios + ECharts
- 后端：Spring Boot 2.7 + MyBatis-Plus + Spring Security + JWT
- 数据库：MySQL 8.0

## 运行方式

**数据库**：MySQL 8.0，执行 `docs/schema.sql` 和 `docs/data.sql` 建表和导入测试数据。

**后端**：IntelliJ IDEA 打开 `backend/pom.xml`，运行 `CampusActivityApplication`。

**前端**：

```bash
cd frontend
npm install
npm run dev
```

## 测试账号（密码均为 123456）

| 用户名 | 角色 |
|--------|------|
| admin | 管理员 |
| organizer01 | 组织者 |
| student01 | 学生 |
