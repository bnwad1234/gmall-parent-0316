package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    ProductFeignClient productFeignClient;

    @Autowired
    ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ListFeignClient listFeignClient;


    @Override
    public Map<String, Object> getItem(String skuId)  {
        Map<String, Object> map = new HashMap<>();
        //map = getItemBak(skuId);
        map = getItemThread(skuId);
        return map;
    }

    private Map<String, Object> getItemThread(String skuId) {


        long currentTimeMillisStart = System.currentTimeMillis();

        Map<String, Object> map = new HashMap<>();

        // 查询sku详细信息sku_info
        CompletableFuture<SkuInfo> completableFutureSkuInfo = CompletableFuture.supplyAsync(new Supplier<SkuInfo>() {
            @Override
            public SkuInfo get() {
                SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
                map.put("skuInfo",skuInfo);
                return skuInfo;
            }
        },threadPoolExecutor);

        // 商品价格查询
        CompletableFuture<Void> completableFuturePrice = CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                BigDecimal price = new BigDecimal("0");
                price = productFeignClient.getSkuPrice(skuId);
                map.put("price", price);
            }
        },threadPoolExecutor);

        // 查询分类列表
        CompletableFuture<Void> completableFutureCategory = completableFutureSkuInfo.thenAcceptAsync(new Consumer<SkuInfo>() {
            @Override
            public void accept(SkuInfo skuInfo) {
                Long category3Id = skuInfo.getCategory3Id();
                BaseCategoryView baseCategoryView = productFeignClient.getCategoryView(category3Id+"");
                map.put("categoryView",baseCategoryView);
            }
        },threadPoolExecutor);

        // 查询销售属性
        CompletableFuture<Void> completableFutureSaleAttr = completableFutureSkuInfo.thenAcceptAsync(new Consumer<SkuInfo>() {
            @Override
            public void accept(SkuInfo skuInfo) {
                List<SpuSaleAttr> spuSaleAttrs = productFeignClient.getMySpuSaleAttrs(skuInfo.getSpuId(), Long.parseLong(skuId));
                map.put("spuSaleAttrList", spuSaleAttrs);
            }
        },threadPoolExecutor);

        // 销售属性对应sku的hash表
        CompletableFuture<Void> completableFutureValueIdsmap = completableFutureSkuInfo.thenAcceptAsync(new Consumer<SkuInfo>() {
            @Override
            public void accept(SkuInfo skuInfo) {
                Map<String, String> valueIdsmap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
                map.put("valuesSkuJson", JSON.toJSONString(valueIdsmap));
            }
        },threadPoolExecutor);

        //商品热度值更新
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                //调用list模块的hotScoere接口更新热度值
                listFeignClient.hotScore(skuId);

            }
        },threadPoolExecutor);

        // 限制线程执行的组合
        CompletableFuture.allOf(completableFutureSkuInfo,completableFuturePrice,completableFutureCategory,completableFutureSaleAttr,completableFutureValueIdsmap).join();
        long currentTimeMillisEnd = System.currentTimeMillis();

        System.out.println("多线程："+(currentTimeMillisEnd - currentTimeMillisStart));
        return map;
    }

    private Map<String, Object> getItemBak(String skuId) {
        long currentTimeMillisStart = System.currentTimeMillis();

        // 商品详情汇总封装基础数据
        Map<String, Object> map = new HashMap<>();

        // 商品价格查询
        BigDecimal price = new BigDecimal("0");
        price = productFeignClient.getSkuPrice(skuId);
        map.put("price",price);

        // 查询sku详细信息sku_info
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        map.put("skuInfo",skuInfo);

        // 查询分类列表
        Long category3Id = skuInfo.getCategory3Id();
        BaseCategoryView baseCategoryView = productFeignClient.getCategoryView(category3Id+"");
        map.put("categoryView",baseCategoryView);

        // 查询销售属性
        List<SpuSaleAttr> spuSaleAttrs =  productFeignClient.getMySpuSaleAttrs(skuInfo.getSpuId(),Long.parseLong(skuId));
        map.put("spuSaleAttrList",spuSaleAttrs);

        // 销售属性对应sku的hash表
        Map<String,String> valueIdsmap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
        map.put("valuesSkuJson", JSON.toJSONString(valueIdsmap));
        long currentTimeMillisEnd = System.currentTimeMillis();

        System.out.println("非多线程："+(currentTimeMillisEnd - currentTimeMillisStart));
        return map;
    }
}
