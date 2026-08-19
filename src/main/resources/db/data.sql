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
  -- operation 运营
  ('operation:banner:edit',     '编辑首页推荐位', 'operation'),
  ('operation:page:arrange',    '编排首页版块',   'operation'),
  ('operation:dashboard:read',  '查看运营看板',   'operation'),
  ('operation:log:read',        '查看操作日志',   'operation'),
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
  'operation:dashboard:read', 'operation:log:read',
  'user:profile:read-own', 'user:profile:update-own'
);

-- 管理员：全部权限点
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `sys_role` r, `sys_permission` p
WHERE r.code = 'ROLE_ADMIN';

-- ------------------------------------------------------------
-- 初始账号，三个角色各一个
--
--   admin    / admin123     管理员，全部 27 个权限点
--   operator / operator123  运营，管得了商品内容订单，碰不到账号与角色
--   demo     / user123      普通用户，只读商品知识 + 一切"自己的"东西
--
-- 三个都建出来是为了能实际演示权限差异：只有 admin 时，
-- "运营看不到用户管理"这类约束在界面上无从验证。
-- 哈希为 BCrypt cost=10，仅供本地开发，上线前必须改密。
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_user` (`username`, `password`, `nickname`, `status`) VALUES
  ('admin',    '$2a$10$MCu1lxFGTXFzUAMBad8bs.MKZU/OPu96DbTEc2lwc5VO5Bns.AFmi', '超级管理员', 1),
  ('operator', '$2a$10$VmKCByIRP6a.d4bKic2YmeHmxF1Bkx7wbx9tQR0R97MQZz/dJdpXm', '运营小艾',   1),
  ('demo',     '$2a$10$ov7UrmyXpJjt/hiRusoYluylDWHk8V6HlHiiKcNX4.KENFw0QMDle', '体验用户',   1);

-- 用户名与角色 code 配对后一次性绑定，新增账号只需在上面的 VALUES 和这里各加一行
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.id, r.id FROM `sys_user` u
JOIN `sys_role` r ON (u.username, r.code) IN (
  ('admin',    'ROLE_ADMIN'),
  ('operator', 'ROLE_OPERATOR'),
  ('demo',     'ROLE_USER')
);

-- ------------------------------------------------------------
-- 植物品种初始数据
--
-- 原有 4 株（琴叶榕、文竹、黑天鹅海芋、金边虎尾兰）的展示文案逐字保留，
-- 只把原先塞在字符串里的条件拆成可计算的数值列。
--
-- 另外 14 株用于让推荐算法的硬过滤有候选可选：只有 4 株且全部对宠物有毒时，
-- 任何"家里有猫"的画像都会返回空结果，算法看着像坏了。
--
-- 光照等级 1 低光 / 2 柔和散射 / 3 明亮散射 / 4 充足直射
-- 养护难度 1 新手友好 / 2 需要关注 / 3 进阶养护
--
-- 幂等依赖 uk_species_code，不写死自增 id
-- ------------------------------------------------------------
INSERT IGNORE INTO `catalog_species`
  (`code`, `name`, `latin_name`, `category`, `ornamental_type`,
   `bloom_season`, `bloom_color`, `fragrance`,
   `light_min`, `light_max`, `light_note`,
   `temp_min`, `temp_max`, `humidity_min`, `humidity_max`,
   `water_interval_days`, `water_note`, `fertilize_interval_days`, `repot_interval_months`,
   `prune_needed`, `care_level`,
   `toxic_cat`, `toxic_dog`, `toxic_child`, `pollen_risk`,
   `mature_height_cm`, `footprint_cm`,
   `match_tags`, `recommendation_reason`, `short_description`, `description`, `care_tips`,
   `status`, `sort`)
VALUES
  -- ===== 原有 4 株：展示文案原样保留 =====
  ('fiddle-leaf-fig', '琴叶榕', 'Ficus lyrata', '大型绿植', 'leaf',
   NULL, NULL, NULL,
   3, 4, '明亮散射光',
   15, 30, 40, 60,
   10, '土表 3–5 厘米干后浇透', 30, 24,
   1, 2,
   1, 1, 1, 0,
   180, 90,
   '["客厅焦点","南向采光","净化空间感"]',
   '宽阔叶片能快速建立客厅的视觉中心，适合采光稳定、愿意定期观察盆土的空间。',
   '轮廓利落、叶片舒展，用一株植物建立安静而有力量的客厅焦点。',
   '琴叶榕拥有提琴形的大叶片与挺拔株形，适合放在客厅窗边或工作室明亮区域。避免频繁挪动，并让叶片远离正午直射光。',
   '["每两周转盆四分之一圈，让株形均匀受光","叶片积灰时用微湿软布轻拭","冬季降低浇水频率并避开冷风"]',
   1, 400),

  ('asparagus-fern', '文竹', 'Asparagus setaceus', '桌面绿植', 'leaf',
   NULL, NULL, NULL,
   2, 3, '柔和散射光',
   10, 26, 50, 70,
   5, '土表微干时补水', 30, 18,
   1, 2,
   1, 1, 1, 0,
   40, 30,
   '["书桌陪伴","柔和光线","轻盈株型"]',
   '细密枝叶不会压迫桌面，柔和散射光环境下就能维持轻盈、舒展的姿态。',
   '羽毛般的层叠枝叶，为书桌和窗台留下一片轻盈的绿色。',
   '文竹株形轻巧，适合书房、卧室窗边等小尺度空间。它喜欢稳定湿度与通风环境，空气干燥时可在植株周围适当增湿。',
   '["避开空调和暖气直吹","盆土保持微润但不要长期积水","发现黄叶后及时检查光照与空气湿度"]',
   1, 300),

  ('alocasia-black-velvet', '黑天鹅海芋', 'Alocasia reginula', '特色观叶', 'leaf',
   NULL, NULL, NULL,
   2, 3, '柔和散射光',
   18, 28, 60, 80,
   6, '土表 2 厘米干后补水', 21, 12,
   0, 3,
   1, 1, 1, 0,
   35, 30,
   '["收藏观叶","精致小空间","稳定湿度"]',
   '深色绒面叶片和清晰银脉具有很高辨识度，适合想为小空间加入精致层次的养植者。',
   '墨绿色绒面与银白叶脉交织，是近距离欣赏也耐看的收藏型观叶植物。',
   '黑天鹅海芋的叶色深沉、质感细腻，适合放在有柔和光线的边桌或植物架。它偏爱温暖和较高空气湿度，但根系不耐积水。',
   '["使用疏松、排水良好的介质","保持环境温暖并避免叶面积水","生长期薄肥少施，休眠期暂停施肥"]',
   1, 200),

  ('snake-plant', '金边虎尾兰', 'Dracaena trifasciata', '耐阴绿植', 'leaf',
   NULL, NULL, NULL,
   1, 4, '低至中等光照',
   10, 32, 30, 60,
   21, '盆土完全干透后浇透', 60, 36,
   0, 1,
   1, 1, 1, 0,
   70, 30,
   '["新手入门","低频浇水","卧室角落"]',
   '耐受较弱光线和短期忘记浇水，直立株型占地小，很适合第一次认真养植物的人。',
   '挺拔叶片和低频养护需求，让光线有限的小空间也能拥有秩序感。',
   '金边虎尾兰耐旱且适应力强，可以放在卧室、玄关或办公室。更明亮的散射光有助于维持清晰叶纹，浇水前务必确认盆土已经干透。',
   '["宁干勿湿，托盘积水要及时倒掉","每月擦拭叶片以保持清洁","低温季节延长两次浇水的间隔"]',
   1, 100),

  -- ===== 宠物友好品种：让"家里有猫狗"的画像有候选可选 =====
  ('spider-plant', '金边吊兰', 'Chlorophytum comosum', '桌面绿植', 'leaf',
   NULL, NULL, NULL,
   2, 4, '柔和至明亮散射光',
   10, 30, 40, 70,
   7, '土表干透后浇透', 45, 18,
   0, 1,
   0, 0, 0, 0,
   35, 40,
   '["宠物友好","新手入门","悬挂垂吊"]',
   '猫狗误食也不会中毒，耐受光线波动，抽生的走茎可以繁殖新株，容错度很高。',
   '垂落的走茎和明快的金边，是最容易养活的宠物友好绿植之一。',
   '金边吊兰适应力极强，明亮散射光下叶缘金边最清晰，弱光环境也能存活。走茎上长出的小株可以直接分盆，很适合作为第一株练手植物。',
   '["叶尖发褐多与水质或空气干燥有关，可改用静置过的水","走茎小株生根后即可分盆","春夏生长期每月施一次薄肥"]',
   1, 380),

  ('parlor-palm', '袖珍椰子', 'Chamaedorea elegans', '耐阴绿植', 'leaf',
   NULL, NULL, NULL,
   1, 3, '低至中等光照',
   13, 27, 50, 70,
   8, '土表 2 厘米干后补水', 45, 24,
   0, 1,
   0, 0, 0, 0,
   120, 60,
   '["宠物友好","耐阴角落","热带气息"]',
   '对猫狗无毒，能在远离窗口的位置正常生长，是光线不足房间里少见的落地选择。',
   '细密羽叶带来热带感，却不挑光线，适合放在客厅或办公室的暗角。',
   '袖珍椰子生长缓慢、株型紧凑，在低光环境下依然保持深绿。它怕强烈直射光，夏季应避开西晒窗口，空气干燥时可适度喷雾。',
   '["避免正午直射光灼伤叶尖","盆土保持微润，忌长期积水","每季度清理一次枯黄下叶"]',
   1, 360),

  ('boston-fern', '波士顿蕨', 'Nephrolepis exaltata', '桌面绿植', 'leaf',
   NULL, NULL, NULL,
   2, 3, '柔和散射光',
   15, 26, 60, 80,
   4, '保持盆土持续微润', 30, 18,
   0, 2,
   0, 0, 0, 0,
   60, 70,
   '["宠物友好","浴室窗台","高湿环境"]',
   '对猫狗无毒，喜欢较高空气湿度，浴室和厨房窗台这类高湿位置反而更适合它。',
   '层层舒展的羽状叶，为潮湿而柔光的角落带来蓬松的绿意。',
   '波士顿蕨对空气湿度要求较高，是少数适合放在浴室的观叶植物。它不耐干旱，盆土一旦完全干透，叶片就会大量枯黄且难以恢复。',
   '["盆土切忌完全干透，夏季需每日检查","空气干燥时每天喷雾或使用加湿托盘","枯黄叶片贴基部剪除以促发新叶"]',
   1, 340),

  ('hoya-carnosa', '球兰', 'Hoya carnosa', '特色观叶', 'flower',
   '春末至夏季', '粉白色', '夜间浓香',
   3, 4, '明亮散射光',
   15, 30, 40, 60,
   14, '盆土干透后浇透', 45, 36,
   0, 1,
   0, 0, 0, 1,
   150, 45,
   '["宠物友好","观花观叶","耐旱省心"]',
   '厚实叶片储水耐旱，对猫狗无毒，光线充足时会开出带浓香的伞状花序。',
   '蜡质厚叶配上星形花簇，是能开花又不娇气的攀援植物。',
   '球兰叶片肥厚多肉，耐旱程度接近多肉植物。明亮散射光下更容易开花，花后不要剪掉花梗——来年会在同一位置再次开花。',
   '["花梗开花后保留，明年会在原处复花","盆土宁干勿湿，冬季进一步减少浇水","花期夜间香气浓郁，卧室摆放需考虑接受度"]',
   1, 320),

  ('calathea-orbifolia', '青苹果竹芋', 'Goeppertia orbifolia', '特色观叶', 'leaf',
   NULL, NULL, NULL,
   1, 2, '柔和弱光',
   18, 28, 60, 85,
   5, '保持微润，忌完全干透', 30, 18,
   0, 3,
   0, 0, 0, 0,
   80, 70,
   '["宠物友好","耐阴观叶","银绿条纹"]',
   '对猫狗无毒且耐受弱光，宽大叶片上的银绿条纹在暗角也很醒目，但需要稳定湿度。',
   '宽阔圆叶上的银绿条纹，为柔光角落带来克制而精致的图案。',
   '青苹果竹芋叶片会随昼夜开合，是观赏性很强的竹芋科植物。它对水质和空气湿度敏感，叶缘焦枯通常是空气太干或水中矿物质过多所致。',
   '["使用静置过的水或纯净水，减少叶缘焦边","远离空调出风口，保持较高空气湿度","避免直射光，弱光反而更利于叶片图案"]',
   1, 180),

  ('areca-palm', '散尾葵', 'Dypsis lutescens', '大型绿植', 'leaf',
   NULL, NULL, NULL,
   3, 4, '明亮散射光',
   16, 30, 40, 70,
   7, '土表 3 厘米干后浇透', 30, 24,
   0, 2,
   0, 0, 0, 0,
   200, 120,
   '["宠物友好","客厅落地","舒展株型"]',
   '对猫狗无毒的落地大株，羽状叶层次舒展，适合采光充足且能预留冠幅空间的客厅。',
   '成丛的羽状叶向外舒展，用一株植物撑起整面墙的绿意。',
   '散尾葵是常见的宠物友好大型绿植，成株冠幅可达 120 厘米，购买前需为叶片预留舒展空间。它喜欢明亮散射光与稳定水分，长期干旱会导致下部叶片枯黄。',
   '["为冠幅预留至少 60 厘米的舒展空间","定期转盆，避免单侧受光造成株型偏斜","叶尖发褐多因空气干燥，可适度增湿"]',
   1, 160),

  ('peperomia-obtusifolia', '圆叶椒草', 'Peperomia obtusifolia', '桌面绿植', 'leaf',
   NULL, NULL, NULL,
   1, 3, '低至中等光照',
   16, 28, 40, 60,
   12, '盆土干透后浇透', 45, 24,
   0, 1,
   0, 0, 0, 0,
   25, 25,
   '["宠物友好","桌面小株","低频浇水"]',
   '厚叶储水耐旱，对猫狗无毒，弱光的书桌角落也能维持紧凑株型，忘记浇水不易出问题。',
   '肉质圆叶紧凑成丛，是办公桌上最省心的一抹绿。',
   '圆叶椒草叶片肥厚，具有一定储水能力，浇水频率可以放得很宽。它能耐受远离窗口的柔和弱光，过强的直射光反而会让叶色发白。',
   '["宁干勿湿，盆土干透再浇","冬季温度低于 15 度时进一步减少浇水","叶片积灰时用软布轻拭即可"]',
   1, 300),

  ('haworthia-fasciata', '条纹十二卷', 'Haworthiopsis fasciata', '桌面绿植', 'leaf',
   NULL, NULL, NULL,
   3, 4, '明亮散射光',
   5, 35, 20, 45,
   20, '盆土完全干透后少量给水', 90, 36,
   0, 1,
   0, 0, 0, 0,
   12, 12,
   '["宠物友好","经常出差","极低频浇水"]',
   '多肉质地能承受长时间缺水，对猫狗无毒，株型只有掌心大小，很适合经常出差的人。',
   '深绿叶片上的白色横纹整齐排列，几周不管也依然挺立。',
   '条纹十二卷是最耐旱的室内多肉之一，两三周浇一次水即可。它需要明亮光线维持紧凑株型，光照不足时叶片会徒长变形。',
   '["浇水前务必确认盆土完全干透","使用颗粒比例较高的多肉专用土","徒长通常意味着光照不足，需移到更亮的位置"]',
   1, 280),

  -- ===== 有毒但常见的品种：硬过滤需要能把它们排除掉 =====
  ('monstera-deliciosa', '龟背竹', 'Monstera deliciosa', '大型绿植', 'leaf',
   NULL, NULL, NULL,
   2, 3, '柔和至明亮散射光',
   16, 30, 50, 70,
   9, '土表 3 厘米干后浇透', 30, 24,
   1, 2,
   1, 1, 1, 0,
   200, 100,
   '["客厅焦点","裂叶造型","攀援生长"]',
   '标志性的裂叶在散射光下就能形成，株型舒展有力，适合有一定高度和宽度的客厅角落。',
   '叶片自带的天然裂口与孔洞，让空间瞬间有了度假感。',
   '龟背竹是最具辨识度的室内大型观叶植物，成熟叶片会自然形成裂口。它是攀援植物，插一根苔藓柱可以引导向上生长并长出更大的叶片。',
   '["立苔藓柱引导攀援，叶片会明显变大","气生根可以引入盆土或保持外露","汁液含草酸钙，修剪时建议戴手套"]',
   1, 350),

  ('pothos-golden', '绿萝', 'Epipremnum aureum', '耐阴绿植', 'leaf',
   NULL, NULL, NULL,
   1, 3, '低至中等光照',
   15, 30, 40, 70,
   10, '土表干透后浇透', 45, 24,
   1, 1,
   1, 1, 1, 0,
   150, 40,
   '["新手入门","耐阴角落","悬挂垂吊"]',
   '几乎任何光线下都能生长，忘记浇水也能恢复，是容错度最高的入门绿植之一。',
   '心形叶片顺着枝条层层垂落，柜顶和书架的常见选择。',
   '绿萝的适应范围极广，从明亮窗边到远离光源的走廊都能存活。枝条剪下插水即可生根，是最容易繁殖的室内植物。垂落的枝叶容易被宠物或幼儿够到，有安全顾虑时应换其他候选。',
   '["枝条剪下水培即可生根","定期修剪徒长枝条以保持饱满株型","叶片含草酸钙，需放在宠物和幼儿够不到的位置"]',
   1, 330),

  ('peace-lily', '白掌', 'Spathiphyllum wallisii', '耐阴绿植', 'flower',
   '春季至秋季', '白色', '无香',
   1, 2, '柔和弱光',
   16, 28, 50, 75,
   6, '土表微干时补水', 30, 18,
   0, 2,
   1, 1, 1, 0,
   60, 50,
   '["耐阴开花","缺水提示","室内净化"]',
   '少见的能在弱光下持续开花的品种，缺水时叶片会明显下垂给出提示，浇水后很快恢复。',
   '洁白的佛焰苞从深绿叶丛中抽出，弱光房间也能拥有花。',
   '白掌是最耐阴的开花室内植物之一。它缺水时整株叶片会明显下垂，补水后数小时内即可恢复挺立，这个特性让它对新手格外友好。',
   '["叶片下垂是缺水信号，及时补水即可恢复","花后剪除枯萎花梗以促发新花","汁液含草酸钙，宠物和幼儿需隔离"]',
   1, 310),

  ('rubber-plant', '橡皮树', 'Ficus elastica', '大型绿植', 'leaf',
   NULL, NULL, NULL,
   3, 4, '明亮散射光',
   15, 30, 40, 60,
   12, '土表 3–5 厘米干后浇透', 45, 30,
   1, 1,
   1, 1, 1, 0,
   200, 80,
   '["客厅落地","厚实叶片","低频浇水"]',
   '厚实革质叶片耐旱性好于同属的琴叶榕，明亮光线下株型挺拔，适合光照充足的落地位置。',
   '油亮厚叶层层向上，是比琴叶榕更省心的落地大株。',
   '橡皮树的叶片厚实且带蜡质，比琴叶榕更耐受光照与浇水的波动，是想要落地大株但担心养不活时的稳妥选择。摘心可以促使侧枝萌发，让株型更饱满。',
   '["摘除顶芽可促发侧枝，避免独杆徒长","叶面定期擦拭以保持光泽","汁液为白色乳液，修剪时注意避免接触皮肤"]',
   1, 290),

  ('zz-plant', '金钱树', 'Zamioculcas zamiifolia', '耐阴绿植', 'leaf',
   NULL, NULL, NULL,
   1, 3, '低至中等光照',
   15, 30, 30, 60,
   21, '盆土完全干透后浇透', 90, 36,
   0, 1,
   1, 1, 1, 0,
   80, 60,
   '["经常出差","耐阴角落","极低频浇水"]',
   '地下块茎储水，三周不浇水也无妨，同时能耐受弱光，适合出差频繁又想要绿意的人。',
   '油亮的羽状叶几乎不需要照料，是最接近"免打理"的室内植物。',
   '金钱树依靠地下块茎储存水分，是耐旱与耐阴兼备的少数品种。它最常见的死因是浇水过多导致块茎腐烂，宁可少浇也不要多浇。',
   '["浇水过多是唯一常见死因，宁干勿湿","冬季可延长至一个月浇一次","汁液含草酸钙，宠物和幼儿需隔离"]',
   1, 270),

  ('anthurium-andraeanum', '红掌', 'Anthurium andraeanum', '特色观叶', 'flower',
   '全年断续开花', '红色', '无香',
   2, 3, '柔和散射光',
   18, 28, 60, 80,
   7, '土表 2 厘米干后补水', 30, 18,
   0, 2,
   1, 1, 1, 0,
   45, 40,
   '["长效观花","蜡质光泽","稳定湿度"]',
   '苞片质地厚实、单朵可观赏数周，在柔和散射光与稳定湿度下能全年断续开花。',
   '红亮的蜡质苞片衬着深绿叶片，是能开很久的室内观花植物。',
   '红掌的观赏部位是佛焰苞而非真正的花，单枚苞片可以维持数周不凋。它需要温暖与较高空气湿度，冬季低于 15 度时容易停止开花并出现叶片发黄。',
   '["使用疏松透气的兰科植料，忌用普通园土","冬季保持温度不低于 15 度","汁液含草酸钙，宠物和幼儿需隔离"]',
   1, 250);

-- ------------------------------------------------------------
-- 植物商品 SKU 初始数据
--
-- species_id 用子查询按 code 关联，不写死自增 id。
-- 同品种内 sort 值最大者为默认 SKU，公开接口的价格与主图取自它。
--
-- 图片：frontend/public/images/ 目前只有 4 张实拍图，新品种轮流复用。
-- image_alt 按图片实际内容写而非按品种名写——写成品种名会让读屏用户
-- 听到的描述与看到的图不符，这比 alt 文案笼统更糟。
-- 补齐实拍图后只需改 image 与 image_alt 两列。
-- ------------------------------------------------------------
INSERT IGNORE INTO `catalog_sku`
  (`species_id`, `sku_code`, `spec`, `pot`, `price`, `stock`, `image`, `image_alt`, `featured`, `status`, `sort`)
SELECT s.id, v.sku_code, v.spec, v.pot, v.price, v.stock, v.image, v.image_alt, v.featured, 1, v.sort
FROM (
  SELECT 'fiddle-leaf-fig' AS code, 'fiddle-leaf-fig-75' AS sku_code, '中大型 · 约 75 厘米' AS spec, '米白陶盆' AS pot, 168.00 AS price, 20 AS stock, '/images/fiddle-leaf-fig.webp' AS image, '陶盆中舒展宽大叶片的琴叶榕' AS image_alt, 1 AS featured, 20 AS sort
  UNION ALL SELECT 'fiddle-leaf-fig', 'fiddle-leaf-fig-110', '大型 · 约 110 厘米', '深灰水泥盆', 288.00, 8, '/images/fiddle-leaf-fig.webp', '陶盆中舒展宽大叶片的琴叶榕', 0, 10

  UNION ALL SELECT 'asparagus-fern', 'asparagus-fern-30', '小型 · 约 30 厘米', '素烧陶盆', 49.00, 40, '/images/asparagus-fern.webp', '细密轻盈叶片的桌面文竹', 0, 20

  UNION ALL SELECT 'alocasia-black-velvet', 'alocasia-black-velvet-25', '小型 · 约 25 厘米', '哑光黑陶盆', 88.00, 15, '/images/alocasia-black-velvet.webp', '深色绒面叶片和银白叶脉的黑天鹅海芋', 1, 20

  UNION ALL SELECT 'snake-plant', 'snake-plant-50', '中型 · 约 50 厘米', '米白陶盆', 69.00, 50, '/images/snake-plant.webp', '叶片挺拔且带有金色叶缘的虎尾兰', 1, 20
  UNION ALL SELECT 'snake-plant', 'snake-plant-30', '小型 · 约 30 厘米', '素烧陶盆', 39.00, 60, '/images/snake-plant.webp', '叶片挺拔且带有金色叶缘的虎尾兰', 0, 10

  UNION ALL SELECT 'spider-plant', 'spider-plant-30', '小型 · 约 30 厘米', '白色垂吊盆', 45.00, 55, '/images/asparagus-fern.webp', '细密轻盈叶片的桌面绿植', 1, 20

  UNION ALL SELECT 'parlor-palm', 'parlor-palm-100', '中型 · 约 100 厘米', '藤编套盆', 158.00, 18, '/images/snake-plant.webp', '株型挺拔的落地绿植', 0, 20
  UNION ALL SELECT 'parlor-palm', 'parlor-palm-60', '中小型 · 约 60 厘米', '素烧陶盆', 89.00, 25, '/images/snake-plant.webp', '株型挺拔的落地绿植', 0, 10

  UNION ALL SELECT 'boston-fern', 'boston-fern-45', '中小型 · 约 45 厘米', '白色垂吊盆', 62.00, 30, '/images/asparagus-fern.webp', '细密轻盈羽状叶片的蕨类植物', 0, 20

  UNION ALL SELECT 'hoya-carnosa', 'hoya-carnosa-40', '小型 · 约 40 厘米', '素烧陶盆', 78.00, 22, '/images/alocasia-black-velvet.webp', '厚实叶片的小型观叶植物', 0, 20

  UNION ALL SELECT 'calathea-orbifolia', 'calathea-orbifolia-50', '中小型 · 约 50 厘米', '哑光白陶盆', 98.00, 20, '/images/alocasia-black-velvet.webp', '宽大叶片带有清晰条纹的观叶植物', 1, 20

  UNION ALL SELECT 'areca-palm', 'areca-palm-150', '大型 · 约 150 厘米', '藤编套盆', 258.00, 10, '/images/fiddle-leaf-fig.webp', '枝叶舒展的大型落地绿植', 1, 20
  UNION ALL SELECT 'areca-palm', 'areca-palm-90', '中大型 · 约 90 厘米', '深灰水泥盆', 148.00, 16, '/images/fiddle-leaf-fig.webp', '枝叶舒展的大型落地绿植', 0, 10

  UNION ALL SELECT 'peperomia-obtusifolia', 'peperomia-obtusifolia-20', '小型 · 约 20 厘米', '素烧陶盆', 35.00, 65, '/images/asparagus-fern.webp', '株型紧凑的小型桌面绿植', 0, 20

  UNION ALL SELECT 'haworthia-fasciata', 'haworthia-fasciata-12', '迷你 · 约 12 厘米', '白色瓷盆', 25.00, 80, '/images/alocasia-black-velvet.webp', '株型紧凑的小型多肉植物', 0, 20

  UNION ALL SELECT 'monstera-deliciosa', 'monstera-deliciosa-120', '大型 · 约 120 厘米', '深灰水泥盆', 228.00, 12, '/images/fiddle-leaf-fig.webp', '叶片宽大舒展的大型观叶植物', 1, 20
  UNION ALL SELECT 'monstera-deliciosa', 'monstera-deliciosa-70', '中型 · 约 70 厘米', '米白陶盆', 128.00, 20, '/images/fiddle-leaf-fig.webp', '叶片宽大舒展的大型观叶植物', 0, 10

  UNION ALL SELECT 'pothos-golden', 'pothos-golden-30', '小型 · 约 30 厘米', '白色垂吊盆', 32.00, 70, '/images/asparagus-fern.webp', '枝叶垂落的小型绿植', 0, 20

  UNION ALL SELECT 'peace-lily', 'peace-lily-55', '中型 · 约 55 厘米', '哑光白陶盆', 86.00, 24, '/images/alocasia-black-velvet.webp', '深绿叶丛中抽出浅色苞片的观花植物', 0, 20

  UNION ALL SELECT 'rubber-plant', 'rubber-plant-120', '大型 · 约 120 厘米', '藤编套盆', 198.00, 14, '/images/fiddle-leaf-fig.webp', '叶片厚实油亮的落地大株', 0, 20
  UNION ALL SELECT 'rubber-plant', 'rubber-plant-70', '中型 · 约 70 厘米', '米白陶盆', 118.00, 22, '/images/fiddle-leaf-fig.webp', '叶片厚实油亮的落地大株', 0, 10

  UNION ALL SELECT 'zz-plant', 'zz-plant-60', '中型 · 约 60 厘米', '深灰水泥盆', 96.00, 35, '/images/snake-plant.webp', '叶片油亮挺拔的耐阴绿植', 1, 20

  UNION ALL SELECT 'anthurium-andraeanum', 'anthurium-andraeanum-40', '小型 · 约 40 厘米', '哑光白陶盆', 92.00, 26, '/images/alocasia-black-velvet.webp', '深绿叶片衬托彩色苞片的观花植物', 0, 20
) v
JOIN `catalog_species` s ON s.code = v.code;

-- ------------------------------------------------------------
-- demo 账号的示例场景画像
--
-- 建两份而不是一份：只有一份时，"多场景切换"和"删除最后一个场景要被拦下"
-- 这两条规则在界面上都验证不了。
--
-- 客厅是明亮落地、养猫、预算宽松；卧室是弱光桌面、无宠物、预算收紧——
-- 两份的硬过滤结果应当明显不同，正好用来演示画像确实在起作用。
--
-- user_id 用子查询按 username 关联，不写死自增 id。
-- max_footprint_cm 与 space_level 对应：1 桌面=30 / 2 层架=60 / 3 落地=150，
-- 派生规则在 SceneProfileServiceImpl 里，这里手工对齐。
-- ------------------------------------------------------------
INSERT IGNORE INTO `user_scene_profile`
  (`user_id`, `scene_name`, `is_default`,
   `placement`, `light_level`, `temp_level`, `humidity_level`,
   `space_level`, `max_footprint_cm`, `ventilation`,
   `budget_min`, `budget_max`, `prefer_ornamental`,
   `has_child`, `has_cat`, `has_dog`,
   `experience_level`, `water_times_week`, `travel_frequency`, `forgetful`,
   `accept_repot`, `accept_fertilize`, `accept_prune`)
SELECT u.id, v.scene_name, v.is_default,
       v.placement, v.light_level, v.temp_level, v.humidity_level,
       v.space_level, v.max_footprint_cm, v.ventilation,
       v.budget_min, v.budget_max, v.prefer_ornamental,
       v.has_child, v.has_cat, v.has_dog,
       v.experience_level, v.water_times_week, v.travel_frequency, v.forgetful,
       v.accept_repot, v.accept_fertilize, v.accept_prune
FROM (
  SELECT '客厅' AS scene_name, 1 AS is_default,
         'living_room' AS placement, 3 AS light_level, 2 AS temp_level, 2 AS humidity_level,
         3 AS space_level, 150 AS max_footprint_cm, 2 AS ventilation,
         50.00 AS budget_min, 300.00 AS budget_max, 'any' AS prefer_ornamental,
         0 AS has_child, 1 AS has_cat, 0 AS has_dog,
         1 AS experience_level, 2 AS water_times_week, 1 AS travel_frequency, 0 AS forgetful,
         1 AS accept_repot, 1 AS accept_fertilize, 0 AS accept_prune
  UNION ALL
  SELECT '卧室', 0,
         'bedroom', 1, 2, 1,
         1, 30, 1,
         0.00, 80.00, 'leaf',
         0, 1, 0,
         1, 1, 2, 1,
         0, 0, 0
) v
JOIN `sys_user` u ON u.username = 'demo';


-- ------------------------------------------------------------
-- 推荐权重默认配置（单行，id=1）
--
-- 30/15/10/20/10/10/5 是方案架构章节给出的口径。光照占 30% 是因为它是
-- 最硬的环境约束——光照不对的植物养不活，其余各维只影响养得省不省心。
-- 管理员可在 /api/admin/rec-weights 调整，改完后的历史推荐仍按快照解释。
-- ------------------------------------------------------------
INSERT INTO `rec_weight_config`
  (`id`, `w_light`, `w_temp`, `w_humidity`, `w_care`, `w_space`, `w_budget`, `w_preference`, `top_n`)
VALUES
  (1, 30, 15, 10, 20, 10, 10, 5, 3);

-- ------------------------------------------------------------
-- demo 的默认收货地址
--
-- 种一条，让下单流程一进去就能跑通，不必先去建地址。
-- 按 username 关联而不是写死 user_id，与其余种子数据一致。
-- ------------------------------------------------------------
INSERT INTO `user_address`
  (`user_id`, `receiver`, `phone`, `province`, `city`, `district`, `detail`, `is_default`)
SELECT u.id, '张雨涛', '13800138000', '江苏省', '南京市', '栖霞区', '文苑路 9 号科文学院 3 号楼 201', 1
FROM `sys_user` u WHERE u.username = 'demo';

-- ------------------------------------------------------------
-- 知识文章 10 篇
--
-- 前四篇由 CareGuidePage.vue 里的静态内容迁入，slug 沿用原来的锚点
-- （watering/light/feeding/repotting），那个页面上的深链因此仍然有效。
-- 后六篇覆盖方案点名但此前空缺的主题：修剪、基质与花盆、季节养护、
-- 宠物安全、病虫害、花期管理。
--
-- 每篇都填齐方案要求的四项：适用条件、操作频次、步骤化说明、常见误区与风险提示。
-- related_task_types 让养护任务能跳到对应文章；related_species 让商品详情能跳。
-- ------------------------------------------------------------
INSERT INTO `knowledge_article`
  (`slug`, `title`, `summary`, `category`, `difficulty`, `seasons`, `tags`,
   `applicable`, `frequency`, `steps`, `mistakes`, `risks`,
   `related_task_types`, `related_species`, `status`, `published_at`, `sort`)
VALUES
  ('watering', '让盆土告诉你时间', '日期只能提醒你去观察，不能直接决定要不要浇。',
   'watering', 1, NULL, '["浇水","新手必读","烂根"]',
   '表土干到适合该植物的深度，花盆明显变轻，叶片还没有严重萎蔫。',
   '每次提醒时检查；实际浇水间隔会随温度、光照和盆器改变。',
   '[{"title":"先探土，别只看表面","detail":"用手指或竹签插入表土下方 3–5 厘米，靠触感判断干湿。表面发白但下面仍湿是最常见的误判来源。"},
     {"title":"确认后一次浇透","detail":"沿盆土边缘缓慢浇，直到盆底刚有水流出为止。浇半截会让下层根系长期缺水。"},
     {"title":"十分钟后倒掉托盘积水","detail":"根系长期泡在积水里会缺氧腐烂，这一步比浇水本身更容易被忽略。"}]',
   '["叶子发黄不等于缺水。盆土湿、叶片软时继续浇水，往往会让问题更严重。","按固定日期浇水而不看盆土。同一株植物在夏天和冬天的需水量能差一倍。"]',
   '["长期积水导致烂根是室内植物最常见的死因，且早期在地上部分看不出来。"]',
   '["water"]', NULL, 2, NOW(), 100),

  ('light', '观察一整天，而非一瞬间', '同一扇窗在上午和下午可能是两种环境，季节也会改变光线角度。',
   'light', 1, NULL, '["光照","位置","徒长"]',
   '新叶变小、叶柄拉长可能在找光；叶面出现发白干斑则可能是突然暴晒。',
   '每周观察一次株型；季节转换或移动位置后的两周内提高观察频率。',
   '[{"title":"一天看三次","detail":"在上午、中午、下午各看一次叶面是否出现明显光斑，判断这个位置真实的光照强度。"},
     {"title":"移动要分阶段","detail":"需要增加光照时分次靠近窗户，每次移动后观察一周，不要一次跨越太大距离。"},
     {"title":"每周转盆四分之一圈","detail":"沿同一方向旋转，避免植株长期单侧受光而歪斜。"}]',
   '["刚到家的植物需要适应期。即使品种喜光，也不要立刻从室内弱光移到正午直晒处。","把「耐阴」理解成「不需要光」。耐阴只是能忍受较弱光线，不是能在全黑环境里活。"]',
   '["突然暴晒造成的叶面灼伤不可逆，受损的叶片不会恢复。"]',
   '["rotate"]', NULL, 2, NOW(), 95),

  ('feeding', '生长稳定，再补充营养', '肥料不是急救药。根系、光照或浇水有问题时，先恢复环境。',
   'feeding', 2, '["spring","summer"]', '["施肥","生长期","浓度"]',
   '处在生长期、持续长出健康新叶，且近期没有换盆或明显病害。',
   '生长季每 3–4 周一次；秋冬生长放缓时减半或暂停。',
   '[{"title":"先确认盆土微湿","detail":"避免在完全干燥的根系上直接施肥，否则肥液会直接灼伤根尖。"},
     {"title":"严格按标签稀释","detail":"从最低建议浓度开始，宁淡勿浓。不要凭感觉加量，也不要混用多种肥料。"},
     {"title":"记录日期与浓度","detail":"施肥后记下来，接下来两周观察叶尖与新叶的反应，据此调整下一次的量。"}]',
   '["把肥料当成救命药。植株状态差时施肥，多半会加速衰败而不是挽救。","浓度加倍见效更快。过量施肥造成的烧根比缺肥严重得多。"]',
   '["刚换盆、根系受损、极端高温或生长停滞时施肥，容易造成不可逆的根系损伤。"]',
   '["fertilize"]', NULL, 2, NOW(), 90),

  ('repotting', '为根系多留一点余地', '换盆看根系状态，不以"买回家就换"为默认动作。',
   'repotting', 2, '["spring"]', '["换盆","根系","盆器"]',
   '根系持续钻出排水孔、盆土很快干透，或水直接从边缘流走且植株明显头重脚轻。',
   '每个生长季检查一到两次；没有拥挤信号时不必频繁打扰。',
   '[{"title":"选只大一号的盆","detail":"新盆直径比原盆大约 2–4 厘米即可，底部必须有排水孔。"},
     {"title":"保留健康根团","detail":"尽量不打散原土球，只剪除发黑腐烂的根，按原来的栽植深度上盆。"},
     {"title":"缓苗两周","detail":"放回稳定的散射光处，短期内避免施肥和频繁搬动，让根系先恢复。"}]',
   '["买回家立刻换盆。刚经历运输的植株正在适应新环境，此时换盆是双重打击。","一次换到很大的盆。多余土壤长期保水，对小根系来说「更大的家」并不安全。"]',
   '["换盆后短期黄叶多属缓苗反应，此时再施肥或再次移动会让恢复更慢。"]',
   '["repot"]', NULL, 2, NOW(), 85),

  ('pruning', '剪掉的是消耗，留下的是形态', '修剪不只是为了好看，更是把养分让给真正在长的枝条。',
   'pruning', 2, '["spring","summer"]', '["修剪","徒长","造型"]',
   '出现枯黄叶、徒长的细长枝条，或株型明显偏向一侧、内部枝条相互遮挡。',
   '生长季每 1–2 个月检查一次；开花植物在花后修剪。',
   '[{"title":"先剪明显该去的","detail":"枯叶、黄叶、病叶、干枯枝条随时可以剪，这一步不会有争议。"},
     {"title":"再处理徒长枝","detail":"细长、叶片稀疏的枝条从基部或健壮芽点上方斜切，促发更紧凑的侧枝。"},
     {"title":"工具保持洁净","detail":"每次修剪前后擦拭刀口，避免在植株之间传播病害。"}]',
   '["舍不得下手，只摘叶不剪枝。徒长枝会持续消耗养分，让整株越来越松散。","一次剪掉过多。单次去除量建议不超过全株的三分之一。"]',
   '["部分植物（如橡皮树）的伤口会渗出白色乳液，可能刺激皮肤，修剪时注意防护。","开花植物在花芽形成后修剪，会把当季的花一起剪掉。"]',
   '["prune"]', '["fiddle-leaf-fig","asparagus-fern","rubber-plant"]', 2, NOW(), 80),

  ('medium', '根系呼吸，比保水更重要', '大多数室内植物死于介质不透气，而不是死于营养不够。',
   'medium', 2, NULL, '["基质","花盆","排水"]',
   '浇水后长时间不干、盆土板结、或换盆时发现根系发黑有异味。',
   '换盆时选择；日常不需要频繁更换。',
   '[{"title":"优先看排水孔","detail":"没有排水孔的盆器再好看也不要用作栽植盆，可以当外套盆使用。"},
     {"title":"按习性配介质","detail":"多数观叶植物用泥炭加珍珠岩；多肉与十二卷类需要更高比例的颗粒；兰科类要用专用树皮植料。"},
     {"title":"避免直接用园土","detail":"园土在花盆里会板结，浇水后表面结壳而内部积水。"}]',
   '["在盆底垫一层石子「增加排水」。这实际会抬高积水层，效果适得其反。","所有植物用同一种土。红掌用普通园土几乎必然烂根。"]',
   '["介质长期不透气会导致根系缺氧，地上部分往往在根系已经严重受损后才出现症状。"]',
   '["repot"]', '["haworthia-fasciata","anthurium-andraeanum"]', 2, NOW(), 75),

  ('season', '四季不是同一套养法', '同一株植物在冬夏的需水量能差一倍，按同一个节奏养必然出问题。',
   'season', 1, '["spring","summer","autumn","winter"]', '["季节","越冬","度夏"]',
   '季节转换时，或发现原本正常的养护节奏突然不合适了。',
   '每个季节交替时调整一次，约一年四次。',
   '[{"title":"春季恢复生长","detail":"逐步增加浇水频率，恢复施肥，这是换盆与修剪的最佳窗口。"},
     {"title":"夏季控温防晒","detail":"高温时蒸发快，浇水加密但避开正午；同时留意通风，闷热是虫害高发的前提。"},
     {"title":"秋季逐步减量","detail":"随气温下降拉长浇水间隔，停止大量施肥，让植株为越冬做准备。"},
     {"title":"冬季以少为主","detail":"多数植物半休眠，浇水间隔要明显拉长，远离暖气与冷风直吹处。"}]',
   '["冬天按夏天的频率浇水。这是室内植物最常见的越冬死因。","冬季继续正常施肥。生长停滞时施肥等于把肥留在土里烧根。"]',
   '["冷风直吹会造成短时间大量失水，症状与缺水相似但补水无法缓解。"]',
   '["water","fertilize"]', NULL, 2, NOW(), 70),

  ('pet-safety', '有猫狗和孩子时，先看这一篇', '很多常见观叶植物含草酸钙，误食会造成口腔与消化道刺激。',
   'pet-safety', 1, NULL, '["宠物","儿童","毒性","安全"]',
   '家里有猫、狗或学龄前儿童，尤其是会啃咬叶片或翻盆土的。',
   '选购前必看；家庭成员变化时重新检查一遍现有植物。',
   '[{"title":"选购前先查毒性","detail":"商品详情页标注了对猫、狗、儿童各自的风险。本站推荐算法会把这一条作为不可放宽的硬性条件。"},
     {"title":"已有的有毒植物移到够不着处","detail":"高处壁挂或独立房间。注意猫的跳跃能力常被低估。"},
     {"title":"修剪时戴手套","detail":"部分植物的汁液会刺激皮肤，修剪与换盆后及时洗手。"},
     {"title":"记下应急做法","detail":"发现误食先清水漱口并保留植株样本，及时联系兽医或医生，不要自行催吐。"}]',
   '["以为「放高一点」就安全。猫能上到大多数你以为够不着的地方。","把「无毒」理解成「可以吃」。无毒只是指不含已知毒性成分，大量误食仍会造成肠胃不适。"]',
   '["龟背竹、白掌、金钱树、红掌等常见品种均含草酸钙，宠物与幼儿需严格隔离。","误食后症状可能延迟出现，不能因为「当时没事」就放松观察。"]',
   NULL, '["monstera-deliciosa","peace-lily","zz-plant","anthurium-andraeanum","pothos-golden"]',
   2, NOW(), 65),

  ('pest', '早发现，处理起来最省事', '虫害在密度低时用清水就能解决，等到肉眼可见成片时已经很被动。',
   'pest', 2, '["summer"]', '["病虫害","红蜘蛛","介壳虫","蚜虫"]',
   '发现叶片有细小斑点、黏液、蛛网状细丝，或新芽畸形卷曲。',
   '每月检查一次；夏季高温高湿期每两周一次。',
   '[{"title":"重点查叶背与叶腋","detail":"蚜虫、红蜘蛛、介壳虫都藏在这些位置，只看叶面几乎一定会漏掉。"},
     {"title":"先物理清除","detail":"虫口密度低时用清水冲洗，或用棉签蘸酒精擦除介壳虫，不必立即用药。"},
     {"title":"隔离观察两周","detail":"处理后与其他植物分开放置，防止扩散。这一步最常被省略，也最常导致复发。"},
     {"title":"改善通风","detail":"闷湿是多数虫害的前提条件，通风改善后复发率显著下降。"}]',
   '["一发现虫就大剂量喷药。室内环境下药剂残留对人和宠物的影响往往大于虫害本身。","只处理看得见的那几只。不隔离、不复查，两周后必然卷土重来。"]',
   '["在室内使用农药需严格按说明通风，有宠物和儿童的家庭应优先选择物理方法。"]',
   '["pest"]', NULL, 2, NOW(), 60),

  ('bloom', '想让它开花，先让它长好', '开花是植株状态好的结果，不是靠催出来的。',
   'bloom', 3, '["spring","summer"]', '["花期","观花","催花"]',
   '养的是观花品种，植株已经稳定生长但迟迟不开花，或想延长花期。',
   '花期前后各调整一次；平时按常规养护即可。',
   '[{"title":"先保证光照充足","detail":"光照不足是观花植物不开花的首要原因，多数需要明亮散射光甚至部分直射。"},
     {"title":"花芽期改用磷钾肥","detail":"氮肥促进长叶，磷钾肥促进花芽分化。花期前改用高磷钾配方。"},
     {"title":"花后及时剪除残花","detail":"避免植株把养分消耗在结籽上，也能促进下一轮花芽形成。"},
     {"title":"注意温差与休眠","detail":"部分品种需要一段低温期才能形成花芽，全年恒温反而不开花。"}]',
   '["不开花就加大施肥。氮肥过量会让植株疯长叶片而完全不开花。","花期频繁移动位置。花苞期的环境变化容易造成落蕾。"]',
   '["花粉可能引发过敏，对花粉敏感的家庭成员应留意商品详情页的花粉风险标注。"]',
   '["fertilize"]', '["peace-lily","anthurium-andraeanum","hoya-carnosa"]', 2, NOW(), 55);
