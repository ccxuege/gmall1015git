package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.user.Service.UserAddressService;
import com.atguigu.gmall.user.Service.UserInfoService;
import com.atguigu.gmall.user.bean.UserAddress;
import com.atguigu.gmall.user.bean.UserInfo;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserInfoController {
    @Autowired
    private UserInfoService userService;


    @Autowired
    UserAddressService userAddressService;

    @RequestMapping("getAllUser")
    public List<UserInfo> getAllUser(){
        return userService.getAllUser();
    }

    @RequestMapping("getAllAddress")
    public List<UserAddress> getAll(){

        List<UserAddress> list = userAddressService.getAll();
        return list;
    }
}


