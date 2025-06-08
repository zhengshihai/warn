package com.tianhai.warn.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;

import java.util.Map;

/**
 * RocketMQConsumerConfig 配置类  用于封装消费者配置信息
 */
@Getter
@Setter
public class RocketMQConsumerConfig {

    private String topic; // 消息的主题
    private String tags; // 消息的标签

    private MessageListenerConcurrently concurrentListener; // 并发消费消息监听器
    private MessageListenerOrderly orderlyListener; // 顺序消费消息监听器

    private Map<String, Object> extraParams; // 额外的参数配置

    private boolean consumeOrderly;

    public RocketMQConsumerConfig(String topic,
                                  String tags,
                                  MessageListenerConcurrently listener) {
        checkTopic(topic);
        this.topic = topic;
        this.tags = tags;
        this.concurrentListener = listener;
        this.consumeOrderly = false;
    }

    public RocketMQConsumerConfig(String topic,
                                  String tags,
                                  MessageListenerOrderly listener) {
        checkTopic(topic);
        this.topic = topic;
        this.tags = tags;
        this.orderlyListener = listener;
        this.consumeOrderly = true;
    }

    public RocketMQConsumerConfig(String topic,
                                  String tags,
                                  MessageListenerConcurrently listener,
                                  Map<String, Object> extraParams) {
        this(topic, tags, listener);
        this.extraParams = extraParams;
    }

    public RocketMQConsumerConfig(String topic,
                                  String tags,
                                  MessageListenerOrderly listener,
                                  Map<String, Object> extraParams) {
        this(topic, tags, listener);
        this.extraParams = extraParams;
    }

    private void checkTopic(String topic) {
        if (topic == null || topic.trim().isEmpty()) {
            throw new IllegalArgumentException("topic cannot be null or empty");
        }
    }

}
