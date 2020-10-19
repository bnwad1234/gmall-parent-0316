package com.atguigu.gmall.common.cache;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
public class GmallCacheAspect {

    @Autowired
    RedisTemplate redisTemplate;

    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint point) {

        Object result = null;

        System.out.println("环绕通知，执行被代理方法之前");
        // 获得参数
        Object[] args = point.getArgs();
        MethodSignature signature = (MethodSignature)point.getSignature();
        GmallCache annotation = signature.getMethod().getAnnotation(GmallCache.class);
        String key = annotation.prefix()+":"+args[0];

        // 查询缓存
        result = cacheHit(signature, key);

        if(null==result){
            // 执行被代理方法
            // 获得分布式锁
            String lockId = UUID.randomUUID().toString();
            Boolean lock = redisTemplate.opsForValue().setIfAbsent(key + ":lock", lockId, 10, TimeUnit.SECONDS);
            if(lock){
                try {
                    result = point.proceed();// 访问db
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
                // 同步到缓存
                if(null!=result){
                    redisTemplate.opsForValue().set(key , JSON.toJSONString(result));
                }else{
                    Object o = null;
                    try {
                        o = signature.getReturnType().newInstance();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    redisTemplate.opsForValue().set(key , JSON.toJSONString(o),10,TimeUnit.SECONDS);
                }
                // 删除分布式锁
                // 使用lua脚本删除锁
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                // 设置lua脚本返回的数据类型
                DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                // 设置lua脚本返回类型为Long
                redisScript.setResultType(Long.class);
                redisScript.setScriptText(script);
                redisTemplate.execute(redisScript, Arrays.asList(key+":lock"),lockId);
            }else{
                // 自旋
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 避免无限自选的方法，是为每一个线程设置自选上限，自选变量的要求，线程独享，而且每次递归自选变量+1
                return cacheHit(signature, key);
            }
        }
        System.out.println("环绕通知，执行被代理方法之后");

        return result;
    }


    /**
     * @param signature 可以获取到方法的返回值
     * @param key       需要获取数据使用
     * @return
     */
    private Object cacheHit(MethodSignature signature, String key) {

        String cache = (String) redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(cache)) {
            Class returnType = signature.getReturnType();
            return JSONObject.parseObject(cache, returnType);
        }else{
            return null;
        }
    }


}
