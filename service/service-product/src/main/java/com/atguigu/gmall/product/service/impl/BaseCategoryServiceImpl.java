package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.BaseCategory1;
import com.atguigu.gmall.model.product.BaseCategory2;
import com.atguigu.gmall.model.product.BaseCategory3;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.product.mapper.BaseCategory1Mapper;
import com.atguigu.gmall.product.mapper.BaseCategory2Mapper;
import com.atguigu.gmall.product.mapper.BaseCategory3Mapper;
import com.atguigu.gmall.product.mapper.BaseCategoryViewMapper;
import com.atguigu.gmall.product.service.BaseCategoryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BaseCategoryServiceImpl implements BaseCategoryService{

    @Autowired
    BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    BaseCategoryViewMapper baseCategoryViewMapper;

    @Override
    public List<BaseCategory1> getCategory1() {
        return baseCategory1Mapper.selectList(null);
    }

    @Override
    public List<BaseCategory2> getCategory2(String category1Id) {

        QueryWrapper<BaseCategory2> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category1_id",category1Id);
        List<BaseCategory2> baseCategory2s = baseCategory2Mapper.selectList(queryWrapper);
        return baseCategory2s;
    }

    @Override
    public List<BaseCategory3> getCategory3(String category2Id) {
        QueryWrapper<BaseCategory3> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category2_id",category2Id);
        List<BaseCategory3> baseCategory3s = baseCategory3Mapper.selectList(queryWrapper);
        return baseCategory3s;
    }

    @Override
    public BaseCategoryView getCategoryView(String category3Id) {

        BaseCategoryView baseCategoryView = baseCategoryViewMapper.selectById(category3Id);

        return baseCategoryView;
    }

    @Override
    public List<JSONObject> getBaseCategoryList() {

        // 查询categoryView
        List<BaseCategoryView> baseCategoryViews = baseCategoryViewMapper.selectList(null);

        // 将categoryView转换成JSON的集合(节点属性相同)
        Map<Long, List<BaseCategoryView>> collectCategory1 = baseCategoryViews.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));

        List<JSONObject> jsonObjects1 = new ArrayList<>();
        for (Map.Entry<Long, List<BaseCategoryView>> ListEntry1 : collectCategory1.entrySet()) {
            // 封装1级分类的id和名字(一级分类)
            Long category1Id = ListEntry1.getKey();
            String category1Name = ListEntry1.getValue().get(0).getCategory1Name();
            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("categoryId",category1Id);
            jsonObject1.put("categoryName",category1Name);

            // 封装1级分类的子集(二级分类)
            List<JSONObject> jsonObjects2 = new ArrayList<>();
            Map<Long, List<BaseCategoryView>> collectCategory2 = ListEntry1.getValue().stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            for (Map.Entry<Long, List<BaseCategoryView>> listEntry2 : collectCategory2.entrySet()) {
                Long category2Id = listEntry2.getKey();
                String category2Name = listEntry2.getValue().get(0).getCategory2Name();
                JSONObject jsonObject2 = new JSONObject();
                jsonObject2.put("categoryId",category2Id);
                jsonObject2.put("categoryName",category2Name);

                // 封装2级分类的子集(三级分类)
                List<BaseCategoryView> listEntry3 = listEntry2.getValue();
                List<JSONObject> jsonObjects3 = listEntry3.stream().map(entry3->{
                    JSONObject jsonObject3 = new JSONObject();
                    Long category3Id = entry3.getCategory3Id();
                    String category3Name = entry3.getCategory3Name();
                    jsonObject3.put("categoryId",category3Id);
                    jsonObject3.put("categoryName",category3Name);
                    return jsonObject3;
                }).collect(Collectors.toList());// 集合三级分类元素
                jsonObject2.put("categoryChild",jsonObjects3);
                jsonObjects2.add(jsonObject2);//集合二级分类元素
            }
            jsonObject1.put("categoryChild",jsonObjects2);
            jsonObjects1.add(jsonObject1);// 集合一级分类元素
        }
        return jsonObjects1;
    }
}
