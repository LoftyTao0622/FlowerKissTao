-- ============================================================
-- FlowerKissTao RBAC 标准五表
-- 库：plant   字符集：utf8mb4
-- 执行：先 schema.sql 建表，再 data.sql 灌初始数据
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `username`    VARCHAR(50)  NOT NULL                COMMENT '登录名',
  `password`    VARCHAR(100) NOT NULL                COMMENT 'BCrypt 哈希，禁止存明文',
  `nickname`    VARCHAR(50)      NULL                COMMENT '昵称',
  `phone`       VARCHAR(20)      NULL                COMMENT '手机号',
  `avatar`      VARCHAR(255)     NULL                COMMENT '头像地址',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '1正常 0封禁',
  `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除 0未删 1已删',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- ------------------------------------------------------------
-- 2. 角色表（数据量极小，3~4 行）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `code`        VARCHAR(50) NOT NULL                 COMMENT 'ROLE_USER / ROLE_OPERATOR / ROLE_ADMIN',
  `name`        VARCHAR(50) NOT NULL                 COMMENT '中文名',
  `description` VARCHAR(200)    NULL,
  `sort`        INT         NOT NULL DEFAULT 0       COMMENT '展示排序',
  `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

-- ------------------------------------------------------------
-- 3. 权限点表，code 形如 模块:资源:动作
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `code`       VARCHAR(80) NOT NULL                  COMMENT 'catalog:plant:update',
  `name`       VARCHAR(50) NOT NULL                  COMMENT '编辑植物商品',
  `module`     VARCHAR(30) NOT NULL                  COMMENT '所属模块，与后端包名对应',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perm_code` (`code`),
  KEY `idx_perm_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限点';

-- ------------------------------------------------------------
-- 4. 用户-角色（多对多，联合主键天然去重）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_ur_role` (`role_id`),
  CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色';

-- ------------------------------------------------------------
-- 5. 角色-权限（多对多）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
  `role_id`       BIGINT NOT NULL,
  `permission_id` BIGINT NOT NULL,
  PRIMARY KEY (`role_id`, `permission_id`),
  KEY `idx_rp_perm` (`permission_id`),
  CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_rp_perm` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限';

-- ------------------------------------------------------------
-- 6. 植物商品表
--
-- match_tags / care_tips 用 JSON 字符串数组存：各三五条、整行读写、从不按元素检索。
-- 拆子表要多两张表和本项目第一个 mapper XML，换不来查询收益；用分隔符拼 VARCHAR
-- 则因文案本身含中文顿号和逗号而迟早损坏。
--
-- 注意实体上必须写 @TableName(autoResultMap = true)，否则 typeHandler 只在写入时
-- 生效、查询时静默返回 null。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `catalog_plant`;
CREATE TABLE `catalog_plant` (
  `id`                    BIGINT        NOT NULL AUTO_INCREMENT,
  `slug`                  VARCHAR(80)   NOT NULL                COMMENT '详情页 URL 标识，如 fiddle-leaf-fig',
  `name`                  VARCHAR(50)   NOT NULL                COMMENT '中文名',
  `latin_name`            VARCHAR(100)  NOT NULL                COMMENT '拉丁学名',
  `price`                 DECIMAL(10,2) NOT NULL                COMMENT '含基础花盆参考价，单位元',
  `image`                 VARCHAR(255)  NOT NULL                COMMENT '主图地址，形如 /images/fiddle-leaf-fig.webp',
  `image_alt`             VARCHAR(120)  NOT NULL                COMMENT '主图 alt 文案，无障碍要求必填',
  `category`              VARCHAR(20)   NOT NULL                COMMENT '大型绿植 / 桌面绿植 / 特色观叶 / 耐阴绿植',
  `light`                 VARCHAR(20)   NOT NULL                COMMENT '明亮散射光 / 柔和散射光 / 低至中等光照',
  `watering`              VARCHAR(60)   NOT NULL                COMMENT '浇水节奏描述',
  `size`                  VARCHAR(60)   NOT NULL                COMMENT '株型尺寸描述',
  `pet_friendly`          TINYINT       NOT NULL DEFAULT 0      COMMENT '1宠物友好 0需隔离',
  `pet_note`              VARCHAR(60)       NULL                COMMENT 'pet_friendly=0 时展示的隔离提示',
  `difficulty`            VARCHAR(20)   NOT NULL                COMMENT '新手友好 / 需要关注 / 进阶养护',
  `match_tags`            JSON              NULL                COMMENT '适配标签，JSON 字符串数组',
  `recommendation_reason` VARCHAR(255)  NOT NULL                COMMENT '推荐理由，需说明环境依据',
  `short_description`     VARCHAR(255)  NOT NULL                COMMENT '卡片用一句话简介',
  `description`           TEXT              NULL                COMMENT '详情页正文',
  `care_tips`             JSON              NULL                COMMENT '养护备忘，JSON 字符串数组，顺序即展示顺序',
  `featured`              TINYINT       NOT NULL DEFAULT 0      COMMENT '1首页精选 0普通',
  `status`                TINYINT       NOT NULL DEFAULT 1      COMMENT '1上架 0下架，下架后公开接口不可见',
  `sort`                  INT           NOT NULL DEFAULT 0      COMMENT '展示排序，值大者靠前',
  `deleted`               TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除 0未删 1已删',
  `created_at`            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plant_slug` (`slug`),
  KEY `idx_plant_status_sort` (`status`, `sort`),
  KEY `idx_plant_category` (`category`),
  KEY `idx_plant_featured` (`featured`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='植物商品';
