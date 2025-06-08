package com.tianhai.warn.mq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RocketMQMessageSender {
    private static final Logger logger = LoggerFactory.getLogger(RocketMQMessageSender.class);

    // 使用Map存储不同业务的生产者
    private final Map<String, DefaultMQProducer> producerMap = new ConcurrentHashMap<>();

    @Autowired
    public RocketMQMessageSender(
            @Qualifier("alarmProducer") DefaultMQProducer alarmProducer,
            @Qualifier("videoProducer") DefaultMQProducer videoProducer) {
        // 初始化生产者Map
        producerMap.put("alarm", alarmProducer);
        producerMap.put("video", videoProducer);
    }

    /**
     * 根据topic获取对应的生产者
     */
    private DefaultMQProducer getProducer(String topic) {
        if (topic.startsWith("alarm-")) {
            return producerMap.get("alarm");
        } else if (topic.startsWith("video-")) {
            return producerMap.get("video");
        } else {
            logger.warn("未知的topic类型: {}, 使用默认的报警生产者", topic);
            return producerMap.get("alarm");
        }
    }

    public SendResult sendMessage(String topic, String tags, String keys, String content) {
        try {
            DefaultMQProducer producer = getProducer(topic);
            Message msg = new Message(topic, tags, keys, content.getBytes(StandardCharsets.UTF_8));
            SendResult sendResult = producer.send(msg);
            logger.info("消息发送成功: topic={}, tags={}, keys={}, result={}",
                    topic, tags, keys, sendResult);
            return sendResult;
        } catch (Exception e) {
            logger.error("消息发送失败: topic={}, tags={}, keys={}", topic, tags, keys, e);
            throw new RuntimeException("消息发送失败", e);
        }
    }

    public void sendMessageAsync(String topic, String tags, String keys, String content,
            SendCallback sendCallback) {
        try {
            DefaultMQProducer producer = getProducer(topic);
            Message msg = new Message(topic, tags, keys, content.getBytes(StandardCharsets.UTF_8));
            producer.send(msg, sendCallback);
        } catch (Exception e) {
            logger.error("异步消息发送失败: topic={}, tags={}, keys={}", topic, tags, keys, e);
            throw new RuntimeException("异步消息发送失败", e);
        }
    }
}

//该Sender使用示例：
/*

@Service
public class AlarmService {
    @Autowired
    private RocketMQMessageSender messageSender;

    public void sendAlarmMessage(String content) {
        // 发送报警消息
        messageSender.sendMessage(
            "alarm-location",  // topic
            "location",        // tags
            "key-001",         // keys
            content            // content
        );
    }
}

@Service
public class VideoService {
    @Autowired
    private RocketMQMessageSender messageSender;

    public void sendVideoMessage(String content) {
        // 发送视频消息
        messageSender.sendMessage(
            "video-stream",    // topic
            "stream",          // tags
            "key-002",         // keys
            content            // content
        );
    }
}

 */
