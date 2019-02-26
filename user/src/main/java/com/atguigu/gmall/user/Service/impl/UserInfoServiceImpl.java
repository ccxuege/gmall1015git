package com.atguigu.gmall.user.Service.impl;

import com.atguigu.gmall.user.Service.UserInfoService;
import com.atguigu.gmall.user.bean.UserInfo;
import com.atguigu.gmall.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class UserInfoServiceImpl implements UserInfoService {
    @Autowired
    UserMapper userMapper;

    @Override
    public List<UserInfo> getAllUser() {
        return userMapper.selectAllUser();
    }
}
