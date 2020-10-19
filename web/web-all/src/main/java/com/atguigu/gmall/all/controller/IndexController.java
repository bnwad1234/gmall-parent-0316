package com.atguigu.gmall.all.controller;


import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class IndexController {

    @Autowired
    ProductFeignClient productFeignClient;

    /**
     * 首页
     * @param request
     * @return
     */
    @GetMapping({"/", "index.html"})
    public String index(HttpServletRequest request) {

        Result result = productFeignClient.getBaseCategoryList();// 返回的是一个三层分类结构的json
        request.setAttribute("list", result.getData());


        return "index/index";
    }


}
