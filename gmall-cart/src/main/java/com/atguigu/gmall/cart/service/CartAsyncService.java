package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CartAsyncService {

    @Autowired
    private CartMapper cartMapper;

    @Async
    public void updateCartByUserIdAndSkuId(String userId, String skuId, Cart cart){
        cartMapper.update(cart,new UpdateWrapper<Cart>().eq("user_id",userId).eq("sku_id",skuId));
    }

    /**
     * 为了方便将来在异常处理器中获取异常用户信息
     * 所有异步方法的第一个参数统一为userId
     * @param userId
     * @param cart
     */
    @Async
    public void saveCart(String userId, Cart cart){
        this.cartMapper.insert(cart);
    }

    @Async
    public void deleteByUserId(String userId) {
        this.cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id", userId));
    }

    public void deleteCart(String userId, Long skuId) {
        this.cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id", userId).eq("sku_id", skuId));
    }
}
