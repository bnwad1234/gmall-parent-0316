package com.atguigu.gmall.product.controller;


import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/product")
@CrossOrigin
public class ProductApiController {


    @Autowired
    BaseCategoryService baseCategoryService;

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    SpuInfoService spuInfoService;

    @Autowired
    BaseTrademarkService baseTrademarkService;

    @Autowired
    BaseAttrInfoService baseAttrInfoService;

    @RequestMapping("inner/getTrademark/{tmId}")
    BaseTrademark getTrademark(@PathVariable("tmId") Long tmId){
        BaseTrademark baseTrademark = baseTrademarkService.getTrademark(tmId);

        return baseTrademark;
    }

    @RequestMapping("inner/getAttrList/{skuId}")
    List<BaseAttrInfo> getAttrList(@PathVariable("skuId") String skuId){
        List<BaseAttrInfo> baseAttrInfos = baseAttrInfoService.getAttrList(skuId);

        return baseAttrInfos;
    }


    @RequestMapping("inner/testProductApi")
    public String testProductApi(){
        return "testProductApi";
    }


    @RequestMapping("auth/testProductApi2")
    public String testProductApi2(){
        return "testProductApi2";
    }



    @RequestMapping("inner/getCategoryView/{category3Id}")
    BaseCategoryView getCategoryView(@PathVariable("category3Id") String category3Id){

        BaseCategoryView baseCategoryView = baseCategoryService.getCategoryView(category3Id);

        return baseCategoryView;
    }

    @RequestMapping("inner/getSkuInfo/{skuId}")
    SkuInfo getSkuInfo(@PathVariable("skuId")  String skuId){
        SkuInfo skuInfo = skuInfoService.getSkuInfo(skuId);
        return skuInfo;
    }

    @RequestMapping("inner/getSkuPrice/{skuId}")
    BigDecimal getSkuPrice(@PathVariable("skuId") String skuId){
        BigDecimal price = skuInfoService.getSkuPrice(skuId);
        return price;
    }

    @RequestMapping("inner/getMySpuSaleAttrs/{spuId}/{skuId}")
    List<SpuSaleAttr> getMySpuSaleAttrs(@PathVariable("spuId") Long spuId,@PathVariable("skuId") Long skuId){

        List<SpuSaleAttr> spuSaleAttrs = spuInfoService.getSpuSaleAttrListCheckBySku(spuId,skuId);//spuInfoService.getMySpuSaleAttrs(spuId);

        return spuSaleAttrs;
    }

    @RequestMapping("inner/getSkuValueIdsMap/{spuId}")
    Map<String,String> getSkuValueIdsMap(@PathVariable("spuId") Long spuId){
        Map<String,String> valueIdsMap = skuInfoService.getSkuValueIdsMap(spuId);

        return valueIdsMap;
    }

    @RequestMapping("getBaseCategoryList")
    Result getBaseCategoryList(){

        List<JSONObject> jsonObjects = baseCategoryService.getBaseCategoryList();

        return Result.ok(jsonObjects);
    }

}
