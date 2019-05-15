package com.atguigu.gmall.interceptors;

import com.atguigu.gmall.annotation.LoginRqueired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpClientUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        System.err.print("用户登陆状态验证拦截器");
        //该方法是否需要拦截
        HandlerMethod hm = (HandlerMethod)handler;
        LoginRqueired methodAnnotation = hm.getMethodAnnotation(LoginRqueired.class);

        if (methodAnnotation==null){
            return true;
        }else {
            boolean neededSuccess = methodAnnotation.isNeededSuccess();
            // cookie中token
            // 地址栏中的token
            String token = "";
            String oleToken = CookieUtil.getCookieValue(request,"gmallToken",true);
            String newToken = request.getParameter("gmallToken");
            if (StringUtils.isNotBlank(newToken)){
                token = newToken;
            }else {
                token = oleToken;
            }
            System.err.print("该方法有注解，需要拦截，验证用户的登陆信息");
            if (StringUtils.isNotBlank(token)){
                String ip = "";
                ip = request.getHeader("x-forwarded-for");// nginx所代理的客户端的ip
                if (StringUtils.isBlank(ip)){
                    ip = request.getRemoteAddr();//直接获得客户端的ip
                }
                String success = HttpClientUtil.doGet("http://passport.gmall.com:8085/verify?token="+token+"&currentIp="+ip);// 远程调用认证中心passport验证,http请求
                if (!success.equals("true")){
                    if (neededSuccess){
                        //验证失败并且必须登陆，打回认证中心
                        String ReturnUrl = request.getRequestURL().toString();
                        response.sendRedirect("http://passport.gmall.com:8085/index?ReturnUrl="+ReturnUrl);
                        return false;
                    }
                }else {
                        // 将用户信息放入请求中
                    Map gmall1015atguigu = JwtUtil.decode("gmall1015atguigu", token, ip);
                    request.setAttribute("userId",gmall1015atguigu.get("userId"));
                    request.setAttribute("nickName",gmall1015atguigu.get("nickName"));
                }
                // 用户通过验证后，将token重新写入cookie
                CookieUtil.setCookie(request,response,"gmallToken",token,60*60,true);
            }else {
                if (neededSuccess){
                    //验证失败并且必须登陆，打回认证中心
                   String ReturnUrl =  request.getRequestURL().toString();
                   response.sendRedirect("http://passport.gmall.com:8085/index?ReturnUrl="+ReturnUrl);
                   return false;
                }
            }
        }
        return true;
    }
}
