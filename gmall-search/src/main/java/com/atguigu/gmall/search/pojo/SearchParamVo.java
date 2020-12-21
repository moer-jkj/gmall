package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParamVo {

    // 检索关键字
    private String keyWord;

    // 品牌过滤
    private List<Long> brandId;
    // 分类过滤
    private List<Long> categoryId;
    // 规格参数   porps=4:8G-12G&props=5:128G-512G
    private List<String> props;
    // 价格区间
    private Double priceFrom;
    private Double priceTo;
    // 显示是否有货
    private Boolean store;
    // 排序 0:默认综合排序 1:价格降序 2:价格升序 3:销量升序 4:新品降序

    private Integer sort;
    // 分页参数
    private Integer pageNum = 1;
    //
    private final Integer pageSize = 8;

}
