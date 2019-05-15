package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.util.ActiveMQUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentInfoMapper paymentInfoMapper;
    @Autowired
    ActiveMQUtil activeMQUtil;
    @Autowired
    AlipayClient alipayClient;
    @Override
    public void savePayment(PaymentInfo paymentInfo) {

        paymentInfoMapper.insertSelective(paymentInfo);

    }

    @Override
    public void updatePayment(PaymentInfo paymentInfo) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",paymentInfo.getOutTradeNo());

        PaymentInfo paymentInfo1Param = new PaymentInfo();
        paymentInfo1Param.setAlipayTradeNo(paymentInfo.getAlipayTradeNo());
        paymentInfo1Param.setCallbackContent(paymentInfo.getCallbackContent());
        paymentInfo1Param.setPaymentStatus(paymentInfo.getPaymentStatus());
        paymentInfo1Param.setCallbackTime(paymentInfo.getCallbackTime());

        paymentInfoMapper.updateByExampleSelective(paymentInfo1Param,example);
    }

    @Override
    public void sendPaymentResult(String outTradeNo, String result, String trade_no) {
        try {
            Connection connection = activeMQUtil.getConnection();
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue testqueue = session.createQueue("PAYMENT_SUCCESS_QUEUE");

            MessageProducer producer = session.createProducer(testqueue);
            MapMessage mapMessage = new ActiveMQMapMessage();

            mapMessage.setString("out_trade_no",outTradeNo);
            mapMessage.setString("result",result);
            mapMessage.setString("tracking_no",trade_no);

            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(mapMessage);
            session.commit();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> checkPaymentStatusFromAli(String out_trade_no) {
        //调用支付宝的检查支付状态接口
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        //封装参数
        Map<String,String> requierMap = new HashMap<>();
        requierMap.put("out_trade_no",out_trade_no);
        String s = JSON.toJSONString(requierMap);
        request.setBizContent(s);

        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        //检查支付结果返回的参数
        Map<String ,Object> result = new HashMap<>();
        if (response.isSuccess()){
            //交易已经创建
            String trade_status = response.getTradeStatus();
            System.out.println("调用成功");
            result.put("trade_status",trade_status);
            result.put("trade_no",response.getTradeNo());
            result.put("queryString",response.getMsg());
        }else {
            //如果交易未创建,将调用失败
            boolean success = response.isSuccess();
            System.out.println("调用失败");
        }
        return result;
    }

    @Override
    public void sendDelayPaymentResult(PaymentInfo paymentInfo, int count) {

        try {
            Connection connection = activeMQUtil.getConnection();
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue testqueue = session.createQueue("PAYMENT_CHECK_QUEUE");

            MessageProducer producer = session.createProducer(testqueue);// 根据消息类型，产生队列或者话题执行对象
            //消息内容
            ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();//kv结构的消息内容

            mapMessage.setString("out_trade_no",paymentInfo.getOutTradeNo());
            mapMessage.setInt("count",count);
            //设置消息延迟时间
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_CRON,30*1000);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            producer.send(mapMessage);//执行对象发送消息
            session.commit();//提交消息事物

            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> checkPaymentStatusFromDb(String out_trade_no) {

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(out_trade_no);
        PaymentInfo paymentInfoFromDb = paymentInfoMapper.selectOne(paymentInfo);

        String payment_status = paymentInfoFromDb.getPaymentStatus();

        Map<String,Object> map = new HashMap<>();

        map.put("payment_status",payment_status);

        return map;
    }
}
