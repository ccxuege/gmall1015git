package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService{

    @Autowired
    CartInfoMapper cartInfoMapper;
    @Autowired
    RedisUtil redisUtil;
    @Override
    public CartInfo if_cart_exists(CartInfo cartInfoParam) {

        CartInfo cartInfoForMapper = new CartInfo();
        cartInfoForMapper.setUserId(cartInfoParam.getUserId());
        cartInfoForMapper.setSkuId(cartInfoParam.getSkuId());

        CartInfo cartInfo = cartInfoMapper.selectOne(cartInfoForMapper);
        return cartInfo;
    }

    @Override
    public void updateCart(CartInfo cartInfo) {
        //更新操作
        CartInfo cartInfoParam = new CartInfo();

        cartInfoParam.setSkuNum(cartInfo.getSkuNum());
        cartInfoParam.setCartPrice(cartInfo.getSkuPrice().multiply(new BigDecimal(cartInfo.getSkuNum())));

        Example e = new Example(CartInfo.class);
        e.createCriteria().andEqualTo("userId",cartInfo.getUserId()).andEqualTo("skuId",cartInfo.getSkuId());

        cartInfoMapper.updateByExampleSelective(cartInfoParam,e);
    }

    @Override
    public void insertCart(CartInfo cartInfo) {
        cartInfoMapper.insert(cartInfo);
    }

    @Override
    public void putCartCache(String userId) {
        Jedis jedis = redisUtil.getJedis();
        try{
            CartInfo cartInfo = new CartInfo();
            cartInfo.setUserId(userId);
            List<CartInfo> cartInfos = cartInfoMapper.select(cartInfo);
            Map<String, String> map = new HashMap<>();
            for (CartInfo info : cartInfos) {
                map.put(info.getSkuId(), JSON.toJSONString(info));
            }
            jedis.hmset("user:"+ userId + ":cart",map);
        }finally {
            jedis.close();
        }

    }

    @Override
    public List<CartInfo> getCartByUserId(String userId) {
        List<CartInfo> cartInfos = new ArrayList<>();
        Jedis jedis = redisUtil.getJedis();
        try{
            List<String> hvals = jedis.hvals("user:" + userId + ":cart");
            if (hvals != null){
                for (String hval : hvals) {
                    CartInfo cartInfo = JSON.parseObject(hval, CartInfo.class);
                    cartInfos.add(cartInfo);
                }
            }
        }finally {
            jedis.close();
        }
        return cartInfos;
    }

    @Override
    public void changeCart(CartInfo cartInfo) {
        //更新购物车选中状态

        //更新数据
        CartInfo cart = new CartInfo();
        cart.setIsChecked(cartInfo.getIsChecked());
        //更新条件
        Example e = new Example(CartInfo.class);
        e.createCriteria().andEqualTo("userId",cartInfo.getUserId()).andEqualTo("skuId",cartInfo.getSkuId());
        cartInfoMapper.updateByExampleSelective(cart,e);

        //从缓存中单拿单放
        Jedis jedis = redisUtil.getJedis();
        try{
            String hget = jedis.hget("user:"+cartInfo.getUserId()+":cart",cartInfo.getSkuId());
            CartInfo cartFromCache = JSON.parseObject(hget, CartInfo.class);
            cartFromCache.setIsChecked(cartInfo.getIsChecked());
            jedis.hset("user:"+cartInfo.getUserId()+":cart",cartFromCache.getSkuId(),JSON.toJSONString(cartFromCache));
        }finally {
            jedis.close();
        }
    }

    @Override
    public void uniteCart(String cartListCookie, String userId) {
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        List<CartInfo> cartInfosFromDb = cartInfoMapper.select(cartInfo);
        if (StringUtils.isNotBlank(cartListCookie)){
            List<CartInfo> cartInfosFromCookie = JSON.parseArray(cartListCookie, CartInfo.class);
            //更新Db
            for (CartInfo infoFromDb : cartInfosFromDb) {
                for (CartInfo infoFromCookie : cartInfosFromCookie) {
                    if (infoFromDb.getSkuId().equals(infoFromCookie.getSkuId())){
                        BigDecimal add = new BigDecimal(infoFromCookie.getSkuNum()).add(new BigDecimal(infoFromDb.getSkuNum()));
                        int i = add.intValue();
                        infoFromDb.setSkuNum(i);
                        infoFromDb.setCartPrice(infoFromDb.getSkuPrice().multiply(add));
                        //更新
                        Example example = new Example(CartInfo.class);
                        example.createCriteria().andEqualTo("userId",infoFromDb.getUserId())
                                .andEqualTo("skuId",infoFromDb.getSkuId());

                        CartInfo updateInfo = new CartInfo();
                        updateInfo.setCartPrice(infoFromDb.getCartPrice());
                        updateInfo.setSkuNum(infoFromDb.getSkuNum());
                        cartInfoMapper.updateByExampleSelective(updateInfo,example);
                    }
                }
            }
            //添加db
            for (CartInfo infoFromCookie : cartInfosFromCookie) {
                boolean b = if_new_cart(cartInfosFromDb, infoFromCookie.getSkuId());
                if (b){
                    //添加数据库
                    infoFromCookie.setUserId(userId);
                    cartInfoMapper.insertSelective(infoFromCookie);

                    cartInfosFromDb.add(infoFromCookie);
                }
            }
        }
        //同步redis
        Jedis jedis = redisUtil.getJedis();
        try{
            Map<String,String> map = new HashMap<>();
            for (CartInfo info : cartInfosFromDb) {
                map.put(info.getSkuId(),JSON.toJSONString(info));
            }
            jedis.hmset("user:"+userId+":cart",map);
        }finally {
            jedis.close();
        }
    }

    @Override
    public void delCartById(ArrayList<String> delCartIds, String userId) {
        String ids = StringUtils.join(delCartIds, ",");
        cartInfoMapper.delCartByIds(ids);

        //同步缓存
        putCartCache(userId);
    }

    /**
     * 判断要添加的购物车商品是否曾经添加过
     * @param cartInfoList
     * @param skuId
     * @return
     */
    private boolean if_new_cart(List<CartInfo> cartInfoList, String skuId) {
        boolean b = true;
        for (CartInfo cartInfo : cartInfoList) {
            if (cartInfo.getSkuId().equals(skuId)){
                b = false;
            }
        }
        return b;
    }
}
