package com.tianhai.warn.service;

import com.tianhai.warn.utils.Result;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;

import java.util.Map;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     * 
     * @param name     姓名
     * @param email    邮箱
     * @param password 密码
     * @param role     角色
     * @return 登录结果，包含用户信息
     */
//    Result<Map<String, Object>> login(String name, String email, String password, String role);
    Map<String, Object> login(String name, String email, String password, String role);


    /**
     * 处理“记住我”功能，生成JWT并返回Cookie
     * @param loginInfo 登录成功后返回的用户信息Map，包含userId、role等
     * @param remember 是否勾选“记住我”
     * @return 如果勾选“记住我”且成功生成，返回Cookie对象；否则返回null
     */
    Cookie handleRememberMe(Map<String, Object> loginInfo, boolean remember);

    Map<String, Object> findUserByRememberMeToken(String token);
}
