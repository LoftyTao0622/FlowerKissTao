package com.zyt.flowerkisstao.user.application.service;

import com.zyt.flowerkisstao.user.web.dto.LoginDTO;
import com.zyt.flowerkisstao.user.web.dto.RegisterDTO;
import com.zyt.flowerkisstao.user.web.vo.TokenVO;
import com.zyt.flowerkisstao.user.web.vo.UserVO;

public interface AuthService {

    /** 注册，默认授予 ROLE_USER */
    TokenVO register(RegisterDTO dto);

    TokenVO login(LoginDTO dto);

    /** 当前登录人信息，供前端刷新页面后恢复权限 */
    UserVO currentUser();
}
