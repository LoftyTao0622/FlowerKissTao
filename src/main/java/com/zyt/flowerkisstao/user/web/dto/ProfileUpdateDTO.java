package com.zyt.flowerkisstao.user.web.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 当前用户可自行修改的资料。未传字段保持原值，昵称传空字符串可清空。 */
@Data
public class ProfileUpdateDTO {

    @Size(min = 4, max = 20, message = "用户名长度需在 4 到 20 之间")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "用户名只能包含字母、数字、下划线和短横线")
    private String username;

    @Size(max = 50, message = "昵称最长 50 个字符")
    private String nickname;

    /** 修改密码时必须填写，用于确认账号当前归属。 */
    @Size(max = 72, message = "当前密码过长")
    private String currentPassword;

    @Size(min = 8, max = 32, message = "新密码长度需在 8 到 32 之间")
    private String newPassword;
}
