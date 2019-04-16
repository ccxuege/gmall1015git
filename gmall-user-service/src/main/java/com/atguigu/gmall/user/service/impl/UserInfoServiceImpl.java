package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class UserInfoServiceImpl implements UserService {
    @Autowired
    UserInfoMapper userMapper;
    @Autowired
    UserAddressMapper userAddressMapper;
    @Autowired
    RedisUtil redisUtil;
    @Override
    public List<UserInfo> getAllUser() {
        return userMapper.selectAllUser();
    }

    @Override
    public List<UserAddress> getAllAddress() {
        return userAddressMapper.selectAll();
    }

    @Override
    public List<UserInfo> allUser() {
        return userMapper.selectAll();
    }

    @Override
    public List<UserInfo> getUserByUsername(String loginName) {
        List<UserInfo> userInfos = userMapper.selectByExample(loginName);

        return userInfos;
    }

    @Override
    public UserInfo login(UserInfo userInfoParam) {
        UserInfo userInfo = userMapper.selectOne(userInfoParam);
        if (userInfo != null){
            // 将用户信息写入缓存
            Jedis jedis = redisUtil.getJedis();
            try {
                jedis.setex("user:"+userInfo.getId()+":info",60*60, JSON.toJSONString(userInfo));
            }finally {
                jedis.close();
            }
        }
        return userInfo;
    }

    @Override
    public List<UserAddress> getAllAddressByUser(String userId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        List<UserAddress> select = userAddressMapper.select(userAddress);
        return select;
    }

    @Override
    public UserAddress getAddressById(String deliveryAddress) {
        UserAddress userAddress = new UserAddress();
        userAddress.setId(deliveryAddress);
        UserAddress userAddress1 = userAddressMapper.selectOne(userAddress);
        return userAddress1;
    }
}
