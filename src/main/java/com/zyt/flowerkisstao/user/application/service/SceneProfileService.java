package com.zyt.flowerkisstao.user.application.service;

import com.zyt.flowerkisstao.user.web.dto.SceneProfileSaveDTO;
import com.zyt.flowerkisstao.user.web.vo.SceneProfileVO;

import java.util.List;

/**
 * 用户场景画像，推荐算法的输入端。
 *
 * <p>所有方法都以当前登录用户为边界：按 id 操作时若画像不属于本人，
 * 一律按"不存在"处理，不返回 403——403 等于告诉对方"这个 id 确实存在，
 * 只是不属于你"。
 */
public interface SceneProfileService {

    /** 我的全部场景，默认场景排在最前 */
    List<SceneProfileVO> listMine();

    /** 我的默认场景。一个都没有时返回 null，而不是抛异常——新用户本就没有 */
    SceneProfileVO getMyDefault();

    SceneProfileVO get(Long id);

    Long create(SceneProfileSaveDTO dto);

    void update(Long id, SceneProfileSaveDTO dto);

    /** 删除。已是最后一个场景时拒绝，抛 BizException(PROFILE_LAST_SCENE) */
    void remove(Long id);

    void setDefault(Long id);
}
