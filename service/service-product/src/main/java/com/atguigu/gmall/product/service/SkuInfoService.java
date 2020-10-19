package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.SkuInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.Map;

public interface SkuInfoService {
    void saveSkuInfo(SkuInfo skuInfo);

    IPage<SkuInfo> list(Page<SkuInfo> pageParam);

    void cancelSale(String skuId);

    void onSale(String skuId);

    SkuInfo getSkuInfo(String skuId);

    BigDecimal getSkuPrice(String skuId);

    Map<String,String> getSkuValueIdsMap(Long spuId);
}
