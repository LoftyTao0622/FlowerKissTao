package com.zyt.flowerkisstao.trade.web.controller;

import com.zyt.flowerkisstao.shared.security.Perms;
import com.zyt.flowerkisstao.shared.web.R;
import com.zyt.flowerkisstao.trade.application.service.AddressService;
import com.zyt.flowerkisstao.trade.web.dto.AddressSaveDTO;
import com.zyt.flowerkisstao.trade.web.vo.AddressVO;
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
 * 我的收货地址簿。
 *
 * <p>权限点沿用 user:profile:read-own / update-own——地址属于个人资料，方案模块 1
 * 原文就是把"个人资料和收货地址维护"放在一起说的，不必为它新增权限点。
 *
 * <p>接口没有 userId 参数：一律取当前登录人。
 */
@RestController
@RequestMapping("/api/addresses")
@Validated
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Perms.USER_PROFILE_READ_OWN + "')")
    public R<List<AddressVO>> listMine() {
        return R.ok(addressService.listMine());
    }

    /** 默认地址，结算页进来先要它 */
    @GetMapping("/default")
    @PreAuthorize("hasAuthority('" + Perms.USER_PROFILE_READ_OWN + "')")
    public R<AddressVO> getMyDefault() {
        return R.ok(addressService.getMyDefault());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Perms.USER_PROFILE_UPDATE_OWN + "')")
    public R<Long> create(@Valid @RequestBody AddressSaveDTO dto) {
        return R.ok(addressService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Perms.USER_PROFILE_UPDATE_OWN + "')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody AddressSaveDTO dto) {
        addressService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Perms.USER_PROFILE_UPDATE_OWN + "')")
    public R<Void> remove(@PathVariable Long id) {
        addressService.remove(id);
        return R.ok();
    }

    @PutMapping("/{id}/default")
    @PreAuthorize("hasAuthority('" + Perms.USER_PROFILE_UPDATE_OWN + "')")
    public R<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(id);
        return R.ok();
    }
}
