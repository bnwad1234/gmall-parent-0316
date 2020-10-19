package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
public class ItemController {

    @Autowired
    ItemFeignClient itemFeignClient;

    @RequestMapping("{skuId}.html")
    public String item(@PathVariable("skuId") String skuId,Model model){

        Map<String,Object> map = new HashMap<>();

        // 调用item-feign查询商品详情
        Result result = itemFeignClient.getItem(skuId);
        map = (Map<String,Object>)result.getData();

        model.addAllAttributes(map);

        return "item/index";
    }

}
