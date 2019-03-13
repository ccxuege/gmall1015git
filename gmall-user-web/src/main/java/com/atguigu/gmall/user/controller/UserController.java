package com.atguigu.gmall.user.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class UserController {

    @Reference
    private UserService userService;

    @ResponseBody
    @RequestMapping("allUser")
    public List<UserInfo> getAll(){

        List<UserInfo> userInfos = userService.getAllUser();

        return userInfos;
    }
    @RequestMapping("get/User/{UserName}")
    public List<UserInfo> getUserByNameAndId(@PathVariable("UserName") String UserName){

        List<UserInfo> userByUsername = userService.getUserByUsername(UserName);
        return userByUsername;
    }
}
