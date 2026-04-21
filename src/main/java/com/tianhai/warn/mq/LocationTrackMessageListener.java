package com.tianhai.warn.mq;

import com.tianhai.warn.constants.AlarmConstants;
import com.tianhai.warn.service.LocationTrackService;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.stereotype.Component; // 已注释：停用此监听器

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 定位信息消息监听器（已停用，改为直接调用服务，不使用 RocketMQ）
 @Component
public class LocationTrackMessageListener implements
        MessageListenerConcurrently, RocketMQListenerMarker {

    private static final Logger logger = LoggerFactory.getLogger(LocationTrackMessageListener.class);

    @Autowired
    private LocationTrackService locationTrackService;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> messageList,
            ConsumeConcurrentlyContext consumeConcurrentlyContext) {
        for (MessageExt message : messageList) {
            try {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                logger.info("RocketMQ 消息接收成功，Topic: {}, Tag: {}, 消息内容: {}",
                        message.getTopic(), message.getTags(), body);

                locationTrackService.handleLocationMessage(body);

            } catch (Exception e) {
                logger.error("RocketMQ 消息处理失败，Topic: {}, Tag: {}, 错误信息: {}",
                        message.getTopic(), message.getTags(), e.getMessage(), e);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER; // 处理失败，稍后重试
            }
        }

        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    @Override
    public String getTopic() {
        return AlarmConstants.ROCKETMQ_TOPIC_LOCATION;
    }

    @Override
    public String getTags() {
        return "*";
    }

    @Override
    public boolean isConsumeOrderly() {
        return false;
    }

    @Override
    public Map<String, Object> extraParams() {
        return new HashMap<>();
    }
}
