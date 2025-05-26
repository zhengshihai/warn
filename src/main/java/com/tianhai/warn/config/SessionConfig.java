package com.tianhai.warn.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 使用spring session 替代默认的HttpSession实现
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800) // session 失效时间： 30分钟
public class SessionConfig {
}
