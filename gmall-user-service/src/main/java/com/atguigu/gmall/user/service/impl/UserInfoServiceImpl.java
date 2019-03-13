package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class UserInfoServiceImpl implements UserService {
    @Autowired
    UserInfoMapper userMapper;
    @Autowired
    UserAddressMapper userAddressMapper;
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
}
