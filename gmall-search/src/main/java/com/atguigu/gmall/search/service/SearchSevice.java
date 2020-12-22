package com.atguigu.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
public class SearchSevice {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public SearchResponseVo search(SearchParamVo paramVo) {

        try {
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"},buildDsl(paramVo));
            SearchResponse response = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            // 解析结果集
            SearchResponseVo responseVo = this.parseResult(response);
            responseVo.setPageNum(paramVo.getPageNum());
            responseVo.setPageSize(paramVo.getPageSize());
            return responseVo;
        } catch (IOException e) {
            e.printStackTrace();

        }
        return null;
    }

    // 将response解析成responseVo
    private SearchResponseVo parseResult(SearchResponse response) {
        SearchResponseVo responseVo = new SearchResponseVo();

        SearchHits hits = response.getHits();
        // 总命中的记录数
        responseVo.setTotal(hits.getTotalHits());

        SearchHit[] hitsHits = hits.getHits();
        List<Goods> goodsList = Stream.of(hitsHits).map(hitsHit -> {
            // 获取内层hits的_source 数据
            String goodsJson = hitsHit.getSourceAsString();
            // 反序列化为goods对象
            Goods goods = JSON.parseObject(goodsJson, Goods.class);

            // 获取高亮的title覆盖掉普通title
            Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
            HighlightField highlightField = highlightFields.get("title");
            String highlightTitle = highlightField.getFragments()[0].toString();
            if (StringUtils.isNotBlank(highlightTitle)){
                goods.setTitle(highlightTitle);
            }

            return goods;
        }).collect(Collectors.toList());
        responseVo.setGoodsList(goodsList);

        // 聚合结果集的解析
        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();
        // 1. 解析聚合结果集，获取品牌》
        // {attrId: null, attrName: "品牌"， attrValues: [{id: 1, name: 尚硅谷, logo: http://www.atguigu.com/logo.gif}, {}]}
        ParsedLongTerms brandIdAgg = (ParsedLongTerms)aggregationMap.get("brandIdAgg");
        List<? extends Terms.Bucket> buckets = brandIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(buckets)){
            List<BrandEntity> brands = buckets.stream().map(bucket -> { // {id: 1, name: 尚硅谷, logo: http://www.atguigu.com/logo.gif}
                // 为了得到指定格式的json字符串，创建了一个map
                BrandEntity brandEntity = new BrandEntity();
                // 获取brandIdAgg中的key，这个key就是品牌的id
                Long brandId = ((Terms.Bucket) bucket).getKeyAsNumber().longValue();
                brandEntity.setId(brandId);
                // 解析品牌名称的子聚合，获取品牌名称
                Map<String, Aggregation> brandAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
                ParsedStringTerms brandNameAgg = (ParsedStringTerms)brandAggregationMap.get("brandNameAgg");
                brandEntity.setName(brandNameAgg.getBuckets().get(0).getKeyAsString());
                // 解析品牌logo的子聚合，获取品牌 的logo
                ParsedStringTerms logoAgg = (ParsedStringTerms)brandAggregationMap.get("logoAgg");
                List<? extends Terms.Bucket> logoAggBuckets = logoAgg.getBuckets();
                if (!CollectionUtils.isEmpty(logoAggBuckets)){
                    brandEntity.setLogo(logoAggBuckets.get(0).getKeyAsString());
                }
                // 把map反序列化为json字符串
                return brandEntity;
            }).collect(Collectors.toList());
            responseVo.setBrands(brands);
        }

        // 2. 解析聚合结果集，获取分类
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms)aggregationMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryIdAggBuckets = categoryIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(categoryIdAggBuckets)){
            List<CategoryEntity> categories = categoryIdAggBuckets.stream().map(bucket -> { // {id: 225, name: 手机}
                CategoryEntity categoryEntity = new CategoryEntity();
                // 获取bucket的key，key就是分类的id
                Long categoryId = ((Terms.Bucket) bucket).getKeyAsNumber().longValue();
                categoryEntity.setId(categoryId);
                // 解析分类名称的子聚合，获取分类名称
                ParsedStringTerms categoryNameAgg = (ParsedStringTerms)((Terms.Bucket) bucket).getAggregations().get("categoryNameAgg");
                categoryEntity.setName(categoryNameAgg.getBuckets().get(0).getKeyAsString());
                return categoryEntity;
            }).collect(Collectors.toList());
            responseVo.setCategories(categories);
        }

        // 3. 解析聚合结果集，获取规格参数
        ParsedNested attrAgg = (ParsedNested)aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = (ParsedLongTerms)attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(attrIdAggBuckets)) {
            List<SearchResponseAttrVo> filters = attrIdAggBuckets.stream().map(bucket -> {
                SearchResponseAttrVo responseAttrVo = new SearchResponseAttrVo();
                // 规格参数id
                responseAttrVo.setAttrId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                // 规格参数的名称
                ParsedStringTerms attrNameAgg = (ParsedStringTerms)((Terms.Bucket) bucket).getAggregations().get("attrNameAgg");
                responseAttrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());
                // 规格参数值
                ParsedStringTerms attrValueAgg = (ParsedStringTerms)((Terms.Bucket) bucket).getAggregations().get("attrValueAgg");
                List<? extends Terms.Bucket> attrValueAggBuckets = attrValueAgg.getBuckets();
                if (!CollectionUtils.isEmpty(attrValueAggBuckets)){
                    List<String> attrValues = attrValueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                    responseAttrVo.setAttrValues(attrValues);
                }
                return responseAttrVo;
            }).collect(Collectors.toList());
            responseVo.setFilters(filters);
        }

        return responseVo;
    }
//    private SearchResponseVo parseResult(SearchResponse response) {
//        SearchResponseVo responseVo = new SearchResponseVo();
//        // 解析 hits
//
//        SearchHits hits = response.getHits();
//
//        // 总命中数的记录
//        responseVo.setTotal(hits.getTotalHits());
//
//        // 解析出当前页的数据
//        SearchHit[] hitsHits = hits.getHits();
//        List<Goods> goodsList = Stream.of(hitsHits).map(hitsHit -> {
//            // 获取内层 hits 的 _source数据
//            String goodsJson = hitsHit.getSourceAsString();
//            // 序列化为goods对象
//            Goods goods = JSON.parseObject(goodsJson, Goods.class);
//            // 获取高亮的title覆盖掉普通的title
//            // 高亮字段
//            Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
//            HighlightField highlightField = highlightFields.get("title");
//            String highlightTitle = highlightField.getFragments()[0].toString();
//            goods.setTitle(highlightTitle);
//            return goods;
//        }).collect(Collectors.toList());
//        responseVo.setGoodsList(goodsList);
//
//
//        // 解析聚合结果集,获取所有集合，以map形式接收
//        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();
//
//
//        ParsedLongTerms brandIdAgg = (ParsedLongTerms)aggregationMap.get("brandIdAgg");
//        List<? extends Terms.Bucket> buckets = brandIdAgg.getBuckets();
//        if (!CollectionUtils.isEmpty(buckets)){
//            responseVo.setBrands(buckets.stream().map(bucket -> {
//                BrandEntity brandEntity = new BrandEntity();
//                brandEntity.setId(bucket.getKeyAsNumber().longValue());
//                // 获取品牌id的自聚合
//                Map<String, Aggregation> subAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
//                // 解析品牌名称自聚合获取品牌名称
//                ParsedStringTerms brandNameAgg = (ParsedStringTerms)subAggregationMap.get("brandNameAgg");
//                // 名称的bucket
//                List<? extends Terms.Bucket> nameAggBuckets = brandNameAgg.getBuckets();
//                if (!CollectionUtils.isEmpty(nameAggBuckets)){
//                    brandEntity.setName(nameAggBuckets.get(0).getKeyAsString());
//                }
//
//                // 解析品牌logo子聚合获取品牌logo
//                ParsedStringTerms logoAgg = (ParsedStringTerms)subAggregationMap.get("logoAgg");
//                // logo的bucket
//                List<? extends Terms.Bucket> logoAggBuckets = logoAgg.getBuckets();
//                if (!CollectionUtils.isEmpty(logoAggBuckets)){
//                    brandEntity.setLogo(logoAggBuckets.get(0).getKeyAsString());
//                }
//
//                return brandEntity;
//            }).collect(Collectors.toList()));
//        }
//
//        ParsedLongTerms categoryIdAgg = (ParsedLongTerms)aggregationMap.get("categoryIdAgg");
//        List<? extends Terms.Bucket> categoryIdAggBuckets = categoryIdAgg.getBuckets();
//        if (!CollectionUtils.isEmpty(categoryIdAggBuckets)){
//            responseVo.setCategorys(categoryIdAggBuckets.stream().map(bucket -> {
//                CategoryEntity categoryEntity = new CategoryEntity();
//                categoryEntity.setId(bucket.getKeyAsNumber().longValue());
//                // 通过子聚合获取分类名称
//                ParsedStringTerms categoryNameAgg = (ParsedStringTerms)((Terms.Bucket)bucket).getAggregations().get("categoryNameAgg");
//                List<? extends Terms.Bucket> nameAggBuckets = categoryNameAgg.getBuckets();
//                if (!CollectionUtils.isEmpty(nameAggBuckets)){
//                    categoryEntity.setName(nameAggBuckets.get(0).getKeyAsString());
//                }
//
//                return categoryEntity;
//            }).collect(Collectors.toList()));
//        }
//
//
//        // 获取规格参数聚合并解析出参数列表
//        ParsedNested attrAgg = (ParsedNested)aggregationMap.get("attrAgg");
//        // 获取嵌套聚合的子聚合
//        ParsedLongTerms attrIdAgg = (ParsedLongTerms)attrAgg.getAggregations().get("attrIdAgg");
//        List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
//        if (!CollectionUtils.isEmpty(attrIdAggBuckets)){
//
//            responseVo.setFilters(attrIdAggBuckets.stream().map(bucket -> {
//                SearchResponseAttrVo responseAttrVo = new SearchResponseAttrVo();
//                // 获取桶中的key
//                responseAttrVo.setAttrId(bucket.getKeyAsNumber().longValue());
//                // 获取attrIdAgg的子聚合 attrNameAgg 和  attrValueAgg
//                Map<String, Aggregation> subAggregationMap = bucket.getAggregations().asMap();
//                // 获取规格参数名称的子聚合，解析出规格参数名
//                ParsedStringTerms attrNameAgg = (ParsedStringTerms) subAggregationMap.get("attrNameAgg");
//                List<? extends Terms.Bucket> nameAggBuckets = attrNameAgg.getBuckets();
//                if (!CollectionUtils.isEmpty(nameAggBuckets)){
//                    responseAttrVo.setAttrName(nameAggBuckets.get(0).getKeyAsString());
//
//                }
//                // 获取规格参数值的子聚合
//                ParsedStringTerms attrValueAgg = (ParsedStringTerms) subAggregationMap.get("attrValueAgg");
//                List<? extends Terms.Bucket> valueAggBuckets = attrValueAgg.getBuckets();
//                if (!CollectionUtils.isEmpty(valueAggBuckets)){
//                    responseAttrVo.setAttrValues(valueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList()));
//
//                }
//
//                return responseAttrVo;
//            }).collect(Collectors.toList()));
//
//        }
//
//
//        return responseVo;
//    }

    private SearchSourceBuilder buildDsl(SearchParamVo paramVo){
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        String keyWord = paramVo.getKeyword();
        if (StringUtils.isBlank(keyWord)){
            return sourceBuilder;
        }
        // 1. 构建查询及过滤条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        sourceBuilder.query(boolQueryBuilder);

        // 1.1. 构建匹配查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("title",keyWord).operator(Operator.AND));

        // 1.2. 构建过滤条件
        // 1.2.1 品牌过滤
        List<Long> brandId = paramVo.getBrandId();
        if (!CollectionUtils.isEmpty(brandId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",brandId));
        }
        // 1.2.2 分类过滤
        List<Long> categoryId = paramVo.getCategoryId();
        if (!CollectionUtils.isEmpty(categoryId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId",categoryId));
        }
        // 1.2.3 价格区间过滤
        Double priceFrom = paramVo.getPriceFrom();
        Double priceTo = paramVo.getPriceTo();
        if (priceFrom != null || priceTo != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            if (priceFrom != null){
                rangeQuery.gte(priceFrom);
            }
            if (priceTo != null) {
                rangeQuery.lte(priceTo);
            }
            boolQueryBuilder.filter(rangeQuery);
        }
        // 1.2.4 是否有货
        Boolean store = paramVo.getStore();
        if (store != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("store",store));
        }

        // 1.2.5 规格参数  porps=4:8G-12G&props=5:128G-512G
        List<String> props = paramVo.getProps();
        if (!CollectionUtils.isEmpty(props)){
            props.forEach(prop -> {  // 4:8G-12G
                String[] attr = StringUtils.split(prop, ":");
                if (attr != null && attr.length == 2){
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    // 规格参数id单词条查询条件
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId",attr[0]));
                    // 规格参数值多次条查询条件
                    String[] attrValue = StringUtils.split(attr[1], "-");
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue",attrValue));
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs",boolQuery, ScoreMode.None));
                }
            });
        }
        // 2. 排序
        // 排序 0:默认综合排序 1:价格降序 2:价格升序 3:销量升序 4:新品降序
        Integer sort = paramVo.getSort();
        if (sort != null){
            switch (sort){
                case 1:
                    sourceBuilder.sort("price", SortOrder.DESC);
                    break;
                case 2:
                    sourceBuilder.sort("price", SortOrder.ASC);
                    break;
                case 3:
                    sourceBuilder.sort("sales", SortOrder.DESC);
                    break;
                case 4:
                    sourceBuilder.sort("createTime", SortOrder.DESC);
                    break;
                default:
                    break;
            }
        }

        // 3. 分页
        Integer pageNum = paramVo.getPageNum();
        Integer pageSize = paramVo.getPageSize();
        sourceBuilder.from((pageNum - 1) * pageSize );
        sourceBuilder.size(pageSize);
        // 4. 高亮
        sourceBuilder.highlighter(new HighlightBuilder().field("title")
                .preTags("<font style='coloe:red'>").postTags("</font>"));
        // 5. 聚合
        // 5.1. 品牌聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo"))
        );
        // 5.2. 分类聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName"))
        );
        // 5.3. 规格参数的嵌套聚合
        sourceBuilder.aggregation(
                AggregationBuilders.nested("attrAgg","searchAttrs")
                        .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                                .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                                .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue")))
        );

        // 6.结果集过滤
        sourceBuilder.fetchSource(new String[]{"skuId","defaultImage","title","subTitle","price"},null);
        System.out.println(sourceBuilder);
        return sourceBuilder;
    }
}
