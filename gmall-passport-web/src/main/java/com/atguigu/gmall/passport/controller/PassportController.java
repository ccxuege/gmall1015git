package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    UserService userService;
    @Reference
    CartService cartService;

    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request, String token, String currentIp){
        String b = "false";
        try {
            Map userMap = JwtUtil.decode("gmall1015atguigu",token,currentIp);
            // 验证通过
            if (userMap!=null){
                b = "true";
            }
        }catch (Exception e){
        }
        return b;
    }

    @RequestMapping("index")
    public String index(String ReturnUrl, ModelMap map){
        map.put("ReturnUrl",ReturnUrl);
        return "index";
    }
    @RequestMapping("login")
    @ResponseBody
    public String login(HttpServletRequest request, HttpServletResponse response, UserInfo userInfoParam){
        String token = "";
        //根据用户信息认证用户身份
        UserInfo user = userService.login(userInfoParam);

        if (user != null){
            //通过jwt算法加密后生成token
            String serviceKey = "gmall1015atguigu";
            String ip = getMyIp(request);
            Map<String,String> userMap = new HashMap<>();
            userMap.put("userId",user.getId());
            userMap.put("nickName",user.getNickName());
            token = JwtUtil.encode(serviceKey,userMap,ip);

            //调用购物车合并服务，并发服务
            String cartListCookie = CookieUtil.getCookieValue(request,"cartListCookie",true);
            cartService.uniteCart(cartListCookie,user.getId());
            //合并购物车之后删除，cookie
            CookieUtil.deleteCookie(request,response,"cartListCookie");
        }else {
            token = "fail";
        }

        return token;
    }

    private String getMyIp(HttpServletRequest request) {
        String ip = "";
        ip = request.getHeader("x-forwarded-for");// nginx所代理的客户端的ip
        if (StringUtils.isBlank(ip)){
            ip = request.getRemoteAddr();//直接获得客户端的ip
        }
        return ip;
    }
}
