package com.atguigu.gmall.search.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;

@Data
@Document(indexName = "goods",type = "info",shards = 3,replicas = 2)
public class Goods {

    // 商品列表字段
    @Id
    @Field(type = FieldType.Long)
    private Long skuId;
    @Field(type = FieldType.Keyword,index = false)
    private String defaultImage;
    @Field(type = FieldType.Double)
    private double price;
    @Field(type = FieldType.Text,analyzer = "ik_max_word")
    private String title;
    @Field(type = FieldType.Keyword,index = false)
    private String subTitle;

    // 筛选排序字段
    @Field(type = FieldType.Long)
    private Long sales;  // 销量
    @Field(type = FieldType.Date)
    private Date createTime; // 新品排序
    @Field(type = FieldType.Boolean)
    private boolean store; //是否有货

    // 过滤字段
    // 品牌相关字段
    @Field(type = FieldType.Long)
    private Long brandId;
    @Field(type = FieldType.Keyword)
    private String brandName;
    @Field(type = FieldType.Keyword)
    private String logo;
    // 分类相关字段
    @Field(type = FieldType.Long)
    private Long categoryId;
    @Field(type = FieldType.Keyword)
    private String categoryName;
    // 规格参数相关字段
    @Field(type = FieldType.Nested)  // 嵌套类型
    private List<SearchAttrValueVo> searchAttrs;


}
