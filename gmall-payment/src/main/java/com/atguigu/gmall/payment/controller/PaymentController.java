package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.annotation.LoginRqueired;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PaymentController {

    @Reference
    OrderService orderService;
    @Autowired
    AlipayClient alipayClient;
    @Autowired
    PaymentService paymentService;

    @RequestMapping("alipay/callback/return")
    @LoginRqueired
    public String alipayCallBackReturn(HttpServletRequest request){//页面重定向
        String trade_no = request.getParameter("trade_no");
        String app_id = request.getParameter("app_id");
        String out_trade_no = request.getParameter("out_trade_no");
        String result = request.getParameter("trade_status");
        //进行幂等性检查
        Map<String, Object> paymentMap = paymentService.checkPaymentStatusFromDb(out_trade_no);
        String payment_status = (String) paymentMap.get("payment_status");
        if (!payment_status.equals("已支付")){
            //跟据回调参数,更新支付信息
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setPaymentStatus("付钱了啊");
            paymentInfo.setOutTradeNo(out_trade_no);
            String queryString = request.getQueryString();
            paymentInfo.setCallbackContent(queryString);
            paymentInfo.setCallbackTime(new Date());
            paymentInfo.setAlipayTradeNo(trade_no);

            //发送任务列表到消息队列，让订单系统修改订单状态和支付单据号
            String outTradeNo = paymentInfo.getOutTradeNo();
            paymentService.sendPaymentResult(outTradeNo,result,trade_no);

            paymentService.updatePayment(paymentInfo);
        }


        return "finish";
    }

    @RequestMapping("/alipay/submit")
    @ResponseBody
    @LoginRqueired
    public String alipaySubmit(String outTradeNo){
        OrderInfo orderInfo = orderService.getOrderByOutTradeNo(outTradeNo);
        //请求阿里支付接口
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址

        //请求参数
        Map<String,String> requireMap = new HashMap<>();
        requireMap.put("out_trade_no",outTradeNo);
        requireMap.put("product_code","FAST_INSTANT_TRADE_PAY");
        requireMap.put("total_amount","0.01");
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        String subject = "";
        for (OrderDetail orderDetail : orderDetailList) {
            if (StringUtils.isNotBlank(subject)){
                subject = subject + "," + orderDetail.getSkuName();
            }
        }
        requireMap.put("subject",orderInfo.getOrderDetailList().get(0).getSkuName());

        String requireStr = JSON.toJSONString(requireMap);
        alipayRequest.setBizContent(requireStr);      //填充业务参数

        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        System.err.println(form);

        //生成和保存支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        paymentInfo.setPaymentStatus("没付钱啊");
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setSubject(orderInfo.getOrderDetailList().get(0).getSkuName());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setOrderId(orderInfo.getId());
        //保存支付信息
        paymentService.savePayment(paymentInfo);
        //开启检查支付情况的延迟队列
        paymentService.sendDelayPaymentResult(paymentInfo,3);
        //请求支付宝的在线page.pay的支付接口
        return form;
    }


    @RequestMapping("index")
    @LoginRqueired
    public String index(HttpServletRequest request, String outTradeNo, BigDecimal totalAmount, ModelMap map){
        String userId = (String) request.getAttribute("userId");
        String nickName = (String) request.getAttribute("nickName");

        map.put("outTradeNo",outTradeNo);
        map.put("totalAmount",totalAmount);
        map.put("nickName",nickName);

        return "paymentindex";
    }

}
