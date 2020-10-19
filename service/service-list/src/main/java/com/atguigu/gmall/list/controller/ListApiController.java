package com.atguigu.gmall.list.controller;


import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.service.ListService;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/list/")
public class ListApiController {

    @Autowired
    ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    ListService listService;

    @RequestMapping("cancelSale/{skuId}")
    void cancelSale(@PathVariable("skuId") String skuId){
        listService.cancelSale(skuId);
    }

    @RequestMapping("onSale/{skuId}")
    void onSale(@PathVariable("skuId") String skuId){
        listService.onSale(skuId);
    }

    @RequestMapping("createIndex")
    public Result createIndex(){

        // 调用es的api导入数据结构
        elasticsearchRestTemplate.createIndex(Goods.class);
        elasticsearchRestTemplate.putMapping(Goods.class);

        return Result.ok();
    }

    /**
     * 更新商品热度值
     * @param skuId
     */
    @RequestMapping("hotScore/{skuId}")
    void hotScore(@PathVariable("skuId") String skuId){
        listService.hotScore(skuId);
    }

    /**
     * 调用搜索查询的功能
     * @param searchParam
     * @return
     */

    @PostMapping("list")
    Result list(@RequestBody SearchParam searchParam) {
        SearchResponseVo searchResponseVo = listService.list(searchParam);
        return Result.ok(searchResponseVo);
    }
}
