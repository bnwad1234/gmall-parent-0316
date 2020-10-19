//package com.atguigu.gmall.test.controller;
//
//import org.redisson.api.RLock;
//import org.redisson.api.RedissonClient;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.ResponseBody;
//
//import javax.servlet.http.HttpServletRequest;
//import java.util.concurrent.TimeUnit;
//
//@Controller
//public class TestController {
//
//    @Value("${myName}")
//    private String myName;
//
//    @Autowired
//   // RedissonClient redissonClient;
//
//    @Autowired
//    RedisTemplate redisTemplate;
//
//    @RequestMapping("test")
//    @ResponseBody
//    public String test(HttpServletRequest request){
//
//        Integer ticket = (Integer)redisTemplate.opsForValue().get("ticket");
//
//        ticket--;
//
//        System.out.println(request.getRemoteAddr()+"剩余票数："+ticket);
//
//        redisTemplate.opsForValue().set("ticket",ticket);
//
//        return ticket+"";
//    }
//
//
//    @RequestMapping("testLock")
//    @ResponseBody
//    public String testLock(HttpServletRequest request){
//
//        RLock lock = redissonClient.getLock("lock");
//        Integer ticket = 0;
//        try {
//
//            lock.lock();// lock是一个类似于sync的通用锁
////            boolean b = false;// tryLock返回一个boolean变量可以获得上锁是否成功
////            try {
////                b = lock.tryLock(10, 10,TimeUnit.SECONDS);
////            } catch (InterruptedException e) {
////                e.printStackTrace();
////            }
////
////            if(!b){
////                // 上锁不成功后执行的代码
////            }
//
//            ticket = (Integer) redisTemplate.opsForValue().get("ticket");
//
//            ticket--;
//
//            System.out.println(request.getRemoteAddr() + "剩余票数：" + ticket);
//
//            redisTemplate.opsForValue().set("ticket", ticket);
//
//        }finally {
//            lock.unlock();
//        }
//
//        return ticket+"";
//    }
//
//
//
//
//
//}
