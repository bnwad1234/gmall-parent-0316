package com.atguigu.gmall.list.client;


import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@FeignClient(value = "service-list")
public interface ListFeignClient {

    @RequestMapping("api/list/cancelSale/{skuId}")
    void cancelSale(@PathVariable("skuId") String skuId);

    @RequestMapping("api/list/onSale/{skuId}")
    void onSale(@PathVariable("skuId") String skuId);

    /**
     * 更新商品热度值的接口
     * @param skuId
     */
    @RequestMapping("api/list/hotScore/{skuId}")
    void hotScore(@PathVariable("skuId") String skuId);

    /**
     * 调用搜索查询的微服务接口
     * @param searchParam
     * @return
     */
    @PostMapping("api/list/list")
    Result list(@RequestBody SearchParam searchParam);
}
