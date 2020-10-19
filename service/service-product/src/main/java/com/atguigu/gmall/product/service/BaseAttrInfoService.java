package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseAttrValue;

import java.util.List;

public interface BaseAttrInfoService {
    List<BaseAttrInfo> attrInfoList(String category1Id, String category2Id, String category3Id);

    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    List<BaseAttrValue> getAttrValueList(String attrId);

    List<BaseAttrInfo> getAttrList(String skuId);
}
