package com.atguigu.gmall.list.test;


import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class TestEsApiController {

    @Autowired
    RestHighLevelClient restHighLevelClient;

    @RequestMapping("testEs")
    public String testEs() throws IOException {
        String json = "";

        // dsl语句
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("name","穿越");
        boolQueryBuilder.must(matchQueryBuilder);
        TermQueryBuilder termQueryBuilder = new TermQueryBuilder("actors.firstName","徐");
        boolQueryBuilder.filter(termQueryBuilder);
        searchSourceBuilder.query(boolQueryBuilder);

        System.out.println(searchSourceBuilder.toString());

        // 封装请求对象
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder);
        searchRequest.indices("index1").types("movie");

        // 执行es查询命令
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        // 处理es返回结果
        SearchHits hits = searchResponse.getHits();
        if(null!=hits){
            for (SearchHit hit : hits) {
                json = hit.getSourceAsString();
                System.out.println(json);
            }
        }

        return json;
    }

}
