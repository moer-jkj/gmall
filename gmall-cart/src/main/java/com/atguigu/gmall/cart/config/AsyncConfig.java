package com.atguigu.gmall.cart.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.util.concurrent.Executor;

public class AsyncConfig implements AsyncConfigurer {

    @Autowired
    private AsyncExceptionHandler asyncExceptionHandler;

    /**
     * 配置线程池，可以创建 ThreadPoolExecutor
     * 默认ThreadPoolExecutor，通过 TaskExecutionAutoConfiguration自动化配置类创建出来的
     * @return
     */
    @Override
    public Executor getAsyncExecutor() {
        return null;
    }

    /**
     * 配置异步未捕获异常处理器
     * @return
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return asyncExceptionHandler;
    }
}


