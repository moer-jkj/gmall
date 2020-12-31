package com.atguigu.gmall.index.util;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DistributedLock {

    @Autowired
    StringRedisTemplate redisTemplate;

    public Boolean tryLock(String lockName,String uuid,Long expire){
        String script = "if(redis.call('exists',KEYS[1]) == 0 or redis.call('hexists',KEYS[1],ARGV[1]) == 1))" +
                "then" +
                "   tredis.call('hincrby',KEYS[1],ARGV[1],1);" +
                "   redis.call('expire',KEYS[1],ARGV[2]);" +
                "else" +
                "   return 0;" +
                "end;";
        if (this.redisTemplate.execute(new DefaultRedisScript<>(script,Boolean.class), Arrays.asList(lockName),uuid,expire.toString())){
            try {
                // 没有获取到锁，重试
                Thread.sleep(200);

                tryLock(lockName,uuid,expire);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 锁续期
        this.renewTime(lockName,expire);

        // 获取到锁 ，返回true
        return true;
    }

    public void unlock(String lockName,String uuid){
        String script = "if (redis.call('hexists', KEYS[1], ARGV[1]) == 0) then" +
                "    return nil;" +
                "end;" +
                "if (redis.call('hincrby', KEYS[1], ARGV[1], -1) > 0) then" +
                "    return 0;" +
                "else" +
                "    redis.call('del', KEYS[1]);" +
                "    return 1;" +
                "end;";
        Long result = this.redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Lists.newArrayList(lockName), uuid);
        if (result == null) {
            throw new IllegalMonitorStateException("attempt to unlock lock, not locked by lockName: " + lockName + " with request: "  + uuid);
        }
    }


    // 自动续期
    public void renewTime(String lockName,Long expire){
        String script = "if redis.call('exists', KEYS[1]) == 1 then return redis.call('expire', KEYS[1], ARGV[1]) else return 0 end";
        new Thread(()->{
            while (this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Lists.newArrayList(lockName), expire.toString())){

                try {
                    Thread.sleep(expire * 2 / 3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

}
