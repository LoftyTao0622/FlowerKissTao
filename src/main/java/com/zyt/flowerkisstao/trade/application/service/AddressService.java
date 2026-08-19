package com.zyt.flowerkisstao.trade.application.service;

import com.zyt.flowerkisstao.trade.web.dto.AddressSaveDTO;
import com.zyt.flowerkisstao.trade.web.vo.AddressVO;

import java.util.List;

/**
 * 收货地址簿。方案模块 1 原文提到"个人资料和收货地址维护"，但地址真正被消费是在
 * 下单环节，所以实现放在 trade 包下。
 */
public interface AddressService {

    /** 我的全部地址，默认地址排最前 */
    List<AddressVO> listMine();

    /** 我的默认地址。一条都没有时返回 null，由前端引导去新增 */
    AddressVO getMyDefault();

    Long create(AddressSaveDTO dto);

    void update(Long id, AddressSaveDTO dto);

    void remove(Long id);

    void setDefault(Long id);
}
