package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.SkuAttrValue;
import com.atguigu.gmall.model.product.SkuImage;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SkuSaleAttrValue;
import com.atguigu.gmall.product.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.product.mapper.SkuImageMapper;
import com.atguigu.gmall.product.mapper.SkuInfoMapper;
import com.atguigu.gmall.product.mapper.SkuSaleAttrValueMapper;
import com.atguigu.gmall.product.service.SkuInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class SkuInfoServiceImpl implements SkuInfoService {

    @Autowired
    SkuInfoMapper skuInfoMapper;

    @Autowired
    SkuImageMapper skuImageMapper;

    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    ListFeignClient listFeignClient;

    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {

        skuInfoMapper.insert(skuInfo);

        Long skuId = skuInfo.getId();

        for (SkuImage skuImage : skuInfo.getSkuImageList()) {
            skuImage.setSkuId(skuId);
            skuImageMapper.insert(skuImage);
        }

        for (SkuAttrValue skuAttrValue : skuInfo.getSkuAttrValueList()) {
            skuAttrValue.setSkuId(skuId);
            skuAttrValueMapper.insert(skuAttrValue);
        }

        for (SkuSaleAttrValue skuSaleAttrValue : skuInfo.getSkuSaleAttrValueList()) {
            skuSaleAttrValue.setSkuId(skuId);
            skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
            skuSaleAttrValueMapper.insert(skuSaleAttrValue);
        }


    }

    @Override
    public IPage<SkuInfo> list(Page<SkuInfo> pageParam) {

        // 分页查询
        IPage<SkuInfo> skuInfoIPage = skuInfoMapper.selectPage(pageParam, null);
        return skuInfoIPage;
    }

    @Override
    public void cancelSale(String skuId) {

        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(Long.parseLong(skuId));
        skuInfo.setIsSale(0);
        skuInfoMapper.updateById(skuInfo);

        // 同步到nosql
        listFeignClient.cancelSale(skuId);
    }

    @Override
    public void onSale(String skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(Long.parseLong(skuId));
        skuInfo.setIsSale(1);
        skuInfoMapper.updateById(skuInfo);

        // 同步到nosql
        listFeignClient.onSale(skuId);
    }

    @GmallCache
    @Override
    public SkuInfo getSkuInfo(String skuId) {
        SkuInfo skuInfo = new SkuInfo();
        // 查询db
        System.out.println("查询skuInfo的db");
        skuInfo = getSkuInfoFromDb(skuId);
        return skuInfo;
    }


    public SkuInfo getSkuInfoForCache(String skuId) {

        System.out.println(Thread.currentThread().getName()+"访问sku详情");

        long currentTimeMillisStart = System.currentTimeMillis();

        SkuInfo skuInfo = null;

        // 查询缓存
        String skuStrFromCache = (String)redisTemplate.opsForValue().get("sku:" + skuId + ":info");

        if(StringUtils.isBlank(skuStrFromCache)){
            System.out.println(Thread.currentThread().getName()+"没有获取到sku详情，申请分布式锁");

            String lockId = UUID.randomUUID().toString();

            Boolean lock = redisTemplate.opsForValue().setIfAbsent("sku:" + skuId + ":lock", lockId, 10, TimeUnit.SECONDS);

            if(lock){
                System.out.println(Thread.currentThread().getName()+"没有获取到sku详情，拿到布式锁，开始访问db");

                // 查询db
                skuInfo = getSkuInfoFromDb(skuId);

                // 同步缓存
                if(null!=skuInfo){
                    redisTemplate.opsForValue().set("sku:" + skuId + ":info", JSON.toJSONString(skuInfo));
                }else{
                    redisTemplate.opsForValue().set("sku:" + skuId + ":info", JSON.toJSONString(new SkuInfo()),10,TimeUnit.SECONDS);
                }
                // 获得分布式锁的线程操作完毕，归还分布式锁
                System.out.println(Thread.currentThread().getName()+"归还布式锁");

                // 删除本线程的锁,判断是否本线程锁
                String lockIdFromCache = (String)redisTemplate.opsForValue().get("sku:" + skuId + ":lock");
                if(StringUtils.isNotBlank(lockIdFromCache)&&lockIdFromCache.equals(lockId)){
                    redisTemplate.delete("sku:" + skuId + ":lock");
                }

                // 使用lua脚本删除锁
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                // 设置lua脚本返回的数据类型
                DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                // 设置lua脚本返回类型为Long
                redisScript.setResultType(Long.class);
                redisScript.setScriptText(script);
                redisTemplate.execute(redisScript, Arrays.asList("sku:"+skuId+":lock"),lockId);

            }else{
                // 自旋
                System.out.println(Thread.currentThread().getName()+"没有获得分布式锁，开始自旋");

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return getSkuInfo(skuId);
            }

        }else{
            skuInfo = JSON.parseObject(skuStrFromCache,SkuInfo.class);
        }
        long currentTimeMillisEnd = System.currentTimeMillis();

        System.out.println(currentTimeMillisEnd - currentTimeMillisStart);

        return skuInfo;
    }

    private SkuInfo getSkuInfoFromDb(String skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);

        QueryWrapper<SkuImage> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("sku_id",skuId);

        List<SkuImage> skuImages = skuImageMapper.selectList(queryWrapper);

        skuInfo.setSkuImageList(skuImages);

        return skuInfo;
    }

    @Override
    public BigDecimal getSkuPrice(String skuId) {

        BigDecimal bigDecimal = new BigDecimal("0");

        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);

        if(null!=skuInfo){
            bigDecimal = skuInfo.getPrice();
        }
        return bigDecimal;
    }

    @Override
    public Map<String, String> getSkuValueIdsMap(Long spuId) {

        List<Map<String,Object>> resultMapList =  skuSaleAttrValueMapper.selectSkuValueIdsMap(spuId);
        Map<String, String> valuleIdsMap = new HashMap<>();
        for (Map<String, Object> map : resultMapList) {
            String skuId =  map.get("sku_id").toString();
            String valueId =  map.get("value_ids").toString();
            valuleIdsMap.put(valueId,skuId);
        }

        return valuleIdsMap;
    }
}
