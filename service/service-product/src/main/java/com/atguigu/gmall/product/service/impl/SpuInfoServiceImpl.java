package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.SpuInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpuInfoServiceImpl implements SpuInfoService {

    @Autowired
    SpuInfoMapper spuInfoMapper;

    @Autowired
    BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    SpuImageMapper spuImageMapper;

    @Autowired
    SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Override
    public IPage<SpuInfo> spuList(Page<SpuInfo> pageParam,String category3Id) {

        QueryWrapper<SpuInfo> spuInfoQueryWrapper = new QueryWrapper<>();
        spuInfoQueryWrapper.eq("category3_id",category3Id);
        IPage<SpuInfo> infoIPage = spuInfoMapper.selectPage(pageParam, spuInfoQueryWrapper);

        return infoIPage;
    }

    @Override
    public List<BaseSaleAttr> baseSaleAttrList() {

        return baseSaleAttrMapper.selectList(null);
    }

    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {

        // 保存spuInfo
        spuInfoMapper.insert(spuInfo);
        Long spuId = spuInfo.getId();

        // 保存图片(没有fdfs时候暂时未空)
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if(null!=spuImageList){
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuId);
                spuImageMapper.insert(spuImage);
            }
        }

        // 保存销售属性
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if(null!=spuSaleAttrList){
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuId);
                spuSaleAttrMapper.insert(spuSaleAttr);

                // 保存销售属性值
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                
                if(null!=spuSaleAttrValueList){
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setBaseSaleAttrId(spuSaleAttr.getBaseSaleAttrId());
                        spuSaleAttrValue.setSpuId(spuId);
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    }
                }
            }
        }
    }

    @Override
    public List<SpuImage> spuImageList(String spuId) {

        QueryWrapper<SpuImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spu_id",spuId);
        List<SpuImage> spuImages = spuImageMapper.selectList(queryWrapper);

        return spuImages;
    }

    @Override
    public List<SpuSaleAttr> spuSaleAttrList(String spuId) {
        QueryWrapper<SpuSaleAttr> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spu_id",spuId);

        List<SpuSaleAttr> spuSaleAttrs = spuSaleAttrMapper.selectList(queryWrapper);

        for (SpuSaleAttr spuSaleAttr : spuSaleAttrs) {
            Long baseSaleAttrId = spuSaleAttr.getBaseSaleAttrId();
            QueryWrapper<SpuSaleAttrValue> spuSaleAttrValueQueryWrapper = new QueryWrapper<>();
            spuSaleAttrValueQueryWrapper.eq("spu_id",spuId);
            spuSaleAttrValueQueryWrapper.eq("base_sale_attr_id",baseSaleAttrId);
            List<SpuSaleAttrValue> spuSaleAttrValues = spuSaleAttrValueMapper.selectList(spuSaleAttrValueQueryWrapper);
            spuSaleAttr.setSpuSaleAttrValueList(spuSaleAttrValues);
        }
        
        return spuSaleAttrs;
    }

    @Override
    public List<SpuSaleAttr> getMySpuSaleAttrs(Long spuId) {

        QueryWrapper<SpuSaleAttr> spuSaleAttrQueryWrapper = new QueryWrapper<>();
        spuSaleAttrQueryWrapper.eq("spu_id",spuId);
        List<SpuSaleAttr> spuSaleAttrs = spuSaleAttrMapper.selectList(spuSaleAttrQueryWrapper);

        for (SpuSaleAttr spuSaleAttr : spuSaleAttrs) {
            QueryWrapper<SpuSaleAttrValue> spuSaleAttrValueQueryWrapper = new QueryWrapper<>();
            spuSaleAttrValueQueryWrapper.eq("spu_id",spuId);
            spuSaleAttrValueQueryWrapper.eq("base_sale_attr_id",spuSaleAttr.getBaseSaleAttrId());
            List<SpuSaleAttrValue> spuSaleAttrValues = spuSaleAttrValueMapper.selectList(spuSaleAttrValueQueryWrapper);
            spuSaleAttr.setSpuSaleAttrValueList(spuSaleAttrValues);

        }
        
        return spuSaleAttrs;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long spuId, Long skuId) {

        List<SpuSaleAttr> spuSaleAttrs = spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(spuId,skuId);

        return spuSaleAttrs;
    }
}
