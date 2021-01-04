package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class IndexController {



    @Autowired
    private IndexService indexService;

    @GetMapping({"index.html","/"})
    public String toIndex(Model model, HttpServletRequest request){
        System.out.println(request.getHeader("userId"));

        // 响应三级分类数据
        List<CategoryEntity> categoryEntityList = this.indexService.queryLvl1CategoryByPid();
        model.addAttribute("categories",categoryEntityList);
        // 广告
        return "index";
    }

    @GetMapping("index/cates/{pid}")
    @ResponseBody
    public ResponseVo<List<CategoryEntity>> queryLvl2CategoriesWithSubById(@PathVariable Long pid){
        List<CategoryEntity> categoryEntities = this.indexService.queryLvl2CategoriesWithSubById(pid);
        return ResponseVo.ok(categoryEntities);
    }

    @GetMapping("index/testlock")
    @ResponseBody
    public void test(){
        this.indexService.testLock();
    }

}
