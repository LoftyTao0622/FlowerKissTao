package com.zyt.flowerkisstao.user.web.controller;

import com.zyt.flowerkisstao.shared.security.Perms;
import com.zyt.flowerkisstao.shared.web.R;
import com.zyt.flowerkisstao.user.application.service.SceneProfileService;
import com.zyt.flowerkisstao.user.web.dto.SceneProfileSaveDTO;
import com.zyt.flowerkisstao.user.web.vo.SceneProfileVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 我的场景画像，推荐算法的输入端。
 *
 * <p>路径挂在 /api/profiles 而不是 /api/catalog 或 /api/home 之下：后两个前缀在
 * SecurityConfig.PUBLIC_GET_ENDPOINTS 白名单里，GET 会对游客敞开，而画像是私有数据。
 * /api/profiles 落进 anyRequest().authenticated()，正是想要的效果。
 *
 * <p>权限点沿用已有的 user:profile:read-own / update-own，ROLE_USER 默认就有，
 * 无需新增权限点或改角色配置。
 *
 * <p>接口没有 userId 参数：一律取当前登录人，前端传不进来别人的 id。
 */
@RestController
@RequestMapping("/api/profiles")
@Validated
public class SceneProfileController {

    private final SceneProfileService sceneProfileService;

    public SceneProfileController(SceneProfileService sceneProfileService) {
        this.sceneProfileService = sceneProfileService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Perms.USER_PROFILE_READ_OWN + "')")
    public R<List<SceneProfileVO>> listMine() {
        return R.ok(sceneProfileService.listMine());
    }

    /**
     * 默认场景，首页档案卡片用。
     *
     * <p>放在 /default 而不是 /{id} 的路径变体上，免得将来出现一个 id 恰好
     * 叫 default 的歧义——虽然 id 是数字，但显式区分更省心。
     */
    @GetMapping("/default")
    @PreAuthorize("hasAuthority('" + Perms.USER_PROFILE_READ_OWN + "')")
    public R<SceneProfileVO> getMyDefault() {
        return R.ok(sceneProfileService.getMyDefault());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Perms.USER_PROFILE_READ_OWN + "')")
    public R<SceneProfileVO> get(@PathVariable Long id) {
        return R.ok(sceneProfileService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Perms.USER_PROFILE_UPDATE_OWN + "')")
    public R<Long> create(@Valid @RequestBody SceneProfileSaveDTO dto) {
        return R.ok(sceneProfileService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Perms.USER_PROFILE_UPDATE_OWN + "')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SceneProfileSaveDTO dto) {
        sceneProfileService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Perms.USER_PROFILE_UPDATE_OWN + "')")
    public R<Void> remove(@PathVariable Long id) {
        sceneProfileService.remove(id);
        return R.ok();
    }

    @PutMapping("/{id}/default")
    @PreAuthorize("hasAuthority('" + Perms.USER_PROFILE_UPDATE_OWN + "')")
    public R<Void> setDefault(@PathVariable Long id) {
        sceneProfileService.setDefault(id);
        return R.ok();
    }
}
