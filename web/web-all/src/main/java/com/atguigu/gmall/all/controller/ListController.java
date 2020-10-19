package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchAttr;
import com.atguigu.gmall.model.list.SearchParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import sun.security.krb5.internal.crypto.Des;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ListController {
    @Autowired
    private ListFeignClient listFeignClient;

    @RequestMapping({"list.html", "search.html"})
    public String list(SearchParam searchParam, Model model) {
        /**
         * 搜索页面展示
         */
        //调用搜索服务，查询搜索结果
        Result<Map> result = listFeignClient.list(searchParam);
        model.addAllAttributes(result.getData());
        model.addAttribute("searcheParam", searchParam);
        model.addAttribute("urlParam", getUrlParam(searchParam));
        Map<String, String> orderMap = getOrderMap(searchParam);
        model.addAttribute("orderMap", orderMap);
        /**
         * 面包屑功能的实现
         */
        String trademark = searchParam.getTrademark();
        if (StringUtils.isNotBlank(trademark)) {
            model.addAttribute("trademark", trademark.split(":")[1]);

        }
        String[] props = searchParam.getProps();
        if (null != props && props.length > 0) {
            ArrayList<SearchAttr> searchAttrs = new ArrayList<>();
            for (String prop : props) {
                String[] split = prop.split(":");
                String attrId = split[0];
                String attrName = split[1];
                String attrValue = split[2];
                SearchAttr searchAttr = new SearchAttr();
                searchAttr.setAttrId(Long.parseLong(attrId));
                searchAttr.setAttrName(attrName);
                searchAttr.setAttrValue(attrValue);
                searchAttrs.add(searchAttr);
            }
            model.addAttribute("propsParamList", searchAttrs);
        }
        return "list/index";
    }

    /**
     * 排序的处理
     *
     * @param searchParam
     * @return
     */
    private Map<String, String> getOrderMap(SearchParam searchParam) {
        String order = searchParam.getOrder();
        HashMap<String, String> orderMap = new HashMap<>();
        if (StringUtils.isNotBlank(order)) {
            String[] split = order.split(":");
            String type = split[0];
            String sort = split[1];
            orderMap.put("tyep", type);
            orderMap.put("order", order);
        } else {
            orderMap.put("type", "1");
            orderMap.put("sort", "desc");
        }
        return orderMap;
    }

    private String getUrlParam(SearchParam searchParam) {
        /**
         * 根据搜索对象searchParam拼接url
         */
        Long category3Id = searchParam.getCategory3Id();
        String keyword = searchParam.getKeyword();
        String trademark = searchParam.getTrademark();
        String[] props = searchParam.getProps();
        String urlParam = "list.html?";
        //判断三级分类
        if (null != category3Id && category3Id > 0) {
            //进行拼接
            urlParam = urlParam + "category3Id=" + category3Id;
        }
        if (StringUtils.isNotBlank(keyword)) {
            urlParam = urlParam + "keyword=" + keyword;
        }
        //判断品牌
        if (StringUtils.isNotBlank(trademark)) {
            urlParam = urlParam + "trademark=" + trademark;

        }
        //判断平台属性
        if (null != props && props.length > 0) {
            for (String prop : props) {
                urlParam = urlParam + "prop=" + prop;
            }

        }
        return urlParam;
    }


}
