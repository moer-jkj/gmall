package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class OrderConfirmVo {

    Set<Map.Entry<String,Object>> entrySet;

    // 收货地址列表
    private List<UserAddressEntity> addresses;

    // 送货清单
    private List<?> items;

    // 购物积分信息，优惠 ums_member表中的 integration字段
    private Integer bounds;

    // 防重唯一标识  做幂等性判断
    private String orderToken;

}

