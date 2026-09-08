package com.zyt.flowerkisstao.user.web.controller;

import com.zyt.flowerkisstao.shared.security.Perms;
import com.zyt.flowerkisstao.shared.web.R;
import com.zyt.flowerkisstao.user.application.service.UserProfileService;
import com.zyt.flowerkisstao.user.web.dto.ProfileUpdateDTO;
import com.zyt.flowerkisstao.user.web.vo.AvatarUploadVO;
import com.zyt.flowerkisstao.user.web.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user/profile")
public class UserProfileController {

    private final UserProfileService profileService;

    public UserProfileController(UserProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Perms.USER_PROFILE_READ_OWN + "')")
    public R<UserVO> current() {
        return R.ok(profileService.current());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('" + Perms.USER_PROFILE_UPDATE_OWN + "')")
    public R<UserVO> update(@Valid @RequestBody ProfileUpdateDTO dto) {
        return R.ok(profileService.update(dto));
    }

    @PostMapping("/avatar")
    @PreAuthorize("hasAuthority('" + Perms.USER_PROFILE_UPDATE_OWN + "')")
    public R<AvatarUploadVO> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return R.ok(profileService.uploadAvatar(file));
    }
}
