package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface SpuInfoService {

    IPage<SpuInfo> spuList(Page<SpuInfo> pageParam,String category3Id);

    List<BaseSaleAttr> baseSaleAttrList();

    void saveSpuInfo(SpuInfo spuInfo);

    List<SpuImage> spuImageList(String spuId);

    List<SpuSaleAttr> spuSaleAttrList(String spuId);

    List<SpuSaleAttr> getMySpuSaleAttrs(Long spuId);

    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long spuId, Long skuId);
}
