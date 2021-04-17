package com.atguigu.gmall.cart.interceptor;

import com.atguigu.gmall.cart.config.JwtProperties;
import com.atguigu.gmall.cart.entity.UserInfo;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class LoginInterceptor implements HandlerInterceptor {

    // 声明线程的局部变量
    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<UserInfo>();

    @Autowired
    private JwtProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserInfo userInfo = new UserInfo();

        // 获取登录头信息
        String userKey = CookieUtils.getCookieValue(request, properties.getUserKey());
        // 如果 userKey为空，制作一个 userKey放入cookie
        if (StringUtils.isBlank(userKey)){
            userKey = UUID.randomUUID().toString();
            CookieUtils.setCookie(request,response,properties.getUserKey(),userKey,properties.getExpire());
        }

        userInfo.setUserKey(userKey);

        // 获取用户登录信息
        String token = CookieUtils.getCookieValue(request, properties.getCookieName());
        if (StringUtils.isBlank(token)){
            // 未登录，把 userKey 放入 THREAD_LOCAL 中
            THREAD_LOCAL.set(userInfo);
            return true;
        }
        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, properties.getPublicKey());
        Long userId = Long.valueOf(map.get("userId").toString());
        userInfo.setUserId(userId);

        // 把信息放入线程的局部变量
        THREAD_LOCAL.set(userInfo);


        // 目的是为了统一获取登录状态，不管有没有登录都要放行
        return true;
    }


    // 封装一个获取线程局部变量值的静态方法
    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }

    // 在视图渲染完成之后执行
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 手动删除，因为使用的是线程池，请求结束后，线程不会结束
        // 如果不手动删除线程变量，可能会造成内存泄漏
        THREAD_LOCAL.remove();
    }
}
