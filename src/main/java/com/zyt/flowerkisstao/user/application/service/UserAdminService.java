package com.zyt.flowerkisstao.user.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zyt.flowerkisstao.user.web.vo.UserVO;

import java.util.List;

public interface UserAdminService {

    IPage<UserVO> page(IPage<?> page, String keyword);

    /**
     * @param status 1 正常 0 封禁
     */
    void changeStatus(Long userId, Integer status);

    /** 全量覆盖，先删后插 */
    void assignRoles(Long userId, List<Long> roleIds);
}
