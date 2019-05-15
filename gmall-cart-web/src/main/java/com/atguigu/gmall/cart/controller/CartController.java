package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotation.LoginRqueired;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CartController {

    @Reference
    SkuService skuService;
    @Reference
    CartService cartService;

    @LoginRqueired(isNeededSuccess = false)
    @RequestMapping("changeCart")
    public String changeCart(HttpServletRequest request,HttpServletResponse response,CartInfo cartInfo,ModelMap map){
        List<CartInfo> cartInfos = new ArrayList<>();
        String userId = (String)request.getAttribute("userId");
        //根据选中状态修改购物车数据
        if (StringUtils.isNotBlank(userId)){
            //用户已经登入,修改DB
            cartInfo.setUserId(userId);
            cartService.changeCart(cartInfo);
            cartInfos = cartService.getCartByUserId(userId);
        }else {
            //用户未登录,修改cookie
            String cartListCookie = CookieUtil.getCookieValue(request,"cartListCookie",true);
            cartInfos = JSON.parseArray(cartListCookie,CartInfo.class);
            for (CartInfo info : cartInfos) {
                String skuId = info.getSkuId();
                if (skuId.equals(cartInfo.getSkuId())){
                    info.setIsChecked(cartInfo.getIsChecked());
                }
            }
            //覆盖cookie
            CookieUtil.setCookie(request,response,"cartListCookie",JSON.toJSONString(cartInfos),24*60,true);

        }
        map.put("cartList",cartInfos);
        //总价格
        map.put("totalPrice",getCartTotalPrice(cartInfos));
        return "cartListInner";
    }
    @LoginRqueired(isNeededSuccess = false)
    @RequestMapping("cartList")
    public String cartList(HttpServletRequest request, ModelMap map){

        List<CartInfo> cartInfos = new ArrayList<>();
        String userId=(String)request.getAttribute("userId");
        if (StringUtils.isBlank(userId)){
            // 从cookie中获取购物车数据
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isNotBlank(cartListCookie)){
                    cartInfos = JSON.parseArray(cartListCookie,CartInfo.class);
                }
        }else {
            //从缓存中获取购物车数据
            cartInfos = cartService.getCartByUserId(userId);
        }
        map.put("cartList",cartInfos);
        map.put("totalPrice",getCartTotalPrice(cartInfos));
        return "cartList";
    }

    private BigDecimal getCartTotalPrice(List<CartInfo> cartInfos) {
        BigDecimal bigDecimal = new BigDecimal("0");
        for (CartInfo cartInfo : cartInfos) {
            if (cartInfo.getIsChecked().equals("1")){
                bigDecimal = bigDecimal.add(cartInfo.getCartPrice());
            }
        }
        return bigDecimal;
    }
    @LoginRqueired(isNeededSuccess = false)
    @RequestMapping("addToCart")
    public String addToCart(HttpServletResponse response,HttpServletRequest request, String skuId, int num){
        List<CartInfo> cartInfoList = new ArrayList<>();
        SkuInfo skuInfo = skuService.getSkuById(skuId);
        // 根据skuId获得要添加的购物车对象
        CartInfo cartInfo = new CartInfo();
        cartInfo.setCartPrice(skuInfo.getPrice().multiply(new BigDecimal(num)));
        cartInfo.setSkuNum(num);
        cartInfo.setIsChecked("1");
        cartInfo.setSkuId(skuId);
        cartInfo.setSkuPrice(skuInfo.getPrice());
        cartInfo.setSkuName(skuInfo.getSkuName());
        cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());

        String usrId = (String)request.getAttribute("userId");
        //添加购物车的业务逻辑
        if (StringUtils.isBlank(usrId)){
            //判断cookie是否为空
            String cookieValueStr = CookieUtil.getCookieValue(request, "cartListCookie", true);

            //判断是否存在
            if (StringUtils.isNotBlank(cookieValueStr)){
                cartInfoList = JSON.parseArray(cookieValueStr, CartInfo.class);
                boolean b = if_new_cart(cartInfoList,skuId);
                if (!b){
                    //更新
                    for (CartInfo cartInfoCookie : cartInfoList) {
                        if (cartInfoCookie.getSkuId().equals(skuId)){
                            cartInfoCookie.setSkuNum(cartInfoCookie.getSkuNum() + num);
                            cartInfoCookie.setCartPrice(cartInfoCookie.getSkuPrice().multiply(new BigDecimal(cartInfoCookie.getSkuNum())));
                        }
                    }
                }else {
                    //添加
                    cartInfoList.add(cartInfo);
                }
            }else {
                //添加
                cartInfoList.add(cartInfo);
            }
            //覆盖cookie
            CookieUtil.setCookie(request,response,"cartListCookie",JSON.toJSONString(cartInfoList),24*60,true);

        }else {
            cartInfo.setUserId(usrId);
            //判断数据库是否为空     //判断是否存在
            CartInfo cartInfoFormDb = cartService.if_cart_exists(cartInfo);
            if (cartInfoFormDb!=null){
                //更新db
                cartInfo.setSkuNum(cartInfoFormDb.getSkuNum()+ cartInfo.getSkuNum());
                cartService.updateCart(cartInfo);
            }else {
                //添加db
                cartService.insertCart(cartInfo);
            }
            //同步缓存
            cartService.putCartCache(usrId);
        }
        return "redirect:/success.html";
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

    @RequestMapping("cartSuccess")
    public String cartSuccess(){
        return "success";
    }

}
