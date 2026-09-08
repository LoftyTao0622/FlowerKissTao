# FlowerKissTao

> 一个把植物选购、个性化推荐与日常养护串起来的全栈应用。  
> A full-stack plant shop that connects discovery, personalized recommendations, and everyday care.

[中文](#中文) · [English](#english)

<a id="中文"></a>

## 中文

### 项目做什么

FlowerKissTao 面向想买植物、也想把植物养好的人。用户可以浏览植物与 SKU、维护个人场景资料、获取推荐、管理购物车和收货地址、提交订单，并在养护模块查看档案与提醒。后台提供商品、SKU、知识内容、用户、订单和运营数据的管理入口。

### 技术组成

- **后端**：Java 17、Spring Boot 3.5.16、Spring Security、JWT、MyBatis-Plus、MySQL、Redis
- **前端**：Vue 3、TypeScript、Vite、Vue Router、Pinia、Element Plus
- **运行职责**：MySQL 保存业务数据；Redis 用于缓存、限流与分布式锁；Spring Task 生成养护任务和站内提醒

### 快速开始

#### 1. 准备依赖

- JDK 17+
- Maven 3.9+
- Node.js `^22.18.0` 或 `>=24.12.0`
- pnpm 11+
- MySQL 8.x（数据库名：`plant`）
- Redis（默认 `127.0.0.1:6386`）

#### 2. 初始化数据库

按顺序执行：

```bash
mysql -u root -p plant < src/main/resources/db/schema.sql
mysql -u root -p plant < src/main/resources/db/data.sql
```

#### 3. 启动后端

在仓库根目录执行：

```bash
mvn spring-boot:run
```

API 默认监听 `http://localhost:8099`。

#### 4. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

开发服务器地址由 Vite 输出；前端请求配置可参考 `frontend/.env.example`。

### 配置

常用环境变量：`MYSQL_USERNAME`、`MYSQL_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`、`JWT_SECRET`、`APP_UPLOAD_DIR`。生产环境请显式设置 `JWT_SECRET`，并根据实际部署修改数据库和 Redis 连接。

### 验证

```bash
# 后端测试
mvn test

# 前端类型检查与构建
cd frontend
pnpm type-check
pnpm build
```

仓库还提供数据库与演示脚本：`scripts/verify_schema.py`、`scripts/verify_clean_init.py`、`scripts/smoke_demo.py`。

### 目录导航

- `src/main/java/com/zyt/flowerkisstao`：Spring Boot 后端，按 user、catalog、recommendation、trade、care、knowledge 等领域组织
- `src/main/resources/db`：数据库结构与种子数据
- `frontend/src`：Vue 应用、路由、布局与业务模块
- `scripts`：初始化校验与冒烟验证脚本

### 许可证

项目采用仓库中的 [LICENSE](LICENSE) 条款。

<a id="english"></a>

## English

### What it is

FlowerKissTao is for people who want to choose the right plants and keep them healthy afterwards. It combines a plant and SKU catalog, scene-based profiles, recommendations, cart and address management, orders, care archives, and reminders. Admin screens cover catalog, knowledge, users, orders, and operations.

### Stack

- **Backend**: Java 17, Spring Boot 3.5.16, Spring Security, JWT, MyBatis-Plus, MySQL, Redis
- **Frontend**: Vue 3, TypeScript, Vite, Vue Router, Pinia, Element Plus
- **Runtime roles**: MySQL is the source of business data; Redis handles caching, rate limiting, and distributed locks; Spring Task creates care tasks and in-app reminders

### Quick start

#### Prerequisites

JDK 17+, Maven 3.9+, Node.js `^22.18.0` or `>=24.12.0`, pnpm 11+, MySQL 8.x, and Redis. The default Redis endpoint is `127.0.0.1:6386`; the MySQL database is `plant`.

#### Initialize MySQL

```bash
mysql -u root -p plant < src/main/resources/db/schema.sql
mysql -u root -p plant < src/main/resources/db/data.sql
```

#### Run the backend

```bash
mvn spring-boot:run
```

The API listens on `http://localhost:8099` by default.

#### Run the frontend

```bash
cd frontend
pnpm install
pnpm dev
```

Use the Vite URL printed in the terminal. Frontend request settings are documented in `frontend/.env.example`.

### Configuration and checks

Set `MYSQL_USERNAME`, `MYSQL_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `JWT_SECRET`, and `APP_UPLOAD_DIR` as needed. Set `JWT_SECRET` explicitly outside local development.

```bash
mvn test
cd frontend
pnpm type-check
pnpm build
```

### Repository map

- `src/main/java/com/zyt/flowerkisstao`: domain-oriented Spring Boot backend
- `src/main/resources/db`: schema and seed data
- `frontend/src`: Vue app, routing, layouts, and modules
- `scripts`: database checks and smoke demos

### License

See the repository [LICENSE](LICENSE).
