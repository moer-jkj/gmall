package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.entity.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CartAsyncService cartAsyncService;

    private static final String KEY_PREFIX = "cart:info:";

    private static final String PRICE_PREFIX = "cart:price:";

    public void addCart(Cart cart) {
        // 1.获取登录信息
        String userId = getUserId();
        String key = KEY_PREFIX + userId;

        // 2. 获取redis中该用户的购物车
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

        // 3. 判断该用户的购物车信息是否包含了该商品
        String skuId = cart.getSkuId().toString();
        BigDecimal count = cart.getCount();  // 用户添加的商品数量
        if (hashOps.hasKey(skuId)){
            // 4.包含，更新数量
            String cartJson = hashOps.get(skuId).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(cart.getCount().add(count));
            //this.cartMapper.update(cart,new UpdateWrapper<Cart>().eq("user_id",cart.getUserId()).eq("sku_id",skuId));
            this.cartAsyncService.updateCartByUserIdAndSkuId(userId,skuId,cart);
        }else {
            // 5.不包含，给该用户新增购物车记录
            cart.setUserId(userId);
            // 根据skuId查询sku
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                throw new CartException("没有对应的商品");
            }
            cart.setTitle(skuEntity.getTitle());
            cart.setPrice(skuEntity.getPrice());
            cart.setDefaultImage(skuEntity.getDefaultImage());


            // 查询销售信息
            ResponseVo<List<SkuAttrValueEntity>> saleAttrValueBySkuId = this.pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrValueBySkuId.getData();
            cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntities));

            // 查询营销信息
            ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            cart.setSales(JSON.toJSONString(itemSaleVos));

            // 查询库存信息
            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkusBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }

            // 商品刚加入购物车时，默认为选中状态
            cart.setCheck(true);

            // 保存到redis和mysql
            this.cartAsyncService.saveCart(userId, cart);

            // 给购物车对应商品添加实时价格
            this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuId, skuEntity.getPrice().toString());
        }
        hashOps.put(skuId, JSON.toJSONString(cart));
    }

    private String getUserId() {
        // 1. 获取登录信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId = userInfo.getUserKey();
        if (userInfo.getUserId() != null){
            userId = userInfo.getUserId().toString();
        }
        return userId;
    }

    public Cart queryCartBySkuId(Long skuId) {
        // 1.获取登录信息
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (!hashOps.hasKey(skuId.toString())){
            throw new CartException("没有对应的购物车记录！");
        }
        String json = hashOps.get(skuId.toString()).toString();
        if (StringUtils.isNotBlank(json)){
            return JSON.parseObject(json,Cart.class);
        }
        throw new CartException("没有对应的购物车记录！");
    }

    public List<Cart> queryCarts() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        // 1. 先查询未登录的购物车
        String unloginKey = KEY_PREFIX + userKey;
        // 获取了未登录的购物车
        BoundHashOperations<String, Object, Object> unloginHashOps = this.redisTemplate.boundHashOps(unloginKey);
        // 获取未登录购物车集合
        List<Object> cartJsons = unloginHashOps.values();
        List<Cart> unloginCarts = null;
        // 反序列化为cart集合
        if (!CollectionUtils.isEmpty(cartJsons)){
            unloginCarts = cartJsons.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                // 把每条记录string 反序列化为cart对象
                String currentPrice = this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());

                cart.setCurrentPrice(new BigDecimal(currentPrice));
                return cart;
            }).collect(Collectors.toList());
        }
        // 2. 判断是否登录,未登录则直接返回
        Long userId = userInfo.getUserId();
        if (userId == null){
            return unloginCarts;
        }

        // 3.登录则合并购物车
        String loginKey = KEY_PREFIX + userId;
        // 获取登录状态购物车操作对象
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(loginKey);
        // 判断是否存在未登录的购物车，有则遍历未登录购物车合并到已登录的购物车中
        if (!CollectionUtils.isEmpty(unloginCarts)){
            unloginCarts.forEach(cart -> {
                String skuId = cart.getSkuId().toString();
                BigDecimal count = cart.getCount();  // 未登录状态购物车数量
                if (loginHashOps.hasKey(skuId)){
                    // 如果登录状态的购物车包含该商品，则更新数量
                    String cartJson = loginHashOps.get(skuId).toString();
                    // 获取登录状态的购物车对象
                    cart = JSON.parseObject(cartJson, Cart.class);
                    cart.setCount(cart.getCount().add(count));


                    // 异步写入mysql
                    this.cartAsyncService.updateCartByUserIdAndSkuId(userId.toString(),skuId,cart);
                }else{
                    // 登录状态购物车不包含该商品，则新增
                    // 把userKey更新为userId
                    cart.setUserId(userId.toString());
                    // 异步添加到mysql
                    this.cartAsyncService.saveCart(userId.toString(),cart);
                }
                // 同步写入redis
                loginHashOps.put(skuId,JSON.toJSONString(cart));
            });
        }


        // 4. 删除 未登录状态的购物车
        // 同步删除redis中购物车
        this.redisTemplate.delete(unloginKey);
        // 异步删除mysql中购物车
        this.cartAsyncService.deleteByUserId(userKey);

        // 5.查询购物车记录(redis)
        List<Object> loginCartJsons = loginHashOps.values();
        if (CollectionUtils.isEmpty(loginCartJsons)){
            return null;
        }


        return loginCartJsons.stream().map(cartJson -> {
            Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);

            cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId())));
            return cart;
        }).collect(Collectors.toList());

    }

    public void updateNum(Cart cart) {
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (!hashOps.hasKey(cart.getSkuId().toString())){
            throw new CartException("该用户没有对应的购物车记录");
        }

        // 用户要更新的数量
        BigDecimal count = cart.getCount();

        // 查询redis中的购物车记录
        String json = hashOps.get(cart.getSkuId().toString()).toString();
        cart = JSON.parseObject(json, Cart.class);
        cart.setCount(count); // 更新购物车中的商品数量

        hashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
        this.cartAsyncService.updateCartByUserIdAndSkuId(userId, cart.getSkuId().toString(), cart);
    }

    public void deleteCart(Long skuId) {

        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

        hashOps.delete(skuId.toString());
        this.cartAsyncService.deleteCart(userId, skuId);

    }


    public List<Cart> queryCheckedCarts(Long userId) {

        String key = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        List<Object> cartJsons = hashOps.values();
        if (CollectionUtils.isEmpty(cartJsons)){
            return null;
        }

        return cartJsons.stream().map(cartJson -> JSON.parseObject(cartJson.toString(), Cart.class))
                .filter(cart -> cart.getCheck()).collect(Collectors.toList());

    }
}
