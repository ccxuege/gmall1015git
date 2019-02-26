package com.atguigu.gmall.user.mapper;

import com.atguigu.gmall.user.bean.UserInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {


    List<UserInfo> selectAllUser();
}
