# 花吻陶 · FlowerKissTao

[English README](README.en.md)

花吻陶是一个面向室内植物爱好者的全栈应用：浏览植物目录，按光照、空间、宠物安全和养护时间生成个性化推荐，并继续完成购买与养护记录。项目包含 Spring Boot API 与 Vue 单页前端。

![自然光下的室内植物温室场景](frontend/src/assets/images/hero-greenhouse.webp)

上图对应首页的核心体验：先描述你的环境，再得到可解释的植物推荐。游客可以浏览目录和知识库；登录后可保存场景画像、推荐结果、购物车、订单和养护计划。

## 快速开始

### 环境要求

- JDK 17 或更高版本（Maven 编译目标为 Java 17）
- Maven
- Node.js `^22.18.0 || >=24.12.0`
- pnpm 11.9.0
- MySQL（默认连接数据库名为 `plant`）
- Redis（默认端口为 `6386`；也可以通过配置关闭 Redis）

### 1. 初始化数据库

在仓库根目录创建数据库，并依次执行项目提供的结构和种子数据脚本：

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p -e "CREATE DATABASE IF NOT EXISTS plant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -h 127.0.0.1 -P 3306 -u root -p plant < src/main/resources/db/schema.sql
mysql -h 127.0.0.1 -P 3306 -u root -p plant < src/main/resources/db/data.sql
```

如果你的 MySQL 或 Redis 不使用默认地址，请在启动后端前设置 `MYSQL_*`、`REDIS_*` 环境变量。种子账号仅用于本地演示；也可以直接在登录页注册新账号。

### 2. 启动后端

在仓库根目录运行：

```bash
mvn spring-boot:run
```

API 默认监听 `http://127.0.0.1:8099`。

### 3. 启动前端

另开一个终端：

```bash
cd frontend
pnpm install
pnpm dev
```

打开 <http://127.0.0.1:5173>。Vite 会把 `/api` 和 `/uploads` 请求代理到后端 8099 端口。前端环境变量示例见 [`frontend/.env.example`](frontend/.env.example)。

## 能做什么

- **逛植物**：目录、详情和筛选维度来自 `/api/catalog`。
- **智能推荐**：保存多个场景画像，按环境与偏好生成推荐，并保留推荐结果供回看。
- **购买流程**：购物车、收货地址、订单状态流转和售后操作位于 `/api/cart`、`/api/addresses`、`/api/orders`。
- **持续养护**：植物档案、任务、提醒、健康报告和养护笔记位于 `/api/care`；定时任务由 Spring Scheduling 驱动。
- **养护知识库**：公开文章、分类筛选、推荐文章、收藏与有用反馈位于 `/api/knowledge`。
- **运营后台**：`/admin` 提供看板、品种与 SKU、订单、知识文章审核、推荐权重、用户角色和操作日志管理；后端使用 Spring Security 与 JWT 做认证和权限控制。

## API 分区

| 路径 | 用途 |
| --- | --- |
| `/api/auth` | 注册、登录、当前用户与退出 |
| `/api/catalog` | 植物目录、详情和筛选项 |
| `/api/profiles` | 场景画像 |
| `/api/recommendations` | 推荐生成、历史结果和点击反馈 |
| `/api/cart`、`/api/addresses`、`/api/orders` | 购物车、地址和订单 |
| `/api/care` | 养护档案、任务、提醒与维护操作 |
| `/api/knowledge` | 知识文章、筛选、收藏和反馈 |
| `/api/admin/*` | 后台管理接口（按权限开放） |

## 项目结构

```text
src/main/java/com/zyt/flowerkisstao/
├── catalog/          # 植物目录与 SKU
├── recommendation/   # 推荐规则与结果
├── trade/            # 购物车、地址、订单
├── care/             # 养护档案、任务、提醒
├── knowledge/        # 养护知识库
├── user/             # 认证、画像、角色
├── operation/        # 运营看板与日志
└── shared/           # 安全、Redis、配置、异常与通用 Web 层
frontend/src/
├── app/               # 路由、布局和全局样式
├── modules/           # 与后端领域对应的页面、API、store 和类型
└── shared/            # 请求、认证状态和通用组件
```

MySQL 保存业务数据；Redis 用于缓存、限流和分布式锁。前后端通过 `/api` JSON 接口通信，上传文件通过 `/uploads` 提供访问。

## 配置与安全

后端配置位于 [`src/main/resources/application.yml`](src/main/resources/application.yml)，可用环境变量覆盖数据库、Redis、上传目录和 JWT 设置：

- `MYSQL_USERNAME`、`MYSQL_PASSWORD`（以及 `SPRING_DATASOURCE_URL`）
- `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`、`REDIS_DATABASE`、`REDIS_ENABLED`
- `APP_UPLOAD_DIR`
- `JWT_SECRET`、`JWT_EXPIRE_MINUTES`

配置文件中的默认值只适合本地开发。部署到共享或生产环境时，请注入新的数据库/Redis 凭据和 JWT 密钥，并限制上传目录权限。

## 验证命令

```bash
# 后端单元测试与 Spring 上下文测试
mvn test

# 前端类型检查与生产构建
cd frontend
pnpm type-check
pnpm build
```

以下脚本都应在仓库根目录运行（如上一步仍在 `frontend` 目录，请先执行 `cd ..`）：

```bash
# 在隔离数据库中重建 schema + data，并启动 Spring 上下文检查
python scripts/verify_clean_init.py

# 对比 schema.sql 与现有数据库的表、列和索引
python scripts/verify_schema.py [database]

# 后端已启动且数据库已初始化后，执行注册→推荐→下单→养护→后台的冒烟流程
python scripts/smoke_demo.py --base-url http://127.0.0.1:8099
```

## 许可证

本项目以 [MIT License](LICENSE) 发布。
