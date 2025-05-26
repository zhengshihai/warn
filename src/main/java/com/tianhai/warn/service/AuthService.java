package com.tianhai.warn.service;

import com.tianhai.warn.utils.Result;
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



}
