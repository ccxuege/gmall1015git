package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.ArrayList;
import java.util.List;

public interface CartService {
    CartInfo if_cart_exists(CartInfo cartInfo);

    void updateCart(CartInfo cartInfo);

    void insertCart(CartInfo cartInfo);

    void putCartCache(String usrId);

    List<CartInfo> getCartByUserId(String userId);

    void changeCart(CartInfo cartInfo);

    void uniteCart(String cartListCookie, String id);

    void delCartById(ArrayList<String> delCartIds, String userId);
}
