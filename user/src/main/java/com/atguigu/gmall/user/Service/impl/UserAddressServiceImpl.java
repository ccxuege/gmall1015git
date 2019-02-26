package com.atguigu.gmall.user.Service.impl;

import com.atguigu.gmall.user.Service.UserAddressService;
import com.atguigu.gmall.user.bean.UserAddress;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class UserAddressServiceImpl implements UserAddressService {

    @Autowired
    UserAddressMapper userAddressMapper;
    @Override
    public List<UserAddress> getAll() {

        List<UserAddress> userAddresses =  userAddressMapper.selectAll();
        return userAddresses;
    }
}
