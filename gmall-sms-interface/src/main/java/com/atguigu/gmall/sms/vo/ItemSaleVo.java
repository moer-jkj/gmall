package com.atguigu.gmall.sms.vo;

import lombok.Data;

@Data
public class ItemSaleVo {

    // 促销类型  积分 满减 打折
    private String type;

    // 描述信息
    private String desc;


}
