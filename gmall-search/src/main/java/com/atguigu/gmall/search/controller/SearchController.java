package com.atguigu.gmall.search.controller;


import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.atguigu.gmall.search.service.SearchSevice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("search")
public class SearchController {

    @Autowired
    private SearchSevice searchSevice;

    @GetMapping
    public ResponseVo<Object> search(SearchParamVo paramVo){
        SearchResponseVo responseVo = this.searchSevice.search(paramVo);
        return ResponseVo.ok(responseVo);
    }

}
