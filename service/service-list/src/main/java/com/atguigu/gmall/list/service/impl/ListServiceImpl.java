package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.list.repository.GoodsElasticsearchRepository;
import com.atguigu.gmall.list.service.ListService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.*;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ListServiceImpl implements ListService {


    @Autowired
    GoodsElasticsearchRepository goodsElasticsearchRepository;

    @Autowired
    ProductFeignClient productFeignClient;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    RestHighLevelClient restHighLevelClient;

    @Override
    public void cancelSale(String skuId) {

        // 调用删除api，根据skuId，删除es中的数据，skuId为es的主键
        Goods goods = new Goods();
        goods.setId(Long.parseLong(skuId));
        goodsElasticsearchRepository.delete(goods);
    }

    @Override
    public void onSale(String skuId) {

        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);

        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id() + "");

        List<BaseAttrInfo> baseAttrInfos = productFeignClient.getAttrList(skuId);

        BaseTrademark baseTrademark = productFeignClient.getTrademark(skuInfo.getTmId());

        // 从mysql中查询出es需要的数据封装给goods
        Goods goods = new Goods();
        if (null != baseAttrInfos) {
            List<SearchAttr> searchAttrList = baseAttrInfos.stream().map(baseAttrInfo -> {
                SearchAttr searchAttr = new SearchAttr();
                searchAttr.setAttrId(baseAttrInfo.getId());
                searchAttr.setAttrName(baseAttrInfo.getAttrName());
                //一个sku只对应一个属性值
                List<BaseAttrValue> baseAttrValueList = baseAttrInfo.getAttrValueList();
                searchAttr.setAttrValue(baseAttrValueList.get(0).getValueName());
                return searchAttr;
            }).collect(Collectors.toList());

            goods.setAttrs(searchAttrList);
        }

        if (baseTrademark != null) {
            goods.setTmId(skuInfo.getTmId());
            goods.setTmName(baseTrademark.getTmName());
            goods.setTmLogoUrl(baseTrademark.getLogoUrl());

        }


        if (categoryView != null) {
            goods.setCategory1Id(categoryView.getCategory1Id());
            goods.setCategory1Name(categoryView.getCategory1Name());
            goods.setCategory2Id(categoryView.getCategory2Id());
            goods.setCategory2Name(categoryView.getCategory2Name());
            goods.setCategory3Id(categoryView.getCategory3Id());
            goods.setCategory3Name(categoryView.getCategory3Name());
        }

        goods.setDefaultImg(skuInfo.getSkuDefaultImg());
        goods.setPrice(skuInfo.getPrice().doubleValue());// 调用查询价格的接口查询一次
        goods.setId(skuInfo.getId());
        goods.setTitle(skuInfo.getSkuName());
        goods.setCreateTime(new Date());


        // 调用es的api插入数据
        goodsElasticsearchRepository.save(goods);

    }

    @Override
    public void hotScore(String skuId) {

        Long hotScore = 0L;

        // 缓存查询原来的热度值
        // 热度值加一hotScore++;
        hotScore = redisTemplate.opsForZSet().incrementScore("hotScore", "sku:" + skuId, 1).longValue();

        // 判断是否达到同步es的阈值
        if (hotScore % 10 == 0) {
            // 修改es
            Optional<Goods> goodsOptional = goodsElasticsearchRepository.findById(Long.parseLong(skuId));
            Goods goods = goodsOptional.get();
            goods.setHotScore(hotScore);
            goodsElasticsearchRepository.save(goods);
        }

    }

    @Override
    public SearchResponseVo list(SearchParam searchParam) {

        SearchResponseVo searchResponseVo = new SearchResponseVo();

        // 生成dsl搜索请求
        SearchRequest searchRequest = buildQueryDsl(searchParam);


        // 执行查询操作，restHighLevelClient.search
        SearchResponse search = null;
        try {
            search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 解析返回结果
        searchResponseVo = parseSearchResult(search);

        return searchResponseVo;
    }

    /***
     * 解析返回结果
     * @param search
     * @return
     */
    private SearchResponseVo parseSearchResult(SearchResponse search) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        // 解析返回结果
        SearchHits hits = search.getHits();
        List<Goods> goods = new ArrayList<>();
        for (SearchHit hit : hits) {

            // 解析商品数据
            String sourceAsString = hit.getSourceAsString();
            Goods good = JSON.parseObject(sourceAsString, Goods.class);

            // 解析高亮
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (null != highlightFields && highlightFields.size() > 0) {
                HighlightField title = highlightFields.get("title");
                String hightlightTitle = title.fragments()[0].string();
                good.setTitle(hightlightTitle);
            }

            goods.add(good);

        }
        searchResponseVo.setGoodsList(goods);

        // 解析商标聚合函数
        Map<String, Aggregation> stringAggregationMap = search.getAggregations().asMap();
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) stringAggregationMap.get("tmIdAgg");
        List<SearchResponseTmVo> searchResponseTmVos = tmIdAgg.getBuckets().stream().map(bucket -> {
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();

            // 商标id
            long tmId = bucket.getKeyAsNumber().longValue();
            searchResponseTmVo.setTmId(tmId);

            // 商标名称和LogoUrl
            Map<String, Aggregation> stringAggregationMapTmIdAgg = bucket.getAggregations().asMap();
            ParsedStringTerms tmNameAgg = (ParsedStringTerms) stringAggregationMapTmIdAgg.get("tmNameAgg");
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);

            // 和LogoUrl
            ParsedStringTerms tmLogoUrlAgg = (ParsedStringTerms) stringAggregationMapTmIdAgg.get("tmLogoUrlAgg");
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmLogoUrl(tmLogoUrl);

            return searchResponseTmVo;
        }).collect(Collectors.toList());


        // 解析属性聚合函数
        ParsedNested attrsAgg = (ParsedNested) stringAggregationMap.get("attrsAgg");
        ParsedLongTerms attrIdAgg = (ParsedLongTerms) attrsAgg.getAggregations().get("attrIdAgg");
        List<SearchResponseAttrVo> searchResponseAttrVos = attrIdAgg.getBuckets().stream().map(bucket -> {
            SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();

            // 属性id
            long attrId = bucket.getKeyAsNumber().longValue();
            searchResponseAttrVo.setAttrId(attrId);

            // 属性名称
            Map<String, Aggregation> stringAggregationMapAttrIdAgg = bucket.getAggregations().asMap();
            ParsedStringTerms attrNameAgg = (ParsedStringTerms) stringAggregationMapAttrIdAgg.get("attrNameAgg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseAttrVo.setAttrName(attrName);

            // 属性值
            ParsedStringTerms attrValueAgg = (ParsedStringTerms) stringAggregationMapAttrIdAgg.get("attrValueAgg");
            List<String> attrValueList = attrValueAgg.getBuckets().stream().map(attrValueBucket -> {
                String attrValue = attrValueBucket.getKeyAsString();
                return attrValue;
            }).collect(Collectors.toList());

            searchResponseAttrVo.setAttrValueList(attrValueList);
            return searchResponseAttrVo;
        }).collect(Collectors.toList());

        searchResponseVo.setAttrsList(searchResponseAttrVos);
        searchResponseVo.setTrademarkList(searchResponseTmVos);

        return searchResponseVo;
    }

    /***
     * 封装dsl搜索请求
     * @param searchParam
     * @return
     */
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        // 搜索参数，三级分类id和keyword有且只有一个
        String[] props = searchParam.getProps();// 属性抽取
        String trademark = searchParam.getTrademark();// 商标抽取
        String order = searchParam.getOrder();
        String keyword = searchParam.getKeyword();// 特殊可选
        Long category3Id = searchParam.getCategory3Id();// 特殊可选

        // 定义dsl语句
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        if (StringUtils.isNotBlank(keyword)) {
            boolQueryBuilder.must(new MatchQueryBuilder("title", keyword));
            // 设置高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.preTags("<span style='color:red;font-weight:bolder;'>");
            highlightBuilder.field("title");
            highlightBuilder.postTags("</span>");
            searchSourceBuilder.highlighter(highlightBuilder);
        }
        if (null != category3Id && category3Id > 0) {
            boolQueryBuilder.filter(new TermQueryBuilder("category3Id", category3Id));
        }

        if(null!=props&&props.length>0){
            for (String prop : props) {
                String[] split = prop.split(":");
                String attrId = split[0];
                String attrValue = split[1];
                String attrName = split[2];

                BoolQueryBuilder boolQueryBuilderAttrs = new BoolQueryBuilder();
                boolQueryBuilderAttrs.filter(new TermQueryBuilder("attrs.attrId", attrId));
                boolQueryBuilderAttrs.must(new MatchQueryBuilder("attrs.attrValue", attrValue));
                boolQueryBuilderAttrs.must(new MatchQueryBuilder("attrs.attrName", attrName));
                // 第二层query，nested的query
                NestedQueryBuilder nestedQueryBuilder = new NestedQueryBuilder("attrs", boolQueryBuilderAttrs,ScoreMode.None);
                boolQueryBuilder.must(nestedQueryBuilder);
            }
        }

        if(StringUtils.isNotBlank(trademark)){
            Long tmId = Long.parseLong(trademark.split(":")[0]);
            boolQueryBuilder.filter(new TermQueryBuilder("tmId",tmId));
        }

        if(StringUtils.isNotBlank(order)){
            String[] split = order.split(":");
            String type = split[0];
            String sort = split[1];
            if(type.equals("1")){
                type = "hotScore";
            }else if(type.equals("2")){
                type = "price";
            }
            searchSourceBuilder.sort(type, sort.equals("asc")?SortOrder.ASC:SortOrder.DESC);
        }else {
            // 默认按照热度值降序排列
            searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        }

        searchSourceBuilder.query(boolQueryBuilder);

        // 商标聚合函数
        TermsAggregationBuilder termsAggregationBuilderTmIdAgg = AggregationBuilders.terms("tmIdAgg").field("tmId")
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"));

        // 属性聚合函数
        NestedAggregationBuilder nestedAggregationBuilder = AggregationBuilders.nested("attrsAgg", "attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName")));


        searchSourceBuilder.aggregation(nestedAggregationBuilder);
        searchSourceBuilder.aggregation(termsAggregationBuilderTmIdAgg);

        System.out.println(searchSourceBuilder.toString());

        // 封装SearchRequest
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("goods");
        searchRequest.types("info");
        searchRequest.source(searchSourceBuilder);
        return searchRequest;
    }

}
