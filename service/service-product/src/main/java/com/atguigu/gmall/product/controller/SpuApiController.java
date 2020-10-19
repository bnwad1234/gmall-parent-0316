package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.SpuInfoService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("admin/product")
@CrossOrigin
public class SpuApiController {

    @Autowired
    SpuInfoService spuInfoService;

    @RequestMapping("spuImageList/{spuId}")
    public Result spuImageList(@PathVariable String spuId){

        List<SpuImage> spuImages = spuInfoService.spuImageList(spuId);
        return Result.ok(spuImages);
    }

    @RequestMapping("spuSaleAttrList/{spuId}")
    public Result spuSaleAttrList(@PathVariable String spuId){

        List<SpuSaleAttr> spuSaleAttrs = spuInfoService.spuSaleAttrList(spuId);
        return Result.ok(spuSaleAttrs);
    }

    @RequestMapping("saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){
        spuInfoService.saveSpuInfo(spuInfo);
        return Result.ok();
    }

    @RequestMapping("baseSaleAttrList")
    public Result baseSaleAttrList(){
        List<BaseSaleAttr> baseSaleAttrs  = spuInfoService.baseSaleAttrList();
        return Result.ok(baseSaleAttrs);
    }

    @RequestMapping("{page}/{limit}")
    public Result spuList(@PathVariable("page") Long page, @PathVariable("limit") Long limit, String category3Id){
        Page<SpuInfo> pageParam = new Page<>(page, limit);
        IPage<SpuInfo> spuInfoIPage = spuInfoService.spuList(pageParam,category3Id);
        return Result.ok(spuInfoIPage);
    }

}
