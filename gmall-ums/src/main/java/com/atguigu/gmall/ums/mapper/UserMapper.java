package com.atguigu.gmall.ums.mapper;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表
 * 
 * @author moerjkj
 * @email moermanske@163.com
 * @date 2020-12-30 20:57:38
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
	
}
