package com.tianhai.warn.mq;

import com.tianhai.warn.constants.AlarmConstants;
import com.tianhai.warn.service.VideoProcessService;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// webm格式视频消息监听器
@Component
public class VideoProcessMessageListener implements
        MessageListenerConcurrently, RocketMQListenerMarker {

    private static final Logger logger = LoggerFactory.getLogger(VideoProcessMessageListener.class);

    @Autowired
    private VideoProcessService videoProcessService;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> messageList,
                                                    ConsumeConcurrentlyContext context) {
        for (MessageExt message : messageList) {
            try {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                logger.info("视频处理消息接收成功，Topic:{}, Tag:{}, 消息内容：{}",
                        message.getTopic(), message.getTags(), body);

                // 处理webm格式视频
                videoProcessService.handleVideoFile(body);
            } catch (Exception e) {
                logger.error("视频处理失败，Topic:{}, Tag:{}", message.getTopic(), message.getTags(), e);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        }

        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    @Override
    public String getTopic() {
        return AlarmConstants.ROCKETMQ_TOPIC_VIDEO_PROCESS;
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
