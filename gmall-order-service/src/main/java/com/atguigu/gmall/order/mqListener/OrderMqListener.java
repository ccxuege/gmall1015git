package com.atguigu.gmall.order.mqListener;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderMqListener {


    @Reference
    PaymentService paymentService;
    @Autowired
    OrderService orderService;
    @JmsListener(containerFactory = "jmsQueueListener",destination = "PAYMENT_SUCCESS_QUEUE")
    public void consumePaymentResult(MapMessage mapMessage){
        //消费消息队列
        String out_trade_no = null;
        String tracking_no = null;

        try {
            out_trade_no = mapMessage.getString("out_trade_no");
            tracking_no = mapMessage.getString("tracking_no");
        } catch (JMSException e) {
            e.printStackTrace();
        }
        System.out.println("消费订单号："+out_trade_no+"支付的消息队列,修改订单状态，写入支付宝的交易号");
        orderService.updateOrder(out_trade_no,tracking_no);

        //发送订单成功队列ORDER_SUCCESS_QUEUE
        orderService.sendOrderResult(out_trade_no);
    }
}
