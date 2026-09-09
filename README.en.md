# FlowerKissTao · 花吻陶

[中文说明](README.md)

FlowerKissTao is a full-stack application for indoor-plant enthusiasts. It lets people browse a plant catalog, generate explainable recommendations from light, space, pet-safety, and care-time constraints, then continue through purchasing and ongoing care records. The repository contains a Spring Boot API and a Vue single-page frontend.

![Indoor plants in a naturally lit greenhouse scene](frontend/src/assets/images/hero-greenhouse.webp)

The image reflects the product path on the home page: describe an environment first, then receive recommendations that can be acted on. Visitors can browse the catalog and knowledge base; signed-in users can save scene profiles, recommendation results, carts, orders, and care plans.

## Quick start

### Prerequisites

- JDK 17 or newer (the Maven build targets Java 17)
- Maven
- Node.js `^22.18.0 || >=24.12.0`
- pnpm 11.9.0
- MySQL (the default database name is `plant`)
- Redis (the default port is `6386`; Redis can also be disabled through configuration)

### 1. Initialize MySQL

From the repository root, create the database and apply the supplied schema and seed scripts in order:

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p -e "CREATE DATABASE IF NOT EXISTS plant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -h 127.0.0.1 -P 3306 -u root -p plant < src/main/resources/db/schema.sql
mysql -h 127.0.0.1 -P 3306 -u root -p plant < src/main/resources/db/data.sql
```

Set `MYSQL_*` and `REDIS_*` environment variables before starting the backend when your services use different addresses. Seed accounts are intended for local demonstration; you can also register a fresh account from the login page.

### 2. Start the backend

Run this from the repository root:

```bash
mvn spring-boot:run
```

The API listens on `http://127.0.0.1:8099` by default.

### 3. Start the frontend

In a second terminal:

```bash
cd frontend
pnpm install
pnpm dev
```

Open <http://127.0.0.1:5173>. Vite proxies `/api` and `/uploads` to the backend on port 8099. See [`frontend/.env.example`](frontend/.env.example) for frontend variables.

## Capabilities

- **Plant browsing**: catalog, details, and filter facets under `/api/catalog`.
- **Personalized recommendations**: save multiple scene profiles, generate recommendations from constraints and preferences, and revisit past results.
- **Commerce flow**: cart, shipping addresses, order transitions, and after-sales operations under `/api/cart`, `/api/addresses`, and `/api/orders`.
- **Ongoing care**: plant archives, tasks, notifications, health reports, and notes under `/api/care`; scheduled work is driven by Spring Scheduling.
- **Knowledge base**: public articles, facets, related recommendations, favorites, and usefulness feedback under `/api/knowledge`.
- **Operations console**: `/admin` includes dashboards, species and SKU management, orders, article workflow, recommendation weights, users and roles, and operation logs. Spring Security and JWT protect authenticated and permissioned actions.

## API areas

| Path | Purpose |
| --- | --- |
| `/api/auth` | Registration, login, current user, and logout |
| `/api/catalog` | Plant catalog, details, and facets |
| `/api/profiles` | Scene profiles |
| `/api/recommendations` | Recommendation generation, history, and click feedback |
| `/api/cart`, `/api/addresses`, `/api/orders` | Cart, addresses, and orders |
| `/api/care` | Care archives, tasks, notifications, and maintenance actions |
| `/api/knowledge` | Articles, facets, favorites, and feedback |
| `/api/admin/*` | Permission-gated administration APIs |

## Repository map

```text
src/main/java/com/zyt/flowerkisstao/
├── catalog/          # Plant catalog and SKUs
├── recommendation/   # Recommendation rules and results
├── trade/            # Cart, addresses, and orders
├── care/             # Care archives, tasks, and notifications
├── knowledge/        # Knowledge base
├── user/             # Authentication, profiles, and roles
├── operation/        # Operations dashboard and logs
└── shared/           # Security, Redis, configuration, errors, and web concerns
frontend/src/
├── app/               # Router, layouts, and global styles
├── modules/           # Domain-aligned pages, APIs, stores, and types
└── shared/            # Requests, auth state, and reusable components
```

MySQL stores business data. Redis supports caching, rate limiting, and distributed locks. The frontend and backend communicate through JSON under `/api`; uploaded files are served under `/uploads`.

## Configuration and security

Backend settings live in [`src/main/resources/application.yml`](src/main/resources/application.yml). Environment variables can override database, Redis, upload, and JWT settings:

- `MYSQL_USERNAME`, `MYSQL_PASSWORD` (and `SPRING_DATASOURCE_URL`)
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_DATABASE`, `REDIS_ENABLED`
- `APP_UPLOAD_DIR`
- `JWT_SECRET`, `JWT_EXPIRE_MINUTES`

Defaults in the configuration file are for local development. For shared or production environments, inject new database/Redis credentials and a new JWT secret, and restrict access to the upload directory.

## Verification

```bash
# Backend tests and Spring context test
mvn test

# Frontend type checking and production build
cd frontend
pnpm type-check
pnpm build
```

Run these scripts from the repository root (if the previous step left you in `frontend`, run `cd ..` first):

```bash
# Rebuild schema + data in an isolated database and check Spring startup
python scripts/verify_clean_init.py

# Compare schema.sql with an existing database's tables, columns, and indexes
python scripts/verify_schema.py [database]

# With the backend running and the database seeded, run the register → recommendation → order → care → admin smoke flow
python scripts/smoke_demo.py --base-url http://127.0.0.1:8099
```

## License

Released under the [MIT License](LICENSE).
