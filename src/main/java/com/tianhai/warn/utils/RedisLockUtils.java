package com.tianhai.warn.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class RedisLockUtils {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final long DEFAULT_LOCK_EXPIRE = 5 * 60L; // 5分钟

    private static final String LOCK_PREFIX = "lock:";

    /**
     * 获取锁
     *
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
     *
     * @param key       键
     * @param value     值
     */
    public void releaseLock(String key, String value) {
        String currentValue = stringRedisTemplate.opsForValue().get(key);
        if (value.equals(currentValue)) {
            stringRedisTemplate.delete(key);
        }
    }

    /**
     * 通用的缓存读取 + 加锁写入方法（防止并发击穿）
     *
     * @param key           Redis 缓存的 key
     * @param type          返回类型的 Class（如 List.class）
     * @param supplier      获取数据的业务逻辑
     * @param timeout       缓存过期时间
     * @param unit          缓存过期单位
     * @param <T>           返回值类型
     * @return              缓存值
     */
    public <T> T getOrLoad(String key, Class<T> type, Supplier<T> supplier,
                           long timeout, TimeUnit unit) {
        // 优先从缓存中获取
        Object cacheData = redisTemplate.opsForValue().get(key);
        if (cacheData != null) {
            return type.cast(cacheData);
        }

        // 加锁 防止缓存击穿
        String lockKey = LOCK_PREFIX + key;
        String lockValue = UUID.randomUUID().toString();
        boolean lockAcquired = Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 10, TimeUnit.SECONDS));

        if (lockAcquired) {
            try {
                // 再次检查缓存，防止其他线程已经加载数据
                cacheData = redisTemplate.opsForValue().get(key);
                if (cacheData != null) { return type.cast(cacheData); }

                // 通过业务逻辑获得数据
                T result = supplier.get();

                // 写入缓存
                redisTemplate.opsForValue().set(key, result, timeout, unit);

                return result;
            } finally {
                // 释放锁
                String currentLock = stringRedisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(currentLock)) {
                    stringRedisTemplate.delete(lockKey);
                }
            }
        } else {
            // 等待其他线程缓存完毕后重新读
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return type.cast(redisTemplate.opsForValue().get(key));
        }
    }
}
