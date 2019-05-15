package com.atguigu.gmall.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotation.LoginRqueired;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.enums.PaymentWay;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    CartService cartService;
    @Reference
    UserService userService;
    @Reference
    OrderService orderService;

    /**
     * 订单提交功能
     * @param request
     * @param tradeCode
     * @param deliveryAddress
     * @param map
     * @return
     */
    @RequestMapping("submitOrder")
    @LoginRqueired(isNeededSuccess = true)
    public String submitOrder(HttpServletRequest request,String tradeCode,String deliveryAddress,ModelMap map){
        String userId = (String) request.getAttribute("userId");
        //检验交易码
        boolean b = orderService.checkTradeCode(userId,tradeCode);
        SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss");
        String format = yyyyMMddHHmmss.format(new Date());

        BigDecimal totalAmount = null;
        String outTradeNo = "atguigu"+ format + System.currentTimeMillis();
        if (b){
            // 提交订单
            // 根据地址id获得收获地址信息
           UserAddress userAddress =  userService.getAddressById(deliveryAddress);
           //根据userId取出购物车数据
            List<CartInfo> cartInfos = cartService.getCartByUserId(userId);

            //将购物车数据生成订单数据
            ArrayList<String> delCartIds = new ArrayList<>();
            OrderInfo orderInfo = new OrderInfo();

            orderInfo.setOutTradeNo(outTradeNo); //系统外部订单号 atguigu+时间字符串+毫秒时间戳
            orderInfo.setProcessStatus("订单已提交");
            orderInfo.setPaymentWay(PaymentWay.ONLINE);
            orderInfo.setOrderStatus("订单已提交");
            //获取一个Calendar对象并可以进行时间的计算，时区的指定
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE,1);
            orderInfo.setExpireTime(calendar.getTime());
            orderInfo.setConsigneeTel(userAddress.getPhoneNum());
            orderInfo.setConsignee(userAddress.getConsignee());
            orderInfo.setCreateTime(new Date());
            orderInfo.setDeliveryAddress(userAddress.getUserAddress());
            orderInfo.setOrderComment("硅谷商品订单");

            totalAmount =  getCartTotalPrice(cartInfos);
            orderInfo.setTotalAmount(totalAmount);
            orderInfo.setUserId(userId);

            List<OrderDetail> orderDetails = new ArrayList<>();
            for (CartInfo cartInfo : cartInfos) {
                if (cartInfo.getIsChecked().equals("1")){
                    OrderDetail orderDetail = new OrderDetail();
                    orderDetail.setSkuNum(cartInfo.getSkuNum());
                    orderDetail.setImgUrl(cartInfo.getImgUrl());

                    orderDetail.setSkuId(cartInfo.getSkuId());
                    orderDetail.setSkuName(cartInfo.getSkuName());

                    //价格和库存强一致性校验
                    //验价,验库存
                    orderDetail.setHasStock("");
                    orderDetail.setOrderPrice(cartInfo.getCartPrice());
                    orderDetails.add(orderDetail);
                    delCartIds.add(cartInfo.getId());
                }
            }
            orderInfo.setOrderDetailList(orderDetails);
            //保存订单
            orderService.saveOrder(orderInfo);
            //删除购物车数据
//             cartService.delCartById(delCartIds,userId);

        }else {
            return "tradeFail";
        }
        return "redirect:http://payment.gmall.com:8090/index?outTradeNo="+outTradeNo+"&totalAmount="+totalAmount;
    }

    /**
     * 订单结算功能
     * @param request
     * @return
     */
    @RequestMapping("trade")
    @LoginRqueired(isNeededSuccess = true)
    public String toTrade(HttpServletRequest request, ModelMap map){
        String userId = (String) request.getAttribute("userId");
        //获取用户的收获地址列表
        List<UserAddress> userAddresses = userService.getAllAddressByUser(userId);
        //将用户的购物车缓存取出
        List<CartInfo> cartInfos = cartService.getCartByUserId(userId);
        //根据购物车商品选中状态,封装订单详情
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (CartInfo cartInfo : cartInfos) {
            if (cartInfo.getIsChecked().equals("1")){
                OrderDetail orderDetail = new OrderDetail();

                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setOrderPrice(cartInfo.getCartPrice());

                orderDetails.add(orderDetail);
            }
        }
        map.put("userAddressList",userAddresses);
        map.put("orderDetailList",orderDetails);
        map.put("totalAmount",getCartTotalPrice(cartInfos));
        //生成交易码,放在后台缓存
        String tradeCode = orderService.getTradeCode(userId);
        map.put("tradeCode",tradeCode);
        return "trade";
    }

    private BigDecimal getCartTotalPrice(List<CartInfo> cartInfos) {

        BigDecimal b = new BigDecimal("0");

        for (CartInfo cartInfo : cartInfos) {
            if (cartInfo.getIsChecked().equals("1")) {
                b = b.add(cartInfo.getCartPrice());
            }
        }
        return b;

    }

}
