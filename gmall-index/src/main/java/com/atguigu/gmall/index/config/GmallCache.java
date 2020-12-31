package com.atguigu.gmall.index.config;


import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GmallCache {

    // 缓存key的前缀
    // key: prefix + ":" + 方法参数
    String prefix() default "";

    // 缓存的有效时间 默认五分钟
    int timeout() default 5;

    /**
     * 防止雪崩
     * 让使用人员指定随机值范围
     * @return
     */
    int random() default 5;

    /**
     * 为了防止缓存击穿
     * 使用人员指定分布式的锁key的前缀
     * 默认是 lock
     */
    String lock() default "lock:";

}
