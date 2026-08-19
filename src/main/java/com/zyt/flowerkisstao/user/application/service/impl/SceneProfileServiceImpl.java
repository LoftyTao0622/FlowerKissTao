package com.zyt.flowerkisstao.user.application.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import com.zyt.flowerkisstao.shared.security.CurrentUser;
import com.zyt.flowerkisstao.user.application.service.SceneProfileService;
import com.zyt.flowerkisstao.user.domain.entity.UserSceneProfile;
import com.zyt.flowerkisstao.user.infrastructure.mapper.UserSceneProfileMapper;
import com.zyt.flowerkisstao.user.web.dto.SceneProfileSaveDTO;
import com.zyt.flowerkisstao.user.web.vo.SceneProfileVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SceneProfileServiceImpl implements SceneProfileService {

    /** 各空间档位对应的冠幅上限厘米，与 schema.sql 的注释一致 */
    private static final int FOOTPRINT_DESKTOP = 30;
    private static final int FOOTPRINT_SHELF = 60;
    private static final int FOOTPRINT_FLOOR = 150;

    private final UserSceneProfileMapper profileMapper;

    public SceneProfileServiceImpl(UserSceneProfileMapper profileMapper) {
        this.profileMapper = profileMapper;
    }

    @Override
    public List<SceneProfileVO> listMine() {
        return listByUser(CurrentUser.requireUserId()).stream()
                .map(SceneProfileConverter::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public SceneProfileVO getMyDefault() {
        Long userId = CurrentUser.requireUserId();
        UserSceneProfile profile = profileMapper.selectOne(Wrappers.<UserSceneProfile>lambdaQuery()
                .eq(UserSceneProfile::getUserId, userId)
                .eq(UserSceneProfile::getIsDefault, 1)
                .last("LIMIT 1"));

        // 历史数据可能出现"有场景但没有默认"，退化成取最早的一个，
        // 而不是让首页卡片凭空空掉
        if (profile == null) {
            List<UserSceneProfile> all = listByUser(userId);
            if (all.isEmpty()) {
                // 新用户本就没有画像，返回 null 由前端显示引导态
                return null;
            }
            profile = all.get(0);
        }
        return SceneProfileConverter.toVO(profile);
    }

    @Override
    public SceneProfileVO get(Long id) {
        return SceneProfileConverter.toVO(requireOwnedProfile(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SceneProfileSaveDTO dto) {
        Long userId = CurrentUser.requireUserId();
        requireConsistent(dto);
        requireSceneNameAvailable(userId, dto.getSceneName(), null);

        UserSceneProfile profile = new UserSceneProfile();
        profile.setUserId(userId);
        applyDto(profile, dto);
        // 第一个场景自动成为默认，否则用户建完不设默认，首页就一直是空的
        boolean first = countByUser(userId) == 0;
        profile.setIsDefault(first ? 1 : 0);

        profileMapper.insert(profile);
        return profile.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SceneProfileSaveDTO dto) {
        UserSceneProfile existing = requireOwnedProfile(id);
        requireConsistent(dto);
        requireSceneNameAvailable(existing.getUserId(), dto.getSceneName(), id);

        UserSceneProfile update = new UserSceneProfile();
        update.setId(id);
        applyDto(update, dto);
        // isDefault 由 setDefault 单独管，避免编辑表单顺手改掉默认场景
        profileMapper.updateById(update);
    }

    /**
     * 删除场景。
     *
     * <p>uk_profile_user_scene 是物理唯一索引，不认 deleted 标记：直接删掉后想用
     * 同一个场景名重建就会撞唯一键。所以先把 scene_name 改名腾位，再标记删除，
     * 做法与 catalog 的 code / sku_code 一致。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        UserSceneProfile existing = requireOwnedProfile(id);
        Long userId = existing.getUserId();

        if (countByUser(userId) <= 1) {
            throw new BizException(ErrorCode.PROFILE_LAST_SCENE,
                    "这是最后一个场景，删掉后就没有推荐依据了。可以直接编辑它，或先新建另一个场景");
        }

        UserSceneProfile rename = new UserSceneProfile();
        rename.setId(id);
        rename.setSceneName(truncateSceneName(existing.getSceneName(), id));
        profileMapper.updateById(rename);

        profileMapper.deleteById(id);

        // 删掉的是默认场景时，把剩下最早的一个提上来，避免用户没有默认场景
        if (isTrue(existing.getIsDefault())) {
            List<UserSceneProfile> rest = listByUser(userId);
            if (!rest.isEmpty()) {
                promoteToDefault(rest.get(0).getId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        UserSceneProfile profile = requireOwnedProfile(id);

        // 先清掉该用户其余场景的默认标记，再置新的。放在一个事务里，
        // 中途失败不会留下"两个默认"或"零个默认"
        UserSceneProfile clear = new UserSceneProfile();
        clear.setIsDefault(0);
        profileMapper.update(clear, Wrappers.<UserSceneProfile>lambdaUpdate()
                .eq(UserSceneProfile::getUserId, profile.getUserId())
                .eq(UserSceneProfile::getIsDefault, 1));

        promoteToDefault(id);
    }

    private void promoteToDefault(Long id) {
        UserSceneProfile update = new UserSceneProfile();
        update.setId(id);
        update.setIsDefault(1);
        profileMapper.updateById(update);
    }

    /**
     * 取画像并确认归属。
     *
     * <p>不属于当前用户时抛 PROFILE_NOT_FOUND 而非 FORBIDDEN：返回 403 等于确认
     * "这个 id 确实存在"，把别人的画像 id 空间暴露给了调用方。
     */
    private UserSceneProfile requireOwnedProfile(Long id) {
        UserSceneProfile profile = profileMapper.selectById(id);
        if (profile == null || !profile.getUserId().equals(CurrentUser.requireUserId())) {
            throw new BizException(ErrorCode.PROFILE_NOT_FOUND, "场景不存在");
        }
        return profile;
    }

    /** 默认场景排最前，其余按创建时间；前端下拉框直接照这个顺序渲染 */
    private List<UserSceneProfile> listByUser(Long userId) {
        return profileMapper.selectList(Wrappers.<UserSceneProfile>lambdaQuery()
                .eq(UserSceneProfile::getUserId, userId)
                .orderByDesc(UserSceneProfile::getIsDefault)
                .orderByAsc(UserSceneProfile::getId));
    }

    private long countByUser(Long userId) {
        return profileMapper.selectCount(Wrappers.<UserSceneProfile>lambdaQuery()
                .eq(UserSceneProfile::getUserId, userId));
    }

    /** 新建时 excludeId 传 null；编辑时传自身 id，否则会和自己撞车 */
    private void requireSceneNameAvailable(Long userId, String sceneName, Long excludeId) {
        long taken = profileMapper.selectCount(Wrappers.<UserSceneProfile>lambdaQuery()
                .eq(UserSceneProfile::getUserId, userId)
                .eq(UserSceneProfile::getSceneName, sceneName)
                .ne(excludeId != null, UserSceneProfile::getId, excludeId));
        if (taken > 0) {
            throw new BizException(ErrorCode.PROFILE_SCENE_NAME_TAKEN,
                    "你已经有一个叫\"" + sceneName + "\"的场景了");
        }
    }

    /**
     * 跨字段的矛盾校验，对应方案里"对必填项、数值范围和矛盾答案进行校验"的后半句。
     *
     * <p>单字段的 @Min/@Max 只能看住取值范围，看不出几个合法取值凑在一起不成立。
     * 这类矛盾不拦下来，推荐算法会照着一份自相矛盾的画像认真算出结果，
     * 而用户完全看不出哪里错了。
     */
    private void requireConsistent(SceneProfileSaveDTO dto) {
        if (dto.getBudgetMin().compareTo(dto.getBudgetMax()) > 0) {
            throw new BizException("预算下限不能高于上限");
        }

        // 一次都不浇、又不出差也不健忘，三个答案凑不到一起
        if (dto.getWaterTimesWeek() == 0
                && dto.getTravelFrequency() == 1
                && Boolean.FALSE.equals(dto.getForgetful())) {
            throw new BizException("你选了很少出差且不容易忘记，但每周浇水次数是 0。"
                    + "请确认浇水次数，或调整出差与健忘的选项");
        }

        // 阳台不会是弱光，多半是位置或光照选错了一个
        if ("balcony".equals(dto.getPlacement()) && dto.getLightLevel() == 1) {
            throw new BizException("阳台通常不会是柔和弱光，请确认摆放位置与光照条件");
        }

        // 落地空间配桌面预算，容易在结果全空时让人摸不着头脑，先提示
        if (dto.getSpaceLevel() == UserSceneProfile.SPACE_FLOOR
                && dto.getBudgetMax().doubleValue() < 50) {
            throw new BizException("落地空间的植株通常在 50 元以上，当前预算上限可能筛不出候选");
        }
    }

    /** scene_name 列限长 50，拼后缀前先给原值留出空间 */
    private String truncateSceneName(String sceneName, Long id) {
        String suffix = "-del-" + id;
        int maxBase = 50 - suffix.length();
        String base = sceneName.length() > maxBase ? sceneName.substring(0, maxBase) : sceneName;
        return base + suffix;
    }

    private void applyDto(UserSceneProfile profile, SceneProfileSaveDTO dto) {
        profile.setSceneName(dto.getSceneName());
        profile.setPlacement(dto.getPlacement());
        profile.setLightLevel(dto.getLightLevel());
        profile.setTempLevel(dto.getTempLevel());
        profile.setHumidityLevel(dto.getHumidityLevel());
        profile.setSpaceLevel(dto.getSpaceLevel());
        // 冠幅上限跟着空间档位走，用户改了空间就必须同步刷新，
        // 否则推荐的硬过滤会拿着上一次的上限继续筛
        profile.setMaxFootprintCm(deriveMaxFootprint(dto.getSpaceLevel()));
        profile.setVentilation(dto.getVentilation());
        profile.setBudgetMin(dto.getBudgetMin());
        profile.setBudgetMax(dto.getBudgetMax());
        profile.setPreferOrnamental(dto.getPreferOrnamental());
        profile.setHasChild(toFlag(dto.getHasChild()));
        profile.setHasCat(toFlag(dto.getHasCat()));
        profile.setHasDog(toFlag(dto.getHasDog()));
        profile.setExperienceLevel(dto.getExperienceLevel());
        profile.setWaterTimesWeek(dto.getWaterTimesWeek());
        profile.setTravelFrequency(dto.getTravelFrequency());
        profile.setForgetful(toFlag(dto.getForgetful()));
        profile.setAcceptRepot(toFlag(dto.getAcceptRepot()));
        profile.setAcceptFertilize(toFlag(dto.getAcceptFertilize()));
        profile.setAcceptPrune(toFlag(dto.getAcceptPrune()));
    }

    /** 空间档位到冠幅上限的唯一定义处，data.sql 的种子数据手工与它对齐 */
    private int deriveMaxFootprint(Integer spaceLevel) {
        if (spaceLevel == null) {
            return FOOTPRINT_FLOOR;
        }
        switch (spaceLevel) {
            case UserSceneProfile.SPACE_DESKTOP:
                return FOOTPRINT_DESKTOP;
            case UserSceneProfile.SPACE_SHELF:
                return FOOTPRINT_SHELF;
            default:
                return FOOTPRINT_FLOOR;
        }
    }

    private Integer toFlag(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private boolean isTrue(Integer value) {
        return value != null && value == 1;
    }
}
