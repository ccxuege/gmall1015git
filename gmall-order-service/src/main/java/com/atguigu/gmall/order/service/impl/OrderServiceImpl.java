package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    OrderInfoMapper orderInfoMapper;
    @Autowired
    OrderDetailMapper orderDetailMapper;
    @Autowired
    ActiveMQUtil activeMQUtil;
    @Override
    public String getTradeCode(String userId) {

        //生成交易码
        String tradeCodeKey = "user:"+userId+":tradeCode";
        String tradeCodeValue = UUID.randomUUID().toString();
        Jedis jedis = redisUtil.getJedis();
        try {
            jedis.setex(tradeCodeKey,60*30,tradeCodeValue);
        }finally {
            jedis.close();
        }
        return tradeCodeValue;
    }

    @Override
    public boolean checkTradeCode(String userId, String tradeCode) {
        boolean b = false;
        String tradeCodeKey = "user:" + userId + ":tradeCode";
        Jedis jedis = redisUtil.getJedis();
        try {
            String tradeCodeFromCache = jedis.get(tradeCodeKey);
            if (StringUtils.isNotBlank(tradeCodeFromCache)){
                if (tradeCodeFromCache.equals(tradeCode)){
                    b = true;
                    //交易码删除
                    jedis.del(tradeCodeKey);
                }
            }
        }finally {
            jedis.close();
        }
        return b;
    }

    @Override
    public void saveOrder(OrderInfo orderInfo) {
        //保存订单表
        orderInfoMapper.insertSelective(orderInfo);
        String orderId = orderInfo.getId();

        //保存订单详情表
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderId);
            orderDetailMapper.insertSelective(orderDetail);
        }
    }

    @Override
    public OrderInfo getOrderByOutTradeNo(String outTradeNo) {
        OrderInfo orderInfoParam = new OrderInfo();
        orderInfoParam.setOutTradeNo(outTradeNo);
        OrderInfo orderInfo = orderInfoMapper.selectOne(orderInfoParam);

        OrderDetail orderDetailParam = new OrderDetail();
        orderDetailParam.setOrderId(orderInfo.getId());
        List<OrderDetail> orderDetails = orderDetailMapper.select(orderDetailParam);

        orderInfo.setOrderDetailList(orderDetails);

        return orderInfo;
    }

    @Override
    public void updateOrder(String out_trade_no, String tracking_no) {
        OrderInfo orderInfoParam = new OrderInfo();

        orderInfoParam.setTrackingNo(tracking_no);
        orderInfoParam.setOrderStatus("订单已支付");
        orderInfoParam.setProcessStatus("订单已支付");

        Example example = new Example(OrderInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",out_trade_no);

        orderInfoMapper.updateByExampleSelective(orderInfoParam,example);
    }

    @Override
    public void sendOrderResult(String out_trade_no) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOutTradeNo(out_trade_no);
        OrderInfo orderInfoResult = orderInfoMapper.selectOne(orderInfo);

        String id = orderInfoResult.getId();
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(id);

        List<OrderDetail> orderDetails = orderDetailMapper.select(orderDetail);
        orderInfoResult.setOrderDetailList(orderDetails);

        String orderJson = JSON.toJSONString(orderInfoResult);
        try {
            Connection connection = activeMQUtil.getConnection();
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);//开启消息事物
            Destination testqueue = session.createQueue("ORDER_SUCCESS_QUEUE");

            MessageProducer producer = session.createProducer(testqueue);// 根据消息类型，产生队列或者话题执行对象

            //消息内容
            ActiveMQTextMessage textMessage = new ActiveMQTextMessage();//kv结构的消息内容

            textMessage.setText(orderJson);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);//持久化消息
            producer.send(textMessage);
            session.commit();
            connection.close();
        }catch (Exception e){
            e.printStackTrace();;
        }
    }
}
