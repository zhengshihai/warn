package com.tianhai.warn.mq;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

//定位信息存储重试任务
@Service
public class LocationCompensateTask {

    private static final Logger logger = LoggerFactory.getLogger(LocationCompensateTask.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RocketMQMessageSender rocketMQMessageSender;

    private static final String COMPENSATE_TOPIC =  "alarm-location-compensate";
    private static final String COMPENSATE_REDIS_LIST = "location:compensate:redis";
    private static final String DEAD_LETTER_LIST = "location:compensate:dead";
    private static final int MAX_RETRY = 10;
    private static final int BATCH_SIZE = 10;

    @Scheduled(fixedDelay = 60000) // 1分钟
    public void compensateFromRedisList() {
        for (int i = 0; i < BATCH_SIZE; i++) {
            Object compensateListObj = redisTemplate.opsForList().leftPop(COMPENSATE_REDIS_LIST);
            if (compensateListObj == null) break;
            try {
                // 直接转为JSON字符串投递
                String json = JSON.toJSONString(compensateListObj);
                rocketMQMessageSender.sendMessage(COMPENSATE_TOPIC, "compensate", null, json);
            } catch (Exception e) {
                // 处理重试次数
                Map<String, Object> map;
                try {
                    map = (Map<String, Object>) compensateListObj;
                } catch (Exception parseEx) {
                    // 数据格式异常，直接丢到死信队列
                    redisTemplate.opsForList().rightPush(DEAD_LETTER_LIST, compensateListObj);
                    continue;
                }

                int retry = map.get("retryCount") == null ? 0 : (Integer) map.get("retryCount");
                if (retry >= MAX_RETRY) {
                    redisTemplate.opsForList().rightPush(DEAD_LETTER_LIST, map);
                    logger.info("补偿消息重试超限，转人工处理：{}", JSON.toJSONString(map));
                } else {
                    map.put("retryCount", retry + 1);
                    redisTemplate.opsForList().rightPush(COMPENSATE_REDIS_LIST, map);
                }

                break; // 防止死循环
            }
        }
    }
}
