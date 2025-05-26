package com.tianhai.warn.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisLockUtils {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final long DEFAULT_LOCK_EXPIRE = 5 * 60L; // 5分钟

    /**
     * 获取锁
     * @param key               键
     * @param value             值
     * @param expireSeconds     过期时间
     * @return                  获取结果
     */
    public boolean tryLock(String key, String value, long expireSeconds) {
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, value, Duration.ofSeconds(expireSeconds));

        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放锁
     * @param key       键
     * @param value     值
     */
    public void releaseLock(String key, String value) {
        String currentValue = stringRedisTemplate.opsForValue().get(key);
        if (value.equals(currentValue)) {
            stringRedisTemplate.delete(key);
        }
    }
}
