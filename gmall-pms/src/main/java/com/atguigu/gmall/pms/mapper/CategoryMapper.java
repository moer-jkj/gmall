package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 商品三级分类
 * 
 * @author moerjkj
 * @email moermanske@163.com
 * @date 2020-12-14 18:37:24
 */
@Mapper
public interface CategoryMapper extends BaseMapper<CategoryEntity> {



    List<CategoryEntity> queryCategoriesWithSubsByPid(Long parentId);
}
