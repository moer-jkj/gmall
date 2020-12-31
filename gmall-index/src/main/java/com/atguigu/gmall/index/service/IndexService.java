package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.config.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.util.DistributedLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    private GmallPmsClient gmallPmsClient;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private DistributedLock distributedLock;

    private static final String KEY_PREFIX = "index:cates";

    public List<CategoryEntity> queryLvl1CategoryByPid(){
        // 根据父分类id查询category，0l则为一级分类
        ResponseVo<List<CategoryEntity>> responseVo = this.gmallPmsClient.queryCategory(0l);
        return responseVo.getData();
    }

    @GmallCache(prefix = KEY_PREFIX,timeout = 43200,random = 4320,lock = "index:lock")
    public List<CategoryEntity> queryLvl2CategoriesWithSubById(Long pid) {

        ResponseVo<List<CategoryEntity>> listResponseVo = this.gmallPmsClient.queryCategoriesWithSubsByPid(pid);
        List<CategoryEntity> data = listResponseVo.getData();

        return data;
    }


    /*public List<CategoryEntity> queryLvl2CategoriesWithSubById(Long pid) {
        // 先查询缓存
        String jsonStr = redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(jsonStr) && !StringUtils.equals(null,jsonStr)){
            return JSON.parseArray(jsonStr,CategoryEntity.class);
        } else if(StringUtils.equals(null,jsonStr)){
            return null;
        }

        RLock lock = this.redissonClient.getLock("index:lock:" + pid);
        lock.lock();

        // 获取锁过程中，可能有其他请求已经提前获取到锁，并把数据放入到缓存中
        // 加完锁之后，再次确认缓存中有没有
        String jsonStr2 = redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(jsonStr2) && !StringUtils.equals(null,jsonStr2)){
            return JSON.parseArray(jsonStr2,CategoryEntity.class);
        } else if(StringUtils.equals(null,jsonStr2)){
            return null;
        }

        // 缓存中没有，就去查询数据
        ResponseVo<List<CategoryEntity>> listResponseVo = this.gmallPmsClient.queryCategoriesWithSubsByPid(pid);
        List<CategoryEntity> data = listResponseVo.getData();

        // 查询到数据之后，放入缓存
        if (CollectionUtils.isEmpty(data)){
            this.redisTemplate.opsForValue().set(KEY_PREFIX + pid,null,10, TimeUnit.SECONDS);
        } else {
            // 为了防止缓存雪崩，给缓存时间添加随机值
            this.redisTemplate.opsForValue().set(KEY_PREFIX + pid,JSON.toJSONString(data),30 + new Random().nextInt(10), TimeUnit.DAYS);
        }
        // 释放锁
        lock.unlock();
        return data;
    }*/




    public void testLock() {
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.distributedLock.tryLock("lock", uuid, 300l);
        if (lock) {
            // 查询redis中的num值
            String value = this.redisTemplate.opsForValue().get("num");
            // 没有值就直接return
            if (StringUtils.isBlank(value)) {
                return;
            }
            // 有值就转换成int
            int num = Integer.parseInt(value);
            // 把redis中的num值+1
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

            //2. 释放锁
            this.distributedLock.unlock("lock",uuid);

        }
    }

    /*public void testLock2() {
        // 1. 从redis中获取锁， setnx
        *//**
         * 释放锁的时候可能会释放其他服务器的锁
         * 解决 ： uuid防误删
         *//*

        //Boolean lock = this.redisTemplate.opsForValue().setIfAbsent("lock", "111", 5, TimeUnit.SECONDS);

        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.redisTemplate.opsForValue().setIfAbsent("losk", uuid, 3, TimeUnit.SECONDS);

        if (lock) {
            // 查询redis中的num值
            String value = this.redisTemplate.opsForValue().get("num");
            // 没有值就直接return
            if (StringUtils.isBlank(value)) {
                return;
            }
            // 有值就转换成int
            int num = Integer.parseInt(value);
            // 把redis中的num值+1
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

            //2. 释放锁
            //this.redisTemplate.delete("lock");
            *//**
             * 在判断的过程中，锁过期，然后删除的可能是其他线程的锁
             * 解决方式：采用lua脚本实现删除操作的原子性
             *//*
            if (StringUtils.equals(redisTemplate.opsForValue().get("lock"),uuid)){
                this.redisTemplate.delete("lock");
            }

        } else {
            //3. 每隔1秒钟回调一次，再次尝试获取锁
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }*/

    /*public void testLock1(){
        //1. 从redis中获取锁， setnx
         *//**
          setnx刚好获取到锁，业务逻辑出现异常，导致锁无法释放
         解决 ： 设置过期时间，自动释放锁
         1.使用expire，但是缺乏原子性
         2.在set时指定过期时间
          *//*
        //Boolean lock = this.redisTemplate.opsForValue().setIfAbsent("lock", "111");
        Boolean lock = this.redisTemplate.opsForValue().setIfAbsent("lock", "111", 5, TimeUnit.SECONDS);
        if (lock){
            // 查询redis中的num值
            String value = this.redisTemplate.opsForValue().get("num");
            // 没有值就直接return
            if (StringUtils.isBlank(value)){
                return;
            }
            // 有值就转换成int
            int num = Integer.parseInt(value);
            // 把redis中的num值+1
            this.redisTemplate.opsForValue().set("num",String.valueOf(++num));

            //2. 释放锁
            this.redisTemplate.delete("lock");

        }else{
            //3. 每隔1秒钟回调一次，再次尝试获取锁
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
*/
}
