-- ============================================================
-- FlowerKissTao 建表脚本
-- 库：plant   字符集：utf8mb4
-- 执行：先 schema.sql 建表，再 data.sql 灌初始数据
-- ============================================================

SET NAMES utf8mb4;

-- 各表的 DROP 顺序与外键依赖相反（sys_user 被 sys_user_role 引用），
-- 逐张 DROP 会在重复执行时报 errno 3730。先关掉检查，末尾再恢复。
SET FOREIGN_KEY_CHECKS = 0;

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
-- 6. 植物品种表（知识属性）
--
-- 品种与商品分开：同一株琴叶榕可以有 60cm 陶盆版和 90cm 藤编盆版两个 SKU，
-- 而"喜明亮散射光、猫狗有毒、需水 7 天一次"是品种固有的，与卖哪个规格无关。
--
-- 每组环境条件都存两份：数值列供推荐算法计算，_note 列供页面展示。
-- 光照写成 light_min/light_max 而非单值，是因为多数植物有可接受区间；
-- 硬过滤判断的是"用户环境等级是否落在区间内"，单值做不到。
--
-- 毒性拆 toxic_cat / toxic_dog / toxic_child 三列而非一个 pet_friendly：
-- 方案要求"有猫狗或幼童时排除存在相应安全风险的品种"，猫毒与狗毒并非同一批
-- （如百合科对猫剧毒、对狗温和），合成一列就做不到"相应"。
--
-- match_tags / care_tips 用 JSON 字符串数组存：各三五条、整行读写、从不按元素检索。
-- 拆子表要多两张表和本项目第一个 mapper XML，换不来查询收益；用分隔符拼 VARCHAR
-- 则因文案本身含中文顿号和逗号而迟早损坏。
--
-- 注意实体上必须写 @TableName(autoResultMap = true)，否则 typeHandler 只在写入时
-- 生效、查询时静默返回 null。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `catalog_sku`;
DROP TABLE IF EXISTS `catalog_plant`;
DROP TABLE IF EXISTS `catalog_species`;

CREATE TABLE `catalog_species` (
  `id`                      BIGINT        NOT NULL AUTO_INCREMENT,
  `code`                    VARCHAR(80)   NOT NULL                COMMENT '详情页 URL 标识，如 fiddle-leaf-fig。前端路由用的就是它',
  `name`                    VARCHAR(50)   NOT NULL                COMMENT '中文名',
  `latin_name`              VARCHAR(100)  NOT NULL                COMMENT '拉丁学名',
  `category`                VARCHAR(20)   NOT NULL                COMMENT '大型绿植 / 桌面绿植 / 特色观叶 / 耐阴绿植',
  `ornamental_type`         VARCHAR(10)   NOT NULL DEFAULT 'leaf' COMMENT 'leaf 观叶 / flower 观花',

  -- 观花品种才填，观叶品种留空
  `bloom_season`            VARCHAR(30)       NULL                COMMENT '花期，如"春末至初夏"',
  `bloom_color`             VARCHAR(30)       NULL                COMMENT '花色',
  `fragrance`               VARCHAR(20)       NULL                COMMENT '香味强度，如"淡香"',

  -- 光照：1 低光 / 2 柔和散射 / 3 明亮散射 / 4 充足直射
  `light_min`               TINYINT       NOT NULL                COMMENT '可接受的最低光照等级 1-4',
  `light_max`               TINYINT       NOT NULL                COMMENT '可接受的最高光照等级 1-4，须 >= light_min',
  `light_note`              VARCHAR(20)   NOT NULL                COMMENT '光照展示文案，如"明亮散射光"',

  `temp_min`                SMALLINT      NOT NULL                COMMENT '可耐受最低温，摄氏度',
  `temp_max`                SMALLINT      NOT NULL                COMMENT '可耐受最高温，摄氏度',
  `humidity_min`            TINYINT       NOT NULL                COMMENT '适宜最低空气湿度百分比',
  `humidity_max`            TINYINT       NOT NULL                COMMENT '适宜最高空气湿度百分比',

  `water_interval_days`     SMALLINT      NOT NULL                COMMENT '浇水间隔天数，越小越费心。用户"每周可浇水次数"与它比对',
  `water_note`              VARCHAR(60)   NOT NULL                COMMENT '浇水展示文案',
  `fertilize_interval_days` SMALLINT      NOT NULL DEFAULT 30     COMMENT '施肥间隔天数，购后养护任务按它排期',
  `repot_interval_months`   SMALLINT      NOT NULL DEFAULT 24     COMMENT '换盆间隔月数',
  `prune_needed`            TINYINT       NOT NULL DEFAULT 0      COMMENT '1 需定期修剪 0 不需要',
  `care_level`              TINYINT       NOT NULL                COMMENT '1 新手友好 / 2 需要关注 / 3 进阶养护',

  `toxic_cat`               TINYINT       NOT NULL DEFAULT 0      COMMENT '1 对猫有毒',
  `toxic_dog`               TINYINT       NOT NULL DEFAULT 0      COMMENT '1 对狗有毒',
  `toxic_child`             TINYINT       NOT NULL DEFAULT 0      COMMENT '1 儿童误食有风险',
  `pollen_risk`             TINYINT       NOT NULL DEFAULT 0      COMMENT '1 花粉过敏风险',

  `mature_height_cm`        SMALLINT      NOT NULL                COMMENT '成株高度厘米，空间硬过滤用',
  `footprint_cm`            SMALLINT      NOT NULL                COMMENT '成株冠幅厘米，小空间放不下大冠幅',

  `match_tags`              JSON              NULL                COMMENT '适配标签，JSON 字符串数组',
  `recommendation_reason`   VARCHAR(255)  NOT NULL                COMMENT '推荐理由，需说明环境依据',
  `short_description`       VARCHAR(255)  NOT NULL                COMMENT '卡片用一句话简介',
  `description`             TEXT              NULL                COMMENT '详情页正文',
  `care_tips`               JSON              NULL                COMMENT '养护备忘，JSON 字符串数组，顺序即展示顺序',

  `status`                  TINYINT       NOT NULL DEFAULT 1      COMMENT '1 启用 0 停用，停用后公开接口不可见',
  `sort`                    INT           NOT NULL DEFAULT 0      COMMENT '展示排序，值大者靠前',
  `deleted`                 TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除 0未删 1已删',
  `created_at`              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_species_code` (`code`),
  KEY `idx_species_status_sort` (`status`, `sort`),
  KEY `idx_species_category` (`category`),
  -- 推荐算法的硬过滤先按光照与毒性收窄候选集，这三列组合命中率最高
  KEY `idx_species_light` (`light_min`, `light_max`),
  KEY `idx_species_toxic` (`toxic_cat`, `toxic_dog`, `toxic_child`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='植物品种（知识属性）';

-- ------------------------------------------------------------
-- 7. 植物商品表（销售属性）
--
-- 一个品种可挂多个 SKU，规格与盆器不同则价格不同。
-- 库存放在这一层：第④步下单扣的是 SKU 的 stock，不是品种的。
--
-- 不设 fk 到 catalog_species：品种用的是逻辑删除，外键约束拦不住
-- deleted=1 的脏引用，反而会在删品种时抛数据库异常而非业务异常。
-- 引用完整性由 SkuAdminService 在应用层校验。
-- ------------------------------------------------------------
CREATE TABLE `catalog_sku` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT,
  `species_id`  BIGINT        NOT NULL                COMMENT '所属品种 catalog_species.id',
  `sku_code`    VARCHAR(80)   NOT NULL                COMMENT '商品编码，如 fiddle-leaf-fig-75cm',
  `spec`        VARCHAR(60)   NOT NULL                COMMENT '规格文案，如"中大型 · 约 75 厘米"',
  `pot`         VARCHAR(40)       NULL                COMMENT '盆器，如"米白陶盆"',
  `price`       DECIMAL(10,2) NOT NULL                COMMENT '售价，单位元',
  `stock`       INT           NOT NULL DEFAULT 0      COMMENT '可售库存，下单时事务内扣减',
  `image`       VARCHAR(255)  NOT NULL                COMMENT '主图地址，形如 /images/fiddle-leaf-fig.webp',
  `image_alt`   VARCHAR(120)  NOT NULL                COMMENT '主图 alt 文案，无障碍要求必填',
  `featured`    TINYINT       NOT NULL DEFAULT 0      COMMENT '1 首页精选 0 普通',
  `status`      TINYINT       NOT NULL DEFAULT 1      COMMENT '1 上架 0 下架',
  `sort`        INT           NOT NULL DEFAULT 0      COMMENT '同品种内排序，值大者为默认 SKU',
  `deleted`     TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除 0未删 1已删',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_code` (`sku_code`),
  KEY `idx_sku_species` (`species_id`, `status`, `sort`),
  KEY `idx_sku_status_sort` (`status`, `sort`),
  KEY `idx_sku_featured` (`featured`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='植物商品 SKU（销售属性）';

-- ------------------------------------------------------------
-- 8. 用户场景画像表
--
-- 推荐算法的输入端。一个用户可以存多份：客厅、卧室、办公室、阳台的
-- 光照与空间条件完全不同，共用一份画像等于假装它们一样。
--
-- 温湿度存 1/2/3 档而非精确数值：用户量不出自家空气湿度，逼他们填
-- "22.5℃"只会得到瞎猜的数据。档位与 catalog_species.temp_min/max 的
-- 比对逻辑同样能算，而且用户答得出来。
--
-- 这张表只存输入，不存推荐结果——方案原文："画像只负责提供标准化输入，
-- 不直接决定排序"。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user_scene_profile`;
CREATE TABLE `user_scene_profile` (
  `id`                 BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`            BIGINT        NOT NULL                COMMENT '所属用户 sys_user.id',
  `scene_name`         VARCHAR(50)   NOT NULL                COMMENT '场景名，如"客厅"，用户可自定义。留出删除时拼 -del-<id> 后缀的空间',
  `is_default`         TINYINT       NOT NULL DEFAULT 0      COMMENT '1 默认场景，每用户至多一个',

  -- ===== 环境条件：硬过滤与加权评分的输入 =====
  `placement`          VARCHAR(20)   NOT NULL                COMMENT 'living_room/bedroom/office/balcony/other',
  `light_level`        TINYINT       NOT NULL                COMMENT '1 低光 / 2 柔和散射 / 3 明亮散射 / 4 充足直射，与 species.light_min~max 同刻度',
  `temp_level`         TINYINT       NOT NULL                COMMENT '1 偏冷 <15℃ / 2 常温 15-28℃ / 3 偏热 >28℃',
  `humidity_level`     TINYINT       NOT NULL                COMMENT '1 干燥 <40% / 2 适中 40-70% / 3 潮湿 >70%',
  `space_level`        TINYINT       NOT NULL                COMMENT '1 桌面 / 2 层架 / 3 落地',
  `max_footprint_cm`   SMALLINT      NOT NULL                COMMENT '冠幅上限厘米，由 space_level 派生并冗余存储，硬过滤直接比对 species.footprint_cm',
  `ventilation`        TINYINT       NOT NULL                COMMENT '1 较差 / 2 一般 / 3 良好',
  `budget_min`         DECIMAL(10,2) NOT NULL DEFAULT 0      COMMENT '预算下限，元',
  `budget_max`         DECIMAL(10,2) NOT NULL                COMMENT '预算上限，元。硬过滤排除超价商品',
  `prefer_ornamental`  VARCHAR(10)   NOT NULL DEFAULT 'any'  COMMENT 'leaf 观叶 / flower 观花 / any 都行',

  -- ===== 安全条件：与 species 的 toxic_cat/dog/child 三列一一对应 =====
  `has_child`          TINYINT       NOT NULL DEFAULT 0      COMMENT '1 家中有幼童',
  `has_cat`            TINYINT       NOT NULL DEFAULT 0      COMMENT '1 家中养猫',
  `has_dog`            TINYINT       NOT NULL DEFAULT 0      COMMENT '1 家中养狗',

  -- ===== 养护能力：决定能接受多高的养护难度 =====
  `experience_level`   TINYINT       NOT NULL                COMMENT '1 新手 / 2 有一些 / 3 有经验',
  `water_times_week`   TINYINT       NOT NULL                COMMENT '每周可浇水次数 0-7，与 species.water_interval_days 比对',
  `travel_frequency`   TINYINT       NOT NULL                COMMENT '1 很少 / 2 偶尔 / 3 频繁出差',
  `forgetful`          TINYINT       NOT NULL DEFAULT 0      COMMENT '1 容易忘记养护',
  `accept_repot`       TINYINT       NOT NULL DEFAULT 1      COMMENT '1 接受换盆',
  `accept_fertilize`   TINYINT       NOT NULL DEFAULT 1      COMMENT '1 接受施肥',
  `accept_prune`       TINYINT       NOT NULL DEFAULT 1      COMMENT '1 接受修剪',

  `deleted`            TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除 0未删 1已删',
  `created_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  -- 场景名同用户下不重复。这是物理唯一索引，不认 deleted 标记：
  -- 删除时由 Service 给 scene_name 加 -del-<id> 后缀腾位，做法与 catalog 的 code 一致。
  -- 若把 deleted 放进唯一键，同一个名字删第二次仍会撞（两行都是 deleted=1）。
  UNIQUE KEY `uk_profile_user_scene` (`user_id`, `scene_name`),
  KEY `idx_profile_user_default` (`user_id`, `is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户场景画像（推荐算法输入）';

-- ------------------------------------------------------------
-- 9. 推荐权重配置表
--
-- 单行表（id 恒为 1）。七个权重必须同时满足"和为 100"，拆成七行键值对后
-- 没有任何一条约束能拦住"只改了一半"的中间状态——那时算出来的分是错的
-- 但看不出来。存成一行才能用一个 CHECK 管住。
--
-- 默认值 30/15/10/20/10/10/5 来自方案架构章节的权重口径。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `rec_weight_config`;
CREATE TABLE `rec_weight_config` (
  `id`            BIGINT   NOT NULL                COMMENT '恒为 1，单行配置',
  `w_light`       TINYINT  NOT NULL DEFAULT 30     COMMENT '光照权重百分比',
  `w_temp`        TINYINT  NOT NULL DEFAULT 15     COMMENT '温度权重百分比',
  `w_humidity`    TINYINT  NOT NULL DEFAULT 10     COMMENT '湿度权重百分比',
  `w_care`        TINYINT  NOT NULL DEFAULT 20     COMMENT '养护能力权重百分比',
  `w_space`       TINYINT  NOT NULL DEFAULT 10     COMMENT '空间权重百分比',
  `w_budget`      TINYINT  NOT NULL DEFAULT 10     COMMENT '预算贴合权重百分比',
  `w_preference`  TINYINT  NOT NULL DEFAULT 5      COMMENT '观赏偏好权重百分比',
  `top_n`         TINYINT  NOT NULL DEFAULT 3      COMMENT '返回条数',
  `updated_by`    BIGINT       NULL                COMMENT '最后修改人 sys_user.id',
  `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  -- 和不为 100 时加权总分就不在 0-100 刻度上，前端的百分比进度条会画错。
  -- MySQL 8.0.16 起 CHECK 才真正生效，低版本会被忽略——Service 层同样校验一遍。
  CONSTRAINT `ck_rec_weight_sum` CHECK (
    `w_light` + `w_temp` + `w_humidity` + `w_care` + `w_space` + `w_budget` + `w_preference` = 100
  ),
  CONSTRAINT `ck_rec_top_n` CHECK (`top_n` BETWEEN 1 AND 20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐算法权重配置（单行）';

-- ------------------------------------------------------------
-- 10. 推荐结果快照表
--
-- 方案要求推荐"可复现、可解释"。只存 species_id 列表做不到这一点：
-- 管理员改过权重之后，历史推荐就再也解释不清当时为什么是这个排序。
-- 所以把当时的权重和漏斗一起存下来。
--
-- filtered_json 存"每条硬规则各排除了多少株"。无候选时前端要能说出
-- "是预算卡掉了 6 株"，而不是干巴巴一句"没有结果"。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `rec_result`;
CREATE TABLE `rec_result` (
  `id`                   BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`              BIGINT       NOT NULL           COMMENT '归属用户 sys_user.id，越权访问按不存在处理',
  `profile_id`           BIGINT       NOT NULL           COMMENT '输入画像 user_scene_profile.id',
  `scene_name_snapshot`  VARCHAR(50)  NOT NULL           COMMENT '当时的场景名。画像改名后历史记录仍显示当时的名字',
  `weight_snapshot`      JSON         NOT NULL           COMMENT '当时的七项权重与 topN，可复现的依据',
  `total_count`          INT          NOT NULL           COMMENT '参与筛选的在售品种总数',
  `candidate_count`      INT          NOT NULL           COMMENT '硬过滤后剩余候选数，0 表示无结果',
  `filtered_json`        JSON         NOT NULL           COMMENT '各条硬规则的排除数，无候选时的诊断依据',
  `created_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  -- 取"我最近一次推荐"要按 user_id 倒序拿第一条，这个索引直接覆盖
  KEY `idx_rec_result_user_time` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐结果快照';

-- ------------------------------------------------------------
-- 11. 推荐条目表
--
-- 一次推荐的 Top-N 明细。score_json 存七项分项得分，前端据此画分项条——
-- 只给一个总分的话，用户没法判断"85 分"是光照好还是价格便宜。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `rec_result_item`;
CREATE TABLE `rec_result_item` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT,
  `result_id`      BIGINT        NOT NULL              COMMENT '所属 rec_result.id',
  `species_id`     BIGINT        NOT NULL              COMMENT '推荐品种 catalog_species.id',
  `sku_id`         BIGINT        NOT NULL              COMMENT '价格最贴合的 SKU catalog_sku.id，点击直达详情页',
  `total_score`    DECIMAL(5,2)  NOT NULL              COMMENT '加权总分 0-100',
  `score_json`     JSON          NOT NULL              COMMENT '七项分项得分，各项均为 0-100',
  `reasons_json`   JSON          NOT NULL              COMMENT '推荐理由文案数组',
  `risk`           VARCHAR(255)      NULL              COMMENT '主要风险提示，得分最低那一维生成',
  `rank_no`        TINYINT       NOT NULL              COMMENT '名次，从 1 起',
  `clicked`        TINYINT       NOT NULL DEFAULT 0    COMMENT '1 用户点进过详情页，第⑦步看板算推荐点击率',
  PRIMARY KEY (`id`),
  KEY `idx_rec_item_result` (`result_id`, `rank_no`),
  -- 看板要按品种统计曝光与点击，这个索引避免全表扫
  KEY `idx_rec_item_species` (`species_id`, `clicked`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐条目（含分项得分与理由）';

-- ------------------------------------------------------------
-- 12. 收货地址表
--
-- 单独一张表而不是往 sys_user 上加几列：一个人有家和公司两个地址是常态，
-- 挂在用户表上就只能存一个。
--
-- 注意订单不存 address_id 而是存地址快照（见 trade_order）：用户删掉地址簿里
-- 那一条之后，历史订单不该变成"寄往未知"。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user_address`;
CREATE TABLE `user_address` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL              COMMENT '所属用户 sys_user.id',
  `receiver`    VARCHAR(30)  NOT NULL              COMMENT '收件人姓名',
  `phone`       VARCHAR(20)  NOT NULL              COMMENT '收件人电话',
  `province`    VARCHAR(30)  NOT NULL              COMMENT '省',
  `city`        VARCHAR(30)  NOT NULL              COMMENT '市',
  `district`    VARCHAR(30)  NOT NULL              COMMENT '区/县',
  `detail`      VARCHAR(120) NOT NULL              COMMENT '详细地址（街道门牌）',
  `is_default`  TINYINT      NOT NULL DEFAULT 0    COMMENT '1 默认地址，每用户至多一个',
  `deleted`     TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除 0未删 1已删',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_address_user_default` (`user_id`, `is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址';

-- ------------------------------------------------------------
-- 13. 购物车表
--
-- 这张表刻意不存价格和商品名。价格必须每次从 catalog_sku 实时读——
-- 存一份快照就等于给了客户端一个可能过期、也可能被篡改的价格来源。
-- 改造前的前端购物车正是把价格存在 localStorage 里的，用户改一下浏览器存储
-- 就能按自己写的价格下单。
--
-- 按 sku_id 而非品种存：同一株琴叶榕 75 厘米款 168 元、110 厘米款 288 元，
-- 按品种存就表达不了"买的是哪个规格"。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `trade_cart`;
CREATE TABLE `trade_cart` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT   NOT NULL                  COMMENT '所属用户 sys_user.id',
  `sku_id`      BIGINT   NOT NULL                  COMMENT '商品 catalog_sku.id',
  `quantity`    INT      NOT NULL DEFAULT 1        COMMENT '数量，最小 1',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  -- 同一个 SKU 在一个人的车里只占一行，再次加购是累加数量而非新增一行。
  -- 这里不做逻辑删除：购物车条目删了就是删了，没有留痕的价值
  UNIQUE KEY `uk_cart_user_sku` (`user_id`, `sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车';

-- ------------------------------------------------------------
-- 14. 订单表
--
-- status 六个取值来自方案原文："订单状态覆盖待付款、待发货、运输中、已完成、
-- 已取消和售后中"。流转规则在 OrderTransition 里，是个可单测的纯函数。
--
-- checkout_idem_key 的唯一索引是"防重复下单"的真正落点；pay_idem_key 是
-- "防重复支付"的落点。两者都由数据库约束兜底。
-- 方案要求"结合幂等标识
-- 避免重复付款"——靠应用层先查再插是有竞态的（两个请求可能同时查到"没付过"），
-- 唯一索引没有这个问题，第二个请求必然撞键。
--
-- 地址存快照而非 address_id：用户删掉地址簿里那条之后，历史订单仍要显示
-- 当时寄到哪里去了。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `trade_order`;
CREATE TABLE `trade_order` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT,
  `order_no`        VARCHAR(32)   NOT NULL              COMMENT '订单号，yyyyMMddHHmmss+6位随机，可读且可排序',
  `user_id`         BIGINT        NOT NULL              COMMENT '下单人 sys_user.id',
  `status`          TINYINT       NOT NULL              COMMENT '0待付款 1待发货 2运输中 3已完成 4已取消 5售后中',
  -- 售后驳回要回到申请之前的状态。按时间戳倒推不可靠，直接记下来
  `prev_status`     TINYINT           NULL              COMMENT '申请售后时的原状态，驳回后回到它',

  `total_amount`    DECIMAL(10,2) NOT NULL              COMMENT '订单总额，下单时算定，不随商品调价变动',
  `item_count`      INT           NOT NULL              COMMENT '商品总件数',

  -- ===== 地址快照 =====
  `receiver`        VARCHAR(30)   NOT NULL              COMMENT '收件人快照',
  `phone`           VARCHAR(20)   NOT NULL              COMMENT '收件电话快照',
  `address_snapshot` VARCHAR(200) NOT NULL              COMMENT '完整地址快照，省市区+详细地址拼好',

  `checkout_idem_key` VARCHAR(64)  NOT NULL              COMMENT '结算请求幂等标识，与用户联合唯一',
  `pay_idem_key`    VARCHAR(64)       NULL              COMMENT '支付幂等标识，唯一索引拦重复付款',
  `paid_at`         DATETIME          NULL,
  `shipped_at`      DATETIME          NULL,
  `received_at`     DATETIME          NULL              COMMENT '确认收货时间，第⑤步据此建养护档案',
  `closed_at`       DATETIME          NULL              COMMENT '取消或售后结束的时间',

  `cancel_reason`   VARCHAR(200)      NULL,
  `refund_reason`   VARCHAR(200)      NULL              COMMENT '用户申请售后填的理由',
  `refund_reject_reason` VARCHAR(200) NULL              COMMENT '管理员驳回时填的理由',

  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_order_user_checkout` (`user_id`, `checkout_idem_key`),
  -- NULL 不参与唯一性判定，所以未支付订单的 NULL 不会互相冲突
  UNIQUE KEY `uk_order_pay_idem` (`pay_idem_key`),
  KEY `idx_order_user_status` (`user_id`, `status`, `created_at`),
  KEY `idx_order_status_time` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

-- ------------------------------------------------------------
-- 15. 订单条目表
--
-- 商品名、规格、单价、图片全部存快照。商品会改名、调价、下架，历史订单
-- 必须显示当时买的到底是什么、花了多少钱——只存 sku_id 的话，管理员明天
-- 调个价，用户上个月的订单金额就跟着变了。
--
-- species_id 冗余存（由 sku_id 也能查出来）：第⑤步"确认收货 → 自动建立
-- 我的植物"是按品种建档的，直接读这一列即可，不必再去关联 catalog_sku——
-- 而那时这个 SKU 可能已经下架甚至删除了。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `trade_order_item`;
CREATE TABLE `trade_order_item` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT,
  `order_id`      BIGINT        NOT NULL            COMMENT '所属 trade_order.id',
  `sku_id`        BIGINT        NOT NULL            COMMENT '下单时的 catalog_sku.id',
  `species_id`    BIGINT        NOT NULL            COMMENT '品种 catalog_species.id，第⑤步建养护档案用',

  -- ===== 商品快照 =====
  `species_name`  VARCHAR(60)   NOT NULL            COMMENT '品种名快照',
  `spec`          VARCHAR(60)   NOT NULL            COMMENT '规格文案快照',
  `image`         VARCHAR(255)  NOT NULL            COMMENT '主图快照',
  `unit_price`    DECIMAL(10,2) NOT NULL            COMMENT '成交单价快照',

  `quantity`      INT           NOT NULL,
  `subtotal`      DECIMAL(10,2) NOT NULL            COMMENT 'unit_price × quantity',
  PRIMARY KEY (`id`),
  KEY `idx_order_item_order` (`order_id`),
  KEY `idx_order_item_species` (`species_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单条目（含商品快照）';

-- ------------------------------------------------------------
-- 16. 养护档案表（"我的植物"）
--
-- 确认收货时自动创建，方案原文："订单确认收货后，系统依据购买的植物品种、
-- 用户场景、当前季节和养护难度自动创建个人养护档案"。
--
-- 植物名与图片存快照：商品会改名、下架、删除，但"我三个月前买的那株琴叶榕"
-- 这件事是既成事实，档案不该跟着商品一起变或一起消失。
--
-- light_level_snapshot 存建档时的场景光照，是"光照变化检测"的基准线：
-- 没有这个基准，就只能知道现在光照是多少，判断不出它变没变过。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `care_archive`;
CREATE TABLE `care_archive` (
  `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`               BIGINT       NOT NULL          COMMENT '所属用户 sys_user.id',

  `species_id`            BIGINT       NOT NULL          COMMENT '品种 catalog_species.id',
  `plant_name`            VARCHAR(60)  NOT NULL          COMMENT '品种名快照',
  `plant_image`           VARCHAR(255)     NULL          COMMENT '主图快照，卡片展示用',

  `order_id`              BIGINT           NULL          COMMENT '来源订单 trade_order.id',
  `order_no`              VARCHAR(32)      NULL          COMMENT '来源订单号快照',

  `scene_id`              BIGINT           NULL          COMMENT '建档时的场景 user_scene_profile.id',
  `light_level_snapshot`  TINYINT          NULL          COMMENT '建档时的光照档位，光照变化检测的基准',

  `adopted_at`            DATETIME     NOT NULL          COMMENT '入手时间，等于确认收货时间',
  -- 连续遗漏计数。方案要求"若用户连续遗漏任务…重新评估后续频率"，
  -- 关键在"连续"：完成一次就清零。现算区分不了连续与累计，只能单独存
  `missed_count`          INT          NOT NULL DEFAULT 0 COMMENT '连续遗漏任务数，≥2 触发调频',
  -- 频率调整系数（百分比）。100 表示按品种原始周期，120 表示间隔拉长两成
  `water_factor`          SMALLINT     NOT NULL DEFAULT 100 COMMENT '浇水间隔调整系数百分比',

  `status`                TINYINT      NOT NULL DEFAULT 1 COMMENT '1 养护中 0 已归档',
  `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  -- 同一订单同一品种只建立一份养护档案；同订单购买不同品种仍可分别建档
  UNIQUE KEY `uk_archive_order_species` (`order_id`, `species_id`),
  KEY `idx_archive_user_status` (`user_id`, `status`),
  -- 每日定时任务要按品种批量补任务，这个索引避免全表扫
  KEY `idx_archive_species` (`species_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='养护档案（我的植物）';

-- ------------------------------------------------------------
-- 17. 养护任务表
--
-- 六类任务对应方案原文点名的六项："生成浇水检查、施肥、换盆、修剪、
-- 转向补光及病虫害预防任务"。生成规则在 CareTaskPlanner 里，
-- 那是个不依赖 Spring、可脱离容器单测的纯函数。
--
-- 任务按时间窗生成（建档时未来 60 天，之后每日定时补足），不是无限生成：
-- 一株浇水间隔 5 天的植物一年就是 73 条浇水任务，全量生成既浪费又让日历没法看。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `care_task`;
CREATE TABLE `care_task` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `archive_id`    BIGINT       NOT NULL              COMMENT '所属 care_archive.id',
  `task_type`     VARCHAR(16)  NOT NULL              COMMENT 'water 浇水 / fertilize 施肥 / repot 换盆 / prune 修剪 / rotate 转向补光 / pest 病虫害预防',
  `title`         VARCHAR(60)  NOT NULL              COMMENT '任务标题，如"检查盆土并浇水"',
  `instruction`   VARCHAR(255) NOT NULL              COMMENT '操作方法、用量或注意事项',
  `due_date`      DATE         NOT NULL              COMMENT '建议日期',
  `planned_date`  DATE         NOT NULL              COMMENT '计划实例日期，延后时保持不变',
  `status`        TINYINT      NOT NULL DEFAULT 0    COMMENT '0 待办 1 已完成 2 已跳过 3 已逾期',
  `completed_at`  DATETIME         NULL,
  `note`          VARCHAR(255)     NULL              COMMENT '完成时附的文字记录',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  -- 任务生成允许多实例并发，但同一计划实例只能有一条；延后不改变 planned_date
  UNIQUE KEY `uk_task_archive_type_planned` (`archive_id`, `task_type`, `planned_date`),
  -- 日历视图按档案 + 日期范围查，这个索引直接覆盖
  KEY `idx_task_archive_due` (`archive_id`, `due_date`),
  -- 每日定时任务要捞"今天到期的"与"已逾期的"，按日期 + 状态查
  KEY `idx_task_due_status` (`due_date`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='养护任务';

-- ------------------------------------------------------------
-- 18. 成长记录表（时间线）
--
-- 方案原文："用户可标记完成、延后、跳过并上传文字或图片记录"、
-- "成长时间线保存植物状态"。
--
-- 图片以 base64 存 MEDIUMTEXT。方案里写的对象存储本项目没有搭，
-- 而"能拍张照记录植物长势"是这个模块的直观价值所在，用 base64 落库
-- 是当前基础设施下唯一能跑通的做法。单张限 300KB，由应用层校验——
-- MEDIUMTEXT 上限 16MB，真放几十张高清图数据库会很难看。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `care_note`;
CREATE TABLE `care_note` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `archive_id`  BIGINT      NOT NULL               COMMENT '所属 care_archive.id',
  `note_type`   VARCHAR(16) NOT NULL DEFAULT 'text' COMMENT 'text 文字 / photo 带图 / health 健康反馈（叶片发黄等）',
  `content`     VARCHAR(500)    NULL               COMMENT '文字内容',
  `image`       MEDIUMTEXT      NULL               COMMENT 'base64 图片，单张限 300KB，由应用层校验',
  `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_note_archive_time` (`archive_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成长记录（时间线）';

-- ------------------------------------------------------------
-- 19. 站内提醒表
--
-- 方案原文："系统通过站内消息按时提醒"。权限点 care:reminder:manage-own
-- 早就预埋好了，这张表是它的落地。
--
-- 不做已读软删除：提醒读完就是读完，留着 read_flag 供列表排序即可。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `care_notification`;
CREATE TABLE `care_notification` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL              COMMENT '接收人 sys_user.id',
  `type`        VARCHAR(16)  NOT NULL              COMMENT 'task_due 任务到期 / overdue 逾期 / health 健康反馈 / re_eval 频率调整',
  `title`       VARCHAR(60)  NOT NULL,
  `content`     VARCHAR(255) NOT NULL,
  `archive_id`  BIGINT           NULL              COMMENT '关联档案，点击跳转用',
  `task_id`     BIGINT           NULL              COMMENT '关联任务，点击跳转用',
  `read_flag`   TINYINT      NOT NULL DEFAULT 0    COMMENT '1 已读',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  -- 同一任务的同类提醒只发送一次；档案建立等 task_id 为空的通知不受此约束影响
  UNIQUE KEY `uk_notification_task_type` (`task_id`, `type`),
  -- 铃铛要数未读、列表要未读优先，这个索引两件事都覆盖
  KEY `idx_notification_user_read` (`user_id`, `read_flag`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内提醒';

-- ------------------------------------------------------------
-- 20. 知识文章表
--
-- 方案要求详情"采用步骤化说明，突出适用条件、操作频次、常见误区与风险提示"——
-- 这描述的是结构而不是排版，所以四项各占一列，而不是塞进一段富文本。
-- 好处有三：不必引入 markdown 库与随之而来的 XSS 面；强制作者把四项填齐
-- （写成一大段时"常见误区"最容易被省略）；与 catalog_species.care_tips
-- 已有的 JSON 数组列约定一致。
--
-- related_task_types 是三处跳转的统一机制：养护任务按 task_type 匹配、
-- 商品详情按 related_species 匹配、推荐理由按短板维度映射到 task type。
-- 一套字段解决三个入口，不必为每个入口各开一张关联表。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `knowledge_article`;
CREATE TABLE `knowledge_article` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `slug`         VARCHAR(60)  NOT NULL              COMMENT '对外标识，详情页路由用的就是它',
  `title`        VARCHAR(80)  NOT NULL,
  `summary`      VARCHAR(200) NOT NULL              COMMENT '一句话摘要，列表卡片展示',
  `cover`        VARCHAR(255)     NULL              COMMENT '封面图，可空',

  `category`     VARCHAR(20)  NOT NULL              COMMENT 'watering 浇水 / light 光照 / feeding 施肥 / repotting 换盆 / pruning 修剪 / medium 基质花盆 / season 季节养护 / pet-safety 宠物安全 / pest 病虫害 / bloom 花期管理',
  `difficulty`   TINYINT      NOT NULL DEFAULT 1    COMMENT '1 入门 / 2 进阶 / 3 专业',
  `seasons`      JSON             NULL              COMMENT '适用季节 ["spring","summer"]，空表示四季通用',
  `tags`         JSON             NULL              COMMENT '自由标签，检索用。沿用 match_tags 的做法',

  -- ===== 方案点名的步骤化四件套 =====
  `applicable`   VARCHAR(300) NOT NULL              COMMENT '适用条件：什么情况下该看这篇',
  `frequency`    VARCHAR(300) NOT NULL              COMMENT '操作频次：多久做一次',
  `steps`        JSON         NOT NULL              COMMENT '步骤数组 [{"title":..,"detail":..}]',
  `mistakes`     JSON             NULL              COMMENT '常见误区数组',
  `risks`        JSON             NULL              COMMENT '风险提示数组',

  -- ===== 关联：三处跳转与个性化推荐的依据 =====
  `related_task_types` JSON       NULL              COMMENT '关联的养护任务类型，与 care_task.task_type 同域',
  `related_species`    JSON       NULL              COMMENT '专门讲某几个品种时填其 code，空表示通用文章',

  `status`       TINYINT      NOT NULL DEFAULT 0    COMMENT '0 草稿 / 1 待审核 / 2 已发布 / 3 已下架',
  `author_id`    BIGINT           NULL              COMMENT '作者 sys_user.id',
  `published_at` DATETIME         NULL,
  -- 冗余计数：看板统计与热门排序直接读，不必每次聚合 knowledge_feedback
  `view_count`   INT          NOT NULL DEFAULT 0,
  `useful_count` INT          NOT NULL DEFAULT 0,

  `sort`         INT          NOT NULL DEFAULT 0    COMMENT '展示排序，值大者靠前',
  `deleted`      TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除 0未删 1已删',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  -- 物理唯一索引，不认 deleted 标记。删除时由 Service 给 slug 加后缀腾位，
  -- 做法与 catalog 的 code、画像的 scene_name 一致
  UNIQUE KEY `uk_article_slug` (`slug`),
  KEY `idx_article_status_sort` (`status`, `sort`, `published_at`),
  KEY `idx_article_category` (`category`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识文章';

-- ------------------------------------------------------------
-- 21. 收藏与有用性反馈表
--
-- 收藏与"有用"两种行为共用一张表，靠 type 区分：两者的数据结构完全相同，
-- 拆两张表只会让"我对这篇做过什么"要查两次。
--
-- 唯一键是关键：没有它，用户连点两次"有用"就会让计数翻倍。
-- 应用层的"查一下有没有点过"在并发下拦不住，唯一索引能。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `knowledge_feedback`;
CREATE TABLE `knowledge_feedback` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT      NOT NULL               COMMENT '用户 sys_user.id',
  `article_id`  BIGINT      NOT NULL               COMMENT '文章 knowledge_article.id',
  `type`        VARCHAR(10) NOT NULL               COMMENT 'useful 有用 / favorite 收藏',
  `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_feedback_user_article_type` (`user_id`, `article_id`, `type`),
  -- 我的收藏列表按用户 + 类型倒序捞
  KEY `idx_feedback_user_type` (`user_id`, `type`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏与有用性反馈';

-- ------------------------------------------------------------
-- 22. 无结果搜索词表
--
-- 方案原文："高频搜索但无结果的关键词会进入后台内容需求清单。"
-- 这张表就是那份清单——它把"用户想知道什么而我们没写"变成可排序的待办，
-- 比运营凭感觉选题可靠得多。
--
-- keyword 唯一 + hit_count 累加，而不是每次搜索插一行：清单要看的是
-- "哪个词被搜得最多"，存流水还得再聚合一次。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `knowledge_search_miss`;
CREATE TABLE `knowledge_search_miss` (
  `id`                BIGINT      NOT NULL AUTO_INCREMENT,
  `keyword`           VARCHAR(60) NOT NULL           COMMENT '搜索词，已 trim',
  `hit_count`         INT         NOT NULL DEFAULT 1 COMMENT '被搜索次数，越高越该补内容',
  `last_searched_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_search_miss_keyword` (`keyword`),
  KEY `idx_search_miss_hit` (`hit_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='无结果搜索词（内容需求清单）';

-- ------------------------------------------------------------
-- 23. 管理操作日志表
--
-- 只记录关键管理写操作的 before/after，不采用全局 AOP 自动序列化所有响应：
-- 响应里可能有密码、地址或图片，自动切面容易把敏感数据带进日志。
-- 日志是审计凭证，不能依赖业务表的逻辑删除。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
  `id`                 BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`            BIGINT            NULL       COMMENT '操作人 sys_user.id，用户被删后仍保留',
  `username_snapshot`  VARCHAR(50)       NULL       COMMENT '操作人用户名快照',
  `module`             VARCHAR(30)   NOT NULL       COMMENT 'catalog/trade/knowledge/recommendation/user/care',
  `action`             VARCHAR(30)   NOT NULL       COMMENT 'CREATE/UPDATE/DELETE/PUBLISH/SHIP/REFUND/CONFIG/PREVIEW',
  `target_type`        VARCHAR(40)       NULL       COMMENT '操作对象类型',
  `target_id`          VARCHAR(64)       NULL       COMMENT '操作对象标识',
  `before_json`        JSON              NULL       COMMENT '修改前快照，不存密码/地址等敏感字段',
  `after_json`         JSON              NULL       COMMENT '修改后快照，不存密码/地址等敏感字段',
  `ip`                 VARCHAR(45)       NULL       COMMENT 'IPv4/IPv6',
  `user_agent`         VARCHAR(255)      NULL,
  `created_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operation_module_action_time` (`module`, `action`, `created_at`),
  KEY `idx_operation_user_time` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理操作日志';

-- ------------------------------------------------------------
-- 24. 页面访问日志表
--
-- 只记页面类型与时间，不记录完整请求 body，避免把地址、手机号等个人数据带进
-- 分析表。游客 user_id 可以为空，访客量也要能进看板。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `operation_visit_log`;
CREATE TABLE `operation_visit_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT           NULL            COMMENT '游客为空',
  `path`        VARCHAR(255) NOT NULL,
  `page_type`   VARCHAR(40)  NOT NULL            COMMENT 'home/catalog/recommendation/knowledge/care/trade/admin',
  `referrer`    VARCHAR(255)     NULL,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_visit_page_time` (`page_type`, `created_at`),
  KEY `idx_visit_user_time` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='页面访问日志';

SET FOREIGN_KEY_CHECKS = 1;
