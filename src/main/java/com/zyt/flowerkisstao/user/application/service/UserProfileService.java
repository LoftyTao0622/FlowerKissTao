package com.zyt.flowerkisstao.user.application.service;

import com.zyt.flowerkisstao.user.web.dto.ProfileUpdateDTO;
import com.zyt.flowerkisstao.user.web.vo.AvatarUploadVO;
import com.zyt.flowerkisstao.user.web.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {

    UserVO current();

    UserVO update(ProfileUpdateDTO dto);

    AvatarUploadVO uploadAvatar(MultipartFile file);
}
