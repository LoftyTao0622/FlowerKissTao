-- ============================================================
-- FlowerKissTao RBAC 初始化数据
-- 前置：已执行 schema.sql
-- 幂等：全部使用 INSERT IGNORE，可重复执行
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 角色
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_role` (`code`, `name`, `description`, `sort`) VALUES
  ('ROLE_USER',     '普通用户', '注册即得，可浏览选购、获取推荐、管理自己的订单与养护计划', 1),
  ('ROLE_OPERATOR', '运营',     '管理商品、知识内容与首页编排，处理订单发货退款，不可管理账号与角色', 2),
  ('ROLE_ADMIN',    '管理员',   '全部权限，包括账号封禁与角色分配', 3);

-- ------------------------------------------------------------
-- 权限点，code 形如 模块:资源:动作
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_permission` (`code`, `name`, `module`) VALUES
  -- catalog 商品
  ('catalog:plant:read',        '查看植物商品',   'catalog'),
  ('catalog:plant:create',      '新建植物商品',   'catalog'),
  ('catalog:plant:update',      '编辑植物商品',   'catalog'),
  ('catalog:plant:delete',      '删除植物商品',   'catalog'),
  ('catalog:plant:publish',     '上下架植物商品', 'catalog'),
  -- trade 交易
  ('trade:order:read-own',      '查看自己的订单', 'trade'),
  ('trade:order:create',        '下单',           'trade'),
  ('trade:order:cancel-own',    '取消自己的订单', 'trade'),
  ('trade:order:read-all',      '查看全部订单',   'trade'),
  ('trade:order:ship',          '订单发货',       'trade'),
  ('trade:order:refund',        '订单退款',       'trade'),
  -- recommendation 推荐
  ('recommendation:result:generate', '生成环境适配推荐', 'recommendation'),
  ('recommendation:result:read-own', '查看自己的推荐',   'recommendation'),
  ('recommendation:rule:manage',     '调整推荐规则参数', 'recommendation'),
  -- care 养护
  ('care:plan:manage-own',      '管理自己的养护计划', 'care'),
  ('care:reminder:manage-own',  '管理自己的养护提醒', 'care'),
  ('care:template:manage',      '维护养护方案模板',   'care'),
  -- knowledge 知识
  ('knowledge:article:read',    '阅读养护知识',   'knowledge'),
  ('knowledge:article:write',   '撰写养护知识',   'knowledge'),
  ('knowledge:article:publish', '发布养护知识',   'knowledge'),
  -- operation 运营位
  ('operation:banner:edit',     '编辑首页推荐位', 'operation'),
  ('operation:page:arrange',    '编排首页版块',   'operation'),
  -- user 账号与权限
  ('user:profile:read-own',     '查看个人资料',   'user'),
  ('user:profile:update-own',   '修改个人资料',   'user'),
  ('user:account:read',         '查看用户列表',   'user'),
  ('user:account:ban',          '封禁解封账号',   'user'),
  ('user:role:assign',          '分配用户角色',   'user');

-- ------------------------------------------------------------
-- 角色-权限绑定
-- 用子查询按 code 关联，不写死自增 ID
-- ------------------------------------------------------------

-- 普通用户：只读商品与知识，外加一切"自己的"东西
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `sys_role` r, `sys_permission` p
WHERE r.code = 'ROLE_USER' AND p.code IN (
  'catalog:plant:read',
  'trade:order:read-own', 'trade:order:create', 'trade:order:cancel-own',
  'recommendation:result:generate', 'recommendation:result:read-own',
  'care:plan:manage-own', 'care:reminder:manage-own',
  'knowledge:article:read',
  'user:profile:read-own', 'user:profile:update-own'
);

-- 运营：内容与订单运营的全部动作，但碰不到 user:account:* 与 user:role:*
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `sys_role` r, `sys_permission` p
WHERE r.code = 'ROLE_OPERATOR' AND p.code IN (
  'catalog:plant:read', 'catalog:plant:create', 'catalog:plant:update',
  'catalog:plant:delete', 'catalog:plant:publish',
  'trade:order:read-all', 'trade:order:ship', 'trade:order:refund',
  'recommendation:rule:manage',
  'care:template:manage',
  'knowledge:article:read', 'knowledge:article:write', 'knowledge:article:publish',
  'operation:banner:edit', 'operation:page:arrange',
  'user:profile:read-own', 'user:profile:update-own'
);

-- 管理员：全部权限点
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `sys_role` r, `sys_permission` p
WHERE r.code = 'ROLE_ADMIN';

-- ------------------------------------------------------------
-- 初始管理员账号
-- 用户名 admin  密码 admin123
-- 该哈希为 BCrypt cost=10，仅供本地开发，上线前必须改密
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_user` (`username`, `password`, `nickname`, `status`) VALUES
  ('admin', '$2a$10$MCu1lxFGTXFzUAMBad8bs.MKZU/OPu96DbTEc2lwc5VO5Bns.AFmi', '超级管理员', 1);

INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.id, r.id FROM `sys_user` u, `sys_role` r
WHERE u.username = 'admin' AND r.code = 'ROLE_ADMIN';

-- ------------------------------------------------------------
-- 植物商品初始数据
-- 文案逐字取自前端原先硬编码的 frontend/src/modules/catalog/data/plants.ts
-- 幂等依赖 uk_plant_slug，不写死自增 id
-- sort 取 40/30/20/10，保证列表顺序与迁移前一致
-- ------------------------------------------------------------
INSERT IGNORE INTO `catalog_plant`
  (`slug`, `name`, `latin_name`, `price`, `image`, `image_alt`, `category`, `light`,
   `watering`, `size`, `pet_friendly`, `pet_note`, `difficulty`, `match_tags`,
   `recommendation_reason`, `short_description`, `description`, `care_tips`,
   `featured`, `status`, `sort`)
VALUES
  ('fiddle-leaf-fig', '琴叶榕', 'Ficus lyrata', 168.00,
   '/images/fiddle-leaf-fig.webp', '陶盆中舒展宽大叶片的琴叶榕',
   '大型绿植', '明亮散射光', '土表 3–5 厘米干后浇透', '中大型 · 约 75 厘米',
   0, '宠物需隔离', '需要关注',
   '["客厅焦点","南向采光","净化空间感"]',
   '宽阔叶片能快速建立客厅的视觉中心，适合采光稳定、愿意定期观察盆土的空间。',
   '轮廓利落、叶片舒展，用一株植物建立安静而有力量的客厅焦点。',
   '琴叶榕拥有提琴形的大叶片与挺拔株形，适合放在客厅窗边或工作室明亮区域。避免频繁挪动，并让叶片远离正午直射光。',
   '["每两周转盆四分之一圈，让株形均匀受光","叶片积灰时用微湿软布轻拭","冬季降低浇水频率并避开冷风"]',
   1, 1, 40),

  ('asparagus-fern', '文竹', 'Asparagus setaceus', 49.00,
   '/images/asparagus-fern.webp', '细密轻盈叶片的桌面文竹',
   '桌面绿植', '柔和散射光', '土表微干时补水', '小型 · 约 30 厘米',
   0, '宠物需隔离', '需要关注',
   '["书桌陪伴","柔和光线","轻盈株型"]',
   '细密枝叶不会压迫桌面，柔和散射光环境下就能维持轻盈、舒展的姿态。',
   '羽毛般的层叠枝叶，为书桌和窗台留下一片轻盈的绿色。',
   '文竹株形轻巧，适合书房、卧室窗边等小尺度空间。它喜欢稳定湿度与通风环境，空气干燥时可在植株周围适当增湿。',
   '["避开空调和暖气直吹","盆土保持微润但不要长期积水","发现黄叶后及时检查光照与空气湿度"]',
   0, 1, 30),

  ('alocasia-black-velvet', '黑天鹅海芋', 'Alocasia reginula', 88.00,
   '/images/alocasia-black-velvet.webp', '深色绒面叶片和银白叶脉的黑天鹅海芋',
   '特色观叶', '柔和散射光', '土表 2 厘米干后补水', '小型 · 约 25 厘米',
   0, '宠物需隔离', '进阶养护',
   '["收藏观叶","精致小空间","稳定湿度"]',
   '深色绒面叶片和清晰银脉具有很高辨识度，适合想为小空间加入精致层次的养植者。',
   '墨绿色绒面与银白叶脉交织，是近距离欣赏也耐看的收藏型观叶植物。',
   '黑天鹅海芋的叶色深沉、质感细腻，适合放在有柔和光线的边桌或植物架。它偏爱温暖和较高空气湿度，但根系不耐积水。',
   '["使用疏松、排水良好的介质","保持环境温暖并避免叶面积水","生长期薄肥少施，休眠期暂停施肥"]',
   1, 1, 20),

  ('snake-plant', '金边虎尾兰', 'Dracaena trifasciata', 69.00,
   '/images/snake-plant.webp', '叶片挺拔且带有金色叶缘的虎尾兰',
   '耐阴绿植', '低至中等光照', '盆土完全干透后浇透', '中型 · 约 50 厘米',
   0, '宠物需隔离', '新手友好',
   '["新手入门","低频浇水","卧室角落"]',
   '耐受较弱光线和短期忘记浇水，直立株型占地小，很适合第一次认真养植物的人。',
   '挺拔叶片和低频养护需求，让光线有限的小空间也能拥有秩序感。',
   '金边虎尾兰耐旱且适应力强，可以放在卧室、玄关或办公室。更明亮的散射光有助于维持清晰叶纹，浇水前务必确认盆土已经干透。',
   '["宁干勿湿，托盘积水要及时倒掉","每月擦拭叶片以保持清洁","低温季节延长两次浇水的间隔"]',
   1, 1, 10);
