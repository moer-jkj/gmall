package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author moerjkj
 * @email moermanske@163.com
 * @date 2021-01-09 11:46:28
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
	
}
