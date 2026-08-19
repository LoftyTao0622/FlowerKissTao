package com.zyt.flowerkisstao.trade.application.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import com.zyt.flowerkisstao.shared.security.CurrentUser;
import com.zyt.flowerkisstao.trade.application.service.AddressService;
import com.zyt.flowerkisstao.trade.domain.entity.UserAddress;
import com.zyt.flowerkisstao.trade.infrastructure.mapper.UserAddressMapper;
import com.zyt.flowerkisstao.trade.web.dto.AddressSaveDTO;
import com.zyt.flowerkisstao.trade.web.vo.AddressVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    private final UserAddressMapper addressMapper;

    public AddressServiceImpl(UserAddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    @Override
    public List<AddressVO> listMine() {
        return listByUser(CurrentUser.requireUserId()).stream()
                .map(TradeConverter::toVO)
                .toList();
    }

    @Override
    public AddressVO getMyDefault() {
        Long userId = CurrentUser.requireUserId();
        UserAddress address = addressMapper.selectOne(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1)
                .last("LIMIT 1"));

        // 有地址但没标默认（历史数据可能这样），退回最早的一条，
        // 而不是让结算页显示"还没有收货地址"
        if (address == null) {
            List<UserAddress> all = listByUser(userId);
            if (all.isEmpty()) {
                return null;
            }
            address = all.get(0);
        }
        return TradeConverter.toVO(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AddressSaveDTO dto) {
        Long userId = CurrentUser.requireUserId();

        UserAddress address = new UserAddress();
        address.setUserId(userId);
        applyDto(address, dto);
        // 第一条地址自动成为默认，否则用户建完不设默认，结算页还得再点一次
        boolean first = countByUser(userId) == 0;
        address.setIsDefault(first ? 1 : 0);

        addressMapper.insert(address);
        return address.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AddressSaveDTO dto) {
        requireOwnedAddress(id);

        UserAddress update = new UserAddress();
        update.setId(id);
        applyDto(update, dto);
        // isDefault 由 setDefault 单独管，避免编辑表单顺手改掉默认地址
        addressMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        UserAddress existing = requireOwnedAddress(id);
        addressMapper.deleteById(id);

        // 删掉的是默认地址时，把剩下最早的一条升为默认。
        // 不补的话用户下次结算会发现"一条地址都没默认"，还得手动再点一次
        if (existing.getIsDefault() != null && existing.getIsDefault() == 1) {
            List<UserAddress> rest = listByUser(existing.getUserId());
            if (!rest.isEmpty()) {
                UserAddress promoted = new UserAddress();
                promoted.setId(rest.get(0).getId());
                promoted.setIsDefault(1);
                addressMapper.updateById(promoted);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        UserAddress target = requireOwnedAddress(id);

        // 先把这个人的全部地址清零再置一，保证"每用户至多一个默认"。
        // 只置一不清零的话会出现两个默认，getMyDefault 取到哪个就看数据库心情了
        addressMapper.update(null, Wrappers.<UserAddress>lambdaUpdate()
                .eq(UserAddress::getUserId, target.getUserId())
                .set(UserAddress::getIsDefault, 0));

        UserAddress update = new UserAddress();
        update.setId(id);
        update.setIsDefault(1);
        addressMapper.updateById(update);
    }

    // ================================================================

    /** 默认地址排最前，其余按创建时间。结算页第一条就是要用的那条 */
    private List<UserAddress> listByUser(Long userId) {
        return addressMapper.selectList(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByAsc(UserAddress::getId));
    }

    private long countByUser(Long userId) {
        return addressMapper.selectCount(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId));
    }

    /**
     * 取地址并确认归属。
     *
     * <p>不属于当前用户时抛 ADDRESS_NOT_FOUND 而非 FORBIDDEN：返回 403 等于确认
     * "这个 id 确实存在"，把别人的地址 id 空间暴露给了调用方。与画像模块同一口径。
     */
    private UserAddress requireOwnedAddress(Long id) {
        UserAddress address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(CurrentUser.requireUserId())) {
            throw new BizException(ErrorCode.ADDRESS_NOT_FOUND, "收货地址不存在");
        }
        return address;
    }

    private void applyDto(UserAddress address, AddressSaveDTO dto) {
        address.setReceiver(dto.getReceiver());
        address.setPhone(dto.getPhone());
        address.setProvince(dto.getProvince());
        address.setCity(dto.getCity());
        address.setDistrict(dto.getDistrict());
        address.setDetail(dto.getDetail());
    }
}
