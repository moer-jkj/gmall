package com.atguigu.gmall.pms.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.CategoryMapper;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryEntity> implements CategoryService {


    @Autowired
    CategoryMapper categoryMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CategoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<CategoryEntity> queryCategory(Long parentId) {
        // 构建查询条件
        QueryWrapper<CategoryEntity> wrapper = new QueryWrapper<>();
        // 如果 parentId为-1，则说明该用户没有传该字段，查询所有
        if (parentId != -1){
            wrapper.eq("parent_id",parentId);
        }
        return this.list(wrapper);
    }

    @Override
    public List<CategoryEntity> queryCategoriesWithSubsByPid(Long pid) {
        return this.categoryMapper.queryCategoriesWithSubsByPid(pid);
    }

    // 根据分类id查询三级分类
    @Override
    public List<CategoryEntity> queryLv123CategoriesByCid(Long id) {
        // 查询三级分类
        CategoryEntity lvl3Category = this.getById(id);
        if (lvl3Category == null) {
            return null;
        }
        // 查询二级分类
        CategoryEntity lvl2Category = this.getById(lvl3Category.getParentId());
        // 三级分类存在，则必然存在父分类，也就是二级分类，所以不需要判空
        CategoryEntity lvl1Category = this.getById(lvl2Category.getParentId());


        return Arrays.asList(lvl1Category,lvl2Category,lvl3Category);
    }

}