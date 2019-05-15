package com.atguigu.gmall.payment.mqListener;

import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Date;
import java.util.Map;

@Component
public class PaymentMqListener {
    @Autowired
    PaymentService paymentService;
    @JmsListener(containerFactory = "jmsQueueListener",destination = "PAYMENT_CHECK_QUEUE")
    public void consumeCheckResult(MapMessage mapMessage){
        String out_trade_no = null;
        int count = 0;
        try {
            out_trade_no = mapMessage.getString("out_trade_no");
            count = mapMessage.getInt("count");

        } catch (JMSException e) {
            e.printStackTrace();
        }
        //根据支付状态,再次检查或者结束队列
       Map<String,Object> result =  paymentService.checkPaymentStatusFromAli(out_trade_no);
        String trade_status = (String) result.get("trade_status");
        String trade_no = (String) result.get("trade_no");
        String queryString = (String) result.get("queryString");
        if (trade_status==null || trade_status.equals("WAIT_BUYER_PAY") || trade_status.equals("TRADE_CLOSED")){
            if (count>0){
                //每调用一次检查接口,消耗一次检查接口
                count = count - 1;
                //如果结果为未支付或者交易未创建等,继续发送延迟队列
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setOutTradeNo(out_trade_no);
                paymentService.sendDelayPaymentResult(paymentInfo,count);
            }
        }else {
            //已支付
            //进行幂等性检查
            Map<String,Object> paymentMap = paymentService.checkPaymentStatusFromDb(out_trade_no);
            String payment_status = (String) paymentMap.get("payment_status");
            if (!payment_status.equals("已支付")){
                //根据回调参数,更新支付消息
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setPaymentStatus("已支付");
                paymentInfo.setOutTradeNo(out_trade_no);
                //String queryString = request.getQueryString();// 返回结果的文本
                paymentInfo.setCallbackContent(queryString);
                paymentInfo.setCallbackTime(new Date());
                paymentInfo.setAlipayTradeNo(trade_no);// 返回结果中的阿里交易号

                paymentService.updatePayment(paymentInfo);

                // 发送任务列表到消息队列，让订单系统修改订单状态和支付单据号
                String outTradeNo = paymentInfo.getOutTradeNo();
                paymentService.sendPaymentResult(outTradeNo, trade_status, trade_no);
            }
        }
    }
}
