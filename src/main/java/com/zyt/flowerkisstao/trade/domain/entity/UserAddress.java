package com.zyt.flowerkisstao.trade.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收货地址。
 *
 * <p>单独一张表而不是往 {@code sys_user} 上加几列：一个人有家和公司两个地址是常态，
 * 挂在用户表上就只能存一个。
 *
 * <p>订单不引用这里的 id 而是存地址快照——用户删掉地址簿里那条之后，
 * 历史订单不该变成"寄往未知"。
 */
@Data
@TableName("user_address")
public class UserAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 sys_user.id。所有查询都必须带上它，否则会读到别人的地址 */
    private Long userId;

    private String receiver;

    private String phone;

    private String province;

    private String city;

    private String district;

    /** 详细地址（街道门牌） */
    private String detail;

    /** 1 默认地址，每用户至多一个 */
    private Integer isDefault;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 拼成一行存进订单快照，省市区之间不加分隔符更接近实际快递单写法 */
    public String fullAddress() {
        return province + city + district + " " + detail;
    }
}
