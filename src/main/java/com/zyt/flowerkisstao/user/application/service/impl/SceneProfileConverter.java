package com.zyt.flowerkisstao.user.application.service.impl;

import com.zyt.flowerkisstao.user.domain.entity.UserSceneProfile;
import com.zyt.flowerkisstao.user.web.vo.SceneProfileVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 画像实体到 VO 的转换，附带档位到展示文案的映射。
 *
 * <p>映射放后端而非前端：首页档案卡片、问卷回顾页、将来的推荐理由都要用同一套
 * 说法，散在前端会变成三份措辞不同的副本。
 */
public final class SceneProfileConverter {

    private SceneProfileConverter() {
    }

    public static SceneProfileVO toVO(UserSceneProfile profile) {
        return SceneProfileVO.builder()
                .id(profile.getId())
                .sceneName(profile.getSceneName())
                .isDefault(isTrue(profile.getIsDefault()))
                .placement(profile.getPlacement())
                .placementLabel(placementLabel(profile.getPlacement()))
                .lightLevel(profile.getLightLevel())
                .lightLabel(lightLabel(profile.getLightLevel()))
                .tempLevel(profile.getTempLevel())
                .tempLabel(tempLabel(profile.getTempLevel()))
                .humidityLevel(profile.getHumidityLevel())
                .humidityLabel(humidityLabel(profile.getHumidityLevel()))
                .spaceLevel(profile.getSpaceLevel())
                .spaceLabel(spaceLabel(profile.getSpaceLevel()))
                .maxFootprintCm(profile.getMaxFootprintCm())
                .ventilation(profile.getVentilation())
                .ventilationLabel(ventilationLabel(profile.getVentilation()))
                .budgetMin(profile.getBudgetMin())
                .budgetMax(profile.getBudgetMax())
                .preferOrnamental(profile.getPreferOrnamental())
                .preferOrnamentalLabel(ornamentalLabel(profile.getPreferOrnamental()))
                .hasChild(isTrue(profile.getHasChild()))
                .hasCat(isTrue(profile.getHasCat()))
                .hasDog(isTrue(profile.getHasDog()))
                .experienceLevel(profile.getExperienceLevel())
                .experienceLabel(experienceLabel(profile.getExperienceLevel()))
                .waterTimesWeek(profile.getWaterTimesWeek())
                .travelFrequency(profile.getTravelFrequency())
                .travelLabel(travelLabel(profile.getTravelFrequency()))
                .forgetful(isTrue(profile.getForgetful()))
                .acceptRepot(isTrue(profile.getAcceptRepot()))
                .acceptFertilize(isTrue(profile.getAcceptFertilize()))
                .acceptPrune(isTrue(profile.getAcceptPrune()))
                .careTags(careTags(profile))
                .build();
    }

    /**
     * 由画像派生养护能力标签。
     *
     * <p>只保留真正影响选择的几条，不把每个字段都翻译成标签——十几个标签堆在
     * 卡片上等于没有重点。
     */
    private static List<String> careTags(UserSceneProfile profile) {
        List<String> tags = new ArrayList<>();

        if (profile.getExperienceLevel() != null && profile.getExperienceLevel() == 1) {
            tags.add("新手养护");
        } else if (profile.getExperienceLevel() != null && profile.getExperienceLevel() == 3) {
            tags.add("有养护经验");
        }

        // 出差频繁与容易忘记都指向同一件事：需要耐旱、容错高的植物
        if (isTrue(profile.getForgetful()) || (profile.getTravelFrequency() != null
                && profile.getTravelFrequency() == 3)) {
            tags.add("需要耐旱品种");
        }

        if (profile.getWaterTimesWeek() != null && profile.getWaterTimesWeek() <= 1) {
            tags.add("低频浇水");
        } else if (profile.getWaterTimesWeek() != null && profile.getWaterTimesWeek() >= 4) {
            tags.add("愿意勤照看");
        }

        if (isTrue(profile.getHasCat()) || isTrue(profile.getHasDog())) {
            tags.add("宠物安全优先");
        }
        if (isTrue(profile.getHasChild())) {
            tags.add("儿童安全优先");
        }

        if (profile.getLightLevel() != null && profile.getLightLevel() == 1) {
            tags.add("弱光环境");
        }

        // 三项养护动作都不接受时才提示，只是不想修剪不值得单独成标签
        if (!isTrue(profile.getAcceptRepot()) && !isTrue(profile.getAcceptFertilize())
                && !isTrue(profile.getAcceptPrune())) {
            tags.add("希望免打理");
        }

        return tags;
    }

    private static String placementLabel(String placement) {
        if (placement == null) {
            return null;
        }
        switch (placement) {
            case "living_room":
                return "客厅";
            case "bedroom":
                return "卧室";
            case "office":
                return "办公室";
            case "balcony":
                return "阳台";
            case "other":
                return "其他位置";
            default:
                return null;
        }
    }

    /** 与 catalog_species.light_note 的措辞保持一致，避免同一等级两处说法不同 */
    private static String lightLabel(Integer level) {
        if (level == null) {
            return null;
        }
        switch (level) {
            case 1:
                return "柔和弱光";
            case 2:
                return "柔和散射光";
            case 3:
                return "明亮散射光";
            case 4:
                return "充足直射光";
            default:
                return null;
        }
    }

    private static String tempLabel(Integer level) {
        if (level == null) {
            return null;
        }
        switch (level) {
            case 1:
                return "偏冷 · 低于 15℃";
            case 2:
                return "常温 · 15-28℃";
            case 3:
                return "偏热 · 高于 28℃";
            default:
                return null;
        }
    }

    private static String humidityLabel(Integer level) {
        if (level == null) {
            return null;
        }
        switch (level) {
            case 1:
                return "干燥 · 低于 40%";
            case 2:
                return "适中 · 40-70%";
            case 3:
                return "潮湿 · 高于 70%";
            default:
                return null;
        }
    }

    private static String spaceLabel(Integer level) {
        if (level == null) {
            return null;
        }
        switch (level) {
            case 1:
                return "桌面小空间";
            case 2:
                return "层架或窗边";
            case 3:
                return "落地空间";
            default:
                return null;
        }
    }

    private static String ventilationLabel(Integer level) {
        if (level == null) {
            return null;
        }
        switch (level) {
            case 1:
                return "通风较差";
            case 2:
                return "通风一般";
            case 3:
                return "通风良好";
            default:
                return null;
        }
    }

    private static String ornamentalLabel(String prefer) {
        if (prefer == null) {
            return null;
        }
        switch (prefer) {
            case "leaf":
                return "偏好观叶";
            case "flower":
                return "偏好观花";
            case "any":
                return "都可以";
            default:
                return null;
        }
    }

    private static String experienceLabel(Integer level) {
        if (level == null) {
            return null;
        }
        switch (level) {
            case 1:
                return "第一次认真养";
            case 2:
                return "养过一些";
            case 3:
                return "有养护经验";
            default:
                return null;
        }
    }

    private static String travelLabel(Integer level) {
        if (level == null) {
            return null;
        }
        switch (level) {
            case 1:
                return "很少出差";
            case 2:
                return "偶尔出差";
            case 3:
                return "经常出差";
            default:
                return null;
        }
    }

    private static boolean isTrue(Integer value) {
        return value != null && value == 1;
    }
}
